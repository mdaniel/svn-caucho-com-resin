package org.springframework.beans.factory;

import org.springframework.beans.*;

public class BeanDefinitionStoreException extends FatalBeanException {
  private String _beanName;
  private String _description;
  
  public BeanDefinitionStoreException(String msg)
  {
    super(msg);
  }
  
  public BeanDefinitionStoreException(String description,
				      String msg)
  {
    super(msg);

    _description = description;
  }
  
  public BeanDefinitionStoreException(String description,
				      String beanName,
				      String msg)
  {
    super(msg);

    _beanName = beanName;
    _description = description;
  }
  
  public BeanDefinitionStoreException(String description,
				      String msg,
				      Throwable cause)
  {
    super(msg, cause);

    _description = description;
  }
  
  public BeanDefinitionStoreException(String description,
				      String beanName,
				      String msg,
				      Throwable cause)
  {
    super(msg, cause);

    _description = description;
    _beanName = beanName;
  }
  
  public BeanDefinitionStoreException(String msg,
				      Throwable cause)
  {
    super(msg, cause);
  }

  public String getBeanName()
  {
    return _beanName;
  }

  public String getResourceDescription()
  {
    return _description;
  }
}
