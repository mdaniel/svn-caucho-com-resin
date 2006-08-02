/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.filters;

import java.io.*;
import java.util.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.servlet.http.*;
import javax.servlet.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

import com.caucho.log.Log;

import com.caucho.loader.Environment;

import com.caucho.config.types.Period;

import com.caucho.server.dispatch.UrlMap;

import com.caucho.server.cluster.Cluster;
import com.caucho.server.cluster.ClusterContainer;
import com.caucho.server.cluster.ClusterServer;
import com.caucho.server.cluster.ClusterStream;

import com.caucho.server.hmux.HmuxRequest;
import com.caucho.server.hmux.HmuxDispatchRequest;

/**
 * Dispatches to a backend hmux.
 */
public class HmuxDispatchFilter implements Filter {
  private static final L10N L = new L10N(HmuxDispatchFilter.class);
  private static final Logger log = Log.open(HmuxDispatchFilter.class);

  private static Entry _mismatchEntry = new Entry(false);

  private HashMap<String,HostEntry> _hostMap =
    new HashMap<String,HostEntry>();
  private long _lastUpdateTime;
  private long _updateInterval = 15000L;
  private String _mapETag;

  private FilterConfig _config;
  private ServletContext _application;
  private RequestDispatcher _remote;

  private String _clusterId;
  private Cluster _cluster;
  private ClusterServer []_srunGroup;

  /**
   * Sets the cluster id.
   */
  public void setClusterId(String id)
  {
    _clusterId = id;
  }

  /**
   * Sets the update interval.
   */
  public void setUpdateInterval(Period period)
  {
    _updateInterval = period.getPeriod();
  }
  
  /**
   * Filter init reads the filter configuration
   */
  public void init(FilterConfig config)
    throws ServletException
  {
    _config = config;
    
    _application = config.getServletContext();
    _remote = _application.getNamedDispatcher("remote");

    if (_remote == null)
      throw new ServletException(L.l("`remote' servlet must be configured so HmuxDispatchFilter can dispatch to the backend."));
    
    String clusterId = config.getInitParameter("cluster");
    if (clusterId != null)
      _clusterId = clusterId;
    
    if (_clusterId == null)
      throw new ServletException("HmuxDispatchFilter needs valid cluster");

    ClusterContainer clusterContainer = ClusterContainer.getLocal();

    if (clusterContainer != null)
      _cluster = clusterContainer.findCluster(_clusterId);

    if (_cluster == null)
      throw new ServletException("HmuxDispatchFilter needs valid cluster");
      
    _srunGroup = _cluster.getServerList();
  }
  
  /**
   * The filter dispatches to the backend.
   */
  public void doFilter(ServletRequest request, ServletResponse response,
                       FilterChain nextFilter)
    throws ServletException, IOException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    
    if (isDispatch(req.getServerName(),
                   req.getServerPort(),
                   req.getRequestURI()))
      _remote.forward(request, response);
    else
      nextFilter.doFilter(request, response);
  }

  /**
   * Decides whether to dispatch to the backend.
   */
  public boolean isDispatch(String serverName, int serverPort, String uri)
  {
    try {
      return isDispatchRemote(serverName, serverPort, uri);
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }

  /**
   * Decides whether to dispatch to the backend.
   */
  public boolean isDispatchRemote(String host, int port, String uri)
    throws IOException
  {
    long now = Alarm.getCurrentTime();

    HostEntry hostEntry = getHostEntry(host + ':' + port, uri);
    if (hostEntry == null)
      return false;

    AppEntry appEntry = hostEntry.getAppEntry(uri);
    if (appEntry == null)
      return false;

    String tail = uri.substring(appEntry.getContextPath().length());
    Entry entry = appEntry.findPattern(tail);
    if (entry == null)
      return false;
    else
      return entry.isDispatch();
  }

  /**
   * Fills the dispatch map.
   */
  public HostEntry getHostEntry(String host, String url)
  {
    HostEntry entry = _hostMap.get(host);

    long now = Alarm.getCurrentTime();
    
    synchronized (_hostMap) {
      if (entry != null && entry.getLastModified() + _updateInterval < now)
	return entry;

      if (entry == null) {
	entry = new HostEntry(host);
	_hostMap.put(host, entry);
      }

      entry.setLastModified(now);
    }

    ClusterServer srun = null;

    for (int i = 0; i < _srunGroup.length; i++) {
      srun = _srunGroup[i];

      if (srun != null)
	break;
    }

    if (srun == null)
      return entry;
    
    ClusterStream stream = null;
    stream = srun.getServerConnector().getClient().openIfLive();

    if (stream == null)
      return entry;

    ReadStream rs = stream.getReadStream();
    WriteStream ws = stream.getWriteStream();
    boolean keepalive = false;

    try {
      int channel = 2;
    
      ws.write(HmuxRequest.HMUX_CHANNEL);
      ws.write(channel >> 8);
      ws.write(channel);

      writeInt(ws, HmuxRequest.HMUX_PROTOCOL,
               HmuxRequest.HMUX_DISPATCH_PROTOCOL);

      writeString(ws, HmuxDispatchRequest.HMUX_HOST, host);

      writeString(ws, HmuxDispatchRequest.HMUX_QUERY_ALL, url);

      String etag = entry.getETag();
      if (etag != null)
	writeString(ws, HmuxDispatchRequest.HMUX_ETAG, etag);
	
      ws.write(HmuxRequest.HMUX_QUIT);

      int code = rs.read();
      if (code != HmuxRequest.HMUX_CHANNEL) {
        log.warning("Hmux protocol error at " + (char) code);

        return entry;
      }

      int outChannel = (rs.read() << 8) + rs.read();

      if (outChannel != 2)
        return entry;

      HashMap<String,AppEntry> appMap = new HashMap<String,AppEntry>();
      AppEntry appEntry = null;

      while ((code = rs.read()) >= 0) {
	switch (code) {
	case HmuxRequest.HMUX_QUIT:
	  entry.setAppMap(appMap);
	  keepalive = true;
	  return entry;
	case HmuxRequest.HMUX_EXIT:
	  entry.setAppMap(appMap);
	  keepalive = false;
	  return entry;

	case HmuxDispatchRequest.HMUX_HOST:
	  String canonicalHost = readString(rs);
	  log.finer("host: " + canonicalHost);

	  if (! canonicalHost.equals(host)) {
	    HostEntry canonicalEntry = getHostEntry(canonicalHost, url);
	    entry.setCanonicalHost(canonicalEntry);
	  }
	  break;

	case HmuxDispatchRequest.HMUX_WEB_APP:
	  String contextPath = readString(rs);
	  appEntry = new AppEntry(contextPath);
	  appMap.put(contextPath, appEntry);
	  log.finer("web-app: " + contextPath);
	  break;

	case HmuxDispatchRequest.HMUX_ETAG:
	  String newETag = readString(rs);
	  if (_mapETag != null && _mapETag.equals(newETag))
	    appMap = entry.getAppMap();
	  _mapETag = newETag;
	  break;
	  
	case HmuxDispatchRequest.HMUX_MATCH:
	  String pattern = readString(rs);
	  appEntry.addMatch(pattern);
	  log.finer("match: " + pattern);
	  break;
	  
	case HmuxDispatchRequest.HMUX_IGNORE:
	  pattern = readString(rs);
	  appEntry.addIgnore(pattern);
	  log.finer("ignore: " + pattern);
	  break;

	case HmuxRequest.HMUX_HEADER:
	  String header = readString(rs);
	  int ch = rs.read();

	  if (ch != HmuxRequest.HMUX_STRING) {
	    log.warning("Hmux protocol error at " + (char) code);
	    return entry;
	  }

	  String value = readString(rs);
	  log.finer(header + ": " + value);
	  break;
	  
	case HmuxDispatchRequest.HMUX_CLUSTER:
	  value = readString(rs);
	  log.finer("cluster: " + value);
	  break;
	  
	case HmuxDispatchRequest.HMUX_SRUN:
	  value = readString(rs);
	  log.finer("srun: " + value);
	  break;

	default:
	  log.warning("Hmux protocol error at " + (char) code);

	  return entry;
	}
      }

      return entry;
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);

      return entry;
    } finally {
      if (keepalive)
	stream.free();
      else
	stream.close();
    }
  }

  private void writeString(WriteStream ws, int code, String value)
    throws IOException
  {
    int len = value.length();

    ws.write(code);
    ws.write(len >> 8);
    ws.write(len);
    ws.print(value);
  }

  private String readString(ReadStream rs)
    throws IOException
  {
    int len = (rs.read() << 8) + rs.read();

    if (len < 0 || len > 65535)
      throw new IOException("PROTOCOL");

    CharBuffer cb = CharBuffer.allocate();

    rs.readAll(cb, len);

    return cb.close();
  }
  
  private void writeInt(WriteStream ws, int code, int value)
    throws IOException
  {
    ws.write(code);
    ws.write(0);
    ws.write(4);
    ws.write(value >> 24);
    ws.write(value >> 16);
    ws.write(value >> 8);
    ws.write(value);
  }
  
  /**
   * Any cleanup for the filter.
   */
  public void destroy()
  {
  }

  /**
   * Web app entries.
   */
  static class HostEntry {
    private String _hostName;
    private HostEntry _canonicalHost;
    private HashMap<String,AppEntry> _appMap;

    private String _etag;
    private long _lastModified;

    public HostEntry(String hostName)
    {
      _hostName = hostName;
    }

    /**
     * Returns the last modified time.
     */
    public long getLastModified()
    {
      return _lastModified;
    }

    /**
     * Sets the last modified time.
     */
    public void setLastModified(long lastModified)
    {
      _lastModified = lastModified;
    }

    /**
     * Returns the etag.
     */
    public String getETag()
    {
      return _etag;
    }

    /**
     * Sets the etag.
     */
    public void setETag(String etag)
    {
      _etag = etag;
    }

    /**
     * Sets the canonical host.
     */
    public void setCanonicalHost(HostEntry host)
    {
      _canonicalHost = host;
    }

    /**
     * Gets the canonical host.
     */
    public HostEntry getCanonicalHost()
    {
      return _canonicalHost;
    }

    /**
     * Returns the app map.
     */
    public HashMap<String,AppEntry> getAppMap()
    {
      return _appMap;
    }

    /**
     * Sets the app map.
     */
    public void setAppMap(HashMap<String,AppEntry> appMap)
    {
      _appMap = appMap;
    }

    public void addWebApp(String contentPath, AppEntry entry)
    {
      if (_appMap == null)
	_appMap = new HashMap<String,AppEntry>();

      _appMap.put(contentPath, entry);
    }

    public AppEntry getAppEntry(String uri)
    {
      if (_canonicalHost != null)
	return _canonicalHost.getAppEntry(uri);
      else if (_appMap == null) {
	return null;
      }
      else {
	HashMap<String,AppEntry> appMap = _appMap;

	int tail = uri.length();
	while (tail >= 0) {
	  String prefix = uri.substring(0, tail);

	  AppEntry appEntry = appMap.get(prefix);

	  if (appEntry != null)
	    return appEntry;

	  tail = uri.lastIndexOf('/', tail - 1);
	}

	return null;
      }
    }
  }

  /**
   * Web app entries.
   */
  static class AppEntry {
    private String _contextPath;
    private UrlMap<Entry> _urlMap = new UrlMap<Entry>();

    public AppEntry(String path)
    {
      _contextPath = path;
    }

    /**
     * Returns the context path.
     */
    public String getContextPath()
    {
      return _contextPath;
    }

    public Entry addMatch(String urlPattern)
    {
      Entry entry = new Entry(true);

      _urlMap.addMap(urlPattern, entry);

      return entry;
    }

    public Entry addIgnore(String urlPattern)
    {
      Entry entry = new Entry(false);

      _urlMap.addMap(urlPattern, entry);

      return entry;
    }

    public Entry findPattern(String urlPattern)
    {
      return _urlMap.map(urlPattern);
    }
  }
    
  /**
   * An entry for each URL/pattern.
   */
  static class Entry {
    private boolean _isDispatch;

    /**
     * Creates the new entry.
     *
     * @param isDispatch true if the URL should be a remote dispatch.
     */
    Entry(boolean isDispatch)
    {
      _isDispatch = isDispatch;
    }

    /**
     * Set true if the URL should be a remote dispatch.
     */
    public void setDispatch(boolean isDispatch)
    {
      _isDispatch = isDispatch;
    }

    /**
     * Return true if the URL should be a remote dispatch.
     */
    public boolean isDispatch()
    {
      return _isDispatch;
    }
  }
}
