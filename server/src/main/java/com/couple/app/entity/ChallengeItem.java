package com.couple.app.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("challenge_item")
public class ChallengeItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long moduleId;
    private String title;
    private Integer sortOrder;
    private String extraHint;
}
