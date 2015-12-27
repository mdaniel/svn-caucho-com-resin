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

public abstract class ELContext 
{
  private static final Logger log = 
    Logger.getLogger(ELContext.class.getName());

  private boolean _isPropertyResolved;

  private Locale _locale;

  private HashMap<Class<?>,Object> _contextMap;
  private ArrayList<Map<String,Object>> _lambdaStack;
  
  private ImportHandler _importHandler;
  
  private List<EvaluationListener> _listeners;
  
  
  public ELContext()
  {
  }
  
  public abstract ELResolver getELResolver();

  public abstract FunctionMapper getFunctionMapper();

  public abstract VariableMapper getVariableMapper();

  public Object getContext(Class<?> key)
  {
    Objects.requireNonNull(key);

    if (_contextMap != null)
      return _contextMap.get(key);
    else
      return null;
  }

  public void putContext(Class<?> key, Object contextObject)
  {
    Objects.requireNonNull(key);
    Objects.requireNonNull(contextObject);

    if (_contextMap == null) {
      _contextMap = new HashMap<>(8);
    }
    
    _contextMap.put(key, contextObject);
  }

  public Locale getLocale()
  {
    return _locale;
  }

  public void setLocale(Locale locale)
  {
    _locale = locale;
  }

  public boolean isPropertyResolved()
  {
    return _isPropertyResolved;
  }

  public void setPropertyResolved(boolean resolved)
  {
    _isPropertyResolved = resolved;
  }
  
  public void setPropertyResolved(Object base, Object property)
  {
    setPropertyResolved(true);
    
    notifyPropertyResolved(base, property);
  }
  
  public ImportHandler getImportHandler()
  {
    if (_importHandler == null) {
      _importHandler = new ImportHandler();
    }
    
    return _importHandler;
  }
  
  public void addEvaluationListener(EvaluationListener listener)
  {
    if (_listeners == null)
      _listeners = new ArrayList<>();
      
    _listeners.add(listener);
  }
  
  public List<EvaluationListener> getEvaluationListeners()
  {
    return _listeners;
  }
  
  public void notifyBeforeEvaluation(String expr)
  {
    if (_listeners == null)
      return;
    
    for (EvaluationListener listener : _listeners) {
      listener.beforeEvaluation(this, expr);
    }
  }

  public void notifyAfterEvaluation(String expr)
  {
    if (_listeners == null)
      return;
    
    for (EvaluationListener listener : _listeners) {
      listener.afterEvaluation(this, expr);
    }
  }
  
  public void notifyPropertyResolved(Object base, Object property)
  {
    if (_listeners == null)
      return;
    
    for (EvaluationListener listener : _listeners) {
      listener.propertyResolved(this, base, property);
    }
  }

  public boolean isLambdaArgument(String key)
  {
    ArrayList<Map<String,Object>> stack = _lambdaStack;
    
    if (stack == null) {
      return false;
    }
    
    int size = stack.size();
    
    for (int i = size - 1; i >= 0; i--) {
      Map<String,Object> scope = stack.get(i);
      
      if (scope.containsKey(key)) {
        return true;
      }
    }
    
    return false;
  }
  
  public Object getLambdaArgument(String key)
  {    
    ArrayList<Map<String,Object>> stack = _lambdaStack;
        
    if (stack == null) {
      return null;
    }
    
    int size = stack.size();
    
    for (int i = size - 1; i >= 0; i--) {
      Map<String,Object> scope = stack.get(i);
      
      Object value = scope.get(key);
      
      if (value != null) {
        return value;
      }
      
      if (scope.containsKey(key)) {
        return null;
      }
    }
    
    return null;
  }

  public void enterLambdaScope(Map<String,Object> args)
  {
    if (_lambdaStack == null) {
      _lambdaStack = new ArrayList<>();
    }
    
    _lambdaStack.add(args);
  }
  
  public void exitLambdaScope()
  {
    int size = _lambdaStack.size();
    
    _lambdaStack.remove(size - 1);
  }
  
  public ArrayList<Map<String,Object>> getLambdaStack()
  {
    return _lambdaStack;
  }
  
  public Map<String,Object> getLambdaScope()
  {
    ArrayList<Map<String,Object>> stack = _lambdaStack;
    
    if (stack == null) {
      return null;
    }
    
    int size = stack.size();

    if (size > 0) {
      return stack.get(size - 1);
    }
    else {
      return null;
    }
  }
  
  public Object convertToType(Object obj, Class<?> targetType)
  {
    boolean wasResolved = isPropertyResolved();
    setPropertyResolved(false);
    
    try {
      ELResolver elResolver = getELResolver();
      
      Object converted = elResolver.convertToType(this, obj, targetType);
      
      if (isPropertyResolved())
        return converted;
    } catch (ELException e) {
      throw e;
    } catch (Exception e) {
      throw new ELException(e);
    } finally {
      setPropertyResolved(wasResolved);
    }
    
    return ELManager.getExpressionFactory().coerceToType(obj, targetType);
  }
  
  public String toString()
  {
    return this.getClass().getSimpleName() + "[]";
  }
}
