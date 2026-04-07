CREATE TABLE IF NOT EXISTS datasource_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL COMMENT '数据源名称',
  type VARCHAR(20) NOT NULL COMMENT 'ORACLE/MYSQL/OCEANBASE/REDIS',
  host VARCHAR(100) COMMENT '主机地址',
  port INT COMMENT '端口',
  database_name VARCHAR(100) COMMENT '数据库名/服务名',
  oracle_connect_mode VARCHAR(20) DEFAULT NULL COMMENT 'Oracle: SID / SERVICE_NAME',
  username VARCHAR(100) COMMENT '用户名',
  password VARCHAR(255) COMMENT '密码',
  extra_params VARCHAR(500) COMMENT '额外连接参数',
  status TINYINT DEFAULT 1 COMMENT '1启用 0禁用',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源配置表';

-- 已有库升级：ALTER TABLE datasource_config ADD COLUMN oracle_connect_mode VARCHAR(20) DEFAULT NULL COMMENT 'Oracle: SID / SERVICE_NAME' AFTER database_name;

-- 系统用户表
CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE COMMENT '登录用户名',
  password_md5 VARCHAR(32) NOT NULL COMMENT '登录密码MD5',
  nickname VARCHAR(100) COMMENT '显示昵称',
  role VARCHAR(20) NOT NULL DEFAULT 'ADMIN' COMMENT '角色：ADMIN/USER',
  status TINYINT DEFAULT 1 COMMENT '1启用 0禁用',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 默认管理员：admin / admin
INSERT INTO sys_user (username, password_md5, nickname, role, status)
VALUES ('admin', '21232f297a57a5a743894a0e4a801fc3', '系统管理员', 'ADMIN', 1)
ON DUPLICATE KEY UPDATE username = username;

-- 用户常用 SQL 表
CREATE TABLE IF NOT EXISTS common_sql (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '所属用户ID',
  datasource_id BIGINT COMMENT '数据源ID，用于与数据源联动',
  title VARCHAR(100) NOT NULL COMMENT '标题',
  sql_text TEXT NOT NULL COMMENT 'SQL 内容',
  description VARCHAR(255) COMMENT '描述',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_id (user_id),
  INDEX idx_datasource_id (datasource_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户常用 SQL';

-- 若表已存在，请执行：ALTER TABLE common_sql ADD COLUMN datasource_id BIGINT COMMENT '数据源ID' AFTER user_id;

-- 优化建议
CREATE TABLE IF NOT EXISTS optimization_suggestion (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '所属用户ID',
  title VARCHAR(200) NOT NULL COMMENT '标题',
  pain_point TEXT NOT NULL COMMENT '痛点',
  improvement_suggestion TEXT NOT NULL COMMENT '改善建议',
  proposer VARCHAR(100) NOT NULL COMMENT '提出人',
  status VARCHAR(20) NOT NULL DEFAULT '审核中' COMMENT '状态：审核中/开发中/已上线/不优化',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提出时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_id (user_id),
  INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优化建议';
