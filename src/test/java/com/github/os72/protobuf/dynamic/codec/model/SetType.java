package com.github.os72.protobuf.dynamic.codec.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author ironman
 * @date 2022/12/26 21:58
 * @desc
 */
public class SetType {

    private Set<User> userList;

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

    public static Set<User> buildUserSet() {
        User user = new User();
        Set<User> userList = new HashSet<>();
        userList.add(user);
        return userList;
    }
}
