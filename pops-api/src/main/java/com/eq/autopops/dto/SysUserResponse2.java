package com.eq.autopops.dto;

import com.eq.autopops.domain.SysUser;
import com.eq.autopops.process.AutoProperty;

/**
 * create time 2025/11/20 22:26
 * 文件说明
 *
 * @author xuejiaming
 */
@AutoProperty(value = SysUser.class,excludes = "name")
public class SysUserResponse2 {
}
