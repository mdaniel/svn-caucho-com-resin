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
 * @author Paul Cowan
 */

package javax.el;

import java.util.*;

/**
 * Represents a Lamdba expression
 */
public class LambdaExpression
{
  private final List<String> _formalParameters;
  private final ValueExpression _expression;

  private ELContext _elContext;
  private ArrayList<Map<String,Object>> _lambdaStack;

  public LambdaExpression(List<String> formalParameters,
                          ValueExpression expression)
  {
    _formalParameters = formalParameters;
    _expression = expression;
  }

  public void setELContext(ELContext elContext)
  {
    _elContext = elContext;
    
    _lambdaStack = elContext.getLambdaStack();
    
    if (_lambdaStack != null) {
      _lambdaStack = new ArrayList<Map<String,Object>>(_lambdaStack);
    }
  }

  public ELContext getELContext()
  {
    return _elContext;
  }

  public Object invoke(ELContext elContext, Object... args)
    throws ELException
  {
    int size = _formalParameters.size();

    if (args.length < size) {
      throw new ELException("expected " + size + " args but saw only " + args.length + " args");
    }
    
    ArrayList<Map<String,Object>> stack = _lambdaStack;
    int stackSize = 0;
    
    if (stack != null) {
      stackSize = stack.size();
      
      for (int i = 0; i < stackSize; i++) {
        Map<String,Object> scope = stack.get(i);
        
        elContext.enterLambdaScope(scope);
      }
    }

    HashMap<String,Object> paramsMap = new HashMap<>();
    
    for (int i = 0; i < size; i++) {
      paramsMap.put(_formalParameters.get(i), args[i]);
    }
    
    elContext.enterLambdaScope(paramsMap);

    try {
      
      return _expression.getValue(elContext);
    } finally {
      elContext.exitLambdaScope();
      
      for (int i = 0; i < stackSize; i++) {
        elContext.exitLambdaScope();
      }
    }
  }

  public Object invoke(Object... args)
  {
    return invoke(_elContext, args);
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    if (_formalParameters == null || _formalParameters.isEmpty()) {
      sb.append("()");
    }
    else if (_formalParameters.size() == 1) {
      sb.append(_formalParameters.get(0));
    }
    else {
      sb.append("(");
      boolean isFirst = true;
      for (String param : _formalParameters) {
        if (! isFirst)
          sb.append(",");
        sb.append(param);
        isFirst = false;
      }
      sb.append(")");
    }
    sb.append("->");
    sb.append(_expression);
    return sb.toString();
  }
}
