package org.springframework.beans.factory;

import org.springframework.beans.*;

public interface ObjectFactory {
  public Object getObject()
    throws BeansException;
}
