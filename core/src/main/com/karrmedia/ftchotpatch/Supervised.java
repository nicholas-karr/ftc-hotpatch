package org.firstinspires.ftc.robotcontroller.internal;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Supervised {
    String name() default "";

    String group() default "";

    boolean autonomous() default false;

    // What opmode will be activated immediately after this one completes
    String next() default "";
}
