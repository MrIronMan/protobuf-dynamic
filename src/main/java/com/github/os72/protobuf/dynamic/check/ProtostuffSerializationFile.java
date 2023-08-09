package com.github.os72.protobuf.dynamic.check;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * @author ironman
 * @date 2023/8/9 21:01
 * @desc
 */

@Data
@Builder
public class ProtostuffSerializationFile {

    /**
     * 时间戳
     */
    private String version;

    private List<ProtostuffSerializationSchema> schemas;

    public ProtostuffSerializationFile() {
    }

    public ProtostuffSerializationFile(String version,
        List<ProtostuffSerializationSchema> schemas) {
        this.version = version;
        this.schemas = schemas;
    }
}
