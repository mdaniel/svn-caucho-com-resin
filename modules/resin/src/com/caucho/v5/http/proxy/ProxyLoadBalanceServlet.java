/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.v5.bartender.BartenderSystem;
import com.caucho.v5.bartender.ServerBartender;
import com.caucho.v5.bartender.pod.NodePodAmp;
import com.caucho.v5.bartender.pod.PodBartender;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.http.container.HttpContainer;
import com.caucho.v5.http.container.HttpContainerServlet;
import com.caucho.v5.http.protocol.RequestHttpBase;
import com.caucho.v5.http.protocol.RequestServlet;
import com.caucho.v5.http.protocol.ResponseCaucho;
import com.caucho.v5.http.protocol2.ClientHttp2;
import com.caucho.v5.http.protocol2.ClientStream2;
import com.caucho.v5.http.protocol2.InputStreamClient;
import com.caucho.v5.http.session.SessionManager;
import com.caucho.v5.io.IoUtil;
import com.caucho.v5.util.CharBuffer;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.LruCache;
import com.caucho.v5.util.QDate;

/**
 * Load balancing.
 *
 * <pre>
 * &lt;servlet-mapping url-pattern='/remote/*'>
 *   &lt;servlet-name>com.caucho.servlets.LoadBalanceServlet&lt;/servlet-name>
 *   &lt;init>
 *      &lt;cluster>app-tier&lt;/cluster>
 *   &lt;/init>
 * &lt;/servlet-mapping>
 * </pre>
 */
@SuppressWarnings("serial")
public class ProxyLoadBalanceServlet extends GenericServlet {
  private final static Logger log
    = Logger.getLogger(ProxyLoadBalanceServlet.class.getName());
  private final static L10N L = new L10N(ProxyLoadBalanceServlet.class);

  private static final int EXIT_MASK = 0x1;
  private static final int QUIT = 0x0;
  private static final int EXIT = 0x1;

  private static final int STATUS_MASK = 0x6;
  private static final int OK = 0x0; // request succeeded
  private static final int BUSY = 0x2; // server sends busy (retry GET/POST)
  private static final int FAIL = 0x4; // server failed (retry GET)
  
  private static final long COOKIE_EXPIRE_EPOCH; 
  
  private static final LruCache<CharBuffer,String> _nameCache
    = new LruCache<>(1024);

  private static final boolean []VALUE;

  private String _clusterId;
  private StrategyLoadBalance _strategy;
  private HashGeneratorStickyRequest _stickyGenerator;

  private LoadBalanceManager _loadBalancer;

  private boolean _isStickySessions = true;
  protected QDate _calendar = new QDate();
  
  private String _sessionId;

  private String _sessionPrefix;
  private String _altSessionPrefix;
  private PodBartender _pod;
    
  private final AtomicLong _roundRobin = new AtomicLong();
  private final ConcurrentHashMap<String,ClientHttp2> _clientMap
    = new ConcurrentHashMap<>();


  /**
   * Set true if sticky-sessions should be enabled.
   */
  public void setStickySessions(boolean isStickySessions)
  {
    _isStickySessions = isStickySessions;
  }
  
  public void setSessionCookie(String sessionId)
  {
    _sessionId = sessionId;
  }

  /**
   * Sets the cluster id
   */
  public void setCluster(String id)
  {
    _clusterId = id;
  }

  /**
   * Sets the strategy.
   */
  public void setStrategy(String strategy)
    throws ConfigException
  {
    if ("round-robin".equals(strategy))
      _strategy = StrategyLoadBalance.ROUND_ROBIN;
    else if ("least-connection".equals(strategy))
      _strategy = StrategyLoadBalance.ADAPTIVE;
    else if ("adaptive".equals(strategy))
      _strategy = StrategyLoadBalance.ADAPTIVE;
    else
      throw new ConfigException(L.l("'{0}' is an unknown load-balance strategy.  'round-robin' and 'least-connection' are the known values.",
                                    strategy));
  }
  
  public void setStickyGenerator(HashGeneratorStickyRequest sticky)
  {
    _stickyGenerator = sticky;
  }
  
  public void setEscapedUrl(boolean isEscaped)
  {
    
  }

  /**
   * Initialize the servlet with the server's sruns.
   */
  @Override
  public void init()
    throws ServletException
  {
    HttpContainerServlet server = HttpContainerServlet.current();

    if (server == null) {
      throw new ConfigException(L.l("'{0}' requires an active Resin server",
                                    this));
    }
    
    _sessionPrefix = server.getSessionURLPrefix();
    _altSessionPrefix = server.getAlternateSessionURLPrefix();

    if (_clusterId == null) {
      _clusterId = getInitParameter("cluster");
    }

    if (_clusterId == null) {
      throw new ServletException("LoadBalanceServlet needs valid 'cluster'");
    }


    String sticky = getInitParameter("sticky-sessions");
    if ("false".equals(sticky)) {
      _isStickySessions = false;
    }
    
    BartenderSystem bartender = BartenderSystem.current();
    
    _pod = bartender.findPod(_clusterId); 
    
    LoadBalanceBuilder builder = new LoadBalanceBuilder();

    if (_strategy != null) {
      builder.setStrategy(_strategy);
    }
    
    builder.setTargetPod(_pod);
    
    if (_stickyGenerator != null)
      builder.setStickyRequestHashGenerator(_stickyGenerator);

    _loadBalancer = builder.create();
  }

  /**
   * Handle the request.
   */
  @Override
  public void service(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    ResponseCaucho res = (ResponseCaucho) response;

    String sessionId = getSessionId(req);
    
    ClientHttp2 clientHttp = openClient(sessionId);
    
    if (clientHttp == null) {
      res.sendError(HttpServletResponse.SC_BAD_GATEWAY);
      return;
    }
    
    boolean isFiner = log.isLoggable(Level.FINER);
    
    String path = createPath(req);
    
    if (isFiner) {
      log.finer("GET " + path + " [" + clientHttp + "]");
    }
    
    ClientStream2 stream = clientHttp.open(path);
    
    stream.method(req.getMethod());
    String host = req.getHeader("host");
    
    if (host != null) {
      stream.host(host);
    }
    
    Enumeration<String> e = req.getHeaderNames();
    while (e.hasMoreElements()) {
      String key = e.nextElement();
      
      stream.setHeader(key.toLowerCase(), req.getHeader(key));
    }
    
    try (InputStream is = req.getInputStream()) {
      if (is != null && is.available() >= 0) {
        try (OutputStream os = stream.getOutputStream()) {
          IoUtil.copy(is, os);
        }
      }
    }
    
    try (InputStreamClient inData = stream.getInputStream()) {
      if (isFiner) {
        log.finer("Status " + inData.getHeader(":status") + " [" + clientHttp + "]");
      }
      
      String statusValue = inData.getHeader(":status");

      if (statusValue != null && statusValue.length() == 3) {
        int status = 0;
        status = (100 * (statusValue.charAt(0) - '0')
                  + 10 * (statusValue.charAt(1) - '0')
                  + (statusValue.charAt(2) - '0'));
                  
        res.setStatus(status);
      }
      
      Map<String,String> headers = inData.getHeaders();
      
      if (headers != null) {
        for (Map.Entry<String,String> entry : headers.entrySet()) {
          String key = entry.getKey();
          String value = entry.getValue();
          
          if (key.startsWith(":")) {
            continue;
          }
          
          res.addHeader(key, value);
        }
      }
      
      try (OutputStream os = res.getOutputStream()) {
        IoUtil.copy(inData, os);
      }
    }
    
    // clientHttp.close();
  }
  
  private String createPath(HttpServletRequest req)
  {
    
    StringBuilder sb = new StringBuilder();
    /*
    sb.append("http://");
    
    sb.append(req.getServerName());
    sb.append(":");
    sb.append(req.getServerPort());
    */
    if (req.getContextPath() != null) {
      sb.append(req.getContextPath());
    }
    
    if (req.getServletPath() != null) {
      sb.append(req.getServletPath());
    }
    
    if (req.getPathInfo() != null) {
      sb.append(req.getPathInfo());
    }
    
    if (req.getQueryString() != null) {
      sb.append("?").append(req.getQueryString());
    }
    
    return sb.toString();
  }
  
  private String getSessionId(HttpServletRequest req)
  {
    String sessionId = null;

    if (! _isStickySessions) {
    }
    else if (_sessionId != null) {
      if (req instanceof RequestServlet) {
        Cookie cookie = ((RequestServlet) req).getCookie(_sessionId);
        
        if (cookie != null) {
          sessionId = cookie.getValue();
        }
      }
      else {
        Cookie []cookies = req.getCookies();
        
        for (int i = cookies != null ? cookies.length - 1 : -1; i >= 0; i--) {
          if (cookies[i].getName().equals(_sessionId)) {
            sessionId = cookies[i].getValue();
          }
        }
      }
    }
    else if (req instanceof RequestServlet) {
      sessionId = ((RequestServlet) req).getRequestedSessionIdNoVary();
    }
    else {
      sessionId = req.getRequestedSessionId();
    }

    return sessionId;
  }
  
  private ClientHttp2 openClient(String sessionId)
    throws IOException
  {
    int len = _pod.getServerCount();
    
    if (_isStickySessions && sessionId != null) {
      int hash = calculateStickySessionHash(sessionId);
      
      ClientHttp2 client = getClient(hash);
      
      if (client != null) {
        return client;
      }
    }
    
    for (int i = 0; i < len; i++) {
      long indexRoundRobin = _roundRobin.getAndIncrement();
      
      ClientHttp2 client = getClient(indexRoundRobin);
      
      if (client != null) {
        return client;
      }
    }
    
    return null;
  }
  
  private ClientHttp2 getClient(long hash)
  {
    int index = (int) (hash % _pod.getNodeCount());
    
    NodePodAmp node = _pod.getNode(index);
  
    ServerBartender server = node.getServer(0);
  
    if (server != null && server.isUp()) {
      ClientHttp2 client = getClient(server);
    
      return client;
    }
    
    return null;
  }
  
  private int calculateStickySessionHash(String sessionId)
  {
    int code = SessionManager.getServerCode(sessionId, 0);
    
    return code;
  }
  
  private ClientHttp2 getClient(ServerBartender server)
  {
    try {
      ClientHttp2 clientOld = _clientMap.get(server.getId());
      
      if (clientOld != null && clientOld.isActive()) {
        return clientOld;
      }
      
      String url = "http://" + server.getAddress() + ":" + server.port();
  
      ClientHttp2 clientNew = new ClientHttp2();
  
      clientNew.connect(url);
      
      if (clientOld != null 
          && _clientMap.replace(server.getId(), clientOld, clientNew)) {
        return clientNew;
      }
      else if (clientOld == null
               && _clientMap.putIfAbsent(server.getId(), clientNew) == null) {
        return clientNew;
      }
      else {
        clientNew.close();
        
        clientOld = _clientMap.get(server.getId());
        
        return clientOld;
      }
    } catch (IOException e) {
      log.log(Level.FINEST, e.toString(), e);
      
      return null;
    }
  }

  private int findCookie(List<Cookie> cookieList, String name)
  {
    for (int i = 0; i < cookieList.size(); i++) {
      Cookie cookie = cookieList.get(i);
      
      if (name.equals(cookie.getName())) {
        return i;
      }
    }
    
    return -1;
  }

  private Cookie parseCookie(CharBuffer rawCookie)
  {
    char []buf = rawCookie.buffer();
    int j = rawCookie.offset();
    int end = j + rawCookie.length();
    Cookie cookie = null;

    while (j < end) {
      char ch = 0;

      for (;
           j < end && ((ch = buf[j]) == ' ' || ch == ';' || ch == ',');
           j++) {
      }

      if (end <= j)
        break;

      int i = -1;
      for (; j < end; j++) {
        ch = buf[j];
        if (ch < 128 && RequestHttpBase.TOKEN[ch]) {
          if (i == -1)
            i = j;
        }
        else
          break;
      }

      final String name = rawCookie.substring(i, j);

      if ("Secure".equalsIgnoreCase(name)) {
        cookie.setSecure(true);

        continue;
      }
      else if ("HttpOnly".equalsIgnoreCase(name)) {
        cookie.setHttpOnly(true);

        continue;
      }

      for (; j < end && (ch = buf[j]) == ' '; j++) {
      }

      if (end <= j) {
        break;
      }
      else if (ch != '=') {
        for (; j < end && (buf[j]) != ';'; j++) {
        }

        continue;
      }

      j++;

      for (; j < end && (ch = buf[j]) == ' '; j++) {
      }

      int z;

      if (ch == '"') {
        j++;
        i = j;
        z = end;

        for (; j < end; j++) {
          ch = buf[j];
          
          if (ch == '"') {
            z = j;
            break;
          }
        }
        j++;
      }
      else {
        i = j;
        z = i;
        
        for (; j < end; j++) {
          ch = buf[j];
          
          if (! (ch < 128 && VALUE[ch])) {
            break;
          }
        }
        z = j;
      }

      final String value = rawCookie.substring(i, z);

      if (cookie == null) {
        cookie = new Cookie(name, value);
      }
      else if ("Version".equalsIgnoreCase(name)) {
        if (value.length() > 0)
          cookie.setVersion(value.charAt(0) - '0');
      }
      else if ("Domain".equalsIgnoreCase(name))
        cookie.setDomain(value);
      else if ("Path".equalsIgnoreCase(name))
        cookie.setPath(value);
      else if ("Max-Age".equalsIgnoreCase(name))
        cookie.setMaxAge(Integer.parseInt(value));
      else if ("Comment".equalsIgnoreCase(name))
        cookie.setComment(value);
      else if (cookie.getMaxAge() == -1 && "Expires".equalsIgnoreCase(name)) {
        QDate date = new QDate();
        try {
          long expires = date.parseDate(value);
          long now = CurrentTime.getCurrentTime();
          
          if (expires <= COOKIE_EXPIRE_EPOCH) {
            // #5456
            cookie.setMaxAge(0);
          }
          else {
            cookie.setMaxAge((int) ((expires - now + 500) / 1000));
          }
        } catch (Exception e) {
          log.log(Level.FINER, e.toString(), e);
        }
      }
    }

    return cookie;
  }

  @Override
  public void destroy()
  {
    super.destroy();

    _loadBalancer.close();
  }

  static {
    VALUE = new boolean[256];

    System.arraycopy(RequestHttpBase.TOKEN,
                     0,
                     VALUE,
                     0,
                     RequestHttpBase.TOKEN.length);

    VALUE[' '] = true;
    VALUE['='] = true;
    VALUE[','] = true;
    
    QDate date = QDate.allocateLocalDate();
    date.setGMTTime(0);
    date.setYear(1995);
    date.setMonth(1);
    date.setDayOfMonth(1);
    
    COOKIE_EXPIRE_EPOCH = date.getGMTTime();
  }
}
