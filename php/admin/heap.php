<?php
/**
 * Summary of heap
 */

require_once "inc.php";

require "restricted.php";

$heap = new Java("com.caucho.profile.Heap");

$mbeanServer = new MBeanServer();

$server = $mbeanServer->lookup("resin:type=Server");
$server_id = $server->Id;

if (! $server_id)
  $server_id = "default";

$title = "Resin: Heap $server_Id";

if (! $profile)
  $title .= " - not available";

?>

<?php display_header("heap.php", $title, $server_Id) ?>

<?php

if ($heap) {
  echo "<form action='heap.php' method='post'>";
  echo "<input type='submit' name='action' value='dump heap'>";
  echo "</form>";

  if ($_POST['action'] == 'dump heap') {
    $entries = $heap->dump();

    if (sizeof($entries) <= 0)
      continue;

    $topSize = $entries[0]->getTotalSize();

    echo "<table class='data'>";
    echo "<tr>\n";
    echo "  <th>rank</th>\n";
    echo "  <th>self+desc</th>\n";
    echo "  <th>self</th>\n";
    echo "  <th>desc</th>\n";
    echo "  <th>count</th>\n";
    echo "  <th>class</th>\n";
    echo "</tr>\n";

    for ($i = 0; $i < sizeof($entries); $i++) {
      $entry = $entries[$i];

      echo "<tr class='" . row_style($i) . "'>";
      echo "<td>";
      printf("%.4f", $entry->getTotalSize() / $topSize);
      echo "</td>";
      echo "<td align='right'>";
      printf("%.3fM", $entry->getTotalSize() / (1024 * 1024));
      echo "</td>";
      echo "<td align='right'>";
      printf("%.3fM", $entry->getSelfSize() / (1024 * 1024));
      echo "</td>";
      echo "<td align='right'>";
      printf("%.3fM", $entry->getChildSize() / (1024 * 1024));
      echo "</td>";
      echo "<td align='right'>";
      printf("%d", $entry->getCount());
      echo "</td>";
      echo "<td>";
      echo "{$entry->getClassName()}";
      echo "</td>";
      echo "</tr>\n";
    }
  }

  echo "</table>\n";
}
else {
  echo "<h2>Heap dump is not available</h2>";
}

display_footer("heap.php");

?>
