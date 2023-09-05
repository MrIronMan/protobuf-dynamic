package com.github.os72.protobuf.dynamic;

import io.protostuff.CollectionSchema;
import io.protostuff.CollectionSchema.MessageFactories;
import io.protostuff.LinkedBuffer;
import io.protostuff.MapSchema;
import io.protostuff.MessageCollectionSchema;
import io.protostuff.MessageMapSchema;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.DefaultIdStrategy;
import io.protostuff.runtime.IdStrategy;
import io.protostuff.runtime.RuntimeSchema;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ironman
 * @date 2023/7/5 23:00
 * @desc
 */
public class ProtostuffUtils {
    private static Logger logger = LoggerFactory.getLogger(ProtostuffUtils.class);

    private static final String PACKAGE_KEYWORD = "java.util";

    /**
     * 安全缓存区，key 和 Schema 对象
     */
    private static final Map<String, Schema<?>> CACHE_SCHEMA = new ConcurrentHashMap<>();

    private static Schema findSchema(Type type) throws ClassNotFoundException {
        String cacheKey = generateCacheKey(type);
        Schema<?> schema = CACHE_SCHEMA.get(cacheKey);
        if (schema != null) {
            return schema;
        }
        schema = buildSchema(type);
        CACHE_SCHEMA.put(cacheKey, schema);
        return schema;
    }

    private static Schema buildSchema(Type type) throws ClassNotFoundException {
        if (!(type instanceof ParameterizedType)) {
            // 修复列表为空的问题，参考 issue：https://github.com/protostuff/protostuff/issues/324
            IdStrategy strategy = new DefaultIdStrategy(IdStrategy.DEFAULT_FLAGS | IdStrategy.COLLECTION_SCHEMA_ON_REPEATED_FIELDS,null,0);
            return RuntimeSchema.createFrom(Class.forName(type.getTypeName()), strategy);
        }
        String selfTypeName = ((ParameterizedType) type).getRawType().getTypeName();
        String classSimpleName = StringUtils.substring(selfTypeName, selfTypeName.lastIndexOf(".") + 1);
        Type[] arguments = ((ParameterizedType) type).getActualTypeArguments();
        if (selfTypeName.startsWith(PACKAGE_KEYWORD) && CollectionSchema.MessageFactories.accept(classSimpleName)) {
            if (arguments == null || arguments.length != 1) {
                throw new IllegalArgumentException("Collection parameter type is missing");
            }
            return new MessageCollectionSchema(buildSchema(arguments[0]), false);
        } else if (selfTypeName.startsWith(PACKAGE_KEYWORD) && MapSchema.MessageFactories.accept(classSimpleName)) {
            if (arguments == null || arguments.length != 2) {
                throw new IllegalArgumentException("Map parameter type is missing");
            }
            Schema keySchema = buildSchema(arguments[0]);
            Schema valueSchema = buildSchema(arguments[1]);
            return new MessageMapSchema(keySchema, valueSchema);
        } else {
            return RuntimeSchema.createFrom(Class.forName(type.getTypeName()));
        }
    }

    private static String generateCacheKey(Type type) {
        if (!(type instanceof ParameterizedType)) {
            return type.getTypeName();
        }
        String selfTypeName = ((ParameterizedType) type).getRawType().getTypeName();
        String classSimpleName = StringUtils.substring(selfTypeName, selfTypeName.lastIndexOf(".") + 1);
        Type[] arguments = ((ParameterizedType) type).getActualTypeArguments();
        if (selfTypeName.startsWith(PACKAGE_KEYWORD) && CollectionSchema.MessageFactories.accept(classSimpleName)) {
            if (arguments == null || arguments.length != 1) {
                throw new IllegalArgumentException("Collection parameter type is missing");
            }
            return selfTypeName + ":" + generateCacheKey(arguments[0]);
        } else if (selfTypeName.startsWith(PACKAGE_KEYWORD) && MapSchema.MessageFactories.accept(classSimpleName)) {
            if (arguments == null || arguments.length != 2) {
                throw new IllegalArgumentException("Map parameter type is missing");
            }
            String keyName = generateCacheKey(arguments[0]);
            String valueName = generateCacheKey(arguments[1]);
            return selfTypeName + ":" + keyName + ":" + valueName;
        } else {
            throw new UnsupportedOperationException("Unknown type " + type.toString());
        }

    }

    public static <T> byte[] serializer(T obj, Type type) {
        //设置缓数组缓冲区
        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);

        byte[] bytes = null;
        try {
            //获取序列化对象
            Object serializerObj = obj;
            //获取Schema对象
            Schema schema = findSchema(type);
            //将对象转换为字节流
            bytes = ProtostuffIOUtil.toByteArray(serializerObj, schema, buffer);
        } catch (Exception e) {
            logger.info("序列化{}失败", obj, e);
            throw new IllegalStateException(e.getMessage(), e);
        } finally {
            //回收buffer
            buffer.clear();
        }
        return bytes;
    }

    public static <T> T deserialize(byte[] data, Type type) {
        try {
            Schema schema = findSchema(type);

            Object message = newMessage(type, schema);
            ProtostuffIOUtil.mergeFrom(data, message, schema);
            return (T) message;
        } catch (Exception e) {
            logger.error("反序列化对象异常 [" + type.toString() + "]", e);
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private static <T> T newMessage(Type type, Schema<T> schema) throws ClassNotFoundException {
        if (!(type instanceof ParameterizedType)) {
            return schema.newMessage();
        }
        String selfTypeName = ((ParameterizedType) type).getRawType().getTypeName();
        String classSimpleName = StringUtils.substring(selfTypeName, selfTypeName.lastIndexOf(".") + 1);
        if (selfTypeName.startsWith(PACKAGE_KEYWORD) && CollectionSchema.MessageFactories.accept(classSimpleName)) {
            return (T) MessageFactories.getFactory(classSimpleName).newMessage();
        } else if (selfTypeName.startsWith(PACKAGE_KEYWORD) && MapSchema.MessageFactories.accept(classSimpleName)) {
            return (T) MapSchema.MessageFactories.getFactory(classSimpleName).newMessage();
        } else {
            throw new UnsupportedOperationException("Unknown type " + type);
        }
    }
}
