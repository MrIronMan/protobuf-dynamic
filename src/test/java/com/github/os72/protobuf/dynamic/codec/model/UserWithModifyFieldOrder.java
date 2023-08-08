package com.github.os72.protobuf.dynamic.codec.model;

import java.util.Date;

/**
 * @author ironman
 * @date 2022/12/21 20:54
 * @desc
 */
public class UserWithModifyFieldOrder {

    private float f = 3.0f;

    private double d = 4.000D;

    private Integer id = 1;

    private int id1 = 2;



    private long l = 5L;

//    private byte b;// = Byte.parseByte("6");

    private boolean bol = false;

    private char c = 'c';

    private Float f1 = 7F;

    private Double d1 = 8.0D;

//    private Byte b1;// = 4;

    private Long l1 = 9L;

    private Character c1 = 'c';

    private UserType type = UserType.NORMAL;

    private String username;

    private CircularUser circularUser;

    private Date date = new Date();

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public UserWithModifyFieldOrder() {
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public float getF() {
        return f;
    }

    public void setF(float f) {
        this.f = f;
    }

    public double getD() {
        return d;
    }

    public void setD(double d) {
        this.d = d;
    }

    public long getL() {
        return l;
    }

    public void setL(long l) {
        this.l = l;
    }

//    public byte getB() {
//        return b;
//    }
//
//    public void setB(byte b) {
//        this.b = b;
//    }

    public boolean isBol() {
        return bol;
    }

    public void setBol(boolean bol) {
        this.bol = bol;
    }

    public char getC() {
        return c;
    }

    public void setC(char c) {
        this.c = c;
    }

    public Float getF1() {
        return f1;
    }

    public void setF1(Float f1) {
        this.f1 = f1;
    }

    public Double getD1() {
        return d1;
    }

    public void setD1(Double d1) {
        this.d1 = d1;
    }

//    public Byte getB1() {
//        return b1;
//    }
//
//    public void setB1(Byte b1) {
//        this.b1 = b1;
//    }

    public Long getL1() {
        return l1;
    }

    public void setL1(Long l1) {
        this.l1 = l1;
    }

    public Character getC1() {
        return c1;
    }

    public void setC1(Character c1) {
        this.c1 = c1;
    }

    public UserType getType() {
        return type;
    }

    public void setType(UserType type) {
        this.type = type;
    }
}
