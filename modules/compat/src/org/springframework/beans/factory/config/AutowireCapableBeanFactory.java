package org.springframework.beans.factory.config;

import org.springframework.beans.*;
import org.springframework.beans.factory.*;
import org.springframework.core.*;

import java.util.*;

public interface AutowireCapableBeanFactory
  extends BeanFactory
{
  public static final int AUTOWIRE_AUTODETECT = 4;
  public static final int AUTOWIRE_BY_NAME = 1;
  public static final int AUTOWIRE_BY_TYPE = 2;
  public static final int AUTOWIRE_CONSTRUCTOR = 3;
  public static final int AUTOWIRE_NO = 0;

  public Object createBean(Class cl)
    throws BeansException;

  public Object createBean(Class cl,
			   int autowireMode,
			   boolean dependencyCheck)
    throws BeansException;

  public Object autowire(Class beanClass,
			 int autowireMode,
			 boolean dependencyCheck)
    throws BeansException;

  public void autowireBeanProperties(Object existingBean,
				     int autowireMode,
				     boolean dependencyCheck)
    throws BeansException;

  public void applyBeanPropertyValues(Object existingBean,
				      String beanName)
    throws BeansException;

  public Object configureBean(Object existingBean,
			      String beanName)
    throws BeansException;

  public Object initializeBean(Object existingBean,
			       String beanName)
    throws BeansException;

  public Object
    applyBeanPostProcessorsBeforeInitialization(Object existingBean,
						String beanName)
    throws BeansException;

  public Object
    applyBeanPostProcessorsAfterInitialization(Object existingBean,
					       String beanName)
    throws BeansException;

  public Object resolveDependency(DependencyDescriptor descriptor,
				  String beanName)
    throws BeansException;

  public Object resolveDependency(DependencyDescriptor descriptor,
				  String beanName,
				  Set autowiredBeanNames,
				  TypeConverter typeConverter)
    throws BeansException;
}
