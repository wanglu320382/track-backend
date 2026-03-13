package com.track.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.track.entity.CommonSql;
import com.track.mapper.CommonSqlMapper;
import com.track.service.CommonSqlService;
import org.springframework.stereotype.Service;

/**
 * 用户常用 SQL 服务实现
 */
@Service
public class CommonSqlServiceImpl extends ServiceImpl<CommonSqlMapper, CommonSql> implements CommonSqlService {
}

