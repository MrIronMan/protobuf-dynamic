package com.github.os72.protobuf.dynamic.codec.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.checkerframework.checker.units.qual.C;

/**
 * @author ironman
 * @date 2023/3/8 00:13
 * @desc
 */
public class CircularUser {

    private Integer id = 1;

    private int id1 = 2;

    // FIXME: 2023/3/8 无法进行初始化
//    private CircularUser self = new CircularUser();

    private List<CircularUser> selfList = Arrays.asList();

    private User user = new User();

    public static CircularUser build() {
        CircularUser circularUser = new CircularUser();
        return circularUser;
    }

    public List<CircularUser> getSelfList() {
        return selfList;
    }

    public void setSelfList(List<CircularUser> selfList) {
        this.selfList = selfList;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getId1() {
        return id1;
    }

    public void setId1(int id1) {
        this.id1 = id1;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
