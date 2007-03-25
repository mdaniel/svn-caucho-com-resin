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

package com.caucho.iiop;

import com.caucho.iiop.orb.*;
import com.caucho.iiop.any.*;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.ByteBuffer;
import com.caucho.util.CharBuffer;
import com.caucho.util.IntArray;
import com.caucho.util.L10N;
import com.caucho.transaction.*;
import com.caucho.vfs.*;

import org.omg.CORBA.Principal;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.portable.IndirectionException;
import org.omg.SendingContext.RunTime;

import javax.rmi.CORBA.Util;
import javax.rmi.CORBA.ValueHandler;
import java.io.EOFException;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

public class IiopReader extends org.omg.CORBA_2_3.portable.InputStream {
  protected static final L10N L = new L10N(IiopReader.class);
  protected static final Logger log
    = Logger.getLogger(IiopReader.class.getName());
  
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
  public static final int SERVICE_RT_CORBA_PRIORITY = 10;
  public static final int SERVICE_RT_CORBA_PRIORITY_RANGE = 11;
  public static final int SERVICE_FT_GROUP_VERSION = 12;
  public static final int SERVICE_FT_REQUEST = 13;
  public static final int SERVICE_EXCEPTION_DETAIL_MESSAGE = 14;
  public static final int SERVICE_SECURITY_ATTRIBUTE_SERVICE = 15;
  public static final int SERVICE_ACTIVITY_SERVICE = 16;
  public static final int SERVICE_RMI_CUSTOM_MAX_STREAM_FORMAT = 17;

  public static final int STATUS_NO_EXCEPTION = 0;
  public static final int STATUS_USER_EXCEPTION = 1;
  public static final int STATUS_SYSTEM_EXCEPTION = 2;
  public static final int STATUS_LOCATION_FORWARD = 3;
  
  private IiopSocketPool _pool;
  private ReadWritePair _pair;
  private ReadStream _rs;
  private byte []_header = new byte[8];
  private byte []buf = new byte[16];

  private ReaderContext _context;
  /*
  private IntArray _refOffsets = new IntArray();
  private ArrayList<String> _refIds = new ArrayList<String>();
  private ArrayList<Class> _refClasses = new ArrayList<Class>();
  private ArrayList<Serializable> _refValues = new ArrayList<Serializable>();

  private HashMap<Integer,String> _savedStrings = new HashMap<Integer,String>();
  */

  private MessageReader _in;
  
  private int _major;
  private int _minor;
  private boolean _isBigEndian;
  private boolean _hasMoreFragments;
  private int _flags;

  private int _type;
  
  private int _fragmentOffset;
  
  private int _chunkEnd = -1;
  private int _chunkDepth = 0;

  private int requestId;
  private boolean responseExpected;
  private ByteBuffer _objectKey = new ByteBuffer();
  private CharBuffer _operation = new CharBuffer();
  private ByteBuffer principal = new ByteBuffer();

  private XidImpl _xid;

  private char []_cb = new char[256];
  
  private ValueHandler _valueHandler = Util.createValueHandler();
  private RunTime runTime = _valueHandler.getRunTimeCodeBase();

  private Throwable _unknownExn;

  private ORBImpl _orb;

  public IiopReader()
  {
  }

  public IiopReader(ReadStream rs)
  {
    init(rs);
  }

  public IiopReader(IiopSocketPool pool, ReadWritePair pair)
  {
    _pool = pool;
    _pair = pair;
    
    init(pair.getReadStream());
  }

  public void setOrb(ORBImpl orb)
  {
    _orb = orb;
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
    _objectKey.clear();
    _operation.clear();
    _fragmentOffset = 0;

    _context = new ReaderContext();
  }

  public ValueHandler getValueHandler()
  {
    return _valueHandler;
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
    return _in.getOffset();
  }

  public boolean isResponseExpected()
  {
    return responseExpected;
  }

  public ByteBuffer getObjectKey()
  {
    return _objectKey;
  }

  public CharBuffer getOperation()
  {
    return _operation;
  }
  
  public XidImpl getXid()
  {
    return _xid;
  }

  public boolean readRequest()
    throws IOException
  {
    int len = _rs.readAll(_header, 0, _header.length);

    if (len < 0)
      return false;

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
    
    _type = _header[7];

    _in = new InputStreamMessageReader(_rs,
				       ! _hasMoreFragments,
				       0);

    // debug
    //System.out.println("---");
    //writeHexGroup(_buffer, 0, _length);

    if (_minor == 0) {
      switch (_type) {
      case MSG_REQUEST:
        readRequest10();
        break;
      case MSG_REPLY:
        readReply10();
        break;
      case MSG_CLOSE_CONNECTION:
	return false;
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
      case MSG_CLOSE_CONNECTION:
	return false;
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
        readReply12();
        break;
      case MSG_CLOSE_CONNECTION:
	return false;
      case MSG_ERROR:
        throw new RuntimeException("MSG_ERROR: unknown protocol error");
      default:
        throw new RuntimeException("unknown type: " + _type);
      }
    }
    else
      throw new IOException("unknown minor");

    return true;
  }

  private void readRequest10()
    throws IOException
  {
    readServiceContextList();
    requestId = _in.read_long();
    responseExpected = _in.read() != 0;
    
    readOctetSequence(_objectKey);

    readString(_operation);
    
    readOctetSequence(principal);
  }

  private void readReply10()
    throws IOException
  {
    readServiceContextList();

    int requestId = _in.read_long();
    int status = _in.read_long();

    switch (status) {
    case STATUS_NO_EXCEPTION:
      //debugTail();
      return;
        
    case STATUS_SYSTEM_EXCEPTION:
      String exceptionId = readString();
      int minorStatus = _in.read_long();
      int completionStatus = _in.read_long();

      if (_unknownExn instanceof RuntimeException)
	throw (RuntimeException) _unknownExn;
      else if (_unknownExn instanceof Throwable)
	throw new RemoteUserException((Throwable) _unknownExn);
      
      throw new RuntimeException("exception: " + exceptionId);
        
    case STATUS_USER_EXCEPTION:
      String type = read_string();
      Object value = read_value();

      if (value instanceof RuntimeException)
	throw (RuntimeException) value;
      else if (value instanceof Throwable)
	throw new RemoteUserException((Throwable) value);
      
      throw new IOException("user exception: " + value);
        
    default:
      throw new IOException("unknown status: " + status);
    }
  }

  private void readRequest12()
    throws IOException
  {
    requestId = _in.read_long();
    int flags = read_octet();
    responseExpected = flags != 0;

    int disposition = read_long();
    readOctetSequence(_objectKey);
    readString(_operation);

    readServiceContextList();

    // align(8) is required for IIOP 1.2, in combination of the
    // initial offset of 12 to align the data after the 12byte header.
    // ejb/1410 vs ejb/1230, ejb/1141 (??)
    //_in.align(8);
  }

  private void readReply12()
    throws IOException
  {
    int requestId = _in.read_long();
    int status = _in.read_long();
    readServiceContextList();

    switch (status) {
    case STATUS_NO_EXCEPTION:
      //debugTail();
      return;
        
    case STATUS_SYSTEM_EXCEPTION:
      String exceptionId = readString();
      int minorStatus = _in.read_long();
      int completionStatus = _in.read_long();

      if (_unknownExn != null)
	throw new RemoteUserException(_unknownExn);
      
      throw new IOException("exception: " + exceptionId);
        
    case STATUS_USER_EXCEPTION:
      Object value = read_value();

      if (value instanceof RuntimeException)
	throw (RuntimeException) value;

      if (value != null)
	throw new IOException("user exception: " + value + " " + value.getClass().getName());
      else
	throw new IOException("null user exception");
        
    default:
      throw new IOException("unknown status: " + status);
    }
  }

  private void readServiceContextList()
    throws IOException
  {
    _xid = null;
    
    int length = _in.read_long();

    for (int i = 0; i < length; i++) {
      int serviceId = _in.read_long();
      int dataLength = _in.read_long();

      int startOffset = _in.setOffset(0);
      
      if (serviceId == SERVICE_CODE_SET) {
        int endian = _in.read();
        int charSet = _in.read_long();
        int wcharSet = _in.read_long();
      }
      else if (serviceId == SERVICE_UNKNOWN_EXCEPTION_INFO) {
        int endian = _in.read();

	_unknownExn = (Throwable) read_value();
      }
      else if (serviceId == SERVICE_TRANSACTION) {
	int endian = _in.read();

	int timeout = _in.read_long();
	int coord = _in.read_long();
	int term = _in.read_long();
	int format = _in.read_long();
	int bqualLength = _in.read_long();
	int xidLength = _in.read_long();
	byte []local = new byte[bqualLength];
	byte []global = new byte[xidLength - bqualLength];
	_in.read(global, 0, global.length);
	_in.read(local, 0, local.length);
	int parents = _in.read_long();

	XidImpl xid = new XidImpl(global, local);

	_xid = xid;
      }
      else {
        _in.skip(dataLength);
      }

      _in.addOffset(startOffset);
    }
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
    int len = _in.read_long();
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
      _in.align(4);
      int startOffset = _in.getOffset();
      
      int code = read_long();

      String repId = "";
      boolean isChunked = false;
      Serializable value = null;

      if (code == 0)
	return null;
      else if (code == 0xffffffff) {
	_chunkEnd = oldChunkEnd;
	
	int start = _in.getOffset();
	int delta = read_long();
	int target = start + delta;
	
	log.fine("INDIRECT:" + delta);

	value = _context.getRef(target);

	if (value != null)
	  return value;

	throw new IndirectionException(target);
      }
      else if ((code & 0x7fffff00) != 0x7fffff00) {
	repId = readString(code);
      }
      else {
	isChunked = (code & 8) == 8;
	boolean hasCodeBase = (code & 1) == 1;
	int repository = (code & 6);
      
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

      try {
	if (isChunked) {
	  // writeHexGroup(16);

	  int chunkLength = _in.read_long();

	  _chunkEnd = chunkLength + _in.getOffset();
	  _chunkDepth++;
	}

	// XXX: assume ucs-16
	if (repId.equals("IDL:omg.org/CORBA/WStringValue:1.0")) {
	  value = read_wstring();
	}
	else if (! repId.startsWith("RMI:") && ! repId.startsWith("IDL:")) {
	  log.warning("unknown rep: " + repId + " " + Integer.toHexString(code));
	  throw new UnsupportedOperationException("problem parsing repid: '" + repId + "'");
	}
	else {
	  int p = repId.indexOf(':', 4);
	  if (p < 0)
	    throw new RuntimeException("unknown RMI: " + repId);

	  String className;

	  if (repId.startsWith("IDL:omg.org")) {
	    className = "org.omg" + repId.substring(11, p).replace('/', '.');

	    int tail = className.lastIndexOf('.');
	    className = (className.substring(0, tail) + "Package"
			 + className.substring(tail));
	  }
	  else
	    className = repId.substring(4, p);
	  
	  // log.fine("CLASS-NAME: " + className);
	  if (className.equals("javax.rmi.CORBA.ClassDesc")) {
	    return readClass();
	  }
	  else {
	    Class cl = null;

	    try {
	      cl = CauchoSystem.loadClass(className);
	    } catch (ClassNotFoundException e) {
	      e.printStackTrace();
	      throw new RuntimeException(e);
	    }

	    value = _valueHandler.readValue(this, startOffset,
					    cl, repId, runTime);
	  }
	}

	_context.addRef(startOffset, value);

	return value;
      } finally {
	if (_chunkDepth > 0) {
	  _chunkDepth--;

	  int delta = _chunkEnd - _in.getOffset();
	  _chunkEnd = -1;
	  
	  if (delta > 0) {
	    _in.skip(delta);
	  }
	  
	  int newChunk = _in.read_long();

	  if (newChunk >= 0)
	    throw new IllegalStateException(L.l("{0}: expected end of chunk {1}",
					    getOffset(), newChunk));
	  
	  _chunkDepth = - (newChunk + 1);

	  if (_chunkDepth > 0) {
	    newChunk = _in.read_long();
	    //System.out.println("REDO:" + newChunk + " D:" + _chunkDepth);
	    _chunkEnd = _in.getOffset() + newChunk;
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

    log.fine("CODE: " + codebase);
    log.fine("REP-ID: " + repId);

    if (codebase != null && codebase.startsWith("RMI:")) {
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

    if ("javax.rmi.CORBA.ClassDesc".equals(className))
      return Class.class;
	  
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
    int startOffset = _in.getOffset();
    int originalOffset = startOffset;
      
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
      
      return _valueHandler.readValue(this, _in.getOffset(), cl, repId, runTime);
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
	  _in.setOffset(startOffset);
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
    return _in.read() != 0;
  }

  /**
   * Reads an 8-bit char from the input stream.
   */
  public char read_char()
  {
    return (char) _in.read();
  }

  /**
   * Reads an 16-bit char from the input stream.
   */
  public char read_wchar()
  {
    if (_minor == 2) {
      _in.read();
      
      return (char) _in.read_short();
    }
    else
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
    return _in.read_long();
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
    return readLong();
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
    return (byte) _in.read();
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
    int len = read_long();
    int offset = _in.getOffset();

    if (len < 0) {
      // ejb/114o
   
      int delta = read_long();

      String v = _context.getString(offset + delta);

      return v;
    }

    char []cb = _cb;
    if (cb.length < len) {
      _cb = new char[len + (1024 - len % 1024) % 1024];
      cb = _cb;
    }
    for (int i = 0; i < len - 1; i++)
      cb[i] = read_char();

    read_octet(); // null

    String v = new String(cb, 0, len - 1);

    _context.putString(offset, v);

    return v;
  }

  /**
   * Reads a string from the input stream.
   */
  public String readString(int len)
  {
    char []cb = _cb;

    if (cb.length < len) {
      _cb = new char[len + (1024 - len % 1024) % 1024];
      cb = _cb;
    }

    for (int i = 0; i < len - 1; i++)
      cb[i] = read_char();

    read_octet(); // null

    return new String(cb, 0, len - 1);
  }

  /**
   * Reads a wchar array from the input stream.
   */
  public void read_wchar_array(char []v, int offset, int length)
  {
    for (int i = 0; i < length; i++) {
      v[i + offset] = read_wchar();
    }
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
    char []cb = _cb;

    if (_minor >= 2) {
      int sublen = len / 2;
      if (cb.length < sublen) {
	_cb = new char[sublen + (1024 - sublen % 1024) % 1024];
	cb = _cb;
      }

      for (int i = 0; i < sublen; i++) {
        cb[i] = (char) read_short();
      }

      String v = new String(cb, 0, sublen);

      return v;
    }
    else {
      if (cb.length < len) {
	_cb = new char[len + (1024 - len % 1024) % 1024];
	cb = _cb;
      }
      
      for (int i = 0; i < len - 1; i++) {
        cb[i] = (char) read_short();
      }
      read_short();

      return new String(cb, 0, len - 1);
    }
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
      v[i + offset] = _in.read_long();
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
    for (int i = 0; i < length; i++) {
      v[i + offset] = read_double();
    }
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

  @Override
  public org.omg.CORBA.Object read_Object(Class cl)
  {
    return read_Object();
  }

  /**
   * Reads a CORBA object from the input stream.
   */
  @Override
  public org.omg.CORBA.Object read_Object()
  {
    try {
      IOR ior = readIOR();
      
      if (_orb != null) {
	return new StubImpl(_orb, ior);
      }
      else
	return new DummyObjectImpl(ior);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Reads a CORBA object from the input stream.
   */
  public org.omg.CORBA.TypeCode read_TypeCode()
  {
    int kind = read_long();

    //System.out.println("KIND: " + Integer.toHexString(kind) + " " + kind);

    try {
    switch (kind) {
    case TCKind._tk_null:
      return TypeCodeImpl.TK_NULL;
      
    case TCKind._tk_boolean:
      return BooleanTypeCode.TYPE_CODE;
      
    case TCKind._tk_wchar:
      return WcharTypeCode.TYPE_CODE;
      
    case TCKind._tk_octet:
      return OctetTypeCode.TYPE_CODE;
      
    case TCKind._tk_short:
      return ShortTypeCode.TYPE_CODE;
      
    case TCKind._tk_long:
      return LongTypeCode.TYPE_CODE;
      
    case TCKind._tk_longlong:
      return LongLongTypeCode.TYPE_CODE;
      
    case TCKind._tk_float:
      return FloatTypeCode.TYPE_CODE;
      
    case TCKind._tk_double:
      return DoubleTypeCode.TYPE_CODE;
      
    case TCKind._tk_string:
      {
	TypeCodeImpl typeCode = new TypeCodeImpl(TCKind.tk_string);
	typeCode.setLength(read_ulong());
	
	return typeCode;
      }
      
    case TCKind._tk_wstring:
      {
	TypeCodeImpl typeCode = new TypeCodeImpl(TCKind.tk_wstring);
	typeCode.setLength(read_ulong());
	
	return typeCode;
      }
      
    case TCKind._tk_sequence:
      return SequenceTypeCode.readTypeCode(this);
      
    case TCKind._tk_value:
      return ValueTypeCode.readTypeCode(this);
      
    case TCKind._tk_value_box:
      return ValueBoxTypeCode.readTypeCode(this);
      
    case TCKind._tk_abstract_interface:
      return AbstractInterfaceTypeCode.readTypeCode(this);
	
    default:
      System.out.println("UNKNOWN:" + kind);
      throw new UnsupportedOperationException("unknown typecode kind: " + kind);
    }
    } finally {
      //System.out.println("DONE:" + kind);
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
    int len = _in.read_long();
    
    if (len > 65536)
      throw new IOException("too large chunk " + len);
    
    bb.ensureCapacity(len);
    _in.read(bb.getBuffer(), 0, len);
    bb.setLength(len);
  }

  public String readString()
  {
    int len = _in.read_long();

    if (len < 1 || len > 65536)
      throw new IllegalStateException("string length problems: " + len);
    
    char []cb = _cb;

    if (cb.length < len) {
      _cb = new char[len + (1024 - len % 1024) % 1024];
      cb = _cb;
    }

    for (int i = 0; i < len - 1; i++) {
      cb[i] = (char) read_octet();
    }
    int ch = read_octet();

    return new String(cb, 0, len - 1);
  }

  public void readString(CharBuffer cb)
    throws IOException
  {
    int len = _in.read_long();

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
    return (short) _in.read_short();
  }
  
  /**
   * Reads a 32-bit integer from the input stream.
   */
  public int readInt()
  {
    return _in.read_long();
  }
  
  /**
   * Reads a 32-bit integer from the input stream.
   */
  public int readInt(byte []buffer, int offset)
  {
    int ch1 = buffer[offset++];
    int ch2 = buffer[offset++];
    int ch3 = buffer[offset++];
    int ch4 = buffer[offset++];

    return (((ch1 & 0xff) << 24) +
            ((ch2 & 0xff) << 16) +
            ((ch3 & 0xff) << 8) +
            ((ch4 & 0xff)));
  }
  
  /**
   * Reads a 64-bit integer from the input stream.
   */
  public long readLong()
  {
    return _in.read_longlong();
  }

  private void align4()
  {
    _in.align(4);
  }

  private void align8()
    throws IOException
  {
    _in.align(8);
  }

  public void alignMethodArgs()
  {
    if (_minor >= 2) {
      _in.align(8);
    }
  }

  /**
   * Reads the next byte.
   */
  public int read()
  {
    return _in.read();
  }

  public byte []readBytes()
    throws IOException
  {
    int len = _in.read_long();

    if (len > 65536)
      throw new IOException("too large chunk " + len);

    byte []buf = new byte[len];
    _in.read(buf, 0, len);

    return buf;
  }

  public void completeRead()
    throws IOException
  {
    // _in.completeRead();
  }

  public void close()
  {
    ReadWritePair pair = _pair;
    _pair = null;

    _rs = null;

    if (pair != null) {
      if (_pool != null) {
	_pool.free(pair);
      }
      else {
	try {
	  pair.getWriteStream().close();
	} catch (IOException e) {
	}
      
	pair.getReadStream().close();
      }
    }
  }

  private void writeHexGroup(byte []buffer, int offset, int length)
  {
    int end = offset + length;
      
    while (offset < end) {
      int chunkLength = 16;
	
      for (int j = 0; j < chunkLength; j++) {
	System.out.print(" ");
	printHex(buffer[offset + j]);
      }

      System.out.print(" ");
      for (int j = 0; j < chunkLength; j++) {
	printCh(buffer[offset + j]);
      }

      offset += chunkLength;
	
      System.out.println();
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
    CharBuffer cb = new CharBuffer();
    for (int i = 28; i >= 0; i -= 4) {
      int h = (v >> i) & 0xf;

      if (h >= 10)
        cb.append((char) ('a' + h - 10));
      else
        cb.append(h);
    }

    return cb.toString();
  }
}
