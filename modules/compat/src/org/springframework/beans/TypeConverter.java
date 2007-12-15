package org.springframework.beans;

import org.springframework.core.*;

public interface TypeConverter
{
  public Object convertIfNecessary(Object value, Class requiredType)
    throws TypeMismatchException;

  public Object convertIfNecessary(Object value,
				   Class requiredType,
				   MethodParameter methodParam)
    throws TypeMismatchException;
}
