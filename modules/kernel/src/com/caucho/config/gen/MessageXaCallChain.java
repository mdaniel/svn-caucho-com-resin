/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.config.gen;

import com.caucho.config.*;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import javax.annotation.security.*;
import javax.ejb.*;
import static javax.ejb.TransactionAttributeType.*;
import javax.interceptor.*;

/**
 * Represents the xa interception
 */
public class MessageXaCallChain extends XaCallChain
{
  private static final L10N L = new L10N(MessageXaCallChain.class);
  
  public MessageXaCallChain(BusinessMethodGenerator bizMethod,
			    EjbCallChain next)
  {
    super(bizMethod, next);
  }
  
  /**
   * Sets the transaction type
   */
  public void setTransactionType(TransactionAttributeType xa)
  {
    if (xa == null
	|| REQUIRED.equals(xa)
	|| NOT_SUPPORTED.equals(xa)) {
      super.setTransactionType(xa);
    }
    else
      throw ConfigException.create(getBusinessMethod().getApiMethod().getMethod(),
				L.l("'{0}' is not an allowed transaction type for message beans",
				    xa));
  }

  @Override
  protected void generateNext(JavaWriter out)
    throws IOException
  {
    if (REQUIRED.equals(getTransactionType())) {
      out.println();
      out.println("if (_xaResource != null)");
      out.println("  _xa.enlist(_xaResource);");
    }

    out.println("/* ... */");
      
    super.generateNext(out);
  }
}
