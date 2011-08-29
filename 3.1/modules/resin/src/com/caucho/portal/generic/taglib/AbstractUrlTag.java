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


package com.caucho.portal.generic.taglib;

import com.caucho.portal.generic.InvocationURL;

import javax.portlet.*;
import javax.servlet.ServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;


abstract public class AbstractUrlTag extends TagSupport {
  // -- attributes --
  private String _var;
  private WindowState _windowState;
  private PortletMode _portletMode;
  private Boolean _secure;

  // -- non-attributes --

  private LinkedHashMap<String,Object> _paramMap 
    = new LinkedHashMap<String,Object>();
  private LinkedHashMap<String,LinkedHashMap<String,Object>> _namespaceParamMap
    = new LinkedHashMap<String,LinkedHashMap<String,Object>>();

  abstract protected PortletURL createPortletURL(RenderResponse response);

  /**
   * The variable to store the url in.  If this is not set,
   * the url is printed to the output writer.
   */
  public void setVar(String var)
  {
    _var = var;
  }

  /**
   * The window state.  If not set, the current window state is maintained.
   */
  public void setWindowState(String windowState)
  {
    _windowState = new WindowState(windowState);
  }

  /**
   * The portlet mode.  If not set, the current portlet mode is maintained.
   */
  public void setPortletMode(String portletMode)
  {
    _portletMode = new PortletMode(portletMode);
  }

  /**
   * Indicates if the resulting url should be a secure connection
   * (secure="true") or an insecure (secure="false").   If not set, the current
   * security setting is maintained.
   */
  public void setSecure(String secure)
    throws JspException
  {
    if (secure == null)
      _secure = null;
    else if (secure.equalsIgnoreCase("true"))
      _secure = Boolean.TRUE;
    else if (secure.equalsIgnoreCase("false"))
      _secure = Boolean.FALSE;
    else
      throw new JspException("secure must be `true' or `false' or unspecified");
  }

  public int doStartTag() 
    throws JspException
  {
    _paramMap.clear();
    _namespaceParamMap.clear();

    return EVAL_BODY_INCLUDE;
  }

  /**
   * Add a parameter.
   */
  public void addParam(String name, String value)
  {
    addParam(_paramMap, name, value);
  }

  /**
   * Add a parameter for a particular namespace.
   */
  public void addParam(String namespace, String name, String value)
  {
    LinkedHashMap<String,Object> map = _namespaceParamMap.get(namespace);

    if (map == null) {
      map = new LinkedHashMap<String,Object>();
      _namespaceParamMap.put(namespace,map);
    }

    addParam(map, name, value);
  }

  private void addParam( LinkedHashMap<String, Object> map, 
                         String name, 
                         String value )
  {
    // slight optimization, assume that the parameter doesn't exist yet
    Object current = map.put(name, value);

    if (current != null) {
      if (current instanceof ArrayList) {
        ArrayList<String> list = (ArrayList<String>) current;
        list.add(value);
        map.put(name, list);
      }
      else {
        ArrayList<String> list = new ArrayList<String>();
        list.add((String) current);
        list.add(value);
        map.put(name, list);
      }
    }
  }

  public int doEndTag() 
    throws JspException
  {
    PortletURL url;

    ServletRequest request = pageContext.getRequest();
    RenderResponse response = (RenderResponse) 
      request.getAttribute("javax.portlet.renderResponse");

    if (response == null)
      throw new JspException("No RenderResponse available");

    url = createPortletURL(response);

    if (_windowState != null) {
      try {
        url.setWindowState(_windowState);
      } 
      catch (WindowStateException ex) {
        throw new JspException(ex);
      }
    }

    if (_portletMode != null) {
      try {
        url.setPortletMode(_portletMode);
      } 
      catch (PortletModeException ex) {
        throw new JspException(ex);
      }
    }

    if (_secure != null) {
      try {
        if (_secure == Boolean.FALSE)
          url.setSecure(false);
        else
          url.setSecure(true);
      }
      catch (PortletSecurityException ex) {
        throw new JspException(ex);
      }
    }

    if (_paramMap.size() > 0) {
      Iterator<Map.Entry<String,Object>> iter = _paramMap.entrySet().iterator();

      while (iter.hasNext()) {
        Map.Entry<String,Object> entry = iter.next();

        String name = entry.getKey();
        Object value = entry.getValue();

        if (value instanceof String)
          url.setParameter(name,(String) value);
        else
          url.setParameter(name, makeStringArray( (ArrayList<String>) value));
      }
    }

    if (_namespaceParamMap.size() > 0) {
      InvocationURL invocationUrl = (InvocationURL) url;

      Iterator<Map.Entry<String,LinkedHashMap<String,Object>>> iter 
        = _namespaceParamMap.entrySet().iterator();

      while (iter.hasNext()) {
        Map.Entry<String,LinkedHashMap<String,Object>> entry = iter.next();

        String namespace = entry.getKey();
        LinkedHashMap<String,Object> map = entry.getValue();

        Iterator<Map.Entry<String,Object>> mapIter = map.entrySet().iterator();

        while (mapIter.hasNext()) {
          Map.Entry<String,Object> mapEntry = mapIter.next();

          String name = mapEntry.getKey();
          Object value = mapEntry.getValue();

          if (value instanceof String) {
            invocationUrl.setParameter(namespace, name, (String) value);
          }
          else {
            String[] values = makeStringArray( (ArrayList<String>) value);
            invocationUrl.setParameter( namespace, name, values );
          }
        }
      }
    }

    String urlString = url.toString();

    if (_var != null) {
      pageContext.setAttribute(_var, urlString);
    }
    else {
      try {
        pageContext.getOut().print(urlString);
      }
      catch (IOException ex) {
        throw new JspException(ex);
      }
    }

    return EVAL_PAGE;
  }

  private String[] makeStringArray(ArrayList<String> list)
  {
    int sz = list.size();

    String[] array = new String[sz];

    for (int i = 0; i < sz; i++) {
      array[i] = list.get(i);
    }

    return array;
  }
}
