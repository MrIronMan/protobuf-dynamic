package com.github.os72.protobuf.dynamic;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.DynamicMessage;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;

/**
 * @author ironman
 * @date 2023/5/18 22:39
 *
 * 本工具仅用于快速进行解码，排查问题，没有进行过性能方面的考虑，请勿在生产环境直接调用
 */
public class CodecUtils {

    private CodecUtils() {
        throw new UnsupportedOperationException(":( bad boy～");
    }

    /**
     *
     * @param encodeStr 从 redis 中取出来的数据
     * @param type 对应方法的返回值类型，例如：
     *             1. <code> Case.class.getDeclaredMethod("getValueFromDB", Long.class)< /code> 通过类获取对应的方法然后回去方法的返回值类型 method.getGenericReturnType()
     *             2. <code> new TypeToken<List<User>>(){}.getType() </code> 使用 com.google.common.reflect.TypeToken 的工具
     */
    @SneakyThrows
    public static void decodeWithRedisString(String encodeStr, Type type) {
        String temp = encodeStr.replaceAll("\\\\x", "%");
        String realData = URLDecoder.decode(temp, "ISO-8859-1");
        byte[] realDataBytes = realData.getBytes(StandardCharsets.ISO_8859_1);

        decodeWithBytes(realDataBytes, type);
    }

    @SneakyThrows
    public static void decodeWithBytes(byte[] bytes, Type type) {
        MessageCodec.HandleWrapper handleWrapper = MessageCodec.buildSchemaV2(type);
        Descriptor descriptor = handleWrapper.getSchema().getMessageDescriptor(handleWrapper.getTopTypeName());
        DynamicMessage dynamicMessage = DynamicMessage.parseFrom(descriptor, bytes);
        Object object = MessageCodec.parseObject(dynamicMessage, type);
        System.out.println(JSON.toJSONString(object));
    }

}
