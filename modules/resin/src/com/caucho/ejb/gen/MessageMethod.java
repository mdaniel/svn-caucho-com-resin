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

import com.caucho.config.gen.ApiMethod;
import com.caucho.config.gen.BusinessMethodGenerator;
import com.caucho.config.gen.EjbCallChain;
import com.caucho.config.gen.XaCallChain;

/**
 * Represents a message local business method
 */
public class MessageMethod extends BusinessMethodGenerator
{
  public MessageMethod(MessageView view,
		       ApiMethod apiMethod,
		       ApiMethod implMethod,
		       int index)
  {
    super(view, apiMethod, implMethod, index);
  }

  @Override
  protected XaCallChain createXa(EjbCallChain next)
  {
    return new MessageXaCallChain(this, next);
  }

  /**
   * Session bean default is REQUIRED
   */
  /*
  @Override
  public void introspect(ApiMethod apiMethod, ApiMethod implMethod)
  {
    getXa().setTransactionType(TransactionAttributeType.REQUIRED);

    super.introspect(apiMethod, implMethod);
  }
  */

  /**
   * Returns true if any interceptors enhance the business method
   */
  @Override
  public boolean isEnhanced()
  {
    return true;
  }
}
