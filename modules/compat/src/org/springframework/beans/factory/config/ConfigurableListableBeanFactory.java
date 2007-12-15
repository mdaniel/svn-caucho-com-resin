package org.springframework.beans.factory.config;

import org.springframework.beans.*;
import org.springframework.beans.factory.*;

public interface ConfigurableListableBeanFactory
  extends ListableBeanFactory,
	  AutowireCapableBeanFactory,
	  ConfigurableBeanFactory
{
  public void ignoreDependencyType(Class type);

  public void ignoreDependencyInterface(Class ifc);

  public void registerResolvableDependency(Class dependencyType,
					   Object autowiredValue);

  public boolean isAutowireCandidate(String beanName,
				     DependencyDescriptor descriptor)
    throws NoSuchBeanDefinitionException;

  public boolean isPrimary(String beanName, Object beanInstance);

  public BeanDefinition getBeanDefinition(String beanName)
    throws NoSuchBeanDefinitionException;

  public void preInstantiateSingletons()
    throws BeansException;
}
