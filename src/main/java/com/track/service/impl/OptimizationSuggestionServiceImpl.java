package com.track.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.track.entity.OptimizationSuggestion;
import com.track.mapper.OptimizationSuggestionMapper;
import com.track.service.OptimizationSuggestionService;
import org.springframework.stereotype.Service;

@Service
public class OptimizationSuggestionServiceImpl
        extends ServiceImpl<OptimizationSuggestionMapper, OptimizationSuggestion>
        implements OptimizationSuggestionService {
}
