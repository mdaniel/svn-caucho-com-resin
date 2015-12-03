/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.server.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletResponse;

import com.caucho.server.dispatch.ErrorFilterChain;
import com.caucho.server.dispatch.FilterChainBuilder;
import com.caucho.server.dispatch.Invocation;
import com.caucho.server.webapp.WebApp;

/**
 * Manages security constraint.
 */
public class ConstraintManager extends FilterChainBuilder {
  private ArrayList<SecurityConstraint> _constraints
    = new ArrayList<SecurityConstraint>();

  public void addConstraint(SecurityConstraint constraint)
  {
    _constraints.add(constraint);
  }

  public FilterChainBuilder getFilterBuilder()
  {
    return this;
    /*
    if (_constraints.size() > 0)
      return this;
    else
      return null;
    */
  }
  
  /**
   * Builds a filter chain dynamically based on the invocation.
   *
   * @param next the next filter in the chain.
   * @param invocation the request's invocation.
   */
  @Override
  public FilterChain build(FilterChain next, Invocation invocation)
  {
    String uri = invocation.getContextURI();

    WebApp webApp = invocation.getWebApp();
    if (webApp == null)
      return next;

    String lower = uri.toLowerCase(Locale.ENGLISH);

    if (lower.startsWith("/web-inf")
        || lower.startsWith("/meta-inf")) {
      return new ErrorFilterChain(HttpServletResponse.SC_NOT_FOUND);
    }

    ArrayList<AbstractConstraint> constraints;
    constraints = new ArrayList<AbstractConstraint>();
    
    HashMap<String,AbstractConstraint[]> methodMap;
    methodMap = new HashMap<String,AbstractConstraint[]>();

    loop:
    for (int i = 0; i < _constraints.size(); i++) {
      SecurityConstraint constraint = _constraints.get(i);

      if (constraint.isMatch(uri)) {
        AbstractConstraint absConstraint = constraint.getConstraint();
        if (absConstraint != null) {
          ArrayList<String> methods = constraint.getMethods(uri);

          for (int j = 0; methods != null && j < methods.size(); j++) {
            String method = methods.get(j);

            AbstractConstraint []methodList = methodMap.get(method);

            if (methodList == null) {
              // server/1ak4
              // server/12ba - the first constraint matches, following are
              // ignored
              methodList = absConstraint.toArray();
              methodMap.put(method, methodList);
            }
          }
          
          if (methods == null || methods.size() == 0) {
            AbstractConstraint []constArray = absConstraint.toArray();
            for (int k = 0; k < constArray.length; k++)
              constraints.add(constArray[k]);

            // server/12ba - the first constraint matches, following are
            // ignored

            if (! constraint.isFallthrough())
              break loop;
          }
        }
        else {
          // server/1233

          if (! constraint.isFallthrough())
            break loop;
        }
      }
    }

    if (constraints.size() > 0 || methodMap.size() > 0) {
      SecurityFilterChain filterChain = new SecurityFilterChain(next);
      filterChain.setWebApp(invocation.getWebApp());
      if (methodMap.size() > 0)
        filterChain.setMethodMap(methodMap);
      filterChain.setConstraints(constraints);

      return filterChain;
    }

    return next;
  }

  public boolean hasConstraintForUrlPattern(String pattern)
  {
    for (SecurityConstraint constraint : _constraints) {
      if (constraint.isMatch(pattern))
        return true;
    }

    return false;
  }
}
