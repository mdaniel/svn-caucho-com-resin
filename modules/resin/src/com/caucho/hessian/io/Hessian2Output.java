/*
 * Copyright (c) 2001-2006 Caucho Technology, Inc.  All rights reserved.
 *
 * The Apache Software License, Version 1.1
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Burlap", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Scott Ferguson
 */

package com.caucho.hessian.io;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;

/**
 * Output stream for Hessian 2 requests.
 *
 * <p>Since HessianOutput does not depend on any classes other than
 * in the JDK, it can be extracted independently into a smaller package.
 *
 * <p>HessianOutput is unbuffered, so any client needs to provide
 * its own buffering.
 *
 * <pre>
 * OutputStream os = ...; // from http connection
 * Hessian2Output out = new Hessian11Output(os);
 * String value;
 *
 * out.startCall("hello");  // start hello call
 * out.writeString("arg1"); // write a string argument
 * out.completeCall();      // complete the call
 * </pre>
 */
public class Hessian2Output extends AbstractHessianOutput {
  public static final int INT_DIRECT_MIN = -0x10;
  public static final int INT_DIRECT_MAX = 0x3f;
  public static final int INT_ZERO = 0x90;
  
  public static final long LONG_DIRECT_MIN = -0x0f;
  public static final long LONG_DIRECT_MAX =  0x0f;
  public static final int LONG_ZERO = 0x30;
  
  public static final int STRING_DIRECT_MAX = 0x1f;
  public static final int STRING_DIRECT = 0xd0;
  
  public static final int BYTES_DIRECT_MAX = 0x0f;
  public static final int BYTES_DIRECT = 0xf0;
  
  public static final int LENGTH_DIRECT_MAX = 0x0f;
  public static final int LENGTH_DIRECT = 0x10;
  
  public static final int INT_BYTE = 0x01;
  public static final int INT_SHORT = 0x02;
  
  public static final int LONG_BYTE = 0x03;
  public static final int LONG_SHORT = 0x04;
  public static final int LONG_INT = 0x05;
  
  public static final int DOUBLE_ZERO = 0x06;
  public static final int DOUBLE_ONE = 0x07;
  public static final int DOUBLE_BYTE = 0x08;
  public static final int DOUBLE_SHORT = 0x09;
  // skip 0x0a
  public static final int DOUBLE_INT = 0x0b;
  public static final int DOUBLE_256_SHORT = 0x0c;

  // skip 0x0d
  public static final int LENGTH_BYTE = 0x0e;
  
  public static final int REF_BYTE = 0x5b;
  public static final int REF_SHORT = 0x5c;
  
  // the output stream/
  protected OutputStream _os;
  // map of references
  private IdentityHashMap _refs;
  // map of classes
  private HashMap _classRefs;
  
  /**
   * Creates a new Hessian output stream, initialized with an
   * underlying output stream.
   *
   * @param os the underlying output stream.
   */
  public Hessian2Output(OutputStream os)
  {
    init(os);
  }

  /**
   * Creates an uninitialized Hessian output stream.
   */
  public Hessian2Output()
  {
  }

  /**
   * Initializes the output
   */
  public void init(OutputStream os)
  {
    _os = os;

    _refs = null;

    if (_serializerFactory == null)
      _serializerFactory = new SerializerFactory();
  }

  /**
   * Writes a complete method call.
   */
  public void call(String method, Object []args)
    throws IOException
  {
    startCall(method);
    
    if (args != null) {
      for (int i = 0; i < args.length; i++)
        writeObject(args[i]);
    }
    
    completeCall();
  }

  /**
   * Starts the method call.  Clients would use <code>startCall</code>
   * instead of <code>call</code> if they wanted finer control over
   * writing the arguments, or needed to write headers.
   *
   * <code><pre>
   * c major minor
   * m b16 b8 method-name
   * </pre></code>
   *
   * @param method the method name to call.
   */
  public void startCall(String method)
    throws IOException
  {
    _os.write('c');
    _os.write(2);
    _os.write(0);

    _os.write('m');
    int len = method.length();
    _os.write(len >> 8);
    _os.write(len);
    printString(method, 0, len);
  }

  /**
   * Writes the call tag.  This would be followed by the
   * headers and the method tag.
   *
   * <code><pre>
   * c major minor
   * </pre></code>
   *
   * @param method the method name to call.
   */
  public void startCall()
    throws IOException
  {
    _os.write('c');
    _os.write(1);
    _os.write(1);
  }

  /**
   * Writes the method tag.
   *
   * <code><pre>
   * m b16 b8 method-name
   * </pre></code>
   *
   * @param method the method name to call.
   */
  public void writeMethod(String method)
    throws IOException
  {
    _os.write('m');
    int len = method.length();
    _os.write(len >> 8);
    _os.write(len);
    printString(method, 0, len);
  }

  /**
   * Completes.
   *
   * <code><pre>
   * z
   * </pre></code>
   */
  public void completeCall()
    throws IOException
  {
    _os.write('z');
  }

  /**
   * Starts the reply
   *
   * <p>A successful completion will have a single value:
   *
   * <pre>
   * r
   * </pre>
   */
  public void startReply()
    throws IOException
  {
    _os.write('r');
    _os.write(1);
    _os.write(1);
  }

  /**
   * Completes reading the reply
   *
   * <p>A successful completion will have a single value:
   *
   * <pre>
   * z
   * </pre>
   */
  public void completeReply()
    throws IOException
  {
    _os.write('z');
  }

  /**
   * Writes a header name.  The header value must immediately follow.
   *
   * <code><pre>
   * H b16 b8 foo <em>value</em>
   * </pre></code>
   */
  public void writeHeader(String name)
    throws IOException
  {
    int len = name.length();
    
    _os.write('H');
    _os.write(len >> 8);
    _os.write(len);

    printString(name);
  }

  /**
   * Writes a fault.  The fault will be written
   * as a descriptive string followed by an object:
   *
   * <code><pre>
   * f
   * &lt;string>code
   * &lt;string>the fault code
   *
   * &lt;string>message
   * &lt;string>the fault mesage
   *
   * &lt;string>detail
   * mt\x00\xnnjavax.ejb.FinderException
   *     ...
   * z
   * z
   * </pre></code>
   *
   * @param code the fault code, a three digit
   */
  public void writeFault(String code, String message, Object detail)
    throws IOException
  {
    _os.write('f');
    writeString("code");
    writeString(code);

    writeString("message");
    writeString(message);

    if (detail != null) {
      writeString("detail");
      writeObject(detail);
    }
    _os.write('z');
  }

  /**
   * Writes any object to the output stream.
   */
  public void writeObject(Object object)
    throws IOException
  {
    if (object == null) {
      writeNull();
      return;
    }

    Serializer serializer;

    serializer = _serializerFactory.getSerializer(object.getClass());

    serializer.writeObject(object, this);
  }

  /**
   * Writes the list header to the stream.  List writers will call
   * <code>writeListBegin</code> followed by the list contents and then
   * call <code>writeListEnd</code>.
   *
   * <code><pre>
   * V
   * t b16 b8 type
   * l b32 b24 b16 b8
   * </pre></code>
   */
  public void writeListBegin(int length, String type)
    throws IOException
  {
    _os.write('V');

    if (type != null) {
      _os.write('t');
      printLenString(type);
    }

    if (length < 0) {
    }
    else if (length < 0x10) {
      _os.write(LENGTH_DIRECT + length);
    }
    else if (length < 0x100) {
      _os.write(LENGTH_BYTE);
      _os.write(length);
    }
    else {
      _os.write('l');
      _os.write(length >> 24);
      _os.write(length >> 16);
      _os.write(length >> 8);
      _os.write(length);
    }
  }

  /**
   * Writes the tail of the list to the stream.
   */
  public void writeListEnd()
    throws IOException
  {
    _os.write('z');
  }

  /**
   * Writes the map header to the stream.  Map writers will call
   * <code>writeMapBegin</code> followed by the map contents and then
   * call <code>writeMapEnd</code>.
   *
   * <code><pre>
   * Mt b16 b8 (<key> <value>)z
   * </pre></code>
   */
  public void writeMapBegin(String type)
    throws IOException
  {
    _os.write('M');

    if (type != null && type.length() > 0) {
      _os.write('t');
      printLenString(type);
    }
  }

  /**
   * Writes the tail of the map to the stream.
   */
  public void writeMapEnd()
    throws IOException
  {
    _os.write('z');
  }

  /**
   * Writes the object header to the stream.
   *
   * <code><pre>
   * Ot b16 b8 <key>* Z <value>* z
   * </pre></code>
   */
  public int writeObjectBegin(String type)
    throws IOException
  {
    if (_classRefs == null)
      _classRefs = new HashMap();
    
    Integer refValue = (Integer) _classRefs.get(type);
    int ref = 0;

    if (refValue != null) {
      ref = refValue.intValue();

      if (ref < 0x100) {
	_os.write('o');
	_os.write(ref);
      }
      else if (ref < 0x10000) {
	_os.write('p');
	_os.write(ref >> 8);
	_os.write(ref);
      }
      else if (ref < 0x10000) {
	_os.write('q');
	_os.write(ref >> 24);
	_os.write(ref >> 16);
	_os.write(ref >> 8);
	_os.write(ref);
      }

      return ref;
    }
    else {
      ref = _classRefs.size() + 1;
      _classRefs.put(type, new Integer(ref));
    
      _os.write('O');

      printLenString(type);

      return 0;
    }
  }

  /**
   * Writes the tail of the class definition to the stream.
   */
  public void writeClassEnd()
    throws IOException
  {
    _os.write('z');
  }

  /**
   * Writes the tail of the object definition to the stream.
   */
  public void writeObjectEnd()
    throws IOException
  {
  }

  /**
   * Writes a remote object reference to the stream.  The type is the
   * type of the remote interface.
   *
   * <code><pre>
   * 'r' 't' b16 b8 type url
   * </pre></code>
   */
  public void writeRemote(String type, String url)
    throws IOException
  {
    _os.write('r');
    _os.write('t');
    printLenString(type);
    _os.write('S');
    printLenString(url);
  }

  /**
   * Writes a boolean value to the stream.  The boolean will be written
   * with the following syntax:
   *
   * <code><pre>
   * T
   * F
   * </pre></code>
   *
   * @param value the boolean value to write.
   */
  public void writeBoolean(boolean value)
    throws IOException
  {
    if (value)
      _os.write('T');
    else
      _os.write('F');
  }

  /**
   * Writes an integer value to the stream.  The integer will be written
   * with the following syntax:
   *
   * <code><pre>
   * I b32 b24 b16 b8
   * </pre></code>
   *
   * @param value the integer value to write.
   */
  public void writeInt(int value)
    throws IOException
  {
    if (INT_DIRECT_MIN <= value && value <= INT_DIRECT_MAX)
      _os.write(value + INT_ZERO);
    else if (-0x80 <= value && value < 0x80) {
      _os.write(INT_BYTE);
      _os.write(value);
    }
    else if (-0x8000 <= value && value <= 0x7fff) {
      _os.write(INT_SHORT);
      _os.write(value >> 8);
      _os.write(value);
    }
    else {
      _os.write('I');
      _os.write(value >> 24);
      _os.write(value >> 16);
      _os.write(value >> 8);
      _os.write(value);
    }
  }

  /**
   * Writes a long value to the stream.  The long will be written
   * with the following syntax:
   *
   * <code><pre>
   * L b64 b56 b48 b40 b32 b24 b16 b8
   * </pre></code>
   *
   * @param value the long value to write.
   */
  public void writeLong(long value)
    throws IOException
  {
    if (LONG_DIRECT_MIN <= value && value <= LONG_DIRECT_MAX)
      _os.write((byte) (value + LONG_ZERO));
    else if (-0x80 <= value && value < 0x80) {
      _os.write(LONG_BYTE);
      _os.write((byte) value);
    }
    else if (-0x8000 <= value && value <= 0x7fff) {
      _os.write(LONG_SHORT);
      _os.write((byte) (value >> 8));
      _os.write((byte) value);
    }
    else if (-0x80000000L <= value && value <= 0x7fffffffL) {
      _os.write(LONG_INT);
      _os.write((byte) (value >> 24));
      _os.write((byte) (value >> 16));
      _os.write((byte) (value >> 8));
      _os.write((byte) (value));
    }
    else {
      _os.write('L');
      _os.write((byte) (value >> 56));
      _os.write((byte) (value >> 48));
      _os.write((byte) (value >> 40));
      _os.write((byte) (value >> 32));
      _os.write((byte) (value >> 24));
      _os.write((byte) (value >> 16));
      _os.write((byte) (value >> 8));
      _os.write((byte) (value));
    }
  }

  /**
   * Writes a double value to the stream.  The double will be written
   * with the following syntax:
   *
   * <code><pre>
   * D b64 b56 b48 b40 b32 b24 b16 b8
   * </pre></code>
   *
   * @param value the double value to write.
   */
  public void writeDouble(double value)
    throws IOException
  {
    int intValue = (int) value;
    
    if (intValue == value) {
      if (intValue == 0)
	_os.write(DOUBLE_ZERO);
      else if (intValue == 1)
	_os.write(DOUBLE_ONE);
      else if (-0x80 <= intValue && intValue < 0x80) {
	_os.write(DOUBLE_BYTE);
	_os.write((byte) intValue);
      }
      else if (-0x8000 <= intValue && intValue < 0x8000) {
	_os.write(DOUBLE_SHORT);
	_os.write((byte) (intValue >> 8));
	_os.write((byte) intValue);
      }
      else {
	_os.write(DOUBLE_INT);
	_os.write((byte) (intValue >> 24));
	_os.write((byte) (intValue >> 16));
	_os.write((byte) (intValue >> 8));
	_os.write((byte) intValue);
      }

      return;
    }

    double d256 = 256 * value;
    int i256 = (int) d256;

    if (d256 == i256 && -0x8000 <= i256 && i256 < 0x8000) {
      _os.write(DOUBLE_256_SHORT);
      _os.write(i256 >> 8);
      _os.write(i256);

      return;
    }
    
    long bits = Double.doubleToLongBits(value);
    
    _os.write('D');
    _os.write((byte) (bits >> 56));
    _os.write((byte) (bits >> 48));
    _os.write((byte) (bits >> 40));
    _os.write((byte) (bits >> 32));
    _os.write((byte) (bits >> 24));
    _os.write((byte) (bits >> 16));
    _os.write((byte) (bits >> 8));
    _os.write((byte) (bits));
  }

  /**
   * Writes a date to the stream.
   *
   * <code><pre>
   * T  b64 b56 b48 b40 b32 b24 b16 b8
   * </pre></code>
   *
   * @param time the date in milliseconds from the epoch in UTC
   */
  public void writeUTCDate(long time)
    throws IOException
  {
    _os.write('d');
    _os.write((byte) (time >> 56));
    _os.write((byte) (time >> 48));
    _os.write((byte) (time >> 40));
    _os.write((byte) (time >> 32));
    _os.write((byte) (time >> 24));
    _os.write((byte) (time >> 16));
    _os.write((byte) (time >> 8));
    _os.write((byte) (time));
  }

  /**
   * Writes a null value to the stream.
   * The null will be written with the following syntax
   *
   * <code><pre>
   * N
   * </pre></code>
   *
   * @param value the string value to write.
   */
  public void writeNull()
    throws IOException
  {
    _os.write('N');
  }

  /**
   * Writes a string value to the stream using UTF-8 encoding.
   * The string will be written with the following syntax:
   *
   * <code><pre>
   * S b16 b8 string-value
   * </pre></code>
   *
   * If the value is null, it will be written as
   *
   * <code><pre>
   * N
   * </pre></code>
   *
   * @param value the string value to write.
   */
  public void writeString(String value)
    throws IOException
  {
    if (value == null) {
      _os.write('N');
    }
    else {
      int length = value.length();
      int offset = 0;
      
      while (length > 0x8000) {
        int sublen = 0x8000;
        
        _os.write('s');
        _os.write(sublen >> 8);
        _os.write(sublen);

        printString(value, offset, sublen);

        length -= sublen;
        offset += sublen;
      }

      if (length <= STRING_DIRECT_MAX) {
	_os.write(STRING_DIRECT + length);
      }
      else {
	_os.write('S');
	_os.write(length >> 8);
	_os.write(length);
      }

      printString(value, offset, length);
    }
  }

  /**
   * Writes a string value to the stream using UTF-8 encoding.
   * The string will be written with the following syntax:
   *
   * <code><pre>
   * S b16 b8 string-value
   * </pre></code>
   *
   * If the value is null, it will be written as
   *
   * <code><pre>
   * N
   * </pre></code>
   *
   * @param value the string value to write.
   */
  public void writeString(char []buffer, int offset, int length)
    throws IOException
  {
    if (buffer == null) {
      _os.write('N');
    }
    else {
      while (length > 0x8000) {
        int sublen = 0x8000;

        _os.write('s');
        _os.write(sublen >> 8);
        _os.write(sublen);

        printString(buffer, offset, sublen);

        length -= sublen;
        offset += sublen;
      }

      if (length < STRING_DIRECT_MAX) {
	_os.write(STRING_DIRECT + length);
      }
      else {
	_os.write('S');
	_os.write(length >> 8);
	_os.write(length);
      }

      printString(buffer, offset, length);
    }
  }

  /**
   * Writes a byte array to the stream.
   * The array will be written with the following syntax:
   *
   * <code><pre>
   * B b16 b18 bytes
   * </pre></code>
   *
   * If the value is null, it will be written as
   *
   * <code><pre>
   * N
   * </pre></code>
   *
   * @param value the string value to write.
   */
  public void writeBytes(byte []buffer)
    throws IOException
  {
    if (buffer == null)
      _os.write('N');
    else
      writeBytes(buffer, 0, buffer.length);
  }
  
  /**
   * Writes a byte array to the stream.
   * The array will be written with the following syntax:
   *
   * <code><pre>
   * B b16 b18 bytes
   * </pre></code>
   *
   * If the value is null, it will be written as
   *
   * <code><pre>
   * N
   * </pre></code>
   *
   * @param value the string value to write.
   */
  public void writeBytes(byte []buffer, int offset, int length)
    throws IOException
  {
    if (buffer == null) {
      _os.write('N');
    }
    else {
      while (length > 0x8000) {
        int sublen = 0x8000;
        
        _os.write('b');
        _os.write(sublen >> 8);
        _os.write(sublen);

        _os.write(buffer, offset, sublen);

        length -= sublen;
        offset += sublen;
      }

      if (length < 0x10) {
	_os.write(BYTES_DIRECT + length);
      }
      else {
	_os.write('B');
	_os.write(length >> 8);
	_os.write(length);
      }
      
      _os.write(buffer, offset, length);
    }
  }
  
  /**
   * Writes a byte buffer to the stream.
   *
   * <code><pre>
   * </pre></code>
   */
  public void writeByteBufferStart()
    throws IOException
  {
  }
  
  /**
   * Writes a byte buffer to the stream.
   *
   * <code><pre>
   * b b16 b18 bytes
   * </pre></code>
   */
  public void writeByteBufferPart(byte []buffer, int offset, int length)
    throws IOException
  {
    while (length > 0) {
      int sublen = length;

      if (0x8000 < sublen)
	sublen = 0x8000;

      _os.write('b');
      _os.write(sublen >> 8);
      _os.write(sublen);

      _os.write(buffer, offset, sublen);

      length -= sublen;
      offset += sublen;
    }
  }
  
  /**
   * Writes a byte buffer to the stream.
   *
   * <code><pre>
   * b b16 b18 bytes
   * </pre></code>
   */
  public void writeByteBufferEnd(byte []buffer, int offset, int length)
    throws IOException
  {
    writeBytes(buffer, offset, length);
  }

  /**
   * Writes a reference.
   *
   * <code><pre>
   * R b32 b24 b16 b8
   * </pre></code>
   *
   * @param value the integer value to write.
   */
  public void writeRef(int value)
    throws IOException
  {
    if (value < 0x100) {
      _os.write(REF_BYTE);
      _os.write(value);
    }
    else if (value < 0x10000) {
      _os.write(REF_SHORT);
      _os.write(value);
    }
    else {
      _os.write('R');
      _os.write(value >> 24);
      _os.write(value >> 16);
      _os.write(value >> 8);
      _os.write(value);
    }
  }

  /**
   * If the object has already been written, just write its ref.
   *
   * @return true if we're writing a ref.
   */
  public boolean addRef(Object object)
    throws IOException
  {
    if (_refs == null)
      _refs = new IdentityHashMap();

    Integer ref = (Integer) _refs.get(object);

    if (ref != null) {
      int value = ref.intValue();
      
      writeRef(value);
      return true;
    }
    else {
      _refs.put(object, new Integer(_refs.size()));
      
      return false;
    }
  }

  /**
   * Removes a reference.
   */
  public boolean removeRef(Object obj)
    throws IOException
  {
    if (_refs != null) {
      _refs.remove(obj);

      return true;
    }
    else
      return false;
  }

  /**
   * Replaces a reference from one object to another.
   */
  public boolean replaceRef(Object oldRef, Object newRef)
    throws IOException
  {
    Integer value = (Integer) _refs.remove(oldRef);

    if (value != null) {
      _refs.put(newRef, value);
      return true;
    }
    else
      return false;
  }

  /**
   * Prints a string to the stream, encoded as UTF-8 with preceeding length
   *
   * @param v the string to print.
   */
  public void printLenString(String v)
    throws IOException
  {
    if (v == null) {
      _os.write(0);
      _os.write(0);
    }
    else {
      int len = v.length();
      _os.write(len >> 8);
      _os.write(len);

      printString(v, 0, len);
    }
  }

  /**
   * Prints a string to the stream, encoded as UTF-8
   *
   * @param v the string to print.
   */
  public void printString(String v)
    throws IOException
  {
    printString(v, 0, v.length());
  }
  
  /**
   * Prints a string to the stream, encoded as UTF-8
   *
   * @param v the string to print.
   */
  public void printString(String v, int offset, int length)
    throws IOException
  {
    for (int i = 0; i < length; i++) {
      char ch = v.charAt(i + offset);

      if (ch < 0x80)
        _os.write(ch);
      else if (ch < 0x800) {
        _os.write(0xc0 + ((ch >> 6) & 0x1f));
        _os.write(0x80 + (ch & 0x3f));
      }
      else {
        _os.write(0xe0 + ((ch >> 12) & 0xf));
        _os.write(0x80 + ((ch >> 6) & 0x3f));
        _os.write(0x80 + (ch & 0x3f));
      }
    }
  }
  
  /**
   * Prints a string to the stream, encoded as UTF-8
   *
   * @param v the string to print.
   */
  public void printString(char []v, int offset, int length)
    throws IOException
  {
    for (int i = 0; i < length; i++) {
      char ch = v[i + offset];

      if (ch < 0x80)
        _os.write(ch);
      else if (ch < 0x800) {
        _os.write(0xc0 + ((ch >> 6) & 0x1f));
        _os.write(0x80 + (ch & 0x3f));
      }
      else {
        _os.write(0xe0 + ((ch >> 12) & 0xf));
        _os.write(0x80 + ((ch >> 6) & 0x3f));
        _os.write(0x80 + (ch & 0x3f));
      }
    }
  }
}
