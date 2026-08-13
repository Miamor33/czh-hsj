package com.couple.app.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("challenge_completion_photo")
public class ChallengeCompletionPhoto {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long completionId;
    private String fileName;
    private Integer sortOrder;
}
