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
 * @author Scott Ferguson
 */

package com.caucho.iiop;

import java.io.*;
import java.net.*;
import java.util.*;

import java.util.logging.Logger;

import javax.rmi.CORBA.ValueHandler;
import javax.rmi.CORBA.Util;

import org.omg.CORBA.Any;
import org.omg.CORBA.Context;
import org.omg.CORBA.ContextList;
import org.omg.CORBA.Principal;
import org.omg.CORBA.TypeCode;

import org.omg.CORBA.portable.IndirectionException;

import org.omg.SendingContext.RunTime;

import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.log.Log;

public class IiopReader extends org.omg.CORBA_2_3.portable.InputStream {
  protected static final L10N L = new L10N(IiopReader.class);
  protected static final Logger log = Log.open(IiopReader.class);
  
  public static final int MSG_REQUEST = 0;
  public static final int MSG_REPLY = 1;
  public static final int MSG_CANCEL_REQUEST = 2;
  public static final int MSG_LOCATE_REQUEST = 3;
  public static final int MSG_LOCATE_REPLY = 4;
  public static final int MSG_CLOSE_CONNECTION = 5;
  public static final int MSG_ERROR = 6;
  public static final int MSG_FRAGMENT = 7;
  
  public static final int SERVICE_TRANSACTION = 0;
  public static final int SERVICE_CODE_SET = 1;
  public static final int SERVICE_CHAIN_BYPASS_CHECK = 2;
  public static final int SERVICE_CHAIN_BYPASS_INFO = 3;
  public static final int SERVICE_LOGICAL_THREAD_ID = 4;
  public static final int SERVICE_BI_DIR_IIOP = 5;
  public static final int SERVICE_SENDING_CONTEXT_RUN_TIME = 6;
  public static final int SERVICE_INVOCATION_POLICIES = 7;
  public static final int SERVICE_FORWARDED_IDENTITY = 8;
  public static final int SERVICE_UNKNOWN_EXCEPTION_INFO = 9;

  public static final int STATUS_NO_EXCEPTION = 0;
  public static final int STATUS_USER_EXCEPTION = 1;
  public static final int STATUS_SYSTEM_EXCEPTION = 2;
  public static final int STATUS_LOCATION_FORWARD = 3;
  
  private ReadStream _rs;
  private byte []_header = new byte[8];
  private byte []buf = new byte[16];

  private IntArray _refOffsets = new IntArray();
  private ArrayList<String> _refIds = new ArrayList<String>();
  private ArrayList<Class> _refClasses = new ArrayList<Class>();
  private ArrayList<Serializable> _refValues = new ArrayList<Serializable>();

  private CharBuffer cb = new CharBuffer();

  private int _major;
  private int _minor;
  private boolean _isBigEndian;
  private boolean _hasMoreFragments;
  private int _flags;

  private int _offset;
  private int _length;
  private int _type;
  
  private int _fragmentOffset;
  
  private int _chunkEnd = -1;
  private int _chunkDepth = 0;

  private int requestId;
  private boolean responseExpected;
  private ByteBuffer objectKey = new ByteBuffer();
  private CharBuffer _operation = new CharBuffer();
  private ByteBuffer principal = new ByteBuffer();
  
  private ValueHandler _valueHandler = Util.createValueHandler();
  private RunTime runTime = _valueHandler.getRunTimeCodeBase();

  public IiopReader()
  {
  }

  public IiopReader(ReadStream rs)
  {
    init(rs);
  }

  /**
   * Initialize the reader with a new underlying stream.
   *
   * @param rs the underlying input stream.
   */
  public void init(ReadStream rs)
  {
    _rs = rs;
    _major = 0;
    _minor = 0;
    _type = 0;
    requestId = 0;
    objectKey.clear();
    _operation.clear();
    _offset = 0;
    _length = 0;
    _fragmentOffset = 0;

    _refOffsets.clear();
    _refIds.clear();
    _refClasses.clear();
    _refValues.clear();
  }

  public int getMajorVersion()
  {
    return _major;
  }

  public int getMinorVersion()
  {
    return _minor;
  }

  public int getRequestType()
  {
    return _type;
  }

  public int getRequestId()
  {
    return requestId;
  }

  public boolean isBigEndian()
  {
    return _isBigEndian;
  }

  public int getOffset()
  {
    return _offset;
  }

  public boolean isResponseExpected()
  {
    return responseExpected;
  }

  public ByteBuffer getObjectKey()
  {
    return objectKey;
  }

  public CharBuffer getOperation()
  {
    return _operation;
  }

  public void readRequest()
    throws IOException
  {
    int len = _rs.readAll(_header, 0, _header.length);

    if (_header[0] != 'G' ||
        _header[1] != 'I' ||
        _header[2] != 'O' ||
        _header[3] != 'P') {
      throw new IOException(L.l("unknown request {0}, {1}, {2}, {3}",
				"" + toCh(_header[0]),
				"" + toCh(_header[1]),
				"" + toCh(_header[2]),
				"" + toCh(_header[3])));
    }

    _major = _header[4];
    _minor = _header[5];

    if (_major != 1)
      throw new IOException("unknown major");

    _flags = _header[6];
    _isBigEndian = (_flags & 1) == 0;
    _hasMoreFragments = (_flags & 2) == 2;
    // log.info("FLAGS: " + _flags + " " + _hasMoreFragments);
    _type = _header[7];
    
    // ejb/1161
    _length = readIntRaw() + 4;
    _offset = 4;

    if (_minor == 0) {
      switch (_type) {
      case MSG_REQUEST:
        readRequest10();
        break;
      case MSG_REPLY:
        readReply10();
        break;
      case MSG_ERROR:
        throw new RuntimeException("MSG_ERROR: unknown protocol error");
      default:
        throw new RuntimeException();
      }
    }
    else if (_minor == 1) {
      switch (_type) {
      case MSG_REQUEST:
        readRequest10();
        break;
      case MSG_REPLY:
        readReply10();
        break;
      case MSG_ERROR:
        throw new RuntimeException("MSG_ERROR: unknown protocol error");
      default:
        throw new RuntimeException();
      }
    }
    else if (_minor == 2) {
      switch (_type) {
      case MSG_REQUEST:
        readRequest12();
        break;
      case MSG_REPLY:
        readReply10();
        break;
      case MSG_ERROR:
        throw new RuntimeException("MSG_ERROR: unknown protocol error");
      default:
        throw new RuntimeException("unknown type: " + _type);
      }
    }
    else
      throw new IOException("unknown minor");
  }

  private void readRequest10()
    throws IOException
  {
    readServiceContextList();
    requestId = readInt();
    responseExpected = read_octet() != 0;
    
    readOctetSequence(objectKey);

    readString(_operation);
    
    readOctetSequence(principal);
  }

  private void readReply10()
    throws IOException
  {
    readServiceContextList();

    int requestId = readInt();
    int status = readInt();

    switch (status) {
    case STATUS_NO_EXCEPTION:
      //debugTail();
      return;
        
    case STATUS_SYSTEM_EXCEPTION:
      String exceptionId = readString();
      int minorStatus = readInt();
      int completionStatus = readInt();
      throw new IOException("exception: " + exceptionId);
        
    case STATUS_USER_EXCEPTION:
      Object value = read_fault();
      
      throw new IOException("user exception: " + value);
        
    default:
      throw new IOException("unknown status: " + status);
    }
  }

  private void readRequest12()
    throws IOException
  {
    requestId = readInt();
    int flags = read_octet();
    responseExpected = flags != 0;

    int disposition = read_long();
    readOctetSequence(objectKey);
    readString(_operation);

    readServiceContextList();
    
    int frag = _offset % 8;
    if (frag > 0 && frag < 8) {
      int delta = 8 - frag;

      if (_length < _offset + delta)
	delta = _length - _offset;

      if (delta > 0) {
	_offset += delta;
	_rs.skip(delta);
      }
    }
  }

  private void readServiceContextList()
    throws IOException
  {
    int length = readInt();

    for (int i = 0; i < length; i++) {
      int serviceId = readInt();
      int dataLength = readInt();

      if (serviceId == SERVICE_CODE_SET) {
        int endian = read_octet();
        int charSet = readInt();
        int wcharSet = readInt();
      }
      else {
        skip(dataLength);
      }
    }
  }

  private void debugTail()
    throws IOException
  {
    int len = _length;
    for (int i = 0; i < len; i += 8) {
      int sublen = _rs.read(buf, 0, 16);

      for (int j = 0; j < 16; j++) {
        if (j < sublen)
          printHex(buf[j]);
        else
          System.out.print("  ");

        if (j == 7)
          System.out.print(" - ");
        else
          System.out.print(" ");
      }
      
      for (int j = 0; j < 16; j++) {
        int ch = buf[j] & 0xff;
        
        if (j >= sublen)
          System.out.print("?");
        else if (ch >= 0x20 && ch < 0x80)
          System.out.print((char) ch);
        else
          System.out.print("?");
      }
        
      System.out.println();
    }
    System.out.println();
  }

  public IOR readIOR()
    throws IOException
  {
    IOR ior = new IOR();

    return ior.read(this);
  }
    
  public Object readObject(Class cl)
    throws IOException
  {
    IOR ior = readIOR();
    return null;
  }

  public String readWideString()
    throws IOException
  {
    // XXX: assume ucs-16
    CharBuffer cb = CharBuffer.allocate();
    int len = readInt();
    for (; len > 0; len--) {
      cb.append((char) read_short());
    }

    return cb.close();
  }

  public Serializable read_value()
  {
    return read_value((Class) null);
  }
    
  public Serializable read_value(Class type)
  {
    try {
      // ejb/114o tests for chunking
      int oldChunkEnd = _chunkEnd;
      
      // writeHexGroup(16);

      _chunkEnd = -1;
      align4();
      int startOffset = _offset - _fragmentOffset;
      System.out.println("RV: " + type + " O:" + startOffset);
      
      int code = read_long();

      String repId = "";
      boolean isChunked = false;
      Serializable value = null;

      if (code == 0)
	return null;
      else if (code == 0xffffffff) {
	_chunkEnd = oldChunkEnd;
	
	int start = _offset - _fragmentOffset;
	int delta = read_long();
	int target = start + delta;
	
	for (int i = 0; i < _refOffsets.size(); i++) {
	  int refOffset = _refOffsets.get(i);

	  if (refOffset == target)
	    return _refValues.get(i);
	}

	throw new IndirectionException(target);
      }
      else if ((code & 0x7fffff00) != 0x7fffff00) {
	repId = readString(code);
	System.out.println("old-rep:" + code + " " + repId);
      }
      else {
	isChunked = (code & 8) == 8;
	boolean hasCodeBase = (code & 1) == 1;
	int repository = (code & 6);
      
	System.out.println("chunked:" + isChunked + " CB: " + hasCodeBase + " REP: " + repository + " " + toHex(code));

	if (hasCodeBase) {
	  readCodeBase();
	}

	if (repository == 2) {
	  repId = read_string();
	}
	else {
	  throw new RuntimeException("Can't cope with repository=" + repository);
	}
      }

      System.out.println("REP: " + repId);

      try {
	if (isChunked) {
	  // writeHexGroup(16);

	  int chunkLength = readInt();

	  _chunkEnd = chunkLength + _offset;
	  _chunkDepth++;

	  System.out.println("CHUNK: " + _chunkEnd + " L:" + chunkLength + " " + _chunkDepth + " OFFSET:" + _offset);
	}

	// XXX: assume ucs-16
	if (repId.equals("IDL:omg.org/CORBA/WStringValue:1.0")) {
	  value = read_wstring();
	}
	else if (! repId.startsWith("RMI:") && ! repId.startsWith("IDL:")) {
	  System.out.println("REP: " + repId);
	  
	  log.warning("unknown rep: " + repId + " " + code);
	  throw new UnsupportedOperationException("problem parsing");
	}
	else {
	  int p = repId.indexOf(':', 4);
	  if (p < 0)
	    throw new RuntimeException("unknown RMI: " + repId);

	  String className = repId.substring(4, p);
	  
	  if (className.equals("javax.rmi.CORBA.ClassDesc")) {
	    value = readClass();
	  }
	  else {
	    Class cl = null;
	    
	    try {
	      cl = CauchoSystem.loadClass(className);
	    } catch (ClassNotFoundException e) {
	      e.printStackTrace();
	      throw new RuntimeException(e);
	    }

	    int refIndex = _refOffsets.size();

	    System.out.println("VH: " + cl  + " SO:" + startOffset);
	    value = _valueHandler.readValue(this, startOffset,
					    cl, repId, runTime);
	    System.out.println("VH: " + value);

	    _refOffsets.add(startOffset);
	    _refValues.add(value);
	  }
	}

	return value;
      } finally {
	if (_chunkDepth > 0) {
	  _chunkDepth--;

	  System.out.println("CHUNK_END: " + _chunkDepth + " " + _chunkEnd + " " + getOffset());
	  
	  int delta = _chunkEnd - getOffset();
	  _chunkEnd = -1;
	  
	  if (delta > 0) {
	    System.out.println("SKIP: " + delta);
	    skip(delta);
	  }
	  
	  int newChunk = readInt();

	  if (newChunk >= 0)
	    throw new IllegalStateException("expected end of chunk.");
	  
	  System.out.println("NEXT_END: NEW: " + newChunk);

	  _chunkDepth = - (newChunk + 1);

	  if (_chunkDepth > 0) {
	    newChunk = readInt();
	    //System.out.println("REDO:" + newChunk + " D:" + _chunkDepth);
	    _chunkEnd = _offset + newChunk;
	    
	    System.out.println("NEXT_END:" + _chunkEnd + " D:" + _chunkDepth + " OFF:" + _offset + " NEW: " + newChunk);
	  }
	}
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Class readClass()
    throws IOException
  {
    String codebase = (String) read_value(String.class);
    String repId = (String) read_value(String.class);

    //System.out.println("CODE: " + codebase);
    //System.out.println("REP-ID: " + repId);

    if (codebase.startsWith("RMI:")) {
      String temp = repId;
      repId = codebase;
      codebase = temp;
    }

    return loadClass(repId);
  }

  private Class loadClass(String repId)
    throws RuntimeException
  {
    if (! repId.startsWith("RMI:"))
      throw new RuntimeException("unknown RMI: " + repId);

    int p = repId.indexOf(':', 4);
    if (p < 0)
      throw new RuntimeException("unknown RMI: " + repId);

    String className = repId.substring(4, p);
	  
    Class cl = null;

    try {
      Thread thread = Thread.currentThread();
	
      return Class.forName(className, false, thread.getContextClassLoader());
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private String readCodeBase()
  {
    String codeBase = read_string();
    return codeBase;
  }

  public Object read_fault()
  {
    int startOffset = _offset;
    int originalOffset = _rs.getOffset();
      
    String repId = read_string();
        
    // XXX: assume ucs-16
    if (repId.equals("IDL:omg.org/CORBA/WStringValue:1.0"))
      return read_wstring();

    Class cl = null;
    
    if (repId.startsWith("RMI:")) {
      int p = repId.indexOf(':', 4);
      if (p < 0)
        throw new RuntimeException("unknown RMI: " + repId);

      String className = repId.substring(4, p);

      try {
        cl = CauchoSystem.loadClass(className);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
      
      return _valueHandler.readValue(this, _offset, cl, repId, runTime);
    }

    String className = null;

    if (repId.startsWith("IDL:")) {
      String tail = repId.substring(4);
      int p = tail.indexOf(':');
      if (p > 0)
        tail = tail.substring(0, p);

      if (tail.startsWith("omg.org/"))
        tail = "org.omg." + tail.substring("omg.org/".length());

      className = tail.replace('/', '.');
    }
    else
      className = repId;
    
    String handler = className + "Handler";

    Class handlerClass = null;
    try {
      cl = CauchoSystem.loadClass(className);
      handlerClass = CauchoSystem.loadClass(handler);
    } catch (ClassNotFoundException e) {
    }

    if (cl == null) {
      int p = className.lastIndexOf('.');
      className = className.substring(0, p) + "Package" + className.substring(p);
      handler = className + "Helper";

      try {
	cl = CauchoSystem.loadClass(className);
	handlerClass = CauchoSystem.loadClass(handler);
      } catch (ClassNotFoundException e) {
      }
    }

    if (cl != null && handlerClass != null) {
      java.lang.reflect.Method readHelper = null;
        
      try {
	readHelper = handlerClass.getMethod("read", new Class[] {
	  org.omg.CORBA.portable.InputStream.class
	});
      } catch (Exception e) {
      }

      if (readHelper != null) {
	try {
	  _offset = startOffset;
	  _rs.setOffset(originalOffset);
            
	  return readHelper.invoke(null, new Object[] { this });
	} catch (java.lang.reflect.InvocationTargetException e) {
	  e.printStackTrace();
	} catch (Exception e) {
	  throw new RuntimeException(String.valueOf(e));
	}
      }
    }

    return new IOException("unknown fault: " + repId);
  }

  /**
   * Reads a boolean from the input stream.
   */
  public boolean read_boolean()
  {
    try {
      return read() != 0;
    } catch (IOException e) {
      throw new RuntimeException(String.valueOf(e));
    }
  }

  /**
   * Reads an 8-bit char from the input stream.
   */
  public char read_char()
  {
    try {
      return (char) read();
    } catch (IOException e) {
      throw new RuntimeException(String.valueOf(e));
    }
  }

  /**
   * Reads an 16-bit char from the input stream.
   */
  public char read_wchar()
  {
    return (char) read_short();
  }
  
  /**
   * Reads an 16-bit short from the input stream.
   */
  public short read_ushort()
  {
    return read_short();
  }
  
  /**
   * Reads a 32-bit integer from the input stream.
   */
  public int read_long()
  {
    try {
      int v = readInt();

      return v;
    } catch (IOException e) {
      throw new RuntimeException(String.valueOf(e));
    }
  }
  
  /**
   * Reads a 32-bit integer from the input stream.
   */
  public int read_ulong()
  {
    return read_long();
  }
  
  /**
   * Reads a 64-bit integer from the input stream.
   */
  public long read_longlong()
  {
    try {
      return readLong();
    } catch (IOException e) {
      throw new RuntimeException(String.valueOf(e));
    }
  }
  
  /**
   * Reads a 64-bit integer from the input stream.
   */
  public long read_ulonglong()
  {
    return read_longlong();
  }

  /**
   * Reads an 8-bit byte from the input stream.
   */
  public byte read_octet()
  {
    try {
      return (byte) read();
    } catch (IOException e) {
      throw new RuntimeException(String.valueOf(e));
    }
  }
  
  /**
   * Reads a 32-bit float from the input stream.
   */
  public float read_float()
  {
    int v = read_long();

    return Float.intBitsToFloat(v);
  }
  
  /**
   * Reads a 64-bit double from the input stream.
   */
  public double read_double()
  {
    long v = read_longlong();

    return Double.longBitsToDouble(v);
  }


  /**
   * Reads a boolean array from the input stream.
   */
  public void read_boolean_array(boolean []v, int offset, int length)
  {
    for (int i = 0; i < length; i++)
      v[i + offset] = read_boolean();
  }

  /**
   * Reads a char array from the input stream.
   */
  public void read_char_array(char []v, int offset, int length)
  {
    for (int i = 0; i < length; i++)
      v[i + offset] = read_char();
  }

  /**
   * Reads a string from the input stream.
   */
  public String read_string()
  {
    CharBuffer cb = CharBuffer.allocate();

    int len = read_long();
    for (int i = 0; i < len - 1; i++)
      cb.append(read_char());

    read_octet(); // null

    return cb.close();
  }

  /**
   * Reads a string from the input stream.
   */
  public String readString(int len)
  {
    CharBuffer cb = CharBuffer.allocate();

    for (int i = 0; i < len - 1; i++)
      cb.append(read_char());

    read_octet(); // null

    return cb.close();
  }

  /**
   * Reads a wchar array from the input stream.
   */
  public void read_wchar_array(char []v, int offset, int length)
  {
    for (int i = 0; i < length; i++)
      v[i + offset] = read_wchar();
  }

  /**
   * Reads a string from the input stream.
   */
  public String read_wstring()
  {
    return read_wstring(read_long());
  }

  /**
   * Reads a string from the input stream.
   */
  public String read_wstring(int len)
  {
    CharBuffer cb = CharBuffer.allocate();

    if (_minor == 2) {
      for (; len > 1; len -= 2) {
        char ch = read_wchar();
        cb.append(ch);
      }
      
      if (len > 0)
        read_octet();
    }
    else {
      for (int i = 0; i < len - 1; i++) {
        char ch = read_wchar();
        cb.append(ch);
      }
      read_wchar();
    }

    return cb.close();
  }

  /**
   * Reads a byte array from the input stream.
   */
  public void read_octet_array(byte []v, int offset, int length)
  {
    for (int i = 0; i < length; i++)
      v[i + offset] = read_octet();
  }

  /**
   * Reads a short array from the input stream.
   */
  public void read_short_array(short []v, int offset, int length)
  {
    for (int i = 0; i < length; i++)
      v[i + offset] = read_short();
  }

  /**
   * Reads a ushort array from the input stream.
   */
  public void read_ushort_array(short []v, int offset, int length)
  {
    for (int i = 0; i < length; i++)
      v[i + offset] = read_ushort();
  }

  /**
   * Reads an int array from the input stream.
   */
  public void read_long_array(int []v, int offset, int length)
  {
    for (int i = 0; i < length; i++)
      v[i + offset] = read_long();
  }

  /**
   * Reads an int array from the input stream.
   */
  public void read_ulong_array(int []v, int offset, int length)
  {
    for (int i = 0; i < length; i++)
      v[i + offset] = read_ulong();
  }

  /**
   * Reads a long array from the input stream.
   */
  public void read_longlong_array(long []v, int offset, int length)
  {
    for (int i = 0; i < length; i++)
      v[i + offset] = read_longlong();
  }

  /**
   * Reads a long array from the input stream.
   */
  public void read_ulonglong_array(long []v, int offset, int length)
  {
    for (int i = 0; i < length; i++)
      v[i + offset] = read_ulonglong();
  }

  /**
   * Reads a float array from the input stream.
   */
  public void read_float_array(float []v, int offset, int length)
  {
    for (int i = 0; i < length; i++)
      v[i + offset] = read_float();
  }

  /**
   * Reads a double array from the input stream.
   */
  public void read_double_array(double []v, int offset, int length)
  {
    for (int i = 0; i < length; i++)
      v[i + offset] = read_double();
  }

  /**
   * Reads a CORBA object from the input stream.
   */
  public Object read_abstract_interface()
  {
    boolean discriminator = read_boolean();

    if (discriminator) {
      return read_Object();
    }
    else
      return read_value();
  }

  /**
   * Reads a CORBA object from the input stream.
   */
  public org.omg.CORBA.Object read_Object()
  {
    try {
      return new DummyObjectImpl(readIOR());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Reads a CORBA object from the input stream.
   */
  public org.omg.CORBA.TypeCode read_TypeCode()
  {
    try {
      return TypeCodeImpl.read(this);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Reads a CORBA object from the input stream.
   */
  public org.omg.CORBA.Any read_any()
  {
    try {
      TypeCode typeCode = read_TypeCode();

      AnyImpl any = new AnyImpl();

      any.read_value(this, typeCode);

      return any;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Reads a CORBA object from the input stream.
   */
  public Principal read_Principal()
  {
    throw new UnsupportedOperationException();
  }

  public int read_sequence_length()
  {
    int length = read_long();
    
    if (length < 0 || length > 65536)
      throw new RuntimeException("sequence too long:" + length);
    
    return length;
  }

  public void readOctetSequence(ByteBuffer bb)
    throws IOException
  {
    int len = readInt();
    
    if (len > 65536)
      throw new IOException("too large chunk " + len);
    
    bb.ensureCapacity(len);
    _offset += len;
    _rs.readAll(bb.getBuffer(), 0, len);
    bb.setLength(len);
  }

  public byte []readBytes()
    throws IOException
  {
    int len = readInt();

    if (len > 65536)
      throw new IOException("too large chunk " + len);

    _offset += len;
    byte []buf = new byte[len];
    _rs.readAll(buf, 0, len);

    return buf;
  }

  public void readBytes(byte []buf, int off, int len)
    throws IOException
  {
    _offset += len;
    _rs.readAll(buf, off, len);
  }

  public String readString()
    throws IOException
  {
    int len = readInt();

    if (len > 65536)
      throw new IOException("too large chunk " + len);
    
    CharBuffer cb = CharBuffer.allocate();
    cb.clear();
    for (int i = 0; i < len - 1; i++) {
      int ch = read_octet();
      cb.append((char) ch);
    }
    int ch = read_octet();

    return cb.close();
  }

  public void readString(CharBuffer cb)
    throws IOException
  {
    int len = readInt();

    if (len > 65536)
      throw new IOException("too large chunk " + len);
    
    cb.clear();
    for (int i = 0; i < len - 1; i++) {
      int ch = read();
      cb.append((char) ch);
    }
    int ch = read();
  }
  
  /**
   * Reads an 16-bit short from the input stream.
   */
  public short read_short()
  {
    try {
      if ((_offset & 1) == 1) {
        read();
      }
    
      if (_length <= _offset && _hasMoreFragments || _offset == _chunkEnd) {
	handleFragment();
      }

      int ch1 = read();
      int ch2 = read();

      if (ch2 < 0)
        throw new EOFException();

      return (short) (((ch1 & 0xff) << 8) + (ch2 & 0xff));
    } catch (IOException e) {
      throw new RuntimeException(String.valueOf(e));
    }
  }
  
  /**
   * Reads a 32-bit integer from the input stream.
   */
  public int readInt()
    throws IOException
  {
    align4();
    
    if (_length <= _offset && _hasMoreFragments || _offset == _chunkEnd)
      handleFragment();
    
    _offset += 4;

    return readIntRaw();
  }
  
  /**
   * Reads a 32-bit integer from the input stream.
   */
  private int readIntRaw()
    throws IOException
  {
    int ch1 = _rs.read();
    int ch2 = _rs.read();
    int ch3 = _rs.read();
    int ch4 = _rs.read();

    if (ch4 < 0)
      throw new EOFException();

    return (((ch1 & 0xff) << 24) +
            ((ch2 & 0xff) << 16) +
            ((ch3 & 0xff) << 8) +
            ((ch4 & 0xff)));
  }
  
  /**
   * Reads a 64-bit integer from the input stream.
   */
  public long readLong()
    throws IOException
  {
    align8();
    
    _offset += 8;
    
    int ch1 = _rs.read();
    int ch2 = _rs.read();
    int ch3 = _rs.read();
    int ch4 = _rs.read();
    int ch5 = _rs.read();
    int ch6 = _rs.read();
    int ch7 = _rs.read();
    int ch8 = _rs.read();

    if (ch8 < 0)
      throw new EOFException();

    return ((((long) ch1 & 0xffL) << 56) +
            (((long) ch2 & 0xffL) << 48) +
            (((long) ch3 & 0xffL) << 40) +
            (((long) ch4 & 0xffL) << 32) +
            (((long) ch5 & 0xffL) << 24) +
            (((long) ch6 & 0xffL) << 16) +
            (((long) ch7 & 0xffL) << 8) +
            (((long) ch8 & 0xffL)));
  }

  private void align4()
    throws IOException
  {
    int frag = _offset % 4;
    if (frag > 0 && frag < 4) {
      _offset += 4 - frag;
      _rs.skip(4 - frag);
    }

    if (_chunkEnd > 0 && _chunkEnd <= _offset)
      handleChunk();
  }

  private void align8()
    throws IOException
  {
    int frag = _offset % 8;
    if (frag > 0 && frag < 8) {
      _offset += 8 - frag;
      _rs.skip(8 - frag);
    }

    if (_chunkEnd > 0 && _chunkEnd <= _offset)
      handleChunk();
  }

  /**
   * Reads the next byte.
   */
  public int read()
    throws IOException
  {
    if (_offset == _chunkEnd)
      handleChunk();
    else if (_chunkEnd > 0 && _chunkEnd < _offset)
      System.out.println("PAST: " + _offset+ " " + _chunkEnd);
    
    // _offset++;
    
    return readImpl();
  }

  private void handleChunk()
    throws IOException
  {
    System.out.println("HANDLE-CHUNK: " + _offset + " " + _chunkEnd);

    while (_offset % 4 != 0) {
      _offset++;
      readImpl();
    }
      
    int chunkLength = readImpl();

    _chunkEnd = getOffset() + chunkLength;
    
    System.out.println("NEW-LEN: " + chunkLength + " " + _chunkEnd);
  }

  /**
   * Reads the next byte.
   */
  private int readImpl()
    throws IOException
  {
    if (_length <= _offset && _hasMoreFragments)
      handleFragment();
    
    _offset++;
    
    return _rs.read();
  }

  public void completeRead()
    throws IOException
  {
    //System.out.println("OFF: " + _offset);

    while (_offset < _length) {
      _offset++;
      _rs.read();
    }
    
    /*
      while (_offset % 4 != 0) {
      _offset++;

      _rs.read();
    }

    while (_offset % 4 != 0) {
      _offset++;

      _rs.read();
    }

    while (_offset % 8 != 0) {
      _offset++;

      _rs.read();
    }

    */
  }

  private void handleFragment()
    throws IOException
  {
    if (_length < _offset)
      throw new IllegalStateException(L.l("Read {0} past length {1}",
					  "" + _offset, "" + _length));

    /*
    if (_offset == _chunkEnd) {
      while (_offset % 4 != 0) {
	if (_length <= _offset)
	  _length++;
	
	_offset++;
	_chunkEnd++;
	_rs.read();
      }
    }
    */
    
    if (_length <= _offset) {
      //System.out.println("FRAG: CHUNK:" + _chunkEnd + " " + _offset);
      //System.out.println("FRAG-");
      // XXX: IIOP 1.2?

      while (_offset % 8 != 0) {
	_offset++;
	_length++;
	
	if (_chunkEnd > 0)
	  _chunkEnd++;
	//_fragmentOffset++;
	
	int ch = _rs.read();
	//System.out.println("BIT:" + ch);
      }

      int len = _rs.readAll(_header, 0, _header.length);

      //System.out.println("READ:" + len);
      
      if (len != _header.length)
	throw new EOFException("Unexpected length: " + len);

      if (_header[0] != 'G' ||
	  _header[1] != 'I' ||
	  _header[2] != 'O' ||
	  _header[3] != 'P') {

	throw new IOException(L.l("unknown request {0},{1},{2},{3}",
				  "" + _header[0], "" + _header[1],
				  "" + _header[2], "" + _header[3]));
      }

      _major = _header[4];
      _minor = _header[5];

      if (_major != 1)
	throw new IOException("unknown major");

      _flags = _header[6];
      _isBigEndian = (_flags & 1) == 0;
      _hasMoreFragments = (_flags & 2) == 2;
      _type = _header[7];

      _fragmentOffset += 8;
      _offset += 4;
      _length += 4;
      if (_chunkEnd > 0)
	_chunkEnd += 4;
      
      int fragLen = readIntRaw();
      _length += fragLen;

      if (_type != MSG_FRAGMENT)
	throw new IOException(L.l("expected Fragment at {0}", "" + _type));
    
      if (_minor == 2) {
	if (_chunkEnd > 0)
	  _chunkEnd += 4;
	
	int requestId = readInt();
	//System.out.println("id: " + requestId);
      }
    }
  }

  private void skip(int len)
    throws IOException
  {
    if (_length <= _offset && _hasMoreFragments)
      handleFragment();

    _offset += len;

    _rs.skip(len);
  }

  private void writeHexGroup(int count)
  {
    try {
      align4();
      
      byte []v = new byte[16];
      for (int i = 0; i < count; i++) {
	_rs.readAll(v, 0, v.length);
	      
	for (int j = 0; j < v.length; j++) {
	  System.out.print(" ");
	  printHex(v[j]);
	}

	System.out.print(" ");
	for (int j = 0; j < v.length; j++) {
	  printCh(v[j]);
	}
	
	System.out.println();
      }
    } catch (IOException e) {
      throw new RuntimeException();
    }
  }

  private void printHex(int d)
  {
    int ch1 = (d >> 4) & 0xf;
    int ch2 = d & 0xf;

    if (ch1 >= 10)
      System.out.print((char) ('a' + ch1 - 10));
    else
      System.out.print((char) ('0' + ch1));
    
    if (ch2 >= 10)
      System.out.print((char) ('a' + ch2 - 10));
    else
      System.out.print((char) ('0' + ch2));
  }

  private void printCh(int d)
  {
    if (d >= 0x20 && d <= 0x7f)
      System.out.print("" + ((char) d));
    else
      System.out.print(".");
  }

  private String toCh(int d)
  {
    if (d >= 0x20 && d <= 0x7f)
      return "" + (char) d;
    else
      return "" + d;
  }

  private static String toHex(int v)
  {
    CharBuffer cb = CharBuffer.allocate();
    for (int i = 28; i >= 0; i -= 4) {
      int h = (v >> i) & 0xf;

      if (h >= 10)
        cb.append((char) ('a' + h - 10));
      else
        cb.append(h);
    }

    return cb.close();
  }
}
