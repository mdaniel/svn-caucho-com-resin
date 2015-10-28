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

package com.caucho.rewrite;

import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;

import com.caucho.v5.config.Configurable;
import com.caucho.v5.http.dispatch.FilterChainRewriteDispatch;
import com.caucho.v5.http.rewrite.DispatchRuleTargetBase;

/**
 * Dispatch the request using the standard servlet and filter processing.
 * The Dispatch rule can be used to select normal processing before
 * additional rewrites.
 *
 * <pre>
 * &lt;web-app xmlns:resin="urn:java:com.caucho.resin">
 *
 *   &lt;resin:Dispatch regexp="\.(php|jpg|gif|js|css)"/>
 *
 *   &lt;resin:Dispatch regexp="^" target="/index.php"/>
 *
 * &lt;/web-app>
 * </pre>
 */
@Configurable
public class Dispatch extends DispatchRuleTargetBase
{
  @Override
  public FilterChain createDispatch(DispatcherType type,
                                    String uri,
                                    String queryString,
                                    String target,
                                    FilterChain next)
  {
    if (getTarget() != null)
      return new FilterChainRewriteDispatch(target);
    else
      return next;
  }
}
