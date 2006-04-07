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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Charles Reich
 */

package com.caucho.quercus.lib.dom;

import org.w3c.dom.CharacterData;

abstract public class DOMCharacterData extends DOMNode {

  abstract CharacterData getCharacterDataNode();


  public String getData()
  {
    return getCharacterDataNode().getData();
  }

  public void setData(String data)
  {
    getCharacterDataNode().setData(data);
  }

  public int getLength()
  {
    return getCharacterDataNode().getLength();
  }

  public void appendData(String arg)
  {
    getCharacterDataNode().appendData(arg);
  }

  public void deleteData(int offset,
                         int count)
  {
    getCharacterDataNode().deleteData(offset, count);
  }

  public void insertData(int offset,
                         String arg)
  {
    getCharacterDataNode().insertData(offset, arg);
  }

  public void replaceData(int offset,
                          int count,
                          String arg)
  {
    getCharacterDataNode().replaceData(offset, count, arg);
  }

  public String substringData(int offset,
                              int count)
  {
    return getCharacterDataNode().substringData(offset, count);
  }
}
