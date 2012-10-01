/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.json.ser;

import com.caucho.json.JsonOutput;

import java.io.*;

public class DoubleSerializer extends AbstractJsonSerializer<Number> {
  static final JsonSerializer SER = new DoubleSerializer();

  private DoubleSerializer() {}
  
  @Override
  public void write(JsonOutput out, Number value, boolean annotated)
    throws IOException
  {
    double dValue = value.doubleValue();
    
    if (Double.isNaN(dValue)) {
      // hessian/5070
      out.writeDouble(0.0);
    }
    else if (Double.isInfinite(dValue)) {
      if (dValue > 0)
        out.writeDouble(Double.MAX_VALUE);
      else
        out.writeDouble(-Double.MAX_VALUE);
    }
    else {
      out.writeDouble(value.doubleValue());
    }
  }
}
