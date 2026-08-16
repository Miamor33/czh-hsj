package com.couple.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.couple.app.common.BusinessException;
import com.couple.app.entity.Partner;
import com.couple.app.entity.QaAnswer;
import com.couple.app.entity.QaQuestion;
import com.couple.app.entity.QaReply;
import com.couple.app.entity.Setting;
import com.couple.app.mapper.PartnerMapper;
import com.couple.app.mapper.QaAnswerMapper;
import com.couple.app.mapper.QaQuestionMapper;
import com.couple.app.mapper.QaReplyMapper;
import com.couple.app.mapper.SettingMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QaService {
    private static final String NUDGE_KEY_PREFIX = "qa_nudge_";

    private final QaQuestionMapper questionMapper;
    private final QaAnswerMapper answerMapper;
    private final QaReplyMapper replyMapper;
    private final PartnerMapper partnerMapper;
    private final SettingMapper settingMapper;

    public QaService(QaQuestionMapper questionMapper, QaAnswerMapper answerMapper,
                     QaReplyMapper replyMapper, PartnerMapper partnerMapper,
                     SettingMapper settingMapper) {
        this.questionMapper = questionMapper;
        this.answerMapper = answerMapper;
        this.replyMapper = replyMapper;
        this.partnerMapper = partnerMapper;
        this.settingMapper = settingMapper;
    }

    public List<Map<String, Object>> list(Long currentPartnerId, String filter) {
        List<Partner> partners = partnerMapper.selectList(null);
        Map<Long, Partner> partnerById = partners.stream()
                .collect(Collectors.toMap(Partner::getId, p -> p));
        Map<Long, String> names = partners.stream()
                .collect(Collectors.toMap(Partner::getId, Partner::getDisplayName));
        List<QaQuestion> questions = questionMapper.selectList(new LambdaQueryWrapper<QaQuestion>()
                .orderByDesc(QaQuestion::getId));
        List<QaQuestion> ascOrder = questionMapper.selectList(new LambdaQueryWrapper<QaQuestion>()
                .orderByAsc(QaQuestion::getId));
        Map<Long, Integer> indexById = new HashMap<>();
        for (int i = 0; i < ascOrder.size(); i++) {
            indexById.put(ascOrder.get(i).getId(), i + 1);
        }
        List<QaAnswer> answers = answerMapper.selectList(null);
        Map<Long, List<QaAnswer>> byQ = answers.stream().collect(Collectors.groupingBy(QaAnswer::getQuestionId));
        List<QaReply> allReplies = replyMapper.selectList(new LambdaQueryWrapper<QaReply>()
                .orderByAsc(QaReply::getCreatedAt)
                .orderByAsc(QaReply::getId));
        Map<Long, List<QaReply>> repliesByQ = allReplies.stream()
                .collect(Collectors.groupingBy(QaReply::getQuestionId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (QaQuestion q : questions) {
            List<QaAnswer> qAnswers = byQ.getOrDefault(q.getId(), List.of());
            Optional<QaAnswer> mine = qAnswers.stream().filter(a -> a.getPartnerId().equals(currentPartnerId)).findFirst();
            Optional<QaAnswer> other = qAnswers.stream().filter(a -> !a.getPartnerId().equals(currentPartnerId)).findFirst();
            boolean both = mine.isPresent() && other.isPresent();

            String status;
            if (mine.isEmpty()) {
                status = "pending_me";
            } else if (other.isEmpty()) {
                status = "pending_other";
            } else {
                status = "done";
            }
            if (filter != null && !filter.isBlank() && !"all".equals(filter) && !status.equals(filter)) {
                continue;
            }

            Map<String, Object> item = new HashMap<>();
            item.put("id", q.getId());
            item.put("content", q.getContent());
            item.put("questionIndex", indexById.getOrDefault(q.getId(), 0));
            item.put("status", status);
            item.put("myAnswer", mine.map(QaAnswer::getContent).orElse(null));
            if (both) {
                item.put("otherAnswer", other.map(QaAnswer::getContent).orElse(null));
                item.put("otherName", other.map(a -> names.getOrDefault(a.getPartnerId(), "对方")).orElse(null));
                item.put("replies", toReplyViews(repliesByQ.getOrDefault(q.getId(), List.of()),
                        currentPartnerId, partnerById));
            } else {
                item.put("otherAnswer", null);
                item.put("otherName", null);
                item.put("replies", List.of());
            }
            result.add(item);
        }
        return result;
    }

    public Map<String, Object> detail(Long questionId, Long currentPartnerId) {
        QaQuestion q = questionMapper.selectById(questionId);
        if (q == null) {
            throw new BusinessException("题目不存在");
        }
        List<Partner> partners = partnerMapper.selectList(null);
        Map<Long, Partner> partnerById = partners.stream()
                .collect(Collectors.toMap(Partner::getId, p -> p));

        List<QaQuestion> ordered = questionMapper.selectList(new LambdaQueryWrapper<QaQuestion>()
                .orderByAsc(QaQuestion::getId));
        int questionIndex = 1;
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getId().equals(questionId)) {
                questionIndex = i + 1;
                break;
            }
        }

        List<QaAnswer> qAnswers = answerMapper.selectList(new LambdaQueryWrapper<QaAnswer>()
                .eq(QaAnswer::getQuestionId, questionId)
                .orderByAsc(QaAnswer::getCreatedAt)
                .orderByAsc(QaAnswer::getId));
        Optional<QaAnswer> mine = qAnswers.stream()
                .filter(a -> a.getPartnerId().equals(currentPartnerId)).findFirst();
        Optional<QaAnswer> other = qAnswers.stream()
                .filter(a -> !a.getPartnerId().equals(currentPartnerId)).findFirst();
        boolean both = mine.isPresent() && other.isPresent();

        String status;
        if (mine.isEmpty()) {
            status = "pending_me";
        } else if (other.isEmpty()) {
            status = "pending_other";
        } else {
            status = "done";
        }

        List<Map<String, Object>> answerViews = new ArrayList<>();
        if (both) {
            for (QaAnswer a : qAnswers) {
                answerViews.add(toAnswerView(a, currentPartnerId, partnerById));
            }
        } else if (mine.isPresent()) {
            answerViews.add(toAnswerView(mine.get(), currentPartnerId, partnerById));
        }

        List<QaReply> replies = both
                ? replyMapper.selectList(new LambdaQueryWrapper<QaReply>()
                .eq(QaReply::getQuestionId, questionId)
                .orderByAsc(QaReply::getCreatedAt)
                .orderByAsc(QaReply::getId))
                : List.of();

        Map<String, Object> item = new HashMap<>();
        item.put("id", q.getId());
        item.put("content", q.getContent());
        item.put("createdAt", q.getCreatedAt());
        item.put("questionIndex", questionIndex);
        item.put("status", status);
        item.put("myAnswer", mine.map(QaAnswer::getContent).orElse(null));
        item.put("answers", answerViews);
        item.put("replies", toReplyViews(replies, currentPartnerId, partnerById));
        Long nudgedTarget = readNudgeTarget(questionId);
        item.put("nudgedMe", nudgedTarget != null && nudgedTarget.equals(currentPartnerId));
        item.put("alreadyNudged", nudgedTarget != null && mine.isPresent() && other.isEmpty());
        return item;
    }

    public QaQuestion addQuestion(String content, Long partnerId) {
        if (content == null || content.isBlank()) {
            throw new BusinessException("题目不能为空");
        }
        QaQuestion q = new QaQuestion();
        q.setContent(content.trim());
        q.setCreatedBy(partnerId);
        q.setCreatedAt(LocalDateTime.now());
        questionMapper.insert(q);
        return q;
    }

    public void answer(Long questionId, String content, Long partnerId) {
        if (content == null || content.isBlank()) {
            throw new BusinessException("答案不能为空");
        }
        QaQuestion q = questionMapper.selectById(questionId);
        if (q == null) {
            throw new BusinessException("题目不存在");
        }
        QaAnswer existing = answerMapper.selectOne(new LambdaQueryWrapper<QaAnswer>()
                .eq(QaAnswer::getQuestionId, questionId)
                .eq(QaAnswer::getPartnerId, partnerId));
        if (existing == null) {
            QaAnswer a = new QaAnswer();
            a.setQuestionId(questionId);
            a.setPartnerId(partnerId);
            a.setContent(content.trim());
            a.setCreatedAt(LocalDateTime.now());
            a.setUpdatedAt(LocalDateTime.now());
            answerMapper.insert(a);
        } else {
            existing.setContent(content.trim());
            existing.setUpdatedAt(LocalDateTime.now());
            answerMapper.updateById(existing);
        }
        clearNudge(questionId);
    }

    public void nudge(Long questionId, Long fromPartnerId) {
        QaQuestion q = questionMapper.selectById(questionId);
        if (q == null) {
            throw new BusinessException("题目不存在");
        }
        QaAnswer mine = answerMapper.selectOne(new LambdaQueryWrapper<QaAnswer>()
                .eq(QaAnswer::getQuestionId, questionId)
                .eq(QaAnswer::getPartnerId, fromPartnerId));
        if (mine == null) {
            throw new BusinessException("请先完成自己的回答");
        }
        List<Partner> partners = partnerMapper.selectList(null);
        Long otherId = partners.stream()
                .map(Partner::getId)
                .filter(id -> !id.equals(fromPartnerId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("对方账号不存在"));
        QaAnswer other = answerMapper.selectOne(new LambdaQueryWrapper<QaAnswer>()
                .eq(QaAnswer::getQuestionId, questionId)
                .eq(QaAnswer::getPartnerId, otherId));
        if (other != null) {
            throw new BusinessException("对方已经回答过了");
        }
        String key = nudgeKey(questionId);
        Setting existing = settingMapper.selectOne(new LambdaQueryWrapper<Setting>()
                .eq(Setting::getSettingKey, key));
        if (existing == null) {
            Setting s = new Setting();
            s.setSettingKey(key);
            s.setSettingValue(String.valueOf(otherId));
            settingMapper.insert(s);
        } else {
            existing.setSettingValue(String.valueOf(otherId));
            settingMapper.updateById(existing);
        }
    }

    public QaReply reply(Long questionId, String content, Long partnerId) {
        if (content == null || content.isBlank()) {
            throw new BusinessException("回复不能为空");
        }
        QaQuestion q = questionMapper.selectById(questionId);
        if (q == null) {
            throw new BusinessException("题目不存在");
        }
        long answerCount = answerMapper.selectCount(new LambdaQueryWrapper<QaAnswer>()
                .eq(QaAnswer::getQuestionId, questionId));
        if (answerCount < 2) {
            throw new BusinessException("双方答完后才能追聊");
        }
        QaReply r = new QaReply();
        r.setQuestionId(questionId);
        r.setPartnerId(partnerId);
        r.setContent(content.trim());
        r.setCreatedAt(LocalDateTime.now());
        replyMapper.insert(r);
        return r;
    }

    public long pendingForMe(Long partnerId) {
        List<QaQuestion> questions = questionMapper.selectList(null);
        List<QaAnswer> mine = answerMapper.selectList(new LambdaQueryWrapper<QaAnswer>()
                .eq(QaAnswer::getPartnerId, partnerId));
        Set<Long> answered = mine.stream().map(QaAnswer::getQuestionId).collect(Collectors.toSet());
        return questions.stream().filter(q -> !answered.contains(q.getId())).count();
    }

    private String nudgeKey(Long questionId) {
        return NUDGE_KEY_PREFIX + questionId;
    }

    private Long readNudgeTarget(Long questionId) {
        Setting s = settingMapper.selectOne(new LambdaQueryWrapper<Setting>()
                .eq(Setting::getSettingKey, nudgeKey(questionId)));
        if (s == null || s.getSettingValue() == null || s.getSettingValue().isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(s.getSettingValue().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void clearNudge(Long questionId) {
        Setting s = settingMapper.selectOne(new LambdaQueryWrapper<Setting>()
                .eq(Setting::getSettingKey, nudgeKey(questionId)));
        if (s != null) {
            settingMapper.deleteById(s.getId());
        }
    }

    private Map<String, Object> toAnswerView(QaAnswer a, Long currentPartnerId,
                                             Map<Long, Partner> partnerById) {
        Partner p = partnerById.get(a.getPartnerId());
        Map<String, Object> row = new HashMap<>();
        row.put("partnerKey", p != null ? p.getPartnerKey() : null);
        row.put("displayName", p != null ? p.getDisplayName() : "对方");
        row.put("content", a.getContent());
        row.put("createdAt", a.getCreatedAt());
        row.put("mine", a.getPartnerId().equals(currentPartnerId));
        return row;
    }

    private List<Map<String, Object>> toReplyViews(List<QaReply> replies, Long currentPartnerId,
                                                   Map<Long, Partner> partnerById) {
        List<Map<String, Object>> views = new ArrayList<>();
        for (QaReply r : replies) {
            Partner p = partnerById.get(r.getPartnerId());
            Map<String, Object> row = new HashMap<>();
            row.put("id", r.getId());
            row.put("partnerKey", p != null ? p.getPartnerKey() : null);
            row.put("displayName", p != null ? p.getDisplayName() : "对方");
            row.put("content", r.getContent());
            row.put("createdAt", r.getCreatedAt());
            row.put("mine", r.getPartnerId().equals(currentPartnerId));
            views.add(row);
        }
        return views;
    }
}
