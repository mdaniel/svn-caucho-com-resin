/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.module;

import java.lang.reflect.Method;
import javax.management.openmbean.*;


import com.caucho.quercus.function.*;
import com.caucho.quercus.expr.*;
import com.caucho.quercus.env.*;
import com.caucho.quercus.program.*;

/**
 * Class-loader specific context for loaded PHP.
 */
public class ResinModuleContext extends ModuleContext {
  /**
   * Constructor.
   */
  public ResinModuleContext()
  {
  }

  /**
   * Constructor.
   */
  public ResinModuleContext(ClassLoader loader)
  {
    super(loader);
  }

  @Override
  protected JavaClassDef createDefaultJavaClassDef(String className,
						   Class type)
  {
    System.out.println("CREATE: " + type);
    if (CompositeData.class.isAssignableFrom(type))
      return new CompositeDataClassDef(this, className, type);
    else
      return new JavaClassDef(this, className, type);
  }
}

