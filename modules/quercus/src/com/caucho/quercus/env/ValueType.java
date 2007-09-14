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
 * @author Scott Ferguson
 */

package com.caucho.quercus.env;

abstract public class ValueType {
  public boolean isBoolean()
  {
    return false;
  }
  
  public boolean isLong()
  {
    return false;
  }
  
  public boolean isLongConvertable()
  {
    return false;
  }
  
  public boolean isDouble()
  {
    return false;
  }
  
  public boolean isNumber()
  {
    return false;
  }
  
  public boolean isNumberConvertable()
  {
    return false;
  }
  
  public static final ValueType NULL = new ValueType()
    {
      public String toString()
      {
	return "ValueType[NULL]";
      }
    };
  
  public static final ValueType BOOLEAN = new ValueType()
    {
      public boolean isBoolean()
      {
	return true;
      }
  
      public String toString()
      {
	return "ValueType[NULL]";
      }
    };
  
  public static final ValueType LONG = new ValueType()
    {
      public boolean isLong()
      {
	return true;
      }
  
      public boolean isLongConvertable()
      {
	return true;
      }
  
      public boolean isNumber()
      {
	return true;
      }
  
      public boolean isNumberConvertable()
      {
	return true;
      }
      
      public String toString()
      {
	return "ValueType[NULL]";
      }
    };
  
  public static final ValueType LONG_CONVERTABLE = new ValueType()
    {
      public boolean isLongConvertable()
      {
	return true;
      }
  
      public boolean isNumberConvertable()
      {
	return true;
      }
      
      public String toString()
      {
	return "ValueType[LONG_CONVERTABLE]";
      }
    };
  
  public static final ValueType DOUBLE = new ValueType()
    {
      public boolean isDouble()
      {
	return true;
      }
  
      public boolean isNumber()
      {
	return true;
      }
  
      public boolean isNumberConvertable()
      {
	return true;
      }
      
      public String toString()
      {
	return "ValueType[DOUBLE]";
      }
    };
  
  public static final ValueType DOUBLE_CONVERTABLE = new ValueType()
    {
      public boolean isNumberConvertable()
      {
	return true;
      }
      
      public String toString()
      {
	return "ValueType[DOUBLE_CONVERTABLE]";
      }
    };
  
  public static final ValueType STRING = new ValueType()
    {
      
      public String toString()
      {
	return "ValueType[STRING]";
      }
    };
  
  public static final ValueType ARRAY = new ValueType()
    {
      
      public String toString()
      {
	return "ValueType[ARRAY]";
      }
    };
  
  public static final ValueType OBJECT = new ValueType()
    {
      
      public String toString()
      {
	return "ValueType[OBJECT]";
      }
    };
  
  public static final ValueType VALUE = new ValueType()
    {
      
      public String toString()
      {
	return "ValueType[VALUE]";
      }
    };
}
