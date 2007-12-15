package org.springframework.beans.factory;

import org.springframework.beans.*;

public interface HierarchicalBeanFactory extends BeanFactory {
  public boolean containsLocalBean(String name);

  public BeanFactory getParentBeanFactory();
}
