package com.github.os72.protobuf.dynamic.codec.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ironman
 * @date 2022/12/21 21:51
 * @desc
 */
public class NestedUser {

    private Integer id = 100000;

    private String name = "Mark-1";

    private User user = new User();

    List<String> addressList = new ArrayList<>();

    List<Long> idsList = new ArrayList<>();

    Map<String, String> map = new HashMap<>();

    List<User> babyList = new ArrayList<>();

    public static NestedUser build() {
        NestedUser nestedUser = new NestedUser();
        nestedUser.getAddressList().add("dddd");
        nestedUser.getIdsList().add(1234L);
        nestedUser.getMap().put("key123", "value123");
        return nestedUser;
    }

    public NestedUser() {
//        babyList.add(new User());
//        babyList.add(new User());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<String> getAddressList() {
        return addressList;
    }

    public void setAddressList(List<String> addressList) {
        this.addressList = addressList;
    }

    public List<Long> getIdsList() {
        return idsList;
    }

    public void setIdsList(List<Long> idsList) {
        this.idsList = idsList;
    }

    public Map<String, String> getMap() {
        return map;
    }

    public void setMap(Map<String, String> map) {
        this.map = map;
    }

    public List<User> getBabyList() {
        return babyList;
    }

    public void setBabyList(List<User> babyList) {
        this.babyList = babyList;
    }
}
