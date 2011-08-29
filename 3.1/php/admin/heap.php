<?php
/**
 * Summary of heap
 */

require_once "WEB-INF/php/inc.php";

$heap = @new Java("com.caucho.profile.Heap");

$mbeanServer = new MBeanServer();

$server = $mbeanServer->lookup("resin:type=Server");
$server_id = $server->Id;

if (! $server_id)
  $server_id = "default";

$title = "Resin: Heap $server_Id";

if (! $heap)
  $title .= " - not available";

?>

<?php display_header("heap.php", $title, $server) ?>

<?php

$is_heap_available_heap = false;

if ($heap) {
  echo "<form action='heap.php' method='post'>";
  echo "<input type='submit' name='action' value='dump heap'>";
  echo "</form>";

  if ($_POST['action'] == 'dump heap') {
    $entries = $heap->dump();

    $is_heap_available = sizeof($entries) > 0;
  }
  else {
    $entries = $heap->lastDump();
    $is_heap_available = true;
  }
}

if (sizeof($entries) > 0) {
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

  echo "</table>\n";
}
else if (! $is_heap_available) {
  echo "<h2>Heap dump is not available</h2>\n";
  echo "<p>The heap dump requires Resin Professional and compiled JNI. It\n";
  echo "also requires an '-agentlib:resin' JVM argument:</p>\n";

  echo "<pre>\n";
  echo "&lt;resin ...>\n";
  echo "  &lt;cluster id='...'>\n";
  echo "    &lt;server id='...'>\n";
  echo "       ...\n";
  echo "       &lt;jvm-arg>-agentlib:resin&lt;/jvm-arg>\n";
  echo "    &lt;/server>\n";
  echo "  &lt;/cluster>\n";
  echo "&lt;/resin>\n";
  echo "</pre>\n";
}

display_footer("heap.php");

?>
