package org.springframework.beans.factory;

public interface DisposableBean {
  public void destroy()
    throws Exception;
}
