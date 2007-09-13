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
 * @author Sam
 */

package com.caucho.quercus.lib.spl;

import com.caucho.quercus.annotation.Delegates;
import com.caucho.quercus.annotation.Name;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.*;
import com.caucho.quercus.lib.ArrayModule;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;

@Delegates({ArrayObject.PrintDelegateImpl.class, ArrayObject.FieldDelegateImpl.class})
public class ArrayObject
  implements ArrayAccess,
             Countable,
             IteratorAggregate,
             Traversable
{
  public static final int STD_PROP_LIST = 0x00000001;
  public static final int ARRAY_AS_PROPS = 0x00000002;

  private final Env _env;
  private final Value _value;
  private int _flags;
  private QuercusClass _iteratorClass;

  @Name("__construct")
  public ArrayObject(Env env,
                     @Optional Value value,
                     @Optional int flags,
                     @Optional("ArrayIterator") String iteratorClass)
  {
    if (value.isNull())
      value = new ArrayValueImpl();

    _env = env;
    _value = value;
    _flags = flags;
    _iteratorClass = _env.findClass(iteratorClass);
  }

  public void append(Value value)
  {
    _value.put(value);
  }

  public boolean asort(ArrayValue array, @Optional long sortFlag)
  {
    if (_value instanceof ArrayValue)
      return ArrayModule.asort(_env, (ArrayValue) _value, sortFlag);
    else
      return false;
  }

  public int count()
  {
    return _value.getCount(_env);
  }

  public void exchangeArray(ArrayValue array)
  {
    if (true) throw new UnsupportedOperationException("unimplemented");
  }

  public ArrayValue getArrayCopy()
  {
    return new ArrayValueImpl().append(_value);
  }

  public int getFlags()
  {
    return _flags;
  }

  public ObjectValue getIterator()
  {
    return _iteratorClass.newInstance(_env);
  }

  public String getIteratorClass()
  {
    return _iteratorClass.getName();
  }

  public boolean ksort(@Optional long sortFlag)
  {
    if (_value instanceof ArrayValue)
      return ArrayModule.ksort(_env, (ArrayValue) _value, sortFlag);
    else
      return false;
  }

  public Value natcasesort()
  {
    if (_value instanceof ArrayValue)
      return ArrayModule.natcasesort((ArrayValue) _value);
    else
      return BooleanValue.FALSE;
  }

  public Value natsort()
  {
    if (_value instanceof ArrayValue)
      return ArrayModule.natsort((ArrayValue) _value);
    else
      return BooleanValue.FALSE;
  }

  public boolean offsetExists(Value offset)
  {
    return _value.get(offset).isset();
  }

  public Value offsetSet(Value offset, Value value)
  {
    return _value.put(offset, value);
  }

  public Value offsetGet(Value offset)
  {
    return _value.get(offset);
  }

  public Value offsetUnset(Value offset)
  {
    return _value.remove(offset);
  }

  public void setFlags(Value flags)
  {
    _flags = flags.toInt();
  }

  public void setIteratorClass(String iteratorClass)
  {
    _iteratorClass = _env.findClass(iteratorClass);
  }

  public boolean uasort(Callback func, @Optional long sortFlag)
  {
    if (_value instanceof ArrayValue)
      return ArrayModule.uasort(_env, (ArrayValue) _value, func, sortFlag);
    else
      return false;
  }

  public boolean uksort(Callback func, @Optional long sortFlag)
  {
    if (_value instanceof ArrayValue)
      return ArrayModule.uksort(_env, (ArrayValue) _value, func, sortFlag);
    else
      return false;
  }

  static public class PrintDelegateImpl
    extends PrintDelegate
  {
    private void printDepth(WriteStream out, int depth)
      throws java.io.IOException
    {
      for (int i = depth; i > 0; i--)
        out.print(' ');
    }

    @Override
    public void varDumpImpl(Env env,
                            ObjectValue obj,
                            WriteStream out,
                            int depth,
                            IdentityHashMap<Value, String> valueSet)
      throws IOException
    {
      ArrayObject arrayObject = (ArrayObject) obj.toJavaObject();
      if ((arrayObject._flags & STD_PROP_LIST) != 0) {
        super.varDumpImpl(env, obj, out, depth, valueSet);
      }
      else {
        Value arrayValue = arrayObject._value;

        out.println("object(ArrayObject) (" + obj.getSize() + ") {");

        depth++;

        java.util.Iterator<Map.Entry<Value,Value>> iterator
          = arrayValue.getIterator(env);

        while (iterator.hasNext()) {
          Map.Entry<Value, Value> entry = iterator.next();

          Value key = entry.getKey();
          Value value = entry.getValue();

          printDepth(out, 2 * depth);

          out.print("[");

          if (key instanceof StringValue)
            out.print("\"" + key + "\"");
          else
            out.print(key);

          out.println("]=>");

          printDepth(out, 2 * depth);

          value.varDump(env, out, depth, valueSet);

          out.println();
        }

        depth--;

        printDepth(out, 2 * depth);

        out.print("}");
      }
    }
  }

  static public class FieldDelegateImpl
    extends FieldDelegate
  {
    @Override
    public Value getField(Env env, Value obj, String name, boolean create)
    {
      ArrayObject arrayObject = (ArrayObject) obj.toJavaObject();

      if ((arrayObject._flags & ARRAY_AS_PROPS) != 0) {
        return arrayObject._value.get(env.createString(name));
      }
      else
        return super.getField(env, obj, name, create);
    }


    public void putField(Env env, Value obj, String name, Value value)
    {
      ArrayObject arrayObject = (ArrayObject) obj.toJavaObject();

      if ((arrayObject._flags & ARRAY_AS_PROPS) != 0)
        arrayObject._value.put(env.wrapJava(name), value);
      else
        super.putField(env, obj, name, value);
    }
  }
}
