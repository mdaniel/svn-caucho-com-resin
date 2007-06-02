/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.simplexml.node;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.util.IdentityHashMap;

/**
 * Represents a SimpleXML node.
 */
public class SimpleAttribute extends SimpleNode
{
  public SimpleAttribute(String name, String value, String namespace)
  {
    setName(name);
    setValue(value);
    setNamespace(namespace);
  }
  
  @Override
  public boolean isAttribute()
  {
    return true;
  }
  
  @Override
  protected void toXMLImpl(StringBuilder sb)
  {
    sb.append(' ');
    
    if (getPrefix() != null) {
      sb.append(getPrefix());
      sb.append(':');
    }
    
    sb.append(getName());
    sb.append("=\"" + getValue() + "\"");
  }
  
  @Override
  public void varDumpImpl(Env env,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    printDepth(out, 2 * depth);
    out.println("[0]=>");
    
    printDepth(out, 2 * depth);
    
    out.print("string(" + getValue().length() + ") \"");
    out.print(getValue());
    out.println('"');
  }
  
  @Override
  void varDumpNested(Env env,
                     WriteStream out,
                     int depth,
                     IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    printDepth(out, 2 * depth);
    out.print("[\"");
    
    out.print(getName());
    out.println("\"]=>");
    
    printDepth(out, 2 * depth);
    
    out.print("string(" + getValue().length() + ") \"");
    out.print(getValue());
    out.println('"');
  }
  
  
  @Override
  public void printRImpl(Env env,
                         WriteStream out,
                         int depth,
                         IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    printDepth(out, 4 * depth);
    out.print("[0] => ");
    out.println(getValue());
  }
  
  @Override
  void printRNested(Env env,
                    WriteStream out,
                    int depth,
                    IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    printDepth(out, 4 * depth);
    out.print("[" + getName() + "] => ");
    out.println(getValue());
  }
}
