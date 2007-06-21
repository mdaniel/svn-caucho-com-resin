<?php
/**
 * Provides the clustering information.
 */


require_once "WEB-INF/php/inc.php";

$mbeanServer = new MBeanServer();

$resin = $mbeanServer->lookup("resin:type=Resin");
$server = $mbeanServer->lookup("resin:type=Server");

$title = "Resin: Cluster";

if (! empty($server->Id))
  $title = $title . " for server " . $server->Id;
?>

<?php

display_header("cluster.php", $title, $server->Id);


?>

<h2>Server: <?= $server->Id ?></h2>

<!-- Cluster -->

<?php

echo "<table class='data'>";
echo "<tr>";
echo "<th>Name</th>";
echo "<th>Memory</th>";
echo "<th>Free Memory</th>";
echo "<th>CPU Avg</th>";
echo "<th>Cache Miss</th>";
echo "<th>Block Miss</th>";
echo "<th>Thread</th>";
echo "</tr>";

foreach ($resin->Clusters as $cluster) {

  if (empty($cluster->Servers))
    continue;

  echo "<tr><td colspan='8'>$cluster->Name</th></tr>\n";

  $client_names = array();
  if ($cluster->Name == $server->Cluster->Name) {
    $client_names[] = $server->Id;
  }

  foreach ($cluster->Servers as $client) {
    $client_names[] = $client->Name;
  }

  sort($client_names);

  foreach ($client_names as $name) {
    $sub_mbean_server = new MBeanServer($name);

    echo "<tr><th>$name</th>";

    $sub_server = $sub_mbean_server->lookup("resin:type=Server");
    if ($sub_server) {
      echo "<td>" . sprintf("%.2f", $sub_server->RuntimeMemory / (1024 * 1024)). "</td>";
      echo "<td>" . sprintf("%.2f", $sub_server->RuntimeMemoryFree / (1024 * 1024)) . "</td>";
      echo "<td>" . sprintf("%.2f", $sub_server->CpuLoadAvg) . "</td>";
    }
    else {
      echo "<td></td>";
      echo "<td></td>";
      echo "<td></td>";
    }

    $proxy_cache = $sub_mbean_server->lookup("resin:type=ProxyCache");
    if ($proxy_cache) {
      echo "<td>";
      echo format_miss_ratio($proxy_cache->HitCountTotal,
                             $proxy_cache->MissCountTotal);
      echo "</td>";
    }
    else {
      echo "<td></td>";
    }

    $block_cache = $sub_mbean_server->lookup("resin:type=BlockManager");
    if ($block_cache) {
      echo "<td>" . format_miss_ratio($block_cache->HitCountTotal,
                                      $block_cache->MissCountTotal)
                  . "</td>";
    }
    else {
      echo "<td></td>";
    }

    $thread_pool = $sub_server->ThreadPool;
    if ($thread_pool) {
/*
      echo "<td>" . $thread_pool->ThreadActiveCount . "</td>";
      echo "<td>" . $thread_pool->ThreadIdleCount . "</td>";
*/
      echo "<td>" . $thread_pool->ThreadCount . "</td>";
    }
    else {
/*
      echo "<td></td>";
      echo "<td></td>";
*/
      echo "<td></td>";
    }

    echo "</tr>\n";
  }
}

echo "</table>";

display_footer("cluster.php");

