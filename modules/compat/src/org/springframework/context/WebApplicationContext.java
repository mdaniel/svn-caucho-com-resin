package org.springframework.context;

import javax.servlet.*;

public interface WebApplicationContext extends ApplicationContext
{
  public static final String SCOPE_GLOBAL_SESSION = "globalSession";
  public static final String SCOPE_REQUEST = "request";
  public static final String SCOPE_SESSION = "session";

  public static final String ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE
    = "org.springframework.webapp.context";

  public ServletContext getServletContext();
}
