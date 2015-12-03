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
 * Represents a name and type ref in the constant pool.
 */
public class NameAndTypeConstant extends ConstantPoolEntry {
  private int _nameIndex;
  private int _descriptorIndex;

  /**
   * Creates a new name-and-type ref constant.
   */
  NameAndTypeConstant(ConstantPool pool, int index,
                      int nameIndex, int descriptorIndex)
  {
    super(pool, index);

    _nameIndex = nameIndex;
    _descriptorIndex = descriptorIndex;
  }

  /**
   * Returns the name
   */
  public String getName()
  {
    return getConstantPool().getUtf8(_nameIndex).getValue();
  }

  /**
   * Returns the type descriptor
   */
  public String getType()
  {
    return getConstantPool().getUtf8(_descriptorIndex).getValue();
  }

  /**
   * Writes the contents of the pool entry.
   */
  void write(ByteCodeWriter out)
    throws IOException
  {
    out.write(ConstantPool.CP_NAME_AND_TYPE);
    out.writeShort(_nameIndex);
    out.writeShort(_descriptorIndex);
  }

  /**
   * Exports to the target pool.
   */
  public int export(ConstantPool target)
  {
    return target.addNameAndType(getName(), getType()).getIndex();
  }

  public String toString()
  {
    return "NameAndTypeConstant[" + getName() + ", " + getType() + "]";
  }
}
