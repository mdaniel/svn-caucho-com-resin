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

  private ResponseWriter _responseWriter;
  private ResponseStream _responseStream;
  
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
    return _factory.getApplication();
  }

  public Iterator<String> getClientIdsWithMessages()
  {
    throw new UnsupportedOperationException();
  }

  public ExternalContext getExternalContext()
  {
    if (_externalContext == null) {
      _externalContext
	= new ServletExternalContext(_webApp, _request, _response);
    }

    return _externalContext;
  }

  public FacesMessage.Severity getMaximumSeverity()
  {
    throw new UnsupportedOperationException();
  }

  public Iterator<FacesMessage> getMessages()
  {
    throw new UnsupportedOperationException();
  }

  public Iterator<FacesMessage> getMessages(String clientId)
  {
    throw new UnsupportedOperationException();
  }

  public RenderKit getRenderKit()
  {
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
    throw new UnsupportedOperationException();
  }

  public void setResponseStream(ResponseStream responseStream)
  {
    throw new UnsupportedOperationException();
  }

  public ResponseWriter getResponseWriter()
  {
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
    _responseWriter = writer;
  }

  /**
   * Returns the root of the UI component tree.
   */
  public UIViewRoot getViewRoot()
  {
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
    _uiViewRoot = root;
  }

  /**
   * If true the facelet will skip to the render phase.
   */
  @Override
  public boolean getRenderResponse()
  {
    return _isRenderResponse;
  }

  /**
   * Ask the lifecycle to skip to the render phase.
   */
  @Override
  public void renderResponse()
  {
    _isRenderResponse = true;
  }

  /**
   * Return true if the lifecycle should skip the response phase.
   */
  @Override
  public boolean getResponseComplete()
  {
    return _isResponseComplete;
  }

  /**
   * Ask the lifecycle to skip the response phase.
   */
  @Override
  public void responseComplete()
  {
    _isResponseComplete = true;
  }

  public void addMessage(String clientId,
				  FacesMessage message)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * @Since 1.2
   */
  public ELContext getELContext()
  {
    if (_elContext == null)
      _elContext = new FacesELContext();

    return _elContext;
  }

  public void release()
  {
  }
}
