package com.track.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 优化建议
 */
@Data
@TableName("optimization_suggestion")
public class OptimizationSuggestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    private String title;

    @TableField("pain_point")
    private String painPoint;

    @TableField("improvement_suggestion")
    private String improvementSuggestion;

    private String proposer;

    /** 审核中 / 开发中 / 已上线 / 不优化 */
    private String status;

    /** 提出时间 */
    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
