/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.server.admin;

public class JmxInvokeQuery implements java.io.Serializable
{
  private String _name;
  private String _op;
  private Object []_args;
  private String []_sig;

  private JmxInvokeQuery()
  {
  }

  public JmxInvokeQuery(String name, String op,
			Object []args, String []sig)
  {
    _name = name;
    _op = op;
    _args = args;
    _sig = sig;

    if (_name == null || _op == null)
      throw new NullPointerException();
  }

  public String getName()
  {
    return _name;
  }

  public String getOp()
  {
    return _op;
  }

  public Object []getArgs()
  {
    return _args;
  }

  public String []getSig()
  {
    return _sig;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _name + "," + _op + "]";
  }
}
