package com.couple.app.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("anniversary")
public class Anniversary {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private LocalDate eventDate;
    private Boolean yearly;
    private Long createdBy;
    private LocalDateTime createdAt;
}
