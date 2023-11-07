package com.github.os72.protobuf.dynamic;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.DynamicMessage;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

/**
 * @author ironman
 * @date 2023/5/18 22:39
 *
 * 本工具仅用于快速进行解码，排查问题，没有进行过性能方面的考虑，请勿在生产环境直接调用
 */
public class CodecUtils {

    public enum ASCIIControlStr {
        /**
         * 空字符（Null）
         */
        Null("\\\\0", "\\\\x00"),
        /**
         * 响铃（Bell）
         */
        Bell("\\\\a", "\\\\x07"),
        /**
         * 退格（Backspace）
         */
        Backspace("\\\\b", "\\\\x08"),
        /**
         * 水平制表符（Horizontal Tab）
         */
        HorizontalTab("\\\\t", "\\\\x09"),
        /**
         * 换行（Line Feed）
         */
        LineFeed("\\\\n", "\\\\x0A"),
        /**
         * 垂直制表符（Vertical Tab）
         */
        VerticalTab("\\\\v", "\\\\x0B"),
        /**
         * 换页（Form Feed）
         */
        FormFeed("\\\\f", "\\\\x0C"),
        /**
         * 回车（Carriage Return）
         */
        CarriageReturn("\\\\r", "\\\\x0D"),
        ;
        private String str;
        private String hexStr;

        ASCIIControlStr(String str, String hexStr) {
            this.str = str;
            this.hexStr = hexStr;
        }

        public String getStr() {
            return str;
        }

        public String getHexStr() {
            return hexStr;
        }
    }

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
    public static void customDecodeWithRedisString(String encodeStr, Type type) {
        String temp = encodeStr.replaceAll("\\\\x", "%");
        String realData = URLDecoder.decode(temp, "ISO-8859-1");
        byte[] realDataBytes = realData.getBytes(StandardCharsets.ISO_8859_1);

        customDecodeWithBytes(realDataBytes, type);
    }

    @SneakyThrows
    public static void customDecodeWithBytes(byte[] bytes, Type type) {
        MessageCodec.HandleWrapper handleWrapper = MessageCodec.buildSchemaV2(type);
        Descriptor descriptor = handleWrapper.getSchema().getMessageDescriptor(handleWrapper.getTopTypeName());
        DynamicMessage dynamicMessage = DynamicMessage.parseFrom(descriptor, bytes);
        Object object = MessageCodec.parseObject(dynamicMessage, type);
        System.out.println(JSON.toJSONString(object));
    }

    /**
     * 对直接使用 protostuff 的2进制的16进制字符串进行解码处理
     *
     * @param encodeStr
     * @param type
     */
    @SneakyThrows
    public static void decodeProto(String encodeStr, Type type) {
        String temp = encodeStr.replaceAll("\\\\x", "%");
        String realData = URLDecoder.decode(temp, "ISO-8859-1");
        byte[] realDataBytes = realData.getBytes(StandardCharsets.ISO_8859_1);

        Object deserialize = ProtostuffUtils.deserialize(realDataBytes, type);
        System.out.println(JSON.toJSONString(deserialize));
    }

    /**
     * 解码从 redis-cli 中获取的数据，进行了 16 进制编码处理，但是对于对于 \b(back) \n(enter) 没有进行编码，需要进行替换
     * 例如：（以下数据的类型为：ResourcesAlbum）
     *  原始数据：\x0b\b\xe2\n\x10\x01\x18\x02 \x01(\x011\xf0-\xca\x94w\x01\x00\x009\x00\xf1H\xc9x\x01\x00\x00\x0c\x0b\b\xca+\x10\x01\x18\x01 \x01(\x011\xb8\x9eO\x02y\x01\x00\x009\xb8\x9eO\x02y\x01\x00\x00\x0c\x0b\b\xc5F\x10\x01\x18\xc2\x0b \xff\xff\xff\xff\xff\xff\xff\xff\xff\x01(\x011\xe0R\x12\xfcv\x01\x00\x009PbVH{\x01\x00\x00\x0c\x0b\b\xc6F\x10\x01\x18\xc3\x0b \x02(\x011\xe0R\x12\xfcv\x01\x00\x009PbVH{\x01\x00\x00\x0c\x0b\b\xc7F\x10\x01\x18\xc4\x0b \x03(\x011\xe0R\x12\xfcv\x01\x00\x009PbVH{\x01\x00\x00\x0c\x0b\b\xc8F\x10\x01\x18\xc5\x0b \x04(\x011\xe0R\x12\xfcv\x01\x00\x009PbVH{\x01\x00\x00\x0c
     *  格式数据：\x0b\x08\xe2\x0A\x10\x01\x18\x02 \x01(\x011\xf0-\xca\x94w\x01\x00\x009\x00\xf1H\xc9x\x01\x00\x00\x0c\x0b\x08\xca+\x10\x01\x18\x01 \x01(\x011\xb8\x9eO\x02y\x01\x00\x009\xb8\x9eO\x02y\x01\x00\x00\x0c\x0b\x08\xc5F\x10\x01\x18\xc2\x0b \xff\xff\xff\xff\xff\xff\xff\xff\xff\x01(\x011\xe0R\x12\xfcv\x01\x00\x009PbVH{\x01\x00\x00\x0c\x0b\x08\xc6F\x10\x01\x18\xc3\x0b \x02(\x011\xe0R\x12\xfcv\x01\x00\x009PbVH{\x01\x00\x00\x0c\x0b\x08\xc7F\x10\x01\x18\xc4\x0b \x03(\x011\xe0R\x12\xfcv\x01\x00\x009PbVH{\x01\x00\x00\x0c\x0b\x08\xc8F\x10\x01\x18\xc5\x0b \x04(\x011\xe0R\x12\xfcv\x01\x00\x009PbVH{\x01\x00\x00\x0c
     * @param encodeStr
     * @param type
     */
    public static void decodeProtoWithCli(String encodeStr, Type type) {
        String realData = encodeStr;
        for (ASCIIControlStr value : ASCIIControlStr.values()) {
            if (!realData.contains(value.getStr())) {
                continue;
            }
            realData = realData.replaceAll(value.getStr(), value.getHexStr());
        }
        decodeProto(realData, type);
    }


    /**
     * 用于解析获取到的 16 进制字符串
     *
     * @param encodeStr
     * @param type
     */
    public static void decodeWithHex(String encodeStr, Type type) {
        byte[] bytes = string2Byte(encodeStr);
        String hexString = bytes2HexString(bytes);
        Object deserialize = ProtostuffUtils.deserialize(bytes, type);
        System.out.println(JSON.toJSONString(deserialize));
    }

    /**
     *
     * @param encodeStr
     * @return
     */
    private static byte[] string2Byte(String encodeStr) {
        String[] parts = encodeStr.split("\\\\x");
        byte[] binaryData = new byte[encodeStr.length() * 2];

        int len = 0;
        for (String temp : parts) {
            if (StringUtils.isEmpty(temp)) {
                continue;
            }
            // 前两位为16进制表示，截取两位
            String start = temp.substring(0, 2);
            binaryData[len++] = (byte) Integer.parseInt(start, 16);

            // 后面的为字符串，需要将每一位都转为 16 进制
            String end = temp.substring(2);
            if (StringUtils.isEmpty(end)) {
                continue;
            }
            String[] splits = end.split("");
            for (String str : splits) {
                binaryData[len++] = (byte) Integer.parseInt(bytes2HexString(str.getBytes(StandardCharsets.UTF_8)), 16);
            }
        }
        byte[] res = new byte[len];
        System.arraycopy(binaryData, 0, res, 0, len--);

        return res;
    }

    /**
     * 将字节数组转换为16进制字符表示
     * @param bytes
     * @return
     */
    private static String bytes2HexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

}
