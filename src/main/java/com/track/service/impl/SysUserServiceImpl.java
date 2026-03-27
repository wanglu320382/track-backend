package com.track.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.track.entity.SysUser;
import com.track.mapper.SysUserMapper;
import com.track.service.SysUserService;
import com.track.util.StatCryptoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * 系统用户服务实现
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {
    private final StatCryptoUtil statCryptoUtil;

    @Override
    public SysUser login(String username, String passwordCipher) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        SysUser user = getOne(wrapper, false);
        if (user == null || user.getStatus() != null && user.getStatus() == 0) {
            throw new IllegalArgumentException("用户不存在或已禁用");
        }
        String passwordPlain = statCryptoUtil.decrypt(passwordCipher);
        String md5 = DigestUtils.md5DigestAsHex(passwordPlain.getBytes(StandardCharsets.UTF_8));
        if (!md5.equalsIgnoreCase(user.getPasswordMd5())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        return user;
    }
}

