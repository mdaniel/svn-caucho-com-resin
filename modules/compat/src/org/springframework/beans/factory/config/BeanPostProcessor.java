package org.springframework.beans.factory.config;

import org.springframework.beans.*;
import org.springframework.beans.factory.*;
import org.springframework.core.*;

public interface BeanPostProcessor
{
  public Object postProcessBeforeInitialization(Object bean,
						String beanName)
    throws BeansException;
  
  public Object postProcessAfterInitialization(Object bean,
					       String beanName)
    throws BeansException;
}
