package com.sprint.monew.global.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// controller parameter에서 RequestUserId 헤더값을 주입받기 위해 만든 어노테이션
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestUserId {

    // false면 헤더가 없을 때 null을 주입한다 (명세에 헤더가 없는 API용)
    boolean required() default true;
}
