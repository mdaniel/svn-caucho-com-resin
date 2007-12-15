package org.springframework.core;

public abstract class AttributeAccessorSupport
  implements AttributeAccessor, java.io.Serializable
{
  public void setAttribute(String name, Object value)
  {
    throw new UnsupportedOperationException();
  }

  public Object getAttribute(String name)
  {
    throw new UnsupportedOperationException();
  }

  public Object removeAttribute(String name)
  {
    throw new UnsupportedOperationException();
  }

  public boolean hasAttribute(String name)
  {
    throw new UnsupportedOperationException();
  }

  public String []attributeNames()
  {
    throw new UnsupportedOperationException();
  }
}
