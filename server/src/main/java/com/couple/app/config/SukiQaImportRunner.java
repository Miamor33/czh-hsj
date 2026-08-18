package com.couple.app.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.couple.app.entity.*;
import com.couple.app.mapper.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 一次性导入 Suki 问答；完成后写入 setting，后续启动跳过。
 */
@Component
@Order(250)
public class SukiQaImportRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SukiQaImportRunner.class);
    public static final String SETTING_KEY = "suki_qa_imported";

    private final ObjectMapper objectMapper;
    private final SettingMapper settingMapper;
    private final PartnerMapper partnerMapper;
    private final QaQuestionMapper questionMapper;
    private final QaAnswerMapper answerMapper;
    private final QaReplyMapper replyMapper;

    public SukiQaImportRunner(ObjectMapper objectMapper, SettingMapper settingMapper,
                              PartnerMapper partnerMapper, QaQuestionMapper questionMapper,
                              QaAnswerMapper answerMapper, QaReplyMapper replyMapper) {
        this.objectMapper = objectMapper;
        this.settingMapper = settingMapper;
        this.partnerMapper = partnerMapper;
        this.questionMapper = questionMapper;
        this.answerMapper = answerMapper;
        this.replyMapper = replyMapper;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Setting flag = settingMapper.selectOne(new LambdaQueryWrapper<Setting>()
                .eq(Setting::getSettingKey, SETTING_KEY));
        if (flag != null && "true".equalsIgnoreCase(flag.getSettingValue())) {
            return;
        }

        JsonNode root;
        try (InputStream in = new ClassPathResource("db/qa-suki.json").getInputStream()) {
            root = objectMapper.readTree(in);
        }
        JsonNode questions = root.get("questions");
        if (questions == null || !questions.isArray() || questions.isEmpty()) {
            log.warn("qa-suki.json 无题目，跳过导入");
            markImported();
            return;
        }

        removeUnansweredPlaceholderQuestions();

        Map<String, Long> partnerIdByKey = partnerMapper.selectList(null).stream()
                .collect(Collectors.toMap(Partner::getPartnerKey, Partner::getId, (a, b) -> a));

        Set<String> existingContents = questionMapper.selectList(null).stream()
                .map(QaQuestion::getContent)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<JsonNode> sorted = new ArrayList<>();
        questions.forEach(sorted::add);
        sorted.sort(Comparator.comparingInt(n -> n.path("sukiNo").asInt(0)));

        int inserted = 0;
        for (JsonNode node : sorted) {
            String content = node.path("content").asText("").trim();
            if (content.isEmpty() || existingContents.contains(content)) {
                continue;
            }
            QaQuestion q = new QaQuestion();
            q.setContent(content);
            q.setCreatedAt(LocalDateTime.now());
            questionMapper.insert(q);
            existingContents.add(content);
            inserted++;

            JsonNode answers = node.get("answers");
            if (answers != null && answers.isObject()) {
                insertAnswerIfPresent(q.getId(), partnerIdByKey.get("czh"), answers.path("czh"));
                insertAnswerIfPresent(q.getId(), partnerIdByKey.get("hsj"), answers.path("hsj"));
            }

            JsonNode replies = node.get("replies");
            if (replies != null && replies.isArray()) {
                for (JsonNode reply : replies) {
                    String partnerKey = reply.path("partnerKey").asText("");
                    String replyContent = reply.path("content").asText("").trim();
                    Long partnerId = partnerIdByKey.get(partnerKey);
                    if (partnerId == null || replyContent.isEmpty()) {
                        continue;
                    }
                    QaReply r = new QaReply();
                    r.setQuestionId(q.getId());
                    r.setPartnerId(partnerId);
                    r.setContent(replyContent);
                    r.setCreatedAt(LocalDateTime.now());
                    replyMapper.insert(r);
                }
            }
        }

        markImported();
        log.info("Suki 问答一次性导入完成，新插入 {} 题", inserted);
    }

    private void insertAnswerIfPresent(Long questionId, Long partnerId, JsonNode node) {
        if (partnerId == null || node == null || node.isNull() || !node.isTextual()) {
            return;
        }
        String content = node.asText("").trim();
        if (content.isEmpty()) {
            return;
        }
        QaAnswer a = new QaAnswer();
        a.setQuestionId(questionId);
        a.setPartnerId(partnerId);
        a.setContent(content);
        a.setCreatedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        answerMapper.insert(a);
    }

    /**
     * 若库中题目均无任何答案，视为旧占位题，整批删除以便灌入 Suki 题。
     */
    private void removeUnansweredPlaceholderQuestions() {
        List<QaQuestion> all = questionMapper.selectList(null);
        if (all.isEmpty()) {
            return;
        }
        long answeredQuestions = answerMapper.selectList(null).stream()
                .map(QaAnswer::getQuestionId)
                .distinct()
                .count();
        if (answeredQuestions > 0) {
            return;
        }
        for (QaQuestion q : all) {
            questionMapper.deleteById(q.getId());
        }
        log.info("已清除 {} 道无答案占位题", all.size());
    }

    private void markImported() {
        Setting existing = settingMapper.selectOne(new LambdaQueryWrapper<Setting>()
                .eq(Setting::getSettingKey, SETTING_KEY));
        if (existing == null) {
            Setting s = new Setting();
            s.setSettingKey(SETTING_KEY);
            s.setSettingValue("true");
            settingMapper.insert(s);
        } else {
            existing.setSettingValue("true");
            settingMapper.updateById(existing);
        }
    }
}
