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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.security;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

public class ContainerConstraint extends ConstraintBase {
  private boolean _needsAuthentication;
  
  private ArrayList<ConstraintBase> _constraints =
    new ArrayList<ConstraintBase>();

  public void init() {
    ArrayList<ConstraintBase> constraints
      = new ArrayList<ConstraintBase>(_constraints.size());

    for (ConstraintBase constraint : _constraints) {
      if (constraint instanceof TransportConstraint)
        constraints.add(constraint);
    }

    for (ConstraintBase constraint : _constraints) {
      if (! (constraint instanceof TransportConstraint))
        constraints.add(constraint);
    }

    _constraints = constraints;
  }

  /**
   * Adds a constraint.
   */
  public void addConstraint(ConstraintBase constraint)
  {
    for (ConstraintBase subConstraint : constraint.toArray()) {
      _constraints.add(subConstraint);

      if (subConstraint.needsAuthentication())
        _needsAuthentication = true;
    }
  }
  
  /**
   * Returns true if the constraint requires authentication.
   */
  public boolean needsAuthentication()
  {
    return _needsAuthentication;
  }
  
  /**
   * Returns true if the user is authorized for the resource.
   *
   * <p>isAuthorized must provide the response if the user is not
   * authorized.  Typically this will just call sendError.
   *
   * <p>isAuthorized will be called after all the other filters, but
   * before the servlet.service().
   *
   * @param request the servlet request
   * @param response the servlet response
   *
   * @return true if the request is authorized.
   */
  public AuthorizationResult isAuthorized(HttpServletRequest request,
                                          HttpServletResponse response,
                                          ServletContext application)
    throws ServletException, IOException
  {
    AuthorizationResult result = AuthorizationResult.NONE;
    
    for (int i = 0; i < _constraints.size(); i++) {
      ConstraintBase constraint = _constraints.get(i);

      result = constraint.isAuthorized(request, response, application);

      if (! result.isFallthrough())
        return result;
    }

    return result;
  }

  /**
   * converts the sub constraints to an array.
   */
  protected ConstraintBase []toArray()
  {
    return _constraints.toArray(new ConstraintBase[_constraints.size()]);
  }
}
