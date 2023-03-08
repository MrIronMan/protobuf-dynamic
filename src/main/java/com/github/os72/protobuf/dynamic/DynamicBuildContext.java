package com.github.os72.protobuf.dynamic;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ironman
 * @date 2023/3/8 19:48
 * @desc
 */

public class DynamicBuildContext {

    /**
     * 保存构建中的对象的类名
     */
    private List<String> buildingList = new ArrayList<>();

    /**
     * 已添加至 schema 中的类名
     */
    private List<String> addedList = new ArrayList<>();

    /**
     * 需要构建的列表
     */
    private List<String> needBuildList = new ArrayList<>();

    public List<String> getBuildingList() {
        return buildingList;
    }

    public void setBuildingList(List<String> buildingList) {
        this.buildingList = buildingList;
    }

    public List<String> getAddedList() {
        return addedList;
    }

    public void setAddedList(List<String> addedList) {
        this.addedList = addedList;
    }

    public List<String> getNeedBuildList() {
        return needBuildList;
    }

    public void setNeedBuildList(List<String> needBuildList) {
        this.needBuildList = needBuildList;
    }
}
