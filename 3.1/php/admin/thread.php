<?php
/**
 * Summary of threading
 */

require_once "WEB-INF/php/inc.php";

$server_id = $_GET["server-id"];

if ($server_id) {
  $mbean_server = new MBeanServer($server_id);

  if (! $mbean_server) {
    $title = "Resin: Thread for server $server_id";

    display_header("thread.php", $title, $server);

    echo "<h3 class='fail'>Can't contact $server_id</h3>";
    return;
  }
}
else
  $mbean_server = new MBeanServer();

$resin = $mbean_server->lookup("resin:type=Resin");
$server = $mbean_server->lookup("resin:type=Server");

$jvm_thread = $mbean_server->lookup("java.lang:type=Threading");

$title = "Resin: Status";

if (! empty($server->Id))
  $title = $title . " for server " . $server->Id;
?>

<?php display_header("thread.php", $title, $server); ?>

<h2>Server: <?= $server->Id ?></h2>

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
    <th colspan='3'>Resin Threads</th>

    <th colspan='2'>JVM Threads</th>

    <th colspan='2'>Config</th>
  </tr>
  <tr>
    <th title="The number of active threads. These threads are busy servicing requests or performing other tasks.">Active</th>
    <th title="The number of idle threads. These threads are allocated but inactive, available for new requests or tasks.">Idle</th>
    <th title="The current total number of threads managed by the pool.">Total</th>

    <th title="The number of threads currently running in the JVM.">Total</th>
    <th title="The maximum number of threads running in the JVM.">Peak</th>

    <th title="The maximum number of threads that Resin can allocate.">thread-max</th>
    <th title="The minimum number of threads Resin should have available for new requests or other tasks.  This value causes a minimum number of idle threads, useful for situations where there is a sudden increase in the number of threads required.">thread-idle-min</th>
  </tr>
  <tr align='right'>
    <td><?= $thread_pool->ThreadActiveCount ?></td>
    <td><?= $thread_pool->ThreadIdleCount ?></td>
    <td><?= $thread_pool->ThreadCount ?></td>

    <td><?= $jvm_thread->ThreadCount ?></td>
    <td><?= $jvm_thread->PeakThreadCount ?></td>

    <td><?= $thread_pool->ThreadMax ?></td>
    <td><?= $thread_pool->ThreadIdleMin ?></td>
  </tr>
</table>

<?php

$threads = array();
$thread_ids = $jvm_thread->AllThreadIds;

foreach ($thread_ids as $id) {
  $threads[] = $jvm_thread->getThreadInfo($id, 50);
}

$thread_group = partition_threads($threads);
$groups = array("active", "misc", "accept", "keepalive", "idle");

echo "<h2>Threads</h2>\n"
echo "<table class='threads'>\n";

foreach ($groups as $name) {
  $threads = $thread_group[$name];

  if (sizeof($threads) <= 0)
    continue;

  usort($threads, "thread_name_cmp");

  echo "<tr class='head'><th colspan='5' align='left'>$name (" . sizeof($threads) . ")";

  $show = "hide('s_$name');show('h_$name');";
  foreach ($threads as $info) {
    $show .= "show('t_{$info->threadId}');";
  }

  $hide = "show('s_$name');hide('h_$name');";
  foreach ($threads as $info) {
    $hide .= "hide('t_{$info->threadId}');";
  }

  echo " <a id='s_$name' href=\"javascript:$show\">show</a> ";
  echo "<a id='h_$name' href=\"javascript:$hide\" style='display:none'>hide</a>";

  echo "</th></tr>\n";

  echo "<tr>";
  echo "<td style='border-width:0'>&nbsp;&nbsp;&nbsp;</td>";
  echo "<th>id</th>";
  echo "<th>name</th>";
  echo "<th>method</th>";
  echo "<th>state</th>";
  echo "</tr>\n";

  foreach ($threads as $info) {
    echo "<tr>";

    $id = $info->threadId;

    echo "<td style='border-width:0'>&nbsp;&nbsp;&nbsp;</td>";
    echo "<td>" . $id . "</td>";
    echo "<td>" . $info->threadName . "</td>";

    if ($info->stackTrace[0]) {
      echo "<td>" . $info->stackTrace[0]->className . "."
	. $info->stackTrace[0]->methodName . "()</td>";
    }
    else
      echo "<td></td>";
    
    echo "<td>" . $info->threadState . "</td>";

    echo "</tr>\n";

    echo "<tr id='t_$id' style='display:none' class='stack_trace'>";
    echo "<td style='border-width:0'></td>";
    echo "<td colspan='4'>";
    echo "<pre>\n";
    foreach ($info->stackTrace as $elt) {
      echo " at {$elt->className}.{$elt->methodName} ({$elt->fileName}:{$elt->lineNumber})\n";
    }
    echo "</pre>\n";
    echo "</td>";
    echo "</tr>\n";
  }
}

echo "</table>\n";

/*
foreach ($thread_ids as $id) {
  var_dump($jvm_thread->getThreadInfo($id));
}
*/
echo "</pre>";

/*

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

function thread_name_cmp($thread_a, $thread_b)
{
  if ($thread_a->threadName == $thread_b->threadName)
    return 0;
  else if ($thread_a->threadName < $thread_b->threadName)
    return -1;
  else
    return 1;
}

function partition_threads($threads)
{
  $partition = array();

  foreach ($threads as $info) {
    $stackTrace = $info->stackTrace;
    if ($stackTrace[0]->className == "java.lang.Object"
        && $stackTrace[0]->methodName == "wait"
        && $stackTrace[1]->className == "com.caucho.util.ThreadPool\$Item"
        && $stackTrace[1]->methodName == "runTasks") {
      $partition["idle"][] = $info;
    }
    else if (is_accept_thread($info)) {
      $partition["accept"][] = $info;
    }
    else if (is_keepalive_thread($info)) {
      $partition["keepalive"][] = $info;
    }
    else if (preg_match("/^(http|hmux)/", $info->threadName)) {
      $partition["active"][] = $info;
    }
    else {
      $partition["misc"][] = $info;
    }
  }

  return $partition;
}

function is_accept_thread($info)
{
  foreach ($info->stackTrace as $item) {
    if ($item->className == "com.caucho.server.port.Port"
	&& $item->methodName == "accept")
      return true;
  }

  return false;
}

function is_keepalive_thread($info)
{
  $stackTrace = $info->stackTrace;
  
  for ($i = 0; $i < sizeof($stackTrace); $i++) {
    $item = $stackTrace[$i];
  
    if ($item->className == "com.caucho.server.port.TcpConnection"
	&& $item->methodName == "run") {
      $prev = $stackTrace[$i - 1];
      
      if ($prev->className == "com.caucho.vfs.ReadStream"
	  && $prev->methodName == "waitForRead")
	return true;
      else if ($prev->className == "com.caucho.server.port.TcpConnection"
  	       && $prev->methodName == "waitForKeepalive")
	return true;
      else
	return false;
    }
  }

  return false;
}

?>

<?php display_footer("status.php"); ?>
