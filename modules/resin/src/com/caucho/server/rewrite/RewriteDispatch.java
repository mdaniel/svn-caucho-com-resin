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
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.types.InitProgram;
import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.RewriteRuleMXBean;
import com.caucho.server.dispatch.*;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.L10N;
import com.caucho.vfs.CaseInsensitive;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.el.ELContext;
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
public class RewriteDispatch
{
  private static final L10N L = new L10N(RewriteDispatch.class);
  private static final Logger log
    = Logger.getLogger(RewriteDispatch.class.getName());

  private final static FilterChain ACCEPT_CHAIN;

  private final WebApp _webApp;
  private final DispatchServer _dispatchServer;

  private final RewriteContext _parseContext = new RewriteContext(this);

  private ArrayList<AbstractRule> _ruleList = new ArrayList<AbstractRule>();
  private AbstractRule[] _rules;


  private final boolean _isFiner;
  private final boolean _isFinest;

  private boolean _isCaseInsensitive = CaseInsensitive.isCaseInsensitive();

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

  private <T extends AbstractRule> T add(T program)
  {
    _ruleList.add(program);

    return program;
  }

  public void addProgram(AbstractRule rule)
  {
    add(rule);
  }

  /**
   * Adds an accept
   */
  public AcceptRule createDispatch()
  {
    return new AcceptRule();
  }

  public void addDispatch(AcceptRule dispatch)
  {
    add(dispatch);
  }

  /**
   * Adds a forbidden.
   */
  public ErrorRule createForbidden()
  {
    return new ErrorRule(HttpServletResponse.SC_FORBIDDEN);
  }

  public void addForbidden(ErrorRule forbidden)
  {
    add(forbidden);
  }

  /**
   * Adds a forward.
   */
  public ForwardRule createForward()
  {
    return new ForwardRule();
  }

  public void addForward(ForwardRule forward)
  {
    add(forward);
  }

  /**
   * Adds a gone.
   */
  public ErrorRule createGone()
  {
    return new ErrorRule(HttpServletResponse.SC_GONE);
  }

  public void addGone(ErrorRule gone)
  {
    add(gone);
  }

  /**
   * Adds a load-balance
   */
  public LoadBalanceRule createLoadBalance()
  {
    if (_webApp == null)
      throw new ConfigException(L.l("<load-balance> requires a web-app.  Host-based <rewrite-dispatch> can not use <load-balance>."));

    return new LoadBalanceRule(_webApp);
  }

  public void addLoadBalance(LoadBalanceRule loadBalance)
  {
    add(loadBalance);
  }

  /**
   * Adds a moved permanently (301)
   */
  public MovedRule createMovedPermanently()
  {
    return new MovedRule(HttpServletResponse.SC_MOVED_PERMANENTLY);
  }

  public void addMovedPermanently(MovedRule moved)
  {
    add(moved);
  }

  /**
   * Adds a not-found.
   */
  public ErrorRule createNotFound()
  {
    return new ErrorRule(HttpServletResponse.SC_NOT_FOUND);
  }

  public void addNotFound(ErrorRule notFound)
  {
    add(notFound);
  }

  /**
   * Adds a redirect.
   */
  public RedirectRule createRedirect()
  {
    return new RedirectRule();
  }

  public void addRedirect(RedirectRule redirect)
  {
    add(redirect);
  }

  /**
   * Adds a rewrite
   */
  public RewriteRule createRewrite()
  {
    return new RewriteRule();
  }

  public void addRewrite(RewriteRule rewrite)
  {
    add(rewrite);
  }

  @PostConstruct
  public void init()
  {
    _rules = _ruleList.toArray(new AbstractRule[_ruleList.size()]);
    _ruleList = null;
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

    for (int i = start; i < _rules.length; i++) {
      AbstractRule rule = _rules[i];

      if (!rule.isEnabled())
        continue;

      if (rule.isSecureSet()) {
        if (!invocation.isSecure() == rule.isSecure()) {
          if (_isFinest)
            log.finest(rule.getLogPrefix()
                       + (invocation.isSecure() ? " request is not secure" : " request is secure")
                       + ", no match");
          continue;
        }
      }

      if (rule.isPortSet()) {
        if (invocation.getPort() != rule.getPort()) {
          if (_isFinest)
            log.finest(rule.getLogPrefix()
                       + " request port is "
                       + invocation.getPort()
                       + ", no match");
          continue;
        }
      }

      Matcher matcher = rule.matcher(uri);

      if (! matcher.find()) {
        if (_isFinest)
          log.finest(rule.getLogPrefix() + " does not match " + uri);

        continue;
      }

      String targetUri = rule.rewrite(uri, matcher);

      FilterChain programChain = rule.dispatch(targetUri);

      Condition []conditions = rule.getConditions();

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

        nextChain = new ConditionFilterChain(rule.getLogPrefix(),
                                             uri,
                                             targetUri,
                                             conditions,
                                             passChain,
                                             failChain);

        break;
      }
      else {
        if (_isFiner)
          log.finer(rule.getLogPrefix() + " '" + uri + "' --> '" + targetUri + "'");

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

  public void clearCache()
  {
    if (_webApp != null)
      _webApp.clearCache();
    else if (_dispatchServer != null)
      _dispatchServer.clearCache();
  }

  @PreDestroy
  public void destroy()
  {
    AbstractRule[] rules = _rules;
    _rules = null;

    if (rules != null) {
      for (AbstractRule rule : rules) {
        // XXX: s/b  Config.destroy(rule);
        rule.destroy();
      }
    }
  }


  public abstract class AbstractRule
  {
    private Pattern _regexp;
    private String _name;
    private Boolean _secure = null;
    private int _port = -1;
    volatile private boolean _isEnabled = true;
    private ArrayList<Condition> _conditionList = new ArrayList<Condition>();

    private Condition []_conditions;
    private String _logPrefix;
    private RewriteRuleAdmin _admin;

    protected AbstractRule()
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

    public void setName(String name)
    {
      _name = name;
    }

    public String getName()
    {
      return _name;
    }

    public void setEnabled(boolean isEnabled)
    {
      if (_isEnabled != isEnabled) {
        _isEnabled = isEnabled;

        clearCache();
      }
    }

    public boolean isEnabled()
    {
      return _isEnabled;
    }

    /**
     * Sets the requirement that the request be secure or not secure for the
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
     * Add a list of conditions that must pass for the program to match.
     */
    public void addAnd(AndConditions condition)
    {
      _conditionList.add(condition);
    }

    /**
     * Add a list of conditions one of which must pass for the program to match.
     */
    public void addOr(OrConditions condition)
    {
      _conditionList.add(condition);
    }

    /**
     * Add a list of conditions that must not pass for the program to match.
     */
    public void addNot(NotConditions condition)
    {
      _conditionList.add(condition);
    }

    /**
     * Add a condition that must pass for the program to match.
     */
    public void addWhen(ConditionConfig condition)
    {
      _conditionList.add(condition.getCondition());
    }

    /**
     * Add a condition that must not pass for the program to match.
     */
    public void addUnless(ConditionConfig condition)
    {
      NotConditions not = new NotConditions();
      not.add(condition.getCondition());
      Config.init(not);

      _conditionList.add(not);
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
      _logPrefix = getTagName() + " " + _regexp.pattern();

      if (isSecureSet())
        _logPrefix = _logPrefix + " (secure)";

      if (isPortSet())
        _logPrefix = _logPrefix + " (port " + getPort() + ")";

      required(_regexp, "regexp");

      if (_conditionList.size() > 0) {
	_conditions = new Condition[_conditionList.size()];
	_conditionList.toArray(_conditions);
      }

      _conditionList = null;

      if (_name == null) {
        if (!_isEnabled)
          throw new ConfigException(L.l("{0} requires 'name' if enabled='false'",
                                        getTagName()));
      }
      else {
        _admin = new RewriteRuleAdmin(this);
        _admin.register();
      }
    }

    public Condition []getConditions()
    {
      return _conditions;
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

    @PreDestroy
    public void destroy()
    {
      RewriteRuleAdmin admin = _admin;
      _admin = null;

      Condition[] conditions = _conditions;
      _conditions = null;

      if (admin != null)
        admin.unregister();

      if (conditions != null) {

        for (Condition condition : conditions) {
          // XXX: s/b Config.destroy()
          try {
            condition.destroy();
          }
          catch (Exception ex) {
            log.log(Level.FINER, ex.toString(), ex);
          }
        }
      }
    }
  }

  public class RewriteRuleAdmin
    extends AbstractManagedObject
    implements RewriteRuleMXBean
  {
    private final AbstractRule _rewriteRule;

    public RewriteRuleAdmin(AbstractRule rewriteRule)
    {
      _rewriteRule = rewriteRule;
    }

    public String getName()
    {
      return _rewriteRule.getName();
    }

    @Override
    public String getType()
    {
      return "RewriteRule";
    }

    public String getState()
    {
      if (_rewriteRule.isEnabled())
        return "active";
      else
        return "stopped";
    }

    public String getRegexp()
    {
      return _rewriteRule.getRegexp().pattern();
    }

    public void start()
    {
      _rewriteRule.setEnabled(true);
    }

    public void stop()
    {
      _rewriteRule.setEnabled(false);
    }

    public void register()
    {
      registerSelf();
    }

    public void unregister()
    {
      unregisterSelf();
    }
  }

  public class AcceptRule
    extends AbstractRule
  {
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

  public class ErrorRule
    extends AbstractRule
  {
    private final String _tagName;
    private final int _code;

    ErrorRule(int code)
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

  public class ForwardRule
    extends AbstractRule
  {
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

    @Override
    public void init()
      throws ConfigException
    {
      super.init();

      required(_target, "target");
    }
  }

  public class LoadBalanceRule
    extends AbstractRule
  {
    private final WebApp _webApp;

    private ServletConfigImpl _servlet;

    private BuilderProgramContainer _program = new BuilderProgramContainer();

    LoadBalanceRule(WebApp webApp)
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

    @Override
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

  public class MovedRule
    extends AbstractRule
  {
    private String _tagName;
    private int _code;
    private String _target;

    public MovedRule(int statusCode)
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

    @Override
    public void init()
      throws ConfigException
    {
      super.init();

      required(_target, "target");
    }
  }

  public class RedirectRule
    extends AbstractRule
  {
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

    @Override
    public void init()
      throws ConfigException
    {
      super.init();

      required(_target, "target");
    }
  }

  public class RewriteRule
    extends AbstractRule
  {
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
    @Override
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
  
