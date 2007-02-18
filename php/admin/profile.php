<?php
/**
 * Summary of profiling
 */

require_once "inc.php";

require "restricted.php";

$profile = new Java("com.caucho.profile.Profile");

$mbeanServer = new MBeanServer();

$server = $mbeanServer->lookup("resin:type=Server");

$title = "Resin: Profile $server->Id";

if (! $profile)
  $title .= " - not available";

?>

<?php display_header("profile.php", $title, $server->Id) ?>

<?php

if ($profile) {
  if ($_POST['action'] == 'stop') {
    $profile->stop();
  }
  else if ($_POST['action'] == 'start') {
    $profile->start();

    if ($_POST['period'] >= 10) {
      $profile->setPeriod($_POST['period']);
    }
  }

  if ($profile->active) {
    echo "<h2>";
    echo "Profile Active: {$profile->period}ms";

    echo "<form action='profile.php' method='post' style='display:inline'>";
    echo "<input type='submit' name='action' value='stop'>";
    echo "</form>";

    echo "</h2>\n";
  }
  else {
    echo "<h2>";
    echo "Profile Stopped";

    echo "<form action='profile.php' method='post' style='display:inline'>";
    echo "<select type='select' name='period'>";
    echo "  <option>10";
    echo "  <option>20";
    echo "  <option>100";
    echo "  <option>250";
    echo "  <option selected>1000";
    echo "  <option>5000";
    echo "</select>";
    echo "<input type='submit' name='action' value='start'>";
    echo "</form>";

    echo "</h2>\n";
  }

  $results = $profile->getResults();

  $partition = do_partition_profile($results);
  $groups = array("active", "resin", "keepalive", "wait", "accept", "single");

  echo "<table class='threads'>\n";

  foreach ($groups as $name) {
    $entries = $partition[$name];

    if (sizeof($entries) <= 0)
      continue;

    $topTicks = $entries[0]->getCount();

    echo "<tr class='head'><th colspan='4' align='left'>$name (" . sizeof($entries) . ")";

    $show = "hide('s_$name');show('h_$name');";
    $hide = "show('s_$name');hide('h_$name');";

    for ($i = 0; $i < sizeof($entries); $i++) {
      $show .= "show('t_{$name}_{$i}');";
      $hide .= "hide('t_{$name}_{$i}');";
    }

    echo " <a id='s_$name' href=\"javascript:$show\">show</a> ";
    echo "<a id='h_$name' href=\"javascript:$hide\" style='display:none'>hide</a>";

    echo "</th></tr>\n";

    for ($i = 0; $i < sizeof($entries); $i++) {
      $entry = $entries[$i];

      echo "<tr>";
      echo "<td>";
      printf("%.4f (%d/%d)", $entry->getCount() / $topTicks, $entry->getCount(), $topTicks);
      echo "</td>";
      echo "<td>";
      echo "<a id='s_{$name}_{$i}' href=\"javascript:show('t_{$name}_{$i}');hide('s_{$name}_{$i}');show('h_{$name}_{$i}');\">show</a> ";
      echo "<a id='h_{$name}_{$i}' href=\"javascript:hide('t_{$name}_{$i}');show('s_{$name}_{$i}');hide('h_{$name}_{$i}');\" style='display:none'>hide</a> ";
      echo "</td>";
      echo "<td>";
      $stack = $entry->getStackTrace()[0];
      echo "{$stack->getClassName()}.{$stack->getMethodName()}()\n";
      echo "</td>";
      echo "</tr>\n";

      echo "<tr id='t_{$name}_{$i}' style='display:none'>";
      echo "<td>";
      echo "</td>";
      echo "<td colspan='2'>";

      echo "<pre>";
      foreach ($entry->getStackTrace() as $stack) {
        echo "  at {$stack->getClassName()}.{$stack->getMethodName()}()\n";
      }
      echo "</pre>";
      echo "</td>";
      echo "</tr>\n";
    }
  }

  echo "</table>\n";
}
else {
  echo "<h2>Profiling is not available</h2>";
}

function do_partition_profile($entries)
{
  $partition = array();
  foreach ($entries as $info) {
    $stackTrace = $info->stackTrace;

    if (! $stackTrace) {
    }
    else if ($info->getCount() < 2) {
      $partition["single"][] = $info;
    }
    else if ($stackTrace[0]->className == "java.lang.Object"
        && $stackTrace[0]->methodName == "wait") {
      $partition["wait"][] = $info;
    }
    else if ($stackTrace[0]->className == "java.lang.Thread"
        && $stackTrace[0]->methodName == "sleep") {
      $partition["wait"][] = $info;
    }
    else if ($stackTrace[0]->className == "com.caucho.vfs.JniServerSocketImpl"
             && $stackTrace[0]->methodName == "nativeAccept") {
      $partition["accept"][] = $info;
    }
    else if ($stackTrace[0]->className == "java.net.PlainSocketImpl"
             && $stackTrace[0]->methodName == "socketAccept") {
      $partition["accept"][] = $info;
    }
    else if ($stackTrace[0]->className == "com.caucho.profile.Profile"
             && $stackTrace[0]->methodName == "nativeProfile") {
      $partition["resin"][] = $info;
    }
    else if ($stackTrace[0]->className == "com.caucho.server.port.JniSelectManager"
             && $stackTrace[0]->methodName == "selectNative") {
      $partition["resin"][] = $info;
    }
    else if (is_resin_main($stackTrace)) {
      $partition["resin"][] = $info;
    }
    else if (is_keepalive($stackTrace)) {
      $partition["keepalive"][] = $info;
    }
    else if ($stackTrace[0]->className == "java.lang.ref.ReferenceQueue") {
    }
    else {
      $partition["active"][] = $info;
    }
  }

  return $partition;
}

function is_resin_main($stackTrace)
{
  for ($i = 0; $i < sizeof($stackTrace); $i++) {
    if ($stackTrace[$i]->className == "com.caucho.server.resin.Resin"
        && $stackTrace[$i]->methodName == "waitForExit") {
      return true;
    }
  }

  return false;
}

function is_keepalive($stackTrace)
{
  for ($i = 0; $i < sizeof($stackTrace); $i++) {
    if ($stackTrace[$i]->className != "com.caucho.server.port.TcpConnection"
        || $stackTrace[$i]->methodName != "run") {
      continue;
    }
    else if ($stackTrace[$i - 1]->className == "com.caucho.server.port.TcpConnection"
             && $stackTrace[$i - 1]->methodName == "waitForKeepalive") {
      return true;
    }
    else if ($stackTrace[$i - 1]->className == "com.caucho.vfs.ReadStream"
             && $stackTrace[$i - 1]->methodName == "waitForRead") {
      return true;
    }
  }

  return false;
}

display_footer("profile.php");

?>
