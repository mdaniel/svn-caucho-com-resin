/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.v5.http.rewrite;

import java.util.regex.Matcher;

import javax.annotation.PostConstruct;
import javax.servlet.FilterChain;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.http.dispatch.FilterChainRewriteDispatch;

public class DispatchRuleBase2
  extends AbstractRuleWithConditions
{
  private String _target;
  private String _absoluteTarget;
  private String _targetHost;

  private boolean _isAbsolute;

  protected DispatchRuleBase2(RewriteDispatch rewriteDispatch)
  {
    super(rewriteDispatch);
  }

  public String getTagName()
  {
    return "dispatch";
  }

  public void setAbsoluteTarget(String target)
  {
    _target = target;

    _isAbsolute = true;
  }

  public void setTarget(String target)
  {
    _target = target;
  }

  public void setTargetHost(String target)
  {
    _targetHost = target;
  }

  public String getTarget()
  {
    return _target;
  }

  @Override
  public String rewrite(String uri, Matcher matcher)
  {
    if (_target != null) {
      String rewrite = matcher.replaceAll(_target);

      // server/1ks6
      if (uri.equals("/") && rewrite.endsWith("/") && ! _target.endsWith("/"))
        rewrite = rewrite.substring(0, rewrite.length() - 1);
      
      return rewrite;
    }
    else
      return uri;
  }

  @Override
  public FilterChain dispatch(String uri,
                              String queryString,
                              FilterChain accept,
                              FilterChainMapper next)
  {
    String uriArg = null;
    
    /*
    if (queryString == null)
      uriArg = uri;
    else if (uri.indexOf('?') >= 0)
      uriArg = uri + "&" + queryString;
    else
      uriArg = uri + "?" + queryString;
      */
    
    uriArg = uri;

    /*
    if (_isAbsolute)
      return new DispatchAbsoluteFilterChain(uriArg, WebApp.getCurrent());
    else
    */
    if (getTarget() != null)
      return new FilterChainRewriteDispatch(uriArg);
    else
      return accept;
  }

  @Override
  @PostConstruct
  public void init()
    throws ConfigException
  {
    super.init();

    // required(_target, "target");
  }
}
