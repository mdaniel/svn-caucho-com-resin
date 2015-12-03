/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.memcached;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import com.caucho.distcache.ClusterCache;
import com.caucho.distcache.ExtCacheEntry;
import com.caucho.network.listen.AbstractProtocolConnection;
import com.caucho.network.listen.SocketLink;
import com.caucho.util.CharBuffer;
import com.caucho.util.CurrentTime;
import com.caucho.util.HashKey;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempStream;
import com.caucho.vfs.WriteStream;

/**
 * Custom serialization for the cache
 */
public class MemcachedConnection extends AbstractProtocolConnection
{
  private static final HashMap<CharBuffer,Command> _commandMap
    = new HashMap<CharBuffer,Command>();
  
  private MemcachedProtocol _memcache;
  private ClusterCache _cache;
  private SocketLink _link;
  private CharBuffer _method = new CharBuffer();
  private SetInputStream _setInputStream = new SetInputStream();
  private GetOutputStream _getOutputStream = new GetOutputStream();
  private StringBuilder _sb = new StringBuilder();
  
  MemcachedConnection(MemcachedProtocol memcache, SocketLink link)
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
    
    while (handleSingleRequest(is)) {
      if (is.getBufferAvailable() <= 0) {
        return true;
      }
    }
    
    return false;
  }
  
  private boolean handleSingleRequest(ReadStream is)
    throws IOException
  {
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
      WriteStream out = getWriteStream();
      
      out.print("ERROR\r\n");

      return true;
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
  
  private static long getCasKey(HashKey valueKey)
  {
    if (valueKey == null)
      return 0;
    
    byte []valueHash = valueKey.getHash();
    
    return (((valueHash[0] & 0x7fL) << 56)
        | ((valueHash[1] & 0xffL) << 48)
        | ((valueHash[2] & 0xffL) << 40)
        | ((valueHash[3] & 0xffL) << 32)
        | ((valueHash[4] & 0xffL) << 24)
        | ((valueHash[5] & 0xffL) << 16)
        | ((valueHash[6] & 0xffL) << 8)
        | ((valueHash[7] & 0xffL)));
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
    abstract public boolean execute(MemcachedConnection conn)
      throws IOException;
    
    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[]";
    }
  }
  
  static abstract class StoreCommand extends Command {
    @Override
    public boolean execute(MemcachedConnection conn)
      throws IOException
    {
      ReadStream rs = conn.getReadStream();
      WriteStream out = conn.getWriteStream();
      
      StringBuilder sb = new StringBuilder();
      
      int ch;
      
      while ((ch = rs.read()) >= 0 && ch == ' ') {
      }
      
      if (ch < 0)
        return false;
      
      do {
        sb.append((char) ch);
      } while ((ch = rs.read()) >= 0 && ch != ' ');
      
      String key = sb.toString();
      
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
      
      for (; ch >= 0 && ch == ' '; ch = rs.read()) {
      }
      
      sb.setLength(0);
      
      for (; ch >= 0 && ch != '\r'; ch = rs.read()) {
        sb.append((char) ch);
      }
      
      boolean isNoReply = sb.length() > 0 && "noreply".equals(sb.toString());
      
      ch = rs.read();
      if (ch != '\n') {
        throw new IOException("PROTOCOL: " + ch);
      }
      
      long timeout = 60 * 1000;
      
      if (expTime <= 0) {
        timeout = 365 * 24 * 60 * 60 * 1000L;
      }
      else if (expTime <= 60 * 60 * 24 * 30) {
        timeout = 1000L * expTime;
      }
      else {
        timeout = expTime * 1000L - CurrentTime.getCurrentTime();
      }

      boolean isStored = doCommand(conn, key, bytes, timeout, flags);
      
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

      if (isNoReply) {
        
      }
      else if (isStored) {
        out.print("STORED\r\n");
      }
      else {
        out.print("NOT_STORED\r\n");
      }
      
      return true;
    }
    
    abstract protected boolean doCommand(MemcachedConnection conn,
                                         String key,
                                         long bytes,
                                         long timeout,
                                         int flags)
      throws IOException;
  }
  
  static class SetCommand extends StoreCommand {
    @Override
    public boolean doCommand(MemcachedConnection conn,
                             String key,
                             long bytes,
                             long expireTimeout,
                             int flags)
      throws IOException
    {
      ReadStream rs = conn.getReadStream();
      
      SetInputStream setIs = conn.getSetInputStream();

      setIs.init(rs, bytes);
      
      ClusterCache cache = conn.getCache();

      cache.put(key, setIs, expireTimeout, expireTimeout, flags);
      
      // ExtCacheEntry entry = cache.peekExtCacheEntry(key);

      return true;
    }
  }
  
  static class AddCommand extends StoreCommand {
    @Override
    public boolean doCommand(MemcachedConnection conn,
                             String key,
                             long bytes,
                             long timeout,
                             int flags)
      throws IOException
    {
      ClusterCache cache = conn.getCache();
      
      ExtCacheEntry entry = cache.getExtCacheEntry(key);
      
      ReadStream rs = conn.getReadStream();
      
      if (entry != null && ! entry.isValueNull()) {
        rs.skip(bytes);
        
        return false;
      }
      
      
      SetInputStream setIs = conn.getSetInputStream();

      setIs.init(rs, bytes);
      
      
      cache.put(key, setIs, timeout, flags);
      
      WriteStream out = conn.getWriteStream();
      out.setDisableClose(true);

      return true;
    }
  }
  
  static class ReplaceCommand extends StoreCommand {
    @Override
    public boolean doCommand(MemcachedConnection conn,
                             String key,
                             long bytes,
                             long timeout,
                             int flags)
      throws IOException
    {
      ClusterCache cache = conn.getCache();
      
      ExtCacheEntry entry = cache.getExtCacheEntry(key);
      
      ReadStream rs = conn.getReadStream();
      
      if (entry == null || entry.isValueNull()) {
        rs.skip(bytes);
        
        return false;
      }
      
      
      SetInputStream setIs = conn.getSetInputStream();

      setIs.init(rs, bytes);
      
      
      cache.put(key, setIs, timeout, flags);
      
      WriteStream out = conn.getWriteStream();
      out.setDisableClose(true);

      return true;
    }
  }
  
  static class AppendCommand extends StoreCommand {
    @Override
    public boolean doCommand(MemcachedConnection conn,
                             String key,
                             long bytes,
                             long timeout,
                             int flags)
      throws IOException
    {
      ClusterCache cache = conn.getCache();
      
      ExtCacheEntry entry = cache.getExtCacheEntry(key);
      
      ReadStream rs = conn.getReadStream();
      
      if (entry == null || entry.isValueNull()) {
        rs.skip(bytes);
        
        return false;
      }
      
      TempStream ts = new TempStream();
      
      WriteStream os = new WriteStream(ts);
      os.setDisableClose(true);
      
      cache.get(key, os);
      
      SetInputStream setIs = conn.getSetInputStream();

      setIs.init(rs, bytes);
      
      os.writeStream(setIs);
      os.setDisableClose(false);
      os.close();
      
      cache.put(key, 
                ts.openRead(), 
                entry.getAccessedExpireTimeout(), 
                entry.getUserFlags());

      return true;
    }
  }
  
  static class PrependCommand extends StoreCommand {
    @Override
    public boolean doCommand(MemcachedConnection conn,
                             String key,
                             long bytes,
                             long timeout,
                             int flags)
      throws IOException
    {
      ClusterCache cache = conn.getCache();
      
      ExtCacheEntry entry = cache.getExtCacheEntry(key);
      
      ReadStream rs = conn.getReadStream();
      
      if (entry == null || entry.isValueNull()) {
        rs.skip(bytes);
        
        return false;
      }
      
      TempStream ts = new TempStream();
      
      WriteStream os = new WriteStream(ts);
      os.setDisableClose(true);
      
      SetInputStream setIs = conn.getSetInputStream();

      setIs.init(rs, bytes);
      
      os.writeStream(setIs);
      
      cache.get(key, os);
      
      os.setDisableClose(false);
      os.close();
      
      cache.put(key, 
                ts.openRead(), 
                entry.getAccessedExpireTimeout(), 
                entry.getUserFlags());

      return true;
    }
  }
  
  static class GetCommand extends Command {
    @Override
    public boolean execute(MemcachedConnection conn)
      throws IOException
    {
      ReadStream rs = conn.getReadStream();
      WriteStream out = conn.getWriteStream();
      out.setDisableClose(true);
      
      StringBuilder cb = conn._sb;
      cb.setLength(0);
      
      while (readKey(rs, cb)) {
        getCache(out, conn.getCache(), cb.toString(), conn, 0);
      }

      int ch = rs.read();
      for (; ch >= 0 && ch != '\r' && ch != '\n'; ch = rs.read()) {
      }
      
      if (ch == '\r') {
        ch = rs.read();
        if (ch != '\n') {
          System.out.println("PROTOL: " + ch);
          throw new IOException("PROTOCOL: " + ch);
        }
      }

      out.print("END\r\n");
      // out.flush();
      
      return true;
    }
    
    private boolean readKey(ReadStream rs, StringBuilder cb)
      throws IOException
    {
      cb.setLength(0);
      int ch;
      
      while ((ch = rs.read()) >= 0 && ch == ' ') {
      }
      
      if (ch < 0 || ch == '\r' || ch == '\n') {
        rs.unread();
        
        return false;
      }
      
      do {
        cb.append((char) ch);
      } while ((ch = rs.read()) >= 0
               && (ch != ' ' && ch != '\r' && ch != '\n'));
      
      rs.unread();

      
      return ch >= 0;
    }

    protected void getCache(WriteStream out,
                            ClusterCache cache,
                            String key,
                            MemcachedConnection conn,
                            long hash)
      throws IOException
    {
      ExtCacheEntry entry = cache.getLiveCacheEntry(key);

      if (entry == null) {
        return;
      }
      
      long valueHash = entry.getValueHash();
      
      if (valueHash == 0) {
        return;
      }
      
      long now = CurrentTime.getCurrentTime();
      
      if (entry.isExpired(now)) {
        System.out.println("EXP: " + key.length());
        return;
      }
      
      // HashKey valueKey = entry.getValueHashKey();
      long unique = valueHash;
      
      if (hash != 0 && hash == unique) {
        // out.print("NOT_MODIFIED\r\n");
        // get-if-modified
        return;
      }
      
      out.print("VALUE ");
      out.print(key);
      out.print(" ");
      int flags = entry.getUserFlags();
      out.print(flags);
      long bytes = entry.getValueLength();
      out.print(" ");
      out.print(bytes);
      /*
      out.print(" ");
      out.print(unique);
      */
      out.print("\r\n");

      // cache.loadData(valueKey, out);
      if (! entry.readData(out, cache.getConfig())) {
        System.out.println("FAILED_WRITE:");
      }

      out.print("\r\n");
    }
  }
  
  static class GetIfModifiedCommand extends GetCommand {
    @Override
    public boolean execute(MemcachedConnection conn)
      throws IOException
    {
      ReadStream rs = conn.getReadStream();
      WriteStream out = conn.getWriteStream();
      out.setDisableClose(true);
      
      StringBuilder sb = new StringBuilder();
      
      int ch = 0;
      
      for (ch = rs.read(); ch >= 0 && ch == ' '; ch = rs.read()) {
      }
      
      for (; ch >= 0 && ch != ' ' && ch != '\n'; ch = rs.read()) {
        sb.append((char) ch);
      }
      
      for (; ch == ' '; ch = rs.read()) {
      }
      
      long hash = 0;
      
      for (; '0' <= ch && ch <= '9'; ch = rs.read()) {
        hash = 10 * hash + ch - '0';
      }
      
      getCache(out, conn.getCache(), sb.toString(), conn, hash);

      for (; ch >= 0 && ch != '\r' && ch != '\n'; ch = rs.read()) {
      }
      
      if (ch == '\r') {
        ch = rs.read();
        if (ch != '\n') {
          System.out.println("PROTOL: " + ch);
          throw new IOException("PROTOCOL: " + ch);
        }
      }

      out.print("END\r\n");
      out.flush();
      
      return true;
    }
  }
  
  static class DeleteCommand extends Command {
    @Override
    public boolean execute(MemcachedConnection conn)
      throws IOException
    {
      ReadStream rs = conn.getReadStream();
      WriteStream out = conn.getWriteStream();
      out.setDisableClose(true);
      
      boolean isNoReply = false;
      
      CharBuffer cb = new CharBuffer();
      
      int ch = 0;
      
      for (ch = rs.read(); ch >= 0 && ch == ' '; ch = rs.read()) {
      }
      
      for (; ch >= 0 && ch != ' ' && ch != '\n'; ch = rs.read()) {
        cb.append((char) ch);
      }
      
      String key = cb.toString();
      
      for (; ch == ' '; ch = rs.read()) {
      }
      
      long time = 0;
      
      for (; '0' <= ch && ch <= '9'; ch = rs.read()) {
        time = 10 * time + ch - '0';
      }
      
      for (; ch == ' '; ch = rs.read()) {
      }
      
      cb.clear();
      for (; ch >= 0 && ch != ' ' && ch != '\r' && ch != '\n'; ch = rs.read()) {
        cb.append((char) ch);
      }
      
      if (cb.length() > 0 && cb.matches("noreply"))
        isNoReply = true;

      for (; ch >= 0 && ch != '\r' && ch != '\n'; ch = rs.read()) {
      }
      
      if (ch == '\r') {
        ch = rs.read();
        if (ch != '\n') {
          System.out.println("PROTOL: " + ch);
          throw new IOException("PROTOCOL: " + ch);
        }
      }
      
      if (deleteCache(conn.getCache(), time, key)) {
        if (! isNoReply)
          out.print("DELETED\r\n");
      }
      else {
        if (! isNoReply)
          out.print("NOT_FOUND\r\n");
      }

      out.flush();
      
      return true;
    }
    
    protected boolean deleteCache(ClusterCache cache,
                                  long time,
                                  String key)
      throws IOException
    {
      ExtCacheEntry entry = cache.getExtCacheEntry(key);
      
      boolean isValue = (entry != null && ! entry.isValueNull());

      cache.remove(key);
      
      return isValue;
    }
  }
  
  static class IncrementCommand extends Command {
    @Override
    public boolean execute(MemcachedConnection conn)
      throws IOException
    {
      ReadStream rs = conn.getReadStream();
      WriteStream out = conn.getWriteStream();
      out.setDisableClose(true);
      
      boolean isNoReply = false;
      
      CharBuffer cb = new CharBuffer();
      
      int ch = 0;
      
      for (ch = rs.read(); ch >= 0 && ch == ' '; ch = rs.read()) {
      }
      
      for (; ch >= 0 && ch != ' ' && ch != '\n'; ch = rs.read()) {
        cb.append((char) ch);
      }
      
      String key = cb.toString();
      
      for (; ch == ' '; ch = rs.read()) {
      }
      
      long delta = 0;
      
      for (; '0' <= ch && ch <= '9'; ch = rs.read()) {
        delta = 10 * delta + ch - '0';
      }
      
      for (; ch == ' '; ch = rs.read()) {
      }
      
      cb.clear();
      for (; ch >= 0 && ch != ' ' && ch != '\r' && ch != '\n'; ch = rs.read()) {
        cb.append((char) ch);
      }
      
      if (cb.length() > 0 && cb.matches("noreply"))
        isNoReply = true;

      for (; ch >= 0 && ch != '\r' && ch != '\n'; ch = rs.read()) {
      }
      
      if (ch == '\r') {
        ch = rs.read();
        if (ch != '\n') {
          System.out.println("PROTOL: " + ch);
          throw new IOException("PROTOCOL: " + ch);
        }
      }
      
      long value = changeCache(conn.getCache(), key, delta);

      if (isNoReply) {
        
      }
      else if (value == Long.MIN_VALUE) { 
        out.print("NOT_FOUND\r\n");
      }
      else {
        out.print("VALUE " + value + "\r\n");
      }
      
      return true;
    }
    
    protected long changeCache(ClusterCache cache,
                               String key,
                               long delta)
      throws IOException
    {
      return incrementCache(cache, key, delta);
    }
    
    protected long incrementCache(ClusterCache cache,
                                  String key,
                                  long delta)
        throws IOException
      {
      ExtCacheEntry entry = cache.getExtCacheEntry(key);
      
      if (entry == null || entry.isValueNull())
        return Long.MIN_VALUE;
      
      CounterStream os = new CounterStream();
      
      cache.get(key, os);
      
      long newValue = os.getValue() + delta;
      
      byte []values = String.valueOf(newValue).getBytes();
      
      ByteArrayInputStream bis = new ByteArrayInputStream(values);
      
      cache.put(key, bis, 
                entry.getAccessedExpireTimeout(),
                entry.getModifiedExpireTimeout());
      
      return newValue;
    }
  }
  
  static class DecrementCommand extends IncrementCommand {
    protected long changeCache(ClusterCache cache,
                               String key,
                               long delta)
      throws IOException
    {
      return incrementCache(cache, key, -delta);
    }
  }

  static class QuitCommand extends Command {
    @Override
    public boolean execute(MemcachedConnection conn)
      throws IOException
    {
      WriteStream out = conn.getWriteStream();
      
      return false;
    }
  }
  
  static class VersionCommand extends Command {
    @Override
    public boolean execute(MemcachedConnection conn)
      throws IOException
    {
      ReadStream rs = conn.getReadStream();
      
      int ch;
      
      while ((ch = rs.read()) >= 0 && ch != '\n') {
      }
      
      WriteStream out = conn.getWriteStream();
      
      out.print("VERSION 1.4.0\r\n");
      
      return true;
    }
  }
  
  static class VerbosityCommand extends Command {
    @Override
    public boolean execute(MemcachedConnection conn)
      throws IOException
    {
      ReadStream rs = conn.getReadStream();
      
      int ch;
      
      while ((ch = rs.read()) >= 0 && ch != '\n') {
      }
      
      WriteStream out = conn.getWriteStream();
      
      out.print("OK\r\n");
      
      return true;
    }
  }
  
  static class StatsCommand extends Command {
    @Override
    public boolean execute(MemcachedConnection conn)
      throws IOException
    {
      ReadStream rs = conn.getReadStream();
      
      int ch;
      
      StringBuilder sb = new StringBuilder();
      
      for (ch = rs.read(); ch >= 0 && ch == ' '; ch = rs.read()) {
      }
      
      for(; ch >= 0 && ch != '\n' && ch != '\r' && ch != ' '; ch = rs.read()) {
        sb.append((char) ch);
      }
      
      for (; ch >= 0 && ch != '\n'; ch = rs.read()) {
      }
      
      WriteStream out = conn.getWriteStream();
      
      String key = sb.toString();
      
      if ("".equals(key)) {
        out.print("END\r\n");
      }
      else if ("resin".equals(key)) {
        printResinStats(out);
        out.print("END\r\n");
      }
      else {
        out.print("ERROR\r\n");
      }
      
      return true;
    }
    
    private void printResinStats(WriteStream out)
      throws IOException
    {
      out.print("STAT enable_get_if_modified 1\r\n");
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
  
  static class CounterStream extends OutputStream {
    private int _sign = 1;
    private long _value;
    
    public void write(int ch)
    {
      if (ch == '-')
        _sign = -1;
      
      if ('0' <= ch && ch <= '9')
        _value = 10 * _value + ch - '0';
    }
    
    public long getValue()
    {
      return _sign * _value;
    }
    
    public void flush() {}
    public void close() {}
  }
  
  static {
    addCommand("add", new AddCommand());
    addCommand("append", new AppendCommand());
    addCommand("get", new GetCommand());
    addCommand("gets", new GetCommand());
    addCommand("get_if_modified", new GetIfModifiedCommand());
    addCommand("decr", new DecrementCommand());
    addCommand("delete", new DeleteCommand());
    addCommand("incr", new IncrementCommand());
    addCommand("prepend", new PrependCommand());
    addCommand("quit", new QuitCommand());
    addCommand("replace", new ReplaceCommand());
    addCommand("set", new SetCommand());
    addCommand("stats", new StatsCommand());
    addCommand("version", new VersionCommand());
    addCommand("verbosity", new VerbosityCommand());
  }
}
