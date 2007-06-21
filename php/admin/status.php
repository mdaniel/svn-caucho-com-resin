<?php
/**
 * Provides the most important status information about the Resin server.
 *
 * @author Sam
 */


require_once "WEB-INF/php/inc.php";

$mbeanServer = new MBeanServer();

$resin = $mbeanServer->lookup("resin:type=Resin");
$server = $mbeanServer->lookup("resin:type=Server");

$title = "Resin: Status";

if (! empty($server->Id))
  $title = $title . " for server " . $server->Id;
?>

<?php display_header("status.php", $title, $server->Id) ?>

<h2>Server: <?= $server->Id ?></h2>

<table class="data">
  <tr title="The server id used when starting this instance of Resin, the value of `-server'.">
    <th>Server id:</th>
    <td><?= $server->Id ? $server->Id : '\"\"' ?></td>
  </tr>

  <tr title="The configuration file used when starting this instance of Resin, the value of `-conf'.">
    <th>Version:</th>
    <td><?= $resin->Version ?></td>
  </tr>

  <tr title="The configuration file used when starting this instance of Resin, the value of `-conf'.">
    <th>Config file:</th>
    <td><?= $resin->ConfigFile ?></td>
  </tr>

  <tr title="The Resin home directory used when starting this instance of Resin. This is the location of the Resin program files.">
  <th>Resin home:</th>
    <td><?= $resin->ResinHome ?></td>
  </tr>

  <tr title="The resin root directory used when starting this instance of Resin. This is the root directory of the web server files.">
    <th>Resin root:</th>
    <td><?= $resin->RootDirectory ?></td>
  </tr>

  <tr title="The ip address of the machine that is running this instance of Resin.">
    <th>Local host:</th>
    <td><?= $resin->LocalHost ?></td>
  </tr>

  <tr title="The current lifecycle state">
    <th>State:</th>
    <td class="<?= $server->State ?>"><?= $server->State ?></td>
  </tr>

  <tr title="The time that this instance was last started or restarted.">
    <th>Start time:</th>
    <td><?= format_datetime($server->StartTime) ?></td>
  </tr>

  <tr title="The current total amount of memory available for the JVM, in bytes.">
    <th>Total memory:</th>
    <td><?= format_memory($server->RuntimeMemory) ?></td>
  </tr>

  <tr title="The current free amount of memory available for the JVM, in bytes.">
    <th>Free memory:</th>
    <td><?= format_memory($server->RuntimeMemoryFree) ?></td>
  </tr>

  <tr title="The current CPU load average.">
    <th>CPU Load:</th>
    <td><?= sprintf("%.2f", $server->CpuLoadAvg) ?></td>
  </tr>

<?php

$block_cache = $mbeanServer->lookup("resin:type=BlockManager");
$proxy_cache = $mbeanServer->lookup("resin:type=ProxyCache");

?>

  <tr title="Percentage of requests that have been served from the proxy cache:">
    <th>Proxy cache miss ratio:</th>
    <td><?= format_miss_ratio($proxy_cache->HitCountTotal,
                              $proxy_cache->MissCountTotal) ?></td>
  </tr>

  <tr title="Percentage of requests that have been served from the proxy cache:">
    <th><?= info("Block cache miss ratio") ?>:</th>
    <td><?= format_miss_ratio($block_cache->HitCountTotal,
                              $block_cache->MissCountTotal) ?></td>
  </tr>

<!-- XXX: show how cacheable apps are: cacheable/non-cacheable -->

  <tr>
    <th>Invocation miss ratio:</th>
    <td><?= format_miss_ratio($server->InvocationCacheHitCountTotal,
                             $server->InvocationCacheMissCountTotal) ?></td>
  </tr>

</table>

<?php

$mbean = $mbeanServer->lookup("resin:type=LoggerManager");
if ($mbean) {
  $messages = $mbean->findRecentMessages(10);

  if (! empty($messages)) {
    echo "<h2>Recent Messages</h2>\n";

    echo "<table class='data'>\n";
    echo "<tr><th>Date</th><th>Level</th><th>Message</th><th>Source</th></tr>\n";

    $messages = array_reverse($messages);

    foreach ($messages as $message) {
      echo "<tr>";
      echo "  <td>";
      echo strftime("%Y-%m-%d %H:%M:%S", $message->timestamp / 1000);
      echo "</td>";
      echo "  <td class='{$message->level}'>{$message->level}</td>";
      echo "  <td>{$message->message}</td>";
      echo "  <td>{$message->className}.{$message->methodName}()</td>";
      echo "</tr>";
    }

    echo "</table>\n";
  }
}

$thread_pool = $server->ThreadPool;
?>

<!--
"Restart" - "Exit this instance cleanly and allow the wrapper script to start a new JVM."
-->

<h2>Thread pool</h2>
<!--
<div class="description">
The ThreadPool manages all threads used by Resin.
</div>
-->

<table class="data">
  <tr>
    <th colspan='3'>Threads</th>
    <th colspan='2'>Config</th>
  </tr>
  <tr>
    <th title="The number of active threads. These threads are busy servicing requests or performing other tasks.">Active thread count</th>
    <th title="The number of idle threads. These threads are allocated but inactive, available for new requests or tasks.">Idle thread count</th>
    <th title="The current total number of threads managed by the pool.">Total count</th>
    <th title="The maximum number of threads that Resin can allocate.">thread-max</th>
    <th title="The minimum number of threads Resin should have available for new requests or other tasks.  This value causes a minimum number of idle threads, useful for situations where there is a sudden increase in the number of threads required.">thread-idle-min</th>
  </tr>
  <tr align='right'>
    <td><?= $thread_pool->ThreadActiveCount ?></td>
    <td><?= $thread_pool->ThreadIdleCount ?></td>
    <td><?= $thread_pool->ThreadCount ?></td>
    <td><?= $thread_pool->ThreadMax ?></td>
    <td><?= $thread_pool->ThreadIdleMin ?></td>
  </tr>
</table>

<!-- TCP ports -->

<?php
$ports = $server->Ports;

if ($ports) {
?>
<h2>TCP ports</h2>

<table class="data">
  <tr>
    <th colspan='2'>&nbsp;</th>
  <th colspan='3'>Threads</th>
  <th colspan='1'>&nbsp;</th>
  <th colspan='2'>Keepalive</th>
  <th colspan='1'>Socket</th>

  <tr>
    <th>&nbsp;</th>
    <th>Status</th>

    <th>Active count</th>
    <th>Idle count</th>
    <th>Total count</th>

    <th>Keepalive</th>

    <th>max</th>
    <th>timeout</th>

    <th>timeout</th>
  </tr>
<?php
  foreach ($ports as $port) {
?>

  <tr>
    <td><?= $port->ProtocolName ?>://<?= $port->Address ? $port->Address : "*" ?>:<?= $port->Port ?></td>
    <td class="<?= $port->State ?>"><?= $port->State ?></td>
    <td><?= $port->ThreadActiveCount ?></td>
    <td><?= $port->ThreadIdleCount ?></td>
    <td><?= $port->ThreadCount ?></td>

    <td><?= $port->ThreadKeepaliveCount ?>
        <?= $port->SelectKeepaliveCount < 0
            ? ""
            : ("(" . $port->SelectKeepaliveCount . ")") ?></td>

    <td><?= $port->KeepaliveMax ?></td>
    <td><?= $port->KeepaliveTimeout ?></td>

    <td><?= $port->SocketTimeout ?></td>
  </tr>
<?php 
  }
}
?>
</table>

<!-- Cluster -->

<?php
  foreach ($resin->Clusters as $cluster) {

  if (empty($cluster->Servers))
    continue;

echo "<h2>Server Connectors: $cluster->Name</h2>";

?>

<table class="data">

  <tr>
    <th>Server</th>
    <th>Address</th>
    <th>Status</th>
    <th>Active</th>
    <th>Idle</th>
    <th>Connection Miss</th>
    <th>Load</th>
    <th>Latency</th>
    <th>Fail Total</th>
    <th>Busy Total</th>
  </tr>

<?php
foreach ($cluster->Servers as $client) {
?>

  <tr class='<?= $client->ping() ? "active" : "inactive" ?>'>
  <tr>
    <td><?= $client->Name ?></td>
    <td><?= $client->Address ?>:<?= $client->Port ?></td>
    <td class="<?= $client->State ?>"><?= $client->State ?></td>
    <td><?= $client->ConnectionActiveCount ?></td>
    <td><?= $client->ConnectionIdleCount ?></td>
    <td><?= format_miss_ratio($client->ConnectionKeepaliveCountTotal,
                              $client->ConnectionNewCountTotal) ?></td>
    <td><?= sprintf("%.2f", $client->ServerCpuLoadAvg) ?></td>
    <td><?= sprintf("%.2f", $client->LatencyFactor) ?></td>
<!-- XXX:
    <td><?= $client->LastFailTime ?></td>
    <td><?= $client->LastBusyTime ?></td>
-->
    <td><?= $client->ConnectionFailCountTotal ?></td>
    <td><?= $client->ConnectionBusyCountTotal ?></td>
  </tr>
<?php 
}
?>

</table>
<?php 
}
?>

<!-- Connection pools -->

<?php
$db_pools = $mbeanServer->query("resin:*,type=ConnectionPool");

if ($db_pools) {
?>

<h2>Connection pools</h2>

<table class="data">
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
  $row = 0;
  foreach ($db_pools as $pool) {
?>

  <tr class='<?= row_style($row++) ?>'>
    <td><?= $pool->Name ?></td>
    <td><?= $pool->ConnectionActiveCount ?></td>
    <td><?= $pool->ConnectionIdleCount ?></td>
    <td><?= $pool->ConnectionCount ?></td>
    <td><?= $pool->MaxConnections ?></td>
    <td><?= $pool->MaxIdleTime ?></td>
  </tr>

<?php
  }
?>
</table>
<?php
}
?>

<!-- Persistent store -->
<?php
$store = $mbeanServer->lookup("resin:type=PersistentStore");

if ($store) {
  echo "<h2>Persistent Store: $store->StoreType</h2>\n";
  echo "<table class='data'>";

  echo "<tr><th>Object Count</th><td>$store->ObjectCount</td>\n";
  echo "<tr><th>Load Count</th><td>$store->LoadCountTotal</td>\n";
  echo "<tr><th>Load Fail Count</th><td>$store->LoadFailCountTotal</td>\n";
  echo "<tr><th>Save Count</th><td>$store->SaveCountTotal</td>\n";
  echo "<tr><th>Save Fail Count</th><td>$store->SaveFailCountTotal</td>\n";
  echo "</table>";
}

?>

<!-- Hosts and Applications -->
<h2>Hosts and Applications</h2>

<table class="data">
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

$hosts = $mbeanServer->query("resin:*,type=Host");

usort($hosts, "sort_host");

foreach ($hosts as $host) {
  $hostName = empty($host->HostName) ? "default" : $host->HostName;
?>

  <tr title='<?= $hostObjectName ?>'><td colspan='4'><?= $host->URL ?></td></tr>
<?php
function sort_webapp($a, $b)
{
  return strcmp($a->ContextPath, $b->ContextPath);
}

$webapps = $host->WebApps;

usort($webapps, "sort_webapp");

foreach ($webapps as $webapp) {
  $session = $webapp->SessionManager;
?>

  <tr class="<?= $webapp->State ?>" title='<?= $webapp->Name ?>'>
    <td>&nbsp;</td>
    <td><?= empty($webapp->ContextPath) ? "/" : $webapp->ContextPath ?>
    <td><?= $webapp->State ?>
    <td><?= $session->SessionActiveCount ?>
  </tr>
<?php
  } // webapps
} // hosts
?>

</table>

<?php
/*
$tcp_conn = $mbeanServer->query("resin:*,type=TcpConnection");
$slow_conn = array();

echo "<table class='data'>";

foreach ($tcp_conn as $conn_name) {
  $conn = $mbeanServer->lookup($conn_name);

  echo "<tr><td>" . $conn->ThreadId . "<td>" . $conn->State . "<td>" . $conn->ActiveTime;

  if ($conn->ActiveTime > 60000)
    $slow_conn[] = $conn;
}


echo "</table>";

$thread_mgr = $mbeanServer->lookup("java.lang:type=Threading");

foreach ($slow_conn as $slow) {
  echo "<h3>" . $slow->ObjectName . " " . ($slow->ActiveTime / 1000) . "</h3>";

  $thread_id = $slow->ThreadId;

  resin_var_dump($thread_id);
  $info = $thread_mgr->getThreadInfo($thread_id, 16);

  if ($info) {
    $bean = Java("java.lang.management.ThreadInfo");
    $info = $bean->from($info);
  }

  resin_var_dump($info->getStackTrace());

}
*/

?>

<?php display_footer("status.php"); ?>
