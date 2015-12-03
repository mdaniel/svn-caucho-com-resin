package com.caucho.json.ser;

import com.caucho.json.JsonOutput;

import java.io.IOException;

public abstract class AbstractJsonSerializer<T> implements JsonSerializer<T>
{
  @Override
  public void write(JsonOutput out, T value) throws IOException
  {
    write(out, value, false);
  }
}
