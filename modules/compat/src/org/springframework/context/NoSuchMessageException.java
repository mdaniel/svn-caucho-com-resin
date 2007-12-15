package org.springframework.context;

import java.util.*;

public class NoSuchMessageException extends RuntimeException {
  public NoSuchMessageException(String code)
  {
    super(code);
  }
  
  public NoSuchMessageException(String code, Locale locale)
  {
    super(code + " for " + locale);
  }
}
