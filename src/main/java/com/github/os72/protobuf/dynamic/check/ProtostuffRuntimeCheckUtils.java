package com.github.os72.protobuf.dynamic.check;

import com.github.os72.protobuf.dynamic.MessageCodec;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

/**
 * @author ironman
 * date 2023/8/1 21:16
 * desc
 */
public class ProtostuffRuntimeCheckUtils {


    private static Map<String, String> TYPE_MAPPING = new HashMap<>(32);

    private static Map<String, Type> CACHE = new HashMap<>(64);

    static {
        TYPE_MAPPING.put("int", "int");
        TYPE_MAPPING.put("float", "float");
        TYPE_MAPPING.put("double", "double");
        TYPE_MAPPING.put("long", "long");
        TYPE_MAPPING.put("byte", "byte");
        TYPE_MAPPING.put("boolean", "boolean");
        TYPE_MAPPING.put("char", "char");

        TYPE_MAPPING.put("java.lang.Integer", "java.lang.Integer");
        TYPE_MAPPING.put("java.lang.Float", "java.lang.Float");
        TYPE_MAPPING.put("java.lang.Double", "java.lang.Double");
        TYPE_MAPPING.put("java.lang.Byte", "java.lang.Byte");
        TYPE_MAPPING.put("java.lang.Long", "java.lang.Long");
        TYPE_MAPPING.put("java.lang.Character", "java.lang.Character");
        TYPE_MAPPING.put("java.lang.String", "java.lang.String");
        TYPE_MAPPING.put("java.lang.Enum", "java.lang.Enum");
        TYPE_MAPPING.put("java.lang.Boolean", "java.lang.Boolean");

        TYPE_MAPPING.put("java.util.Date", "java.util.Date");
    }

    /**
     * 忽略的包名前缀，不用去检查是否添加注解
     */
    private static final String IGNORE_PACKAGE_NAME_PREFIX = "java.";

    /**
     * 检查是否存在规定的注解
     * 嵌套的类也需要添加相应的注解，包括泛型类
     *
     * @param valueType
     */
    public static void check(Type valueType) {
        if (valueType == null) {
            throw new IllegalArgumentException("valueType is null");
        }

        if (TYPE_MAPPING.get(valueType.getTypeName()) != null || valueType.getTypeName().startsWith(IGNORE_PACKAGE_NAME_PREFIX)) {
            return;
        }
        if (CACHE.get(valueType.getTypeName()) != null) {
            return;
        }

        Map<String, Class<?>> classMap = new HashMap<>(32);
        if (!(valueType instanceof ParameterizedTypeImpl)) {
            // 一般类
            parse(valueType.getTypeName(), classMap);
        } else if (List.class.isAssignableFrom(((ParameterizedTypeImpl) valueType).getRawType())) {
            // list
            ParameterizedTypeImpl parameterizedType = (ParameterizedTypeImpl) valueType;
            parse(parameterizedType.getActualTypeArguments()[0].getTypeName(), classMap);
        } else if (Set.class.isAssignableFrom(((ParameterizedTypeImpl) valueType).getRawType())) {
            // set
            ParameterizedTypeImpl parameterizedType = (ParameterizedTypeImpl) valueType;
            parse(parameterizedType.getActualTypeArguments()[0].getTypeName(), classMap);
        } else if (Map.class.isAssignableFrom(((ParameterizedTypeImpl) valueType).getRawType())) {
            // map
            ParameterizedType parameterizedType = (ParameterizedTypeImpl) valueType;
            Type[] arguments = parameterizedType.getActualTypeArguments();
            if (arguments == null || arguments.length < 2) {
                throw new IllegalArgumentException("Map should have two parameter");
            }
            parse(arguments[0].getTypeName(), classMap);
            parse(arguments[1].getTypeName(), classMap);
        } else {
            throw new UnsupportedOperationException("type:" + valueType.getTypeName());
        }
        CACHE.put(valueType.getTypeName(), valueType);
    }

    private static void parse(String type, Map<String, Class<?>> classMap) {
        if (TYPE_MAPPING.get(type) != null  || type.startsWith(IGNORE_PACKAGE_NAME_PREFIX)) {
            return;
        }
        if (classMap == null) {
            classMap = new HashMap<>(32);
        }
        if (classMap.get(type) != null) {
            // 已经进行过解析，则不用在进行解析
            return;
        }
        try {
            Class<?> clazz = Class.forName(type);
            ProtostuffSerializationClass annotation = clazz.getAnnotation(ProtostuffSerializationClass.class);
            if (annotation == null) {
                throw new IllegalArgumentException("使用 Protostuff，需要给关的类[" + type + "]添加 @ProtostuffSerializationClass 注解");
            }
            if (annotation.firstGenerate()) {
                throw new IllegalArgumentException("@ProtostuffSerializationClass 在运行期间 [firstGenerate] 不能为 true");
            }
            classMap.put(type, clazz);
            Field[] fields = clazz.getDeclaredFields();
            List<Field> allField = new ArrayList<>(Arrays.asList(fields));
            Class<?> parentClass = clazz.getSuperclass();
            while (parentClass != null && parentClass != Object.class) {
                allField.addAll(Arrays.asList(parentClass.getDeclaredFields()));
                parentClass = parentClass.getSuperclass();
            }
            for (int i = 0; i < allField.size(); i++) {
                Field field = allField.get(i);
                if (ignoreField(field)) {
                    continue;
                }
                String typeName = TYPE_MAPPING.get(field.getType().getName());
                if (typeName != null) {
                    continue;
                }
                if (Collection.class.isAssignableFrom(field.getType())) {
                    // Collection 容器类型
                    ParameterizedType parameterizedType = MessageCodec.getParameterizedType(clazz, field);
                    if (parameterizedType.getActualTypeArguments()[0].getTypeName().contains("java.util.Map")) {
                        throw new UnsupportedOperationException("type: " + parameterizedType.getActualTypeArguments()[0].getTypeName());
                    }
                    String parameterTypeName = parameterizedType.getActualTypeArguments()[0].getTypeName();
                    parse(parameterTypeName, classMap);
                } else if (Map.class.isAssignableFrom(field.getType())) {
                    ParameterizedType parameterizedType = MessageCodec.getParameterizedType(clazz, field);
                    Type[] arguments = parameterizedType.getActualTypeArguments();
                    if (arguments == null || arguments.length < 2) {
                        throw new IllegalArgumentException("Map should have two parameter");
                    }
                    parse(arguments[0].getTypeName(), classMap);
                    parse(arguments[1].getTypeName(), classMap);
                } else {
                    parse(field.getType().getTypeName(), classMap);
                }
            }

        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static boolean ignoreField(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers);
    }
}
