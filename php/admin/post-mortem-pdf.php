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
define("PERIOD", 3600/2);
define("X_GRID_MINOR", 300 * 1000);
define("X_GRID_MAJOR", 600 * 1000);
define("STEP", 1);

function admin_init_no_output($query="", $is_refresh=false)
{
  global $g_server_id;
  global $g_server_index;
  global $g_mbean_server;
  global $g_resin;
  global $g_server;
  global $g_page;

  if (! mbean_init()) {
    if ($g_server_id)
      debug( "admin_init_no_output:: Server ID FOUND: Resin: $g_page for server $g_server_id");
    else
      debug ("admin_init_no_output:: Resin: $g_page for server default");

    debug("admin_init_no_output:: $page = g_page, server = $g_server, query = $query, refresh = $is_refresh");

    return false;
  }

  return true;
}

function getRestartTime($stat) {

  $index = $g_server->SelfServer->ClusterIndex;
  $now = time();
  $start = $now - 7 * 24 * 3600;

  $restart_list = $stat->getStartTimes($index, $start * 1000, $now * 1000);

  if (empty($restart_list)) {
    debug( "getRestartTime:: No server restarts have been found in the last 7 days.");
    return null;
  }


  $form_time = $_REQUEST['time'];

  if (in_array($form_time, $restart_list)) {
    $restart_ms = $form_time;
  } else {
    sort($restart_list);
    $restart_ms = $restart_list[count($restart_list) - 1];
  }  
  $restart_time = floor($restart_ms / 1000);

  return $restart_time;
}


if (! admin_init_no_output()) {
  debug("Failed to load admin, die");
  return;
} else {
    debug("admin_init successful");
}


initPDF();
debug("initialized PDF");


startDoc();
debug("Started new document PDF");

$logo = $pdf.load_image("auto", "images/caucho-logo.jpg", ""); 
if ($logo == -1)
  throw new Exception("Error: " + $pdf.get_errmsg());

$pdf.fit_image($logo, 50.0, 500.0, ""); 

$pdf.close_image($logo);


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

function writeFooter() {
  global $page;
  global $canvas;
  global $serverID;
  global $restart_time;
  $time = date("Y-m-d H:i", $restart_time);
  $page +=1;
  $canvas->setFont("Helvetica-Bold", 8);
  $canvas->writeText(new Point(175, 10), "Postmortem Analysis \t\t $time \t\t $serverID \t\t  \t\t\t \t page $page");
}

$index = $g_server->SelfServer->ClusterIndex;
$si = sprintf("%02d", $index);

$restart_time = getRestartTime($stat);

if ($_REQUEST['period']) {
  $period = (int) $_REQUEST['period']; 
} else {
  $period = PERIOD;
}
$end = $restart_time;
$start = $end - $period;



$canvas->setFont("Helvetica-Bold", 16);
$canvas->writeText(new Point(175,775), "Restart at " . date("Y-m-d H:i", $restart_time));


$full_names = $stat->statisticsNames();




function findStats(String $category, String $subcategory=null) {
  global $start;
  global $end;
  global $stat;
  global $statList;
  global $si;

  $map = array();
  foreach ($statList as $statItem) {
    if ($statItem->server != $si) continue;
    if ($category == $statItem->category) {
      if ($subcategory && $subcategory == $statItem->subcategory) {
	$map[$statItem->name]= $stat->statisticsData($statItem->fullName, $start * 1000, $end * 1000,
                                    STEP * 1000);
      }
    }
  }
  return $map;
}


function findStatByName(String $name, String $subcategory="Health", String $category="Resin") {
  global $start;
  global $end;
  global $stat;
  global $statList;
  global $si;


  $arr = array();
  foreach ($statList as $statItem) {
    if ($statItem->server != $si) continue;
    if($subcategory==$statItem->subcateogry) {
      debug(" NAME " . $statItem->name); 
    }
    if ($name == $statItem->name && $category == $statItem->category) {
	$arr = $stat->statisticsData($statItem->fullName, $start * 1000, $end * 1000,
                                    STEP * 1000);
    }
  }
  return $arr;
}


class GraphData {
  public $name;
  public $dataLine;
  public $max;
  public $yincrement;
  public $color;

  function __toString() {
    return "GraphData name $this->name dataLine $this->dataLine max $this->max yincrement $this->yincrement";
  }

  function validate() {

    if (sizeof($this->dataLine)==0) {
      debug(" no data in " . $this->name);
      return false;
    }

    if ($this->max==0) {
      $this->max=10;
      $this->yincrement=1;
    }

    
    return true;
  }
}

function calcYincrement($max) {
  $yincrement = (int)($max / 3);

  $div = 5;

  if ($max > 5000000000) {
	$div = 1000000000;
  } elseif ($max > 5000000000) {
	$div = 1000000000;
  } elseif ($max > 500000000) {
	$div = 100000000;
  } elseif ($max > 50000000) {
	$div = 10000000;
  } elseif ($max > 5000000) {
	$div = 1000000;
  } elseif ($max > 500000) {
	$div = 100000;
  } elseif ($max > 50000) {
	$div = 10000;
  } elseif ($max > 5000) {
	$div = 1000;
  } elseif ($max > 500) {
	$div = 100;
  } elseif ($max > 50) {
	$div = 10;
  }
  
  $yincrement = $yincrement - ($yincrement % $div); //make the increment divisible by 5


  if ($yincrement == 0) {
      $yincrement = round($max / 5, 2);
  }
  return $yincrement;
}


function getStatDataForGraphBySubcategory($subcategory, $category="Resin", $nameMatch=null) {
  global $blue, $red, $orange, $purple, $green, $cyan, $brown, $black;
  $cindex = 0;
  $gds = array();	
  $map=findStats($category, $subcategory);
  $colors = array($blue, $red, $orange, $purple, $green, $cyan, $brown, $black, $blue, $red, $orange, $purple, $green, $cyan, $brown, $black);

  foreach ($map as $name => $data) {
	$dataLine = array();
  	$max = -100;
	$process =  true; 
	if($nameMatch) {
		if(!strstr($name, $nameMatch)){
			$process = false;
		}
	}
	if ($process) {
		debug(" $name -------------------- ");
  		foreach($data as $d) {  
    			$value = $d->value;
    			$time = $d->time;
			debug(" $time  --- $value  ");

    			array_push($dataLine, new Point($time, $value));
    			if ($value > $max) $max = $value;
  		}
  		$gd = new GraphData();
  		$gd->name = $name;
  		$gd->dataLine = $dataLine;
  		$gd->yincrement = calcYincrement($max);
  		$gd->max = $max + ($max * 0.05) ;
  		$gd->color=$colors[$cindex];
		array_push($gds, $gd);
		$cindex++;

	}
  }



  return $gds;
}

function getStatDataForGraph($name, $subcategory, $color=$blue, $category="Resin") {

  $data=findStatByName($name, $subcategory, $category);
  $dataLine = array();
  $max = -100;
  foreach($data as $d) {
    
    $value = $d->value;
    $hour = $d->time;
    array_push($dataLine, new Point($hour, $value));
    if ($value > $max) $max = $value;
  }

  $gd = new GraphData();
  $gd->name = $name;
  $gd->dataLine = $dataLine;
  $gd->max = $max + ($max * 0.05) ;
  $gd->yincrement = calcYincrement($max);
  $gd->color=$color;

  return $gd;
}


function displayTimeLabel($time){
    return strftime("%H:%M", $time/1000);
}

function createGraph(String $title, GraphData $gd, Point $origin, boolean $displayYLabels=true, Size $gsize=GRAPH_SIZE_6_TO_PAGE) {
  global $start;
  global $end;
  global $canvas;
  global $lightGrey;
  global $grey;
  global $darkGrey;
  global $black;

  $graph = new Graph($origin, $gsize, new Range($start * 1000, $end * 1000), new Range(0,$gd->max));
  $graph->start();


  $valid = $gd->validate();

  if ($valid) {
    $canvas->setColor($black);
    $canvas->setFont("Helvetica-Bold", 12);
    $graph->drawTitle($title);

    $canvas->setColor($lightGrey);
    $graph->drawGridLines(X_GRID_MINOR, $gd->yincrement/2);

    $canvas->setColor($grey);
    $graph->drawGridLines(X_GRID_MAJOR, $gd->yincrement);

    $canvas->setColor($black);
    $graph->drawGrid();

    if ($displayYLabels) {
      $graph->drawYGridLabels($gd->yincrement);
    }
    $graph->drawXGridLabels(X_GRID_MAJOR, "displayTimeLabel");
  } else {
    $canvas->setColor($black);
    $canvas->setFont("Helvetica-Bold", 12);
    $graph->drawTitle($title . " NO DATA");
    $canvas->setColor($darkGrey);
    $graph->drawGrid();

  }
  return $graph;
}


function getDominantGraphData($gds) {
  $gdd = $gds[0];
  foreach($gds as $gd) {
    if ($gd->max > $gdd->max) {
      $gdd=$gd;
    }
  }
  return $gdd;
}

class Stat {
  private $server;
  private $category;
  private $subcategory;
  private $fullName;
  private $elements;
  private $name;
 


  function __construct() {
    $args = func_get_args();
    $this->fullName = $args[0];
    $arr = explode("|", $this->fullName);
    $this->elements = $arr;

    $this->server = $arr[0];

    $isResin = true;
    
    $this->category = $arr[1];
    $this->subcategory = $arr[2];  

    $arr = array_slice($arr, 3); 

    $this->name = implode(" ", $arr);
    debug("name " . $this->name);
  }

  function __get($name) {
    return $this->$name;
  }


  function __toString() {
    return " name=" . $this->name . "\t\t\t\tserver=" . $this->server .  " category=" . $this->category . " subcategory=" . $this->subcategory ;
  }
}

$statList = array();

foreach ($full_names as $full_name)  {
  debug($full_name);
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

function pdf_format_memory($memory)
{
  return sprintf("%.2f M", $memory / (1024 * 1024))
}

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


function drawLines($gds, $graph) {
  global $canvas;
  foreach($gds as $gd) {
    if ($gd->validate()) {
      $canvas->setColor($gd->color);
      $graph->drawLine($gd->dataLine);
    }
  }
}

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
debug("--------------------");

$gds = getStatDataForGraphBySubcategory("Memory", "JVM", "GC Time");

$gd = getDominantGraphData($gds);

$graph = createGraph("GC Time", $gd, new Point(COL2, ROW1));

drawLines($gds, $graph);
$graph->drawLegends($gds);


$graph->end();


debug("--------------------");
debug("--------------------");
debug("--------------------");
debug("--------------------");

//--

$gd3 = getStatDataForGraph("Tenured Memory Free", "Memory", $blue, "JVM");
$gd1 = getStatDataForGraph("Heap Memory Free", "Memory", $red, "JVM");
$gd2 = getStatDataForGraph("PermGen Memory Free", "Memory", $purple, "JVM");

$gds = array($gd1, $gd2, $gd3);

$gd = getDominantGraphData($gds);

if( $gd->yincrement > (4 * MILLION)) {
  $gd->yincrement = round($gd->yincrement/MILLION);
  $gd->yincrement = $gd->yincrement * MILLION;
  if($gd->yincrement > (35 * MILLION) && $gd->yincrement < (75 * MILLION)) {
    $gd->yincrement = 50 * MILLION;
  }
}

$graph = createGraph("Memory", $gd, new Point(COL1,ROW1), false);

function displayLabelMeg($value) {
  return "" . $value / MILLION . " M";
}

$graph->drawYGridLabels($gd->yincrement, "displayLabelMeg", -28);

drawLines($gds, $graph);
$graph->drawLegends($gds);


$graph->end();




//--

//00|OS|CPU|Unix Load Avg

//--


$gds = getStatDataForGraphBySubcategory("CPU", "OS", "Active");

$gd = getDominantGraphData($gds);

$graph = createGraph("CPU Load ", $gd, new Point(COL2,ROW2));

$canvas->setColor($gd->color);

drawLines($gds, $graph);
$graph->drawLegends($gds);
$graph->end();


//--


$gd = getStatDataForGraph("File Descriptor Count", "Process", $blue, "OS");


$graph = createGraph("File Descriptor", $gd, new Point(COL1,ROW2));

$canvas->setColor($gd->color);
$graph->drawLine($gd->dataLine);


$graph->end();


//--


//---

$gd = getStatDataForGraph("Connection Active", "Database", $red);


$graph = createGraph("Database Connection Active", $gd, new Point(COL1,ROW3));

$canvas->setColor($gd->color);
$graph->drawLine($gd->dataLine);

$graph->end();

//---


$gd1 = getStatDataForGraph("Query Time", "Database", $red);
$gd2 = getStatDataForGraph("Query Time Max", "Database", $blue);
$gds = array($gd1, $gd2);
$gd = getDominantGraphData($gds);

$graph = createGraph("Database Query Time", $gd, new Point(COL2,ROW3));

drawLines($gds, $graph);
$graph->drawLegends($gds);


$graph->end();


//---


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
