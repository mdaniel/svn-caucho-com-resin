/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.vfs;

import com.caucho.v5.inject.Module;
import com.caucho.v5.util.IntMap;
import com.caucho.v5.vfs.QSocket;
import com.caucho.v5.vfs.SocketChannelStream;
import com.caucho.v5.vfs.StreamImpl;
import com.caucho.v5.vfs.TempBuffer;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract socket to handle both normal sockets and bin/resin sockets.
 */
@Module
public class QSocketChannelWrapper extends QSocket {
  private static final Logger log
    = Logger.getLogger(QSocketChannelWrapper.class.getName());
  private static Class<?> sslSocketClass;
  private static IntMap sslKeySizes;
  
  private SocketChannel _channel;
  private ByteBuffer _byteBuffer;
  private byte []_buffer;

  private SocketChannelStream _streamImpl;

  public QSocketChannelWrapper()
  {
    _byteBuffer = ByteBuffer.allocate(TempBuffer.SIZE);
  }

  public QSocketChannelWrapper(SocketChannel s)
  {
    this();
    
    init(s);
  }

  public void init(SocketChannel channel)
  {
    _channel = channel;
  }
  
  public Socket getSocket()
  {
    return _channel.socket();
  }

  /* (non-Javadoc)
   * @see com.caucho.v5.vfs.QSocket#acceptInitialRead(byte[], int, int)
   */
  @Override
  public int acceptInitialRead(byte[] buffer, int offset, int length)
      throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  } 

  /**
   * Sets the socket timeout.
   */
  public void setReadTimeout(int ms)
    throws IOException
  {
    getSocket().setSoTimeout(ms);
  }

  /**
   * Returns the server inet address that accepted the request.
   */
  @Override
  public InetAddress getLocalAddress()
  {
    return getSocket().getLocalAddress();
  }
  
  /**
   * Returns the server port that accepted the request.
   */
  @Override
  public int getLocalPort()
  {
    return getSocket().getLocalPort();
  }

  /**
   * Returns the remote client's inet address.
   */
  @Override
  public InetAddress getRemoteAddress()
  {
    if (_channel != null)
      return getSocket().getInetAddress();
    else
      return null;
  }
  
  /**
   * Returns the remote client's port.
   */
  @Override
  public int getRemotePort()
  {
    if (_channel != null)
      return getSocket().getPort();
    else
      return 0;
  }

  /**
   * Returns true if the connection is secure.
   */
  @Override
  public boolean isSecure()
  {
    if (_channel == null || sslSocketClass == null)
      return false;
    else
      return sslSocketClass.isAssignableFrom(getSocket().getClass());
  }
  /**
   * Returns the secure cipher algorithm.
   */
  @Override
  public String getCipherSuite()
  {
    if (! (getSocket() instanceof SSLSocket))
      return super.getCipherSuite();

    SSLSocket sslSocket = (SSLSocket) getSocket();
    
    SSLSession sslSession = sslSocket.getSession();
    
    if (sslSession != null)
      return sslSession.getCipherSuite();
    else
      return null;
  }

  /**
   * Returns the bits in the socket.
   */
  @Override
  public int getCipherBits()
  {
    if (! (getSocket() instanceof SSLSocket))
      return super.getCipherBits();
    
    SSLSocket sslSocket = (SSLSocket) getSocket();
    
    SSLSession sslSession = sslSocket.getSession();
    
    if (sslSession != null)
      return sslKeySizes.get(sslSession.getCipherSuite());
    else
      return 0;
  }
  
  /**
   * Returns the client certificate.
   */
  @Override
  public X509Certificate getClientCertificate()
    throws CertificateException
  {
    X509Certificate []certs = getClientCertificates();

    if (certs == null || certs.length == 0)
      return null;
    else
      return certs[0];
  }

  /**
   * Returns the client certificate.
   */
  @Override
  public X509Certificate []getClientCertificates()
    throws CertificateException
  {
    if (sslSocketClass == null)
      return null;
    else
      return getClientCertificatesImpl();
  }
  
  /**
   * Returns the client certificate.
   */
  private X509Certificate []getClientCertificatesImpl()
    throws CertificateException
  {
    if (! (getSocket() instanceof SSLSocket))
      return null;
    
    SSLSocket sslSocket = (SSLSocket) getSocket();

    SSLSession sslSession = sslSocket.getSession();
    if (sslSession == null)
      return null;

    try {
      return (X509Certificate []) sslSession.getPeerCertificates();
    } catch (SSLPeerUnverifiedException e) {
      if (log.isLoggable(Level.FINEST))
        log.log(Level.FINEST, e.toString(), e);
      
      return null;
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }
    
    return null;
  }

  /**
   * Returns the selectable channel.
   */
  @Override
  public SelectableChannel getSelectableChannel()
  {
    return _channel;
  }
  
  /**
   * Returns the socket's input stream.
   */
  @Override
  public StreamImpl getStream()
    throws IOException
  {
    if (_streamImpl == null)
      _streamImpl = new SocketChannelStream();

    _streamImpl.init(_channel);

    return _streamImpl;
  }
  
  public void resetTotalBytes()
  {
    /*
    if (_streamImpl != null)
      _streamImpl.resetTotalBytes();
      */
  }

  @Override
  public long getTotalReadBytes()
  {
    // return (_streamImpl == null) ? 0 : _streamImpl.getTotalReadBytes();
    return 0;
  }

  @Override
  public long getTotalWriteBytes()
  {
    // return (_streamImpl == null) ? 0 : _streamImpl.getTotalWriteBytes();
    return 0;
  }

  /**
   * Returns true for closes.
   */
  @Override
  public boolean isClosed()
  {
    return _channel == null;
  }

  /**
   * Closes the underlying socket.
   */
  @Override
  public void close()
    throws IOException
  {
    SocketChannel channel = _channel;
    _channel = null;

    if (channel != null) {
      try {
        channel.close();
      } catch (Exception e) {
      }
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _channel + "]";
  }

  static {
    try {
      sslSocketClass = Class.forName("javax.net.ssl.SSLSocket");
    } catch (Throwable e) {
    }

    sslKeySizes = new IntMap();
    sslKeySizes.put("SSL_DH_anon_WITH_DES_CBC_SHA", 56);
    sslKeySizes.put("SSL_DH_anon_WITH_3DES_EDE_CBC_SHA", 168);
    sslKeySizes.put("SSL_DH_anon_WITH_RC4_128_MD5", 128);
    sslKeySizes.put("SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA", 40);
    sslKeySizes.put("SSL_DH_anon_EXPORT_WITH_RC4_40_MD5", 40);
    sslKeySizes.put("SSL_DHE_DSS_WITH_DES_CBC_SHA", 56);
    sslKeySizes.put("SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA", 40);
    sslKeySizes.put("SSL_RSA_WITH_RC4_128_MD5", 128);
    sslKeySizes.put("SSL_RSA_WITH_RC4_128_SHA", 128);
    sslKeySizes.put("SSL_RSA_WITH_DES_CBC_SHA", 56);
    sslKeySizes.put("SSL_RSA_WITH_3DES_EDE_CBC_SHA", 168);
    sslKeySizes.put("SSL_RSA_EXPORT_WITH_RC4_40_MD5", 40);
    sslKeySizes.put("SSL_RSA_WITH_NULL_MD5", 0);
    sslKeySizes.put("SSL_RSA_WITH_NULL_SHA", 0);
    sslKeySizes.put("SSL_DSA_WITH_RC4_128_MD5", 128);
    sslKeySizes.put("SSL_DSA_WITH_RC4_128_SHA", 128);
    sslKeySizes.put("SSL_DSA_WITH_DES_CBC_SHA", 56);
    sslKeySizes.put("SSL_DSA_WITH_3DES_EDE_CBC_SHA", 168);
    sslKeySizes.put("SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA", 168);
    sslKeySizes.put("SSL_DSA_EXPORT_WITH_RC4_40_MD5", 40);
    sslKeySizes.put("SSL_DSA_WITH_NULL_MD5", 0);
    sslKeySizes.put("SSL_DSA_WITH_NULL_SHA", 0);
  }
}

