package com.easypan.common.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})  // 只能用在方法上
@Retention(RetentionPolicy.RUNTIME)  // 运行时可用反射读取
@Documented
public @interface RequiresLogin {
}