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
package com.caucho.config.gen;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

import javax.cache.Cache;
import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheKeyParam;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheRemoveEntry;
import javax.cache.annotation.CacheResolverFactory;
import javax.cache.annotation.CacheResult;
import javax.cache.annotation.CacheValue;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;

import com.caucho.config.ConfigException;
import com.caucho.config.util.CacheKeyGeneratorImpl;
import com.caucho.config.util.CacheKeyImpl;
import com.caucho.config.util.CacheUtil;
import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

/**
 * Represents the caching interception
 */
@Module
public class CacheGenerator<X> extends AbstractAspectGenerator<X> {
  private static final L10N L = new L10N(CacheGenerator.class);
  
  private CacheResult _cacheResult;
  private CachePut _cachePut;
  private CacheRemoveEntry _cacheRemove;
  private CacheRemoveAll _cacheRemoveAll;
  
  private Class<?> _keyGenerator = CacheKeyGenerator.class;
  private Class<?> _cacheResolver = CacheResolverFactory.class;
  
  private String _cacheName;
  private String _cacheInstance;
  private String _keyGenInstance;

  public CacheGenerator(CacheFactory<X> factory,
                        AnnotatedMethod<? super X> method,
                        AspectGenerator<X> next,
                        CacheResult cacheResult,
                        CachePut cachePut,
                        CacheRemoveEntry cacheRemove,
                        CacheRemoveAll cacheRemoveAll)
  {
    super(factory, method, next);
    
    _cacheResult = cacheResult;
    _cachePut = cachePut;
    _cacheRemove = cacheRemove;
    _cacheRemoveAll = cacheRemoveAll;
    
    if (_cacheResult != null) {
      _cacheName = _cacheResult.cacheName();
      _keyGenerator = _cacheResult.cacheKeyGenerator();
      _cacheResolver = _cacheResult.cacheResolverFactory();
    }
    else if (_cachePut != null) {
      _cacheName = _cachePut.cacheName();
      _keyGenerator = _cachePut.cacheKeyGenerator();
      _cacheResolver = _cachePut.cacheResolverFactory();
    }
    else if (_cacheRemove != null) {
      _cacheName = _cacheRemove.cacheName();
      _keyGenerator = _cacheRemove.cacheKeyGenerator();
      _cacheResolver = _cacheRemove.cacheResolverFactory();
    }
    else if (_cacheRemoveAll != null) {
      _cacheName = _cacheRemoveAll.cacheName();
      _cacheResolver = _cacheRemoveAll.cacheResolverFactory();
    }
    else
      throw new IllegalStateException();
    
    CacheDefaults cacheDefaults
      = method.getDeclaringType().getAnnotation(CacheDefaults.class);
    
    if (cacheDefaults != null) {
      if ("".equals(_cacheName))
        _cacheName = cacheDefaults.cacheName();
      
      if (CacheKeyGenerator.class.equals(_keyGenerator))
        _keyGenerator = cacheDefaults.cacheKeyGenerator();
      
      if (CacheResolverFactory.class.equals(_cacheResolver))
        _cacheResolver = cacheDefaults.cacheResolverFactory();
    }
  
    
    if ("".equals(_cacheName)) {
      Method javaMethod = method.getJavaMember();

      _cacheName = (javaMethod.getDeclaringClass().getName()
                    + "." + javaMethod.getName());
    }
    
    if (CacheKeyGenerator.class.equals(_keyGenerator))
      _keyGenerator = null;
    
    if (CacheResolverFactory.class.equals(_cacheResolver))
      _cacheResolver = null;
  }

  //
  // bean prologue generation
  //

  /**
   * Generates the static class prologue
   */
  @Override
  public void generateMethodPrologue(JavaWriter out, HashMap<String, Object> map)
      throws IOException
  {
    super.generateMethodPrologue(out, map);
    
    _cacheInstance = "__caucho_cache_" + out.generateId();
    
    out.println();
    out.println("private static final javax.cache.Cache " + _cacheInstance);
    out.print("  = com.caucho.config.util.CacheUtil.getCache(\"");
    out.printJavaString(_cacheName);
    out.print("\");");
    
    if (_keyGenerator != null
        || _cacheResolver != null) {
      _keyGenInstance = "__caucho_cache_key_" + out.generateId();
      
      out.println();
      out.println("private static final "
                  + CacheKeyGeneratorImpl.class.getName()
                  + " " + _keyGenInstance);
      out.print(" = new " + CacheKeyGeneratorImpl.class.getName() + "(");
      
      if (_keyGenerator != null) {
        out.print("new " + _keyGenerator.getName() + "()");
      }
      else {
        out.print("null");
      }
      
      if (_cacheResolver != null) {
        out.print(", new " + _cacheResolver.getName() + "()");
      }
      else {
        out.print(", null");
      }
      
      out.print(", \"");
      out.printJavaString(_cacheName);
      out.print("\"");
      out.print(", " + getJavaClass().getName() + ".class");
      
      Method method = getJavaMethod();
      out.print(",\"");
      out.printJavaString(method.getName());
      out.print("\"");
      
      for (Class<?> param : method.getParameterTypes()) {
        out.print(",");
        out.printClass(param);
        out.print(".class");
      }
      
      out.println(");");
    }
  }
  
  //
  // method generation code
  //

  /**
   * Generates the interceptor code after the try-block and before the call.
   * 
   * <code><pre>
   * retType myMethod(...)
   * {
   *   try {
   *     [pre-call]
   *     retValue = super.myMethod(...);
   * }
   * </pre></code>
   */
  @Override
  public void generatePreCall(JavaWriter out) throws IOException
  {
    out.println(Cache.class.getName() + " caucho_cache = " + _cacheInstance + ";");;
    
    buildCacheResolver(out);
    
    buildCacheKey(out);

    if (_cacheResult != null && ! _cacheResult.skipGet()) {
      out.println("Object cacheValue = caucho_cache.get(candiCacheKey);");
    
      out.println("if (cacheValue != null) {");
      out.print("  return (");
      out.printClass(getJavaMethod().getReturnType());
      out.println(") cacheValue;");
      out.println("}");
    }

    if (_cachePut != null && ! _cachePut.afterInvocation()) {
      AnnotatedParameter<?> value = getCacheValueParam();
      
      if (value == null)
        throw new ConfigException(L.l("@CachePut requires a @CacheValue"));
      
      out.println("caucho_cache.put(candiCacheKey, a" + value.getPosition() + ");");
    }

    if (_cacheRemove != null && ! _cacheRemove.afterInvocation()) {
      out.println("caucho_cache.remove(candiCacheKey);");
    }

    super.generatePreCall(out);
  }

  /**
   * Generates the interceptor code after invocation and before the call.
   * 
   * <code><pre>
   * retType myMethod(...)
   * {
   *   try {
   *     retValue = super.myMethod(...);
   *     [post-call]
   *     return retValue;
   *   } finally {
   *     ...
   *   }
   * }
   * </pre></code>
   */
  @Override
  public void generatePostCall(JavaWriter out) throws IOException
  {
    super.generatePostCall(out);

    if (_cacheResult != null) {
      out.println("caucho_cache.put(candiCacheKey, result);");
    }

    if (_cachePut != null && _cachePut.afterInvocation()) {
      AnnotatedParameter<?> value = getCacheValueParam();
      
      if (value == null)
        throw new ConfigException(L.l("@CachePut requires a @CacheValue"));
      
      out.println("caucho_cache.put(candiCacheKey, a" + value.getPosition() + ");");
    }

    if (_cacheRemove != null && _cacheRemove.afterInvocation()) {
      out.println("caucho_cache.remove(candiCacheKey);");
    }

    if (_cacheRemoveAll != null) {
      out.println("caucho_cache.removeAll();");
    }
  }
  
  private void buildCacheResolver(JavaWriter out)
    throws IOException
  {
    if (_cacheResolver == null)
      return;
    
    out.print("caucho_cache = " + _keyGenInstance + ".resolveCache(");
      
    out.print("caucho_cache, ");
      
    out.print(getInstanceName());
      
    List params = getParameters();
      
    for (int i = 0; i < params.size(); i++) {
      out.print(", ");
        
      out.print("a" + i);
    }
      
    out.println(");");
  }
  
  private void buildCacheKey(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println(CacheKey.class.getName() + " candiCacheKey");
    
    if (_keyGenInstance != null) {
      out.print(" = " + _keyGenInstance + ".generateKey(");
      
      out.print(getInstanceName());
      
      List params = getParameters();
      
      for (int i = 0; i < params.size(); i++) {
        out.print(", ");
        
        out.print("a" + i);
      }
      
      out.println(");");
    }
    else {
      out.print("  = new " + CacheKeyImpl.class.getName() + "(");
    
      List params = getMethod().getParameters();
    
      boolean isCacheKeyParam = isCacheKeyParam(params);
      boolean isFirst = true;
  
      for (int i = 0; i < params.size(); i++) {
        AnnotatedParameter<?> param = (AnnotatedParameter<?>) params.get(i);
      
        if (param.isAnnotationPresent(CacheValue.class))
          continue;
      
        if (isCacheKeyParam && ! param.isAnnotationPresent(CacheKeyParam.class))
          continue;
      
        if (! isFirst)
          out.print(", ");
      
        out.print("a" + i);
        isFirst = false;
      }

      out.println(");");
    }
  }
  
  private void buildInvocationContext(JavaWriter out)
    throws IOException
  {
    out.print(CacheUtil.class.getName() + ".generateKeyContext(");
    out.print(")");
  }
  
  private boolean isCacheKeyParam(List<AnnotatedParameter<?>> params)
  {
    for (AnnotatedParameter<?> param : params) {
      if (param.isAnnotationPresent(CacheKeyParam.class)) {
        return true;
      }
    }
    
    return false;
  }
  
  private AnnotatedParameter<?> getCacheValueParam()
  {
   for (AnnotatedParameter<?> param : getParameters()) {
      if (param.isAnnotationPresent(CacheValue.class)) {
        return param;
      }
    }
    
    return null;
  }
  
  
  private List<AnnotatedParameter<?>> getParameters()
  {
    return (List) getMethod().getParameters();
  }
}