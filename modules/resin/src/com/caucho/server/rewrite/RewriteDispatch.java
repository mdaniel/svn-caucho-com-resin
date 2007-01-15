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

import com.caucho.config.BuilderProgram;
import com.caucho.config.BuilderProgramContainer;
import com.caucho.config.ConfigException;
import com.caucho.config.types.InitProgram;
import com.caucho.server.dispatch.ErrorFilterChain;
import com.caucho.server.dispatch.ForwardFilterChain;
import com.caucho.server.dispatch.Invocation;
import com.caucho.server.dispatch.MovedFilterChain;
import com.caucho.server.dispatch.RedirectFilterChain;
import com.caucho.server.dispatch.ServletConfigImpl;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.L10N;
import com.caucho.vfs.CaseInsensitive;

import javax.annotation.PostConstruct;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.el.ELContext;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configuration for a rewrite-dispatch
 */
public class RewriteDispatch {
  private static final L10N L = new L10N(RewriteDispatch.class);
  private static final Logger log
    = Logger.getLogger(RewriteDispatch.class.getName());

  private final static FilterChain ACCEPT_CHAIN;

  private final WebApp _webApp;

  private final ArrayList<Program> _programList = new ArrayList<Program>();

  private final RewriteContext _parseContext = new RewriteContext(this);

  private final boolean _isFiner;
  private final boolean _isFinest;

  private boolean _isCaseInsensitive = CaseInsensitive.isCaseInsensitive();

  public RewriteDispatch()
  {
    this(null);
  }

  public RewriteDispatch(WebApp webApp)
  {
    _webApp = webApp;

    _isFiner = log.isLoggable(Level.FINER);
    _isFinest = log.isLoggable(Level.FINEST);
  }

  /**
   * Sets the case insensitivity for regexp matching of uri's,
   * default is obtained from the environment.
   */
  public void setCaseInsensitive(boolean caseInsensitive)
  {
    _isCaseInsensitive = caseInsensitive;
  }

  public boolean isCaseInsensitive()
  {
    return _isCaseInsensitive;
  }

  private <T extends Program> T add(T program)
  {
    _programList.add(program);

    return program;
  }

  public void addProgram(Program program)
  {
    add(program);
  }

  /**
   * Adds an accept
   */
  public Accept createDispatch()
  {
    return new Accept();
  }

  public void addDispatch(Accept dispatch)
  {
    dispatch.init();
    add(dispatch);
  }

  /**
   * Adds a forbidden.
   */
  public Error createForbidden()
  {
    return new Error(HttpServletResponse.SC_FORBIDDEN);
  }

  public void addForbidden(Error forbidden)
  {
    forbidden.init();
    add(forbidden);
  }

  /**
   * Adds a forward.
   */
  public Forward createForward()
  {
    return new Forward();
  }

  public void addForward(Forward forward)
  {
    forward.init();
    add(forward);
  }

  /**
   * Adds a gone.
   */
  public Error createGone()
  {
    return new Error(HttpServletResponse.SC_GONE);
  }

  public void addGone(Error gone)
  {
    gone.init();
    add(gone);
  }

  /**
   * Adds a load-balance
   */
  public LoadBalance createLoadBalance()
  {
    if (_webApp == null)
      throw new ConfigException(L.l("<load-balance> requires a web-app.  Host-based <rewrite-dispatch> can not use <load-balance>."));

    return new LoadBalance(_webApp);
  }

  public void addLoadBalance(LoadBalance loadBalance)
  {
    loadBalance.init();
    add(loadBalance);
  }

  /**
   * Adds a moved permanently (301)
   */
  public Moved createMovedPermanently()
  {
    return new Moved(HttpServletResponse.SC_MOVED_PERMANENTLY);
  }

  public void addMovedPermanently(Moved moved)
  {
    moved.init();
    add(moved);
  }

  /**
   * Adds a not-found.
   */
  public Error createNotFound()
  {
    return new Error(HttpServletResponse.SC_NOT_FOUND);
  }

  public void addNotFound(Error notFound)
  {
    notFound.init();
    add(notFound);
  }

  /**
   * Adds a redirect.
   */
  public Redirect createRedirect()
  {
    return new Redirect();
  }

  public void addRedirect(Redirect redirect)
  {
    redirect.init();
    add(redirect);
  }

  /**
   * Adds a rewrite
   */
  public Rewrite createRewrite()
  {
    return new Rewrite();
  }

  public void addRewrite(Rewrite rewrite)
  {
    rewrite.init();
    add(rewrite);
  }

  public FilterChain map(String uri, Invocation invocation, FilterChain next)
    throws ServletException
  {
    return map(new RewriteContext(this), uri, invocation, next,  0);
  }

  FilterChain map(RewriteContext rewriteContext,
                  String uri,
                  Invocation invocation,
                  FilterChain nextChain,
                  int start)
    throws ServletException
  {
    if (_isFinest)
      log.finest("rewrite-dispatch check uri '" + uri + "'");

    for (int i = start; i < _programList.size(); i++) {
      Program program = _programList.get(i);

      if (program.isSecureSet()) {
        if (!invocation.isSecure() == program.isSecure()) {
          if (_isFinest)
            log.finest(program.getLogPrefix()
                       + (invocation.isSecure() ? " request is not secure" : " request is secure")
                       + ", no match");
          continue;
        }
      }

      if (program.isPortSet()) {
        if (invocation.getPort() != program.getPort()) {
          if (_isFinest)
            log.finest(program.getLogPrefix()
                       + " request port is "
                       + invocation.getPort()
                       + ", no match");
          continue;
        }
      }

      Matcher matcher = program.matcher(uri);

      if (!matcher.find()) {
        if (_isFinest)
          log.finest(program.getLogPrefix() + " no match");

        continue;
      }

      String targetUri = program.rewrite(uri, matcher);

      FilterChain programChain = program.dispatch(targetUri);

      AbstractConditions conditions = program.getConditions();

      if (conditions != null) {
        FilterChain passChain;

        if (programChain == null) {
          passChain = new ContinueMapFilterChain(rewriteContext,
                                                 targetUri,
                                                 invocation,
                                                 nextChain,
                                                 i + 1);
        }
        else if (programChain == ACCEPT_CHAIN) {
          passChain = nextChain;
        }
        else
          passChain = programChain;

        FilterChain failChain
          = new ContinueMapFilterChain(rewriteContext,
                                       uri,
                                       invocation,
                                       nextChain,
                                       i + 1);

        nextChain = new ConditionFilterChain(rewriteContext,
                                             program.getLogPrefix(),
                                             uri,
                                             targetUri,
                                             conditions,
                                             passChain,
                                             failChain);

        break;
      }
      else {
        if (_isFiner)
          log.finer(program.getLogPrefix() + " '" + uri + "' --> '" + targetUri + "'");

        if (programChain == null)
          continue;
        else if (programChain != ACCEPT_CHAIN)
          nextChain = programChain;

        break;
      }
    }

    return nextChain;
  }

  ELContext getParseContext()
  {
    return _parseContext;
  }

  public abstract class Program {
    private Pattern _regexp;
    private Boolean _secure = null;
    private int _port = -1;
    private AndConditions _conditions;
    private String _logPrefix;

    protected Program()
    {
      _logPrefix = getTagName();
    }

    abstract public String getTagName();

    /**
     * Sets the regular expression pattern that must match the uri for the
     * program to match, required.
     */
    public void setRegexp(String regexp)
    {
      if (isCaseInsensitive())
        _regexp = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE);
      else
        _regexp = Pattern.compile(regexp);
    }

    public Pattern getRegexp()
    {
      return _regexp;
    }

    /**
     * Set's the requirement that the request be secure or not secure for the
     * program to match, default is to match both.
     */
    public void setSecure(boolean secure)
    {
      _secure = secure;
    }

    public boolean isSecureSet()
    {
      return _secure != null;
    }

    public boolean isSecure()
    {
      return _secure;
    }

    /**
     * Set's the requirement that the request be from a certain port,
     * default is to match any port.
     */
    public void setPort(int port)
    {
      _port = port;
    }

    public boolean isPortSet()
    {
      return _port != -1;
    }

    public int getPort()
    {
      return _port;
    }

    /**
     * Create a set of conditions that must pass for the program to match,
     * default is no conditions.
     */
    public AndConditions createConditions()
    {
      if (_conditions == null)
        _conditions = new AndConditions(RewriteDispatch.this);

      return _conditions;
    }

    AbstractConditions getConditions()
    {
      return _conditions;
    }

    /**
     * Throws an exception if the passed value is null.
     */
    protected void required(Object value, String name)
      throws ConfigException
    {
      if (value == null)
        throw new ConfigException(L.l("{0} requires '{1}' attribute.",
                                      getTagName(), name));
    }

    @PostConstruct
    public void init()
      throws ConfigException
    {
      _logPrefix = getTagName() + " " + getRegexp().pattern();

      if (isSecureSet())
        _logPrefix = _logPrefix + " (secure)";

      if (isPortSet())
        _logPrefix = _logPrefix + " (port " + getPort() + ")";

      required(_regexp, "regexp");
    }

    public String getLogPrefix()
    {
      return _logPrefix;
    }

    public Matcher matcher(String uri)
    {
      return _regexp.matcher(uri);
    }

    public String rewrite(String uri, Matcher matcher)
    {
      return uri;
    }

    public FilterChain dispatch(String uri)
      throws ServletException
    {
      return null;
    }

  }

  public class Accept extends Program {
    @Override
    public String getTagName()
    {
      return "accept";
    }

    @Override
    public FilterChain dispatch(String uri)
    {
      return ACCEPT_CHAIN;
    }
  }

  public class Error extends Program {
    private final String _tagName;
    private final int _code;

    Error(int code)
    {
      _code = code;
      _tagName = "error(" + _code + ")";
    }

    @Override
    public String getTagName()
    {
      return _tagName;
    }

    @Override
    public FilterChain dispatch(String uri)
    {
      return new ErrorFilterChain(_code);
    }
  }

  public class Forward extends Program {
    private String _target;

    @Override
    public String getTagName()
    {
      return "forward";
    }

    public void setTarget(String target)
    {
      _target = target;
    }

    @Override
    public String rewrite(String uri, Matcher matcher)
    {
      return matcher.replaceAll(_target);
    }

    @Override
    public FilterChain dispatch(String uri)
    {
      return new ForwardFilterChain(uri);
    }

    @PostConstruct
    public void init()
      throws ConfigException
    {
      super.init();

      required(_target, "target");
    }
  }

  public class LoadBalance extends Program {
    private final WebApp _webApp;

    private ServletConfigImpl _servlet;

    private BuilderProgramContainer _program = new BuilderProgramContainer();

    LoadBalance(WebApp webApp)
    {
      _webApp = webApp;
    }

    @Override
    public String getTagName()
    {
      return "load-balance";
    }

    public void addBuilderProgram(BuilderProgram program)
    {
      _program.addProgram(program);
    }

    @Override
    public FilterChain dispatch(String uri)
      throws ServletException
    {
      return _servlet.createServletChain();
    }

    @PostConstruct
    public void init()
      throws ConfigException
    {
      super.init();

      try {
        _servlet = new ServletConfigImpl();

        _servlet.setServletName("resin-dispatch-lb");
        Class cl = Class.forName("com.caucho.servlets.LoadBalanceServlet");
        _servlet.setServletClass("com.caucho.servlets.LoadBalanceServlet");

        _servlet.setInit(new InitProgram(_program));

        _webApp.addServlet(_servlet);
      }
      catch (ServletException ex) {
        throw new ConfigException(ex);
      }
      catch (ClassNotFoundException e) {
        log.log(Level.FINER, e.toString(), e);

        throw new ConfigException(L.l("load-balance requires Resin Professional"));
      }
    }
  }

  public class Moved extends Program {
    private String _tagName;
    private int _code;
    private String _target;

    public Moved(int statusCode)
    {
      _code = statusCode;
      _tagName = "moved(" + statusCode + ")";
    }

    @Override
    public String getTagName()
    {
      return _tagName;
    }

    public void setTarget(String target)
    {
      _target = target;
    }

    public String rewrite(String uri, Matcher matcher)
    {
      return matcher.replaceAll(_target);
    }

    @Override
    public FilterChain dispatch(String uri)
    {
      return new MovedFilterChain(_code, uri);
    }

    @PostConstruct
    public void init()
      throws ConfigException
    {
      super.init();

      required(_target, "target");
    }
  }

  public class Redirect extends Program {
    private String _target;

    @Override
    public String getTagName()
    {
      return "redirect";
    }

    public void setTarget(String target)
    {
      _target = target;
    }

    @Override
    public String rewrite(String uri, Matcher matcher)
    {
      return matcher.replaceAll(_target);
    }

    @Override
    public FilterChain dispatch(String uri)
    {
      return new RedirectFilterChain(uri);
    }

    @PostConstruct
    public void init()
      throws ConfigException
    {
      super.init();

      required(_target, "target");
    }
  }

  public class Rewrite extends Program {
    private String _replacement;

    @Override
    public String getTagName()
    {
      return "rewrite";
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
      super.init();

      required(_replacement, "replacement");
    }

    @Override
    public String rewrite(String uri, Matcher matcher)
    {
      return matcher.replaceAll(_replacement);
    }
  }

  static {
    ACCEPT_CHAIN = new FilterChain() {
        public void doFilter(ServletRequest req, ServletResponse res) {}
      };
  }
}
  
