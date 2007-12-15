package org.springframework.context;

import java.util.Locale;

public interface MessageSource {
  public String getMessage(String code,
			   Object []args,
			   String defaultMessage,
			   Locale locale);

  public String getMessage(String code,
			   Object []args,
			   Locale locale)
    throws NoSuchMessageException;

  public String getMessage(MessageSourceResolvable resolvable,
			   Locale locale)
    throws NoSuchMessageException;
}
