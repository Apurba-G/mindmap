package com.mindmap.auth.entity;

/**
 * User authorization roles in MindMap platform.
 */
public enum Role {
    USER,
    ADMIN;

    public String getAuthority() {
        return "ROLE_" + name();
    }
}
