package com.github.os72.protobuf.dynamic.check;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ironman
 * @date 2023/7/27 11:21
 * desc
 *  标记一个类是由 Protostuff 进行序列化的类
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ProtostuffSerializationClass {

    /**
     * 是否是首次生成，如果是首次生成，则需要在编译是改成 true，编译完成之后在改成 false
     *
     * @return
     */
    boolean firstGenerate() default false;

    /**
     * 项目根路径下配置文件目录所在的位置，例如：该类在 /api/api-server 的 src/main/resources/ 目录下
     * 则 {@link ProtostuffSerializationClass#configPath()} 配置为 /api/api-server，请保证正确，否则无法读取对应位置的文件
     * 如果直接在更目录下，则直接配置为 /
     *
     * @return
     */
    String configPath();
}
