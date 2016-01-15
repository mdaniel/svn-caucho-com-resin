/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.env.health;

import java.io.IOException;
import java.net.Socket;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.caucho.v5.bartender.BartenderSystem;
import com.caucho.v5.bartender.ServerBartender;
import com.caucho.v5.bartender.network.NetworkSystem;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.health.check.AbstractHealthCheck;
import com.caucho.v5.health.meter.MeterActiveTime;
import com.caucho.v5.health.meter.MeterService;
import com.caucho.v5.io.IoUtil;
import com.caucho.v5.network.port.PortTcp;
import com.caucho.v5.util.CharBuffer;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.ReadStream;
import com.caucho.v5.vfs.VfsOld;
import com.caucho.v5.vfs.WriteStream;

public class HttpStatusHealthCheckImpl extends AbstractHealthCheck
{
  private static final Logger log = 
    Logger.getLogger(HttpStatusHealthCheckImpl.class.getName());
  private static final L10N L = new L10N(HttpStatusHealthCheckImpl.class);
  
  private static final MeterActiveTime _pingMeter
    = MeterService.createActiveTimeMeter("Caucho|Http|Ping");

  // The server's host
  private String _pingHost;
  
  // The server's port
  private int _pingPort;
  
  private Map<String, PingItem> _pingUrls = new LinkedHashMap<String, PingItem>();
  
  // How many milliseconds to wait for a response before giving up
  private long _socketTimeout = 10 * 1000L;
  
  private Pattern _regexp = Pattern.compile("200");
  
  public String getPingHost()
  {
    return _pingHost;
  }
  
  public void setPingHost(String pingHost)
  {
    _pingHost = pingHost;
  }
  
  public int getPingPort()
  {
    return _pingPort;
  }
  
  public void setPingPort(int pingPort)
  {
    _pingPort = pingPort;
  }
  
  @Configurable
  public void addUrl(String url)
  {
    if (! _pingUrls.containsKey(url)) {
      _pingUrls.put(url, new PingItem(url));
    }
  }

  @Configurable
  public void addUrlList(String urlList)
  {
    for (String value : urlList.split("[\\s]+")) {
      if (value.length() > 0) {
        addUrl(value);
      }
    }
  }
  
  public Collection<String> getUrls()
  {
    return Collections.unmodifiableCollection(_pingUrls.keySet());
  }
  
  public long getSocketTimeout()
  {
    return _socketTimeout;
  }
  
  public void setSocketTimeout(long timeout)
  {
    _socketTimeout = timeout;
  }
  
  public Pattern getRegexp()
  {
    return _regexp;
  }
  
  public void setRegexp(Pattern regexp)
  {
    _regexp = regexp;
  }
  
  /**
   * Initialize the pinger.
   */
  @Override
  public void init()
  {
    super.init();
    
    if (_pingUrls.isEmpty()) {
      ServerBartender cloudServer =
        BartenderSystem.current().serverSelf();
      
      /* XXX:
      ClusterServer selfServer = cloudServer.getData(ClusterServer.class);
      
      for (String url : selfServer.getPingUrlList()) {
        addUrl(url);
      }
      */
    }
    
    if (_pingHost == null || _pingPort == 0) {
      PortTcp port = getHttpPort();
      
      if (port != null) {
        if (_pingHost == null)
          _pingHost = port.address();
        
        if (_pingPort == 0)
          _pingPort = port.getLocalPort();
      }
    }
  }
  
  private PortTcp getHttpPort()
  {
    NetworkSystem network = NetworkSystem.current();
    
    for (PortTcp port : network.getPorts()) {
      if ("http".equals(port.protocolName())) {
        return port;
      }
    }
    
    return null;
  }
  
  /**
   * Periodically pings the server to check for anything going wrong.
   */
  @Override
  public HealthCheckResult checkHealth()
  {
    StringBuilder sb = new StringBuilder();
    
    for (PingItem ping : _pingUrls.values()) {
      String url = ping.getUrl();
      
      // ActiveTimeMeter sensor = ping.getSensor();
      
      long time = _pingMeter.start();
      long ms;
      
      try {
        HealthCheckResult result = checkPing(url);
        
        if (! result.isOk())
          return result;
      } finally {
        ms = _pingMeter.end(time);
      }
      
      sb.append(" " + ms + "ms " + url);
    }
    
    return new HealthCheckResult(HealthStatus.OK, sb.toString());
  }
  
  protected HealthCheckResult checkPing(String url) 
  {
    String address = _pingHost;
    
    if (address == null)
      address = "127.0.0.1";

    if (_pingPort <= 0) {
      return new HealthCheckResult(HealthStatus.WARNING,
                                   L.l("missing HttpStatus port"));
    }
    
    ReadStream is = null;
    WriteStream os = null;
    Socket s = null;
    
    try {
      s = new Socket(address, _pingPort);
      s.setSoTimeout((int) _socketTimeout);
      
      os = VfsOld.openWrite(s.getOutputStream());
      
      os.print("HEAD ");
      os.print(url);
      os.print(" HTTP/1.0\r\n\r\n");
      os.flush();

      is = VfsOld.openRead(s.getInputStream());
      
      String status = is.readLine();
      
      if (log.isLoggable(Level.FINE)) {
        log.fine(L.l("{0}: http ping {1} {2}", getName(), url, status));
      }
      
      if (status != null && _regexp.matcher(status).find()) {
        return new HealthCheckResult(HealthStatus.OK);
      }
      
      String response = null;
      try {
        response = readResponse(is);
      } catch (IOException e) {
        if (log.isLoggable(Level.FINER)) {
          log.log(Level.FINER, e.toString(), e);
        }
        
        response = e.toString();
      }
      
      String msg =
        L.l("ping {0} failed, status: {1}\n{2}", url, status, response);
      return new HealthCheckResult(HealthStatus.CRITICAL, msg);
    } catch (IOException e) {
      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, e.toString(), e);
      }
      
      String msg = L.l("ping {0} failed, error: {1}", url, e.toString());
      return new HealthCheckResult(HealthStatus.CRITICAL, msg);
    } finally {
      IoUtil.close(is);
      IoUtil.close(os);
      
      try {
        if (s != null) {
          s.close();
        }
      } catch (IOException e) {
        
      }
    }
  }
  
  protected String readResponse(ReadStream is) 
    throws IOException
  {
    CharBuffer cb = new CharBuffer();
    
    try {
      int ch;
      
      while ((ch = is.read()) >= 0) {
        cb.append((char) ch);
      }
      
      return cb.toString();
    } finally {
      cb.close();
    }
  }
  
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    
    sb.append(getName()).append("[");
    
    boolean isFirst = true;
    for(String url : _pingUrls.keySet()) {
      if (! isFirst)
        sb.append(", ");
      sb.append(url);
      isFirst = false;
    }
    
    sb.append("]");
    
    return sb.toString();
  }
  
  private static class PingItem {
    private String _url;
    private MeterActiveTime _sensor;
    
    PingItem(String url)
    {
      _url = url;
      _sensor = MeterService.createActiveTimeMeter("Caucho|Http|Ping|" + _url);
    }
    
    String getUrl()
    {
      return _url;
    }
    
    MeterActiveTime getSensor()
    {
      return _sensor;
    }
  }
}
