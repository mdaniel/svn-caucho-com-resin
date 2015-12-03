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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.bytecode;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Represents a generic attribute
 */
public class OpaqueAttribute extends Attribute {
  private byte []_value;

  OpaqueAttribute(String name)
  {
    super(name);
  }

  /**
   * Sets the opaque value.
   */
  public void setValue(byte []value)
  {
    _value = value;
  }

  /**
   * Gets the opaque value.
   */
  public byte []getValue()
  {
    return _value;
  }

  /**
   * Writes the field to the output.
   */
  public void write(ByteCodeWriter out)
    throws IOException
  {
    out.writeUTF8Const(getName());
    out.writeInt(_value.length);
    out.write(_value, 0, _value.length);
  }

  /**
   * Clones the attribute
   */
  public Attribute export(JavaClass cl, JavaClass target)
  {
    target.getConstantPool().addUTF8(getName());
      
    OpaqueAttribute attr = new OpaqueAttribute(getName());

    byte []value = new byte[_value.length];
    System.arraycopy(_value, 0, value, 0, _value.length);
    
    attr.setValue(value);

    return attr;
  }

  public String toString()
  {
    return "OpaqueAttribute[" + getName() + "]";
  }
}
