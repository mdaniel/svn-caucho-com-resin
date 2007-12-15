package org.springframework.beans.factory.config;

import org.springframework.beans.*;
import org.springframework.beans.factory.*;
import org.springframework.core.*;

public interface BeanDefinition
  extends AttributeAccessor, BeanMetadataElement
{
  public static final int ROLE_APPLICATION = 0;
  public static final int ROLE_SUPPORT = 1;
  public static final int ROLE_INFRASTRUCTURE = 1;
  
  public static final String SCOPE_PROTOTYPE = "prototype";
  public static final String SCOPE_SINGLETON = "singleton";

  public String getParentName();

  public void setParentName(String parentName);

  public String getBeanClassName();

  public void setBeanClassName(String beanClassName);

  public String getFactoryBeanName();

  public void setFactoryBeanName(String factoryBeanName);

  public String getFactoryMethodName();

  public void setFactoryMethodName(String factoryMethodName);

  public String getScope();

  public void setScope(String scope);

  public boolean isAutowireCandidate();

  public void setAutowireCandidate(boolean isAutowire);

  public ConstructorArgumentValues getConstructorArgumentValue();

  public MutablePropertyValues getPropertyValues();

  public boolean isSingleton();

  public boolean isAbstract();

  public boolean isLazyInit();

  public String getResourceDescription();

  public int getRole();
}
