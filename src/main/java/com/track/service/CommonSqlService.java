/*
 * CommonSqlService.java
 * 问题溯源系统 - 常用 SQL 管理服务接口
 */
package com.track.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.track.entity.CommonSql;

/**
 * 常用 SQL 服务接口。
 * <p>
 * 对常用 SQL 实体进行 CRUD，供前端快捷执行与管理。
 * </p>
 *
 * @see com.track.service.impl.CommonSqlServiceImpl
 */
public interface CommonSqlService extends IService<CommonSql> {
}

