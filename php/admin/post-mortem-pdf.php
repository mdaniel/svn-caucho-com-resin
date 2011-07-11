<?php

require_once "WEB-INF/php/inc.php";
require_once 'pdfGraph.php';


define("ROW1",     500);
define("ROW2",     300);
define("ROW3",     100);
define("COL1",     50);
define("COL2",     305);
define("MILLION", 1000000);
define("GRAPH_SIZE_6_TO_PAGE", new Size(175, 125));
define("GRAPH_SIZE", new Size(175, 125));
define("BIG_GRAPH_SIZE", new Size(400, 300));

define("PERIOD", 3600/2);
define("X_GRID_MINOR", 300 * 1000);
define("X_GRID_MAJOR", 600 * 1000);
define("STEP", 1);

$period = $_REQUEST['period'] ? (int) $_REQUEST['period'] : PERIOD; 

$majorTicks = $_REQUEST['majorTicks'] ? (int) ($_REQUEST['majorTicks'] * 1000) : X_GRID_MAJOR; 
$minorTicks = $_REQUEST['minorTicks'] ? (int) ($_REQUEST['minorTicks'] * 1000) : X_GRID_MINOR; 

debug("$majorTicks $minorTicks");



if (! admin_init_no_output()) {
  debug("Failed to load admin, die");
  return;
} else {
    debug("admin_init successful");
}


initPDF();

startDoc();


$mbean_server = $g_mbean_server;
$resin = $g_resin;
$server = $g_server;
$runtime = $g_mbean_server->lookup("java.lang:type=Runtime");
$os = $g_mbean_server->lookup("java.lang:type=OperatingSystem");
$log_mbean = $mbean_server->lookup("resin:type=LogService");


if ($g_mbean_server)
  $stat = $g_mbean_server->lookup("resin:type=StatService");



if (! $stat) {
  debug("Postmortem analysis:: requires Resin Professional and a <resin:StatService/> and <resin:LogService/> defined in
  the resin.xml.");
    return;
}

$canvas->setFont("Helvetica-Bold", 26);
$canvas->writeText(new Point(175,800), "Postmortem Analysis ");

$page = 0;

$index = $g_server->SelfServer->ClusterIndex;
$si = sprintf("%02d", $index);

$restart_time = getRestartTime($stat);

$end = $restart_time;
$start = $end - $period;



$canvas->setFont("Helvetica-Bold", 16);
$canvas->writeText(new Point(175,775), "Restart at " . date("Y-m-d H:i", $restart_time));


$full_names = $stat->statisticsNames();





$statList = array();

foreach ($full_names as $full_name)  {
  //debug($full_name);
  $arr = explode("|", $full_name);
  array_push($statList,new Stat($full_name)) 
}

//--

$canvas->setColor($black);
$canvas->moveTo(new Point(0, 770));
$canvas->lineTo(new Point(595, 770));
$canvas->stroke();

$x = 20;
$y = 750;
$yinc = 12;

$serverID = $server->Id ? $server->Id : '""';
$userName = $resin->UserName;
$ipAddress = $runtime->Name;
$resinVersion = $resin->Version;
$jvm = "$runtime->VmName  $runtime->VmVersion";
$machine = "$os->AvailableProcessors $os->Name $os->Arch $os->Version";

$start_time = $server->StartTime->time / 1000;
$now = $server->CurrentTime->time / 1000;
$uptime = $now - $start_time;
$ups = sprintf("%d days %02d:%02d:%02d",
                   $uptime / (24 * 3600),
                   $uptime / 3600 % 24,
                   $uptime / 60 % 60,
                   $uptime % 60) . " -- " . format_datetime($server->StartTime);


writeFooter();


$canvas->setFont("Helvetica-Bold", 9);
$canvas->writeText(new Point($x,$y), "$resinVersion ");
$y -= $yinc;
$canvas->writeText(new Point($x,$y), "$jvm $machine  ");
$y -= $yinc;
$canvas->writeText(new Point($x,$y), "$serverID at $ipAddress running as $userName ");
$y -= $yinc;
$y -= $yinc;
$canvas->writeText(new Point($x,$y), "$resin->WatchdogStartMessage");
$y -= $yinc;
$canvas->writeText(new Point($x,$y), "$ups \t\t state($server->State)");


$x +=375;
$y = 750;


$totalHeap = pdf_format_memory($server->RuntimeMemory);
$freeHeap = pdf_format_memory($server->RuntimeMemoryFree);
$osFreeSwap = pdf_format_memory($os->FreeSwapSpaceSize);
$osTotalSwap = pdf_format_memory($os->TotalSwapSpaceSize);
$osFreePhysical = pdf_format_memory($os->FreePhysicalMemorySize);
$osFreeTotal = pdf_format_memory($os->TotalPhysicalMemorySize);

$canvas->writeText(new Point($x,$y), "JVM Heap:        \t\t\t $totalHeap");
$y -= $yinc;
$canvas->writeText(new Point($x,$y), "JVM Free Heap: \t\t $freeHeap");
$y -= $yinc;
$canvas->writeText(new Point($x,$y), "OS Free Swap: \t\t $osFreeSwap");
$y -= $yinc;
$canvas->writeText(new Point($x,$y), "OS Total Swap: \t\t $osTotalSwap");
$y -= $yinc;
$canvas->writeText(new Point($x,$y), "OS Physical:    \t\t\t $osFreeTotal");
$y -= $yinc;

$canvas->setColor($black);
$canvas->moveTo(new Point(0, 680));
$canvas->lineTo(new Point(595, 680));
$canvas->stroke();


//--
//00|Resin|Health|Resin
//00|Resin|Health|Resin|ConnectionPool
//00|Resin|Health|Resin|Cpu
//00|Resin|Health|Resin|HealthSystem
//00|Resin|Health|Resin|Heartbeat
//00|Resin|Health|Resin|JvmDeadlock
//00|Resin|Health|Resin|MemoryPermGen
//00|Resin|Health|Resin|MemoryTenured
//00|Resin|Health|Resin|Transaction



$gd1 = getStatDataForGraph("Resin", "Health", $black);
$gd2 = getStatDataForGraph("Resin ConnectionPool", "Health", $green);
$gd3 = getStatDataForGraph("Resin HealthSystem", "Health", $blue);
$gd4 = getStatDataForGraph("Resin HeartBeat", "Health", $purple);
$gd5 = getStatDataForGraph("Resin JvmDeadlock", "Health", $orange);
$gd6 = getStatDataForGraph("Resin MemoryPermGen", "Health", $cyan);
$gd7 = getStatDataForGraph("Resin MemoryTenured", "Health", $brown);
$gd8 = getStatDataForGraph("Resin Transaction", "Health", $red);

$gds = array($gd1, $gd2, $gd3, $gd4, $gd5, $gd6, $gd7, $gd8);

$gd = getDominantGraphData($gds);

$gd->max=4;
$gd->yincrement=1;

$graph = createGraph("Health", $gd, new Point((COL2+COL1)/2,ROW1), false);

$healthStatus = array(0 => "UNKNOWN", 1 => "OK", 2=> "WARN", 3=>"CRITICAL", 4=>"FATAL");

function displayLabelStatus($value) {
  global $healthStatus;
  return $healthStatus[$value];
}

$graph->drawYGridLabels($gd->yincrement, "displayLabelStatus", -60);



drawLines($gds, $graph);
$graph->drawLegends($gds);

$graph->end();






//----------------- Request Count

$title = "Request Count";
$gd = getStatDataForGraph($title, "Http");
$graph = createGraph($title, $gd, new Point(COL1,ROW2));



$canvas->setColor($blue);
$graph->drawLine($gd->dataLine);



$graph->end();



//--------- Request Time


$gd1 = getStatDataForGraph("Request Time", "Http", $red);
$gd2 = getStatDataForGraph("Request Time Max", "Http", $blue);
$gds = array($gd1, $gd2);

$gd = getDominantGraphData($gds);

$graph = createGraph("Request Time ", $gd, new Point(COL2,ROW2));


drawLines($gds, $graph);
$graph->drawLegends($gds);


$graph->end();



//------------ JVM Threads

$gd1 = getStatDataForGraph("JVM Thread Count", "Thread", $red, "JVM");
$gd2 = getStatDataForGraph("JVM Runnable Count", "Thread", $cyan , "JVM");
$gd3 = getStatDataForGraph("JVM Native Count", "Thread", $brown , "JVM");
$gd4 = getStatDataForGraph("JVM Waiting Count", "Thread", $orange, "JVM");
$gd5 = getStatDataForGraph("JVM Blocked Count", "Thread", $blue, "JVM");

$gds = array($gd1, $gd2, $gd3, $gd4, $gd5);

$gd = getDominantGraphData($gds);

$graph = createGraph("JVM Threads", $gd, new Point(COL1,ROW3));


drawLines($gds, $graph);
$graph->drawLegends($gds);


$graph->end();

$gd1 = getStatDataForGraph("Thread Count", "Thread", $red);
$gd2 = getStatDataForGraph("Thread Active Count", "Thread", $cyan);
$gd3 = getStatDataForGraph("Thread Idle Count", "Thread", $brown);
$gd4 = getStatDataForGraph("Thread Task Queue", "Thread", $orange);
$gd5 = getStatDataForGraph("Thread Overflow Count", "Thread", $blue);

$gds = array($gd1, $gd2, $gd3, $gd4, $gd5);

$gd = getDominantGraphData($gds);

$graph = createGraph("Resin Threads", $gd, new Point(COL2,ROW3));


drawLines($gds, $graph);
$graph->drawLegends($gds);



$graph->end();


//-----------




$pdf->end_page();
$pdf->begin_page(595, 842);


writeFooter();
//
//0|JVM|Memory|GC Time|Copy
//00|JVM|Memory|GC Time|MarkSweepCompact

//--

//|
$gds = getStatDataForGraphBySubcategory("Memory", "JVM", "Memory Free");

$gd = getDominantGraphData($gds);

$graph = createGraph("Memory", $gd, new Point(COL1,ROW3), true, BIG_GRAPH_SIZE, true);


drawLines($gds, $graph);
$graph->drawLegends($gds);


$graph->end();


//--

$gds = getStatDataForGraphBySubcategory("Memory", "JVM", "GC Time");

$gd = getDominantGraphData($gds);

//function createGraph(String $title, GraphData $gd, Point $origin, boolean $displayYLabels=true, Size $gsize=GRAPH_SIZE_6_TO_PAGE, boolean $trace=false) {

$graph = createGraph("GC Time", $gd, new Point(COL1, ROW1), true, BIG_GRAPH_SIZE);

drawLines($gds, $graph);
$graph->drawLegends($gds);


$graph->end();



//--



//--


$pdf->end_page();

$pdf->begin_page(595, 842);
writeFooter();


$gds = getStatDataForGraphBySubcategory("CPU", "OS", "Active");

$gd = getDominantGraphData($gds);

$graph = createGraph("CPU Load ", $gd, new Point(COL1, ROW1), true, BIG_GRAPH_SIZE);

$canvas->setColor($gd->color);

drawLines($gds, $graph);
$graph->drawLegends($gds);
$graph->end();


//--



//--


$gd = getStatDataForGraph("File Descriptor Count", "Process", $blue, "OS");


$graph = createGraph("File Descriptor", $gd, new Point(COL1,ROW3), true, BIG_GRAPH_SIZE);

$canvas->setColor($gd->color);
$graph->drawLine($gd->dataLine);


$graph->end();




//---

$pdf->end_page();

$pdf->begin_page(595, 842);
writeFooter();


$gd = getStatDataForGraph("Connection Active", "Database", $red);
$graph = createGraph("Database Connection Active", $gd, new Point(COL1, ROW1), true, BIG_GRAPH_SIZE);

$canvas->setColor($gd->color);
$graph->drawLine($gd->dataLine);

$graph->end();

//---


$gd1 = getStatDataForGraph("Query Time", "Database", $red);
$gd2 = getStatDataForGraph("Query Time Max", "Database", $blue);
$gds = array($gd1, $gd2);
$gd = getDominantGraphData($gds);


$graph = createGraph("Database Query Time", $gd, new Point(COL1, ROW3), true, BIG_GRAPH_SIZE);

drawLines($gds, $graph);
$graph->drawLegends($gds);


$graph->end();




//---

$pdf->end_page();

$pdf->begin_page(595, 842);
writeFooter();


$gds = getStatDataForGraphBySubcategory("Network", "OS", "tcp-");

$gd = getDominantGraphData($gds);

$graph = createGraph("Netstat ", $gd, new Point(60, 200), true, new Size(400, 400), false);

$canvas->setColor($gd->color);

drawLines($gds, $graph);
$graph->drawLegends($gds);
$graph->end();


$pdf->end_page();

$pdf->begin_page(595, 842);
writeFooter();


$messages = $log_mbean->findMessages("warning",
                                       ($restart_time - $period) * 1000,
                                       ($restart_time) * 1000);



$y = 800;

$canvas->setFont("Helvetica-Bold", 8);

$index=1;
foreach ($messages as $message) {
  $ts = strftime("%Y-%m-%d\t%H:%M:%S", $message->timestamp / 1000);
  $canvas->writeText(new Point(20,$y), "$ts");
  $canvas->writeText(new Point(110,$y), "$message->level");
  $canvas->writeText(new Point(150,$y), "$message->message");
  $y -= $yinc;
  if ($index % 65 == 0) {
    $pdf->end_page();
    $pdf->begin_page(595, 842);
    $canvas->setFont("Helvetica-Bold", 8);
    $y=800;
    writeFooter();
  }
  $index++;
}

//--
$pdf->end_page();
$pdf->end_document();

$document = $pdf->get_buffer();
$length = strlen($document);



$filename = "postmortem.pdf";



header("Content-Type:application/pdf");



header("Content-Length:" . $length);



header("Content-Disposition:inline; filename=" . $filename);



echo($document);



unset($document);



pdf_delete($pdf);

?>
