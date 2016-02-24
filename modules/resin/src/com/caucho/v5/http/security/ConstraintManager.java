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

package com.caucho.v5.http.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.HttpConstraintElement;
import javax.servlet.HttpMethodConstraintElement;
import javax.servlet.ServletSecurityElement;
import javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic;
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;
import javax.servlet.http.HttpServletResponse;

import com.caucho.v5.http.dispatch.FilterChainBuilder;
import com.caucho.v5.http.dispatch.FilterChainError;
import com.caucho.v5.http.dispatch.InvocationServlet;
import com.caucho.v5.http.webapp.WebAppResinBase;

/**
 * Manages security constraint.
 */
public class ConstraintManager extends FilterChainBuilder {
  private static String []httpMethods
    = new String[]{"GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS", "TRACE"};

  private ArrayList<SecurityConstraint> _constraints
    = new ArrayList<>();
    
  private Map<String,List<SecurityConstraint>> _servletConstraintMap
    = new HashMap<>();

  private boolean _isDenyUncoveredMethods;

  public void setDenyUncoveredHttpMethods(boolean isDeny)
  {
    _isDenyUncoveredMethods = isDeny;
  }

  public void addConstraint(SecurityConstraint constraint)
  {
    _constraints.add(constraint);
  }

  public void addSecurityElement(String servletName,
                                 ServletSecurityElement securityElement)
  {
    SecurityConstraint constraint = buildConstraint(securityElement);
    
    addConstraint(servletName, constraint);
  }

  public void addConstraint(String servletName, SecurityConstraint constraint)
  {
    List<SecurityConstraint> constraints = _servletConstraintMap.get(servletName);
    
    if (constraints == null) {
      constraints = new ArrayList<>();
      _servletConstraintMap.put(servletName, constraints);
    }
    
    constraints.add(constraint);
  }
  
  public static SecurityConstraint buildConstraint(ServletSecurityElement secElt)
  {
    HttpConstraintElement elt = secElt;
    
    SecurityConstraint constraint = buildConstraint(elt);

    for (HttpMethodConstraintElement eltMethod : secElt.getHttpMethodConstraints()) {
     SecurityConstraint constraintMethod = ConstraintManager.buildConstraint(eltMethod);

     constraint.addMethod(eltMethod.getMethodName(), constraintMethod);
    }
    
    return constraint;
  }

  public static SecurityConstraint buildConstraint(HttpConstraintElement elt)
  {
    EmptyRoleSemantic emptyRoleSemantic = elt.getEmptyRoleSemantic();

    TransportGuarantee transportGuarantee = elt.getTransportGuarantee();

    String[] roles = elt.getRolesAllowed();

    SecurityConstraint constraint = new SecurityConstraint();
    constraint.setFallthrough(false);

    if (emptyRoleSemantic == EmptyRoleSemantic.DENY) {
      constraint.addConstraint(new DenyConstraint());
    } 
    else if (roles.length == 0
        && transportGuarantee == TransportGuarantee.NONE) {
      constraint.addConstraint(new PermitConstraint());
    } 
    else {
      for (String role : roles) {
        constraint.addRoleName(role);
      }

      if (transportGuarantee == TransportGuarantee.CONFIDENTIAL) {
        constraint.addConstraint(new TransportConstraint("CONFIDENTIAL"));
      }
    }
  
    return constraint;
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
  public FilterChain build(FilterChain next, InvocationServlet invocation)
  {
    String uri = invocation.getContextURI();

    WebAppResinBase webApp = invocation.getWebApp();
    if (webApp == null) {
      return next;
    }

    String lower = uri.toLowerCase(Locale.ENGLISH);

    if (lower.startsWith("/web-inf")
        || lower.startsWith("/meta-inf")) {
      return new FilterChainError(HttpServletResponse.SC_NOT_FOUND);
    }

    ArrayList<ConstraintBase> constraints = new ArrayList<>();
    
    HashMap<String,ConstraintBase[]> methodMap = new HashMap<>();

    boolean isMatch = false;
    
    for (int i = 0; i < _constraints.size(); i++) {
      SecurityConstraint constraint = _constraints.get(i);

      if (constraint.isMatch(uri)) {
        isMatch = true;
        if (! buildConstraints(constraints, methodMap, constraint, uri)) {
          break;
        }
      }
    }
    
    if (! isMatch) {
      List<SecurityConstraint> constraintList
        = _servletConstraintMap.get(invocation.getServletName());
    
      // servlet/1911 - web.xml overrides annotation
      if (constraintList != null && constraints.size() == 0) {
        for (SecurityConstraint constraint : constraintList) {
          buildConstraints(constraints, methodMap, constraint, uri);
        }
      }
    }

    if (methodMap.size() > 0 && _isDenyUncoveredMethods)
      fillDenyUncoveredMethods(methodMap);

    if (constraints.size() > 0 || methodMap.size() > 0) {
      FilterChainSecurity filterChain = new FilterChainSecurity(next);
      filterChain.setWebApp(invocation.getWebApp());
      if (methodMap.size() > 0)
        filterChain.setMethodMap(methodMap);
      filterChain.setConstraints(constraints);

      return filterChain;
    }

    return next;
  }

  private boolean buildConstraints(List<ConstraintBase> constraints,
                                 HashMap<String,ConstraintBase[]> methodMap,
                                 SecurityConstraint constraint,
                                 String uri)
  {
    ConstraintBase absConstraint = constraint.getConstraint();

    if (absConstraint == null)
      absConstraint = new PermitConstraint();

    ArrayList<String> methods = constraint.getMethods(uri);

    if (absConstraint != null && methods != null) {
      for (String methodName : methods) {
        ConstraintBase []methodList = methodMap.get(methodName);

        if (methodList == null) {
          // server/1ak4
          // server/12ba - the first constraint matches, following are
          // ignored
          methodList = absConstraint.toArray();
          methodMap.put(methodName, methodList);
        }
      }
    }
    
    for (Map.Entry<String,SecurityConstraint> entry
        : constraint.getMethodMap().entrySet()) {
      String methodName = entry.getKey();
      SecurityConstraint constraintMethod = entry.getValue();

      ConstraintBase []methodList = methodMap.get(methodName);

      if (methodList == null) {
        // server/1ak4
        // server/12ba - the first constraint matches, following are
        // ignored
        methodList = constraintMethod.getConstraint().toArray();
        methodMap.put(methodName, methodList);
      }
    }

    if (absConstraint != null) {
      if (methods == null || methods.size() == 0) {
        for (ConstraintBase constMethod : absConstraint.toArray()) {
          constraints.add(constMethod);
        }

        // server/12ba - the first constraint matches, following are
        // ignored

        if (! constraint.isFallthrough()) {
          return false;
        }
      }
    }
    else {
      // server/1233

      if (! constraint.isFallthrough()) {
        return false;
      }
    }

    return true;
  }

  public void fillDenyUncoveredMethods(HashMap<String,ConstraintBase[]> methods)
  {
    for (String httpMethod : httpMethods) {
      if (methods.containsKey(httpMethod))
        continue;

      methods.put(httpMethod, new ConstraintBase[]{new DenyConstraint()});
    }
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
