package org.springframework.beans.factory.config;

import java.beans.*;

import org.springframework.beans.*;
import org.springframework.beans.factory.*;
import org.springframework.util.*;

public interface ConfigurableBeanFactory
  extends HierarchicalBeanFactory, SingletonBeanRegistry
{
  public static final String SCOPE_PROTOTYPE = "prototype";
  public static final String SCOPE_SINGLETON = "singleton";

  public void setParentBeanFactory(BeanFactory parentBeanFactory)
    throws IllegalStateException;

  public void setBeanClassLoader(ClassLoader beanClassLoader);

  public ClassLoader getBeanClassLoader();

  public void setTempClassLoader(ClassLoader tempClassLoader);

  public ClassLoader getTempClassLoader();

  public void setCacheBeanMetadata(boolean cacheBeanMetadata);

  public boolean isCacheBeanMetadata();

  public void addPropertyEditorRegistrar(PropertyEditorRegistrar registrar);

  @Deprecated
    public void registerCustomEditor(Class requiredType,
				     PropertyEditor propertyEditor);

  public void setTypeConverter(TypeConverter typeConverter);

  public TypeConverter getTypeConverter();

  public void addBeanPostProcessor(BeanPostProcessor postProcessor);

  public int getBeanPostProcessorCount();

  public void registerScope(String scopeName, Scope scope);

  public String []getRegisteredScopeNames();

  public Scope getRegisteredScope(String scopeName);

  public void copyConfigurationFrom(ConfigurableBeanFactory other);

  public void registerAlias(String beanName, String alias)
    throws BeanDefinitionStoreException;

  public void resolveAliases(StringValueResolver valueResolver);

  public BeanDefinition getMergedBeanDefinition(String beanName)
    throws NoSuchBeanDefinitionException;

  public boolean isFactoryBean(String name)
    throws NoSuchBeanDefinitionException;

  public boolean isCurrentlyInCreation(String beanName);

  public void registerDependentBean(String beanName, String dependentBeanName);

  public String []getDependentBeans(String beanName);

  public String []getDependenciesForBean(String beanName);

  public void destroyBean(String beanName, Object beanInstance);

  public void destroyScopedBean(String beanName);

  public void destroySingletons();
}
