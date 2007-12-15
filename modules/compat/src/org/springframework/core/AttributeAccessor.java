package org.springframework.core;

public interface AttributeAccessor
{
  public void setAttribute(String name, Object value);

  public Object getAttribute(String name);

  public Object removeAttribute(String name);

  public boolean hasAttribute(String name);

  public String []attributeNames();
}
