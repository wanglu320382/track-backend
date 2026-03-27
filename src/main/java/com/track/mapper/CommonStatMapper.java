package com.track.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.track.entity.CommonStat;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户常用查询 Mapper
 */
@Mapper
public interface CommonStatMapper extends BaseMapper<CommonStat> {
}
