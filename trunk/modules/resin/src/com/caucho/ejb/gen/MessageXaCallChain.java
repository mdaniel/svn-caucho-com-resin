/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.ejb.gen;

import static javax.ejb.TransactionAttributeType.REQUIRED;

import java.io.IOException;

import com.caucho.config.gen.BusinessMethodGenerator;
import com.caucho.config.gen.EjbCallChain;
import com.caucho.config.gen.XaCallChain;
import com.caucho.java.JavaWriter;

/**
 * Represents the xa interception
 */
public class MessageXaCallChain extends XaCallChain
{
  public MessageXaCallChain(BusinessMethodGenerator bizMethod,
			    EjbCallChain next)
  {
    super(bizMethod, next);
  }
  
  /**
   * Sets the transaction type
   */
  /*
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
  */

  @Override
  public void generatePreCall(JavaWriter out)
    throws IOException
  {
    super.generatePreCall(out);
    
    if (REQUIRED.equals(getTransactionType())) {
      out.println();
      out.println("if (_xaResource != null)");
      out.println("  _xa.enlist(_xaResource);");
    }

    out.println("/* ... */");
  }
}
