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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.webapp;

import com.caucho.config.*;
import com.caucho.config.types.InitProgram;
import com.caucho.server.dispatch.ErrorFilterChain;
import com.caucho.server.dispatch.ForwardFilterChain;
import com.caucho.server.dispatch.Invocation;
import com.caucho.server.dispatch.MovedFilterChain;
import com.caucho.server.dispatch.RedirectFilterChain;
import com.caucho.server.dispatch.ServletConfigImpl;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configuration for a rewrite-dispatch
 */
public class RewriteInvocation {
  private static final L10N L = new L10N(RewriteInvocation.class);
  private static final Logger log
    = Logger.getLogger(RewriteInvocation.class.getName());

  private final static FilterChain ACCEPT_CHAIN;

  private final WebApp _webApp;

  private final ArrayList<Program> _programList = new ArrayList<Program>();

  public RewriteInvocation()
  {
    _webApp = null;
  }

  public RewriteInvocation(WebApp webApp)
  {
    _webApp = webApp;
  }
  
  /**
   * Adds an accept
   */
  public void addDispatch(Accept accept)
  {
    _programList.add(accept);
  }

  /**
   * Adds a load-balance
   */
  public LoadBalance createLoadBalance()
  {
    if (_webApp == null)
      throw new ConfigException(L.l("<load-balance> requires a web-app.  Host-based <rewrite-dispatch> can not use <load-balance>."));

    LoadBalance loadBalance = new LoadBalance(_webApp);
    
    _programList.add(loadBalance);

    return loadBalance;
  }

  /**
   * Adds a moved-permanently
   */
  public void addMovedPermanently(Moved moved)
  {
    moved.setStatusCode(301);
    
    _programList.add(moved);
  }

  /**
   * Adds a forward.
   */
  public Forward createForward()
  {
    return new Forward();
  }

  /**
   * Adds a forward.
   */
  public void addForward(Forward forward)
  {
    _programList.add(forward);
  }

  /**
   * Adds a forbidden.
   */
  public Error createForbidden()
  {
    Error error = new Error(HttpServletResponse.SC_FORBIDDEN);
    
    _programList.add(error);

    return error;
  }

  /**
   * Adds a gone.
   */
  public Error createGone()
  {
    Error error = new Error(HttpServletResponse.SC_GONE);
    
    _programList.add(error);

    return error;
  }

  /**
   * Adds a not-found.
   */
  public Error createNotFound()
  {
    Error error = new Error(HttpServletResponse.SC_NOT_FOUND);
    
    _programList.add(error);

    return error;
  }

  /**
   * Adds a rewrite
   */
  public void addRewrite(Rewrite rewrite)
  {
    _programList.add(rewrite);
  }

  /**
   * Adds a redirect.
   */
  public void addRedirect(Redirect redirect)
  {
    _programList.add(redirect);
  }

  public FilterChain map(String uri, Invocation invocation)
    throws ServletException
  {
    for (int i = 0; i < _programList.size(); i++) {
      Program program = _programList.get(i);
      
      uri = program.rewrite(uri);

      FilterChain chain = program.dispatch(uri);

      if (chain == ACCEPT_CHAIN)
	return null;
      else if (chain != null)
	return chain;
    }
    
    return null;
  }

  abstract static class Program {
    private final Logger _log;
    private final boolean _isFiner;
    private final boolean _isFinest;

    protected Program(Logger log)
    {
      _log = log;
      _isFiner = log.isLoggable(Level.FINER);
      _isFinest = log.isLoggable(Level.FINEST);
    }

    abstract public String getTagName();

    final protected void logMatch(Pattern regexp, String uri)
    {
      if (_isFiner)
        _log.finer(getTagName() + " " + regexp.pattern() + ": '" + uri + "'  match");
    }

    final protected void logMatch(Pattern regexp, String uri, String targetUri)
    {
      if (_isFiner)
        log.finer(getTagName() + " " + regexp.pattern() + ": '" + uri + "'  -->  '" + targetUri + "'");
    }

    final protected void logNoMatch(Pattern regexp, String uri)
    {
      if (_isFinest)
        _log.finest(getTagName() + " " + regexp.pattern() + ": '" + uri + "'  no match");
    }

    public String rewrite(String uri)
    {
      return uri;
    }
    
    public FilterChain dispatch(String uri)
      throws ServletException
    {
      return null;
    }
  }

  public static class Rewrite extends Program {
    private static final Logger log = Logger.getLogger(Rewrite.class.getName());

    private Pattern _regexp;
    private String _replacement;

    public Rewrite()
    {
      super(log);
    }

    @Override
    public String getTagName()
    {
      return "rewrite";
    }

    /**
     * Sets the regular expression.
     */
    public void setRegexp(String regexp)
    {
      _regexp = Pattern.compile(regexp);
    }

    /**
     * Sets the target.
     */
    public void setReplacement(String replacement)
    {
      _replacement = replacement;
    }

    /**
     * Init
     */
    @PostConstruct
    public void init()
      throws ConfigException
    {
      if (_regexp == null)
	throw new ConfigException(L.l("{0} needs 'regexp' attribute.",
				      getTagName()));
      if (_replacement == null)
	throw new ConfigException(L.l("{0} needs 'replacement' attribute.",
				      getTagName()));
    }

    @Override
    public String rewrite(String uri)
    {
      Matcher matcher = _regexp.matcher(uri);

      if (matcher.find()) {
	matcher.reset();
	String targetUri =  matcher.replaceAll(_replacement);

        logMatch(_regexp, uri, targetUri);

        return targetUri;
      }
      else {
        logNoMatch(_regexp, uri);

        return uri;
      }
    }
  }

  public static class Accept extends Program {
    private static final Logger log = Logger.getLogger(Accept.class.getName());

    private Pattern _regexp;

    public Accept()
    {
      super(log);
    }

    @Override
    public String getTagName()
    {
      return "accept";
    }

    /**
     * Sets the regular expression.
     */
    public void setRegexp(String regexp)
    {
      _regexp = Pattern.compile(regexp);
    }

    /**
     * Init
     */
    @PostConstruct
    public void init()
      throws ConfigException
    {
      if (_regexp == null)
	throw new ConfigException(L.l("{0} needs 'regexp' attribute.",
				      getTagName()));
    }

    @Override
    public FilterChain dispatch(String uri)
    {
      Matcher matcher = _regexp.matcher(uri);

      if (matcher.find()) {
        logMatch(_regexp, uri);

        return ACCEPT_CHAIN;
      }
      else {
        logNoMatch(_regexp, uri);

        return null;
      }
    }
  }

  public static class Redirect extends Program {
    private static final Logger log = Logger.getLogger(Redirect.class.getName());

    private String _target;
    private Pattern _regexp;

    public Redirect()
    {
      super(log);
    }

    @Override
    public String getTagName()
    {
      return "redirect";
    }

    /**
     * Sets the regular expression.
     */
    public void setRegexp(String regexp)
    {
      _regexp = Pattern.compile(regexp);
    }

    public void setTarget(String target)
    {
      _target = target;
    }

    @Override
    public FilterChain dispatch(String uri)
    {
      Matcher matcher = _regexp.matcher(uri);

      if (matcher.find()) {
        matcher.reset();
        String targetUri = matcher.replaceAll(_target);

        logMatch(_regexp, uri, targetUri);

        return new RedirectFilterChain(targetUri);
      }
      else {
        logNoMatch(_regexp, uri);

        return null;
      }
    }

    @PostConstruct
    public void init()
      throws ConfigException
    {
      if (_regexp == null)
	throw new ConfigException(L.l("redirect needs 'regexp' attribute."));
      if (_target == null)
	throw new ConfigException(L.l("redirect needs 'target' attribute."));
    }
  }

  public static class Moved extends Program {
    private static final Logger log = Logger.getLogger(Moved.class.getName());

    private int _code = 302;

    private String _tagName = "moved";
    private String _target;
    private Pattern _regexp;

    public Moved()
    {
      super(log);
    }

    @Override
    public String getTagName()
    {
      return _tagName;
    }

    /**
     * Sets the regular expression.
     */
    public void setRegexp(String regexp)
    {
      _regexp = Pattern.compile(regexp);
    }

    public void setTarget(String target)
    {
      _target = target;
    }

    /**
     * Sets the redirect code.
     */
    public void setStatusCode(int code)
    {
      _code = code;
    }

    @Override
    public FilterChain dispatch(String uri)
    {
      Matcher matcher = _regexp.matcher(uri);

      if (matcher.find()) {
	matcher.reset();
	String targetUri = matcher.replaceAll(_target);

        logMatch(_regexp, uri, targetUri);

        return new MovedFilterChain(_code, targetUri);
      }
      else {
        logNoMatch(_regexp, uri);
	return null;
      }
    }

    @PostConstruct
    public void init()
      throws ConfigException
    {
      if (_regexp == null)
	throw new ConfigException(L.l("moved needs 'regexp' attribute."));
      if (_target == null)
	throw new ConfigException(L.l("moved needs 'target' attribute."));

      _tagName = "moved(" + _code + ")";
    }
  }

  public static class LoadBalance extends Program {
    private static final Logger log = Logger.getLogger(LoadBalance.class.getName());

    private final WebApp _webApp;
    
    private Pattern _regexp;
    private ServletConfigImpl _servlet;

    private BuilderProgramContainer _program = new BuilderProgramContainer();

    LoadBalance(WebApp webApp)
    {
      super(log);
      _webApp = webApp;
    }

    @Override
    public String getTagName()
    {
      return "load-balance";
    }

    /**
     * Sets the regular expression.
     */
    public void setRegexp(String regexp)
    {
      _regexp = Pattern.compile(regexp);
    }

    public void addBuilderProgram(BuilderProgram program)
    {
      _program.addProgram(program);
    }

    @Override
    public FilterChain dispatch(String uri)
      throws ServletException
    {
      Matcher matcher = _regexp.matcher(uri);

      if (matcher.find()) {
        logMatch(_regexp, uri);
        return _servlet.createServletChain();
      }
      else {
        logNoMatch(_regexp, uri);
	return null;
      }
    }

    @PostConstruct
    public void init()
      throws ConfigException, ServletException
    {
      if (_regexp == null)
	throw new ConfigException(L.l("load-balance needs 'regexp' attribute."));

      try {
	_servlet = new ServletConfigImpl();

	_servlet.setServletName("resin-dispatch-lb");
	Class cl = Class.forName("com.caucho.servlets.LoadBalanceServlet");
	_servlet.setServletClass("com.caucho.servlets.LoadBalanceServlet");

	_servlet.setInit(new InitProgram(_program));

	_webApp.addServlet(_servlet);
      } catch (ClassNotFoundException e) {
	log.log(Level.FINER, e.toString(), e);
	
	throw new ConfigException(L.l("load-balance requires Resin Professional"));
      }
    }
  }

  public static class Forward extends Program {
    private static final Logger log = Logger.getLogger(Forward.class.getName());

    private String _target;
    private Pattern _regexp;

    public Forward()
    {
      super(log);
    }

    @Override
    public String getTagName()
    {
      return "forward";
    }

    /**
     * Sets the regular expression.
     */
    public void setRegexp(String regexp)
    {
      _regexp = Pattern.compile(regexp);
    }

    public void setTarget(String target)
    {
      _target = target;
    }

    @Override
    public FilterChain dispatch(String uri)
    {
      Matcher matcher = _regexp.matcher(uri);

      if (matcher.find()) {
	matcher.reset();
	
	String targetUri = matcher.replaceAll(_target);

        logMatch(_regexp, uri, targetUri);

        return new ForwardFilterChain(targetUri);
      }
      else {
        logNoMatch(_regexp, uri);
        return null;
      }
    }

    @PostConstruct
    public void init()
      throws ConfigException
    {
      if (_regexp == null)
	throw new ConfigException(L.l("redirect needs 'regexp' attribute."));
      if (_target == null)
	throw new ConfigException(L.l("redirect needs 'target' attribute."));
    }
  }

  public static class Error extends Program {
    private static final Logger log = Logger.getLogger(Error.class.getName());

    private final String _tagName;
    private final int _code;
    private Pattern _regexp;

    Error(int code)
    {
      super(log);
      _code = code;
      _tagName = "error(" + _code + ")";
    }

    @Override
    public String getTagName()
    {
      return _tagName;
    }

    /**
     * Sets the regular expression.
     */
    public void setRegexp(String regexp)
    {
      _regexp = Pattern.compile(regexp);
    }

    @Override
    public FilterChain dispatch(String uri)
    {
      Matcher matcher = _regexp.matcher(uri);

      if (matcher.find()) {
	matcher.reset();

        logMatch(_regexp, uri);

        return new ErrorFilterChain(_code);
      }
      else {
        logNoMatch(_regexp, uri);
	return null;
      }
    }

    @PostConstruct
    public void init()
      throws ConfigException
    {
      if (_regexp == null)
	throw new ConfigException(L.l("error needs 'regexp' attribute."));
    }
  }

  static {
    ACCEPT_CHAIN = new FilterChain() {
	public void doFilter(ServletRequest req, ServletResponse res) {}
      };
  }
}
  
