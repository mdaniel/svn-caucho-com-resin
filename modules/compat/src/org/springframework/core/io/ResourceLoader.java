package org.springframework.core.io;

import java.io.*;

public interface ResourceLoader {
  public static final String CLASSPATH_URL_PREFIX = "classpath:";

  public Resource getResource(String location);
  
  public ClassLoader getClassLoader();
}
