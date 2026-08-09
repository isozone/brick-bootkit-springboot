package com.zqzqq.bootkits.sdk.annotation;

import java.lang.annotation.*;

/**
 * 测试用 @BrickServiceReference 注解。
 * <p>
 * 与 SDK 模块中的注解保持同名同包（全名一致），
 * 使 {@code BrickServiceReferenceInjector} 按类型全名匹配时能识别测试字段。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BrickServiceReference {

    /**
     * 服务接口
     */
    Class<?> value() default void.class;

    /**
     * 服务版本范围
     */
    String version() default "";

    /**
     * 是否可选（找不到服务时不报错）
     */
    boolean optional() default false;
}
