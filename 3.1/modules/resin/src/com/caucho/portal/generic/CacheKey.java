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

import javax.portlet.PortletMode;
import javax.portlet.WindowState;
import java.util.Locale;


/**
 * XXX: Cache implementation currently not complete.
 *
 * The  ConnectionContext should start building a CacheKey
 * as soon as it sees a window with expiration-cache != 0.
 * It should do this in the action stage, because each window is
 * seen in the action stage. 
 * It should add parameters for every child namespace encountered, to whatever
 * depth.
 *
 * Then when it gets to the render stage, it can use the CacheKey 
 * to see if a cached value is available.
 *
 * If not, it inserts a CachingPortletWriteStream, and fills the cache. 
 */
public class CacheKey {
  private String _namespace;
  private int _namespaceValue;

  private PortletMode _portletMode;
  private int _portletModeValue;

  private WindowState _windowState;
  private int _windowStateValue;

  private long _value1, _value2; // parameters

  private String _contentType;
  private int _contentTypeValue;
  private Locale _locale;
  private int _localeValue;
  private boolean _isPrivate = true;
  private String _sessionId;

  private String _stringValue;

  public void setNamespace(String namespace)
  {
    _stringValue = null;
    _namespace = namespace;
    _namespaceValue = namespace.hashCode();
  }

  public void setPortletMode(PortletMode portletMode)
  {
    _stringValue = null;
    _portletMode = portletMode;
    _portletModeValue = portletMode.hashCode();
  }

  public void setWindowState(WindowState windowState)
  {
    _stringValue = null;
    _windowState = windowState;
    _windowStateValue = windowState.hashCode();
  }

  private void add(String name, String value) 
  {
    _stringValue = null;
    int v = (name == null ? 65353 : name.hashCode());

    _value1 += v;
    _value2 += v + (value == null ? 65353 : value.hashCode());
  }

  public void addParameter(String name, String[] values) 
  {
    _stringValue = null;
    if (values == null || values.length == 0)
      add(name, null);
    else {
      int i = values.length;

      while (i-- > 0)
        add(name, values[i]);
    }
  }

  public void setContentType(String contentType)
  {
    _stringValue = null;
    _contentType = contentType;
    _contentTypeValue = contentType.hashCode();
  }

  /**
   * When a cache key is created to check for a cached response the locale
   * is set to the value of request.getLocale().
   * A cache should first check the cache for an entry with the cacheKey 
   * as it is received, and then call cacheKey.setLocale(null) and try again. 
   *
   * When a cache key is created to cache a response the locale is only set in
   * the cache key if a portlet has called request.getLocale() or 
   * request.getLocales() or request.setLocale() method. It is set to the locale
   * set by the portlet with response.setLocale(), or if the portlet does not
   * call response.setLocale(), the value of request.getLocale().
   */
  public void setLocale(Locale locale)
  {
    _stringValue = null;
    _locale = locale;
    _localeValue = locale.hashCode();
  }

  public void setPrivate(boolean isPrivate)
  {
    _isPrivate = isPrivate;
  }

  public void setRequestedSessionId(String sessionId)
  {
    _stringValue = null;
    _sessionId = sessionId;
  }

  public boolean isPrivate()
  {
    return _isPrivate;
  }

  public String getNamespace()
  {
    return _namespace;
  }

  public PortletMode getPortletMode()
  {
    return _portletMode;
  }

  public WindowState getWindowState()
  {
    return _windowState;
  }

  public String getContentType()
  {
    return _contentType;
  }

  public Locale getLocale()
  {
    return _locale;
  }

  /**
   * Return a session id if isPrivate() is true, null if isPrivate() is false.
   */ 
  public String getRequestedSessionId()
  {
    return _isPrivate ? _sessionId : null;
  }

  /**
   * Reset to a state similar to that following construction.
   */
  public void reset()
  {
    _stringValue = null;

    _namespace = null;
    _namespaceValue = 0;
    _portletMode = null;
    _portletModeValue = 0;
    _windowState = null;
    _windowStateValue = 0;
    _value1 = 0;
    _value2 = 0;

    _localeValue = 0;
    _locale = null;
    _contentTypeValue = 0;
    _contentType = null;

    _isPrivate = true;
    _sessionId = null;
  }

  public int hashCode()
  { 
    int isPrivateValue = _isPrivate ? 31 : 33331;

    int sessionIdValue = _isPrivate && _sessionId != null 
                         ? _sessionId.hashCode() 
                         : 7331;
    return (int) 
      ((long) _namespaceValue 
              * _portletModeValue 
              * _windowStateValue 
              * _localeValue 
              * _contentTypeValue 
              * isPrivateValue
              * sessionIdValue
              * _value1 
              * _value2);
  }

  public boolean equals(Object o)
  {
    if (o == null || !(o instanceof CacheKey))
      return false;

    CacheKey other = (CacheKey) o;

    return 
      (_namespaceValue == other._namespaceValue)
      && (_portletModeValue == other._portletModeValue)
      && (_windowStateValue == other._windowStateValue) 
      && (_value1 == other._value1) 
      && (_value2 == other._value2)
      && (_localeValue == other._localeValue) 
      && (_contentTypeValue == other._contentTypeValue)
      && (_isPrivate == other._isPrivate)
      && (_isPrivate ? _sessionId != null : true)
      && (_isPrivate ? _sessionId.equals(other._sessionId) : true);
  }

  static private void encode(long number, StringBuffer buf)
  {
    int code;
    char ch = '-';

    do {
      code = ((int) number) & 0x3f; // 6 bits

      if (code < 26)
        ch =  (char) ('a' + code);
      else if (code < 52)
        ch =  (char) ('A' + code - 26);
      else if (code < 62)
        ch =  (char) ('0' + code - 52);
      else if (code == 62 || code == 63)
        ch =  '_';
      else
        ch =  'z';

      number = number >> 6;

    } while (number != 0);
  }

  /**
   * Return a string that uniquely identifies this CacheKey.
   * The set of characters that form the string is "A-Z a-z 0-9 _".
   * That means the returned string is suitable as a file name, for example. 
   */
  public String toString()
  {
    if (_stringValue != null)
      return _stringValue;

    StringBuffer buf = new StringBuffer();

    encode(_value1, buf);
    encode(_value2, buf);

    int valuesIndex = buf.length();

    encode(_namespaceValue, buf);
    encode(_portletModeValue, buf);
    encode(_windowStateValue, buf);
    encode(_localeValue, buf);
    encode(_contentTypeValue, buf);

    if (_isPrivate)
      buf.append(getRequestedSessionId());
     
    // mix up the string so that if it is truncated by the cache 
    // there is less likelyhood of significant loss
    
    int len = buf.length();
    int half = len / 2;

    int i = valuesIndex;
    int j = len - 1;

    while (i < half) {
      char ch = buf.charAt(i);
      buf.setCharAt(i, buf.charAt(j));
      buf.setCharAt(j, ch);
      i += 3;
      j -= 3;
    }

    i = valuesIndex;
    j = half > i ? half : len;

    while (j < len) {
      char ch = buf.charAt(i);
      buf.setCharAt(i, buf.charAt(j));
      buf.setCharAt(j, ch);
      i += 5;
      j += 5;
    }

    // return the string

    _stringValue = buf.toString();

    return _stringValue;
  }
} // CacheKey

