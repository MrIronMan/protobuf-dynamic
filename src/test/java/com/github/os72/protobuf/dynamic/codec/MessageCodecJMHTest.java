package com.github.os72.protobuf.dynamic.codec;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.github.os72.protobuf.dynamic.MessageCodec;
import com.github.os72.protobuf.dynamic.MessageCodec.HandleWrapper;
import com.github.os72.protobuf.dynamic.PersonSchema2.Person;
import com.github.os72.protobuf.dynamic.Wrapper;
import com.github.os72.protobuf.dynamic.codec.model.CircularUser;
import com.github.os72.protobuf.dynamic.codec.model.ListType;
import com.github.os72.protobuf.dynamic.codec.model.NestedUser;
import com.github.os72.protobuf.dynamic.codec.model.ThirdLevel;
import com.github.os72.protobuf.dynamic.codec.model.User;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * @author ironman
 * @date 2023/4/19 09:17
 * @desc
 */

@BenchmarkMode(Mode.Throughput)
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Threads(1)
public class MessageCodecJMHTest {

    static User buildUser() {
        User user = new User();
        user.setId(1);
        user.setId1(2);
        user.setUsername("ironman");

        return user;
    }

    @Benchmark
    public void testSimpleClass()
        throws DescriptorValidationException, ClassNotFoundException, IllegalAccessException, InvalidProtocolBufferException {
        HandleWrapper userHandleWrapper = MessageCodec.buildSchemaV2(User.class);

        User user = buildUser();

        DynamicMessage dynamicMessage = MessageCodec.buildMessage(userHandleWrapper, user);

        DynamicMessage dynamicMessage1 = DynamicMessage.parseFrom(
            userHandleWrapper.getSchema().getMessageDescriptor(userHandleWrapper.getTopTypeName()),
            dynamicMessage.toByteArray());

        User o = (User) MessageCodec.parseObject(dynamicMessage1, User.class);
    }

    @Benchmark
    public void testCircularClass() throws DescriptorValidationException, ClassNotFoundException, IllegalAccessException, InvalidProtocolBufferException {
        HandleWrapper handleWrapper = MessageCodec.buildSchemaV2(CircularUser.class);

        CircularUser circularUser = CircularUser.build();

        DynamicMessage dynamicMessage = MessageCodec.buildMessage(handleWrapper, circularUser);

        DynamicMessage dynamicMessage1 = DynamicMessage.parseFrom(
            handleWrapper.getSchema().getMessageDescriptor(handleWrapper.getTopTypeName()),
            dynamicMessage.toByteArray());

        CircularUser o = (CircularUser) MessageCodec.parseObject(dynamicMessage1, CircularUser.class);
        Assert.assertEquals(JSON.toJSONString(o), JSON.toJSONString(circularUser));
    }

    @Benchmark
    public void testThirdLevel() throws DescriptorValidationException, ClassNotFoundException, IllegalAccessException, InvalidProtocolBufferException {
        HandleWrapper handleWrapper = MessageCodec.buildSchemaV2(ThirdLevel.class);

        ThirdLevel thirdLevel = ThirdLevel.build();

        DynamicMessage dynamicMessage = MessageCodec.buildMessage(handleWrapper, thirdLevel);

        DynamicMessage dynamicMessage1 = DynamicMessage.parseFrom(
            handleWrapper.getSchema().getMessageDescriptor(handleWrapper.getTopTypeName()),
            dynamicMessage.toByteArray());

        ThirdLevel o = (ThirdLevel) MessageCodec.parseObject(dynamicMessage1, ThirdLevel.class);
        Assert.assertEquals(JSON.toJSONString(o), JSON.toJSONString(thirdLevel));
    }

    @Benchmark
    public void testNestedClass()
        throws DescriptorValidationException, ClassNotFoundException, IllegalAccessException, InvalidProtocolBufferException {
        MessageCodec.buildSchemaV2(User.class);
        HandleWrapper nestedUserHandleWrapper1 = MessageCodec.buildSchemaV2(NestedUser.class);

        NestedUser nestedUser = NestedUser.build();

        DynamicMessage dynamicMessage = MessageCodec.buildMessage(nestedUserHandleWrapper1, nestedUser);

        DynamicMessage dynamicMessage1 = DynamicMessage.parseFrom(
            nestedUserHandleWrapper1.getSchema().getMessageDescriptor(nestedUserHandleWrapper1.getTopTypeName()),
            dynamicMessage.toByteArray());

        NestedUser o = (NestedUser) MessageCodec.parseObject(dynamicMessage1, NestedUser.class);
        Assert.assertEquals(JSON.toJSONString(o), JSON.toJSONString(nestedUser));
    }

    @Benchmark
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


        DynamicMessage dynamicMessage = MessageCodec.buildMessage(handleWrapper, nestedUserList);
        DynamicMessage serializableData = DynamicMessage.parseFrom(
            handleWrapper.getSchema().getMessageDescriptor(handleWrapper.getTopTypeName()),
            dynamicMessage.toByteArray());

        Object o = MessageCodec.parseObject(serializableData, genericReturnType);

        Assert.assertEquals(JSON.toJSONString(o), JSON.toJSONString(nestedUserList, SerializerFeature.DisableCircularReferenceDetect));
    }


    @Benchmark
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

    @SneakyThrows
    public static void main(String[] args) {
        Options opt = new OptionsBuilder()
            .include(MessageCodecJMHTest.class.getSimpleName())
            .forks(1)
            .warmupIterations(2)
            .measurementIterations(5)
            .output("./Benchmark-Throughput.log")
            .build();

        new Runner(opt).run();
    }
}
