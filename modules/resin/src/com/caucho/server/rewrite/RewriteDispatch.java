/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.rewrite;

import com.caucho.config.*;
import com.caucho.server.dispatch.DispatchServer;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Configuration for a rewrite-dispatch
 */
public class RewriteDispatch
{
  private static final L10N L = new L10N(RewriteDispatch.class);
  private static final Logger log
    = Logger.getLogger(RewriteDispatch.class.getName());

  private final WebApp _webApp;
  private final DispatchServer _dispatchServer;

  private MatchRule _matchRule;

  private BuilderProgramContainer _program = new BuilderProgramContainer();

  private final boolean _isFiner;
  private final boolean _isFinest;

  public RewriteDispatch(DispatchServer dispatchServer)
  {
    this(dispatchServer, null);
  }

  public RewriteDispatch(WebApp webApp)
  {
    this(null, webApp);
  }

  private RewriteDispatch(DispatchServer dispatchServer, WebApp webApp)
  {
    _dispatchServer = dispatchServer;
    _webApp = webApp;

    _isFiner = log.isLoggable(Level.FINER);
    _isFinest = log.isLoggable(Level.FINEST);
  }

  public WebApp getWebApp()
  {
    return _webApp;
  }

  /**
   * Adds to the builder program.
   */
  public void addBuilderProgram(BuilderProgram program)
  {
    _program.addProgram(program);
  }

  /*
  public AcceptRule createDispatch()
  {
    return _matchRule.createDispatch();
  }

  public void addDispatch(AcceptRule dispatch)
  {
    _matchRule.addDispatch(dispatch);
  }

  public ForwardRule createForward()
  {
    return _matchRule.createForward();
  }

  public void addForward(ForwardRule forward)
  {
    _matchRule.addForward(forward);
  }

  public ErrorRule createForbidden()
  {
    return _matchRule.createForbidden();
  }

  public void addForbidden(ErrorRule forbidden)
  {
    _matchRule.addForbidden(forbidden);
  }

  public ErrorRule createGone()
  {
    return _matchRule.createGone();
  }

  public void addGone(ErrorRule gone)
  {
    _matchRule.addGone(gone);
  }

  public ImportRule createImport()
  {
    return _matchRule.createImport();
  }

  public void addImport(ImportRule gone)
  {
    _matchRule.addImport(gone);
  }

  public LoadBalanceRule createLoadBalance()
  {
    return _matchRule.createLoadBalance();
  }

  public void addLoadBalance(LoadBalanceRule loadBalance)
  {
    _matchRule.addLoadBalance(loadBalance);
  }

  public MatchRule createMatch()
  {
    return _matchRule.createMatch();
  }

  public void addMatch(MatchRule match)
  {
    _matchRule.addMatch(match);
  }

  public MovedRule createMovedPermanently()
  {
    return _matchRule.createMovedPermanently();
  }

  public void addMovedPermanently(MovedRule moved)
  {
    _matchRule.addMovedPermanently(moved);
  }

  public ErrorRule createNotFound()
  {
    return _matchRule.createNotFound();
  }

  public void addNotFound(ErrorRule notFound)
  {
    _matchRule.addNotFound(notFound);
  }

  public ProxyRule createProxy()
  {
    return _matchRule.createProxy();
  }

  public void addProxy(ProxyRule proxy)
  {
    _matchRule.addProxy(proxy);
  }

  public RewriteRule createRewrite()
  {
    return _matchRule.createRewrite();
  }

  public void addRewrite(RewriteRule rewrite)
  {
    _matchRule.addRewrite(rewrite);
  }

  public RedirectRule createRedirect()
  {
    return _matchRule.createRedirect();
  }

  public void addRedirect(RedirectRule redirect)
  {
    _matchRule.addRedirect(redirect);
  }

  public SetRule createSet()
  {
    return _matchRule.createSet();
  }

  public void addSet(SetRule set)
  {
    _matchRule.addSet(set);
  }
  */

  @PostConstruct
  public void init()
  {
  }

  public FilterChain map(String uri, FilterChain next)
    throws ServletException
  {
    if (_isFinest)
      log.finest("rewrite-dispatch check uri '" + uri + "'");

    if (_matchRule == null || _matchRule.isModified()) {
      if (_matchRule != null)
	_matchRule.destroy();
      
      _matchRule = new MatchRule(this);
      _matchRule.setRegexp(Pattern.compile(".*"));

      _program.configure(_matchRule);

      _matchRule.init();
    }

    return _matchRule.map(uri, next);
  }

  public void clearCache()
  {
    if (_webApp != null)
      _webApp.clearCache();
    else if (_dispatchServer != null)
      _dispatchServer.clearCache();
  }

  /*
  @PreDestroy
  public void destroy()
  {
    _matchRule.destroy();
  }
  */
}
  
