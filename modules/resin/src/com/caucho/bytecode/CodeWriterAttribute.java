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

package com.caucho.bytecode;

import com.caucho.log.Log;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.TempStream;
import com.caucho.vfs.WriteStream;

import java.io.*;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Represents a generic attribute
 */
public class CodeWriterAttribute extends CodeAttribute {
  static private final Logger log
    = Logger.getLogger(CodeAttribute.class.getName());

  private int _stack;
  private ByteArrayOutputStream _bos;

  public CodeWriterAttribute(JavaClass jClass)
  {
    setJavaClass(jClass);
    
    addUTF8("Code");

    _bos = new ByteArrayOutputStream();
  }

  public void cast(String className)
  {
    int index = addClass(className);

    write(CodeVisitor.CHECKCAST);
    write(index >> 8);
    write(index);
  }

  public void getField(String className, String fieldName, String sig)
  {
    int index = addFieldRef(className, fieldName, sig);

    write(CodeVisitor.GETFIELD);
    write(index >> 8);
    write(index);
  }

  public void putField(String className, String fieldName, String sig)
  {
    int index = addFieldRef(className, fieldName, sig);

    write(CodeVisitor.PUTFIELD);
    write(index >> 8);
    write(index);
  }

  public void pushObjectVar(int index)
  {
    _stack++;
      
    if (index <= 3) {
      write(CodeVisitor.ALOAD_0 + index);
    }
    else {
      write(CodeVisitor.ALOAD);
      write(index >> 8);
      write(index);
    }
  }

  public void pushIntVar(int index)
  {
    _stack++;
      
    if (index <= 3) {
      write(CodeVisitor.ILOAD_0 + index);
    }
    else {
      write(CodeVisitor.ILOAD);
      write(index >> 8);
      write(index);
    }
  }

  public void pushLongVar(int index)
  {
    _stack += 2;
      
    if (index <= 3) {
      write(CodeVisitor.LLOAD_0 + index);
    }
    else {
      write(CodeVisitor.LLOAD);
      write(index >> 8);
      write(index);
    }
  }

  public void pushFloatVar(int index)
  {
    _stack += 1;
      
    if (index <= 3) {
      write(CodeVisitor.FLOAD_0 + index);
    }
    else {
      write(CodeVisitor.FLOAD);
      write(index >> 8);
      write(index);
    }
  }

  public void pushDoubleVar(int index)
  {
    _stack += 2;
      
    if (index <= 3) {
      write(CodeVisitor.DLOAD_0 + index);
    }
    else {
      write(CodeVisitor.DLOAD);
      write(index >> 8);
      write(index);
    }
  }

  public void invoke(String className,
		     String methodName,
		     String signature,
		     int argStack,
		     int returnStack)
  {
    _stack += returnStack - argStack;

    int index = addMethodRef(className, methodName, signature);
    
    write(CodeVisitor.INVOKEVIRTUAL);
    write(index >> 8);
    write(index);
  }

  public void invokespecial(String className,
			    String methodName,
			    String signature,
			    int argStack,
			    int returnStack)
  {
    _stack += returnStack - argStack;

    int index = addMethodRef(className, methodName, signature);
    
    write(CodeVisitor.INVOKESPECIAL);
    write(index >> 8);
    write(index);
  }
  
  public void addReturn()
  {
    write(CodeVisitor.RETURN);
  }
  
  public void addIntReturn()
  {
    write(CodeVisitor.IRETURN);
  }

  public void addLongReturn()
  {
    write(CodeVisitor.LRETURN);
  }

  public void addFloatReturn()
  {
    write(CodeVisitor.FRETURN);
  }

  public void addDoubleReturn()
  {
    write(CodeVisitor.DRETURN);
  }

  public void addObjectReturn()
  {
    write(CodeVisitor.ARETURN);
  }

  public int addFieldRef(String className, String fieldName, String sig)
  {
    FieldRefConstant ref
      = getConstantPool().addFieldRef(className, fieldName, sig);

    return ref.getIndex();
  }

  public int addMethodRef(String className, String methodName, String sig)
  {
    MethodRefConstant ref
      = getConstantPool().addMethodRef(className, methodName, sig);

    return ref.getIndex();
  }

  public void addUTF8(String code)
  {
    getConstantPool().addUTF8(code);
  }

  public int addClass(String className)
  {
    ClassConstant value = getConstantPool().addClass(className);

    return value.getIndex();
  }

  public ConstantPool getConstantPool()
  {
    return getJavaClass().getConstantPool();
  }

  private void write(int v)
  {
    _bos.write(v);
  }

  public void close()
  {
    if (_bos != null) {
      setCode(_bos.toByteArray());
      _bos = null;
    }
  }
}
