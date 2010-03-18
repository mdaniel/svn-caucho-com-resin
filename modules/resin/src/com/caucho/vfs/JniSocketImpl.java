/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.vfs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.util.Alarm;

/**
 * Abstract socket to handle both normal sockets and bin/resin sockets.
 */
public class JniSocketImpl extends QSocket {
  private final static Logger log
    = Logger.getLogger(JniSocketImpl.class.getName());

  private long _fd;
  private JniStream _stream;

  private byte []_localAddrBuffer = new byte[256];
  private char []_localAddrCharBuffer = new char[256];
  private int _localAddrLength;
  private String _localName;
  private InetAddress _localAddr;

  private int _localPort;

  private byte []_remoteAddrBuffer = new byte[256];
  private char []_remoteAddrCharBuffer = new char[256];
  private int _remoteAddrLength;
  private String _remoteName;
  private InetAddress _remoteAddr;

  private int _remotePort;

  private boolean _isSecure;

  private Object _readLock = new Object();
  private Object _writeLock = new Object();

  private final AtomicBoolean _isClosed = new AtomicBoolean();

  JniSocketImpl()
  {
    _fd = nativeAllocate();
  }

  void initFd()
  {
    //_localAddrLength = getLocalIP(_fd, _localAddrBuffer, 0,
    //                              _localAddrBuffer.length);
    _localName = null;
    _localAddr = null;

    //_remoteAddrLength = getRemoteIP(_fd, _remoteAddrBuffer, 0,
    //                                _remoteAddrBuffer.length);
    _remoteName = null;
    _remoteAddr = null;

    _isSecure = false;

    nativeInit(_fd); // initialize fields from the _fd

    _isClosed.set(false);
  }

  final long getFd()
  {
    return _fd;
  }

  public int getNativeFd()
  {
    return getNativeFd(_fd);
  }

  /**
   * Returns the server port that accepted the request.
   */
  public int getLocalPort()
  {
    return _localPort;

    // return getLocalPort(_fd);
  }

  /**
   * Returns the remote client's host name.
   */
  @Override
  public String getRemoteHost()
  {
    if (_remoteName == null) {
      byte []remoteAddrBuffer = _remoteAddrBuffer;
      char []remoteAddrCharBuffer = _remoteAddrCharBuffer;

      for (int i = _remoteAddrLength - 1; i >= 0; i--)
        remoteAddrCharBuffer[i] = (char) (remoteAddrBuffer[i] & 0xff);

      _remoteName = new String(remoteAddrCharBuffer, 0, _remoteAddrLength);
    }

    return _remoteName;
  }

  /**
   * Returns the remote client's inet address.
   */
  @Override
  public InetAddress getRemoteAddress()
  {
    if (_remoteAddr == null) {
      try {
        _remoteAddr = InetAddress.getByName(getRemoteHost());
      } catch (Exception e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }

    return _remoteAddr;
  }

  /**
   * Returns the remote client's inet address.
   */
  @Override
  public int getRemoteAddress(byte []buffer, int offset, int length)
  {
    System.arraycopy(_remoteAddrBuffer, 0, buffer, offset, _remoteAddrLength);

    return _remoteAddrLength;
  }

  /**
   * Returns the remote client's inet address.
   */
  @Override
  public long getRemoteIP()
  {
    int len = _remoteAddrLength;

    byte []bytes = _remoteAddrBuffer;

    long address = 0;
    for (int i = 0; i < len; i++)
      address = 256 * address + (bytes[i] & 0xff);

    return address;
  }

  /**
   * Returns the remote client's port.
   */
  public int getRemotePort()
  {
    return _remotePort;

    // return getRemotePort(_fd);
  }

  /**
   * Returns the local server's host name.
   */
  public String getLocalHost()
  {
    if (_localName == null)
      _localName = new String(_localAddrBuffer, 0, _localAddrLength);

    return _localName;
  }

  /**
   * Returns the local server's inet address.
   */
  public InetAddress getLocalAddress()
  {
    if (_localAddr == null) {
      try {
        _localAddr = InetAddress.getByName(getLocalHost());
      } catch (Exception e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }

    return _localAddr;
  }

  /**
   * Returns the local server's inet address.
   */
  public int getLocalAddress(byte []buffer, int offset, int length)
  {
    System.arraycopy(_localAddrBuffer, 0, buffer, offset, _localAddrLength);

    return _localAddrLength;
  }

  /**
   * Set true for secure.
   */
  public void setSecure(boolean isSecure)
  {
    _isSecure = isSecure;
  }

  /**
   * Returns true if the connection is secure.
   */
  public boolean isSecure()
  {
    // return isSecure(_fd);

    return _isSecure;
  }

  /**
   * Returns the cipher for an ssl connection.
   */
  public String getCipherSuite()
  {
    return getCipher(_fd);
  }

  /**
   * Returns the number of bits in the cipher for an ssl connection.
   */
  @Override
  public int getCipherBits()
  {
    return getCipherBits(_fd);
  }

  /**
   * Returns the client certificate.
   */
  @Override
  public X509Certificate getClientCertificate()
    throws java.security.cert.CertificateException
  {
    TempBuffer tb = TempBuffer.allocate();
    byte []buffer = tb.getBuffer();
    int len = getClientCertificate(_fd, buffer, 0, buffer.length);
    X509Certificate cert = null;

    if (len > 0 && len < buffer.length) {
      try {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        InputStream is = new ByteArrayInputStream(buffer, 0, len);
        cert = (X509Certificate) cf.generateCertificate(is);
        is.close();
      } catch (IOException e) {
        return null;
      }
    }

    TempBuffer.free(tb);
    tb = null;

    return cert;
  }

  /**
   * Read non-blocking
   */
  public boolean readNonBlock(int ms)
  {
    synchronized (_readLock) {
      return nativeReadNonBlock(_fd, ms);
    }
  }

  /**
   * Reads from the socket.
   */
  public int read(byte []buffer, int offset, int length, long timeout)
    throws IOException
  {
    long expires = timeout + Alarm.getCurrentTime();
    
    synchronized (_readLock) {
      int result = 0;
      
      do {
        result = readNative(_fd, buffer, offset, length, timeout);
      } while (result == JniStream.TIMEOUT_EXN
               && Alarm.getCurrentTime() <= expires);

      return result;
    }
  }

  /**
   * Writes to the socket.
   */
  public int write(byte []buffer, int offset, int length, boolean isEnd)
    throws IOException
  {
    synchronized (_writeLock) {
      if (! isEnd)
        return writeNative(_fd, buffer, offset, length);
      else {
        _isClosed.set(true);

        return writeCloseNative(_fd, buffer, offset, length);
      }
    }
  }

  /**
   * Flushes the socket.
   */
  public int flush()
    throws IOException
  {
    synchronized (_writeLock) {
      return flushNative(_fd);
    }
  }

  /**
   * Returns a stream impl for the socket encapsulating the
   * input and output stream.
   */
  public StreamImpl getStream()
    throws IOException
  {
    if (_stream == null)
      _stream = new JniStream(this);

    _stream.init();

    return _stream;
  }

  public long getTotalReadBytes()
  {
    return (_stream == null) ? 0 : _stream.getTotalReadBytes();
  }

  public long getTotalWriteBytes()
  {
    return (_stream == null) ? 0 : _stream.getTotalWriteBytes();
  }

  /**
   * Returns true if closed.
   */
  public boolean isClosed()
  {
    return _isClosed.get();
  }

  /**
   * Closes the socket.
   *
   * XXX: potential sync issues
   */
  @Override
  public void forceShutdown()
  {
    nativeCloseFd(_fd);
  }

  /**
   * Closes the socket.
   *
   * XXX: potential sync issues
   */
  public void close()
    throws IOException
  {
    if (_isClosed.getAndSet(true))
      return;

    if (_stream != null)
      _stream.close();

    synchronized (this) {
      nativeClose(_fd);
    }
  }

  protected void finalize()
    throws Throwable
  {
    try {
      super.finalize();

      close();
    } catch (Throwable e) {
    }

    long fd = _fd;
    _fd = 0;

    nativeFree(fd);
  }

  native int getNativeFd(long fd);

  native boolean nativeReadNonBlock(long fd, int ms);

  private native int nativeInit(long fd);

  native int getRemoteIP(long fd, byte []buffer, int offset, int length);

  native int getRemotePort(long fd);

  native int getLocalIP(long fd, byte []buffer, int offset, int length);

  native int getLocalPort(long fd);

  native boolean isSecure(long fd);

  native String getCipher(long fd);

  native int getCipherBits(long fd);

  native int getClientCertificate(long fd,
                                  byte []buffer,
                                  int offset,
                                  int length);

  native int readNative(long fd, byte []buf, int offset, int length,
                        long timeout)
    throws IOException;

  private native int writeNative(long fd, byte []buf, int offset, int length)
    throws IOException;

  private native int writeCloseNative(long fd,
                                      byte []buf, int offset, int length)
    throws IOException;

  native int writeNative2(long fd,
                          byte []buf1, int off1, int len1,
                          byte []buf2, int off2, int len2)
    throws IOException;

  native int flushNative(long fd) throws IOException;

  private native void nativeCloseFd(long fd);
  private native void nativeClose(long fd);

  native long nativeAllocate();

  native void nativeFree(long fd);

  public String toString()
  {
    return ("JniSocketImpl$" + System.identityHashCode(this)
            + "[" + _fd + ",fd=" + getNativeFd(_fd) + "]");
  }
}

