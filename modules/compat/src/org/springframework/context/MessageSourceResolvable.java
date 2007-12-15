package org.springframework.context;

public interface MessageSourceResolvable {
  public String []getCodes();

  public Object []getArguments();

  public String getDefaultMessage();
}
