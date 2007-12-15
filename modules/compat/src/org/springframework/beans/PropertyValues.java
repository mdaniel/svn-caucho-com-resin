package org.springframework.beans;

public interface PropertyValues {
  public PropertyValue []getPropertyValues();

  public PropertyValue getPropertyValue(String propertyName);

  public boolean contains(String propertyName);

  public boolean isEmpty();

  public PropertyValues changesSince(PropertyValues old);
}
