package com.github.os72.protobuf.dynamic.check;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import org.apache.commons.lang3.tuple.Pair;

/**
 * 对比新旧列表，拆分出对应的操作列表。
 */
public class UpdateUtils {

    /**
     * 对比新旧列表，拆分出对应的操作列表。
     *
     * @param oldList 旧列表
     * @param newList 新列表
     * @param <T>     数据类型
     * @return 包含数据操作列表的VO对象
     */
    public static <T> UpdateListVo<T> split(List<T> oldList, List<T> newList,
                                            BiFunction<T, T, Boolean> equalsFunction) {
        List<T> innerOldList = oldList == null ? Collections.emptyList() : oldList;
        List<T> innerNewList = newList == null ? Collections.emptyList() : newList;

        if (innerOldList.isEmpty()) {
            return new UpdateListVo<>(Collections.emptyList(), innerNewList,
                    Collections.emptyList());
        } else if (innerNewList.isEmpty()) {
            return new UpdateListVo<>(innerOldList, Collections.emptyList(),
                    Collections.emptyList());
        }

        List<Pair<T, T>> updateList = new ArrayList<>();
        List<T> deleteList = new ArrayList<>();
        List<T> insertList = new ArrayList<>();

        Cache<String, Boolean> cache = CacheBuilder.newBuilder()
                .initialCapacity(Math.min(100, innerNewList.size() * innerNewList.size()))
                .maximumSize(Math.min(100, innerNewList.size() * innerNewList.size())).build();

        for (T oldItem : innerOldList) {
            boolean findInNewList = false;
            for (T newItem : innerNewList) {
                if (equalsCache(oldItem, newItem, equalsFunction, cache)) {
                    updateList.add(Pair.of(oldItem, newItem));
                    findInNewList = true;
                    break;
                }
            }
            if (!findInNewList) {
                deleteList.add(oldItem);
            }
        }

        for (T newItem : innerNewList) {
            boolean findInOldList = false;
            for (T oldItem : innerOldList) {
                if (equalsCache(oldItem, newItem, equalsFunction, cache)) {
                    findInOldList = true;
                    break;
                }
            }
            if (!findInOldList) {
                insertList.add(newItem);
            }
        }

        return new UpdateListVo<>(deleteList, insertList, updateList);
    }

    private static <T> boolean equalsCache(T a, T b, BiFunction<T, T, Boolean> equalsFunction,
                                           Cache<String, Boolean> cache) {
        String key = a.hashCode() + "@" + b.hashCode();
        Boolean result = cache.getIfPresent(key);
        if (result != null) {
            return result;
        }

        boolean apply = equalsFunction.apply(a, b);
        cache.put(key, apply);
        return apply;
    }


    /**
     * 直接指定method
     * @param oldList
     * @param newList
     * @param equalsMethod
     * @param <T>
     * @return
     */
    public static <T> UpdateListVo<T> split(List<T> oldList, List<T> newList, Method equalsMethod) {
        List<T> innerOldList = oldList == null ? Collections.emptyList() : oldList;
        List<T> innerNewList = newList == null ? Collections.emptyList() : newList;

        if (innerOldList.isEmpty()) {
            return new UpdateListVo<>(Collections.emptyList(), innerNewList,
                Collections.emptyList());
        } else if (innerNewList.isEmpty()) {
            return new UpdateListVo<>(innerOldList, Collections.emptyList(),
                Collections.emptyList());
        }

        List<Pair<T, T>> updateList = new ArrayList<>();
        List<T> deleteList = new ArrayList<>();
        List<T> insertList = new ArrayList<>();

        Cache<String, Boolean> cache = CacheBuilder.newBuilder()
            .initialCapacity(Math.min(100, innerNewList.size() * innerNewList.size()))
            .maximumSize(Math.min(100, innerNewList.size() * innerNewList.size())).build();

        for (T oldItem : innerOldList) {
            boolean findInNewList = false;
            for (T newItem : innerNewList) {
                if (equalsCache(oldItem, newItem, equalsMethod, cache)) {
                    updateList.add(Pair.of(oldItem, newItem));
                    findInNewList = true;
                    break;
                }
            }
            if (!findInNewList) {
                deleteList.add(oldItem);
            }
        }

        for (T newItem : innerNewList) {
            boolean findInOldList = false;
            for (T oldItem : innerOldList) {
                if (equalsCache(oldItem, newItem, equalsMethod, cache)) {
                    findInOldList = true;
                    break;
                }
            }
            if (!findInOldList) {
                insertList.add(newItem);
            }
        }

        return new UpdateListVo<>(deleteList, insertList, updateList);
    }


    private static <T> boolean equalsCache(T a, T b, Method equalsMethod,
        Cache<String, Boolean> cache) {
        String key = a.hashCode() + "@" + b.hashCode();
        Boolean result = cache.getIfPresent(key);
        if (result != null) {
            return result;
        }

        boolean apply = false;
        try {
            apply = (boolean) equalsMethod.invoke(a, b);
        } catch (Exception e) {
            throw new RuntimeException("比较工具调用失败", e);
        }
        cache.put(key, apply);
        return apply;
    }
}