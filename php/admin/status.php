<?php
/**
 * Provides the most important status information about the Resin server.
 *
 * @author Sam
 */


require_once "WEB-INF/php/inc.php";

$server_id = $_GET["server-id"];

if ($server_id) {
  $mbean_server = new MBeanServer($server_id);

  if (! $mbean_server) {
    $title = "Resin: Status for server $server_id";

    display_header("thread.php", $title, $server);

    echo "<h3 class='fail'>Can't contact $server_id</h3>";
    return;
  }
}
else
  $mbean_server = new MBeanServer();

if ($mbean_server) {
  $resin = $mbean_server->lookup("resin:type=Resin");
  $server = $mbean_server->lookup("resin:type=Server");
}

$title = "Resin: Status";

if (! empty($server->Id))
  $title = $title . " for server " . $server->Id;
?>

<?php

display_header("status.php", $title, $server, true);

if (! $server) {
  echo "<h2 class='fail'>Can't contact '$server_id'</h2>";
  return;
}

?>

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
    <th>Uptime:</th>
    <?php
      $start_time = $server->StartTime->time / 1000;
      $now = $server->CurrentTime->time / 1000;
      $uptime = $now - $start_time;

      if ($uptime < 12 * 3600)
        echo "<td class='warmup'>";
      else
        echo "<td>";

      echo sprintf("%d days %02d:%02d:%02d",
                   $uptime / (24 * 3600),
                   $uptime / 3600 % 24,
                   $uptime / 60 % 60,
                   $uptime % 60);
      echo " -- " . format_datetime($server->StartTime);
     ?>
   </td>
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

if ($mbean_server) {
  $block_cache = $mbean_server->lookup("resin:type=BlockManager");
  $proxy_cache = $mbean_server->lookup("resin:type=ProxyCache");
}

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
    <th title="The number of active threads. These threads are busy servicing requests or performing other tasks.">Active</th>
    <th title="The number of idle threads. These threads are allocated but inactive, available for new requests or tasks.">Idle</th>
    <th title="The current number of threads managed by the pool.">Total</th>
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
  <th colspan='4'>Keepalive</th>

  <tr>
    <th>&nbsp;</th>
    <th>Status</th>

    <th>Active</th>
    <th>Idle</th>
    <th>Total</th>

    <th>Total</th>
    <th>Thread</th>
    <th>Select</th>
    <th>Comet</th>
  </tr>
<?php
  $count = 0;
  foreach ($ports as $port) {
?>

  <tr class='<?= $count++ % 2 == 0 ? "ra" : "rb" ?>'>
    <td class='item'><?= $port->ProtocolName ?>://<?= $port->Address ? $port->Address : "*" ?>:<?= $port->Port ?></td>
    <td class="<?= $port->State ?>"><?= $port->State ?></td>
    <td><?= $port->ThreadActiveCount ?></td>
    <td><?= $port->ThreadIdleCount ?></td>
    <td><?= $port->ThreadCount ?></td>

    <td><?= $port->KeepaliveCount ?></td>
    <td><?= $port->KeepaliveThreadCount ?></td>
    <td><?= $port->KeepaliveSelectCount ?></td>
    <td><?= $port->CometIdleCount ?>
  </tr>
<?php 
  }
}
?>
</table>

<!-- Cluster -->

<h2>Server Connectors</h2>

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
    <th colspan='2'>Failures</th>
    <th colspan='2'>Busy</th>
  </tr>
<?php

  foreach ($resin->Clusters as $cluster) {
    if (empty($cluster->Servers))
      continue;

    echo "<tr><td class='group' colspan='12'>$cluster->Name</td></tr>\n";

  $count = 0;
  foreach ($cluster->Servers as $client) {
?>

  <tr class='<?= $count++ % 2 == 0 ? "ra" : "rb" ?>'>
    <td class='item'><?= $client->Name ?></td>
    <td><?= $client->Address ?>:<?= $client->Port ?></td>
    <td class="<?= $client->State ?>"><?= $client->State ?></td>
    <td><?= $client->ConnectionActiveCount ?></td>
    <td><?= $client->ConnectionIdleCount ?></td>
    <td><?= format_miss_ratio($client->ConnectionKeepaliveCountTotal,
                              $client->ConnectionNewCountTotal) ?></td>
    <td><?= sprintf("%.2f", $client->ServerCpuLoadAvg) ?></td>
    <td><?= sprintf("%.2f", $client->LatencyFactor) ?></td>
    <?php
      format_ago_td_pair($client->ConnectionFailCountTotal,
                         $client->LastFailTime);

      format_ago_td_pair($client->ConnectionBusyCountTotal,
                         $client->LastBusyTime);

    ?>
  </tr>
<?php 
  }
}
?>

</table>

<!-- Connection pools -->

<?php
if ($mbean_server) {
  $db_pools = $mbean_server->query("resin:*,type=ConnectionPool");
}

if ($db_pools) {
?>

<h2>Connection pools</h2>

<table class="data">
  <tr>
    <th>&nbsp;</th>
    <th colspan='5'>Connections</th>
    <th colspan='2'>Config</th>
  </tr>
  <tr>
    <th>Name</th>
    <th>Active</th>
    <th>Idle</th>
    <th>Created</th>
    <th colspan='2'>Failed</th>
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
    <td><?= format_miss_ratio($pool->ConnectionCountTotal,
                              $pool->ConnectionCreateCountTotal) ?></td>
    <td><?= $pool->ConnectionFailCountTotal ?></td>
    <td class='<?= format_ago_class($pool->LastFailTime) ?>'>
        <?= format_ago($pool->LastFailTime) ?></td>
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

<?php

if ($mbean_server) {
  $mbean = $mbean_server->lookup("resin:type=LoggingManager");
}

//
// recent messages
//

if ($mbean) {
  $now = time();

  $messages = $mbean->findMessages(($now - 24 * 3600) * 1000, $now * 1000);

  if (! empty($messages)) {
    echo "<h2>Recent Messages</h2>\n";

    echo "<table class='data'>\n";
/*
    //echo "<thead class='scroll'>\n";
    echo "<tr><th class='date'>Date</th>"
    echo "    <th class='level'>Level</th>"
    echo "    <th class='message'>Message</th></tr>\n";
    //echo "</thead>\n";
*/

    $messages = array_reverse($messages);

    echo "<tbody class='scroll'>\n";
    foreach ($messages as $message) {
      echo "<tr class='{$message->level}'>";
      echo "  <td class='date'>";
      echo strftime("%Y-%m-%d %H:%M:%S", $message->timestamp / 1000);
      echo "</td>";
      echo "  <td class='level'>{$message->level}</td>";
      echo "  <td class='message'>" . htmlspecialchars(wordwrap($message->message, 90));
      echo "  </td>";
      echo "</tr>";
    }

    echo "</tbody>\n";
    echo "</table>\n";
  }

  //
  // startup
  //
  $start_time = $server->StartTime->time / 1000;

  $messages = $mbean->findMessages(($start_time - 15 * 60) * 1000, ($start_time - 2) * 1000);

  if (! empty($messages)) {
    echo "<h2>Shutdown Messages</h2>\n";

    echo "<table class='data'>\n";
/*
    //echo "<thead class='scroll'>\n";
    echo "<tr><th class='date'>Date</th>"
    echo "    <th class='level'>Level</th>"
    echo "    <th class='message'>Message</th></tr>\n";
    //echo "</thead>\n";
*/

    $messages = array_reverse($messages);

    echo "<tbody class='scroll'>\n";

    // mark the start time
    echo "<tr class='warning'>";
    echo "  <td class='date'>";
    echo strftime("%Y-%m-%d %H:%M:%S", $start_time);
    echo "</td>";
    echo "  <td class='level'></td>";
    echo "  <td class='message'>Start Time</td>";
    echo "</tr>";

    foreach ($messages as $message) {
      echo "<tr class='{$message->level}'>";
      echo "  <td class='date'>";
      echo strftime("%Y-%m-%d %H:%M:%S", $message->timestamp / 1000);
      echo "</td>";
      echo "  <td class='level'>{$message->level}</td>";
      echo "  <td class='message'>" . htmlspecialchars(wordwrap($message->message, 90)) . "</td>";
      echo "</tr>";
    }

    echo "</tbody>\n";
    echo "</table>\n";
  }
}
?>
<!-- Persistent store -->
<?php
if ($mbean_server) {
  $store = $mbean_server->lookup("resin:type=PersistentStore");
}

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

<!-- Applications -->
<h2>WebApps</h2>

<table class="data">
  <tr>
    <th>Web-App</th>
    <th>State</th>
    <th>Active</th>
    <th>Sessions</th>
    <th>Uptime</th>
    <th colspan='2'>500</th>
  </tr>
<?php
function sort_host($a, $b)
{
  return strcmp($a->URL, $b->URL);
}

if ($mbean_server) {
  $hosts = $mbean_server->query("resin:*,type=Host");
}

usort($hosts, "sort_host");

foreach ($hosts as $host) {
  $hostName = empty($host->HostName) ? "default" : $host->HostName;
?>

  <tr title='<?= $hostObjectName ?>'><td class='group' colspan='7'><?= $host->URL ?></td></tr>
<?php
function sort_webapp($a, $b)
{
  return strcmp($a->ContextPath, $b->ContextPath);
}

$webapps = $host->WebApps;

usort($webapps, "sort_webapp");
$count = 0;
foreach ($webapps as $webapp) {
  $session = $webapp->SessionManager;
?>

  <tr class='<?= row_style($count++) ?>'>
    <td class='item'>
       <?= empty($webapp->ContextPath) ? "/" : $webapp->ContextPath ?>
    </td>
    <td class='<?= format_state_class($webapp->State) ?>'>
       <?= $webapp->State ?>
    </td>
    <td><?= $webapp->RequestCount ?></td>
    <td><?= $session->SessionActiveCount ?></td>
    <td class='<?= format_ago_class($webapp->StartTime) ?>'>
      <?= format_ago($webapp->StartTime) ?>
    </td>
    <?php
        format_ago_td_pair($webapp->Status500CountTotal,
                           $webapp->Status500LastTime);
    ?>
  </tr>
<?php
  } // webapps
} // hosts
?>

</table>

<?php
/*
if ($mbean_server) {
  $tcp_conn = $mbean_server->query("resin:*,type=TcpConnection");
}

$slow_conn = array();

echo "<table class='data'>";

foreach ($tcp_conn as $conn_name) {
  $conn = $mbeanServer->lookup($conn_name);

  echo "<tr><td>" . $conn->ThreadId . "<td>" . $conn->State . "<td>" . $conn->ActiveTime;

  if ($conn->ActiveTime > 60000)
    $slow_conn[] = $conn;
}


echo "</table>";

if ($mbean_server) {
  $thread_mgr = $mbean_server->lookup("java.lang:type=Threading");
}

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
