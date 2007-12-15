package org.springframework.beans.factory;

import org.springframework.beans.*;

public class NoSuchBeanDefinitionException extends BeansException {
  private String _name;
  private Class _type;
  
  public NoSuchBeanDefinitionException(Class type, String msg)
  {
    super(msg);

    _type = type;
  }
  
  public NoSuchBeanDefinitionException(String name)
  {
    super(name);

    _name = name;
  }
  
  public NoSuchBeanDefinitionException(String name, String msg)
  {
    super(msg);

    _name = name;
  }

  public String getBeanName()
  {
    return _name;
  }

  public Class getBeanType()
  {
    return _type;
  }
}
