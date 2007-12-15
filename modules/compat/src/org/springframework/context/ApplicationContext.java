package org.springframework.context;

import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.*;
import org.springframework.core.io.*;
import org.springframework.core.io.support.*;

public interface ApplicationContext
  extends ListableBeanFactory, HierarchicalBeanFactory, MessageSource,
	  ApplicationEventPublisher, ResourcePatternResolver
{
  public ApplicationContext getParent();

  public String getDisplayName();

  public AutowireCapableBeanFactory getAutowireCapableBeanFactory();
  
  public long getStartupDate();
}
