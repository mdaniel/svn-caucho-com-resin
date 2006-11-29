/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Sam
 */

package com.caucho.portal;

import com.caucho.util.L10N;

import javax.portlet.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PortletSupport extends GenericPortlet {
  private static final L10N L = new L10N(PortletSupport.class);

  static protected final Logger log = 
    Logger.getLogger(PortletSupport.class.getName());

  private static final Class[] _actionMethodParams
    = new Class[] { ActionRequest.class, ActionResponse.class };

  private static final Class[] _renderMethodParams
    = new Class[] { RenderRequest.class, RenderResponse.class };

  private Map<PortletMode, ActionProxy> _actionProxyCache 
    = Collections.synchronizedMap(new HashMap<PortletMode, ActionProxy>());

  private Map<PortletMode, RenderProxy> _renderProxyCache 
    = Collections.synchronizedMap(new HashMap<PortletMode, RenderProxy>());

  private interface ActionProxy {
    public void processAction(ActionRequest request, ActionResponse response)
      throws PortletException, IOException;
  }

  private interface RenderProxy {
    public void render(RenderRequest request, RenderResponse response)
      throws PortletException, IOException;
  }

  public void init()
    throws PortletException
  {
  }

  protected <T> T useBean( PortletRequest request, String name, Class<T> c)
  {
    return useBean( request, name, c, true );
  }

  protected <T> T useBean( PortletRequest request, 
                           String name, 
                           Class<T> c, 
                           boolean create )
  {
    T bean = (T) request.getAttribute( name );

    if (bean == null && create) {
      try {
        bean = c.newInstance();
      }
      catch (Exception ex) {
        throw new RuntimeException(ex);
      }

      request.setAttribute( name, bean );
    }

    return bean;
  }

  protected boolean isEmpty( String s )
  {
    return s == null || s.length() == 0;
  }

  protected static <S extends PortletMediator>
    S createMediator( RenderRequest request, 
                      RenderResponse response,
                      Class<S> mediatorClass )
    throws PortletException
  {
    String namespace = response.getNamespace();

    return createMediator( request, response, mediatorClass, namespace );
  }

  protected static <S extends PortletMediator>
    S createMediator( RenderRequest request, 
                      RenderResponse response,
                      Class<S> mediatorClass,
                      String namespace )
    throws PortletException
  {
    String attributeName = mediatorClass.getName();

    if ( namespace != null )
      attributeName = attributeName + namespace;

    S mediator = (S) request.getAttribute( attributeName );

    if ( mediator == null ) {
      try {
        mediator = (S) mediatorClass.newInstance();
      }
      catch ( Exception ex ) {
        throw new PortletException( ex );
      }

      request.setAttribute( attributeName, mediator );
    }

    mediator.setNamespace( namespace );
    mediator.setRequest( request );
    mediator.setResponse( response );

    return mediator;
  }

  /**
   * Prepare is called once for each request, either before the action method
   * appropriate for the mode is invoked, or if there is no action then
   * before a render appropriate for the mode is invoked.
   */
  protected void prepare( PortletRequest request, PortletResponse response )
    throws PortletException
  {
  }

  protected void checkPrepare(PortletRequest request, PortletResponse response)
    throws PortletException
  {

    String attributeName = "__prepared__" + System.identityHashCode(this);

    if (request.getAttribute( attributeName ) == null) {
      request.setAttribute( attributeName, Boolean.TRUE);

      if (log.isLoggable(Level.FINEST))
        log.finest(L.l("prepare for mode `{0}'", request.getPortletMode()));

      prepare(request, response);
    }
  }

  private ActionProxy findActionProxy( ActionRequest request, 
                                       ActionResponse response )
  {
    PortletMode mode = request.getPortletMode();
    ActionProxy proxy = _actionProxyCache.get(mode);

    if (proxy != null)
      return proxy;

    // try to find a method named "action" + mode

    try {
      String methodName = new StringBuffer()
        .append("action")
        .append(Character.toUpperCase(mode.toString().charAt(0)))
        .append(mode.toString().substring(1))
        .toString();

      if (log.isLoggable(Level.FINER))
        log.log(Level.FINER, "looking for method `" + methodName + "'");

      final Method method 
        = getClass().getMethod(methodName, _actionMethodParams);

      if (method != null) {
        proxy = new ActionProxy() {
          public void processAction( ActionRequest request, 
                                     ActionResponse response )
            throws PortletException, IOException
          {
            try {
              if (log.isLoggable(Level.FINEST))
                log.log(Level.FINER, "invoking method " + method);

              method.invoke( PortletSupport.this, 
                             new Object[] { request, response });
            }
            catch (IllegalAccessException ex) {
              throw new PortletException(ex);
            }
            catch (InvocationTargetException ex) {
              throw new PortletException(ex);
            }
          }
        };
      }
    }
    catch (NoSuchMethodException ex) {
      if (log.isLoggable(Level.FINE))
        log.log(Level.FINE, ex.toString(), ex);
    }

    if (proxy != null)
      _actionProxyCache.put(mode, proxy);

    return proxy;
  }

  public void processAction( ActionRequest request, ActionResponse response )
    throws PortletException, IOException
  {
    PortletMode mode = request.getPortletMode();

    if (log.isLoggable(Level.FINEST))
      log.finest(L.l("processAction for mode `{0}'", mode));

    ActionProxy proxy = findActionProxy( request, response );

    if (proxy == null)
      throw new PortletModeException( L.l("No action for mode `{0}'", mode),
          mode );

    checkPrepare( request, response );

    proxy.processAction( request, response);
  }

  private RenderProxy findRenderProxy( RenderRequest request, 
                                       RenderResponse response )
  {
    PortletMode mode = request.getPortletMode();

    RenderProxy proxy = _renderProxyCache.get(mode);

    if (proxy != null)
      return proxy;

    // try to find a .xtp/.jsp/.jspx file

    Iterator<String> candidates = getViewCandidates( request, response );

    while (candidates.hasNext()) {
      String candidate = candidates.next();

      if (log.isLoggable(Level.FINER))
        log.finer(L.l("view candidate `{0}'", candidate));

      if (new File(getPortletContext().getRealPath(candidate)).exists()) {
        final String target = candidate;

        proxy = new RenderProxy() {
          public void render(RenderRequest request, RenderResponse response)
            throws PortletException, IOException
          {
            dispatchView( request, response, target );
          }
        };

        break;
      }
    }

    if (proxy != null) {
      _renderProxyCache.put(mode, proxy);

      return proxy;
    }

    // try to find a method named "do" + mode

    String methodName = new StringBuffer()
      .append("do")
      .append(Character.toUpperCase(mode.toString().charAt(0)))
      .append(mode.toString().substring(1))
      .toString();

    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER, "looking for method `" + methodName + "'");

    try {
      final Method method 
        = getClass().getMethod(methodName, _renderMethodParams);

      if (method != null) {
        proxy = new RenderProxy() {
          public void render( RenderRequest request, RenderResponse response )
            throws PortletException, IOException
          {
            try {
              if (log.isLoggable(Level.FINER))
                log.log(Level.FINER, "invoking method " + method);

              if (log.isLoggable(Level.FINER))
                log.log(Level.FINER, "with " + request + " " + response);


              method.invoke( PortletSupport.this, 
                             new Object[] { request, response });
            }
            catch (IllegalAccessException ex) {
              throw new PortletException(ex);
            }
            catch (InvocationTargetException ex) {
              throw new PortletException(ex);
            }
          }
        };
      }
    } catch (NoSuchMethodException ex) {
      if (log.isLoggable(Level.FINEST))
        log.log(Level.FINEST, ex.toString(), ex);
    }

    if (proxy != null)
      _renderProxyCache.put(mode, proxy);

    return proxy;
  }

  protected Iterator<String> getViewCandidates( RenderRequest request, 
                                                RenderResponse response )
  {
    final String path = new StringBuffer()
      .append(getPortletName())
      .append('/')
      .append(request.getPortletMode().toString())
      .toString();

    
    return new Iterator<String>() {
      int i = 0;

      public boolean hasNext()
      {
        return i < 3;
      }

      public String next()
      {
        switch (i++) {
          case 0:
            return path + ".xtp";
          case 1:
            return path + ".jsp";
          case 2:
            return path + ".jspx";
          default:
            throw new NoSuchElementException();
        }
      }

      public void remove()
      {
        throw new UnsupportedOperationException();
      }
    };
  }

  public void render( RenderRequest request, RenderResponse response )
    throws PortletException, IOException
  {
    WindowState windowState = request.getWindowState();

    if (windowState.equals(WindowState.MINIMIZED))
      return;

    RenderProxy proxy = findRenderProxy( request, response );

    PortletMode mode = request.getPortletMode();

    if (log.isLoggable(Level.FINEST))
      log.finest(L.l("render for mode `{0}'", mode));

    if (proxy == null)
      throw new PortletModeException( L.l("No render for mode `{0}'", mode),
                                      mode );

    checkPrepare( request, response );

    if (response.getContentType() == null)
      response.setContentType("text/html");

    proxy.render( request, response );

  }

  protected void dispatchView( RenderRequest request, 
                               RenderResponse response, 
                               String path )
    throws PortletException, IOException
  {
    if (log.isLoggable(Level.FINEST))
      log.finest(L.l("dispatching view to `{0}'", path));

    PortletRequestDispatcher dispatcher 
        = getPortletContext().getRequestDispatcher(path);

    dispatcher.include(request, response);
  }
}
