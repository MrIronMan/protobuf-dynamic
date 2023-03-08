package com.github.os72.protobuf.dynamic.codec.model;

/**
 * @author ironman
 * @date 2022/12/21 21:28
 * @desc
 */
public enum UserType {
    NORMAL(1, "normal");

    private Integer type;

    private String info;

    UserType(Integer type, String info) {
        this.type = type;
        this.info = info;
    }
}
