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

package com.caucho.bytecode.compat;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.bytecode.Analyzer;
import com.caucho.bytecode.Attribute;
import com.caucho.bytecode.ByteCodeParser;
import com.caucho.bytecode.ClassConstant;
import com.caucho.bytecode.CodeAttribute;
import com.caucho.bytecode.CodeEnhancer;
import com.caucho.bytecode.CodeVisitor;
import com.caucho.bytecode.ConstantPool;
import com.caucho.bytecode.ConstantPoolEntry;
import com.caucho.bytecode.FieldRefConstant;
import com.caucho.bytecode.JavaClass;
import com.caucho.bytecode.JavaMethod;
import com.caucho.bytecode.JavaField;
import com.caucho.bytecode.MethodRefConstant;
import com.caucho.bytecode.OpaqueAttribute;
import com.caucho.bytecode.StringConstant;
import com.caucho.bytecode.Utf8Constant;

import com.caucho.util.ByteBuffer;
import com.caucho.util.Log;

import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;
import com.caucho.vfs.Vfs;

/**
 * Converts jdk15 .class files to jdk14.
 */
public class Jdk15Compat {
  private static final Logger log = Log.open(Jdk15Compat.class);

  private static final HashMap<String,String> _boxedClasses =
    new HashMap<String,String>();
  
  public static final int MAJOR_1_5 = 49;
  public static final int MAJOR_1_4 = 48;
  
  private JavaClass _jClass;
  private ConstantPool _jPool;

  private ArrayList<String> _classes = new ArrayList<String>();

  public static void main(String []args)
    throws Exception
  {
    for (String pathName : args) {
      Path path = Vfs.lookup(pathName);

      convert(path);
    }
  }

  private static void convert(Path path)
    throws Exception
  {
    if (path.isDirectory()) {
      for (String name : path.list())
	convert(path.lookup(name));
    }
    else if (path.getPath().endsWith(".class")) {
      new Jdk15Compat().convertFile(path);
    }
  }

  private void convertFile(Path path)
    throws Exception
  {
    JavaClass jClass = null;
    ReadStream is = path.openRead();
    
    try {
      jClass = new ByteCodeParser().parse(is);
    } finally {
      is.close();
    }

    _jClass = jClass;
    _jPool = jClass.getConstantPool();

    if (jClass.getMajor() < MAJOR_1_5)
      return;

    /*
    if (jClass.getMajor() != MAJOR_1_5)
      throw new IllegalStateException();
    */

    jClass.setMajor(MAJOR_1_4);

    try {
      ArrayList<ConstantPoolEntry> entries;
      entries = new ArrayList<ConstantPoolEntry>();
      
      entries.addAll(jClass.getConstantPool().getEntries());
      
      for (ConstantPoolEntry entry : entries) {
	if (entry instanceof Utf8Constant) {
	  Utf8Constant utf8 = (Utf8Constant) entry;
	  String value = utf8.getValue();

	  value = convertStringBuilder(value);

	  utf8.setValue(value);
	}
	else if (entry instanceof MethodRefConstant) {
	  MethodRefConstant method = (MethodRefConstant) entry;

	  convertMethodName(jClass.getConstantPool(), method);
	}
      }
      
      for (JavaField jField : jClass.getFieldList()) {
	jField.setDescriptor(convertStringBuilder(jField.getDescriptor()));
	
	convertField(jClass, jField);
      }
      
      for (JavaMethod jMethod : jClass.getMethodList()) {
	jMethod.setDescriptor(convertStringBuilder(jMethod.getDescriptor()));
	convertMethod(jClass, jMethod);
      }

      for (int i = 0; i < _classes.size(); i++) {
	addClassForName(_classes.get(i));
      }

      System.out.println("COMPLETE: " + jClass);

      WriteStream os = path.openWrite();
      try {
	jClass.write(os);
      } finally {
	os.close();
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private static String STRING_BUILDER =
    "java*lang*StringBuilder".replace('*', '/');

  private String convertStringBuilder(String value)
  {
    while (value.indexOf(STRING_BUILDER) >= 0) {
      value = value.replace(STRING_BUILDER, "com/caucho/util/CharBuffer");
    }

    return value;
  }
  
  private void convertMethodName(ConstantPool cp, MethodRefConstant call)
  {
    if (! call.getName().equals("valueOf"))
      return;

    String shortName = _boxedClasses.get(call.getClassName());
					 
    if (shortName == null)
      return;

    if (call.getType().indexOf("String") >= 0)
      return;

    String className = call.getClassName();
    ClassConstant jClass = cp.addClass("com/caucho/bytecode/compat/Box");
    
    call.setClassIndex(jClass.getIndex());
    call.setNameAndType("box" + shortName, call.getType());
  }

  private void convertMethod(JavaClass jClass, JavaMethod jMethod)
    throws Exception
  {
    CodeAttribute jCode = jMethod.getCode();

    if (jCode == null) {
      return;
    }

    ArrayList<Attribute> attrList = jCode.getAttributes();
    for (int i = attrList.size() - 1; i >= 0; i--) {
      Attribute attr = attrList.get(i);

      if (attr.getName().equals("LocalVariableTable")) {
	convertLocalVariableTable((OpaqueAttribute) attr);
      }
      else if (attr.getName().equals("LocalVariableTypeTable")) {
	attrList.remove(i);
	// convertVariableTypeTable((OpaqueAttribute) attr);
      }
    }
    jCode.setAttributes(attrList);

    CompatAnalyzer analyzer = new CompatAnalyzer();

    CodeEnhancer visitor = new CodeEnhancer(jClass, jCode);

    try {
      visitor.analyze(analyzer, true);

      visitor.update();
    } catch (Exception e) {
      throw e;
    }
  }

  private void convertField(JavaClass jClass, JavaField jField)
    throws Exception
  {
  }

  private void convertVariableTypeTable(OpaqueAttribute attr)
    throws Exception
  {
    byte []data = attr.getValue();

    int offset = 0;
    int len = readShort(data, offset);
    offset += 2;

    for (int i = 0; i < len; i++) {
      int index = readShort(data, offset + 6);

      String descriptor = _jPool.getUtf8AsString(index);

      String newValue = convertDescriptor(descriptor);
      
      Utf8Constant newDescriptor = _jPool.addUTF8(newValue);

      writeShort(data, offset + 6, newDescriptor.getIndex());

      offset += 10;
    }
  }

  private void convertLocalVariableTable(OpaqueAttribute attr)
    throws Exception
  {
    byte []data = attr.getValue();

    int offset = 0;
    int len = readShort(data, offset);
    offset += 2;

    for (int i = 0; i < len; i++) {
      int index = readShort(data, offset + 6);

      String descriptor = _jPool.getUtf8AsString(index);

      String newValue = convertDescriptor(descriptor);
      
      Utf8Constant newDescriptor = _jPool.addUTF8(newValue);

      writeShort(data, offset + 6, newDescriptor.getIndex());

      offset += 10;
    }
  }

  private void addClassForName(String className)
  {
    int classIndex = _classes.indexOf(className);

    if (classIndex < 0)
      throw new IllegalStateException();

    ByteBuffer bb = new ByteBuffer();
      
    String fieldName = "_resin_compat_class_" + classIndex;
    String methodName = "_resin_compat_class_" + classIndex;

    if (_jClass.isInterface())
      throw new IllegalStateException(_jClass + " is an interface.  It can't have .class references.");

    JavaField field = new JavaField();
    field.setJavaClass(_jClass);
    field.setName(fieldName);
    field.setDescriptor("Ljava/lang/Class;");
    field.setAccessFlags(JavaClass.ACC_STATIC|JavaClass.ACC_PRIVATE);

    _jClass.addField(field);

    StringConstant classNameConst = _jPool.addString(className.replace('/', '.'));
      
    FieldRefConstant fieldRefConst;
    fieldRefConst = _jPool.addFieldRef(_jClass.getName().replace('.', '/'),
				       fieldName,
				       "Ljava/lang/Class;");
      
    MethodRefConstant forNameConst;
    forNameConst = _jPool.addMethodRef("java/lang/Class",
				       "forName",
				       "(Ljava/lang/String;)Ljava/lang/Class;");

    bb.addByte(CodeVisitor.GETSTATIC);
    bb.addShort(fieldRefConst.getIndex());
    bb.addByte(CodeVisitor.DUP);
    bb.addByte(CodeVisitor.IFNONNULL);
    bb.addShort(14);
    bb.addByte(CodeVisitor.POP);
    bb.addByte(CodeVisitor.LDC_W);
    bb.addShort(classNameConst.getIndex());
    bb.addByte(CodeVisitor.INVOKESTATIC);
    bb.addShort(forNameConst.getIndex());
    bb.addByte(CodeVisitor.DUP);
    bb.addByte(CodeVisitor.PUTSTATIC);
    bb.addShort(fieldRefConst.getIndex());

    bb.addByte(CodeVisitor.ARETURN);

    int end = bb.getLength();

    bb.addByte(CodeVisitor.ACONST_NULL);
    bb.addByte(CodeVisitor.ARETURN);

    CodeAttribute code = new CodeAttribute();
    byte []codeBuffer = new byte[bb.getLength()];
    System.arraycopy(bb.getBuffer(), 0, codeBuffer, 0, bb.getLength());
      
    code.setCode(codeBuffer);
    code.setMaxLocals(0);
    code.setMaxStack(2);

    code.addException(_jPool.addClass("java/lang/ClassNotFoundException"),
		      0, end, end);

    String descriptor = "()Ljava/lang/Class;";

    _jPool.addUTF8(methodName);
    _jPool.addUTF8("Code");
    _jPool.addUTF8(descriptor);
      
    JavaMethod method = new JavaMethod();
    method.setJavaClass(_jClass);
    method.setName(methodName);
    method.setDescriptor(descriptor);
    method.setAccessFlags(JavaClass.ACC_STATIC|JavaClass.ACC_PRIVATE);

    method.addAttribute(code);

    _jClass.addMethod(method);

    // get static field
    // dup
    // ifnotnull L
    // ldc classname
    // call Class.forName
    // dup
    // set static field
    // L:
  }

  private String convertDescriptor(String descriptor)
  {
    StringBuilder sb = new StringBuilder();

    convertDescriptor(sb, descriptor, 0);

    return sb.toString();
  }

  private int convertDescriptor(StringBuilder sb, String descriptor, int index)
  {
    int length = descriptor.length();

    while (index < length) {
      char ch = descriptor.charAt(index);

      switch (ch) {
      case '<':
	index = convertDescriptor(null, descriptor, index + 1);
	break;
	
      case '>':
	return index + 1;
	
      default:
	if (sb != null)
	  sb.append(ch);
	index++;
	break;
      }
    }

    return index;
  }

  private int readShort(byte []data, int offset)
  {
    return ((data[offset] & 0xff) << 8) + (data[offset + 1] & 0xff);
  }

  private void writeShort(byte []data, int offset, int value)
  {
    data[offset + 0] = (byte) (value >> 8);
    data[offset + 1] = (byte) (value);
  }

  public class CompatAnalyzer extends Analyzer {
    public void analyze(CodeVisitor visitor)
      throws Exception
    {
      CodeEnhancer enhancer = (CodeEnhancer) visitor;
      int op = visitor.getOpcode();
      int index;
      ConstantPoolEntry entry;

      switch (op) {
      case CodeVisitor.LDC_W:
	entry = _jPool.getEntry(visitor.getShortArg(1));

	if (entry instanceof ClassConstant) {
	  ClassConstant classEntry = (ClassConstant) entry;
	  int offset = enhancer.getOffset();
	  enhancer.remove(offset, 3);
	  addClassGetter(enhancer, offset, classEntry.getName());
	}
	break;

      case CodeVisitor.LDC:
	entry = _jPool.getEntry(visitor.getByteArg(1));

	if (entry instanceof ClassConstant) {
	  ClassConstant classEntry = (ClassConstant) entry;
	  int offset = enhancer.getOffset();
	  enhancer.remove(offset, 2);
	  addClassGetter(enhancer, offset, classEntry.getName());
	}
	break;

      default:
	break;
      }
    }

    private void addClassGetter(CodeEnhancer enhancer,
				int offset,
				String className)
    {
      int classIndex = _classes.indexOf(className);

      if (classIndex < 0) {
	_classes.add(className);
	classIndex = _classes.size() - 1;
      }

      String methodName = "_resin_compat_class_" + classIndex;
      
      StringConstant classNameConst = _jPool.addString(className);
      
      MethodRefConstant forNameConst;
      forNameConst = _jPool.addMethodRef(_jClass.getName().replace('.', '/'),
					 methodName,
					 "()Ljava/lang/Class;");


      enhancer.addNulls(offset, 3);
      enhancer.setByte(offset  + 0, CodeVisitor.INVOKESTATIC);
      enhancer.setShort(offset + 1, forNameConst.getIndex());
      enhancer.setOffset(offset);
    }
  }

  static {
    _boxedClasses.put("java/lang/Boolean", "Boolean");
    _boxedClasses.put("java/lang/Byte", "Byte");
    _boxedClasses.put("java/lang/Short", "Short");
    _boxedClasses.put("java/lang/Integer", "Integer");
    _boxedClasses.put("java/lang/Long", "Long");
    _boxedClasses.put("java/lang/Float", "Float");
    _boxedClasses.put("java/lang/Double", "Double");
    _boxedClasses.put("java/lang/Character", "Character");
  }
}
