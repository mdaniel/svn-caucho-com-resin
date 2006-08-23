package com.caucho.quercus.lib.dom;

import com.caucho.quercus.program.JavaClassDef;
import com.caucho.quercus.module.ModuleContext;
import com.caucho.quercus.module.Optional;
import com.caucho.xml.QProcessingInstruction;

import java.lang.reflect.Method;

public class DOMProcessingInstructionDef
  extends JavaClassDef
{
  public DOMProcessingInstructionDef(ModuleContext moduleContext, String name, Class type)
  {
    super(moduleContext, name, type);

  }

  public synchronized void introspect()
  {
    super.introspect();

    try {
      Method method = getClass().getMethod("__construct",
                                           String.class,
                                           String.class);
      setCons(method);
    }
    catch (NoSuchMethodException ex) {
      throw new AssertionError(ex);
    }
  }

  public static QProcessingInstruction __construct(String name,
                                                   @Optional String content)
  {
    return new QProcessingInstruction(name, content);
  }
}
