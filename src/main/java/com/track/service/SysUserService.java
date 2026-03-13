/*
 * SysUserService.java
 * 问题溯源系统 - 系统用户服务接口（用户管理、登录校验）
 */
package com.track.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.track.entity.SysUser;

/**
 * 系统用户服务接口。
 * <p>
 * 系统用户 CRUD 及登录校验（明文密码 MD5 比对）。
 * </p>
 *
 * @see com.track.service.impl.SysUserServiceImpl
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 用户登录校验：根据用户名与明文密码验证，返回登录用户。
     *
     * @param username       登录用户名
     * @param passwordPlain  明文密码
     * @return 登录成功的用户实体
     * @throws IllegalArgumentException 用户不存在、已禁用或密码错误时抛出
     */
    SysUser login(String username, String passwordPlain);
}

