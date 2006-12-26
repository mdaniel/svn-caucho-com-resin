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

import org.omg.CORBA.Any;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.TypeCodePackage.BadKind;
import org.omg.CORBA.TypeCodePackage.Bounds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

abstract public class AbstractTypeCode extends TypeCode
{
  protected static final L10N L = new L10N(AbstractTypeCode.class);
  protected static final Logger log
    = Logger.getLogger(AbstractTypeCode.class.getName());
  /**
   * Returns true if the type codes are equal.
   */
  public boolean equal(TypeCode tc)
  {
    return tc == this;
  }

  /**
   * Returns true if the type codes are equivalent.
   */
  public boolean equivalent(TypeCode tc)
  {
    return tc == this;
  }

  /**
   * Returns the compact typecode
   */
  public TypeCode get_compact_typecode()
  {
    return this;
  }

  /**
   * Returns the kind of the type code.
   */
  abstract public TCKind kind();

  /**
   * Returns the rep-id of the typecode.
   */
  public String id()
    throws BadKind
  {
    throw new BadKind();
  }

  /**
   * Returns the simple name withing the scope.
   */
  public String name()
    throws BadKind
  {
    throw new BadKind();
  }
  
  /**
   * Returns the type modifier.
   */
  public short type_modifier()
    throws BadKind
  {
    throw new BadKind();
  }
  
  /**
   * Returns the type code of the concrete base
   */
  public TypeCode concrete_base_type()
    throws BadKind
  {
    throw new BadKind();
  }
  
  /**
   * Returns the number of fields in the type code
   */
  public int member_count()
    throws BadKind
  {
    throw new BadKind();
  }

  /**
   * Returns the field name for the given index.
   */
  public String member_name(int index)
    throws BadKind, Bounds
  {
    throw new BadKind();
  }

  /**
   * Returns the typecode for the member type.
   */
  public TypeCode member_type(int index)
    throws BadKind, Bounds
  {
    throw new BadKind();
  }

  /**
   * Returns the visibility status of the given member.
   */
  public short member_visibility(int index)
    throws BadKind, Bounds
  {
    throw new BadKind();
  }

  /**
   * Returns the label for the given index.
   */
  public Any member_label(int index)
    throws BadKind, Bounds
  {
    throw new BadKind();
  }

  /**
   * Returns the discriminator type for the given index.
   */
  public TypeCode discriminator_type()
    throws BadKind
  {
    throw new BadKind();
  }

  /**
   * Returns the default member.
   */
  public int default_index()
    throws BadKind
  {
    throw new BadKind();
  }
  
  /**
   * Returns the number of elements.
   */
  public int length()
    throws BadKind
  {
    throw new BadKind();
  }

  /**
   * Returns the typecode for the content type.
   */
  public TypeCode content_type()
    throws BadKind
  {
    throw new BadKind();
  }

  /**
   * Returns the number of digits in the fixed type.
   */
  public short fixed_digits()
    throws BadKind
  {
    throw new BadKind();
  }

  /**
   * Returns the scale of the fixed type.
   */
  public short fixed_scale()
    throws BadKind
  {
    throw new BadKind();
  }

  /**
   * Reads the value from the reader.
   */
  abstract public Object readValue(IiopReader is);

  /**
   * Writes the value to the writer
   */
  public void writeValue(IiopWriter os, Object value)
  {
    System.out.println("V: " + value);
    
    throw new UnsupportedOperationException(getClass().getName());
  }
}
