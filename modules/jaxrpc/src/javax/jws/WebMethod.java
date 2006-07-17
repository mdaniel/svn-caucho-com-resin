package javax.jws;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface WebMethod {
  String operationName() default "";
  String action() default "";
  boolean exclude() default false;
}

