package com.github.os72.protobuf.dynamic.check;

import lombok.Builder;
import lombok.Data;

/**
 * @author ironman
 * @date 2023/7/27 21:30
 * @desc
 */
@Data
@Builder
public class ProtostuffSerializationSchema {

    private String fieldName;

    private String fieldType;

    private Integer fieldIndex;

    public ProtostuffSerializationSchema() {
    }

    public ProtostuffSerializationSchema(String fieldName, String fieldType, Integer fieldIndex) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.fieldIndex = fieldIndex;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public Integer getFieldIndex() {
        return fieldIndex;
    }

    public void setFieldIndex(Integer fieldIndex) {
        this.fieldIndex = fieldIndex;
    }
}
