package com.github.os72.protobuf.dynamic;

/**
 * @author ironman
 * @date 2023/1/31 21:03
 * @desc
 */
public class Wrapper <T> {

    private T value;

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}
