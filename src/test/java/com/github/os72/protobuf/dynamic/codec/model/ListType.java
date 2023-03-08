package com.github.os72.protobuf.dynamic.codec.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ironman
 * @date 2022/12/26 21:58
 * @desc
 */
public class ListType {

    private List<User> userList;

    private List<NestedUser> nestedUserList;

    public static List<NestedUser> buildNestedUserList() {
        NestedUser nestedUser = new NestedUser();
        nestedUser.getAddressList().add("dddd");
        nestedUser.getIdsList().add(1234L);
        nestedUser.getMap().put("key123", "value123");
        List<NestedUser> nestedUserList = new ArrayList<>();
        nestedUserList.add(nestedUser);
        nestedUserList.add(nestedUser);
        return nestedUserList;
    }

    public static List<User> buildUserList() {
        User user = new User();
        List<User> userList = new ArrayList<>();
        userList.add(user);
        userList.add(user);
        return userList;
    }
}
