package javax.jws;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface WebService {
  String name() default "";
  String targetNamespace() default "";
  String serviceName() default "";
  String wsdlLocation() default "";
  String endpointInterface() default "";
  String portName() default "";
};
