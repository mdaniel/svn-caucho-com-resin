package org.springframework.core.io.support;

import java.io.*;

import org.springframework.core.io.*;

public interface ResourcePatternResolver extends ResourceLoader {
  public static final String CLASSPATH_ALL_URL_PREFIX = "classpath*:";

  public Resource []getResources(String locationPattern)
    throws IOException;
}
