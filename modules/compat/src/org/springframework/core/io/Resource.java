package org.springframework.core.io;

import java.net.*;
import java.io.*;

public interface Resource extends InputStreamSource {
  public boolean exists();

  public boolean isOpen();

  public URL getURL()
    throws IOException;

  public URI getURI()
    throws IOException;

  public File getFile()
    throws IOException;

  public Resource createRelative(String relativePath)
    throws IOException;

  public String getFilename();
  
  public String getDescription();
}
