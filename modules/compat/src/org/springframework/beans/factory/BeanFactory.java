package org.springframework.beans.factory;

import org.springframework.beans.*;

public interface BeanFactory {
  public static final String FACTORY_BEAN_PREFIX = "&";

  public Object getBean(String name)
    throws BeansException;

  public Object getBean(String name, Class requiredType)
    throws BeansException;

  public Object getBean(String name, Object []args)
    throws BeansException;

  public boolean containsBean(String name);

  public boolean isSingleton(String name)
    throws NoSuchBeanDefinitionException;

  public boolean isPrototype(String name)
    throws NoSuchBeanDefinitionException;

  public boolean isTypeMatch(String name, Class targetType)
    throws NoSuchBeanDefinitionException;

  public Class getType(String name)
    throws NoSuchBeanDefinitionException;

  public String [] getAliases(String name)
    throws NoSuchBeanDefinitionException;
}
