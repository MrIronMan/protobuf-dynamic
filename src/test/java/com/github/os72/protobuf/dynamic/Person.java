package com.github.os72.protobuf.dynamic;

/**
 * @author ironman
 * @date 2022/10/14 09:40
 * @desc
 */
public class Person {

    private Integer id;
    private String name;
    private String email;
    private String homeAddr;
    private String workAddr;
    private PhoneNumber phone;
    private Address add;

    public Address getAdd() {
        return add;
    }

    public void setAdd(Address add) {
        this.add = add;
    }

    public static enum PhoneType {
        MOBILE(0),
        HOME(1),
        WORK(2)
        ;

        PhoneType(Integer type) {
            this.type = type;
        }

        private Integer type;
    }

    public static class Address {
        private String street;
        private int num;

        public Address() {
        }

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public int getNum() {
            return num;
        }

        public void setNum(int num) {
            this.num = num;
        }
    }

    public static class PhoneNumber {
        public PhoneNumber() {
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public PhoneType getType() {
            return type;
        }

        public void setType(PhoneType type) {
            this.type = type;
        }

        private String number;
        private PhoneType type;
    }

    public Person() {
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getHomeAddr() {
        return homeAddr;
    }

    public void setHomeAddr(String homeAddr) {
        this.homeAddr = homeAddr;
    }

    public String getWorkAddr() {
        return workAddr;
    }

    public void setWorkAddr(String workAddr) {
        this.workAddr = workAddr;
    }

    public PhoneNumber getPhone() {
        return phone;
    }

    public void setPhone(PhoneNumber phone) {
        this.phone = phone;
    }
}
