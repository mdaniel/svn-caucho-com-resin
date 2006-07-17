package javax.jws.soap;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * XXX: temp for compile only, please replace.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SOAPBinding {
  public enum Style {
    DOCUMENT,
    RPC
  };
  
  public enum Use {
    LITERAL,
    ENCODED
  };

  public enum ParameterStyle {
    BARE,
    WRAPPED
  };

  Style style() default Style.DOCUMENT;
  Use use() default Use.LITERAL;
  ParameterStyle parameterStyle() default ParameterStyle.WRAPPED;
};

