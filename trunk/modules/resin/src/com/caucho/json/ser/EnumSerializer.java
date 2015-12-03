package com.caucho.json.ser;

import com.caucho.json.JsonOutput;

import java.io.IOException;

public class EnumSerializer extends AbstractJsonSerializer<Enum>
{
  static final JsonSerializer SER = new EnumSerializer();

  @Override 
  public void write(JsonOutput out, Enum value, boolean annotated)
    throws IOException
  {
    out.writeString(value.name());
  }
}
