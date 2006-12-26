/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.iiop.any;

import com.caucho.util.L10N;
import com.caucho.iiop.IiopReader;
import com.caucho.iiop.IiopWriter;

import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;

public class BooleanTypeCode extends AbstractTypeCode
{
  public static final AbstractTypeCode TYPE_CODE = new BooleanTypeCode();

  /**
   * Returns the kind of the type code.
   */
  @Override
  public TCKind kind()
  {
    return TCKind.tk_boolean;
  }

  /**
   * Reads the value from the reader.
   */
  @Override
  public Object readValue(IiopReader is)
  {
    return is.read_boolean();
  }
  
  /**
   * Writes the value to the writer
   */
  @Override
  public void writeValue(IiopWriter os, Object value)
  {
    os.write_boolean((Boolean) value);
  }

  public String toString()
  {
    return "BooleanTypeCode[]";
  }
}
