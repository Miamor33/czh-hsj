package com.couple.app.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("qa_reply")
public class QaReply {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long questionId;
    private Long partnerId;
    private String content;
    private LocalDateTime createdAt;
}
