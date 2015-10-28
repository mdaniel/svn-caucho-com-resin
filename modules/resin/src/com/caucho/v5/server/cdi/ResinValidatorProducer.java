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

package com.caucho.v5.server.cdi;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import com.caucho.v5.config.CauchoBean;
import com.caucho.v5.config.CauchoDeployment;

/**
 * Resin CDI producer for the main singletons.
 */

@CauchoDeployment
@Singleton
@CauchoBean
public class ResinValidatorProducer
{
  private Validator _validator;
  
  public ResinValidatorProducer()
  {
  }
  
  /**
   * Returns the validator factory.
   */
  @Produces
  @CauchoBean
  public ValidatorFactory createValidatorFactory()
  {
    return Validation.buildDefaultValidatorFactory();
  }
  
  /**
   * Returns the validator factory.
   */
  @Produces
  @CauchoBean
  public Validator createValidator()
  {
    if (_validator == null)
      _validator = Validation.buildDefaultValidatorFactory().getValidator();
    
    return _validator;
  }
}
