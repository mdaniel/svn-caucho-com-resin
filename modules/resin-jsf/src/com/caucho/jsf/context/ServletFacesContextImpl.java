/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package com.caucho.jsf.context;

import java.util.*;

import javax.el.*;

import javax.faces.*;
import javax.faces.application.*;
import javax.faces.context.*;
import javax.faces.component.*;
import javax.faces.render.*;

import javax.servlet.*;
import javax.servlet.http.*;

public class ServletFacesContextImpl extends FacesContext
{
  private final FacesContextFactoryImpl _factory;

  private ServletContext _webApp;
  private HttpServletRequest _request;
  private HttpServletResponse _response;

  private ExternalContext _externalContext;
  private FacesELContext _elContext;
  
  private boolean _isResponseComplete;  
  private boolean _isRenderResponse;
  
  private UIViewRoot _uiViewRoot;

  private HashMap<String,ArrayList<FacesMessage>> _messageMap
    = new HashMap<String,ArrayList<FacesMessage>>();

  private ResponseWriter _responseWriter;
  private ResponseStream _responseStream;

  private boolean _isClosed;
  
  ServletFacesContextImpl(FacesContextFactoryImpl factory,
			  ServletContext webApp,
			  HttpServletRequest request,
			  HttpServletResponse response)
  {
    _factory = factory;

    _webApp = webApp;
    _request = request;
    _response = response;
  }
  
  public Application getApplication()
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");
    
    return _factory.getApplication();
  }

  public ExternalContext getExternalContext()
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");
    
    if (_externalContext == null) {
      _externalContext
	= new ServletExternalContext(_webApp, _request, _response);
    }

    return _externalContext;
  }

  public RenderKit getRenderKit()
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");
    
    UIViewRoot viewRoot = getViewRoot();

    if (viewRoot == null)
      return null;

    String renderKitId = viewRoot.getRenderKitId();

    if (renderKitId == null)
      return null;

    RenderKitFactory factory = (RenderKitFactory)
      FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY);

    return factory.getRenderKit(this, renderKitId);
  }

  public ResponseStream getResponseStream()
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");
    
    return _responseStream;
  }

  public void setResponseStream(ResponseStream responseStream)
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");
    
     _responseStream = responseStream;
  }

  public ResponseWriter getResponseWriter()
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");
    
    if (_responseWriter == null) {
      try {
	_responseWriter = new ResponseWriterImpl(_response,
						 _response.getWriter());
      } catch (RuntimeException e) {
	throw e;
      } catch (Exception e) {
	throw new FacesException(e);
      }
    }

    return _responseWriter;
  }

  public void setResponseWriter(ResponseWriter writer)
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");
    
    _responseWriter = writer;
  }

  /**
   * Returns the root of the UI component tree.
   */
  public UIViewRoot getViewRoot()
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");
    
    if (_uiViewRoot == null) {
      _uiViewRoot = getApplication().getViewHandler().createView(this,
								 null);
    }
    
    return _uiViewRoot;
  }

  /**
   * Sets the root of the UI component tree.
   */
  public void setViewRoot(UIViewRoot root)
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");
    
    if (root == null)
      throw new NullPointerException();
    
    _uiViewRoot = root;
  }

  /**
   * If true the facelet will skip to the render phase.
   */
  @Override
  public boolean getRenderResponse()
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");
    
    return _isRenderResponse;
  }

  /**
   * Ask the lifecycle to skip to the render phase.
   */
  @Override
  public void renderResponse()
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");
    
    _isRenderResponse = true;
  }

  /**
   * Return true if the lifecycle should skip the response phase.
   */
  @Override
  public boolean getResponseComplete()
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");
    
    return _isResponseComplete;
  }

  /**
   * Ask the lifecycle to skip the response phase.
   */
  @Override
  public void responseComplete()
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");
    
    _isResponseComplete = true;
  }

  public void addMessage(String clientId,
			 FacesMessage message)
  {
    if (_isClosed)
      throw new IllegalStateException("FacesContext is closed");
    
    if (message == null)
      throw new NullPointerException();

    synchronized (_messageMap) {
      ArrayList<FacesMessage> messages = _messageMap.get(clientId);

      if (messages == null) {
	messages = new ArrayList<FacesMessage>();
	_messageMap.put(clientId, messages);
      }

      messages.add(message);
    }
  }

  public Iterator<String> getClientIdsWithMessages()
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");
    
    synchronized (_messageMap) {
      ArrayList<String> list = new ArrayList<String>(_messageMap.keySet());

      return list.iterator();
    }
  }

  public FacesMessage.Severity getMaximumSeverity()
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");
    
    synchronized (_messageMap) {
      FacesMessage.Severity severity = null;
      
      for (Map.Entry<String,ArrayList<FacesMessage>> entry
	     : _messageMap.entrySet()) {
	for (FacesMessage msg : entry.getValue()) {
	  if (severity == null)
	    severity = msg.getSeverity();
	  else if (severity.compareTo(msg.getSeverity()) < 0)
	    severity = msg.getSeverity();
	}
      }

      return severity;
    }
  }

  public Iterator<FacesMessage> getMessages()
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");
    
    synchronized (_messageMap) {
      ArrayList<FacesMessage> messages = new ArrayList<FacesMessage>();
      
      for (Map.Entry<String,ArrayList<FacesMessage>> entry
	     : _messageMap.entrySet()) {
	messages.addAll(entry.getValue());
      }

      //_messageMap.clear();

      return messages.iterator();
    }
  }

  public Iterator<FacesMessage> getMessages(String clientId)
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");
    
    synchronized (_messageMap) {
      ArrayList<FacesMessage> messages = _messageMap.get(clientId);

      if (messages == null)
	messages = new ArrayList<FacesMessage>();

      return messages.iterator();
    }
  }

  /**
   * @Since 1.2
   */
  @Override
  public ELContext getELContext()
  {
    if (_isClosed)
      throw new IllegalStateException(getClass().getName() + " is closed");
    
    if (_elContext == null) {
      _elContext = new FacesELContext(this, getApplication().getELResolver());
      _elContext.putContext(FacesContext.class, this);
    }

    return _elContext;
  }

  public void release()
  {
    _isClosed = true;
    FacesContext.setCurrentInstance(null);
  }

  public String toString()
  {
    return "ServletFacesContextImpl[]";
  }
}
