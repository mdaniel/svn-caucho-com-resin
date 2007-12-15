package org.springframework.core;

public class NestedRuntimeException extends RuntimeException {
  public NestedRuntimeException(String msg)
  {
    super(msg);
  }
  
  public NestedRuntimeException(String msg, Throwable e)
  {
    super(msg, e);
  }

  public boolean contains(Class exClass)
  {
    return (exClass.isAssignableFrom(getClass())
	    || (getCause() != null
		&& exClass.isAssignableFrom(getCause().getClass())));
  }
}
