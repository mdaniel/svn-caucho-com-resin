/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;

import org.w3c.dom.Node;

import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.ReadStream;
import com.caucho.v5.vfs.VfsOld;

/**
 * QAbstractNode is an abstract implementation for any DOM node.
 */
public class SerializedXml implements java.io.Externalizable {
  protected static L10N L = new L10N(SerializedXml.class);
  
  private Node _node;

  public SerializedXml()
  {
  }

  public SerializedXml(Node node)
  {
    _node = node;
  }

  public void writeExternal(ObjectOutput out)
    throws IOException
  {
    XmlPrinter printer = new XmlPrinter(new OutputStreamWrapper(out));

    printer.printXml(_node);

    // feff
    out.writeByte(0xef);
    out.writeByte(0xbf);
    out.writeByte(0xbf);
  }

  public void readExternal(ObjectInput in)
    throws IOException
  {
    Xml parser = Xml.create();

    try {
      ReadStream is = VfsOld.openRead(new InputStreamWrapper(in));
      _node = parser.parseDocument(is);
      is.close();
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException(e);
    }

    parser.free();
  }

  private Object readResolve()
  {
    return _node;
  }

  static class InputStreamWrapper extends InputStream {
    private ObjectInput _in;

    InputStreamWrapper(ObjectInput in)
    {
      _in = in;
    }

    public int read()
      throws IOException
    {
      int ch = _in.readByte() & 0xff;
      
      return ch;
    }

    public int read(byte []buffer, int off, int length)
      throws IOException
    {
      buffer[off] = _in.readByte();

      return 1;
    }
  }
  
  static class OutputStreamWrapper extends OutputStream {
    private ObjectOutput _out;

    OutputStreamWrapper(ObjectOutput out)
    {
      _out = out;
    }

    public void write(int ch)
      throws IOException
    {
      _out.write(ch);
    }

    public void write(byte []buf, int off, int len)
      throws IOException
    {
      _out.write(buf, off, len);
    }
  }
}
