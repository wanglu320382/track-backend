package com.track.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.track.common.Result;
import com.track.entity.SysUser;
import com.track.service.SysUserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 系统用户管理控制器（REST API 入口）。
 * <p>
 * 提供系统用户的增删改查、重置密码等接口。
 * </p>
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final SysUserService sysUserService;

    /**
     * 获取系统用户列表（按 ID 升序）。
     *
     * @return 用户列表
     */
    @GetMapping("/list")
    public Result<List<SysUser>> list() {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysUser::getId);
        return Result.success(sysUserService.list(wrapper));
    }

    /**
     * 新增系统用户（密码会 MD5 存储）。
     *
     * @param request 用户名、密码、昵称、角色、状态
     * @return 新增用户 ID
     */
    @PostMapping
    public Result<Long> create(@RequestBody CreateUserRequest request) {
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setNickname(request.getNickname());
        user.setRole(request.getRole());
        user.setStatus(request.getStatus());
        String md5 = DigestUtils.md5DigestAsHex(request.getPassword().getBytes(StandardCharsets.UTF_8));
        user.setPasswordMd5(md5);
        sysUserService.save(user);
        return Result.success(user.getId());
    }

    /**
     * 更新系统用户信息（昵称、角色、状态；不修改密码）。
     *
     * @param request 用户 ID、昵称、角色、状态
     * @return 无内容
     */
    @PutMapping
    public Result<Void> update(@RequestBody UpdateUserRequest request) {
        SysUser user = sysUserService.getById(request.getId());
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setNickname(request.getNickname());
        user.setRole(request.getRole());
        user.setStatus(request.getStatus());
        sysUserService.updateById(user);
        return Result.success(null);
    }

    /**
     * 重置指定用户的密码（新密码会 MD5 存储）。
     *
     * @param id      用户 ID
     * @param request 新密码
     * @return 无内容
     */
    @PostMapping("/resetPassword/{id}")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody ResetPasswordRequest request) {
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        String md5 = DigestUtils.md5DigestAsHex(request.getPassword().getBytes(StandardCharsets.UTF_8));
        user.setPasswordMd5(md5);
        sysUserService.updateById(user);
        return Result.success(null);
    }

    /**
     * 根据 ID 删除系统用户。
     *
     * @param id 用户 ID
     * @return 无内容
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysUserService.removeById(id);
        return Result.success(null);
    }

    /** 新增用户请求体 */
    @Data
    public static class CreateUserRequest {
        private String username;
        private String password;
        private String nickname;
        private String role;
        private Integer status;
    }

    /** 更新用户请求体（不包含密码） */
    @Data
    public static class UpdateUserRequest {
        private Long id;
        private String nickname;
        private String role;
        private Integer status;
    }

    /** 重置密码请求体 */
    @Data
    public static class ResetPasswordRequest {
        private String password;
    }
}

