<?php

require_once 'pdfGraph.php';

define("STEP", 1);

define("HOUR", 3600);
define("DAY", 24 * HOUR);

if ($g_is_snapshot || $_REQUEST["snapshot"]) {
  $snapshot = $g_mbean_server->lookup("resin:type=SnapshotService");

  if ($snapshot) {
    $snapshot->snapshotJmx();
    $snapshot->snapshotHeap();
    $snapshot->snapshotThreadDump();

    if (! $profile_time)
      $profile_time = $_REQUEST["profile_time"];

    if (! $profile_tick)
      $profile_tick = $_REQUEST["profile_tick"];

    if (! $profile_tick)
      $profile_tick = 100;

    if ($profile_time > 0 && $profile_time <= 120) {
      $snapshot->startProfile($profile_tick, 16);
      sleep($profile_time);
      $snapshot->stopProfile();

      $snapshot->snapshotProfile();
    }

    sleep(2);
  }
}

initPDF();
startDoc();

if (! $pdf_name) {
  $pdf_name = $_REQUEST["report"];
  
  if (! $pdf_name) {
    $pdf_name = "Summary";
  }
}

$mPage = getMeterGraphPage($pdf_name);

if (! $mPage) {
  $mPage = getMeterGraphPage("Summary");
  $pageName = $pdf_name;
}
else {
  $pageName = $mPage->name;

  if (! $pageName)
    $pageName = $pdf_name;
}    

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

$g_canvas->set_header_center("Report: $pageName");

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
  $time = time() + 5;
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


$g_canvas->write_section("Report: $pageName");

if ($mPage->isSummary()) {
  admin_pdf_summary();
}

$g_canvas->setDataFontAndSize(8);
$g_canvas->write_text_newline();
$g_canvas->writeTextLine("Data from " . date("Y-m-d H:i", $g_start)
                         . " to " . date("Y-m-d H:i", $g_end));

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

$filename = "$pageName" . "_" . date("Ymd_Hi", $g_end) . ".pdf";

header("Content-Type:application/pdf");



header("Content-Length:" . $length);



header("Content-Disposition:inline; filename=" . $filename);



echo($document);



unset($document);



pdf_delete($g_pdf);

// needed for PdfReport health action
return "ok";

function draw_graphs($mPage)
{
  global $g_canvas, $x, $y, $columns;
  
  $g_canvas->write_section("Graphs");

  $graphs = $mPage->getMeterGraphs();

  $index = 0;
  $gCount = 0;

  foreach ($graphs as $graphData) {
    $index++;
	
    if ($columns == 1) {
      $x = COL1;
    
      if ($index % 2 == 0) {
        $y = ROW2;
      } else {
        $y = ROW1;
      }
    }

    if ($columns == 2) {
      if ($gCount <= 1) {
        $y = ROW1;
      } elseif ($gCount > 1 && $gCount <= 3) {
        $y = ROW2;
      } elseif ($gCount > 3 && $gCount <= 5) {
        $y = ROW3;
      }

      if ($index%6==0) {
        $gCount=0;
      } else {
        $gCount++;
      }

      if ($index % 2 == 0) {
        $x = COL2;
      } else {
        $x = COL1;
      }
    }

    $meterNames = $graphData->getMeterNames();
    $gds = getStatDataForGraphByMeterNames($meterNames);
    $gd = getDominantGraphData($gds);
    $graph = createGraph($graphData->getName(), $gd, new Point($x,$y));
    drawLines($gds, $graph);
    $graph->drawLegends($gds);
    $graph->end();

    if ($index % 2 == 0 && $columns == 1) {
      $g_canvas->newPage();
    } elseif ($index % 6 == 0 && $columns == 2) {
      $g_canvas->newPage();
    }
  }
}

?>
