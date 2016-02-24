/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.transaction.xalog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import com.caucho.v5.amp.thread.ThreadPool;
import com.caucho.v5.config.types.Bytes;
import com.caucho.v5.transaction.TransactionManagerImpl;
import com.caucho.v5.transaction.XidImpl;
import com.caucho.v5.util.ThreadTask;
import com.caucho.v5.vfs.PathImpl;

/**
 * Implements a single log stream.  Each log stream has two associated
 * files in order to switch at the end of the file.
 */
public class XALogManager extends AbstractXALogManager implements ThreadTask
{
  private ArrayList<XALogStream> _logStreamList
    = new ArrayList<XALogStream>();

  private HashSet<XidImpl> _recoverXids = new HashSet<XidImpl>();
  
  private XALogStream []_logStreams;

  private int _roundRobin;

  private int _flushCount = 1;

  private boolean _isInit;


  /**
   * Creates the log.
   */
  public XALogManager()
  {
  }

  /**
   * Sets the max file size.
   */
  public void setMaxFileSize(Bytes size)
  {
    size.getBytes();
  }

  /**
   * Adds a log path.
   */
  @Override
  public void setPath(PathImpl path)
    throws IOException
  {
    _logStreamList.add(new XALogStream(this, path));
  }

  /**
   * Initialize the log manager.
   */
  @Override
  public void init()
  {
    if (_isInit)
      return;
    
    _logStreams = new XALogStream[_logStreamList.size()];

    _logStreamList.toArray(_logStreams);

    _isInit = true;
  }

  /**
   * Starts the log manager.
   */
  @Override
  public void start()
    throws IOException
  {
    init();

    TransactionManagerImpl tm = TransactionManagerImpl.getLocal();
    tm.setXALogManager(this);
    
    for (int i = 0; i < _logStreams.length; i++) {
      _logStreams[i].start();
    }
  }

  /**
   * Called from the stream to add a recover xid.
   */
  void addRecoverXid(XidImpl xid)
  {
    _recoverXids.add(xid);
  }

  /**
   * Called from the stream to add a recover xid.
   */
  void addRecoverXids(HashSet<XidImpl> xidSet)
  {
    _recoverXids.addAll(xidSet);
  }

  /**
   * True if the xid is an already-committed xid
   */
  @Override
  public boolean hasCommittedXid(XidImpl xid)
  {
    return _recoverXids.contains(xid);
  }

  /**
   * Returns a stream for a new transaction.
   */
  @Override
  public XALogStream getStream()
  {
    int len = _logStreams.length;

    synchronized (this) {
      // First, find a stream that isn't blocked flushing
      for (int i = 0; i < len; i++) {
        XALogStream stream = _logStreams[_roundRobin];

        _roundRobin = (_roundRobin + 1) % len;

        if (! stream.isFlushing())
          return stream;
      }

      return _logStreams[_roundRobin];
    }
  }

  /**
   * Returns a stream for a new transaction.
   */
  public void flush()
  {
    int len = _logStreams.length;

    synchronized (this) {
      for (int i = 0; i < len; i++) {
        XALogStream stream = _logStreams[i];

        try {
          stream.flush();
        } catch (Throwable e) {
        }
      }
    }
  }

  /**
   * Returns true if the stream is allowed to flush itself.
   *
   * Each stream may only call this from a single thread.
   */
  boolean allocateFlush(XALogStream stream)
  {
    synchronized (this) {
      if (_flushCount > 0) {
        _flushCount--;
        
        return true;
      }
      else
        return false;
    }
  }

  /**
   * Returns true if the stream is allowed to flush itself.
   *
   * Each stream may only call this from a single thread.
   */
  void releaseFlush(XALogStream stream)
  {
    synchronized (this) {
      _flushCount++;
    }

    ThreadPool.current().schedule(this);
  }

  /**
   * Updates the waiting flush.
   */
  public void run()
  {
    for (int i = 0; i < _logStreams.length; i++) {
      _logStreams[i].wake();
    }
  }

  /**
   * Closes the log manager.
   */
  public void close()
  {
    synchronized (this) {
      _flushCount = Integer.MAX_VALUE / 2;
    }
    
    for (int i = 0; i < _logStreams.length; i++) {
      _logStreams[i].close();
    }
  }
}
