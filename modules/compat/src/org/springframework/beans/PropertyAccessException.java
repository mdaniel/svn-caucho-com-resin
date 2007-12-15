package org.springframework.beans;

import java.beans.*;

import org.springframework.core.*;

public class PropertyAccessException extends BeansException {
  private PropertyChangeEvent _event;

  public PropertyAccessException(PropertyChangeEvent event,
				 String msg,
				 Throwable cause)
  {
    super(msg, cause);

    _event = event;
  }
  
  public PropertyAccessException(String msg, Throwable cause)
  {
    super(msg, cause);
  }

  public PropertyChangeEvent getPropertyChangeEvent()
  {
    return _event;
  }
}
