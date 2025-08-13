package com.easypan.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})  // 只能用在方法上
@Retention(RetentionPolicy.RUNTIME)  // 运行时可用反射读取
@Documented
public @interface RequiresAdmin {
}