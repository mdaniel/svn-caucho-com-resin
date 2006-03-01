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
 * @author Charles Reich
 */

package com.caucho.quercus.lib;

import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.StringValueImpl;
import com.caucho.quercus.env.Value;

import org.w3c.dom.Text;

public class DOMTextValue extends DOMCharacterDataValue {
  
  public DOMTextValue(Text text)
  {
    super(text);
  }
  
  @Override
  public Value getField(String name)
  {
    if (_node == null)
      return NullValue.NULL;
    
    if ("wholeText".equals(name))
      return new StringValueImpl(((Text) _node).getWholeText());
    
    return NullValue.NULL;
  }

  public Value isWhitespaceInElementContent()
  {
    if (_node == null)
      return BooleanValue.FALSE;
    
    if (((Text) _node).isElementContentWhitespace())
      return BooleanValue.TRUE;
    else
      return BooleanValue.FALSE;
  }
  
  public Value splitText(Value offset)
  {
    if (_node == null)
      return NullValue.NULL;
    
    return new DOMTextValue(((Text) _node).splitText(offset.toInt()));
  }
  
  @Override
  public Value evalMethod(Env env, String methodName)
    throws Throwable
  {
    if ("isWhitespaceInElementContent".equals(methodName))
      return isWhitespaceInElementContent();
    
    return super.evalMethod(env, methodName);
  }
  
  @Override
  public Value evalMethod(Env env, String methodName, Value a0)
    throws Throwable
  {
    if ("splitText".equals(methodName))
      return splitText(a0);
    
    return super.evalMethod(env, methodName, a0);
  }
}
