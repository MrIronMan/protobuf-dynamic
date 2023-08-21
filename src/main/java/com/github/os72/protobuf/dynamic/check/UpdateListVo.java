package com.github.os72.protobuf.dynamic.check;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;

/**
 * 数据操作分类列表。
 *
 */
@Getter
@AllArgsConstructor
public class UpdateListVo<T> {

    /**
     * 删除列表
     */
    private List<T>          deleteList;
    /**
     * 新增列表
     */
    private List<T>          insertList;
    /**
     * 修改列表，left为旧值，right为新值
     */
    private List<Pair<T, T>> updateList;

}
