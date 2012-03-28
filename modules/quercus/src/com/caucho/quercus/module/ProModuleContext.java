/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.module;

import java.lang.reflect.Method;

import com.caucho.quercus.expr.ExprFactory;
import com.caucho.quercus.expr.ExprFactoryPro;
import com.caucho.quercus.marshal.MarshalFactory;
import com.caucho.quercus.marshal.ProMarshalFactory;
import com.caucho.quercus.program.ProByteCodeFunctionFactory;
import com.caucho.quercus.program.ProStaticFunction;

/**
 * Class-loader specific context for loaded PHP.
 */
public class ProModuleContext extends ModuleContext {
  private ExprFactory _exprFactory;
  private MarshalFactory _marshalFactory;
  // private ProByteCodeFunctionFactory _functionFactory;

  /**
   * Constructor.
   */
  public ProModuleContext(ModuleContext parent, ClassLoader loader)
  {
    super(parent,loader);
    
    _marshalFactory = new ProMarshalFactory(this);
    _exprFactory = new ExprFactoryPro();
    // _functionFactory = new ProFunctionFactory(this);
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

