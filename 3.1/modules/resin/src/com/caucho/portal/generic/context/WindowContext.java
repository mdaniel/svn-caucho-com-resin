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

import com.caucho.portal.generic.Constraint;
import com.caucho.portal.generic.Invocation;
import com.caucho.portal.generic.Window;

import java.util.Locale;
import java.util.Map;
import java.util.Set;


/** 
 * Some of the state of the Context depends on the current portlet
 * being rendered.  The WindowContect class stores that state.
 * The ConnectionContext pushes WindowContext objects onto a stack as portlets
 * are recursively processed, and pops them off of the stack when processing
 * of them is done.
 *
 * This class just has getters and setters, it does not perform operations on
 * other classes and is just used by the ConnectionContext to store values. 
 */ 
public class WindowContext {
  private Window _window;
  private String _namespace;
  private int _stage;

  private Invocation _invocation;
  private Map<String, String[]> _actionMap;

  public boolean _didAction;

  private boolean _isExcluded;
  private int _constraintIndex;
  private Constraint _constraintFailure;
  private int _constraintFailureCode;
  private Exception _exception;

  private Map<String, String> _windowRequestAttributes;

  private boolean _isPrivate;
  private int _expirationCache;

  private LinkingPortletPreferences _preferences;
  private Map<String, String> _userAttributeMap;

  private Locale _responseLocale;
  private Set<Locale> _responseLocales;
  private String _responseCharacterEncoding;
  private Set<String> _responseCharacterEncodings;
  private String _responseContentType;
  private Set<String> _responseContentTypes;

  private ResponseHandler _parentResponseHandler;
  private ResponseHandler _responseHandler;

  public void start( Window window, 
                     String namespace )
  {
    _window = window;
    _namespace = namespace;
  }

  public void finish()
  {
    _invocation = null;
    _actionMap = null;
    _isExcluded = false;
    _constraintIndex = 0;
    _constraintFailure = null;
    _constraintFailureCode = 0;
    _exception = null;
    _isPrivate = false;
    _expirationCache = 0;
    _preferences = null;

    _responseLocale = null;
    _responseLocales = null;
    _responseCharacterEncoding = null;
    _responseCharacterEncodings = null;
    _responseContentType = null;
    _responseContentTypes = null;

    _responseHandler = null;
    _parentResponseHandler = null;

    _window = null;
    _namespace = null;
  }

  public Window getWindow()
  {
    return _window;
  }

  public String getNamespace()
  {
    return _namespace;
  }

  public void setDidAction()
  {
    _didAction = true;
  }

  public boolean getDidAction()
  {
    return _didAction;
  }

  public void setInvocation(Invocation invocation)
  {
    _invocation = invocation;
  }

  public Invocation getInvocation()
  {
    return _invocation;
  }

  public void setActionMap(Map<String, String[]> actionMap)
  {
    _actionMap = actionMap;
  }

  public Map<String, String[]> getActionMap()
  {
    return _actionMap;
  }

  public void setExcluded()
  {
    _isExcluded = true;
  }

  public boolean isExcluded()
  {
    return _isExcluded;
  }

  public void setConstraintIndex(int constraintIndex)
  {
    _constraintIndex = constraintIndex;
  }

  public int getConstraintIndex()
  {
    return _constraintIndex;
  }

  public void setConstraintFailure(Constraint constraint, int failureCode)
  {
    _constraintFailure = constraint;
    _constraintFailureCode = failureCode;
  }

  public boolean isConstraintFailure()
  {
    return _constraintFailure != null;
  }

  public Constraint getConstraintFailureConstraint()
  {
    return _constraintFailure;
  }

  public int getConstraintFailureCode()
  {
    return _constraintFailureCode;
  }

  public void setException(Exception ex)
  {
    _exception = ex;
  }

  public boolean isException()
  {
    return _exception != null;
  }

  public Exception getException()
  {
    return _exception;
  }

  public void setPrivate()
  {
    _isPrivate = true;
  }

  public boolean isPrivate()
  {
    return _isPrivate;
  }

  public void setExpirationCache(int expirationCache)
  {
    _expirationCache = expirationCache;
  }

  public int getExpirationCache()
  {
    return _expirationCache;
  }

  public void setPreferences(LinkingPortletPreferences preferences)
  {
    _preferences = preferences;
  }

  public LinkingPortletPreferences getPreferences()
  {
    return _preferences;
  }

  public void setUserAttributeMap(Map<String, String> userAttributeMap)
  {
    _userAttributeMap = userAttributeMap;
  }

  public Map<String, String> getUserAttributeMap()
  {
    return _userAttributeMap;
  }

  public void setResponseLocale(Locale responseLocale)
  {
    _responseLocale = responseLocale;
  }
  
  public Locale getResponseLocale()
  {
    return _responseLocale;
  }
  
  public void setResponseLocales(Set<Locale> responseLocales)
  {
    _responseLocales = responseLocales;
  }
  
  public Set<Locale> getResponseLocales()
  {
    return _responseLocales;
  }

  public void setResponseCharacterEncoding(String responseCharacterEncoding)
  {
    _responseCharacterEncoding = responseCharacterEncoding;
  }
  
  public String getResponseCharacterEncoding()
  {
    return _responseCharacterEncoding;
  }
  
  public void setResponseCharacterEncodings(Set<String> encodings)
  {
    _responseCharacterEncodings = encodings;
  }
  
  public Set<String> getResponseCharacterEncodings()
  {
    return _responseCharacterEncodings;
  }

  public void setResponseContentType(String responseContentType)
  {
    _responseContentType = responseContentType;
  }
  
  public String getResponseContentType()
  {
    return _responseContentType;
  }
  
  public void setResponseContentTypes(Set<String> responseContentTypes)
  {
    _responseContentTypes = responseContentTypes;
  }
  
  public Set<String> getResponseContentTypes()
  {
    return _responseContentTypes;
  }

  public void setParentResponseHandler(ResponseHandler parentResponseHandler)
  {
    _parentResponseHandler = parentResponseHandler;
  }

  public ResponseHandler getParentResponseHandler()
  {
    return _parentResponseHandler;
  }

  public void setResponseHandler(ResponseHandler responseHandler)
  {
    _responseHandler = responseHandler;
  }

  public ResponseHandler getResponseHandler()
  {
    return _responseHandler;
  }

  public void setWindowRequestAttributes(Map<String, String> windowRequestAttributes)
  {
    _windowRequestAttributes = windowRequestAttributes;

  }

  public Map<String, String> getWindowRequestAttributes()
  {
    return _windowRequestAttributes;
  }
}
