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


package com.caucho.portal.generic.context;

import com.caucho.portal.generic.Cache;
import com.caucho.portal.generic.CacheKey;

import java.io.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import java.util.logging.Level;
import java.util.logging.Logger;

public class CachingResponseHandler extends AbstractResponseHandler
{
  private Cache _cache;
  private String _namespace;
  private int _expirationCache ;
  private boolean _isPrivate;

  private Writer _cacheWriter;
  private OutputStream _cacheOutputStream;

  private LinkedHashMap<String, Object> _propertiesMap;
  private LinkedHashMap<String, Object> _cachePropertiesMap;

  public CachingResponseHandler( ResponseHandler responseHandler,
                                 Cache cache, String namespace, 
                                 int expirationCache, boolean isPrivate )
  {
    super(responseHandler);
    _cache = cache;
    _namespace = namespace;
    _expirationCache = expirationCache;
    _isPrivate = isPrivate;
  }

  private LinkedHashMap<String, Object> getMapForProperty(String name)
  {
    if (name.startsWith("Cache-")) {
      if (_cachePropertiesMap == null)
        _cachePropertiesMap = new LinkedHashMap<String, Object>();

      return _cachePropertiesMap;
    }
    else {
      if (_propertiesMap == null)
        _propertiesMap = new LinkedHashMap<String, Object>();

      return _propertiesMap;
    }
  }

  public void setProperty(String name, String value)
  {
    super.setProperty(name, value);

    Map<String, Object> map = getMapForProperty(name);;

    map.put(name, value);
  }

  public void addProperty(String name, String value)
  {
    super.addProperty(name, value);

    Map<String, Object> map = getMapForProperty(name);

    Object currentValue = map.get(name);

    if (currentValue == null) {
      ArrayList<String> values = new ArrayList<String>();
      values.add(value);
      map.put(name, values);
    } 
    else if (currentValue instanceof String) {
      ArrayList<String> values = new ArrayList<String>();
      values.add( (String) currentValue);
      values.add(value);
      map.put(name, values);
    }
    else {
      ArrayList<String> values = (ArrayList<String>) currentValue;
      values.add(value);
    }
  }

  public void finish( int expirationCache,
                      CacheKey cacheKey,
                      Map<String, String> requestAttributesMap )
    throws IOException
  {
    Cache cache = _cache;
    OutputStream cacheOutputStream = _cacheOutputStream;
    Writer cacheWriter = _cacheWriter;

    _cacheOutputStream = null;
    _cacheWriter = null;
    _cache = null;
    _namespace = null;
    _isPrivate = false;
    _expirationCache = 0;

    String enc = null;

    boolean fail = true;

    try {
      if (cacheWriter != null)
        enc = getCharacterEncoding();

      boolean isError = isError();

      super.finish();

      if (!isError) {

        Map<String, Object> cachePropertiesMap = null;
        Map<String, Object> propertiesMap = null;

        if ( _cachePropertiesMap != null && !_cachePropertiesMap.isEmpty())
          cachePropertiesMap = _cachePropertiesMap;

        if ( _propertiesMap != null && !_propertiesMap.isEmpty())
          propertiesMap = _propertiesMap;

        if (cacheWriter != null) {
          cache.finishCaching(
            cacheWriter, 
            expirationCache, 
            cacheKey, 
            enc, 
            cachePropertiesMap,
            propertiesMap,
            requestAttributesMap );

          fail = false;
        }

        if (cacheOutputStream != null) {
          cache.finishCaching(
            cacheOutputStream,
            expirationCache, 
            cacheKey, 
            cachePropertiesMap,
            propertiesMap,
            requestAttributesMap );

          fail = false;
        }
      }
    }
    finally {

      if (fail) {
        if (cacheWriter != null) {
          cache.finishCaching(cacheWriter, 0, null, null, null, null, null);
        }

        if (cacheOutputStream != null) {
          cache.finishCaching(cacheOutputStream, 0, null, null, null, null);
        }
      }
    }
  }

  public void finish()
    throws IOException
  {
    throw new UnsupportedOperationException();
  }

  public PrintWriter getWriter()
    throws IOException
  {
    PrintWriter writer = super.getWriter();

    try {
      _cacheWriter = _cache.getCachingWriter(_namespace, _expirationCache, _isPrivate);
    }
    catch (Exception ex) {
      log.log(Level.WARNING, ex.toString(), ex);
    }

    return writer;
  }

  public OutputStream getOutputStream()
    throws IOException
  {
    OutputStream outputStream = super.getOutputStream();

    try {
      _cacheOutputStream 
        = _cache.getCachingOutputStream(_namespace, _expirationCache, _isPrivate);
    }
    catch (Exception ex) {
      log.log(Level.WARNING, ex.toString(), ex);
    }

    return outputStream;
  }

  protected void print(char buf[], int off, int len)
    throws IOException
  {
    super.print(buf, off, len);

    if (_cacheWriter != null)
      _cacheWriter.write(buf, off, len);
  }

  protected void print(String str, int off, int len)
    throws IOException
  {
    super.print(str, off, len);

    if (_cacheWriter != null)
      _cacheWriter.write(str, off, len);
  }

  protected void print(char c)
    throws IOException
  {
    super.print(c);

    if (_cacheWriter != null)
      _cacheWriter.write((int)c);
  }

  protected void write(byte[] buf, int off, int len) 
    throws IOException
  {
    super.write(buf, off, len);

    if (_cacheOutputStream != null)
      _cacheOutputStream.write(buf, off, len);
  }

  protected void write(byte b) 
    throws IOException
  {
    super.write(b);

    if (_cacheOutputStream != null)
      _cacheOutputStream.write((int)b);
  }
}
