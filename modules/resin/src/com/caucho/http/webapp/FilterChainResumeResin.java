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

package com.caucho.http.webapp;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.caucho.transaction.TransactionManagerImpl;
import com.caucho.transaction.UserTransactionImpl;
import com.caucho.transaction.UserTransactionProxy;

/**
 * Represents the next filter in a filter chain.  The final filter will
 * be the servlet itself.
 */
public class FilterChainResumeResin extends FilterChainResume
{
  private static final Logger log
    = Logger.getLogger(FilterChainResumeResin.class.getName());
  
  // transaction manager
  private TransactionManagerImpl _tm;
  // transaction proxy
  private UserTransactionProxy _utm;

  /**
   * Creates a new FilterChainFilter.
   *
   * @param next the next filterChain
   * @param filter the user's filter
   */
  public FilterChainResumeResin(FilterChain next, WebApp webApp)
  {
    super(next, webApp);

    try {
      _tm = TransactionManagerImpl.getInstance();
      _utm = UserTransactionProxy.getInstance();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Invokes the next filter in the chain or the final servlet at
   * the end of the chain.
   *
   * @param request the servlet request
   * @param response the servlet response
   * @since Servlet 2.3
   */
  @Override
  public void doFilter(ServletRequest request,
                       ServletResponse response)
    throws ServletException, IOException
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    UserTransactionImpl ut = null;
    ut = _utm.getUserTransaction();

    try {
      super.doFilter(request,  response);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      try {
        if (ut != null)
          ut.abortTransaction();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      thread.setContextClassLoader(oldLoader);
    }
  }
}
