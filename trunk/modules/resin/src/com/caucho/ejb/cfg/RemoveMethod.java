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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Rodrigo Westrupp
 */

package com.caucho.ejb.cfg;

import com.caucho.config.gen.BeanGenerator;
import com.caucho.config.gen.BusinessMethodGenerator;
import com.caucho.config.gen.View;
import com.caucho.config.gen.ApiMethod;
import com.caucho.util.L10N;
import java.lang.reflect.*;

/**
 * Configuration for remove-method.
 */
public class RemoveMethod {
  private BeanMethod _beanMethod;
  private boolean _retainIfException;

  public RemoveMethod()
  {
  }

  public BeanMethod getBeanMethod()
  {
    return _beanMethod;
  }

  public boolean isRetainIfException()
  {
    return _retainIfException;
  }

  public void setBeanMethod(BeanMethod beanMethod)
  {
    _beanMethod = beanMethod;
  }

  public void setRetainIfException(boolean retainIfException)
  {
    _retainIfException = retainIfException;
  }

  public boolean isMatch(Method method)
  {
    return _beanMethod.isMatch(method);
  }

  /**
   * Configures the bean with the override values
   */
  public void configure(BeanGenerator bean)
  {
    for (View view : bean.getViews()) {
      // XXX: check for type
      
      for (BusinessMethodGenerator bizMethod : view.getMethods()) {
	ApiMethod apiMethod = bizMethod.getApiMethod();
	
	if (_beanMethod.isMatch(apiMethod)) {
	  bizMethod.setRemove(true);
	  bizMethod.setRemoveRetainIfException(_retainIfException);
	}
      }
    }
  }
}
