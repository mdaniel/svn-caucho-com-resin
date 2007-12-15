package org.springframework.core.io;

import java.net.*;
import java.io.*;

public interface InputStreamSource {
  public InputStream getInputStream()
    throws IOException;
}
