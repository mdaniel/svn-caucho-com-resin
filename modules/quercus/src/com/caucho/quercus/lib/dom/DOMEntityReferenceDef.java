package com.caucho.quercus.lib.dom;

import com.caucho.quercus.module.ModuleContext;
import com.caucho.quercus.program.JavaClassDef;
import com.caucho.xml.QEntityReference;

import java.lang.reflect.Method;

public class DOMEntityReferenceDef
  extends JavaClassDef
{
  public DOMEntityReferenceDef(ModuleContext moduleContext, String name, Class type)
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

  public static QEntityReference __construct(String name)
  {
    return new QEntityReference(name);
  }
}
