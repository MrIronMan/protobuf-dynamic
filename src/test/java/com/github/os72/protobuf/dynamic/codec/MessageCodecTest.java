package com.github.os72.protobuf.dynamic.codec;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.github.os72.protobuf.dynamic.MessageCodec;
import com.github.os72.protobuf.dynamic.MessageCodec.HandleWrapper;
import com.github.os72.protobuf.dynamic.PersonSchema2.Person;
import com.github.os72.protobuf.dynamic.Wrapper;
import com.github.os72.protobuf.dynamic.codec.model.CircularUser;
import com.github.os72.protobuf.dynamic.codec.model.EmptyUser;
import com.github.os72.protobuf.dynamic.codec.model.ListType;
import com.github.os72.protobuf.dynamic.codec.model.ModifyFieldIndexUser;
import com.github.os72.protobuf.dynamic.codec.model.NestedUser;
import com.github.os72.protobuf.dynamic.codec.model.SetType;
import com.github.os72.protobuf.dynamic.codec.model.ThirdLevel;
import com.github.os72.protobuf.dynamic.codec.model.User;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ListValue;
import com.google.protobuf.Message;
import com.google.protobuf.Struct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author ironman
 * @date 2022/12/21 20:53
 * @desc
 */
public class MessageCodecTest {

    static User buildUser() {
        User user = new User();
        user.setId(1);
        user.setId1(2);
        user.setUsername("ironman");

        return user;
    }

    /**
     * 直接使用 protocol buffer 的 Struct to Message 进行转换，但是结构太大
     *
     * @throws IOException
     */
    @Test
    public void testJson() throws IOException {
        String data = "{\"dynamicDataList\":[{\"addressList\":[\"dddd\"],\"babyList\":[{\"bol\":false,\"c\":\"c\",\"c1\":\"c\",\"d\":4.0,\"d1\":8.0,\"f\":3.0,\"f1\":7.0,\"id\":1,\"id1\":2,\"l\":5,\"l1\":9,\"type\":\"NORMAL\"},{\"bol\":false,\"c\":\"c\",\"c1\":\"c\",\"d\":4.0,\"d1\":8.0,\"f\":3.0,\"f1\":7.0,\"id\":1,\"id1\":2,\"l\":5,\"l1\":9,\"type\":\"NORMAL\"}],\"id\":100000,\"idsList\":[1234],\"map\":{\"key123\":\"value123\"},\"name\":\"Mark-1\",\"user\":{\"bol\":false,\"c\":\"c\",\"c1\":\"c\",\"d\":4.0,\"d1\":8.0,\"f\":3.0,\"f1\":7.0,\"id\":1,\"id1\":2,\"l\":5,\"l1\":9,\"type\":\"NORMAL\"}},{\"addressList\":[\"dddd\"],\"babyList\":[{\"bol\":false,\"c\":\"c\",\"c1\":\"c\",\"d\":4.0,\"d1\":8.0,\"f\":3.0,\"f1\":7.0,\"id\":1,\"id1\":2,\"l\":5,\"l1\":9,\"type\":\"NORMAL\"},{\"bol\":false,\"c\":\"c\",\"c1\":\"c\",\"d\":4.0,\"d1\":8.0,\"f\":3.0,\"f1\":7.0,\"id\":1,\"id1\":2,\"l\":5,\"l1\":9,\"type\":\"NORMAL\"}],\"id\":100000,\"idsList\":[1234],\"map\":{\"key123\":\"value123\"},\"name\":\"Mark-1\",\"user\":{\"bol\":false,\"c\":\"c\",\"c1\":\"c\",\"d\":4.0,\"d1\":8.0,\"f\":3.0,\"f1\":7.0,\"id\":1,\"id1\":2,\"l\":5,\"l1\":9,\"type\":\"NORMAL\"}}]}";
        Message message = MessageCodec.fromJson(data);
        System.out.println(JSON.toJSONString(message.toByteArray()));
        Struct struct = Struct.newBuilder().mergeFrom(message.toByteArray()).build();
        MessageCodec.toJson(struct);
        String s = MessageCodec.toJson(message);
        System.out.println(message);
        System.out.println(s);

        String listData = "[{\"addressList\":[\"dddd\"],\"babyList\":[{\"bol\":false,\"c\":\"c\",\"c1\":\"c\",\"d\":4.0,\"d1\":8.0,\"f\":3.0,\"f1\":7.0,\"id\":1,\"id1\":2,\"l\":5,\"l1\":9,\"type\":\"NORMAL\"},{\"bol\":false,\"c\":\"c\",\"c1\":\"c\",\"d\":4.0,\"d1\":8.0,\"f\":3.0,\"f1\":7.0,\"id\":1,\"id1\":2,\"l\":5,\"l1\":9,\"type\":\"NORMAL\"}],\"id\":100000,\"idsList\":[1234],\"map\":{\"key123\":\"value123\"},\"name\":\"Mark-1\",\"user\":{\"bol\":false,\"c\":\"c\",\"c1\":\"c\",\"d\":4.0,\"d1\":8.0,\"f\":3.0,\"f1\":7.0,\"id\":1,\"id1\":2,\"l\":5,\"l1\":9,\"type\":\"NORMAL\"}},{\"addressList\":[\"dddd\"],\"babyList\":[{\"bol\":false,\"c\":\"c\",\"c1\":\"c\",\"d\":4.0,\"d1\":8.0,\"f\":3.0,\"f1\":7.0,\"id\":1,\"id1\":2,\"l\":5,\"l1\":9,\"type\":\"NORMAL\"},{\"bol\":false,\"c\":\"c\",\"c1\":\"c\",\"d\":4.0,\"d1\":8.0,\"f\":3.0,\"f1\":7.0,\"id\":1,\"id1\":2,\"l\":5,\"l1\":9,\"type\":\"NORMAL\"}],\"id\":100000,\"idsList\":[1234],\"map\":{\"key123\":\"value123\"},\"name\":\"Mark-1\",\"user\":{\"bol\":false,\"c\":\"c\",\"c1\":\"c\",\"d\":4.0,\"d1\":8.0,\"f\":3.0,\"f1\":7.0,\"id\":1,\"id1\":2,\"l\":5,\"l1\":9,\"type\":\"NORMAL\"}}]";
        ListValue listValue = MessageCodec.fromJsonList(listData);
        ListValue build = ListValue.newBuilder().mergeFrom(listValue.toByteArray()).build();
        String list = MessageCodec.toJson(listValue);

        List<NestedUser> nestedUserList = ListType.buildNestedUserList();

        Method[] methods = ListType.class.getMethods();
        Method buildNestedUserListMethod = Arrays.stream(methods)
            .filter(method -> method.getName().equals("buildNestedUserList")).collect(Collectors.toList()).get(0);
        Type genericReturnType = buildNestedUserListMethod.getGenericReturnType();
        List<NestedUser> object = JSON.parseObject(list, genericReturnType);

        System.out.println(listValue);
        System.out.println(list);
    }

    //-----------------------------------------------------------------------------------------------------------------//
    //-------------------------------------------以上为原生API----------------------------------------------------------//
    //-----------------------------------------------------------------------------------------------------------------//



    @Test
    public void testSimpleClass() throws DescriptorValidationException, ClassNotFoundException, IllegalAccessException {
        HandleWrapper userHandleWrapper = MessageCodec.buildSchemaV2(User.class);
        System.out.println(userHandleWrapper.getSchema().toString());

        User user = buildUser();

        DynamicMessage dynamicMessage = MessageCodec.buildMessage(userHandleWrapper, user);

        User o = (User) MessageCodec.parseObject(dynamicMessage, User.class);
        System.out.println(o);
    }

    @Test
    public void testCircularClass() throws DescriptorValidationException, ClassNotFoundException, IllegalAccessException, InvalidProtocolBufferException {
        HandleWrapper handleWrapper = MessageCodec.buildSchemaV2(CircularUser.class);
        System.out.println(handleWrapper.getSchema().toString());

        CircularUser circularUser = CircularUser.build();

        DynamicMessage dynamicMessage = MessageCodec.buildMessage(handleWrapper, circularUser);

        DynamicMessage dynamicMessage1 = DynamicMessage.parseFrom(
            handleWrapper.getSchema().getMessageDescriptor(handleWrapper.getTopTypeName()),
            dynamicMessage.toByteArray());

        CircularUser o = (CircularUser) MessageCodec.parseObject(dynamicMessage1, CircularUser.class);
        System.out.println(JSON.toJSONString(o));
        Assert.assertEquals(JSON.toJSONString(o), JSON.toJSONString(circularUser));
    }

    @Test
    public void testThirdLevel() throws DescriptorValidationException, ClassNotFoundException, IllegalAccessException, InvalidProtocolBufferException {
        HandleWrapper handleWrapper = MessageCodec.buildSchemaV2(ThirdLevel.class);
        System.out.println(handleWrapper.getSchema().toString());

        ThirdLevel thirdLevel = ThirdLevel.build();

        DynamicMessage dynamicMessage = MessageCodec.buildMessage(handleWrapper, thirdLevel);

        DynamicMessage dynamicMessage1 = DynamicMessage.parseFrom(
            handleWrapper.getSchema().getMessageDescriptor(handleWrapper.getTopTypeName()),
            dynamicMessage.toByteArray());

        ThirdLevel o = (ThirdLevel) MessageCodec.parseObject(dynamicMessage1, ThirdLevel.class);
        System.out.println(JSON.toJSONString(o));
        Assert.assertEquals(JSON.toJSONString(o), JSON.toJSONString(thirdLevel));
    }

    @Test
    public void testNestedClass()
        throws DescriptorValidationException, ClassNotFoundException, IllegalAccessException, InvalidProtocolBufferException {
        MessageCodec.buildSchemaV2(User.class);
        HandleWrapper nestedUserHandleWrapper1 = MessageCodec.buildSchemaV2(NestedUser.class);
        System.out.println(nestedUserHandleWrapper1.getSchema().toString());

        NestedUser nestedUser = NestedUser.build();

        DynamicMessage dynamicMessage = MessageCodec.buildMessage(nestedUserHandleWrapper1, nestedUser);

        DynamicMessage dynamicMessage1 = DynamicMessage.parseFrom(
            nestedUserHandleWrapper1.getSchema().getMessageDescriptor(nestedUserHandleWrapper1.getTopTypeName()),
            dynamicMessage.toByteArray());

        NestedUser o = (NestedUser) MessageCodec.parseObject(dynamicMessage1, NestedUser.class);
        System.out.println(JSON.toJSONString(o));
        Assert.assertEquals(JSON.toJSONString(o), JSON.toJSONString(nestedUser));
    }

    @Test
    public void testNestedList()
        throws DescriptorValidationException, ClassNotFoundException, IllegalAccessException, InvalidProtocolBufferException {
        List<NestedUser> nestedUserList = ListType.buildNestedUserList();

        Method[] methods = ListType.class.getMethods();
        Method buildNestedUserListMethod = Arrays.stream(methods)
            .filter(method -> method.getName().equals("buildNestedUserList")).collect(Collectors.toList()).get(0);
        Type genericReturnType = buildNestedUserListMethod.getGenericReturnType();
        HandleWrapper handleWrapper = MessageCodec.buildSchemaV2(genericReturnType);

        Map<String, List<NestedUser>> jsonObject = new HashMap<>();
        jsonObject.put("dynamicDataList", nestedUserList);
        Wrapper<List<NestedUser>> wrapper = new Wrapper<>();
        wrapper.setValue(nestedUserList);

        System.out.println(handleWrapper.getSchema());

        DynamicMessage dynamicMessage = MessageCodec.buildMessage(handleWrapper, nestedUserList);
        DynamicMessage serializableData = DynamicMessage.parseFrom(
            handleWrapper.getSchema().getMessageDescriptor(handleWrapper.getTopTypeName()),
            dynamicMessage.toByteArray());

        Object o = MessageCodec.parseObject(serializableData, genericReturnType);

        Assert.assertEquals(JSON.toJSONString(o), JSON.toJSONString(nestedUserList, SerializerFeature.DisableCircularReferenceDetect));
    }


    @Test
    public void testList()
        throws DescriptorValidationException, ClassNotFoundException, IllegalAccessException, InvalidProtocolBufferException {
        List<User> userList = ListType.buildUserList();

        Method[] methods = ListType.class.getMethods();
        Method buildUserListMethod = Arrays.stream(methods).filter(method -> method.getName().equals("buildUserList"))
            .collect(Collectors.toList()).get(0);
        Type genericReturnType = buildUserListMethod.getGenericReturnType();
        HandleWrapper handleWrapper = MessageCodec.buildSchemaV2(genericReturnType);

        DynamicMessage dynamicMessage = MessageCodec.buildMessage(handleWrapper, userList);
        DynamicMessage serializableData = DynamicMessage.parseFrom(
            handleWrapper.getSchema().getMessageDescriptor(handleWrapper.getTopTypeName()),
            dynamicMessage.toByteArray());

        Object o = MessageCodec.parseObject(serializableData, genericReturnType);

        Assert.assertEquals(JSON.toJSONString(o), JSON.toJSONString(userList, SerializerFeature.DisableCircularReferenceDetect));
    }

    // 大概处理 1w 次需要 2500 ms，一次 0.25ms
    @Test
    public void testTime() throws DescriptorValidationException, ClassNotFoundException, IllegalAccessException {
        HandleWrapper nestedUserHandleWrapper = MessageCodec.buildSchemaV2(NestedUser.class);
        for (int i = 0; i < 10000; i++) {
            try {
                NestedUser nestedUser = NestedUser.build();

                DynamicMessage dynamicMessage = MessageCodec.buildMessage(nestedUserHandleWrapper, nestedUser);

                DynamicMessage dynamicMessage1 = DynamicMessage.parseFrom(
                    nestedUserHandleWrapper.getSchema().getMessageDescriptor(nestedUserHandleWrapper.getTopTypeName()),
                    dynamicMessage.toByteArray());

                NestedUser o = (NestedUser) MessageCodec.parseObject(dynamicMessage1, NestedUser.class);
                JSON.toJSONString(o);
//                System.out.println(JSON.toJSONString(o));
//                Assert.assertEquals(JSON.toJSONString(o), JSON.toJSONString(nestedUser));
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }
    }

    // 大概处理 1w 次需要 2500 ms，一次 0.25ms
    /*@Test
    public void testJsonTime() throws IOException {
        for (int i = 0; i < 10000; i++) {
            String data = "{\"dataList\":[{\"addressList\":[\"dddd\"],\"babyList\":[{\"bol\":false,\"c\":\"c\",\"c1\":\"c\",\"d\":4.0,\"d1\":8.0,\"f\":3.0,\"f1\":7.0,\"id\":1,\"id1\":2,\"l\":5,\"l1\":9,\"type\":\"NORMAL\"},{\"bol\":false,\"c\":\"c\",\"c1\":\"c\",\"d\":4.0,\"d1\":8.0,\"f\":3.0,\"f1\":7.0,\"id\":1,\"id1\":2,\"l\":5,\"l1\":9,\"type\":\"NORMAL\"}],\"id\":100000,\"idsList\":[1234],\"map\":{\"key123\":\"value123\"},\"name\":\"Mark-1\",\"user\":{\"bol\":false,\"c\":\"c\",\"c1\":\"c\",\"d\":4.0,\"d1\":8.0,\"f\":3.0,\"f1\":7.0,\"id\":1,\"id1\":2,\"l\":5,\"l1\":9,\"type\":\"NORMAL\"}},{\"addressList\":[\"dddd\"],\"babyList\":[{\"bol\":false,\"c\":\"c\",\"c1\":\"c\",\"d\":4.0,\"d1\":8.0,\"f\":3.0,\"f1\":7.0,\"id\":1,\"id1\":2,\"l\":5,\"l1\":9,\"type\":\"NORMAL\"},{\"bol\":false,\"c\":\"c\",\"c1\":\"c\",\"d\":4.0,\"d1\":8.0,\"f\":3.0,\"f1\":7.0,\"id\":1,\"id1\":2,\"l\":5,\"l1\":9,\"type\":\"NORMAL\"}],\"id\":100000,\"idsList\":[1234],\"map\":{\"key123\":\"value123\"},\"name\":\"Mark-1\",\"user\":{\"bol\":false,\"c\":\"c\",\"c1\":\"c\",\"d\":4.0,\"d1\":8.0,\"f\":3.0,\"f1\":7.0,\"id\":1,\"id1\":2,\"l\":5,\"l1\":9,\"type\":\"NORMAL\"}}]}";
            Message message = MessageCodec.fromJson(data);
            Struct build = Struct.newBuilder().mergeFrom(message.toByteArray()).build();
            String s = MessageCodec.toJson(message);
//            System.out.println(message);
//            System.out.println(s);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            String listData = "[{\"addressList\":[\"dddd\"],\"babyList\":[{\"bol\":false,\"c\":\"c\",\"c1\":\"c\",\"d\":4.0,\"d1\":8.0,\"f\":3.0,\"f1\":7.0,\"id\":1,\"id1\":2,\"l\":5,\"l1\":9,\"type\":\"NORMAL\"},{\"bol\":false,\"c\":\"c\",\"c1\":\"c\",\"d\":4.0,\"d1\":8.0,\"f\":3.0,\"f1\":7.0,\"id\":1,\"id1\":2,\"l\":5,\"l1\":9,\"type\":\"NORMAL\"}],\"id\":100000,\"idsList\":[1234],\"map\":{\"key123\":\"value123\"},\"name\":\"Mark-1\",\"user\":{\"bol\":false,\"c\":\"c\",\"c1\":\"c\",\"d\":4.0,\"d1\":8.0,\"f\":3.0,\"f1\":7.0,\"id\":1,\"id1\":2,\"l\":5,\"l1\":9,\"type\":\"NORMAL\"}},{\"addressList\":[\"dddd\"],\"babyList\":[{\"bol\":false,\"c\":\"c\",\"c1\":\"c\",\"d\":4.0,\"d1\":8.0,\"f\":3.0,\"f1\":7.0,\"id\":1,\"id1\":2,\"l\":5,\"l1\":9,\"type\":\"NORMAL\"},{\"bol\":false,\"c\":\"c\",\"c1\":\"c\",\"d\":4.0,\"d1\":8.0,\"f\":3.0,\"f1\":7.0,\"id\":1,\"id1\":2,\"l\":5,\"l1\":9,\"type\":\"NORMAL\"}],\"id\":100000,\"idsList\":[1234],\"map\":{\"key123\":\"value123\"},\"name\":\"Mark-1\",\"user\":{\"bol\":false,\"c\":\"c\",\"c1\":\"c\",\"d\":4.0,\"d1\":8.0,\"f\":3.0,\"f1\":7.0,\"id\":1,\"id1\":2,\"l\":5,\"l1\":9,\"type\":\"NORMAL\"}}]";
            ListValue listValue = MessageCodec.fromJsonList(listData);
            ListValue build1 = ListValue.newBuilder().mergeFrom(listValue.toByteArray()).build();
            String list = MessageCodec.toJson(listValue);

            Method[] methods = ListType.class.getMethods();
            Method buildNestedUserListMethod = Arrays.stream(methods)
                .filter(method -> method.getName().equals("buildNestedUserList")).collect(Collectors.toList()).get(0);
            Type genericReturnType = buildNestedUserListMethod.getGenericReturnType();
            List<NestedUser> object = JSON.parseObject(list, genericReturnType);

//            System.out.println(listValue);
//            System.out.println(list);
        }
    }*/

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

        System.out.println("native size: " + byteArrayOutputStream.toByteArray().length);
        System.out.println("generate size: " + byteArrayOutputStream1.toByteArray().length);
        System.out.println("message size: " + byteArrayOutputStream2.toByteArray().length);
    }

    // FIXME: 2023/4/27 调整字段的顺序的影响

    @Test
    public void testModifyFieldIndex()
        throws DescriptorValidationException, ClassNotFoundException, IllegalAccessException, InvalidProtocolBufferException {
        HandleWrapper userHandleWrapper = MessageCodec.buildSchemaV2(User.class);
        HandleWrapper modifyFieldIndexUserHandleWrapper = MessageCodec.buildSchemaV2(ModifyFieldIndexUser.class);

        User user = buildUser();

        DynamicMessage dynamicMessage = MessageCodec.buildMessage(userHandleWrapper, user);

        DynamicMessage message = DynamicMessage.parseFrom(
            userHandleWrapper.getSchema().getMessageDescriptor(userHandleWrapper.getTopTypeName()),
            dynamicMessage.toByteArray());

        ModifyFieldIndexUser o = (ModifyFieldIndexUser) MessageCodec.parseObject(dynamicMessage, ModifyFieldIndexUser.class);
        System.out.println(o);
    }



    @Test
    public void testListWithEmpty()
        throws DescriptorValidationException, ClassNotFoundException, IllegalAccessException, InvalidProtocolBufferException {
        List<User> userList = Collections.emptyList();

        Method[] methods = ListType.class.getMethods();
        Method buildUserListMethod = Arrays.stream(methods).filter(method -> method.getName().equals("buildUserList"))
            .collect(Collectors.toList()).get(0);
        Type genericReturnType = buildUserListMethod.getGenericReturnType();
        HandleWrapper handleWrapper = MessageCodec.buildSchemaV2(genericReturnType);

        DynamicMessage dynamicMessage = MessageCodec.buildMessage(handleWrapper, userList);
        DynamicMessage serializableData = DynamicMessage.parseFrom(
            handleWrapper.getSchema().getMessageDescriptor(handleWrapper.getTopTypeName()),
            dynamicMessage.toByteArray());

        Object o = MessageCodec.parseObject(serializableData, genericReturnType);

        Assert.assertEquals(JSON.toJSONString(o), JSON.toJSONString(userList, SerializerFeature.DisableCircularReferenceDetect));
    }

    @Test
    public void testSimpleClassWithEmpty() throws DescriptorValidationException, ClassNotFoundException, IllegalAccessException {
        HandleWrapper userHandleWrapper = MessageCodec.buildSchemaV2(EmptyUser.class);
        System.out.println(userHandleWrapper.getSchema().toString());

        EmptyUser emptyUser = new EmptyUser();

        DynamicMessage dynamicMessage = MessageCodec.buildMessage(userHandleWrapper, emptyUser);

        User o = (User) MessageCodec.parseObject(dynamicMessage, User.class);
        System.out.println(o);
    }

    @Test
    public void testSet()
        throws DescriptorValidationException, ClassNotFoundException, IllegalAccessException, InvalidProtocolBufferException {
        Set<User> userSet = SetType.buildUserSet();

        Method[] methods = SetType.class.getMethods();
        Method buildUserListMethod = Arrays.stream(methods).filter(method -> method.getName().equals("buildUserSet"))
            .collect(Collectors.toList()).get(0);
        Type genericReturnType = buildUserListMethod.getGenericReturnType();
        HandleWrapper handleWrapper = MessageCodec.buildSchemaV2(genericReturnType);

        DynamicMessage dynamicMessage = MessageCodec.buildMessage(handleWrapper, userSet);
        DynamicMessage serializableData = DynamicMessage.parseFrom(
            handleWrapper.getSchema().getMessageDescriptor(handleWrapper.getTopTypeName()),
            dynamicMessage.toByteArray());

        Object o = MessageCodec.parseObject(serializableData, genericReturnType);

        Assert.assertEquals(JSON.toJSONString(o), JSON.toJSONString(userSet, SerializerFeature.DisableCircularReferenceDetect));
    }
}
