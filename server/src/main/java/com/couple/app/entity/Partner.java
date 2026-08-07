package com.couple.app.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("partner")
public class Partner {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String partnerKey;
    private String displayName;
    private String passwordHash;
    private LocalDateTime createdAt;
}
