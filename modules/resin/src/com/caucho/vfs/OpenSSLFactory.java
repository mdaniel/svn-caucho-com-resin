/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.vfs;

import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import com.caucho.config.ConfigException;
import com.caucho.config.types.Period;
import com.caucho.util.JniTroubleshoot;
import com.caucho.util.L10N;


/**
 * Abstract socket to handle both normal sockets and bin/resin sockets.
 */
public class OpenSSLFactory extends QServerSocket implements SSLFactory {
  private static final L10N L = new L10N(OpenSSLFactory.class);

  private static final int PROTOCOL_SSL2 = 0x01;
  private static final int PROTOCOL_SSL3 = 0x02;
  private static final int PROTOCOL_TLS1 = 0x04;

  private static Object _sslInitLock = new Object();
  
  private static boolean _hasJniInit;
  private static final JniTroubleshoot _jniTroubleshoot;

  private String _certificateFile;
  private String _keyFile;
  private String _certificateChainFile;
  private String _caCertificatePath;
  private String _caCertificateFile;
  private String _caRevocationPath;
  private String _caRevocationFile;
  private String _password;
  private String _verifyClient;
  private int _verifyDepth = -1;
  private String _cipherSuite;
  private String _cryptoDevice;
  private boolean _uncleanShutdown;
  private String _protocol;
  private int _protocolFlags = ~0;

  private boolean _enableSessionCache = true;
  private int _sessionCacheTimeout = 300;

  private QServerSocket _stdServerSocket;
  
  private long _configFd;
  
  /**
   * Creates a ServerSocket factory without initializing it.
   */
  public OpenSSLFactory()
  {
  }

  /**
   * Sets the certificate file.
   */
  public void setCertificateFile(Path certificateFile)
  {
    _certificateFile = certificateFile.getNativePath();
  }

  /**
   * Returns the certificate file.
   */
  public String getCertificateFile()
  {
    return _certificateFile;
  }

  /**
   * Sets the key file.
   */
  public void setCertificateKeyFile(Path keyFile)
  {
    _keyFile = keyFile.getNativePath();
  }

  /**
   * Returns the key file.
   */
  public String getCertificateKeyFile()
  {
    return _keyFile;
  }

  /**
   * Sets the certificateChainFile.
   */
  public void setCertificateChainFile(Path certificateChainFile)
  {
    _certificateChainFile = certificateChainFile.getNativePath();
  }

  /**
   * Returns the certificateChainFile
   */
  public String getCertificateChainFile()
  {
    return _certificateChainFile;
  }

  /**
   * Sets the caCertificatePath.
   */
  public void setCACertificatePath(Path caCertificatePath)
  {
    _caCertificatePath = caCertificatePath.getNativePath();
  }

  /**
   * Returns the caCertificatePath.
   */
  public String getCACertificatePath()
  {
    return _caCertificatePath;
  }

  /**
   * Sets the caCertificateFile.
   */
  public void setCACertificateFile(Path caCertificateFile)
  {
    _caCertificateFile = caCertificateFile.getNativePath();
  }

  /**
   * Returns the caCertificateFile.
   */
  public String getCACertificateFile()
  {
    return _caCertificateFile;
  }

  /**
   * Sets the caRevocationPath.
   */
  public void setCARevocationPath(Path caRevocationPath)
  {
    _caRevocationPath = caRevocationPath.getNativePath();
  }

  /**
   * Returns the caRevocationPath.
   */
  public String getCARevocationPath()
  {
    return _caRevocationPath;
  }

  /**
   * Sets the caRevocationFile.
   */
  public void setCARevocationFile(Path caRevocationFile)
  {
    _caRevocationFile = caRevocationFile.getNativePath();
  }

  /**
   * Returns the caRevocationFile.
   */
  public String getCARevocationFile()
  {
    return _caRevocationFile;
  }

  /**
   * Sets the cipher-suite
   */
  public void setCipherSuite(String cipherSuite)
  {
    _cipherSuite = cipherSuite;
  }

  /**
   * Returns the cipher suite
   */
  public String getCipherSuite()
  {
    return _cipherSuite;
  }

  /**
   * Sets the crypto-device
   */
  public void setCryptoDevice(String cryptoDevice)
  {
    _cryptoDevice = cryptoDevice;
  }

  /**
   * Returns the crypto-device
   */
  public String getCryptoDevice()
  {
    return _cryptoDevice;
  }

  /**
   * Sets the password.
   */
  public void setPassword(String password)
  {
    _password = password;
  }

  /**
   * Returns the key file.
   */
  public String getPassword()
  {
    return _password;
  }

  /**
   * Sets the verifyClient.
   */
  public void setVerifyClient(String verifyClient)
    throws ConfigException
  {
    if (! "optional_no_ca".equals(verifyClient)
        && ! "optional-no-ca".equals(verifyClient)
        && ! "optional".equals(verifyClient)
        && ! "require".equals(verifyClient)
        && ! "none".equals(verifyClient))
      throw new ConfigException(L.l("'{0}' is an unknown value for verify-client.  Valid values are 'optional-no-ca', 'optional', and 'require'.",
                                    verifyClient));

    if ("none".equals(verifyClient))
      _verifyClient = null;
    else
      _verifyClient = verifyClient;
  }

  /**
   * Returns the verify client
   */
  public String getVerifyClient()
  {
    return _verifyClient;
  }

  /**
   * Sets the verify depth
   */
  public void setVerifyDepth(int verifyDepth)
  {
    _verifyDepth = verifyDepth;
  }

  /**
   * Sets the unclean-shutdown
   */
  public void setUncleanShutdown(boolean uncleanShutdown)
  {
    _uncleanShutdown = uncleanShutdown;
  }

  /**
   * Returns the unclean shutdown
   */
  public boolean getUncleanShutdown()
  {
    return _uncleanShutdown;
  }

  /**
   * Enable the session cache
   */
  public void setSessionCache(boolean enable)
  {
    _enableSessionCache = enable;
  }

  /**
   * Sets the session cache timeout
   */
  public void setSessionCacheTimeout(Period period)
  {
    _sessionCacheTimeout = (int) (period.getPeriod() / 1000);
  }

  /**
   * Sets the protocol: +SSLv3
   */
  public void setProtocol(String protocol)
    throws ConfigException
  {
    _protocol = protocol;
    
    String []values = Pattern.compile("\\s+").split(protocol);

    int protocolFlags = 0;
    for (int i = 0; i < values.length; i++) {
      if (values[i].equalsIgnoreCase("+all"))
	protocolFlags = ~0;
      else if (values[i].equalsIgnoreCase("-all"))
	protocolFlags = 0;
      else if (values[i].equalsIgnoreCase("+sslv2"))
	protocolFlags |= PROTOCOL_SSL2;
      else if (values[i].equalsIgnoreCase("-sslv2"))
	protocolFlags &= ~PROTOCOL_SSL2;
      else if (values[i].equalsIgnoreCase("+sslv3"))
	protocolFlags |= PROTOCOL_SSL3;
      else if (values[i].equalsIgnoreCase("-sslv3"))
	protocolFlags &= ~PROTOCOL_SSL3;
      else if (values[i].equalsIgnoreCase("+tlsv1"))
	protocolFlags |= PROTOCOL_TLS1;
      else if (values[i].equalsIgnoreCase("-tlsv1"))
	protocolFlags &= ~PROTOCOL_TLS1;
      else
	throw new ConfigException(L.l("unknown protocol value '{0}'",
				      protocol));
    }

    if (values.length > 0)
      _protocolFlags = protocolFlags;
  }

  /**
   * Initialize
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    if (_certificateFile == null)
      throw new ConfigException(L.l("`certificate-file' is required for OpenSSL."));
    if (_password == null)
      throw new ConfigException(L.l("`password' is required for OpenSSL."));
  }

  /**
   * Creates the server socket.
   */
  public QServerSocket create(InetAddress addr, int port)
    throws ConfigException, IOException
  {
    synchronized (_sslInitLock) {
      if (_stdServerSocket != null)
	throw new IOException(L.l("Can't create duplicte ssl factory."));

      initConfig();
    
      _stdServerSocket = QJniServerSocket.createJNI(addr, port);

      initSSL();

      return this;
    }
  }

  /**
   * Creates the server socket.
   */
  public QServerSocket bind(QServerSocket ss)
    throws ConfigException, IOException
  {
    synchronized (_sslInitLock) {
      if (_stdServerSocket != null)
	throw new ConfigException(L.l("Can't create duplicte ssl factory."));

      try {
	initConfig();
      } catch (RuntimeException e) {
	e.printStackTrace();
	throw e;
      }
    
      _stdServerSocket = ss;

      initSSL();

      return this;
    }
  }

  private void initSSL()
    throws IOException
  {
    JniServerSocketImpl jniServerSocket = (JniServerSocketImpl) _stdServerSocket;

    boolean isOk = false;
    try {
      nativeInit(jniServerSocket.getFd(), _configFd);
      isOk = true;
    } finally {
      if (! isOk)
	_stdServerSocket = null;
      
      if (! isOk)
	jniServerSocket.close();
    }

    if (_stdServerSocket == null)
      throw new IOException(L.l("Can't create OpenSSL factory."));
  }
  
  public void setTcpNoDelay(boolean delay)
  {
    _stdServerSocket.setTcpNoDelay(delay);
  }
  
  public boolean isTcpNoDelay()
  {
    return _stdServerSocket.isTcpNoDelay();
  }

  /**
   * Sets the socket timeout for connections.
   */
  public void setConnectionSocketTimeout(int ms)
  {
    _stdServerSocket.setConnectionSocketTimeout(ms);
  }

  /**
   * Sets the socket's listen backlog.
   */
  @Override
  public void listen(int backlog)
  {
    _stdServerSocket.listen(backlog);
  }

  public boolean accept(QSocket socket)
    throws IOException
  {
    JniSocketImpl jniSocket = (JniSocketImpl) socket;

    if (! _stdServerSocket.accept(socket))
      return false;

    long fd;
    
    synchronized (jniSocket) {
      fd = open(jniSocket.getFd(), _configFd);
    }

    if (fd == 0) {
      jniSocket.close();
      throw new IOException(L.l("failed to open SSL socket"));
    }
    else {
      jniSocket.setSecure(true);
      
      return true;
    }
  }
  
  public QSocket createSocket()
    throws IOException
  {
    return _stdServerSocket.createSocket();
  }

  public InetAddress getLocalAddress()
  {
    return _stdServerSocket.getLocalAddress();
  }

  public int getLocalPort()
  {
    return _stdServerSocket.getLocalPort();
  }
  
  public void close()
    throws IOException
  {
    QServerSocket ss = _stdServerSocket;
    _stdServerSocket = null;

    if (ss != null)
      ss.close();
  }

  public synchronized void initConfig()
    throws ConfigException
  {
    _jniTroubleshoot.checkIsValid();

    if (_configFd != 0)
      throw new ConfigException(L.l("Configuration is already initialized."));
    
    String certificateFile = _certificateFile;
    String keyFile = _keyFile;

    if (keyFile == null)
      keyFile = certificateFile;
    if (certificateFile == null)
      certificateFile = keyFile;

    if (certificateFile == null)
      throw new ConfigException(L.l("certificate file is missing"));
    
    if (keyFile == null)
      throw new ConfigException(L.l("key file is missing"));

    if (_password == null)
      throw new ConfigException(L.l("password is missing"));

    _configFd = initConfig(certificateFile, keyFile, _password,
			   _certificateChainFile,
			   _caCertificatePath, _caCertificateFile,
			   _caRevocationPath, _caRevocationFile,
			   _cipherSuite, _cryptoDevice, _protocolFlags,
			   _uncleanShutdown);

    if (_configFd == 0)
      throw new ConfigException("Error initializing SSL server socket");

    setVerify(_configFd, _verifyClient, _verifyDepth);
    setSessionCache(_configFd, _enableSessionCache, _sessionCacheTimeout);
  }

  /**
   * Initializes the configuration
   */
  native long initConfig(String certificateFile,
                         String keyFile,
                         String password,
                         String certificateChainFile,
                         String caCertificatePath,
                         String caCertificateFile,
                         String caRevocationPath,
                         String caRevocationFile,
                         String cipherSuite,
                         String cryptoDevice,
                         int protocolFlags,
                         boolean uncleanShutdown)
    throws ConfigException;

  /**
   * Sets the verify depth.
   */
  native void setVerify(long fd, String verifyClient, int verifyDepth);

  /**
   * Sets the session cache
   */
  native void setSessionCache(long fd, boolean enable, int timeout);

  /**
   * Initialize the socket
   */
  native void nativeInit(long ssFd, long configFd)
    throws ConfigException;

  /**
   * Opens the connection for SSL.
   */
  native long open(long fd, long configFd);

  static {
    JniTroubleshoot jniTroubleshoot = null;

    try {
      System.loadLibrary("resinssl");

      jniTroubleshoot 
        = new JniTroubleshoot(OpenSSLFactory.class, "resinssl");
    } 
    catch (Throwable e) {
      jniTroubleshoot 
        = new JniTroubleshoot(OpenSSLFactory.class, "resinssl", e);
    }

    _jniTroubleshoot = jniTroubleshoot;
  }

}

