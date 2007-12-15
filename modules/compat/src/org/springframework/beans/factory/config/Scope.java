package org.springframework.beans.factory.config;

import org.springframework.beans.*;
import org.springframework.beans.factory.*;
import org.springframework.core.*;

public interface Scope
{
  public Object get(String name, ObjectFactory objectFactory);

  public Object remove(String name);

  public void registerDescrutionCallback(String name, Runnable callback);

  public String getConversationId();
}
