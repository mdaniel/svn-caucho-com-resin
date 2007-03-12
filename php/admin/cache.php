<?php
/**
 * Summary of caching
 */

require_once "inc.php";

require "restricted.php";

$mbeanServer = new MBeanServer();

$resin = $mbeanServer->lookup("resin:type=Resin");
$server = $mbeanServer->lookup("resin:type=Server");

$block_cache = $mbeanServer->lookup("resin:type=BlockManager");
$proxy_cache = $mbeanServer->lookup("resin:type=ProxyCache");

$title = "Resin: Cache";

if (! empty($server->Id))
  $title = $title . " for server " . $server->Id;
?>

<?php display_header("cache.php", $title, $server->Id) ?>

<h2>Server: <?= $server->Id ?></h2>

<table class="data">
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



?>

<?php display_footer("cache.php"); ?>
