package com.caucho.quercus.lib.dom;

import com.caucho.quercus.module.ModuleContext;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.program.JavaClassDef;
import com.caucho.xml.QAttr;
import com.caucho.xml.QName;

import java.lang.reflect.Method;

public class DOMAttrDef
  extends JavaClassDef
{
  public DOMAttrDef(ModuleContext moduleContext, String name, Class type)
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

  public static QAttr __construct(String name,
                                  @Optional String textContent)
  {
    QName qName = new QName(name);

    return new QAttr(qName, textContent);
  }
}
