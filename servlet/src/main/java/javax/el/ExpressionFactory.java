/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package javax.el;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.lang.reflect.*;

/**
 * Represents an EL expression factory
 */
public abstract class ExpressionFactory 
{
  public abstract Object coerceToType(Object obj,
                                      Class<?> targetType)
    throws ELException;

  public abstract MethodExpression
    createMethodExpression(ELContext context,
                           String expression,
                           Class<?> expectedReturnType,
                           Class<?> []expectedParamTypes)
    throws ELException;

  public abstract ValueExpression
    createValueExpression(ELContext context,
                          String expression,
                          Class<?> expectedType)
    throws ELException;

  public abstract ValueExpression
    createValueExpression(Object instance,
                          Class<?> expectedType)
    throws ELException;
  
  public abstract ELResolver getStreamELResolver();
  
  public abstract Map<String, Method> getInitFunctionMap();

  public static ExpressionFactory newInstance()
  {
    return newInstance(null);
  }

  public static ExpressionFactory newInstance(Properties properties)
  {
    return (ExpressionFactory) 
      DelegateFactory.newInstance(ExpressionFactory.class,
                                  "com.caucho.v5.el.ExpressionFactoryImpl",
                                  properties);
  }

  public String toString()
  {
    return this.getClass().getSimpleName() + "[]";
  }
}
