/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.resources;

import java.io.IOException;

import com.caucho.vfs.Path;

import com.caucho.quercus.env.ResourceValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.StringValue;

/**
 * Represents a PHP stream context.
 */
public class StreamContextResource extends ResourceValue {
  private ArrayValue _options;
  
  public StreamContextResource()
  {
    this(null);
  }

  public StreamContextResource(ArrayValue options)
  {
    if (options == null)
      options = new ArrayValueImpl();
    
    _options = options;
  }

  /**
   * Returns the options.
   */
  public ArrayValue getOptions()
  {
    return _options;
  }

  /**
   * Sets the options.
   */
  public void setOptions(ArrayValue options)
  {
    _options = options;
  }

  /**
   * Sets an option
   */
  public void setOption(String wrapper, String option, Value value)
  {
    StringValue wrapperV = new StringValue(wrapper);
    StringValue optionV = new StringValue(option);

    _options.getArray(wrapperV).put(optionV, value);
  }
  
  /**
   * Converts to a string.
   * @param env
   */
  public String toString(Env env)
  {
    return "StreamContextResource[]";
  }
}

