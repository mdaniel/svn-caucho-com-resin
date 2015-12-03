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
 * Represents a field ref in the constant pool.
 */
public class FieldRefConstant extends ConstantPoolEntry {
  private int _classIndex;
  private int _nameAndTypeIndex;

  /**
   * Creates a new field ref constant.
   */
  FieldRefConstant(ConstantPool pool, int index,
                   int classIndex, int nameAndTypeIndex)
  {
    super(pool, index);

    _classIndex = classIndex;
    _nameAndTypeIndex = nameAndTypeIndex;
  }

  /**
   * Returns the class index.
   */
  public int getClassIndex()
  {
    return _classIndex;
  }

  /**
   * Sets the class index.
   */
  public void setClassIndex(int index)
  {
    _classIndex = index;
  }

  /**
   * Returns the class name
   */
  public String getClassName()
  {
    return getConstantPool().getClass(_classIndex).getName();
  }

  /**
   * Returns the field name
   */
  public String getName()
  {
    return getConstantPool().getNameAndType(_nameAndTypeIndex).getName();
  }

  /**
   * Returns the method type
   */
  public String getType()
  {
    return getConstantPool().getNameAndType(_nameAndTypeIndex).getType();
  }

  /**
   * Writes the contents of the pool entry.
   */
  void write(ByteCodeWriter out)
    throws IOException
  {
    out.write(ConstantPool.CP_FIELD_REF);
    out.writeShort(_classIndex);
    out.writeShort(_nameAndTypeIndex);
  }

  /**
   * Sets the method name and type
   */
  public void setNameAndType(String name, String type)
  {
    _nameAndTypeIndex = getConstantPool().addNameAndType(name, type).getIndex();
  }

  /**
   * Exports to the target pool.
   */
  public int export(ConstantPool target)
  {
    return target.addFieldRef(getClassName(), getName(), getType()).getIndex();
  }

  public String toString()
  {
    return "FieldRefConstant[" + getClassName() + "." + getName() + "(" + getType() + ")]";
  }
}
