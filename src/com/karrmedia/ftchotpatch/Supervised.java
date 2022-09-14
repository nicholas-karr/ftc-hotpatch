package com.karrmedia.ftchotpatch;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Supervised {
    // Human-readable name visible in the Driver Station
    String name() default "";

    // Optional group for this OpMode in the Driver Station list
    String group() default "";

    // Controls whether this OpMode will have access to controllers and what tab it will appear in
    boolean autonomous() default false;

    // What OpMode will be automatically selected after this one completes
    // Note: The next OpMode must be manually initialized and started
    String next() default "";

    // What variations can be made to this OpMode
    // A version of this class will be generated with a variation
    // replacing any question marks in the name, per variation
    // Example: variations={"Red", "Blue"} for the name ?TeleOp to generate RedTeleOp and BlueTeleOp
    String[] variations() default { };
}
