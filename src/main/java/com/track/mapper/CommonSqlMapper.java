package com.track.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.track.entity.CommonSql;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户常用 SQL Mapper
 */
@Mapper
public interface CommonSqlMapper extends BaseMapper<CommonSql> {
}

