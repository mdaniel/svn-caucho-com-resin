package org.springframework.beans.factory.config;

import org.springframework.beans.*;
import org.springframework.beans.factory.*;

public interface SingletonBeanRegistry
{
  public void registerSingleton(String beanName, Object singletonObject);
  
  public Object getSingleton(String beanName);
  
  public boolean containsSingleton(String beanName);

  public String []getSingletonNames();

  public int getSingletonCount();
}
