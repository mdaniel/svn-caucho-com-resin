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

import java.lang.reflect.Method;
import java.util.*;

public class StandardELContext extends ELContext
{
  private ExpressionFactory _factory;
  private ELContext _parentContext;
  
  private FunctionMapper _functionMapper;
  private VariableMapper _variableMapper;
  
  private LocalBeanNameResolver _localBeanNameResolver;
  
  private CompositeELResolver _elResolver;
  private CompositeELResolver _childCompositeELResolver;
  
  public StandardELContext(ExpressionFactory factory)
  {
    super();
    
    _factory = factory;
    _parentContext = null;
  }
  
  public StandardELContext(ELContext context)
  {
    _parentContext = context;
    
    _functionMapper = context.getFunctionMapper();
    _variableMapper = context.getVariableMapper();
    
    setLocale(context.getLocale());
  }
  
  @Override
  public void putContext(Class<?> key, Object contextObject) 
  {
    if (_parentContext != null) {
      _parentContext.putContext(key, contextObject);
    } else {
      super.putContext(key, contextObject);
    }
  }

  @Override
  public Object getContext(Class<?> key) 
  {
    if (_parentContext != null) {
      return _parentContext.getContext(key);
    } else {
      return super.getContext(key);
    }
  }

  /**
   * Retrieves the ELResolver associated with this context. This is a 
   * CompositeELResover consists of an ordered list of ELResolvers.
   * 
   *  A BeanNameELResolver for beans defined locally
   *  Any custom ELResolvers
   *  An ELResolver supporting the collection operations
   *  A StaticFieldELResolver for resolving static fields
   *  A MapELResolver for resolving Map properties
   *  A ResourceBundleELResolver for resolving ResourceBundle properties
   *  A ListELResolver for resolving List properties
   *  An ArrayELResolver for resolving array properties
   *  A BeanELResolver for resolving bean properties
   */
  @Override
  public ELResolver getELResolver()
  {
    if (_elResolver == null) {
      _elResolver = new CompositeELResolver();
      _elResolver.add(new BeanNameELResolver(getLocalBeanNameResolver()));
      _elResolver.add(getChildCompositeELResolver());
      if (_factory != null && _factory.getStreamELResolver() != null) {
        _elResolver.add(_factory.getStreamELResolver());
      }
      _elResolver.add(new StaticFieldELResolver());
      _elResolver.add(new MapELResolver());
      _elResolver.add(new ResourceBundleELResolver());
      _elResolver.add(new ListELResolver());
      _elResolver.add(new ArrayELResolver());
      _elResolver.add(new BeanELResolver());
      if (_parentContext != null) {
        _elResolver.add(_parentContext.getELResolver());
      }
    }
    
    return _elResolver;
  }
  
  public void addELResolver(ELResolver elResolver)
  {
    getChildCompositeELResolver().add(elResolver);
  }
  
  public Object defineBean(String name, Object bean)
  {
    return getLocalBeanNameResolver().defineBean(name, bean);
  }
  
  @Override
  public FunctionMapper getFunctionMapper()
  {
    if (_functionMapper == null) {
      _functionMapper = new LocalFunctionMapper();
    }
    
    return _functionMapper;
  }
  
  @Override
  public VariableMapper getVariableMapper()
  {
    if (_variableMapper == null) {
      _variableMapper = new LocalVariableMapper();
    }
    
    return _variableMapper;
  }
  
  private LocalBeanNameResolver getLocalBeanNameResolver()
  {
    if (_localBeanNameResolver == null) {
      _localBeanNameResolver = new LocalBeanNameResolver();
    }
    
    return _localBeanNameResolver;
  }
  
  private CompositeELResolver getChildCompositeELResolver()
  {
    if (_childCompositeELResolver == null) {
      _childCompositeELResolver = new CompositeELResolver();
    }
    
    return _childCompositeELResolver;
  }
  
  private static class LocalFunctionMapper extends FunctionMapper
  {
    private Map<String, Method> _functionMap = new HashMap<>();
    
    @Override
    public Method resolveFunction(String prefix, String localName)
    {
      String key = String.format("%s:%s", prefix, localName);
      return _functionMap.get(key);
    }
    
    @Override
    public void mapFunction(String prefix, String localName, Method method)
    {
      String key = String.format("%s:%s", prefix, localName);
      _functionMap.put(key, method);
    }
    
  }
  
  private static class LocalVariableMapper extends VariableMapper
  {
    private Map<String, ValueExpression> _variableMap = new HashMap<>();
    
    @Override
    public ValueExpression resolveVariable(String name)
    {
      return _variableMap.get(name);
    }
    
    @Override
    public ValueExpression setVariable(String name, ValueExpression expression)
    {
      ValueExpression old = null;
      
      if (name == null)
        old = _variableMap.remove(name);
      else
        old = _variableMap.put(name, expression);
      
      return old;
    }
    
  }
  
  private class LocalBeanNameResolver extends BeanNameResolver
  {
    private Map<String, Object> _localBeans = new HashMap<>();

    @Override
    public boolean isNameResolved(String name)
    {
      return _localBeans.containsKey(name);
    }
    
    @Override
    public Object getBean(String name)
    {
      return _localBeans.get(name);
    }
    
    @Override
    public void setBeanValue(String name, Object value)
    {
      _localBeans.put(name, value);
    }
    
    public Object defineBean(String name, Object bean)
    {
      setBeanValue(name, bean);
      return bean;
    }
    
    @Override
    public boolean isReadOnly(String beanName)
    {
      return false;
    }
    
    @Override
    public boolean canCreateBean(String beanName)
    {
      return true;
    }
  }
  
}
