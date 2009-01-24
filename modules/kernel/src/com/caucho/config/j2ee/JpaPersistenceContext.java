package com.caucho.config.j2ee;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import java.lang.annotation.*;

import javax.inject.BindingType;

/**
 * JpaPersistenceContext is a binding type for the JPA EntityManager, letting
 * you select the context by its name and allowing the extended value to
 * be selected.
 */
@BindingType
@Documented
@Target({TYPE,FIELD,METHOD,PARAMETER})
@Retention(RUNTIME)
public @interface JpaPersistenceContext {
  /**
   * The unitName of the JPA Persistence Context
   */
  public String value();

  /**
   * If true, return the extended persistence context, defaults to false.
   */
  public boolean extended() default false;
}
