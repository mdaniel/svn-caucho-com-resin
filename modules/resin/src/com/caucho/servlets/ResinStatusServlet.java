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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.servlets;

import java.util.Set;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;
import java.util.Comparator;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.IOException;
import java.io.PrintWriter;

import javax.management.ObjectName;
import javax.management.MBeanServer;

import javax.servlet.ServletException;
import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.util.L10N;
import com.caucho.util.QDate;

import com.caucho.log.Log;

import com.caucho.config.ConfigException;

import com.caucho.jmx.Jmx;

import com.caucho.mbeans.server.*;

/**
 * Displays some status information about the Resin server.
 * The servlet must be explicitly enabled (using /servlet is forbidden),
 * and it must have the init-param enable set to "read" or "write".
 * (There will likely be a future additional requirement of satisfying
 * a role.)
 */
public class ResinStatusServlet extends GenericServlet {
  static final protected Logger log = Log.open(ResinStatusServlet.class);
  static final L10N L = new L10N(ResinStatusServlet.class);

  private static final long SECOND = 1000L;
  private static final long MINUTE = 60 * SECOND;
  private static final long HOUR = 60 * MINUTE;
  private static final long DAY = 24 * HOUR;

  private String _enable;

  private MBeanServer _mbeanServer;
  private ResinMBean _resin;
  private ServerMBean _server;

  /**
   * Set to read or write.
   */
  public void setEnable(String enable)
    throws ConfigException
  {
    if ("read".equals(enable) || "write".equals(enable))
      _enable = enable;
    else
      throw new ConfigException(L.l("enable value '{0}' must either be read or write.",
                                    enable));
  }

  /**
   * Initialize the servlet with the server's sruns.
   */
  public void init()
    throws ServletException
  {
    if (_enable == null)
      throw new ServletException(L.l("ResinStatusServlet requires an explicit enable attribute."));

    try {
      //_mbeanServer = (MBeanServer) new InitialContext().lookup("java:comp/jmx/GlobalMBeanServer");

      // _mbeanServer = Jmx.findMBeanServer();

      // _mbeanServer = Jmx.getMBeanServer();
      _mbeanServer =
        Jmx.getGlobalMBeanServer();

      //_resinServer = (ResinServerMBean) Jmx.find("resin:type=ResinServer");
      //_servletServer = (ServletServerMBean) Jmx.find("resin:name=default,type=Server");

      _resin = (ResinMBean) Jmx.findGlobal("resin:type=Resin");
      _server = (ServerMBean) Jmx.findGlobal("resin:name=default,type=Server");
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  /**
   * Handle the request.
   */
  public void service(ServletRequest request, ServletResponse response)
    throws IOException, ServletException
  {
    try {
      HttpServletRequest req = (HttpServletRequest) request;
      HttpServletResponse res = (HttpServletResponse) response;

      res.setContentType("text/html");

      Thread thread = Thread.currentThread();
      thread.setContextClassLoader(ClassLoader.getSystemClassLoader());

      PrintWriter out = res.getWriter();

      printHeader(out);

      String hostName = req.getParameter("host");
      String appName = req.getParameter("app");

      printServerHeader(out);
      printPorts(out);
      printSrun(out);
      /*
	printJNDI(out, _server.getJndiContext());
      */
      printApplicationSummary(out, req.getRequestURI());
      printFooter(out);
    } catch (IOException e) {
      throw e;
    } catch (ServletException e) {
      throw e;
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

    /*
  private void printApplication(PrintWriter out,
                                ApplicationAdmin app,
                                String pwd)
    throws IOException, ServletException
  {
    ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();

    try {
      Thread.currentThread().setContextClassLoader(app.getClassLoader());

      printHeader(out);

      printApplicationHeader(out, app, pwd);
      printJNDI(out, app.getJndiContext());

      printJMXServlets(out, app.getMBeanServer());

      printFooter(out);
    } finally {
      Thread.currentThread().setContextClassLoader(oldLoader);
    }
  }
    */

  /**
   * Prints generic server information.
   */
  public void printHeader(PrintWriter out)
    throws IOException, ServletException
  {
  }

  /**
   * Prints server information.
   */
  public void printServerHeader(PrintWriter out)
    throws Exception
  {
    out.println("<b>resin-status</b><br><br>");

    String id = _resin.getServerId();

    out.println("<table border=\"0\">");
    if (id != null)
      out.println("<tr><td><b>Server:</b><td>" + id);

    String configFile = _resin.getConfigFile();
    if (configFile != null)
      out.println("<tr><td><b>Config:</b><td>" + configFile);

    long initialStartTime = _resin.getInitialStartTime().getTime();
    long startTime = _resin.getStartTime().getTime();

    out.println("<tr><td><b>Server Start:</b><td>" +
                QDate.formatLocal(initialStartTime));
    out.println("<tr><td><b>Server Reload:</b><td> " +
                QDate.formatLocal(startTime));

    long totalMemory = Runtime.getRuntime().totalMemory();
    out.println("<tr><td><b>Total Memory:</b><td> " +
                (totalMemory / 1000000) + "." +
                (totalMemory / 100000) % 10 +
                (totalMemory / 10000) % 10 +
                "Meg");
    long freeMemory = Runtime.getRuntime().freeMemory();
    out.println("<tr><td><b>Free Memory:</b><td> " +
                (freeMemory / 1000000) + "." +
                (freeMemory / 100000) % 10 +
                (freeMemory / 10000) % 10 +
                "Meg");

    long invocationHitCount = _server.getInvocationCacheHitCount();
    long invocationMissCount = _server.getInvocationCacheMissCount();

    long totalCount = invocationHitCount + invocationMissCount;
    if (totalCount == 0)
      totalCount = 1;

    long hitRatio = (10000 * invocationHitCount) / totalCount;

    out.print("<tr><td><b>Invocation Hit Ratio:</b><td> " +
              (hitRatio / 100) + "." +
              (hitRatio / 10) % 10 +
              (hitRatio) % 10 + "%");
    out.println(" (" + invocationHitCount + "/" + totalCount + ")");

    long proxyHitCount = _server.getProxyCacheHitCount();
    long proxyMissCount = _server.getProxyCacheMissCount();

    totalCount = proxyHitCount + proxyMissCount;
    if (totalCount == 0)
      totalCount = 1;

    hitRatio = (10000 * proxyHitCount) / totalCount;

    out.print("<tr><td><b>Proxy Cache Hit Ratio:</b><td> " +
              (hitRatio / 100) + "." +
              (hitRatio / 10) % 10 +
              (hitRatio) % 10 + "%");
    out.println(" (" + proxyHitCount + "/" + totalCount + ")");

    out.println("</table>");

    printThreadHeader(out);
    printConnectionPools(out, "");
  }

  /**
   * Prints thread information.
   */
  public void printThreadHeader(PrintWriter out)
    throws Exception
  {
    out.println("<table border='3'>");

    ThreadPoolMBean threadPool = (ThreadPoolMBean) Jmx.findGlobal("resin:type=ThreadPool");
    out.println("<tr><th colspan='3'>Threads");
    out.println("    <th colspan='3'>Config");
    out.println("<tr><th>Active<th>Idle<th>Total");
    out.println("    <th>thread-max<th>spare-thread-min");
    out.println("<tr align='right'>");
    out.println("    <td>" + threadPool.getActiveThreadCount());
    out.println("    <td>" + threadPool.getIdleThreadCount());
    out.println("    <td>" + threadPool.getThreadCount());

    out.println("    <td>" + threadPool.getThreadMax());
    out.println("    <td>" + threadPool.getSpareThreadMin());

    out.println("</table>");
  }

  /**
   * Prints application information.
   */
  /*
  public void printApplicationHeader(PrintWriter out,
                                     ApplicationAdmin app,
                                     String pwd)
    throws IOException, ServletException
  {
    HostAdmin host = app.getHostAdmin();

    out.println("<b><a href=\"" + pwd + "\">resin-status</a> > ");
    out.println("<a href=\"" + pwd +
                "?host=" + host.getName() + "\">" + host.getURL() + "</a> > ");
    out.println(app.getContextPath() + "</b><br><br>");

    String id = _server.getServerId();

    out.println("<table border=\"0\">");
    if (id != null)
      out.println("<tr><td><b>Server:</b><td>" + id);
    Path config = _server.getConfig();
    if (config != null)
      out.println("<tr><td><b>Config:</b><td>" + config.getNativePath());

    out.println("<tr><td><b>Host:</b><td>" + app.getHostAdmin().getURL());
    out.println("<tr><td><b>Web-App:</b><td>" + app.getContextPath());
    out.println("<tr><td><b>App-Dir:</b><td>" + app.getAppDir().getNativePath());

    long startTime = _server.getStartTime();
    long restartTime = app.getStartTime();

    out.println("<tr><td><b>Server Start:</b><td>" + QDate.formatLocal(startTime));
    out.println("<tr><td><b>Web-App Start:</b><td> " + QDate.formatLocal(restartTime));
    long totalMemory = Runtime.getRuntime().totalMemory();
    out.println("<tr><td><b>Total Memory:</b><td> " +
                (totalMemory / 1000000) + "." +
                (totalMemory / 100000) % 10 +
                (totalMemory / 10000) % 10 +
                "Meg");
    long freeMemory = Runtime.getRuntime().freeMemory();
    out.println("<tr><td><b>Free Memory:</b><td> " +
                (freeMemory / 1000000) + "." +
                (freeMemory / 100000) % 10 +
                (freeMemory / 10000) % 10 +
                "Meg");

    out.println("<tr><td><b>Sessions:</b><td>" +
                app.getActiveSessionCount());

    out.println("</table>");
  }
  */

  public void printPorts(PrintWriter out)
    throws IOException, ServletException
  {
    try {
      String []portList = _server.getPortObjectNames();

      if (portList.length > 0) {
        out.println("<h3>TCP ports</h3>");
        out.println("<table border='2'>");
        out.println("<tr><th><th colspan='3'>Threads<th>&nbsp;");
        out.println("<tr><th>Protocol:Port");
        out.println("    <th>Active<th>Idle<th>Total");
        out.println("    <th>Keepalive<th>Select");

        for (int i = 0; i < portList.length; i++) {
          PortMBean port = (PortMBean) Jmx.findGlobal(portList[i]);

          if (port == null || ! "active".equals(port.getState()))
            continue;

          String host = port.getHost();
          if (host == null)
            host = "*";

          out.print("<tr><td>");
          out.print(port.getProtocolName() + "://" + host + ":" + port.getPort());
          out.println();

          out.print("    <td>" + port.getActiveThreadCount());
          out.print("<td>" + port.getIdleThreadCount());
          out.print("<td>" + port.getActiveThreadCount());
          out.print("<td>" + port.getKeepaliveThreadCount());
          out.print("<td>" + port.getKeepaliveSelectCount());
          out.println();

          out.println();
        }
        out.println("</table>");
      }
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  public void printSrun(PrintWriter out)
    throws IOException, ServletException
  {
    try {
      String[]clusterList = _server.getClusterObjectNames();

      for (int i = 0; i < clusterList.length; i++) {
        ClusterMBean cluster = (ClusterMBean) Jmx.findGlobal(clusterList[i]);

        if (cluster == null) {
          out.println("<h3>Cluster " + clusterList[i] + " null</h3>");
          continue;
        }

        ObjectName objectName = new ObjectName(cluster.getObjectName());

        String clusterName = objectName.getKeyProperty("name");

        out.println("<h3>Cluster " + clusterName + "</h3>");
        out.println("<table border='2'>");
        out.println("<tr><th>Host");
        out.println("    <th>Active");

        String []srunNames = cluster.getClientObjectNames();

        for (int j = 0; j < srunNames.length; j++) {
          ObjectName srunName = new ObjectName(srunNames[j]);

          ClusterClientMBean client;
          client = (ClusterClientMBean) Jmx.findGlobal(srunName);

          String host = srunName.getKeyProperty("host");
          String port = srunName.getKeyProperty("port");

          out.println("<tr>");

          boolean canConnect = client.ping();

          if (canConnect)
            out.print("<td bgcolor='#80ff80'>");
          else
            out.print("<td>");

          out.print(host + ":" + port);

          if (canConnect)
            out.println(" (up)");
          else
            out.println(" (down)");

          out.println("<td>" + client.getActiveCount());
        }

        out.println("</table>");
      }
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  public void printConnectionPools(PrintWriter out, String context)
    throws Exception
  {
    ObjectName pattern = new ObjectName("resin:*,type=ConnectionPool" + context);

    Set<ObjectName> poolNames;
    poolNames = _mbeanServer.queryNames(pattern, null);

    if (poolNames.size() == 0)
      return;

    out.println("<h3>Connection Pools</h3>");

    out.println("<table border='2'>");
    out.println("<tr><th>&nbsp;<th colspan='3'>Connections<th colspan='2'>Config");
    out.println("<tr><th>Name<th>Active<th>Idle<th>Total");
    out.println("    <th>max-connections<th>idle-time");

    Iterator<ObjectName> iter = poolNames.iterator();
    while (iter.hasNext()) {
      ObjectName name = iter.next();

      ConnectionPoolMBean pool = (ConnectionPoolMBean) Jmx.findGlobal(name);

      if (pool != null) {
        out.println("<tr><td>" + pool.getName());
        out.println("    <td>" + pool.getActiveConnectionCount());
        out.println("    <td>" + pool.getIdleConnectionCount());
        out.println("    <td>" + pool.getConnectionCount());

        out.println("    <td>" + pool.getMaxConnections());
        out.println("    <td>" + periodToString(pool.getMaxIdleTime()));
      }
    }

    out.println("</table>");
  }

  private String periodToString(long time)
  {
    if (time == 0)
      return "0s";
    else if (time % DAY == 0)
      return (time / DAY) + "d";
    else if (time % HOUR == 0)
      return (time / HOUR) + "h";
    else if (time % MINUTE == 0)
      return (time / MINUTE) + "min";
    else if (time % SECOND == 0)
      return (time / SECOND) + "s";
    else
      return time + "ms";
  }

  /*
  public void printSrun(PrintWriter out)
    throws IOException, ServletException
  {
    DistributionServer []srunList = _server.getDistributionServerList();

    if (srunList == null || srunList.length == 0)
      return;

    out.println("<h3>Srun Servers</h3>");
    out.println("<table border=2>");
    out.println("<tr><th>Host</th><th>Active Count</th><th>live-time</th><th>dead-time</th><th>request-timeout</th>");

    for (int i = 0; i < srunList.length; i++) {
      DistributionServer srun = srunList[i];
      out.print("<tr>");

      boolean isLive = false;
      try {
        ReadWritePair pair = srun.open();
        if (pair != null) {
          isLive = true;
          srun.free(pair);
        }
      } catch (Throwable e) {
        dbg.log(e);
      }

      if (isLive) {
        out.println("<td bgcolor=\"#66ff66\">" +
                    (srun.getIndex() + 1) + ". " +
                    srun.getHost() + ":" + srun.getPort() +
                    (srun.isBackup() ? "*" : "") +
                    " (ok)");
      }
      else {
        out.println("<td bgcolor=\"#ff6666\">" +
                    (srun.getIndex() + 1) + ". " +
                    srun.getHost() + ":" + srun.getPort() +
                    (srun.isBackup() ? "*" : "") +
                    " (down)");
      }
      out.println("<td>" + srun.getActiveCount());
      out.println("<td>" + srun.getLiveTime() / 1000);
      out.println("<td>" + srun.getDeadTime() / 1000);
      out.println("<td>" + srun.getTimeout() / 1000);
    }
    out.println("</table>");
  }

  public void printJNDI(PrintWriter out, Context ic)
    throws IOException, ServletException
  {
    printDatabasePools(out, ic);
    printEJBLocalHomes(out, ic);
    printEJBRemoteHomes(out, ic);
  }

  public void printEJBLocalHomes(PrintWriter out, Context ic)
    throws IOException, ServletException
  {
    try {
      Context cmpCxt = (Context) ic.lookup("java:comp/env/cmp");

      if (cmpCxt == null)
        return;

      NamingEnumeration list = cmpCxt.list("");

      ArrayList cmpNames = new ArrayList();
      while (list.hasMoreElements()) {
        NameClassPair pair = (NameClassPair) list.nextElement();

        cmpNames.add(pair.getName());
      }

      if (cmpNames.size() == 0)
        return;

      out.println("<h3>EJB Local Home</h3>");

      out.println("<table border=\"2\">");
      out.println("<tr><th>Name<th>Home Stub Class");

      Collections.sort(cmpNames);

      for (int i = 0; i < cmpNames.size(); i++) {
        String name = (String) cmpNames.get(i);

        Object value = cmpCxt.lookup(name);
        if (! (value instanceof EJBLocalHome))
          continue;

        EJBLocalHome home = (EJBLocalHome) value;
        out.print("<tr><td>cmp/" + name);
        Class homeStub = home.getClass();
        Class []interfaces = home.getClass().getInterfaces();
        for (int j = 0; j < interfaces.length; j++) {
          if (EJBLocalHome.class.isAssignableFrom(interfaces[j])) {
            homeStub = interfaces[j];
            break;
          }
        }
        out.print("<td>" + homeStub.getName());
      }
      out.println("</table>");
    } catch (Exception e) {
      dbg.log(e);
    }
  }

  public void printEJBRemoteHomes(PrintWriter out, Context ic)
    throws IOException, ServletException
  {
    try {
      Context ejbCxt = (Context) ic.lookup("java:comp/env/ejb");

      if (ejbCxt == null)
        return;

      NamingEnumeration list = ejbCxt.list("");

      ArrayList ejbNames = new ArrayList();
      while (list.hasMoreElements()) {
        NameClassPair pair = (NameClassPair) list.nextElement();

        ejbNames.add(pair.getName());
      }

      if (ejbNames.size() == 0)
        return;

      out.println("<h3>EJB Home</h3>");

      out.println("<table border=\"2\">");
      out.println("<tr><th>Name<th>Home Stub Class");

      Collections.sort(ejbNames);

      for (int i = 0; i < ejbNames.size(); i++) {
        String name = (String) ejbNames.get(i);

        Object value = ejbCxt.lookup(name);
        if (! (value instanceof EJBHome))
          continue;

        EJBHome home = (EJBHome) value;
        out.print("<tr><td>ejb/" + name);
        Class homeStub = home.getClass();
        Class []interfaces = home.getClass().getInterfaces();
        for (int j = 0; j < interfaces.length; j++) {
          if (EJBHome.class.isAssignableFrom(interfaces[j])) {
            homeStub = interfaces[j];
            break;
          }
        }
        out.print("<td>" + homeStub.getName());
      }
      out.println("</table>");
    } catch (Exception e) {
      dbg.log(e);
    }
  }

  public void printJMXServlets(PrintWriter out, MBeanServer server)
    throws IOException, ServletException
  {
    try {
      ObjectName queryName = new ObjectName("*:j2eeType=Servlet,*");

      Set servlets = server.queryNames(queryName, null);

      if (servlets.size() == 0)
        return;

      out.println("<h3>Servlets</h3>");

      Iterator iter = servlets.iterator();
      while (iter.hasNext()) {
        ObjectName servletName = (ObjectName) iter.next();

        String name = servletName.getKeyProperty("name");
        MBeanInfo mbeanInfo = server.getMBeanInfo(servletName);
        MBeanAttributeInfo []attrs = mbeanInfo.getAttributes();

        out.println("<table border=\"2\">");
        out.print("<tr><th>Name</th>");

        for (int i = 0; i < attrs.length; i++)
          out.print("<th>" + attrs[i].getName() + "</th>");
        out.println("</tr>");

        out.print("<tr><td>" + name + "</td>");
        for (int i = 0; i < attrs.length; i++) {
          Object value = server.getAttribute(servletName, attrs[i].getName());

          out.print("<td>" + value + "</td>");
        }

        out.println("</table>");
      }
    } catch (Exception e) {
      dbg.log(e);
    }
  }
  */

  public void printApplicationSummary(PrintWriter out, String pwd)
    throws Exception
  {
    out.println("<h3>Hosts and Applications</h3>");

    out.println("<table border=\"2\">");
    out.println("<tr><th>Host<th>Web-App<th>State<th>Sessions");

    ObjectName hostPattern = new ObjectName("resin:*,type=Host");

    Set<ObjectName> names = _mbeanServer.queryNames(hostPattern, null);
    Iterator<ObjectName> iter = names.iterator();

    ArrayList<HostMBean> hosts = new ArrayList<HostMBean>();

    while (iter.hasNext()) {
      ObjectName name = iter.next();

      // the Host with name=current is a duplicate
      if ("current".equals(name.getKeyProperty("name")))
        continue;

      HostMBean host = (HostMBean) Jmx.findGlobal(name);

      if (host != null) {
        hosts.add(host);
      }
    }

    Collections.sort(hosts, new HostCompare());

    for (int i = 0; i < hosts.size(); i++) {
      HostMBean host = hosts.get(i);

      out.println("<tr><td><b>" + host.getURL() + "</b>");

      // thread.setContextClassLoader(hostLoader);

      String hostName = host.getHostName();
      if (hostName.equals(""))
        hostName = "default";

      ObjectName appPattern = new ObjectName("resin:*,Host=" + hostName + ",type=WebApp");

      names = _mbeanServer.queryNames(appPattern, null);
      iter = names.iterator();

      ArrayList<WebAppMBean> apps = new ArrayList<WebAppMBean>();

      while (iter.hasNext()) {
        ObjectName name = iter.next();

        try {
          WebAppMBean app = (WebAppMBean) Jmx.findGlobal(name);

          if (app != null)
            apps.add(app);
        } catch (Throwable e) {
          log.log(Level.WARNING, e.toString());
          out.println("<tr><td>" + name + "<td>" + e.toString());
        }
      }

      Collections.sort(apps, new AppCompare());

      for (int j = 0; j < apps.size(); j++) {
        WebAppMBean app = apps.get(j);

        String contextPath = app.getContextPath();

        if (contextPath.equals(""))
          contextPath = "/";

        out.print("<tr><td><td>");
        out.print("<a href=\"" + pwd + "?host=" + host.getHostName() +
                  "&app=" + app.getContextPath() + "\">");
        out.print(contextPath);
        out.print("</a>");

        String state = app.getState();
        if (state.equals("active"))
          out.print("<td bgcolor='#80ff80'>" + app.getState());
        else
          out.print("<td>" + app.getState());
        out.print("<td>" + app.getActiveSessionCount());
      }
    }

    out.println("</table>");
  }

  public void printVirtualHosts(PrintWriter out)
    throws IOException, ServletException
  {
  }

  /**
   * Prints footer information.
   */
  public void printFooter(PrintWriter out)
    throws IOException, ServletException
  {
    /*
    if (_server.isTesting())
      out.println("<br><em>Resin test</em>");
    else
    */
      out.println("<br><em>" + com.caucho.Version.FULL_VERSION + "</em>");
  }

  static class HostCompare implements Comparator<HostMBean> {
    public int compare(HostMBean a, HostMBean b)
    {
      String urlA = a.getURL();
      String urlB = b.getURL();

      if (urlA == urlB)
        return 0;
      else if (urlA == null)
        return -1;
      else if (urlB == null)
        return 1;
      else
        return urlA.compareTo(urlB);
    }
  }

  static class AppCompare implements Comparator<WebAppMBean> {
    public int compare(WebAppMBean a, WebAppMBean b)
    {
      String cpA = a.getContextPath();
      String cpB = b.getContextPath();

      return cpA.compareTo(cpB);
    }
  }
}
