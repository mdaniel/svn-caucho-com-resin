/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.rewrite;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;

import com.caucho.v5.http.dispatch.FilterChainForward;
import com.caucho.v5.http.dispatch.FilterChainRewriteDispatch;
import com.caucho.v5.http.dispatch.FilterChainRewriteInclude;
import com.caucho.v5.http.webapp.WebApp;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.PathImpl;

import io.baratine.config.Configurable;

/**
 * Manages the welcome-file as a rewrite-dispatch.
 *
 * <pre>
 * &lt;web-app xmlns:resin="urn:java:com.caucho.resin">
 *
 *   &lt;resin:WelcomeFile welcome-file="index.jsp"/>
 *
 * &lt;/web-app>
 * </pre>
 */
@Configurable
public class WelcomeFile extends DispatchRuleBase
{
  private static final Logger log 
    = Logger.getLogger(WelcomeFile.class.getName());
  private static final L10N L = new L10N(WelcomeFile.class);

  private static final HashSet<String> _welcomeFileResourceMap
    = new HashSet<String>();
  
  private final WebApp _webApp;
  
  private ArrayList<String> _welcomeFileList = new ArrayList<String>();
  
  public WelcomeFile()
  {
    WebApp webApp = WebApp.getCurrent();
    
    if (webApp == null)
      throw new IllegalStateException(L.l("{0} must have an active {1}.",
                                          WelcomeFile.class.getSimpleName(),
                                          WebApp.class.getSimpleName()));
    
    _webApp = webApp;
  }
  
  public WelcomeFile(ArrayList<String> welcomeFile)
  {
    this();

    if (welcomeFile != null) {
      _welcomeFileList.addAll(welcomeFile);
    }
  }

  @Override
  public boolean isInclude()
  {
    return true;
  }

  @Override
  public boolean isForward()
  {
    return true;
  }
  
  public void addWelcomeFile(String welcomeFile)
  {
    _welcomeFileList.add(welcomeFile);
  }

  @Override
  public String rewriteUri(String uri, String queryString)
  {
    return uri;
  }
  
  @Override
  public FilterChain map(DispatcherType type,
                         String uri,
                         String queryString,
                         FilterChain next,
                         FilterChain tail)
    throws ServletException
  {
    String welcomeUri = matchWelcomeFileResource(type, uri, null);

    if (welcomeUri == null) {
      return next;
    }

    // server/1u24
    /*
    if (queryString != null) {
      welcomeUri = welcomeUri + '?' + queryString;
    }
    */

    if (DispatcherType.INCLUDE.equals(type))
      return new FilterChainRewriteInclude(next, welcomeUri);
    else if (DispatcherType.FORWARD.equals(type))
      return new FilterChainForward(welcomeUri);
    else
      return new FilterChainRewriteDispatch(welcomeUri);
  }

  private String matchWelcomeFileResource(DispatcherType type,
                                          String uri,
                                          ArrayList<String> vars)
  {
    if (matchWelcomeUri(uri) != Match.NONE) {
      return null;
    }

    // String contextUri = invocation.getContextURI();
    
    try {
      PathImpl path = _webApp.getCauchoPath(uri);

      if (! path.exists())
        return null;
    } catch (Exception e) {
      if (log.isLoggable(Level.FINER))
        log.log(Level.FINER, L.l(
                                 "can't match a welcome file path {0}",
                                 uri), e);

      return null;
    }

    // String servletName = null;

    ArrayList<String> welcomeFileList = _welcomeFileList;
    int size = welcomeFileList.size();
    
    String bestWelcomeUri = null;
    Match bestMatch = Match.NONE;

    for (int i = 0; i < size; i++) {
      String file = welcomeFileList.get(i);
      
      String welcomeUri;

      if (uri.endsWith("/"))
        welcomeUri = uri + file;
      else if (! DispatcherType.REQUEST.equals(type)) {
        welcomeUri = uri + '/' + file;
      }
      else {
        continue;
      }

      Match match = matchWelcomeUri(welcomeUri);
      
      if (bestMatch.ordinal() < match.ordinal()) {
        bestMatch = Match.FILE;
        bestWelcomeUri = welcomeUri; 
      }
   }

    return bestWelcomeUri;
  }
  
  private Match matchWelcomeUri(String welcomeUri)
  {
    try {
      InputStream is;
      is = _webApp.getResourceAsStream(welcomeUri);

      if (is != null)
        is.close();

      if (is != null)
        return Match.FILE;
    } catch (Exception e) {
      log.fine("welcome-file lookup failed: " + welcomeUri);
      log.log(Level.ALL, e.toString(), e);
    }
      
    String servletClassName
      = _webApp.getDispatcher().getServletMapper().getServletClassByUri(welcomeUri);
    
    if (servletClassName != null
        && ! _welcomeFileResourceMap.contains(servletClassName)) {
      return Match.SERVLET;
    }
    
    return Match.NONE;
  }
  
  enum Match {
    NONE,
    SERVLET,
    FILE;
  }

  static {
    _welcomeFileResourceMap.add("com.caucho.v5.servlets.FileServlet");
    _welcomeFileResourceMap.add("com.caucho.v5.jsp.JspServlet");
    _welcomeFileResourceMap.add("com.caucho.v5.jsp.JspXmlServlet");
    _welcomeFileResourceMap.add("com.caucho.v5.quercus.servlet.QuercusServlet");
    _welcomeFileResourceMap.add("com.caucho.v5.jsp.XtpServlet");
    _welcomeFileResourceMap.add("com.caucho.xtpdoc.ResinDocServlet");
  }
}
