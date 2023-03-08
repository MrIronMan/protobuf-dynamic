package com.github.os72.protobuf.dynamic;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.github.os72.protobuf.dynamic.MessageDefinition.Builder;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ListValue;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Parser;
import com.google.protobuf.util.JsonFormat.Printer;
import com.google.protobuf.util.Structs;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

/**
 * @author ironman
 * @date 2022/12/9 09:20
 * @desc
 */

@Slf4j
public class MessageCodec {

    /**
     * 默认前缀
     */
    private static final String DEFAULT_PREFIX = "com.github";

    private static Map<String, String> TYPE_MAPPING = new HashMap<>(32);

    // FIXME: 2022/12/26 需要把该对象放在流程中去
    private static Map<String, MessageDefinition> CUSTOM_TYPE_MAPPING = new ConcurrentHashMap<>(256);

    /**
     * 处理缓存
     */
    private static Map<Type, HandleWrapper> CACHE = new ConcurrentHashMap<>(512);

    private static boolean cacheable = true;

    private static Map<String, String> CONTAINER_TYPE_MAPPING = new HashMap<>(16);

    static {
        TYPE_MAPPING.put("int", "int32");
        TYPE_MAPPING.put("float", "float");
        TYPE_MAPPING.put("double", "double");
        TYPE_MAPPING.put("long", "int64");
        TYPE_MAPPING.put("byte", "bytes");
        TYPE_MAPPING.put("boolean", "bool");
        TYPE_MAPPING.put("char", "string");

        TYPE_MAPPING.put("java.lang.Integer", "int32");
        TYPE_MAPPING.put("java.lang.Float", "float");
        TYPE_MAPPING.put("java.lang.Double", "double");
        TYPE_MAPPING.put("java.lang.Byte", "bytes");
        TYPE_MAPPING.put("java.lang.Long", "int64");
        TYPE_MAPPING.put("java.lang.Character", "string");
        TYPE_MAPPING.put("java.lang.String", "string");
        TYPE_MAPPING.put("java.lang.Enum", "string");
        TYPE_MAPPING.put("java.lang.Boolean", "bool");

        CONTAINER_TYPE_MAPPING.put("java.util.Collection", "java.util.Collection");
        CONTAINER_TYPE_MAPPING.put("java.util.Map", "java.util.Map");
    }

    public static class HandleWrapper {

        private String name;
        private DynamicSchema schema;
        private String topTypeName;

        public String getTopTypeName() {
            return topTypeName;
        }

        public void setTopTypeName(String topTypeName) {
            this.topTypeName = topTypeName;
        }

        public HandleWrapper() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public DynamicSchema getSchema() {
            return schema;
        }

        public void setSchema(DynamicSchema schema) {
            this.schema = schema;
        }
    }

    @SuppressWarnings("unchecked")
    public static DynamicMessage buildMessage(HandleWrapper wrapper, Object value) {
        if (value instanceof List) {
            Map<String, List> map = new HashMap<>();
            map.put("dataList", (List) value);
            value = map;
        }
        DynamicSchema schema = wrapper.getSchema();
        DynamicMessage.Builder msgBuilder = schema.newMessageBuilder(wrapper.getTopTypeName());
        try {
            JsonFormat.parser().merge(JSON.toJSONString(value, SerializerFeature.DisableCircularReferenceDetect), msgBuilder);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        return msgBuilder.build();
    }

    public static Object parseObject(DynamicMessage dynamicMessage, Type valueType) {
        String data;
        try {
            data = JsonFormat.printer().print(dynamicMessage);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        if (StringUtils.contains(data, "dataList")) {
            String realData = data.substring(StringUtils.indexOf(data, "[{"), data.length() - 1);
            return JSONObject.parseObject(realData, valueType);
        }
        return JSON.parseObject(data, valueType);
    }

    private static void loadMapDef(DynamicSchema.Builder schemaBuilder) {
        // FIXME: 2022/12/10 暂时先只支持 String 类型的 Map
        MessageDefinition mapDef = MessageDefinition.newBuilder("Map")
            .addField("required", "string", "key", 1)
            .addField("required", "string", "value", 2)
            .buildWithMap();
        schemaBuilder.addMessageDefinition(mapDef);
    }

    private static Parser PARSE = JsonFormat.parser().ignoringUnknownFields();

    private static Printer PRINTER = JsonFormat.printer();

    public static Message fromJson(String json) throws IOException {
        Struct.Builder structBuilder = Struct.newBuilder();
        PARSE.merge(json, structBuilder);
        return structBuilder.build();
    }

    public static String toJson(MessageOrBuilder messageOrBuilder) throws IOException {
        return PRINTER.print(messageOrBuilder);
    }

    public static ListValue fromJsonList(String json) throws InvalidProtocolBufferException {
        ListValue.Builder builder = ListValue.newBuilder();
        PARSE.merge(json, builder);
        return builder.build();
    }

    private static final List<String> ILLEGAL_SYMBOL_LIST = Arrays.asList(".", "$");

    private static String parseFieldName(String source) {
        if (source == null) {
            return null;
        }
        for (String symbol : ILLEGAL_SYMBOL_LIST) {
            source = StringUtils.replace(source, symbol, "");
        }
        return source;
    }

    public static void generateSchema(String valueTypeName, DynamicSchema.Builder schemaBuilder, DynamicBuildContext context)
        throws ClassNotFoundException {
        // 如果已经正在构建的列表中包含自己则线返回，避免递归
        if (context.getBuildingList().contains(valueTypeName)) {
            context.getNeedBuildList().add(valueTypeName);
            return;
        } else {
            // 将自己添加至正在构建的列表中，便于进行循环检测
            context.getBuildingList().add(valueTypeName);
        }
        Class<?> targetClass = Class.forName(valueTypeName);
        Builder msgBuilder = MessageDefinition.newBuilder(parseFieldName(valueTypeName));
        Field[] fields = targetClass.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            int fieldIndex = i + 1;
            Field field = fields[i];
            String fieldTypeName = field.getType().getName();
            String protocolBufferType = TYPE_MAPPING.get(fieldTypeName);
            if (protocolBufferType != null) {
                // 普通类型
                msgBuilder.addField("optional", protocolBufferType, field.getName(), fieldIndex);
            } else if (fieldTypeName.startsWith(DEFAULT_PREFIX) && !field.getType().isEnum()) {
                // 嵌套类型
                generateSchema(fieldTypeName, schemaBuilder, context);
                msgBuilder.addField("optional", parseFieldName(fieldTypeName), field.getName(), fieldIndex);
            } else if (Collection.class.isAssignableFrom(field.getType())) {
                // Collection 容器类型
                // FIXME: 2022/12/9 List<Map<String, String>> 无法支持这种结构
                ParameterizedType parameterizedType = getParameterizedType(targetClass, field);
                String typeName = parameterizedType.getActualTypeArguments()[0].getTypeName();
                protocolBufferType = TYPE_MAPPING.get(typeName);
                if (protocolBufferType != null) {
                    msgBuilder.addField("repeated", protocolBufferType, field.getName(), fieldIndex);
                } else {
                    msgBuilder.addField("repeated", parseFieldName(typeName), field.getName(), fieldIndex);
                    generateSchema(typeName, schemaBuilder, context);
                }
            } else if (Map.class.isAssignableFrom(field.getType())) {
                msgBuilder.addField("repeated", "Map", field.getName(), fieldIndex);
            } else if (field.getType().isEnum()) {
                msgBuilder.addField("optional", "string", field.getName(), fieldIndex);
            } else {
                throw new UnsupportedOperationException("type:" + fieldTypeName);
            }
        }
        if (!context.getAddedList().contains(valueTypeName)) {
            schemaBuilder.addMessageDefinition(msgBuilder.build());
        }
        context.getAddedList().add(valueTypeName);
        context.getBuildingList().remove(valueTypeName);
    }

    public static HandleWrapper buildSchemaV2(Type valueType)
        throws ClassNotFoundException, DescriptorValidationException {
        HandleWrapper cacheWrapper = CACHE.get(valueType);
        if (cacheWrapper != null) {
            return cacheWrapper;
        }
        String typeName = null;
        String prefix = null;

        // Create dynamic schema
        DynamicSchema.Builder schemaBuilder = DynamicSchema.newBuilder();
        if (!(valueType instanceof ParameterizedTypeImpl)) {
            typeName = valueType.getTypeName();
            prefix = "";
            // 一般类
        } else if (List.class.isAssignableFrom(((ParameterizedTypeImpl) valueType).getRawType())) {
            ParameterizedTypeImpl parameterizedType = (ParameterizedTypeImpl) valueType;
            typeName = parameterizedType.getActualTypeArguments()[0].getTypeName();
            prefix = "List";
            MessageDefinition wrapperDef = MessageDefinition.newBuilder(parseFieldName(prefix + typeName))
                .addField("repeated", parseFieldName(typeName), "dataList", 1)
                .build();
            schemaBuilder.addMessageDefinition(wrapperDef);
        } else {
            throw new UnsupportedOperationException("type:" + valueType.getTypeName());
        }
        String fileDefinitionName = parseFieldName(typeName);
        String name = prefix + fileDefinitionName;
        schemaBuilder.setName(typeName + ".proto");

        DynamicBuildContext context = new DynamicBuildContext();

        loadMapDef(schemaBuilder);

        generateSchema(typeName, schemaBuilder, context);

        DynamicSchema dynamicSchema = schemaBuilder.build();

        if (log.isDebugEnabled()) {
            log.info("dynamic schema:[{}]", dynamicSchema.toString());
        }

        HandleWrapper handleWrapper = new HandleWrapper();
        handleWrapper.setSchema(dynamicSchema);
        handleWrapper.setName(name);
        handleWrapper.setTopTypeName(name);
        CACHE.putIfAbsent(valueType, handleWrapper);
        return handleWrapper;
    }






    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static HandleWrapper buildSchema(Type valueType)
        throws ClassNotFoundException, IllegalAccessException, DescriptorValidationException {
        HandleWrapper cacheWrapper = CACHE.get(valueType);
        if (cacheWrapper != null) {
            return cacheWrapper;
        }
        String typeName = null;
        String prefix = null;

        // Create dynamic schema
        DynamicSchema.Builder schemaBuilder = DynamicSchema.newBuilder();
        if (!(valueType instanceof ParameterizedTypeImpl)) {
            typeName = valueType.getTypeName();
            prefix = "";
            // 一般类
        } else if (List.class.isAssignableFrom(((ParameterizedTypeImpl) valueType).getRawType())) {
            ParameterizedTypeImpl parameterizedType = (ParameterizedTypeImpl) valueType;
            typeName = parameterizedType.getActualTypeArguments()[0].getTypeName();
            prefix = "List";
            MessageDefinition wrapperDef = MessageDefinition.newBuilder(parseFieldName(prefix + typeName))
                .addField("repeated", parseFieldName(typeName), "dataList", 1)
                .build();
            schemaBuilder.addMessageDefinition(wrapperDef);
        } else {
            throw new UnsupportedOperationException("type:" + valueType.getTypeName());
        }
        String fileDefinitionName = parseFieldName(typeName);
        String name = prefix + fileDefinitionName;
        schemaBuilder.setName(typeName + ".proto");

        Builder msgBuilder = MessageDefinition.newBuilder(fileDefinitionName);
        Class<?> targetClass = Class.forName(typeName);
        Field[] fields = targetClass.getDeclaredFields();

        loadMapDef(schemaBuilder);

        for (int i = 0; i < fields.length; i++) {
            int fieldIndex = i + 1;
            Field field = fields[i];
            String fieldTypeName = field.getType().getName();
            String protocolBufferType = TYPE_MAPPING.get(fieldTypeName);
            if (protocolBufferType != null) {
                msgBuilder.addField("optional", protocolBufferType, field.getName(), fieldIndex);
            } else if (fieldTypeName.startsWith(DEFAULT_PREFIX) && !field.getType().isEnum()) {
                MessageDefinition definition = CUSTOM_TYPE_MAPPING.get(fieldTypeName);
                if (definition != null) {
                    schemaBuilder.addMessageDefinition(definition);
                    msgBuilder.addField("optional", parseFieldName(fieldTypeName), field.getName(), fieldIndex);
                    continue;
                }
                generate(fieldTypeName, schemaBuilder);
                msgBuilder.addField("optional", parseFieldName(fieldTypeName), field.getName(), fieldIndex);
            } else if (Collection.class.isAssignableFrom(field.getType())) {
                // Collection 容器类型
                // FIXME: 2022/12/9 List<Map<String, String>> 无法支持这种结构
                ParameterizedType fieldParameterizedType = getParameterizedType(targetClass, field);
                String realTypeName = fieldParameterizedType.getActualTypeArguments()[0].getTypeName();
                protocolBufferType = TYPE_MAPPING.get(realTypeName);
                if (protocolBufferType != null) {
                    msgBuilder.addField("repeated", protocolBufferType, field.getName(), fieldIndex);
                } else {
                    msgBuilder.addField("repeated", parseFieldName(realTypeName), field.getName(), fieldIndex);
                    generate(realTypeName, schemaBuilder);
                }
            } else if (Map.class.isAssignableFrom(field.getType())) {
                msgBuilder.addField("repeated", "Map", field.getName(), fieldIndex);
            } else if (field.getType().isEnum()) {
                msgBuilder.addField("optional", "string", field.getName(), fieldIndex);
            } else {
                throw new UnsupportedOperationException("type:" + fieldTypeName);
            }
        }
        schemaBuilder.addMessageDefinition(msgBuilder.build());
        DynamicSchema dynamicSchema = schemaBuilder.build();
        // 一般类结构
        // List
//            ((ParameterizedTypeImpl) m.getGenericReturnType()).getActualTypeArguments()[0]
        // Map
        HandleWrapper handleWrapper = new HandleWrapper();
        handleWrapper.setSchema(dynamicSchema);
        handleWrapper.setName(name);
        handleWrapper.setTopTypeName(name);
        CACHE.putIfAbsent(valueType, handleWrapper);
        return handleWrapper;
    }

    public static void generate(String valueTypeName, DynamicSchema.Builder schemaBuilder)
        throws ClassNotFoundException, IllegalAccessException {
        MessageDefinition messageDefinition = CUSTOM_TYPE_MAPPING.get(valueTypeName);
        if (messageDefinition != null) {
            return;
        }
        Class<?> targetClass = Class.forName(valueTypeName);
        Builder msgBuilder = MessageDefinition.newBuilder(parseFieldName(valueTypeName));
        Field[] fields = targetClass.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            String fieldTypeName = field.getType().getName();
            String protocolBufferType = TYPE_MAPPING.get(fieldTypeName);
            if (protocolBufferType != null) {
                // 普通类型
                msgBuilder.addField("optional", protocolBufferType, field.getName(), i + 1);
            } else if (fieldTypeName.startsWith(DEFAULT_PREFIX) && !field.getType().isEnum()) {
                // 嵌套类型
                generate(fieldTypeName, schemaBuilder);
            } else if (Collection.class.isAssignableFrom(field.getType())) {
                // Collection 容器类型
                // FIXME: 2022/12/9 List<Map<String, String>> 无法支持这种结构
                ParameterizedType parameterizedType = getParameterizedType(targetClass, field);
                String typeName = parameterizedType.getActualTypeArguments()[0].getTypeName();
                protocolBufferType = TYPE_MAPPING.get(typeName);
                if (protocolBufferType != null) {
                    msgBuilder.addField("repeated", protocolBufferType, field.getName(), i + 1);
                } else {
                    msgBuilder.addField("repeated", parseFieldName(typeName), field.getName(), i + 1);
                    generate(typeName, schemaBuilder);
                }
            } else if (Map.class.isAssignableFrom(field.getType())) {
                msgBuilder.addField("repeated", "Map", field.getName(), i);
            } else if (field.getType().isEnum()) {
                msgBuilder.addField("optional", "string", field.getName(), i + 1);
            } else {
                throw new UnsupportedOperationException("type:" + fieldTypeName);
            }
        }
        CUSTOM_TYPE_MAPPING.putIfAbsent(valueTypeName, msgBuilder.build());
        schemaBuilder.addMessageDefinition(msgBuilder.build());
    }

    public static ParameterizedType getParameterizedType(Class<?> targetClass, Field field) {
        String getMethodName = "get" + field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
        Method[] methods = targetClass.getMethods();
        List<Method> methodList = Arrays.stream(methods)
            .filter(method -> method.getName().equals(getMethodName))
            .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(methodList)) {
            throw new IllegalArgumentException(targetClass.getName() + " 未找到对于的 get 方法：" + getMethodName + "，无法获取对应的类型");
        }
        Method method = methodList.get(0);
        ParameterizedTypeImpl parameterizedType = (ParameterizedTypeImpl) method.getGenericReturnType();
        return parameterizedType;
    }
}
