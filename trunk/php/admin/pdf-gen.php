<?php

require_once 'pdfGraph.php';

define("STEP", 1);

define("HOUR", 3600);
define("DAY", 24 * HOUR);

if (! $g_report)
  $g_report = $_REQUEST["report"];

if (! $g_title) {
  $g_title = $_REQUEST["title"];
}

if (! $g_title) {
  $g_title = $g_report;
}  

if ($g_is_snapshot || $_REQUEST["snapshot"]) {
  $snapshot = $g_mbean_server->lookup("resin:type=SnapshotService");

  if ($snapshot) {
    $snapshot->snapshotJmx();
    $snapshot->snapshotHeap();
    $snapshot->snapshotThreadDump();

    if ($profile_time || $_REQUEST["profile_time"]) {
      
      if (! $profile_time)
        $profile_time = $_REQUEST["profile_time"];
      
      if (! $profile_depth)
        $profile_depth = $_REQUEST["profile_depth"];
      
      if (! $profile_tick)
        $profile_tick = $_REQUEST["profile_tick"];

      if (! $profile_depth)
        $profile_depth = 16;
      
      if (! $profile_tick)
        $profile_tick = 100;

      $snapshot->snapshotProfile(($profile_time*1000), $profile_tick, $profile_depth);
    }

    sleep(2);
  }
}

initPDF();
startDoc();

$pdf_name = $g_report;

if (! $pdf_name) {
  $pdf_name = $_REQUEST["report"];
  
  if (! $pdf_name) {
    $pdf_name = "Snapshot";
  }
}

$mPage = getMeterGraphPage($pdf_name);

if (! $mPage) {
  $mPage = getMeterGraphPage("Snapshot");
}

$title = $g_title;

if (! $title && $mPage)
  $title = $mPage->name;

if (! $title)
  $title = $pdf_name;

if (! $title)
  $title = "Snapshot";

$columns = $mPage->columns;

if ($columns > 2)
  $columns = 2;
else if ($columns < 1)
  $columns = 1;


if ($columns==1) {
	define("ROW1",     450);
	define("ROW2",     100);
	define("COL1",     50);
	define("COL2",     305);
	define("GRAPH_SIZE", new Size(400, 250));
} elseif ($columns==2) {
	define("ROW1",     550);
	define("ROW2",     325);
	define("ROW3",     100);

	define("COL1",     50);
	define("COL2",     305);
	define("GRAPH_SIZE", new Size(200, 125));
}

$g_canvas->set_header_center("Report: $g_title");

if (! $period) {
  $period = $_REQUEST['period'] ? (int) $_REQUEST['period'] : ($mPage->period/1000);
}

if ($period < HOUR) {
  $majorTicks = HOUR / 6;
}
elseif ($period >= HOUR && $period < 3 * HOUR) {
  $majorTicks = HOUR / 2;
}
elseif ($period >= 3 * HOUR && $period < 6 * HOUR) {
  $majorTicks = HOUR;
}
elseif ($period >= 6 * HOUR && $period < 12 * HOUR) {
  $majorTicks = 2 * HOUR;
}
elseif ($period >= 12 * HOUR && $period < 24 * HOUR) {
  $majorTicks = 4 * HOUR;
}
elseif ($period >= 24 * HOUR && $period <= 48 * HOUR) {
  $majorTicks = 6 * HOUR;
}
else {
  $majorTicks = 24 * HOUR;
}

$majorTicks = $majorTicks * 1000;

$minorTicks = $majorTicks/2;

$page = 0;


$index = $g_server->SelfServer->ClusterIndex;
$si = sprintf("%02d", $index);

$g_canvas->set_header_left("$si - " . $g_server->SelfServer->Name);

$time = $_REQUEST["time"];

if (! $time) {
  if ($g_is_watchdog) {
    $time = $g_server->StartTime->getTime() / 1000;
  }
  else {
    $time = time() + 5;
  }
}  

$g_end = $time;

if (2 * DAY <= $period) {
  $tz = date_offset_get(new DateTime);

  $ticks_sec = $majorTicks / 1000;

  $g_end = ceil(($g_end + $tz) / $ticks_sec) * $ticks_sec - $tz;
}

$g_start = $g_end - $period;

$g_canvas->set_footer_left(date("Y-m-d H:i", $g_end));

// $g_canvas->writeText(new Point(175,775), "Time at " . date("Y-m-d H:i", $time));


$g_canvas->write_section("Report: $g_title");

if ($mPage->isSummary()) {
  admin_pdf_summary();
}

$g_canvas->setDataFontAndSize(8);
$g_canvas->write_text_newline();
$g_canvas->writeTextLine("Data from " . date("Y-m-d H:i", $g_start)
                         . " to " . date("Y-m-d H:i", $g_end));

$start_message = $g_resin->getWatchdogStartMessage();

$g_canvas->write_text_newline();
$g_canvas->writeTextLine("Start message: " . $start_message);

admin_pdf_log_messages($g_canvas,
                       "Anomalies",
                       "/^com.caucho.health.analysis/",
                       $g_start, $g_end);

admin_pdf_log_messages($g_canvas,
                       "Log Messages",
                       "//",
                       $g_end - 30 * 60, $g_end,
                       15);

$full_names = $stat->statisticsNames();

$statList = array();

foreach ($full_names as $full_name)  {
  array_push($statList, new Stat($full_name));
}

/*
$g_canvas->setColor($black);
$g_canvas->moveTo(new Point(0, 770));
$g_canvas->lineTo(new Point(595, 770));
$g_canvas->stroke();
*/

draw_cluster_graphs($mPage);

draw_graphs($mPage);
 
if ($mPage->isHeapDump()) {
  admin_pdf_heap_dump();
}
 
if ($mPage->isProfile()) {
  admin_pdf_profile();
}

if ($mPage->isThreadDump()) {
  admin_pdf_thread_dump();
}

if ($mPage->isLog()) {
  admin_pdf_draw_log();
}

if ($mPage->isJmxDump()) {
  admin_pdf_jmx_dump();
}

$g_pdf->end_page();
$g_pdf->end_document();

$document = $g_pdf->get_buffer();
$length = strlen($document);

$filename = "$g_title" . "_" . date("Ymd_Hi", $g_end) . ".pdf";

header("Content-Type:application/pdf");
header("Content-Length:" . $length);
header("Content-Disposition:inline; filename=" . $filename);

echo($document);

unset($document);

pdf_delete($g_pdf);

// needed for PdfReport health action
return "ok";

function draw_cluster_graphs($mPage)
{
  global $g_canvas;
  global $g_server;
  
  $g_canvas->set_graph_rows(4);
  $g_canvas->set_graph_columns(2);
  
  $cluster = $g_server->getCluster();

  $triad_a = $cluster->getServers()[0];

  $mbean_server = new MBeanServer($triad_a->getName());
  $stat = $mbean_server->lookup("resin:type=StatService");
  if (! $stat) {
    $mbean_server = $g_mbean_server;
    $stat = $mbean_server->lookup("resin:type=StatService");
  }

  if (! $stat)
    return;

  $g_canvas->write_section("Cluster Graphs");

  foreach ($cluster->getServers() as $server) {
    $items = array();

    pdf_cluster_item($items, $stat, $server, "Uptime|Start Count", 0);
    pdf_cluster_item($items, $stat, $server, "Log|Critical", 1);
    pdf_cluster_item($items, $stat, $server, "Log|Warning", 2);

    $graph_name = sprintf("Server %02d - %s",
                          $server->getClusterIndex(),
                          $server->getName());

    $g_canvas->draw_graph($graph_name, $items);
  }
}

function pdf_cluster_item(&$items, $stat_mbean, $server, $name, $index)
{
  global $g_pdf_colors;
  
  $full_name = sprintf("%02d|Resin|%s", $server->getClusterIndex(), $name);

  // $items[$name] = admin_pdf_get_stat_item($stat_mbean, $full_name);
  $data = admin_pdf_get_stat_item($stat_mbean, $full_name);
  
  $gd = admin_pdf_create_graph_data($name, $data, $g_pdf_colors[$index]);

  $items[] = $gd;
}

function draw_graphs($mPage)
{
  global $g_canvas;
  global $g_server;

  $title = sprintf("Server Graphs: %02d - %s",
                   $g_server->getClusterIndex(), $g_server->getId());
                   
  $g_canvas->write_section($title);

  $g_canvas->set_graph_rows(3);
  $g_canvas->set_graph_columns(2);

  $graphs = $mPage->getMeterGraphs();

  foreach ($graphs as $graphData) {
    $meterNames = $graphData->getMeterNames();
    $gds = getStatDataForGraphByMeterNames($meterNames);
    
    $g_canvas->draw_graph($graphData->getName(), $gds);
  }
}

?>
