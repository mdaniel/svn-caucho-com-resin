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

import javax.portlet.PortletContext;
import javax.portlet.PortletSession;
import javax.servlet.http.HttpSession;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

/**
 * An adapter to a {@link javax.servlet.HttpSession}.
 */
public class HttpPortletSession implements PortletSession {
  static protected final Logger log 
    = Logger.getLogger(HttpPortletSession.class.getName());

  public final String PORTLET_SCOPE_PREFIX = "javax.portlet.p.";
  public final String PORTLET_SCOPE_RESERVED_PREFIX = "javax.portlet.p.javax.portlet.";

  private PortletContext _portletContext;
  private HttpSession _httpSession;
  
  public HttpPortletSession()
  {
  }

  public void start( PortletContext portletContext,
                     HttpSession httpSession )
  {
    _portletContext = portletContext;
    _httpSession = httpSession;

    if (_httpSession != null) {
      for (Enumeration en = _httpSession.getAttributeNames(); en.hasMoreElements(); ) {
        String name = (String) en.nextElement();
        Object value = _httpSession.getAttribute(name);
      }
    }

  }

  /**
   * Finish with the HttpSession object.  After calling this method, 
   * this object can be placed into a pool and reused for subsequent requests.
   */
  public void finish()
  {
    _httpSession = null;
    _portletContext = null;
  }

  public PortletContext getPortletContext()
  {
    return _portletContext;
  }

  public HttpSession getHttpSession()
  {
    return _httpSession;
  }

  private String scopedName(String name, int scope)
  {
    if (name == null)
      throw new IllegalArgumentException("name is null");

    if (false) // XXX: isInvalid() might be handled by HttpSession
      throw new IllegalStateException("session is invalid");

    if (scope == APPLICATION_SCOPE) {
    }
    else if (scope == PORTLET_SCOPE) {
      StringBuffer sb = new StringBuffer(PORTLET_SCOPE_PREFIX);
      sb.append(name);
      name = sb.toString();
    }
    else {
      throw new IllegalArgumentException("invalid scope `" + scope + "'");
    }

    return name;
  }

  public Object getAttribute(String name)
  {
    return getAttribute(name,PORTLET_SCOPE);
  }

  public Object getAttribute(String name,int scope)
  {
    return _httpSession.getAttribute(scopedName(name,scope));
  }

  public Enumeration getAttributeNames()
  {
    return getAttributeNames(PORTLET_SCOPE);
  }

  public Enumeration getAttributeNames(int scope)
  {
    if (scope == APPLICATION_SCOPE) {
      return _httpSession.getAttributeNames();
    }
    else if (scope == PORTLET_SCOPE) {
      return new Enumeration() {
        private Enumeration _e = _httpSession.getAttributeNames();
        private Object _next;

        public boolean hasMoreElements()
        {
          nextIfNeeded();
          return _next != null ? true : false;
        } 

        public Object nextElement()
        {
          nextIfNeeded();
          if (_next == null)
            throw new NoSuchElementException();
          String result = (String) _next;
          result = result.substring(PORTLET_SCOPE_PREFIX.length());
          _next = null;
          return result;
        }

        private void nextIfNeeded()
        {
          if (_next == null) {
            while (_e.hasMoreElements()) {
              String e = (String) _e.nextElement();
              if (e.startsWith(PORTLET_SCOPE_PREFIX)) {
                if (!e.startsWith(PORTLET_SCOPE_RESERVED_PREFIX)) {
                  _next = e;
                  break;
                }
              }
            }
          }
        }
      };
    }
    else {
      throw new IllegalArgumentException("invalid scope `" + scope + "'");
    }
  }

  public long getCreationTime()
  {
    return _httpSession.getCreationTime();
  }

  public String getId()
  {
    return _httpSession.getId();
  }

  public long getLastAccessedTime()
  {
    return _httpSession.getLastAccessedTime();
  }

  public int getMaxInactiveInterval()
  {
    return _httpSession.getMaxInactiveInterval();
  }

  public void invalidate()
  {
    _httpSession.invalidate();
  }

  public boolean isNew()
  {
    return _httpSession.isNew();
  }

  public void removeAttribute(String name) 
  {
    removeAttribute(name,PORTLET_SCOPE);
  }

  public void removeAttribute(String name, int scope) 
  {
    _httpSession.removeAttribute(scopedName(name,scope));
  }

  public void setAttribute(String name, Object value)
  {
    setAttribute(name,value,PORTLET_SCOPE);
  }

  public void setAttribute(String name, Object value, int scope)
  {
    _httpSession.setAttribute(scopedName(name,scope),value);
  }

  public void setMaxInactiveInterval(int interval)
  {
    _httpSession.setMaxInactiveInterval(interval);
  }

}

