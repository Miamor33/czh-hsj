package com.couple.app.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("challenge_completion")
public class ChallengeCompletion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long itemId;
    private String note;
    private String photoFile;
    private Long completedBy;
    private LocalDateTime completedAt;
}
