/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.module;

import com.caucho.quercus.program.CompositeDataClassDef;
import com.caucho.quercus.program.JavaArrayClassDef;
import com.caucho.quercus.program.JavaClassDef;
import com.caucho.quercus.program.ProStaticFunction;

import java.lang.reflect.Method;

import javax.management.openmbean.CompositeData;

import com.caucho.quercus.marshal.MarshalFactory;
import com.caucho.quercus.marshal.ProMarshalFactory;
import com.caucho.quercus.program.ProByteCodeFunctionFactory;
import com.caucho.quercus.program.ProByteCodeStaticFunction;
import com.caucho.quercus.expr.*;

/**
 * Class-loader specific context for loaded PHP.
 */
public class ProResinModuleContext extends ResinModuleContext {
  private ExprFactory _exprFactory;
  private MarshalFactory _marshalFactory;
  // private ProByteCodeFunctionFactory _functionFactory;

  /**
   * Constructor.
   */
  public ProResinModuleContext(ModuleContext parent, ClassLoader loader)
  {
    super(parent,loader);
    
    _marshalFactory = new ProMarshalFactory(this);
    _exprFactory = new ExprFactoryPro();
    // _functionFactory = new ProByteCodeFunctionFactory(this);
  }

  /**
   * Creates a static function.
   */
  @Override
  public StaticFunction createStaticFunction(QuercusModule module,
                                             Method method)
  {
    return new ProStaticFunction(this, module, method);
  }

  /**
   * Creates a static function.
   */
  /*
  public StaticFunction createStaticFunction(QuercusModule module,
					     Method method)
  {
    return _functionFactory.create(module, method);
  }
  */

  @Override
  public MarshalFactory getMarshalFactory()
  {
    return _marshalFactory;
  }

  @Override
  public ExprFactory getExprFactory()
  {
    return _exprFactory;
  }
}

