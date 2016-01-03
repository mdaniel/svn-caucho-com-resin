/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.transaction.xalog;

import com.caucho.v5.transaction.XidImpl;
import com.caucho.v5.util.Alarm;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.LongMap;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.ReadStream;
import com.caucho.v5.vfs.WriteStream;

import java.io.EOFException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements a single log stream.  Each log stream has two associated
 * files in order to switch at the end of the file.
 */
public class XALogStream extends AbstractXALogStream {
  private static L10N L = new L10N(XALogStream.class);
  private static Logger log
    = Logger.getLogger(XALogStream.class.getName());

  public final static int LOG_HEADER_BEGIN = 'L';
  public final static int LOG_HEADER_END   = 'X';
  
  public final static int TM_COMMIT        = 'C';
  public final static int TM_FINISH        = 'F';
  
  public final static int RA_DATA_PART     = 'd';
  public final static int RA_DATA_END      = 'D';

  private XALogManager _manager;

  private PathImpl _pathA;
  private PathImpl _pathB;
  
  private PathImpl _currentPath;

  private long _maxFileSize = 64L * 1024L * 1024L;

  private LongMap<XidImpl> _activeXids = new LongMap<XidImpl>();
  private HashSet<XidImpl> _recoverXids = new HashSet<XidImpl>();
  
  private XidImpl _tempXid = new XidImpl(0, 0, 0);

  private int _currentLog;
  private WriteStream _out;
  
  private long _fileSize;
  private long _recordSequence;
  private long _flushSequence;

  private boolean _isActive;
  
  private boolean _waitForFlush;
  
  private volatile boolean _isFlushing;
  private volatile int _flushCount;

  /**
   * Creates the log stream.
   */
  XALogStream(XALogManager manager, PathImpl path)
    throws IOException
  {
    _manager = manager;

    path.getParent().mkdirs();

    _pathA = path.getParent().lookup(path.getTail() + ".a");
    _pathB = path.getParent().lookup(path.getTail() + ".b");
  }

  /**
   * Starts the stream.
   */
  void start()
    throws IOException
  {
    recover();

    _out = _currentPath.openWrite();

    _fileSize = (1 + 4 + 2 * 8) + (1 + 8);

    long now = CurrentTime.getCurrentTime();
      
    _out.write(LOG_HEADER_BEGIN);
    writeInt(_out, 0);
    writeLong(_out, now);
    writeLong(_out, _fileSize);
    _out.write(LOG_HEADER_END);
    writeLong(_out, now);
    _out.flushToDisk();

    _isActive = true;
  }

  /**
   * Recovers the stream.
   */
  private void recover()
  {
    long aTime = recoverFileHeader(_pathA);
    long bTime = recoverFileHeader(_pathB);

    if (aTime > 0 && (bTime <= aTime)) {
      recoverFile(_pathA);
      _currentPath = _pathB;
    }
    else if (bTime > 0) {
      recoverFile(_pathB);
      _currentPath = _pathA;
    }
    else {
      _currentPath = _pathA;
    }

    _manager.addRecoverXids(_recoverXids);
  }

  /**
   * Recovers the file header.
   */
  private long recoverFileHeader(PathImpl path)
  {
    ReadStream is = null;
    
    try {
      if (! path.canRead())
        return -1;

      is = path.openRead();

      int code = is.read();
      if (code != LOG_HEADER_BEGIN)
        throw new XALogException(L.l("'{0}' is an illegal log start.",
                                     code));

      int headerCount = readInt(is);
      long time = readLong(is);
      long checkpoint = readLong(is);

      for (int i = 0; i < headerCount; i++) {
        readHeader(is);
      }

      code = is.read();
      if (code == LOG_HEADER_END) {
        long endTime = readLong(is);

        if (time == endTime)
          return time;
      }
      
      throw new XALogException(L.l("log header was broken or incomplete."));
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      try {
        if (is != null)
          is.close();
      } catch (Exception e) {
      }
    }

    return -1;
  }

  /**
   * Recovers the file header.
   */
  private long recoverFile(PathImpl path)
  {
    ReadStream is = null;

    _recoverXids.clear();
    
    try {
      if (! path.canRead())
        return -1;

      is = path.openRead();

      int code = is.read();
      if (code != LOG_HEADER_BEGIN)
        throw new XALogException(L.l("'{0}' is an illegal log start.",
                                     code));

      int headerCount = readInt(is);
      long time = readLong(is);
      long checkpoint = readLong(is);

      for (int i = 0; i < headerCount; i++) {
        code = is.read();
        
        readRecord(is, code);
      }

      code = is.read();

      if (code != LOG_HEADER_END)
        return -1;
      else if (time != readLong(is)) {
        return -1;
      }

      while ((code = is.read()) >= 0) {
        readRecord(is, code);
      }

      if (_recoverXids.size() > 0)
        log.info(L.l("recovering {0} transactions", _recoverXids.size()));

      if (log.isLoggable(Level.FINE)) {
        Iterator<XidImpl> keys = _recoverXids.iterator();

        while (keys.hasNext()) {
          log.fine(L.l("recovering {0}", keys.next()));
        }
      }
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      try {
        if (is != null)
          is.close();
      } catch (Exception e) {
      }
    }

    return -1;
  }

  /**
   * Reads a header entry.
   */
  private void readHeader(ReadStream is)
    throws IOException
  {
    int code = is.read();

    if (code != TM_COMMIT)
      throw new XALogException(L.l("'{0}' is an illegal log header.",
                                   code));


    int len = readShort(is);

    is.skip(len);
  }

  /**
   * Reads a record.
   */
  public void readRecord(ReadStream is, int code)
    throws IOException
  {
    int len = readShort(is);

    switch (code) {
    case TM_COMMIT:
      readXid(is, _tempXid);
      if (log.isLoggable(Level.FINER))
        log.finer("recover commit: " + _tempXid);
      XidImpl xid = (XidImpl) _tempXid.clone();
      _recoverXids.add(xid);
      break;
      
    case TM_FINISH:
      readXid(is, _tempXid);
      _recoverXids.remove(_tempXid);
      if (log.isLoggable(Level.FINER))
        log.finer("recover finish: " + _tempXid);
      break;
      
    default:
      is.skip(len);
      break;
    }
  }

  /**
   * Reads a record.
   */
  public void readXid(ReadStream is, XidImpl xid)
    throws IOException
  {
    byte []gtid = xid.getGlobalTransactionId();
    
    is.readAll(gtid, 0, gtid.length);
  }

  /**
   * Returns true if the stream is flushing.
   */
  public boolean isFlushing()
  {
    return _isFlushing;
  }

  /**
   * Writes a commit entry to the log file.
   */
  public void writeTMCommit(XidImpl xid)
  {
    try {
      byte []gtid = xid.getGlobalTransactionId();
      int gtidLength = gtid.length;
      long seq;
    
      synchronized (this) {
        _activeXids.putIfNew(xid, _fileSize);

        _out.write(TM_COMMIT);
        _out.write(gtidLength >> 8);
        _out.write(gtidLength);
        _out.write(gtid, 0, gtidLength);

        _fileSize += gtidLength + 5;
        seq = ++_recordSequence;

        flushToDisk(seq);
      }
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Writes a finish entry to the log file.
   */
  public void writeTMFinish(XidImpl xid)
  {
    try {
      byte []gtid = xid.getGlobalTransactionId();
      int gtidLength = gtid.length;
      long seq;

      synchronized (this) {
        _activeXids.remove(xid);

        _out.write(TM_FINISH);
        _out.write(gtidLength >> 8);
        _out.write(gtidLength);
        _out.write(gtid, 0, gtidLength);

        _fileSize += gtidLength + 5;
      }
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Flushes the buffers, etc to disk.  This must be called from within
   * synchronized(this).
   */
  private void flushToDisk(long seq)
    throws IOException
  {
    _flushCount++;

    try {
      while (_isActive && _flushSequence < seq) {
        if (! _isFlushing) {
          try {
            _isFlushing = true;

            if (_manager.allocateFlush(this)) {
              try {
                long flushSequence = _recordSequence;
                _out.flushToDisk();
                _flushSequence = flushSequence;

                return;
              } finally {
                _manager.releaseFlush(this);

                notifyAll();
              }
            }
          } finally {
            _isFlushing = false;
          }
        }

        try {
          wait(1000);
        } catch (InterruptedException e) {
        }
      }
    } finally {
      _flushCount--;
    }

    if (! _isActive)
      throw new IOException(L.l("log closed underneath"));
  }

  /**
   * Flushes the buffers, etc to disk.  This must be called from within
   * synchronized(this).
   */
  void flush()
    throws IOException
  {
    if (_out != null)
      _out.flush();
  }

  /**
   * Wakes the waiting threads.
   */
  void wake()
  {
    try {
      synchronized (this) {
	notifyAll();
      }
    } catch (Throwable e) {
    }
  }

  /**
   * Wakes the waiting threads.
   */
  void close()
  {
    try {
      _isActive = false;
      wake();

      synchronized (this) {
	_out.flushToDisk();
      }
    } catch (Throwable e) {
    }
  }

  /**
   * Reads a short
   */
  private int readShort(ReadStream is)
    throws IOException
  {
    int ch1 = is.read();
    int ch2 = is.read();

    if (ch2 < 0)
      throw new EOFException();

    return (((ch1 & 0xff) << 8) +
        ((ch2)));
  }

  /**
   * Reads an int
   */
  private int readInt(ReadStream is)
    throws IOException
  {
    int ch1 = is.read();
    int ch2 = is.read();
    int ch3 = is.read();
    int ch4 = is.read();

    if (ch4 < 0)
      throw new EOFException();

    return (((ch1 & 0xff) << 24) +
        ((ch2 & 0xff) << 16) +
        ((ch3 & 0xff) << 8) +
        ((ch4 & 0xff)));
  }

  /**
   * Reads a long
   */
  private long readLong(ReadStream is)
    throws IOException
  {
    int ch1 = is.read();
    int ch2 = is.read();
    int ch3 = is.read();
    int ch4 = is.read();
    int ch5 = is.read();
    int ch6 = is.read();
    int ch7 = is.read();
    int ch8 = is.read();

    if (ch8 < 0)
      throw new EOFException();

    return (((ch1 & 0xffL) << 56) +
        ((ch2 & 0xffL) << 48) +
        ((ch3 & 0xffL) << 40) +
        ((ch4 & 0xffL) << 32) +
        ((ch5 & 0xffL) << 24) +
        ((ch6 & 0xffL) << 16) +
        ((ch7 & 0xffL) << 8) +
        ((ch8 & 0xffL)));
  }

  /**
   * Writes an int
   */
  private void writeInt(WriteStream os, int value)
    throws IOException
  {
    os.write(value >> 24);
    os.write(value >> 16);
    os.write(value >> 8);
    os.write(value);
  }

  /**
   * Writes a long.
   */
  private void writeLong(WriteStream os, long value)
    throws IOException
  {
    os.write((int) (value >> 56));
    os.write((int) (value >> 48));
    os.write((int) (value >> 40));
    os.write((int) (value >> 32));
    os.write((int) (value >> 24));
    os.write((int) (value >> 16));
    os.write((int) (value >> 8));
    os.write((int) value);
  }
}
