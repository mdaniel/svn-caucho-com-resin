<?php
/**
 * Configuration information
 *
 * @author Sam
 */


require_once "WEB-INF/php/inc.php";

$mbean_server = new MBeanServer();

if ($mbean_server) {
  $resin = $mbean_server->lookup("resin:type=Resin");
  $server = $mbean_server->lookup("resin:type=Server");
}

$title = "Resin: Config";

if (! empty($server->Id))
  $title = $title . " for server " . $server->Id;
?>

<?php

display_header("config.php", $title, $server, true);

if (! $server) {
  echo "<h2 class='fail'>Can't contact '$server_id'</h2>";
  return;
}

?>

<h2>Server: <?= $server->Id ?></h2>
<div class='section'>
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
  </table>

<?php
$thread_pool = $server->ThreadPool;
?>
  <p/>
  <table class="data">
  <tr>
    <td class='item'>thread-idle-min</td>
    <td><?= $thread_pool->ThreadIdleMin ?></td>
  </tr>
  <tr>
    <td class='item'>thread-idle-max</td>
    <td><?= $thread_pool->ThreadIdleMax ?></td>
  </tr>
  <tr>
    <td class='item'>thread-max</td>
    <td><?= $thread_pool->ThreadMax ?></td>
  </tr>
  </table>
</div>

<!-- TCP ports -->

<?php
$ports = $server->Ports;

if ($ports) {
?>
<h2>TCP ports</h2>
<div class='section'>
<table class="data">
<?php
  $count = 0;
  foreach ($ports as $port) {
?>

  <tr>
    <td class='group' colspan='4'><?= $port->ProtocolName ?>://<?= $port->Address ? $port->Address : "*" ?>:<?= $port->Port ?></td>
  </tr>

  <tr>
    <td class='item'>accept-thread-min</td>
    <td><?= $port->AcceptThreadMin ?></td>

    <td class='item'>keepalive-max</td>
    <td><?= $port->KeepaliveMax ?></td>
  </tr>

  <tr>
    <td class='item'>accept-thread-max</td>
    <td><?= $port->AcceptThreadMax ?></td>

    <td class='item'>keepalive-select-max</td>
    <td><?= $port->KeepaliveSelectMax ?></td>
  </tr>

  <tr>
    <td class='item'>accept-listen-backlog</td>
    <td><?= $port->AcceptListenBacklog ?></td>

    <td class='item'>keepalive-timeout</td>
    <td><?= $port->KeepaliveTimeout ?></td>
  </tr>

  <tr>
    <td class='item'>connection-max</td>
    <td><?= $port->ConnectionMax ?></td>

    <td class='item'>socket-timeout</td>
    <td><?= $port->SocketTimeout ?></td>
  </tr>

  <tr>
    <td class='item'>keepalive-connection-time-max</td>
    <td><?= $port->KeepaliveConnectionTimeMax ?></td>

    <td class='item'>suspend-time-max</td>
    <td><?= $port->SuspendTimeMax ?></td>
  </tr>
<?php 
  }
}
?>
</table>
</div>
<?php
echo "<h2>ServerConnectors</h2>"
echo "<div class='section'>";

echo "<table class='data'>\n";

$servers = $mbean_server->query("resin:*,type=ServerConnector");
foreach ($servers as $srun) {
?>
  <tr>
    <td class='group'><?= $srun->ClusterIndex + 1 ?>. <?= $srun->Name ?></td>
    <td class='group'>hmux://<?= $srun->Address ? $srun->Address : "*" ?>:<?= $srun->Port ?></td>
  </tr>

  <tr>
    <td class='item'>load-balance-connect-timeout</td>
    <td><?= $srun->ConnectTimeout ?></td>
  </tr>

  <tr>
    <td class='item'>load-balance-fail-recover-time</td>
    <td><?= $srun->RecoverTime ?></td>
  </tr>

  <tr>
    <td class='item'>load-balance-idle-time</td>
    <td><?= $srun->IdleTime ?></td>
  </tr>

  <tr>
    <td class='item'>load-balance-socket-timeout</td>
    <td><?= $srun->SocketTimeout ?></td>
  </tr>

  <tr>
    <td class='item'>load-balance-warmup-time</td>
    <td><?= $srun->WarmupTime ?></td>
  </tr>

  <tr>
    <td class='item'>load-balance-weight</td>
    <td><?= $srun->Weight ?></td>
  </tr>
<?php 
  }
?>
</table>
</div>

<!-- cluster ports -->

<?php
$cluster = $server->Cluster;

$cluster_name = empty($cluster->Name) ? "default" : $cluster->Name;

echo "<h2>Cluster: $cluster_name</h2>";
echo "<div class='section'>";

$servers = $cluster->Servers;
?>
<!-- host data -->

<?php

function sort_host($a, $b)
{
  return strcmp($a->URL, $b->URL);
}

  $hosts = $mbean_server->query("resin:*,type=Host");

usort($hosts, "sort_host");

foreach ($hosts as $host) {

  $hostName = empty($host->HostName) ? "default" : $host->HostName;
?>

  <h2>Host <?= $host->URL ?></h2>
  <div class='section'>

  <table class='data'>
  <tr>
    <td class='item'>root-directory</td>
    <td><?= $host->RootDirectory ?></td>
  </tr>
  </table>

<?php
function sort_webapp($a, $b)
{
  return strcmp($a->ContextPath, $b->ContextPath);
}

echo "<h3>WebApps</h3>\n";

$webapps = $host->WebApps;

usort($webapps, "sort_webapp");
$count = 0;
foreach ($webapps as $webapp) {
  $session = $webapp->SessionManager;
  $persistent_store = $session->PersistentStore;
?>

  <p />
  <table class='data' width='100%'>
  <tr><td class='group' colspan='2'>
       <?= empty($webapp->ContextPath) ? "/" : $webapp->ContextPath ?>
    </td>
  <tr>
    <td class='item' width='25%'>root-directory</td>
    <td>
       <?= $webapp->RootDirectory ?>
    </td>
  </tr>

  <tr>
    <td class='item'>session-timeout</td>
    <td>
       <?= $session->SessionTimeout / 1000 ?>s
    </td>
  </tr>

  <tr>
    <td class='item'>session-max</td>
    <td>
       <?= $session->SessionMax ?>
    </td>
  </tr>

<?php if ($persistent_store) { ?>

  <tr>
    <td class='item'>persistent-store</td>
    <td>
       <?= $persistent_store->StoreType ?>
    </td>
  </tr>

  <tr>
    <td class='item'>save-mode</td>
    <td>
       <?= $session->SaveMode ?>
    </td>
  </tr>

<?php } // persistent
  echo "</table>";

   } // webapps
   echo "</div>";
 } // hosts
 echo "</div>";

?>

</table>

<?php display_footer("config.php"); ?>
