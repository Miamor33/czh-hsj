package com.couple.app.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("challenge_module")
public class ChallengeModule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String moduleKey;
    private String title;
    private Integer targetCount;
    private Integer sortOrder;
}
