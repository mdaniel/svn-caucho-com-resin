/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2004 Caucho Technology, Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Hessian", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Sam
 */


package com.caucho.portal.generic;

import java.io.*;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.portlet.*;


abstract public class Cache
{
  protected static final Logger log = 
    Logger.getLogger(Cache.class.getName());

  /**
   * Send a response from the cache, or return false if there is no
   * response from the cache.
   * 
   * The passed cacheKey can be manipulated by the cache without side-effects,
   * but the cache cannot take ownership of the cacheKey because it may be 
   * pooled by the caller.
   *
   * Binary responses should be sent to the OutputStream obtained with
   * response.getOutputStream().
   *  
   * Textual response should be sent to the PrintWriter obtained with
   * response.getWriter().
   * response.setCharacterEncoding() should be used before getWriter()
   * to set the character encoding to the character encoding that was in
   * use when the response was cached.
   *
   * If isPrivate is true, then cacheKey.getRequestedSessionId() will have a
   * non-null value. 
   *
   * A cache should first perform a lookup with the CacheKey as provided.
   * If that fails, it should call cacheKey.setLocale(null) and try again.
   * 
   * The cache should also check the value of cacheKey.getContentType().
   * If the contentType is null, the cache can respond if it has an entry
   * that matches for one contentType.  If a match is found, the cache
   * sets the contentType with connection.setContentType().
   *
   * @return 0 if no response was written from the cache, otherwise the number 
   * of seconds that a response written from the cache is good for or -1
   * if the cached reponse is good forever
   */
  abstract public int respondFromCache(CacheKey cacheKey,
                                       RenderRequest request,
                                       RenderResponse response);

  /**
   * Called to give the Cache an opportunity to cache the response.
   *
   * If the response can be cached, the implementation returns a Writer
   * that receives the response.
   * 
   * When the response is completed, finishCaching(Writer) will be called.
   *
   * If the response cannot be cached, the implementation returns null.
   *
   * At this point, the expirationCache is a best guess and the real value
   * passed to finishCaching() may be different.
   *
   * @param  window the portlet configuration for the portlet about to
   * be rendered
   *
   * @param namespace the namespace for the portlet about to be rendered
   *
   * @param expirationCache a best guess at the expiration period in seconds, 
   * -1 if unlimited
   *
   * @return a Writer that intercepts the content and then writes to
   * response.getWriter(), or null if the response cannot be cached. 
   */
  abstract public Writer getCachingWriter(String namespace,
                                          int expirationCache,
                                          boolean isPrivate)
    throws IOException;

  /**
   * Called immediately before a Portlet is rendered to give the Cache
   * an opportunity to cache the response.
   *
   * If the response can be cached, the implementation returns an OutputStream
   * that receives the response.
   * 
   * When the response is completed, finishCaching(OutputStream) will be called.
   *
   * If the response cannot be cached, the implementation returns null.
   *
   * At this point, the expirationCache is a best guess and the real value
   * passed to finishCaching() may be different.
   *
   * @param  window the portlet window for the portlet about to
   * be rendered
   *
   * @param namespace the namespace for the portlet about to be rendered
   *
   * @param expirationCache a best guess at the expiration period in seconds, 
   * -1 if unlimited
   *
   * @return an OutputStream that intercepts the content and then writes to
   * response.getOutputStream(), or null if the response cannot be cached. 
   */
  abstract public OutputStream getCachingOutputStream(String namespace,
                                                      int expirationCache,
                                                      boolean isPrivate)

    throws IOException;

  /**
   * Finish with a Writer previously returned by 
   * {@link #startCachingWriter}. If the expirationCache is 0 or the cacheKey is
   * null, the cached response must be discarded.
   *
   * @param writer the writer returned from {@link #startCachingWriter}
   *
   * @param expirationCache the updated expirationCache, this may the same
   * value received in {@link  #startWriter}, a new value set by
   * the portlet while it rendered itself, or 0 if the cache must be
   * discarded.
   *
   * @param cacheKey the {@link CacheKey} that uniquely differentiates this 
   * response from other responses
   *
   * @param encoding the encoding for the Writer, the cache needs to call
   * response.setEncoding(encoding) if it later responds in respondFromCache().
   *
   * @param cachePropertiesMap a map of properties that begin with "Cache-",
   * these may be directives to the cache or may be keys and values that should
   * distinguish the uniqueness of the Cached value beyond the uniqueness
   * established by CacheKey, depending on the portal implementation. 
   * These properties should be recreated during respondFromCache().
   * If the value is a String, setProperty is used.  If the value is
   * an ArrayList<String>, add property is used. 
   *
   * @param propertiesMap a map of properties that the Cache must recreate
   * when the cached response is used in a susbsequent call to respondFromCache
   * If the value is a String, setProperty is used.  If the value is
   * an ArrayList<String>, add property is used. 
   *
   * @param requestAttributesMap a map of request attributes that the Cache
   * must recreate when the cached response is used in a susbsequent call to
   * respondFromCache
   */ 
  abstract public void finishCaching( 
      Writer writer, 
      int expirationCache,
      CacheKey cacheKey,
      String encoding,
      Map<String, Object> cachePropertiesMap,
      Map<String, Object> propertiesMap,
      Map<String, String> requestAttributeMap );

  /**
   * Finish with an OutputStream previously returned by 
   * {@link #startCachingOutputStream}. If the cacheKey is null or
   * the expirationCache is 0, the cached response must be discarded.
   */ 
  abstract public void finishCaching(
      OutputStream outputStream,
      int expirationCache,
      CacheKey cacheKey,
      Map<String, Object> cachePropertiesMap,
      Map<String, Object> propertiesMap,
      Map<String, String> requestAttributeMap );
}

