package org.springframework.beans;

import org.springframework.core.*;

public class BeansException extends NestedRuntimeException {
  public BeansException(String msg)
  {
    super(msg);
  }
  
  public BeansException(String msg, Throwable e)
  {
    super(msg, e);
  }
}
