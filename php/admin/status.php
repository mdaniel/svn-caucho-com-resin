<?php
  $mbeanServer = new MBeanServer();

  function format_datetime($date)
  {
    return strftime("%a %b %d %H:%M:%S %Z %Y", $date->time / 1000);
  }

  function format_memory($memory)
  {
    return sprintf("%.2fMeg", $memory / (1024 * 1024))
  }

  function format_hit_ratio($hit, $miss)
  {
    $total = $hit + $miss;

    if ($total == 0)
      return "0.00% (0 / 0)";
    else
      return sprintf("%.2f%% (%d / %d)", 100 * $hit / $total, $hit, $total);
  }

  $resin = $mbeanServer->lookup("resin:type=Resin");
  $domain = $mbeanServer->lookup("resin:name=CurrentDomain");
  $server = $domain;

  $title = "Resin status";

  if (! empty($resin->serverId)) {
    $title = $title . " for server " . $resin->serverId;
  }
?>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Strict//EN" "http://www.w3.org/TR/html4/strict.dtd">

<html>
<head>
  <title><?= $title ?></title>
  <link href="status.css" rel="stylesheet" type="text/css" />
  <link rel="shortcut icon" href="dragonfly.ico">
  <link rel="icon" type="image/png" href="dragonfly-tiny.png">
</head>

<body>
<h1><?= $title ?></h1>

<table>

<h2>Server: <?= $resin->serverId ?></h2>


 <?php if (! empty($resin->serverId)) {  ?>
<tr title="The server id used when starting this instance of Resin, the value of `-server'.">
<th>Server id:</th>
<td><?= $resin->serverId ?></td>
</tr>
<? } ?>

<tr title="The configuration file used when starting this instance of Resin, the value of `-conf'.">
<th>Config file:</th>
<td><?= $resin->configFile ?></td>
</tr>

<tr title="The Resin home directory used when starting this instance of Resin. This is the location of the Resin program files.">
<th>Resin home:</th>
<td><?= $resin->resinHome ?></td>
</tr>

<tr title="The server root directory used when starting this instance of Resin. This is the root directory of the web server files.">
<th>Server root:</th>
<td><?= $resin->serverRoot ?></td>
</tr>

<tr title="The ip address of the machine that is running this instance of Resin.">
<th>Local host:</th>
<td><?= $resin->localHost ?></td>
</tr>

<tr title="The current lifecycle state">
<th>State:</th>
<td><?= $resin->state ?></td>
</tr>

<tr title="The time that this instance was first started.">
<th>Inital start time:</th>
<td><?= format_datetime($resin->initialStartTime) ?></td>
</tr>

<tr title="The time that this instance was last started or restarted.">
<th>Start time:</th>
<td><?= format_datetime($resin->startTime) ?></td>
</tr>

<tr title="The current total amount of memory available for the JVM, in bytes.">
<th>Total memory:</th>
<td><?= format_memory($resin->totalMemory) ?></td>
</tr>

<tr title="The current free amount of memory available for the JVM, in bytes.">
<th>Free memory:</th>
<td><?= format_memory($resin->freeMemory) ?></td>
</tr>

<tr title="Percentage of requests that have been served from the proxy cache:">
<th>Proxy cache hit ratio:</th>
<td><?= format_hit_ratio($server->proxyCacheHitCount, $server->proxyCacheMissCount) ?></td>
</tr>

<!-- XXX: show how cacheable apps are: cacheable/non-cacheable -->

<tr>
<th>Invocation hit ratio:</th>
<td><?= format_hit_ratio($server->invocationCacheHitCount, $server->invocationCacheMissCount) ?></td>
</tr>

</table>

<?php
  $threadPool = $mbeanServer->lookup("resin:type=ThreadPool");
?>

<!--
"Restart" - "Exit this instance cleanly and allow the wrapper script to start a new JVM."
-->

<h2>Thread pool</h2>

<div class="description">
The ThreadPool manages all threads used by Resin.
</div>

<table>
<tr>
<th colspan='2'>Config</th>
<th colspan='3'>Threads</th>
</tr>
<tr>
<th title="The maximum number of threads that Resin can allocate.">thread-max</th>
<th title="The minimum number of threads Resin should have available for new requests or other tasks.  This value causes a minimum number of idle threads, useful for situations where there is a sudden increase in the number of threads required.">spare-thread-min</th>
<th title="The number of active threads. These threads are busy servicing requests or performing other tasks.">Active thread count</th>
<th title="The number of idle threads. These threads are allocated but inactive, available for new requests or tasks.">Idle thread count</th>
<th title="The current total number of threads managed by the pool.">Total count</th>
</tr>
<tr align='right'>
<td><?= $threadPool->threadMax ?></td>
<td><?= $threadPool->spareThreadMin ?></td>
<td><?= $threadPool->activeThreadCount ?></td>
<td><?= $threadPool->idleThreadCount ?></td>
<td><?= $threadPool->threadCount ?></td>
</tr>
</table>

<!-- Connection pools -->

<?php
  $poolObjectNames = $mbeanServer->query("resin:*,type=ConnectionPool");

  if ($poolObjectNames) {
?>

<h2>Connection pools</h2>

<table>
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
    $pool = $mbeanServer->lookup($poolObjectName);
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
  $portObjectNames = $server->PortObjectNames;

  if ($portObjectNames) {
?>
<h2>TCP ports</h2>

<table>
<tr>
<th colspan='2'>&nbsp;</th>
<th colspan='3'>Threads</th>
<th colspan='2'>&nbsp;</th>

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
    $port = $mbeanServer->lookup($portObjectName);
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
  # XXX: sort by $cluster->index
  foreach ($server->clusterObjectNames as $clusterObjectName) {
    $cluster = $mbeanServer->lookup($clusterObjectName);

    if (empty($cluster->clientObjectNames))
      continue;
?>

<h2>Cluster: <?= mbean_explode($clusterObjectName)['name'] ?></h2>

<table>

<tr>
<th>Id</th>
<th>Address</th>
<th>Status</th>
<th>Active</th>
<th>Idle</th>
<th>Connection</th>
</tr>
<?php
  foreach ($cluster->clientObjectNames as $clientObjectName) {
    $client = $mbeanServer->lookup($clientObjectName);
    $clientParts = $mbeanServer->explode($clientObjectName);
?>

<tr>
<td><?= $client->serverId ?></td>
<td class='<?= $client->canConnect() ? "active" : "inactive" ?>'>
<?= $clientParts["host"] ?>:<?= $clientParts["port"] ?></td>
<?php
if ($client->canConnect())
  echo "<td style='background:#66ff66'>up</td>\n";
else
  echo "<td style='background:#ff6666'>down</td>\n";
?>
<td><?= $client->activeConnectionCount ?></td>
<td><?= $client->idleConnectionCount ?></td>
<td><?= format_hit_ratio($client->lifetimeKeepaliveCount,
                         $client->lifetimeConnectionCount) ?></td>
</tr>
<?php 
}
?>

</table>
<?php 
}
?>

<!-- Hosts and Applications -->
<h2>Hosts and Applications</h2>

<table>
<tr>
<th>Host</th>
<th>Web-App</th>
<th>State</th>
<th>Sessions</th>
</tr>
<?php
  function sort_host($a, $b)
  {
    return strcmp($a->URL, $b->URL);
  }

  $hostObjectNames = $mbeanServer->query("resin:*,type=Host");

  $hosts = array();

  foreach ($hostObjectNames as $hostObjectName) {
    $hosts[] = $mbeanServer->lookup($hostObjectName);
  }

  usort($hosts, "sort_host");

  foreach ($hosts as $host) {
    $hostName = empty($host->hostName) ? "default" : $host->hostName;
?>

<tr title='<?= $hostObjectName ?>'><td colspan='4'><?= $host->URL ?></td></tr>
<?php
  function sort_webapp($a, $b)
  {
    return strcmp($a->contextPath, $b->contextPath);
  }

  $webappPattern = "resin:*,Host=" . $hostName . ",type=WebApp";

  $webappObjectNames = $mbeanServer->query($webappPattern);

  $webapps = array();

  foreach ($webappObjectNames as $webappObjectName) {
    $webapps[] = $mbeanServer->lookup($webappObjectName);
  }

  usort($webapps, "sort_webapp");

  foreach ($webapps as $webapp) {
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
