<?php
  $timeformat = "%a %b %e %H:%M:%S %Z %Y";

  function format_memory($memory)
  {
    return sprintf("%.2fMeg", $memory / (1024 * 1024))
  }

  function sort_name($a, $b)
  {
    return strcmp($a->name, $b->name);
  }

  $resin = mbean_lookup("resin:type=ResinServer");
  $server = mbean_lookup("resin:name=default,type=Server");
?>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Strict//EN" "http://www.w3.org/TR/html4/strict.dtd">

<html>
<head>
  <title>resin-info</title>
</head>

<body>
<h1>resin-info</h1>

<table border='0'>

<tr>
<th>Server id:</th>
<td><?= $resin->serverId ?></td>
</tr>

<tr>
<th>Conf file:</th>
<td><?= $resin->configFile ?></td>
</tr>

<tr>
<th>Server start:</th>
<td><?= strftime($timeformat, $resin->initialStartTime->time) ?></td>
</tr>

<tr>
<th>Server reload:</th>
<td><?= strftime($timeformat, $resin->startTime->time) ?></td>
</tr>

<tr>
<th>Total memory:</th>
<td><?= format_memory($resin->totalMemory) ?></td>
</tr>

<tr>
<th>Free memory:</th>
<td><?= format_memory($resin->freeMemory) ?></td>
</tr>

<?php
$proxyHitCount = $server->proxyCacheHitCount;
$proxyMissCount = $server->proxyCacheMissCount;
$totalCount = max(1, $proxyHitCount + $proxyMissCount);
$hitRatio = (10000 * $proxyHitCount) / $totalCount;
?>

<tr>
<th>Proxy cache hit ratio:</th>
<td><?= sprintf("%.2f%% (%d / %d)", $hitRatio / 100, $proxyHitCount, $totalCount) ?></td>
</tr>

<?php
$invocationHitCount = $server->invocationCacheHitCount;
$invocationMissCount = $server->invocationCacheMissCount;
$totalCount = max(1, $invocationHitCount + $invocationMissCount);
$hitRatio = (10000 * $invocationHitCount) / $totalCount;
?>

<tr>
<th>Invocation hit ratio:</th>
<td><?= sprintf("%.2f%% (%d / %d)", $hitRatio / 100, $invocationHitCount, $totalCount) ?></td>
</tr>

</table>

<?php
  $threadPool = mbean_lookup("resin:type=ThreadPool");
?>

<table border='3'>
<tr>
<th colspan='3'>Threads</th>
<th colspan='2'>Config</th>
</tr>
<tr>
<th>Active count</th>
<th>Idle count</th>
<th>Total count</th>
<th>thread-max</th>
<th>spare-thread-min</th>
</tr>
<tr align='right'>
<td><?= $threadPool->activeThreadCount ?></td>
<td><?= $threadPool->idleThreadCount ?></td>
<td><?= $threadPool->threadCount ?></td>
<td><?= $threadPool->threadMax ?></td>
<td><?= $threadPool->spareThreadMin ?></td>
</tr>
</table>

<!-- Connection pools -->

<?php
  $poolObjectNames = mbean_query("resin:*,type=ConnectionPool");

  if ($poolObjectNames) {
?>

<h2>Connection pools</h2>

<table border='2'>
<tr>
<th>&nbsp;</th>
<th colspan='3'>Connections</th>
<th colspan='2'>Config</th>
</tr>
<tr>
<th>Name</th>
<th>Active count</th>
<th>Idle count</th>
<th>Total count</th>
<th>max-connections</th>
<th>idle-time</th>
</tr>

<?php
    foreach ($poolObjectNames as $poolObjectName) {
      $pool = mbean_lookup($poolObjectName);
?>

<tr>
<td><?= $pool->name ?></td>
<td><?= $pool->activeConnectionCount ?></td>
<td><?= $pool->idleConnectionCount ?></td>
<td><?= $pool->connectionCount ?></td>
<td><?= $pool->maxConnections ?></td>
<td><?= $pool->maxIdleTime ?></td>
</tr>

<?php
  }
}
?>
</table>

<!-- TCP ports -->

<?php
  $portObjectNames = $server->portObjectNames;

  if ($portObjectNames) {
?>
<h2>TCP ports</h2>

<table border='2'>
<tr>
<th colspan='2'>&nbsp;</th>
<th colspan='3'>Threads</th>
<th>&nbsp;</th>

<tr>
<th>&nbsp;</th>
<th>Status</th>
<th>Active count</th>
<th>Idle count</th>
<th>Total count</th>
<th>Keepalive count</th>
<th>Select count</th>
</tr>
<?php
  foreach ($portObjectNames as $portObjectName) {
    $port = mbean_lookup($portObjectName);
?>

<tr>
<td><?= $port->protocolName ?>://<?= $port->host ? $port->host : "*" ?>:<?= $port->port ?></td>
<td><?= $port->active ? "active" : "inacative" ?></td>
<td><?= $port->activeThreadCount ?></td>
<td><?= $port->idleThreadCount ?></td>
<td><?= $port->threadCount ?></td>
<td><?= $port->keepaliveCount ?></td>
<td><?= $port->selectConnectionCount < 0 ? "N/A" : $port->selectConnectionCount ?></td>
</tr>
<?php 
  }
}
?>
</table>

<!-- Cluster -->

<?php
  foreach ($server->clusterObjectNames as $clusterObjectName) {
    $cluster = mbean_lookup($clusterObjectName);
?>

<h2>Cluster <?= $clusterObjectName->name ?></h2>

<table border='2'>

<tr>
<th>Host</th>
<th>Status</th>
<th>Active</th>
</tr>
<?php
  foreach ($cluster->clientObjectNames as $clientObjectName) {
    $client = mbean_lookup($clientObjectName);
?>

<tr>
<td class='<?= $client->canConnect() ? "active" : "inactive" ?>'><?= $clientObjectName->host ?>:<?= $clientObjectName->port ?></td>
<td><?= $client->canConnect() ? "up" : "down" ?></td>
<td><?= $client->activeCount ?></td>
</tr>
<?php 
}
?>

</table>
<?php 
}
?>

<!-- Hosts and Applications -->
<h3>Hosts and Applications</h3>

<table border='2'>
<tr>
<th>Host</th>
<th>Web-App</th>
<th>State</th>
<th>Sessions</th>
</tr>
<?php
  $hostObjectNames = mbean_query("resin:*,type=Host");

  // XXX: sort by URL
  usort($hostObjectNames, "sort_name");

  foreach ($hostObjectNames as $hostObjectName) {
    $host = mbean_lookup($hostObjectName);
?>

<tr title='<?= $hostObjectName ?>'><td colspan='4'><?= $host->URL ?></td></tr>
<?php
  $hostName = empty($host->hostName) ? "default" : $host->hostName;
  $webappPattern = "resin:*,Host=" . $hostName . ",type=WebApp";

  $webappObjectNames = mbean_query($webappPattern);

  usort($webappObjectNames, "sort_name");

  foreach ($webappObjectNames as $webappObjectName) {
    $webapp = mbean_lookup($webappObjectName);
?>

<tr class="<?= $webapp->state ?>" title='<?= $webappObjectName ?>'>
<td>&nbsp;</td>
<td><?= empty($webapp->contextPath) ? "/" : $webapp->contextPath ?>
<td><?= $webapp->state ?>
<td><?= $webapp->activeSessionCount ?>
</tr>
<?php
    } // webapps
  } // hosts
?>

</table>

<!-- Footer -->
<p>
<em><?= resin_version() ?></em>
</p>

</body>
</html>

<?php 
  $capture = <<<EOD
      out.println("<h3>Hosts and Applications</h3>");

      out.println("<table border=\"2\">");
      out.println("<tr><th>Host<th>Web-App<th>State<th>Sessions");

      ObjectName hostPattern = new ObjectName("resin:*,type=Host");

      Set<ObjectName> names = _mbeanServer.queryNames(hostPattern, null);
      Iterator<ObjectName> iter1 = names.iterator();

      ArrayList<HostMBean> hosts = new ArrayList<HostMBean>();

      while (iter1.hasNext()) {
        ObjectName name1 = iter1.next();

        // the Host with name=current is a duplicate
        if ("current".equals(name1.getKeyProperty("name")))
          continue;

        HostMBean host1 = (HostMBean) Jmx.findGlobal(name1);

        if (host1 != null) {
          hosts.add(host1);
        }
      }

      Collections.sort(hosts, new HostCompare());

      for (int i1 = 0; i1 < hosts.size(); i1++) {
        HostMBean host1 = hosts.get(i1);

        out.println("<tr><td><b>" + host1.getURL() + "</b>");

        // thread.setContextClassLoader(hostLoader);

        String hostName1 = host1.getHostName();
        if (hostName1.equals(""))
          hostName1 = "default";

        ObjectName appPattern = new ObjectName("resin:*,Host=" + hostName1 + ",type=WebApp");

        names = _mbeanServer.queryNames(appPattern, null);
        iter1 = names.iterator();

        ArrayList<WebAppMBean> apps = new ArrayList<WebAppMBean>();

        while (iter1.hasNext()) {
          ObjectName name1 = iter1.next();

          try {
            WebAppMBean app = (WebAppMBean) Jmx.findGlobal(name1);

            if (app != null)
              apps.add(app);
          } catch (Throwable e1) {
            ResinStatusBogus.log.log(Level.WARNING, e1.toString());
            out.println("<tr><td>" + name1 + "<td>" + e1.toString());
          }
        }

        Collections.sort(apps, new AppCompare());

        for (int j = 0; j < apps.size(); j++) {
          WebAppMBean app = apps.get(j);

          String contextPath = app.getContextPath();

          if (contextPath.equals(""))
            contextPath = "/";

          out.print("<tr><td><td>");
          out.print("<a href=\"" + req.getRequestURI() + "?host=" + host1.getHostName() +
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
      /*
      if (_server.isTesting())
        out.println("<br><em>Resin test</em>");
      else
      */
    out.println("<br><em>" + com.caucho.Version.FULL_VERSION + "</em>");
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

  private String periodToString(long time)
  {
    if (time == 0)
      return "0s";
    else if (time % ResinStatusBogus.DAY == 0)
      return (time / ResinStatusBogus.DAY) + "d";
    else if (time % ResinStatusBogus.HOUR == 0)
      return (time / ResinStatusBogus.HOUR) + "h";
    else if (time % ResinStatusBogus.MINUTE == 0)
      return (time / ResinStatusBogus.MINUTE) + "min";
    else if (time % ResinStatusBogus.SECOND == 0)
      return (time / ResinStatusBogus.SECOND) + "s";
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
EOD
?>
