package com.track.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.track.entity.CommonStat;
import com.track.mapper.CommonStatMapper;
import com.track.service.CommonStatService;
import org.springframework.stereotype.Service;

/**
 * 用户常用查询服务实现
 */
@Service
public class CommonStatServiceImpl extends ServiceImpl<CommonStatMapper, CommonStat> implements CommonStatService {
}
