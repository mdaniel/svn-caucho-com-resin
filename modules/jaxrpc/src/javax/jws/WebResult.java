package javax.jws;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface WebResult {

    boolean header() default false;
    String name() default "";
    String partName() default "";
    String targetNamespace() default "";

}

