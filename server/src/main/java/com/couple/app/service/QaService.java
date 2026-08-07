package com.couple.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.couple.app.common.BusinessException;
import com.couple.app.entity.Partner;
import com.couple.app.entity.QaAnswer;
import com.couple.app.entity.QaQuestion;
import com.couple.app.mapper.PartnerMapper;
import com.couple.app.mapper.QaAnswerMapper;
import com.couple.app.mapper.QaQuestionMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QaService {
    private final QaQuestionMapper questionMapper;
    private final QaAnswerMapper answerMapper;
    private final PartnerMapper partnerMapper;

    public QaService(QaQuestionMapper questionMapper, QaAnswerMapper answerMapper, PartnerMapper partnerMapper) {
        this.questionMapper = questionMapper;
        this.answerMapper = answerMapper;
        this.partnerMapper = partnerMapper;
    }

    public List<Map<String, Object>> list(Long currentPartnerId, String filter) {
        List<Partner> partners = partnerMapper.selectList(null);
        Map<Long, String> names = partners.stream()
                .collect(Collectors.toMap(Partner::getId, Partner::getDisplayName));
        List<QaQuestion> questions = questionMapper.selectList(new LambdaQueryWrapper<QaQuestion>()
                .orderByDesc(QaQuestion::getId));
        List<QaAnswer> answers = answerMapper.selectList(null);
        Map<Long, List<QaAnswer>> byQ = answers.stream().collect(Collectors.groupingBy(QaAnswer::getQuestionId));

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
            item.put("status", status);
            item.put("myAnswer", mine.map(QaAnswer::getContent).orElse(null));
            if (both) {
                item.put("otherAnswer", other.map(QaAnswer::getContent).orElse(null));
                item.put("otherName", other.map(a -> names.getOrDefault(a.getPartnerId(), "对方")).orElse(null));
            } else {
                item.put("otherAnswer", null);
                item.put("otherName", null);
            }
            result.add(item);
        }
        return result;
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
    }

    public long pendingForMe(Long partnerId) {
        List<QaQuestion> questions = questionMapper.selectList(null);
        List<QaAnswer> mine = answerMapper.selectList(new LambdaQueryWrapper<QaAnswer>()
                .eq(QaAnswer::getPartnerId, partnerId));
        Set<Long> answered = mine.stream().map(QaAnswer::getQuestionId).collect(Collectors.toSet());
        return questions.stream().filter(q -> !answered.contains(q.getId())).count();
    }
}
