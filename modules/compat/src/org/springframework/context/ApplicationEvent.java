package org.springframework.context;

import java.util.*;

public abstract class ApplicationEvent extends EventObject {
  private long _timestamp;
  
  protected ApplicationEvent(Object source)
  {
    super(source);

    _timestamp = System.currentTimeMillis();
  }

  public final long getTimestamp()
  {
    return _timestamp;
  }
}
