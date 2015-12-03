/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */
package com.caucho.config.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.cache.Cache;
import javax.cache.annotation.CacheInvocationContext;
import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheKeyInvocationContext;
import javax.cache.annotation.CacheKeyParam;
import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheRemoveEntry;
import javax.cache.annotation.CacheResolver;
import javax.cache.annotation.CacheResolverFactory;
import javax.cache.annotation.CacheResult;
import javax.cache.annotation.CacheValue;

/**
 * Key for a cacheable method.
 */
@SuppressWarnings("serial")
public class CacheKeyGeneratorImpl {
  private CacheKeyGenerator _keyGenerator;
  private CacheResolverFactory _resolverFactory;
  private CacheResolver _cacheResolver;
  private String _cacheName;
  
  private Method _method;
  
  private Set<Annotation> _annotations;
  private Annotation _cacheAnnotation;
  private Set<Annotation> []_paramAnnotations;
  
  private int []_keyParameters;
  private int _valueParam = -1;
  
  public CacheKeyGeneratorImpl(CacheKeyGenerator keyGenerator,
                               CacheResolverFactory resolverFactory,
                               String cacheName,
                               Class<?> targetClass,
                               String methodName,
                               Class<?> ...parameterTypes)
  {
    _keyGenerator = keyGenerator;
    _resolverFactory = resolverFactory;
    _cacheName = cacheName;
    
    try {
      _method = targetClass.getMethod(methodName, parameterTypes);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    
    if (_method.isAnnotationPresent(CacheResult.class))
      _cacheAnnotation = _method.getAnnotation(CacheResult.class);
    else if (_method.isAnnotationPresent(CachePut.class))
      _cacheAnnotation = _method.getAnnotation(CachePut.class);
    else if (_method.isAnnotationPresent(CacheRemoveEntry.class))
      _cacheAnnotation = _method.getAnnotation(CacheRemoveEntry.class);
    else if (_method.isAnnotationPresent(CacheRemoveAll.class))
      _cacheAnnotation = _method.getAnnotation(CacheRemoveAll.class);
    
    _annotations = new HashSet<Annotation>();
    for (Annotation ann : _method.getAnnotations()) {
      _annotations.add(ann);
    }
    
    Class<?> []paramTypes = _method.getParameterTypes();
    
    Annotation [][]paramAnn = _method.getParameterAnnotations();
    
    _paramAnnotations = new Set[paramTypes.length];
    
    ArrayList<Integer> keyParameters = new ArrayList<Integer>();
    boolean isKeyParam = false;
    
    for (int i = 0; i < paramTypes.length; i++) {
      _paramAnnotations[i] = new HashSet<Annotation>();
      
      if (paramAnn != null && paramAnn[i] != null) {
        for (Annotation ann : paramAnn[i]) {
          _paramAnnotations[i].add(ann);
          
          if (CacheValue.class.equals(ann.annotationType()))
            _valueParam = i;
        }
        
        if (isAnnotationPresent(CacheKeyParam.class, paramAnn[i])) {
          if (! isKeyParam) {
            isKeyParam = true;
            keyParameters.clear();
          }
          
          keyParameters.add(i);
        }
        else if (isAnnotationPresent(CacheValue.class, paramAnn[i])) {
        }
        else if (! isKeyParam) {
          keyParameters.add(i);
        }
      }
    }
    
    if (keyParameters.size() < paramTypes.length) {
      _keyParameters = new int[keyParameters.size()];
      
      for (int i = 0; i < _keyParameters.length; i++) {
        _keyParameters[i] = keyParameters.get(i);
      }
    }
    
    if (_resolverFactory != null) {
      _cacheResolver = _resolverFactory.getCacheResolver(new MethodDetails());
    }
  }
  
  private boolean isAnnotationPresent(Class<?> annType, Annotation []annList)
  {
    if (annList == null)
      return false;
    
    for (Annotation ann : annList) {
      if (annType.equals(ann.annotationType()))
        return true;
    }
    
    return false;
  }
  
  public CacheKey generateKey(Object target, Object...args)
  {
    if (_keyGenerator != null)
      return _keyGenerator.generateCacheKey(new KeyContext(target, args));
    else
      return new CacheKeyImpl(args);
  }
  
  public Cache resolveCache(Cache cache, Object target, Object...args)
  {
    if (_cacheResolver != null) {
      CacheInvocationContext cxt = new InvocationContext(target, args);
      
      Cache resolvedCache = _cacheResolver.resolveCache(cxt);

      if (resolvedCache != null)
        return resolvedCache;
      else
        return cache;
    }
    else {
      return cache;
    }
  }
  
  class MethodDetails implements CacheMethodDetails {
    @Override
    public String getCacheName()
    {
      return _cacheName;
    }

    @Override
    public Method getMethod()
    {
      return _method;
    }

    @Override
    public Set getAnnotations()
    {
      return _annotations;
    }

    @Override
    public Annotation getCacheAnnotation()
    {
      return _cacheAnnotation;
    }
    
    public String toString()
    {
      return (getClass().getSimpleName() + "[" + _cacheName + ","
              + _method.getDeclaringClass().getSimpleName() + "."
              + _method.getName());
    }
  }
  
  class InvocationContext extends MethodDetails
    implements CacheInvocationContext {
    private Object _target;
    
    private CacheInvocationParameter []_parameters;
    
    InvocationContext(Object target, Object []args)
    {
      _target = target;
      
      int length = args != null ? args.length : 0;
      
      _parameters = new CacheInvocationParameter[length];
      
      for (int i = 0; i < length; i++) {
        _parameters[i] = new ParameterContext(i, args[i]);
      }
    }

    @Override
    public CacheInvocationParameter[] getAllParameters()
    {
      return _parameters;
    }

    @Override
    public Object getTarget()
    {
      return _target;
    }

    @Override
    public Object unwrap(Class cl)
    {
      return null;
    }
  }
  
  class KeyContext extends InvocationContext implements CacheKeyInvocationContext {
    KeyContext(Object target, Object []args)
    {
      super(target, args);
    }

    @Override
    public CacheInvocationParameter[] getKeyParameters()
    {
      if (_keyParameters == null)
        return getAllParameters();
 
      CacheInvocationParameter []parameters = getAllParameters();

      CacheInvocationParameter []keyParameters
        = new CacheInvocationParameter[_keyParameters.length];
      
      for (int i = 0; i < keyParameters.length; i++) {
        keyParameters[i] = parameters[_keyParameters[i]];
      }
      
      return keyParameters;
    }

    @Override
    public CacheInvocationParameter getValueParameter()
    {
      if (_valueParam >= 0)
        return getAllParameters()[_valueParam];
      else
        return null;
    }
  }
  
  class ParameterContext implements CacheInvocationParameter {
    private final int _pos;
    private final Object _value;
    
    ParameterContext(int pos, Object value)
    {
      _pos = pos;
      _value = value;
    }

    @Override
    public Object getValue()
    {
      return _value;
    }

    @Override
    public int getParameterPosition()
    {
      return _pos;
    }

    @Override
    public Class<?> getRawType()
    {
      return _method.getParameterTypes()[_pos];
    }

    @Override
    public Set<Annotation> getAnnotations()
    {
      return _paramAnnotations[_pos];
    }
  }
}
