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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.iiop;

import java.io.*;
import java.net.*;
import java.util.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

public class IOR {
  private static final String RMI_VERSION = ":0000000000000000";
  
  public static final int TAG_INTERNET_IOP = 0;
  public static final int TAG_MULTIPLE_COMPONENTS = 1;

  public static final int TAG_ORB_TYPE = 0;
  public static final int TAG_CODE_SETS = 1;
  public static final int TAG_POLICIES = 2;
  public static final int TAG_ALTERNATE_IIOP_ADDRESS = 3;
  public static final int TAG_ASSOCIATION_OPTIONS = 13;
  public static final int TAG_SEC_NAME = 14;
  public static final int TAG_SPKM_1_SEC_MECH = 15;
  public static final int TAG_SPKM_2_SEC_MECH = 16;
  public static final int TAG_KerberosV5_SEC_MECH = 17;
  public static final int TAG_CSI_ECMA_Secret_SEC_MECH = 18;
  public static final int TAG_CSI_ECMA_Hybrid_SEC_MECH = 19;
  public static final int TAG_SSL_SEC_TRANS = 20;
  public static final int TAG_ECMA_Public_SEC_MECH = 21;
  public static final int TAG_GENERIC_SEC_MECH = 22;
  public static final int TAG_JAVA_CODEBASE = 25;

  // ftp://ftp.opengroup.org/pub/code_set_registry/code_set_registry1.2g.txt
  public static final int CS_ISO8859_1 = 0x10020;
  public static final int CS_UTF16 = 0x10100; // ucs-16 level 1
  
  String _typeId;
  int major;
  int minor;
  String _host;
  int port;
  byte []oid;
  String uri;

  byte []bytes;

  /**
   * Null constructor for reading.
   */
  public IOR()
  {
  }

  /**
   * Null constructor for writing.
   */
  public IOR(Class type, String host, int port, String uri)
  {
    this("RMI:" + type.getName() + RMI_VERSION, host, port, uri);
  }
  
  /**
   * Null constructor for writing.
   */
  public IOR(String typeId, String host, int port, String uri)
  {
    try {
      _typeId = typeId;
      this.major = 1;
      this.minor = 2;
      _host = host;
      this.port = port;
      this.uri = uri;

      oid = uri.getBytes("UTF8");
    } catch (UnsupportedEncodingException e) {
    }
  }

  /**
   * Returns the type identifier.  This is the java class.
   */
  public String getTypeId()
  {
    return _typeId;
  }

  /**
   * Returns the IIOP major number (1)
   */
  public int getMajor()
  {
    return major;
  }

  /**
   * Returns the IIOP minor number (2)
   */
  public int getMinor()
  {
    return minor;
  }

  public void setMinor(int minor)
  {
    this.minor = minor;
  }

  /**
   * Returns the host
   */
  public String getHost()
  {
    return _host;
  }

  /**
   * Returns the port
   */
  public int getPort()
  {
    return port;
  }

  /**
   * returns the oid
   */
  public byte []getOid()
  {
    return oid;
  }

  /**
   * Returns the object's URI
   */
  public String getURI()
  {
    if (uri == null) {
      if (oid == null)
        return null;
      
      try {
        uri = new String(oid, 0, oid.length, "UTF8");
      } catch (UnsupportedEncodingException e) {
      }
    }

    return uri;
  }
  
  /**
   * Read directly from an IiopReader
   */
  IOR read(IiopReader is)
    throws IOException
  {
    _typeId = is.readString();
    int count = is.readInt();

    for (int i = 0; i < count; i++) {
      int tag = is.readInt();

      if (tag != TAG_INTERNET_IOP)
        throw new RuntimeException("unsupported iop " + tag);

      int sublen = is.readInt();

      int topEndian = is.read();
      major = is.read();
      minor = is.read();

      _host = is.readString();
      port = is.read_short() & 0xffff;

      oid = is.readBytes();

      uri = null;

      if (minor >= 1) {
        int tagCount = is.readInt();
        for (int j = 0; j < tagCount; j++) {
          int compType = is.readInt();

          if (compType == TAG_CODE_SETS) {
            int len = is.readInt();
            int endian = is.readInt();
            int charCode = is.readInt();
            sublen = is.readInt();
            for (int k = 0; k < sublen; k++)
              is.readInt();
            
            int wcharCode = is.readInt();
            sublen = is.readInt();
            for (int k = 0; k < sublen; k++)
              is.readInt();
          }
          else {
            byte []bytes = is.readBytes();
          }
        }
      }
    }

    if (count == 0)
      return null;
    else
      return this;
  }

  /**
   * Read from a byte array.
   */
  public void readByteArray(byte []buf, int offset, int length)
  {
    int i = 0;

    int strlen = getInt(buf, offset + i);
    i += 4;
    
    _typeId = getString(buf, offset + i, strlen);
    i += strlen;
    
    i += (4 - i % 4) % 4;
    int len = getInt(buf, offset + i);
    i += 4;

    for (int k = 0; k < len; k++) {
      int tag = getInt(buf, offset + i);
      i += 4;
      
      if (tag != TAG_INTERNET_IOP)
        throw new RuntimeException("unsupported iop " + tag);

      int sublen = getInt(buf, offset + i);
      i += 4;

      major = buf[offset + i++] & 0xff;
      minor = buf[offset + i++] & 0xff;

      i += 2;

      int startOff = offset;
      strlen = getInt(buf, offset + i);
      i += 4;

      _host = getString(buf, offset + i, strlen);
      i += strlen;
      
      i += i & 1;
      port = getShort(buf, offset + i);
      i += 2;

      i += (4 - i % 4) % 4;
      strlen = getInt(buf, offset + i);
      i += 4;
      
      uri = null;
      oid = new byte[strlen];
      for (int j = 0; j < strlen; j++)
        oid[j] = buf[offset + i + j];
      
      i += strlen;
      i += (4 - i % 4) % 4;
    }
  }

  /**
   * Read an integer from the byte array.  This assumes big endian.
   */
  private static int getInt(byte []buf, int offset)
  {
    return (((buf[offset] & 0xff) << 24) +
            ((buf[offset + 1] & 0xff) << 16) +
            ((buf[offset + 2] & 0xff) << 8) +
            (buf[offset + 3] & 0xff));
  }

  /**
   * Read an integer from the byte array.  This assumes big endian.
   */
  private static int getShort(byte []buf, int offset)
  {
    return (((buf[offset] & 0xff) << 8) +
            ((buf[offset + 1] & 0xff)));
  }

  /**
   * Reads a string from the byte array.
   */
  private static String getString(byte []buf, int offset, int len)
  {
    CharBuffer cb = CharBuffer.allocate();

    for (int i = 0; i < len - 1; i++)
      cb.append((char) buf[offset + i]);

    return cb.close();
  }

  /**
   * Read from a byte array.
   */
  public byte []getByteArray()
  {
    if (bytes != null)
      return bytes;

    ByteBuffer bb = new ByteBuffer();

    writeString(bb, _typeId);
    
    align4(bb);
    bb.addInt(1);
    
    bb.addInt(TAG_INTERNET_IOP);
    int offset = bb.size();
    bb.addInt(0);

    bb.add(0); // encoding
    bb.add(major);
    bb.add(minor);

    writeString(bb, _host);

    if ((bb.size() & 0x1) == 1)
      bb.add(0);
    bb.addShort(port);

    align4(bb);
    bb.addInt(oid.length);
    for (int i = 0; i < oid.length; i++)
      bb.add(oid[i]);

    if (minor >= 1) {
      align4(bb);
      bb.addInt(1); // tagged profiles

      bb.addInt(TAG_CODE_SETS);
      bb.addInt(20);      // length
      bb.addInt(0);       // endian
      bb.addInt(CS_ISO8859_1); // codeset - ISO-8859-1
      bb.addInt(0);       // no alt codesets
      bb.addInt(CS_UTF16); // codeset - UTF-16
      bb.addInt(0);       // no alt codesets
    }
    
    bb.setInt(offset, bb.size() - offset - 4);

    bytes = bb.getByteArray();

    return bytes;
  }

  private void writeString(ByteBuffer bb, String str)
  {
    align4(bb);
    bb.addInt(str.length() + 1);
    for (int i = 0; i < str.length(); i++)
      bb.add(str.charAt(i));
    bb.add(0);
  }

  private void align4(ByteBuffer bb)
  {
    int len = bb.getLength();
    int delta = (4 - len % 4) % 4;
    for (int i = 0; i < delta; i++)
      bb.add(0);
  }

  /**
   * Writes the canonical IOR representation.
   */
  public String toString()
  {
    return "IOR:" + _typeId + "//" + _host + ":" + port + "/" + bytesToHex(oid);
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

  private static String toStr(int v)
  {
    CharBuffer cb = CharBuffer.allocate();
    for (int i = 24; i >= 0; i -= 8) {
      int ch = (v >> i) & 0xff;
      
      if (ch >= 0x20 && ch < 0x7f)
        cb.append((char) ch);
      else
        break;
    }

    return cb.close();
  }
  /**
   * Convert a byte array to hex.
   */
  private String bytesToHex(byte []bytes)
  {
    if (bytes == null)
      return "null";
    
    CharBuffer cb = CharBuffer.allocate();

    for (int i = 0; i < bytes.length; i++) {
      int ch1 = (bytes[i] >> 4) & 0xf;
      int ch2 = bytes[i] & 0xf;

      if (ch1 < 10)
        cb.append((char) (ch1 + '0'));
      else
        cb.append((char) (ch1 + 'a' - 10));
      
      if (ch2 < 10)
        cb.append((char) (ch2 + '0'));
      else
        cb.append((char) (ch2 + 'a' - 10));
    }

    return cb.close();
  }
}
