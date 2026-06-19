package com.example.common.enums;

public enum RoleEnum {
    ADMIN,
    USER;

    /**
     * 获取角色中文标签（用于前端显示）
     */
    public String getCname() {
        return switch (this) {
            case ADMIN -> "管理员";
            case USER -> "普通用户";
        };
    }
}
