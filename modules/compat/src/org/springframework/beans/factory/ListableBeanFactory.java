package org.springframework.beans.factory;

import org.springframework.beans.*;

import java.util.*;

public interface ListableBeanFactory extends BeanFactory {
  public boolean containsBeanDefinition(String beanName);

  public int getBeanDefinitionCount();

  public String []getBeanDefinitionNames();

  public String []getBeanNamesForType(Class type);

  public String []getBeanNamesForType(Class type,
				      boolean includePrototypes,
				      boolean allowEagerInit);

  public Map getBeansOfType(Class type)
    throws BeansException;

  public Map getBeansOfType(Class type,
			    boolean includePrototypes,
			    boolean allowEagerInit)
    throws BeansException;
}
