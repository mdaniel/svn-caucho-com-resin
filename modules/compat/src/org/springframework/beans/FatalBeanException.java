package org.springframework.beans;

import org.springframework.core.*;

public class FatalBeanException extends BeansException {
  public FatalBeanException(String msg)
  {
    super(msg);
  }
  
  public FatalBeanException(String msg, Throwable e)
  {
    super(msg, e);
  }
}
