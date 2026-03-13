package com.track.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据源配置实体
 */
@Data
@TableName("datasource_config")
public class DatasourceConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    /** ORACLE/MYSQL/OCEANBASE/REDIS */
    private String type;
    private String host;
    private Integer port;
    @TableField("database_name")
    private String databaseName;
    private String username;
    private String password;
    @TableField("extra_params")
    private String extraParams;
    private Integer status;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
}
