/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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

package com.caucho.distcache.memcache;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import com.caucho.distcache.ClusterCache;
import com.caucho.distcache.ExtCacheEntry;
import com.caucho.network.listen.ProtocolConnection;
import com.caucho.network.listen.SocketLink;
import com.caucho.util.CharBuffer;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

/**
 * Custom serialization for the cache
 */
public class MemcacheConnection implements ProtocolConnection
{
  private static final HashMap<CharBuffer,Command> _commandMap
    = new HashMap<CharBuffer,Command>();
  
  private MemcacheProtocol _memcache;
  private ClusterCache _cache;
  private SocketLink _link;
  private CharBuffer _method = new CharBuffer();
  private SetInputStream _setInputStream = new SetInputStream();
  private GetOutputStream _getOutputStream = new GetOutputStream();
  
  MemcacheConnection(MemcacheProtocol memcache, SocketLink link)
  {
    _memcache = memcache;
    _link = link;
    
    _cache = memcache.getCache();
  }
  
  @Override
  public String getProtocolRequestURL()
  {
    return "memcache:";
  }
  
  @Override
  public void init()
  {
  }
  
  SocketLink getLink()
  {
    return _link;
  }
  
  ReadStream getReadStream()
  {
    return _link.getReadStream();
  }
  
  WriteStream getWriteStream()
  {
    return _link.getWriteStream();
  }
  
  SetInputStream getSetInputStream()
  {
    return _setInputStream;
  }
  
  GetOutputStream getGetOutputStream()
  {
    return _getOutputStream;
  }
  
  ClusterCache getCache()
  {
    return _cache;
  }

  @Override
  public boolean handleRequest() throws IOException
  {
    ReadStream is = _link.getReadStream();
    
    _method.clear();
    
    int ch;
    
    while ((ch = is.read()) >= 0 && Character.isWhitespace(ch)) {
    }
    
    if (ch < 0)
      return false;
    
    do {
      _method.append((char) ch);
    } while ((ch = is.read()) >= 0 && ! Character.isWhitespace(ch));
    
    Command command = _commandMap.get(_method);
    
    if (command == null) {
      System.out.println("unknown command: '" + _method + "' " + command);
      return false;
    }
    
    //return command.execute(this);
    if (! command.execute(this)) {
      return false;
    }
    
    return true;
  }
  
  @Override
  public boolean handleResume() throws IOException
  {
    return false;
  }

  @Override
  public boolean isWaitForRead()
  {
    return false;
  }

  @Override
  public void onCloseConnection()
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void onStartConnection()
  {
  }
  
  static void addCommand(String name, Command command)
  {
    CharBuffer sb = new CharBuffer();
    sb.append(name);
    
    _commandMap.put(sb, command);
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _link + "]"; 
  }
  
  abstract static class Command {
    abstract public boolean execute(MemcacheConnection conn)
      throws IOException;
    
    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[]";
    }
  }
  
  static class SetCommand extends Command {
    @Override
    public boolean execute(MemcacheConnection conn)
      throws IOException
    {
      ReadStream rs = conn.getReadStream();
      
      StringBuilder sb = new StringBuilder();
      
      int ch;
      
      while ((ch = rs.read()) >= 0 && ch == ' ') {
      }
      
      if (ch < 0)
        return false;
      
      do {
        sb.append((char) ch);
      } while ((ch = rs.read()) >= 0 && ch != ' ');
      
      while ((ch = rs.read()) >= 0 && ch == ' ') {
      }
      
      int flags = 0;
      
      for (; '0' <= ch && ch <= '9'; ch = rs.read()) {
        flags = 10 * flags + ch - '0';
      }
      
      long expTime = 0;
      
      for (; ch >= 0 && ch == ' '; ch = rs.read()) {
      }
      
      for (; '0' <= ch && ch <= '9'; ch = rs.read()) {
        expTime = 10 * expTime + ch - '0';
      }
      
      for (; ch >= 0 && ch == ' '; ch = rs.read()) {
      }
      
      long bytes = 0;
      
      for (; '0' <= ch && ch <= '9'; ch = rs.read()) {
        bytes = 10 * bytes + ch - '0';
      }
      
      // XXX: noreply
      
      for (; ch >= 0 && ch != '\r'; ch = rs.read()) {
      }
      
      ch = rs.read();
      if (ch != '\n') {
        throw new IOException("PROTOCOL: " + ch);
      }
      
      SetInputStream setIs = conn.getSetInputStream();

      setIs.init(rs, bytes);
      
      ClusterCache cache = conn.getCache();
      
      long timeout = 60 * 1000;
      
      String key = sb.toString();

      cache.put(key, setIs, timeout);
      
      WriteStream out = conn.getWriteStream();
      out.setDisableClose(true);
      
      ch = rs.read();
      if (ch != '\r') {
        out.println("PROTOCOL_ERROR");
        throw new IOException("PROTOCOL: " + ch);
      }
      
      ch = rs.read();
      if (ch != '\n') {
        out.println("PROTOCOL_ERROR");
        throw new IOException("PROTOCOL: " + ch);
      }

      out.print("STORED\r\n");
      return true;
    }
  }
  
  static class GetCommand extends Command {
    @Override
    public boolean execute(MemcacheConnection conn)
      throws IOException
    {
      ReadStream rs = conn.getReadStream();
      
      CharBuffer cb = new CharBuffer();
      
      int ch;
      
      while ((ch = rs.read()) >= 0 && ch == ' ') {
      }
      
      if (ch < 0) {
        System.out.println("EOF: " + ch);
        return false;
      }
      
      do {
        cb.append((char) ch);
      } while ((ch = rs.read()) >= 0 && ! Character.isWhitespace(ch));
      
      for (; ch >= 0 && ch == ' '; ch = rs.read()) {
      }
      
      // XXX: noreply
      
      for (; ch >= 0 && ch != '\r'; ch = rs.read()) {
      }
      
      ch = rs.read();
      if (ch != '\n') {
        System.out.println("PROTOL: " + ch);
        throw new IOException("PROTOCOL: " + ch);
      }
      
      ClusterCache cache = conn.getCache();
      
      WriteStream out = conn.getWriteStream();
      out.setDisableClose(true);
      
      String key = cb.toString();
      
      ExtCacheEntry entry = cache.getExtCacheEntry(key);
      
      out.print("VALUE ");
      out.print(key);
      out.print(" ");
      int flags = 0;
      out.print(flags);
      long bytes = entry.getValueLength();
      out.print(" ");
      out.print(bytes);
      
      long unique = 0;
      out.print(" ");
      out.print(unique);
      out.print("\r\n");

      GetOutputStream gOut = conn.getGetOutputStream();

      gOut.init(out);
      cache.get(key, gOut);

      out.print("\r\n");
      
      out.print("END\r\n");

      return true;
    }
  }

  static class SetInputStream extends InputStream {
    private ReadStream _is;
    private long _length;
    
    void init(ReadStream is, long length)
    {
      _is = is;
      _length = length;
    }
    
    @Override
    public int read()
      throws IOException
    {
      if (_length <= 0)
        return -1;
      
      _length--;
      
      return _is.read();
    }
    
    @Override
    public int read(byte []buffer, int offset, int length)
      throws IOException
    {
      if (_length <= 0)
        return -1;
      
      int sublen = (int) _length;
      
      if (length < sublen)
        sublen = length;
      
      int readLength = _is.read(buffer, offset, sublen);
      
      if (readLength <= 0)
        return readLength;
      
      _length -= readLength;
      
      return readLength;
    }
  }

  static class GetOutputStream extends OutputStream {
    private WriteStream _os;
    
    void init(WriteStream os)
    {
      _os = os;
    }
    
    @Override
    public final void write(int ch)
      throws IOException
    {
      _os.write(ch);
    }
    
    @Override
    public final void write(byte []buffer, int offset, int length)
      throws IOException
    {
      _os.write(buffer, offset, length);
    }
  }
  
  static {
    addCommand("get", new GetCommand());
    addCommand("set", new SetCommand());
  }
}
