<?php
  $timeformat = "%a %b %e %H:%M:%S %Z %Y";

  function format_memory($memory)
  {
    return sprintf("%.2fMeg", $memory / (1024 * 1024))
  }

  $resin = mbean_lookup("resin:type=ResinServer");
  $server = mbean_lookup("resin:name=default,type=Server");

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

<h2>Server <?= $resin->serverId ?></h2>

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

<h2>Connections</h2>

<table>
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

<table>

<tr>
<th>Host</th>
<th>Status</th>
<th>Active</th>
</tr>
<?php
  foreach ($cluster->clientObjectNames as $clientObjectName) {
    $client = mbean_lookup($clientObjectName);
    $clientParts = mbean_explode($clientObjectName);
?>

<tr>
<td class='<?= $client->canConnect() ? "active" : "inactive" ?>'><?= $clientParts["host"] ?>:<?= $clientParts["port"] ?></td>
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

  $hostObjectNames = mbean_query("resin:*,type=Host");

  $hosts = array();

  foreach ($hostObjectNames as $hostObjectName) {
    $hosts[] = mbean_lookup($hostObjectName);
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

  $webappObjectNames = mbean_query($webappPattern);

  $webapps = array();

  foreach ($webappObjectNames as $webappObjectName) {
    $webapps[] = mbean_lookup($webappObjectName);
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
