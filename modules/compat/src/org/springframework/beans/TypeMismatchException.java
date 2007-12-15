package org.springframework.beans;

import org.springframework.core.*;

public class TypeMismatchException extends PropertyAccessException {
  public static final String ERROR_CODE = "error";

  private String _errorCode;
  private Class _requiredType;
  
  public TypeMismatchException(Object value, Class requiredType)
  {
    super(value + " must be " + requiredType, null);
    
    _requiredType = requiredType;
  }
}
