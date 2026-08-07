package com.couple.app.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("qa_question")
public class QaQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String content;
    private Long createdBy;
    private LocalDateTime createdAt;
}
