/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
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

package com.caucho.v5.config.cf;

import java.util.Objects;

import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.attribute.AttributeConfig;
import com.caucho.v5.config.expr.ExprCfg;
import com.caucho.v5.config.type.ConfigType;
import com.caucho.v5.config.type.TypeFactoryConfig;
import com.caucho.v5.inject.impl.InjectContext;
import com.caucho.v5.util.L10N;

/**
 * Program to assign parameters.
 */
public class ProgramPropertyExpr extends ProgramIdBase
{
  private static final L10N L = new L10N(ProgramPropertyExpr.class);
  private final NameCfg _id;
  private final ExprCfg _expr;
  private final String _text;
  
  public ProgramPropertyExpr(ConfigContext config, 
                             String id, 
                             ExprCfg expr,
                             String text)
  {
    super(config, id);
    
    Objects.requireNonNull(expr);
    
    _id = new NameCfg(id);
    
    _expr = expr;
    _text = text;
  }
  
  public ProgramPropertyExpr(ConfigContext config, 
                             NameCfg id, 
                             ExprCfg expr,
                             String text)
  {
    super(config, id);
    
    Objects.requireNonNull(expr);
    
    _id = id;
    
    _expr = expr;
    _text = text;
  }
  
  @Override
  public <T> void inject(T bean, InjectContext env)
  {
    inject(bean, TypeFactoryConfig.getType(bean), env);
  }
  
  @Override
  public <T> void inject(T bean, ConfigType<T> type, InjectContext env)
  {
    try {
      AttributeConfig attr = getAttribute(type);

      if (attr == null) {
        throw error("{0} is an unknown attribute of {1}", _id.getLocalName(), bean);
      }

      if (attr.isProgram()) {
        attr.setValue(bean, _id, this);
      }
      else if (attr.isEL()) {
        // Object value = _expr.evalObject(getELContext());
        
        Object value = null;
        
        if (env != null) {
          value = _expr.eval(env.getVarMap());
        }
        else {
          value = _expr.eval(ConfigContext.getEnvironment());
        }
        
        attr.setValue(bean, _id, attr.getConfigType().valueOf(value));
      }
      else {
        attr.setText(bean, _id, _text);
      }
    } catch (RuntimeException e) {
      throw error(e);
    } catch (Exception e) {
      throw error(e);
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "=" + _expr + "]";
  }
}

