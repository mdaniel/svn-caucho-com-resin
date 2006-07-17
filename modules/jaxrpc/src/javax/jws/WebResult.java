package javax.jws;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * XXX: temp for compile only, please replace.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface WebResult {
  String name() default "";
  String partName() default "";
  String targetNamespace() default "";
  boolean header() default false;
};

