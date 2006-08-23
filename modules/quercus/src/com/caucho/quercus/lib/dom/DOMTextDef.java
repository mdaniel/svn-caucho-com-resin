package com.caucho.quercus.lib.dom;

import com.caucho.quercus.program.JavaClassDef;
import com.caucho.quercus.module.ModuleContext;
import com.caucho.quercus.module.Optional;
import com.caucho.xml.QText;

import java.lang.reflect.Method;

public class DOMTextDef
  extends JavaClassDef
{
  public DOMTextDef(ModuleContext moduleContext, String name, Class type)
  {
    super(moduleContext, name, type);

  }

  public synchronized void introspect()
  {
    super.introspect();

    try {
      Method method = getClass().getMethod("__construct", String.class);
      setCons(method);
    }
    catch (NoSuchMethodException ex) {
      throw new AssertionError(ex);
    }
  }

  public static QText __construct(@Optional String content)
  {
    return new QText(content);
  }
}
