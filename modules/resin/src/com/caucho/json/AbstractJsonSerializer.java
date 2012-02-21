package com.caucho.json;

import java.io.IOException;

public abstract class AbstractJsonSerializer implements JsonSerializer
{
  @Override
  public void write(JsonOutput out, Object value) throws IOException
  {
    write(out, value, false);
  }
}
