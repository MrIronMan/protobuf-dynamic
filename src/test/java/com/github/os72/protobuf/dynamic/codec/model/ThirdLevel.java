package com.github.os72.protobuf.dynamic.codec.model;

/**
 * @author ironman
 * @date 2023/3/8 10:17
 * @desc
 */
public class ThirdLevel {

    private Integer id = 1;

    private String info = "info";

    private NestedUser nestedUser = new NestedUser();

    public static ThirdLevel build() {
        return new ThirdLevel();
    }

}
