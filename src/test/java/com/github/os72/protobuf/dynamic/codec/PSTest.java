package com.github.os72.protobuf.dynamic.codec;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.github.os72.protobuf.dynamic.MessageCodec;
import com.github.os72.protobuf.dynamic.MessageCodec.HandleWrapper;
import com.github.os72.protobuf.dynamic.PersonSchema2.Person;
import com.github.os72.protobuf.dynamic.ProtostuffUtils;
import com.github.os72.protobuf.dynamic.codec.model.CircularUser;
import com.github.os72.protobuf.dynamic.codec.model.EmptyUser;
import com.github.os72.protobuf.dynamic.codec.model.ListType;
import com.github.os72.protobuf.dynamic.codec.model.ModifyFieldIndexUser;
import com.github.os72.protobuf.dynamic.codec.model.NestedUser;
import com.github.os72.protobuf.dynamic.codec.model.SetType;
import com.github.os72.protobuf.dynamic.codec.model.ThirdLevel;
import com.github.os72.protobuf.dynamic.codec.model.User;
import com.github.os72.protobuf.dynamic.codec.model.UserWithAddField;
import com.github.os72.protobuf.dynamic.codec.model.UserWithDeleteField;
import com.github.os72.protobuf.dynamic.codec.model.UserWithModifyFieldOrder;
import com.google.gson.reflect.TypeToken;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import io.protostuff.LinkedBuffer;
import io.protostuff.MessageCollectionSchema;
import io.protostuff.MessageMapSchema;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.StringMapSchema;
import io.protostuff.runtime.RuntimeSchema;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author ironman
 * @date 2023/7/5 23:06
 * @desc
 */
public class PSTest {

    static User buildUser() {
        User user = new User();
        user.setId(1);
        user.setId1(2);
        user.setUsername("ironman");

        return user;
    }

    @Test
    public void testDate() {
        Date date = new Date();
        System.out.println(date.getTime());

        byte[] bytes = ProtostuffUtils.serializer(date, Date.class);
        Date da = ProtostuffUtils.deserialize(bytes, Date.class);
        System.out.println(da.getTime());
    }

    public void decodeTest() {
        String str = "\\x0B\\x09\\xF0+\\xB3$\\x88\\x01\\x00\\x00\\x11\\xF0+\\xB3$\\x88\\x01\\x00\\x00\\x18\\x8D\\xA4\\x02 \\x959(\\xC6\\x9C\\x010\\x018\\x01\\x0C";


    }

    @Test
    public void testSimpleClass() throws DescriptorValidationException, ClassNotFoundException, IllegalAccessException {
        HandleWrapper userHandleWrapper = MessageCodec.buildSchemaV2(User.class);
        System.out.println(userHandleWrapper.getSchema().toString());

        TypeToken<User> typeToken = new TypeToken<User>(){};
        User user = buildUser();
        byte[] bytes = ProtostuffUtils.serializer(user, typeToken.getType());

        User o = ProtostuffUtils.deserialize(bytes, User.class);
        System.out.println(o);
    }

    @Test
    public void testCircularClass() throws DescriptorValidationException, ClassNotFoundException, IllegalAccessException, InvalidProtocolBufferException {
        HandleWrapper handleWrapper = MessageCodec.buildSchemaV2(CircularUser.class);
        System.out.println(handleWrapper.getSchema().toString());

        TypeToken<CircularUser> typeToken = new TypeToken<CircularUser>(){};
        CircularUser circularUser = CircularUser.build();
        byte[] bytes = ProtostuffUtils.serializer(circularUser, typeToken.getType());

        CircularUser o = ProtostuffUtils.deserialize(bytes, CircularUser.class);
        System.out.println(JSON.toJSONString(o));
        Assert.assertEquals(JSON.toJSONString(o), JSON.toJSONString(circularUser));
    }

    @Test
    public void testThirdLevel() throws DescriptorValidationException, ClassNotFoundException, IllegalAccessException, InvalidProtocolBufferException {
        HandleWrapper handleWrapper = MessageCodec.buildSchemaV2(ThirdLevel.class);
        System.out.println(handleWrapper.getSchema().toString());

        TypeToken<ThirdLevel> typeToken = new TypeToken<ThirdLevel>(){};
        ThirdLevel thirdLevel = ThirdLevel.build();
        byte[] bytes = ProtostuffUtils.serializer(thirdLevel, typeToken.getType());

        DynamicMessage dynamicMessage = MessageCodec.buildMessage(handleWrapper, thirdLevel);

        ThirdLevel o = ProtostuffUtils.deserialize(bytes, typeToken.getType());
        System.out.println(JSON.toJSONString(o));
        Assert.assertEquals(JSON.toJSONString(o), JSON.toJSONString(thirdLevel));
    }

    @Test
    public void testNestedClass()
        throws DescriptorValidationException, ClassNotFoundException, IllegalAccessException, InvalidProtocolBufferException {
        MessageCodec.buildSchemaV2(User.class);
        HandleWrapper nestedUserHandleWrapper1 = MessageCodec.buildSchemaV2(NestedUser.class);
        System.out.println(nestedUserHandleWrapper1.getSchema().toString());

        TypeToken<NestedUser> typeToken = new TypeToken<NestedUser>(){};
        NestedUser nestedUser = NestedUser.build();

        byte[] bytes = ProtostuffUtils.serializer(nestedUser, typeToken.getType());

        NestedUser o = ProtostuffUtils.deserialize(bytes, NestedUser.class);
        System.out.println(JSON.toJSONString(o));
        Assert.assertEquals(JSON.toJSONString(o), JSON.toJSONString(nestedUser));
    }

    @Test
    public void testCustomToProtoStuff()
        throws DescriptorValidationException, ClassNotFoundException, IllegalAccessException, InvalidProtocolBufferException {
        HandleWrapper nestedUserHandleWrapper1 = MessageCodec.buildSchemaV2(NestedUser.class);
        System.out.println(nestedUserHandleWrapper1.getSchema().toString());


        TypeToken<NestedUser> typeToken = new TypeToken<NestedUser>(){};
        NestedUser nestedUser = NestedUser.build();

        DynamicMessage dynamicMessage = MessageCodec.buildMessage(nestedUserHandleWrapper1, nestedUser);
        byte[] bytes = ProtostuffUtils.serializer(nestedUser, typeToken.getType());

        NestedUser o = ProtostuffUtils.deserialize(dynamicMessage.toByteArray(), NestedUser.class);
        System.out.println(JSON.toJSONString(o));
        Assert.assertEquals(JSON.toJSONString(o), JSON.toJSONString(nestedUser));
    }

    @SneakyThrows
    @Test
    public void testProtostuffToProtocolBufferNative() {
        Person person = Person.newBuilder()
            .setId(123)
            .setName("ironman")
            .setEmail("1@qq.com")
            .build();
        byte[] bytes = person.toByteArray();
        TypeToken<com.github.os72.protobuf.dynamic.Person> typeToken = new TypeToken<com.github.os72.protobuf.dynamic.Person>(){};
        Object deserialize = ProtostuffUtils.deserialize(bytes, typeToken.getType());
        System.out.println(deserialize);


        com.github.os72.protobuf.dynamic.Person person2 = new com.github.os72.protobuf.dynamic.Person();
        person2.setId(12333);
        person2.setName("Mark-1");
        person2.setEmail("2@qq.com");
        TypeToken<com.github.os72.protobuf.dynamic.Person> personType = new TypeToken<com.github.os72.protobuf.dynamic.Person>(){};
        byte[] bytes2 = ProtostuffUtils.serializer(person2, personType.getType());
        Person person3 = Person.parseFrom(bytes2);
        System.out.println(person3);
    }

    @Test
    public void testNestedList()
        throws DescriptorValidationException, ClassNotFoundException, IllegalAccessException, InvalidProtocolBufferException {
        List<NestedUser> nestedUserList = ListType.buildNestedUserList();


        TypeToken<List<NestedUser>> typeToken = new TypeToken<List<NestedUser>>(){};

        Method[] methods = ListType.class.getMethods();
        Method buildNestedUserListMethod = Arrays.stream(methods)
            .filter(method -> method.getName().equals("buildNestedUserList")).collect(Collectors.toList()).get(0);
        Type genericReturnType = buildNestedUserListMethod.getGenericReturnType();
        byte[] bytes = ProtostuffUtils.serializer(nestedUserList, buildNestedUserListMethod.getGenericReturnType());

        ProtostuffUtils.deserialize(bytes, buildNestedUserListMethod.getGenericReturnType());
        TypeToken<List<NestedUser>> listTypeToken = new TypeToken<List<NestedUser>>() {
        };
        Object o = ProtostuffUtils.deserialize(bytes, listTypeToken.getType());

        Assert.assertEquals(JSON.toJSONString(o), JSON.toJSONString(nestedUserList, SerializerFeature.DisableCircularReferenceDetect));
    }


    @Test
    public void testList()
        throws DescriptorValidationException, ClassNotFoundException, IllegalAccessException, InvalidProtocolBufferException {
        List<User> userList = ListType.buildUserList();
        TypeToken<List<User>> typeToken = new TypeToken<List<User>>(){};
        byte[] bytes = ProtostuffUtils.serializer(userList, typeToken.getType());

        TypeToken<List<User>> listTypeToken = new TypeToken<List<User>>() {
        };

        Object o = ProtostuffUtils.deserialize(bytes, listTypeToken.getType());

        Assert.assertEquals(JSON.toJSONString(o), JSON.toJSONString(userList, SerializerFeature.DisableCircularReferenceDetect));
    }

    // 大概处理 1w 次需要 2500 ms，一次 0.25ms
    // 大概处理 1w 次需要 563 ms，一次 0.0563ms
    @Test
    public void testTime() throws DescriptorValidationException, ClassNotFoundException, IllegalAccessException {
        HandleWrapper nestedUserHandleWrapper = MessageCodec.buildSchemaV2(NestedUser.class);
        for (int i = 0; i < 10000; i++) {
            try {
                NestedUser nestedUser = NestedUser.build();
                TypeToken<NestedUser> typeToken = new TypeToken<NestedUser>(){};
                byte[] bytes = ProtostuffUtils.serializer(nestedUser, nestedUser.getClass());
                NestedUser deserialize = ProtostuffUtils.deserialize(bytes, NestedUser.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void spaceTest()
        throws IOException, DescriptorValidationException, ClassNotFoundException, IllegalAccessException {
        com.github.os72.protobuf.dynamic.Person singlePerson = new com.github.os72.protobuf.dynamic.Person();
        singlePerson.setId(123);
        singlePerson.setName("ironman");
        singlePerson.setEmail("1@qq.com");
        Person person = Person.newBuilder()
            .setId(123)
            .setName("ironman")
            .setEmail("1@qq.com")
            .build();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        person.writeDelimitedTo(byteArrayOutputStream);

        Message message = MessageCodec.fromJson(JSON.toJSONString(singlePerson));
        ByteArrayOutputStream byteArrayOutputStream1 = new ByteArrayOutputStream();
        message.writeDelimitedTo(byteArrayOutputStream1);

        HandleWrapper handleWrapper = MessageCodec.buildSchemaV2(com.github.os72.protobuf.dynamic.Person.class);
        DynamicMessage dynamicMessage = MessageCodec.buildMessage(handleWrapper, singlePerson);
        ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();
        dynamicMessage.writeDelimitedTo(byteArrayOutputStream2);

        TypeToken<com.github.os72.protobuf.dynamic.Person> typeToken = new TypeToken<com.github.os72.protobuf.dynamic.Person>(){};
        byte[] bytes = ProtostuffUtils.serializer(singlePerson, typeToken.getType());
        ProtostuffUtils.deserialize(bytes, com.github.os72.protobuf.dynamic.Person.class);
        ByteArrayOutputStream byteArrayOutputStream3 = new ByteArrayOutputStream();
        byteArrayOutputStream3.write(bytes);

        System.out.println("json size: " + JSON.toJSONBytes(singlePerson).length);
        System.out.println("native size: " + byteArrayOutputStream.toByteArray().length);
        System.out.println("generate size: " + byteArrayOutputStream1.toByteArray().length);
        System.out.println("message size: " + byteArrayOutputStream2.toByteArray().length);
        System.out.println("protostuff size: " + byteArrayOutputStream3.toByteArray().length);
    }

    // FIXME: 2023/4/27 调整字段的顺序的影响

    @Test
    public void testModifyFieldIndex()
        throws DescriptorValidationException, ClassNotFoundException, IllegalAccessException, InvalidProtocolBufferException {

        User user = buildUser();
        TypeToken<User> typeToken = new TypeToken<User>(){};
        byte[] bytes = ProtostuffUtils.serializer(user, typeToken.getType());

        try {
            TypeToken<ModifyFieldIndexUser> modifyFieldIndexUserTypeToken = new TypeToken<ModifyFieldIndexUser>(){};
            ModifyFieldIndexUser deserialize = ProtostuffUtils.deserialize(bytes, modifyFieldIndexUserTypeToken.getType());
            System.out.println(deserialize);
        } catch (Exception e) {
            System.out.println(e);
        }

        try {
            TypeToken<UserWithAddField> userWithAddFieldTypeToken = new TypeToken<UserWithAddField>(){};
            UserWithAddField userWithAddField = ProtostuffUtils.deserialize(bytes, userWithAddFieldTypeToken.getType());
            System.out.println(userWithAddField);
        } catch (Exception e) {
            System.out.println(e);
        }

        try {
            TypeToken<UserWithDeleteField> userWithDeleteFieldTypeToken = new TypeToken<UserWithDeleteField>(){};
            UserWithDeleteField userWithDeleteField = ProtostuffUtils.deserialize(bytes, userWithDeleteFieldTypeToken.getType());
            System.out.println(userWithDeleteField);
        } catch (Exception e) {
            System.out.println(e);
        }

        try {
            TypeToken<UserWithModifyFieldOrder> userWithModifyFieldOrderTypeToken = new TypeToken<UserWithModifyFieldOrder>(){};
            UserWithModifyFieldOrder userWithModifyFieldOrder = ProtostuffUtils.deserialize(bytes, userWithModifyFieldOrderTypeToken.getType());
            System.out.println(userWithModifyFieldOrder);
        } catch (Exception e) {
                System.out.println(e);
        }
    }



    @Test
    public void testListWithEmpty()
        throws DescriptorValidationException, ClassNotFoundException, IllegalAccessException, InvalidProtocolBufferException {
        List<User> userList = Collections.emptyList();

        TypeToken<List<User>> token = new TypeToken<List<User>>(){};
        byte[] bytes = ProtostuffUtils.serializer(userList, token.getType());
        Object object = ProtostuffUtils.deserialize(bytes, token.getType());

        Assert.assertEquals(JSON.toJSONString(object), JSON.toJSONString(userList, SerializerFeature.DisableCircularReferenceDetect));
    }

    @Test
    public void testSimpleClassWithEmpty() throws DescriptorValidationException, ClassNotFoundException, IllegalAccessException {
        EmptyUser emptyUser = new EmptyUser();
        TypeToken<EmptyUser> token = new TypeToken<EmptyUser>(){};
        byte[] bytes = ProtostuffUtils.serializer(emptyUser, token.getType());

        User deserialize = ProtostuffUtils.deserialize(bytes, User.class);

        System.out.println(deserialize);
    }

    @Test
    public void testSet() {

        Set<User> userSet = SetType.buildUserSet();

        MessageCollectionSchema<User> messageCollectionSchema = new MessageCollectionSchema<User>(RuntimeSchema.createFrom(User.class), false);

        byte[] bytes1 = ProtostuffIOUtil.toByteArray(userSet, messageCollectionSchema, LinkedBuffer.allocate());

        Set<User> userSet1 = new HashSet<>();
        ProtostuffIOUtil.mergeFrom(bytes1, userSet1, messageCollectionSchema);

        Assert.assertEquals(JSON.toJSONString(userSet1), JSON.toJSONString(userSet, SerializerFeature.DisableCircularReferenceDetect));




        List<Map<String, User>> list = new ArrayList<>();
        Map<String, User> map = new HashMap<>();
        map.put("1", buildUser());
        list.add(map);

        StringMapSchema<User> stringMapSchema = new StringMapSchema<>(RuntimeSchema.createFrom(User.class));
        MessageMapSchema<String, User> mapSchema = new MessageMapSchema<>(RuntimeSchema.createFrom(String.class),
            RuntimeSchema.createFrom(User.class));
        MessageCollectionSchema<Map<String, User>> listMapSchema = new MessageCollectionSchema<>(stringMapSchema, false);

        byte[] listMapByte = ProtostuffIOUtil.toByteArray(list, listMapSchema, LinkedBuffer.allocate());

        List<Map<String, User>> listData = new ArrayList<>();
        ProtostuffIOUtil.mergeFrom(listMapByte, listData, listMapSchema);

        System.out.println(JSON.toJSONString(listData));

        TypeToken<List<Map<String, User>>> token = new TypeToken<List<Map<String, User>>>(){};
        Class<? super List<Map<String, User>>> rawType = token.getRawType();

    }

    @Test
    public void testSetWithUtils() {

        TypeToken<List<Map<String, User>>> token = new TypeToken<List<Map<String, User>>>(){};
        Class<? super List<Map<String, User>>> rawType = token.getRawType();
        Set<User> userSet = SetType.buildUserSet();

        TypeToken<Set<User>> setTypeToken = new TypeToken<Set<User>>(){};

        byte[] bytes = ProtostuffUtils.serializer(userSet, setTypeToken.getType());

        Set<User> deserialize = ProtostuffUtils.deserialize(bytes, setTypeToken.getType());

        MessageCollectionSchema<User> messageCollectionSchema = new MessageCollectionSchema<User>(RuntimeSchema.createFrom(User.class), false);

        byte[] bytes1 = ProtostuffIOUtil.toByteArray(userSet, messageCollectionSchema, LinkedBuffer.allocate());

        Set<User> userSet1 = new HashSet<>();
        ProtostuffIOUtil.mergeFrom(bytes1, userSet1, messageCollectionSchema);

        Assert.assertEquals(JSON.toJSONString(userSet1), JSON.toJSONString(userSet, SerializerFeature.DisableCircularReferenceDetect));




        List<Map<String, User>> list = new ArrayList<>();
        Map<String, User> map = new HashMap<>();
        map.put("1", buildUser());
        list.add(map);

        StringMapSchema<User> stringMapSchema = new StringMapSchema<>(RuntimeSchema.createFrom(User.class));
        MessageMapSchema<String, User> mapSchema = new MessageMapSchema<>(RuntimeSchema.createFrom(String.class),
            RuntimeSchema.createFrom(User.class));
        MessageCollectionSchema<Map<String, User>> listMapSchema = new MessageCollectionSchema<>(stringMapSchema, false);

        byte[] listMapByte = ProtostuffIOUtil.toByteArray(list, listMapSchema, LinkedBuffer.allocate());

        List<Map<String, User>> listData = new ArrayList<>();
        ProtostuffIOUtil.mergeFrom(listMapByte, listData, listMapSchema);

        System.out.println(JSON.toJSONString(listData));
    }
}
