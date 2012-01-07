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

import javax.cache.annotation.CacheResult;
import javax.ejb.MessageDriven;
import javax.ejb.SessionSynchronization;
import javax.ejb.Singleton;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;

import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;

/**
 * Represents the XA interception
 */
@Module
public class CacheGenerator<X> extends AbstractAspectGenerator<X> {
  private CacheResult _cache;
  
  private String _cacheName;
  private String _cacheInstance;

  public CacheGenerator(CacheFactory<X> factory,
                        AnnotatedMethod<? super X> method,
                        AspectGenerator<X> next,
                        CacheResult cache)
  {
    super(factory, method, next);
    
    _cache = cache;
    
    _cacheName = _cache.cacheName();
    
    if ("".equals(_cacheName)) {
      Method javaMethod = method.getJavaMember();

      _cacheName = (javaMethod.getDeclaringClass().getName()
                    + "." + javaMethod.getName());
    }
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
    out.println();
    out.println("com.caucho.config.util.CacheKey candiCacheKey");
    out.print("  = new com.caucho.config.util.CacheKey(");
    
    List params = getMethod().getParameters();
    
    for (int i = 0; i < params.size(); i++) {
      if (i != 0)
        out.print(", ");
      out.print("a" + i);
    }

    out.println(");");
    
    out.println("Object cacheValue = " + _cacheInstance + ".get(candiCacheKey);");
    
    out.println("if (cacheValue != null) {");
    out.print("return (");
    out.printClass(getJavaMethod().getReturnType());
    out.print(") cacheValue;");
    out.println("}");

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

    out.println(_cacheInstance + ".put(candiCacheKey, result);");
  }
}