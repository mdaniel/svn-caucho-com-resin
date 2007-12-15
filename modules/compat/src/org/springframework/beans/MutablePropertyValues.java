package org.springframework.beans;

public class MutablePropertyValues
  implements PropertyValues, java.io.Serializable
{
  public MutablePropertyValues()
  {
    throw new UnsupportedOperationException();
  }
  
  public PropertyValue []getPropertyValues()
  {
    throw new UnsupportedOperationException();
  }

  public PropertyValue getPropertyValue(String propertyName)
  {
    throw new UnsupportedOperationException();
  }

  public boolean contains(String propertyName)
  {
    throw new UnsupportedOperationException();
  }

  public boolean isEmpty()
  {
    throw new UnsupportedOperationException();
  }

  public PropertyValues changesSince(PropertyValues old)
  {
    throw new UnsupportedOperationException();
  }
}
