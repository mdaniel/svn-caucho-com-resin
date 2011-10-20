<?php

require_once "WEB-INF/php/inc.php";


import java.lang.System;
/*
$g_colors = array("#ff3030", // red
                  "#30b0ff", // azure
                  "#906000", // brown
                  "#ff9030", // orange
                  "#3030ff", // blue
                  "#000000", // black
                  "#50b000", // green
                  "#d030d0", // magenta
                  "#008080", // cyan
                  "#b03060", // rose
                  "#e090ff", // indigo
                  "#c0c0c0", // gray
                  "#408040"); // forest green
*/

$g_pdf_colors = array(
                  new RGBColor(0xff, 0x30, 0x30), // red
                  new RGBColor(0x30, 0xb0, 0xff), // azure
                  new RGBColor(0x90, 0x60, 0x00), // brown
                  new RGBColor(0xff, 0x90, 0x30), // orange
                  new RGBColor(0x30, 0x30, 0xff), // blue
                  new RGBColor(0x00, 0x00, 0x00), // black
                  new RGBColor(0x50, 0xb0, 0x00), // green
                  new RGBColor(0xd0, 0x30, 0xd0), // magenta
                  new RGBColor(0x00, 0x80, 0x80), // cyan
                  new RGBColor(0xb0, 0x30, 0x60), // rose
                  new RGBColor(0xe0, 0x90, 0xff), // indigo
                  new RGBColor(0xc0, 0xc0, 0xc0), // gray
                  new RGBColor(0x40, 0x80, 0x40)); // forest green

$black = new RGBColor(0.0, 0.0, 0.0);
$red = new RGBColor(1.0, 0.0, 0.0);
$green = new RGBColor(0.0, 1.0, 0.0);
$blue = new RGBColor(0.0, 0.0, 1.0);
$darkGrey = new RGBColor(0.2, 0.2, 0.2);
$lightGrey = new RGBColor(0.9, 0.9, 0.9);
$grey = new RGBColor(0.45, 0.45, 0.45);
$medGrey = new RGBColor(0.6, 0.6, 0.6);
$purple = new RGBColor(0.45, 0.2, 0.45);
$orange = new RGBColor(1.0, 0.66, 0.0);
$cyan = new RGBColor(0.0, 0.66, 1.0);
$brown = new RGBColor(0.66, 0.20, 0.20);

$x = 20;
$y = 750;
$yinc = 12;

if (! admin_init_no_output()) {
  debug("Failed to load admin, die");
  return;
} else {
    debug("admin_init successful");
}


$mbean_server = $g_mbean_server;
$resin = $g_resin;
$server = $g_server;



if ($g_mbean_server)
  $stat = $g_mbean_server->lookup("resin:type=StatService");

if (! $stat) {
  debug("Postmortem analysis:: requires Resin Professional and a <resin:StatService/> and <resin:LogService/> defined in
  the resin.xml.");
    return;
}


$mbean_server = $g_mbean_server;
$resin = $g_resin;
$server = $g_server;
$runtime = $g_mbean_server->lookup("java.lang:type=Runtime");
$os = $g_mbean_server->lookup("java.lang:type=OperatingSystem");
$log_mbean = $mbean_server->lookup("resin:type=LogService");


function admin_pdf_summary()
{
  global $x, $y, $yinc, $server, $runtime, $os, $log_mbean, $g_canvas, $resin;
  global $pageName;

  $summary = admin_pdf_summary_fill();

  if (! $summary)
    return;

/*
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
                 $uptime % 60) . " -- started " . format_datetime($server->StartTime);


  $totalHeap = pdf_format_memory($summary[->RuntimeMemory);
  $freeHeap = pdf_format_memory($server->RuntimeMemoryFree);
  $osFreeSwap = pdf_format_memory($os->FreeSwapSpaceSize);
  $osTotalSwap = pdf_format_memory($os->TotalSwapSpaceSize);
  $osFreePhysical = pdf_format_memory($os->FreePhysicalMemorySize);
  $osFreeTotal = pdf_format_memory($os->TotalPhysicalMemorySize);
*/

  $col = 375;
  
//  $g_canvas->write_section("Report: $pageName");

  $g_canvas->setDataFontAndSize(9);
  
  $g_canvas->write_text_x(20, $summary["resinVersion"]);
  $g_canvas->write_text_x($col, "JVM Heap: " . $summary["totalHeap"]);
  $g_canvas->write_text_newline();

  $g_canvas->write_text_x(20, $summary["jvm"] . " " .  $summary['machine']);
  $g_canvas->write_text_x($col, "JVM Free Heap: " . $summary['freeHeap']);
  $g_canvas->write_text_newline();
  
  $g_canvas->write_text_x(20, "{$summary['serverID']} at {$summary['ipAddress']} running as {$summary['userName']} ");
  $g_canvas->write_text_x($col, "OS Free Swap: {$summary['osFreeSwap']}");
  $g_canvas->write_text_newline();
  
  $g_canvas->write_text_x(20, $summary['watchdogStartMessage']);
  $g_canvas->write_text_x($col, "OS Total Swap: {$summary['osTotalSwap']}");
  $g_canvas->write_text_newline();
  
  $g_canvas->write_text_x(20, "uptime {$summary['ups']} [{$summary['server_state']}]");
  $g_canvas->write_text_x($col, "OS Physical: {$summary['osFreeTotal']}");
  $g_canvas->write_text_newline();
  
  $license_array = $summary["licenses"];
  foreach($license_array as $license_msg) {
    $g_canvas->write_text_x(20, $license_msg);
    $g_canvas->write_text_newline();
  }
  
  $g_canvas->write_hrule();
}

function admin_pdf_summary_fill()
{
  $dump = admin_pdf_snapshot("Resin|JmxDump");
  
  $jmx =& $dump["jmx"];

  if (! $jmx)
    return;

  $server = $jmx["resin:type=Server"];
  $resin = $jmx["resin:type=Resin"];  
  $runtime = $jmx["java.lang:type=Runtime"];
  $os = $jmx["java.lang:type=OperatingSystem"];
  $licenseStore = $jmx["resin:type=LicenseStore"];

  $summary = array();

  $summary["serverID"] = $server["Id"] ? $server["Id"] : '""';
  
  $summary["userName"] = $resin["UserName"];
  $summary["ipAddress"] = $runtime["Name"];
  $summary["resinVersion"] = $resin["Version"];
  $summary["jvm"] = "{$runtime['VmName']} {$runtime['VmVersion']}";
  $summary["machine"] = "{$os['AvailableProcessors']} {$os['Name']} {$os['Arch']} {$os['Version']}";
  $summary["watchdogStartMessage"] = $resin["WatchdogStartMessage"];

  $q_date = java("com.caucho.util.QDate");
  $start_time = $q_date->parseDate($server["StartTime"]) / 1000;

  $now = $q_date->parseDate($server["CurrentTime"]) / 1000;

  $uptime = $now - $start_time;

  $summary["ups"] = sprintf("%d days %02d:%02d:%02d",
                 $uptime / (24 * 3600),
                 $uptime / 3600 % 24,
                 $uptime / 60 % 60,
                 $uptime % 60)
                 . " -- started " . date("Y-m-d H:i", $start_time);

  $summary["server_state"] = $server["State"];                 


  $summary["totalHeap"] = pdf_format_memory($server["RuntimeMemory"]);
  $summary["freeHeap"] = pdf_format_memory($server["RuntimeMemoryFree"]);
  $summary["osFreeSwap"] = pdf_format_memory($os["FreeSwapSpaceSize"]);
  $summary["osTotalSwap"] = pdf_format_memory($os["TotalSwapSpaceSize"]);
  $summary["osFreePhysical"] = pdf_format_memory($os["FreePhysicalMemorySize"]);
  $summary["osFreeTotal"] = pdf_format_memory($os["TotalPhysicalMemorySize"]);
  
  $licenses = array();
  
  $license_names = $licenseStore["ValidLicenses"];
  foreach($license_names as $license_name) {
    $license = $jmx[$license_name];
    array_push($licenses, "{$license['Description']}, {$license['ExpireMessage']}");
  }
  
  $summary["licenses"] = $licenses;
    
  return $summary;
}

function pdf_graph_draw_lines($gds, $graph)
{
  global $g_canvas;

  $g_canvas->set_line_width(1);

  $gds = array_reverse($gds);

  foreach($gds as $gd) {
    if ($gd->validate()) {
      $g_canvas->setColor($gd->color);
      
      if (sizeof($gd->dataLine) != 0) {
      	$graph->draw_line_graph($gd->dataLine);
      }
    }
  }
}


class Stat {
  private $server;
  private $category;
  private $subcategory;
  private $fullName;
  private $elements;
  private $name;

  function statFromName($fullName, $server="00")
  {
    $this->fullName = $fullName;
    $arr = explode("|", $this->fullName);
    $this->elements = $arr;
    
    $this->category = $arr[0];
    $this->subcategory = $arr[1];  

    $arr = array_slice($arr, 2); 

    $this->name = implode(" ", $arr);

    $this->server = $server;
  }

  function __construct()
  {
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
    //debug("name " . $this->name);
  }

  function __get($name) {
    return $this->$name;
  }


  function __toString()
  {
    return " name=" . $this->name . "\t\t\t\tserver=" . $this->server .  " category=" . $this->category . " subcategory=" . $this->subcategory ;
  }

  function eq($that)
  {
    return $this->name == $that->name
           && $this->category == $that->category
           && $this->subcategory == $that->subcategory;
  }
}


function getStatDataForGraphByMeterNames($meterNames)
{
  // global $blue, $red, $orange, $purple, $green, $cyan, $brown, $black;
  global $g_pdf_colors;
  $cindex = 0;
  $colors = $g_pdf_colors;
  // array($blue, $red, $orange, $purple, $green, $cyan, $brown, $black, $blue, $red, $orange, $purple, $green, $cyan, $brown, $black);

  $gds = array();   
  foreach ($meterNames as $name) {
    $statItem = new Stat();
    $statItem->statFromName($name);
    $gd = getStatDataForGraphByStat($statItem);
    $gd->color = $colors[$cindex];
        
    array_push($gds, $gd);
        
    $cindex++;
  }

  return $gds;
}

function getStatDataForGraphByStat($theStat, $color=$blue)
{
  $data = findStatByStat($theStat);

  return admin_pdf_create_graph_data($theStat->name, $data, $blue)  
}

function admin_pdf_create_graph_data($name, $data, $color=$blue)
{
  debug("DATA " . sizeof($data));
  $dataLine = array();
  $max = -100;
  
  foreach($data as $d) {
    $time = $d->time;
    
    $value_avg = $d->value;
    $value_min = $d->min;
    $value_max = $d->max;

    if ($value_min < $value_max) {
      array_push($dataLine, new Point($time, $value_avg));
      array_push($dataLine, new Point($time + 0, $value_max));
      array_push($dataLine, new Point($time + 0, $value_min));
      array_push($dataLine, new Point($time + 0, $value_avg));
    }
    else {
      array_push($dataLine, new Point($time + 0, $value_max));
    }

    if ($max < $value_max)
      $max = $value_max;
  }

  $gd = new GraphData();
  $gd->name = $name;
  $gd->dataLine = $dataLine;
  $gd->max = $max + ($max * 0.05) ;
  $gd->yincrement = calcYincrement($max);
  $gd->color=$color;

  return $gd;
}

function findStats(String $category, String $subcategory=null)
{
  global $g_start;
  global $g_end;
  global $stat;
  global $statList;
  global $si;

  $map = array();
  foreach ($statList as $statItem) {
    if ($statItem->server != $si) continue;
    if ($category == $statItem->category) {
      if ($subcategory && $subcategory == $statItem->subcategory) {
	$map[$statItem->name]
          = admin_pdf_get_stat_item($stat, $statItem->fullName);
      }
    }
  }
  return $map;
}

function admin_pdf_get_stat_item($stat_mbean, $full_name)
{
  global $g_start, $g_end;
  
  $step = ($g_end - $g_start) / 500;
  
  if ($step < 120)
    $step = 1;
  
  $data = $stat_mbean->statisticsData($full_name,
                                      $g_start * 1000, $g_end * 1000,
                                      $step * 1000);

  return $data;                                      
}

function findStatByStat($theStat) {
  global $g_start;
  global $g_end;
  global $stat;
  global $statList;
  global $si;

  //$step = ($g_end - $g_start) / 1000;
  $step = ($g_end - $g_start) / 500;

  if ($step < 120)
    $step = 1;

  foreach ($statList as $statItem) {
    if ($statItem->server != $si) continue;
    if ($statItem->eq($theStat)) {
 	return $stat->statisticsData($statItem->fullName,
                                     $g_start * 1000, $g_end * 1000,
                                     $step * 1000);
      }
    
  }
}



function findStatByName(String $name,
                        String $subcategory="Health",
                        String $category="Resin")
{
  global $g_start;
  global $g_end;
  global $stat;
  global $statList;
  global $si;

  $arr = array();
  
  foreach ($statList as $statItem) {
    if ($statItem->server != $si)
      continue;
      
    if ($subcategory==$statItem->subcateogry) {
      //debug(" NAME " . $statItem->name); 
    }
    
    if ($name == $statItem->name && $category == $statItem->category) {
	$arr = $stat->statisticsData($statItem->fullName,
                                     $g_start * 1000, $g_end * 1000,
                                     STEP * 1000);
    }
  }
  return $arr;
}


function getMeterGraphPage($pdfName)
{
  global $stat;
  $mpages = $stat->getMeterGraphPages();
  
  foreach($mpages as $mg) {
    if ($mg->name == $pdfName) {
      return $mg;
    }
  }
}

function debug($msg) {
  //System::out->println($msg);
}


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


class Range {
  private $start;
  private $stop;

  function Range($start, $stop)
  {
    $this->start = (float) $start;
    $this->stop = (float) $stop;
  }

  function __set($name, $value)
  {
    $this->$name = (double) $value;
  }

  function __get($name)
  {
    return $this->$name;
  }

  function __toString()
  {
    $str = " (RANGE WIDTH:$this->start; HEIGHT:$this->stop;)";
    return $str;
  }

  function size()
  {
    return $this->stop - $this->start;
  }
}


class Size {
  private $width;
  private $height;

  function Size($width = 0, $height = 0)
  {
    $this->width = $width;
    $this->height = $height;
  }

  function __set($name, $value)
  {
    $this->$name = (double) $value;
  }

  function __get($name)
  {
    return $this->$name;
  }

  function __toString()
  {
    $str = " (SIZE WIDTH:$this->width; HEIGHT:$this->height;)";
    return $str;
  }
}

class Point {
  private $x;
  private $y;

  function Point($x = 0, $y = 0)
  {
    $this->x = (float) $x;
    $this->y = (float) $y;
  }


  function __set($name, $value)
  {
    $this->$name = (double) $value;
  }


  function __get($name)
  {
    return $this->$name;
  }


  function __toString()
  {
    $str = "POINT( X:$this->x; Y:$this->y;)";
    return $str;
  }
}

class Graph {
  private $pixelSize;
  private $xRange;
  private $yRange;
  private $g_canvas;
  private $title;
  private $pixelPerUnit;

  function Graph($pdf,
                 string $title,
                 Point $origin,
                 Size $pixelSize,
                 Range $xRange,
                 Range $yRange,
                 boolean $trace=false)
  {
    $this->title = $title;
    $this->canvas = new Canvas($pdf, $origin);
    $this->pixelSize = $pixelSize;
    $this->xRange = $xRange;
    $this->yRange = $yRange;
    $this->trace = $trace;
   

    if ($this->yRange->size()==0.0) {
       debug("YRANGE was 0 for " . $this->title);
       $this->valid=false;
    } else {
      $this->valid=true;
    }

    $this->pixelPerUnit = new Size();
    $this->pixelPerUnit->width = $this->pixelSize->width / $this->xRange->size();
    $this->pixelPerUnit->height = $this->pixelSize->height / $this->yRange->size();

    if ($this->pixelPerUnit->width == 0.0 || $this->pixelPerUnit->height == 0.0) {
       debug("pixel per unit was 0.0 " . $this->title);
       $this->valid=false;
       
    } else {
      $this->xOffsetPixels = $this->xRange->start * $this->pixelPerUnit->width;
      $this->yOffsetPixels = $this->yRange->start * $this->pixelPerUnit->height;
    }
    if ($this->trace) {
       $this->trace("$title graph created --------------------- ");
    }

  }

  function trace($msg) {
	if ($this->trace) debug("GRAPH " . $this->title . " " . $msg);
  }


  function __toString() {
    $str = "(GRAPH Canvas $this->canvas, XRANGE $this->xRange, YRANGE $this->yRange)";
    return $str;
  }

  function start() {
    $this->canvas->start();
  }

  function end() {
    $this->canvas->end();
  }


  function __destruct() {
    $this->canvas = null;
  }


  function convertPoint($point) {
    $convertedPoint = new Point();
    $convertedPoint->x = intval(($point->x  * $this->pixelPerUnit->width) - $this->xOffsetPixels);
    $convertedPoint->y = intval(($point->y  * $this->pixelPerUnit->height) - $this->yOffsetPixels);
    if ($convertedPoint->x > 1000 || $convertedPoint->x < 0) {
       debug("Point out of range x axis $convertedPoint->x  for " .  $this->title);
       $this->valid = false;
    }
    if ($convertedPoint->y > 1000 || $convertedPoint->y < 0) {
       debug("Point out of range y axis $convertedPoint->y for " .  $this->title);
       $this->valid = false;
    }
    return $convertedPoint;
  }

  function drawTitle($title=null) {
    $this->trace("drawTitle " . $title);

    if (!$title) $title = $this->title;
    $y = $this->pixelSize->height + 15;
    $x = 0.0;
    if ($this->valid) {
       $this->trace("drawTitle valid" );
       $this->canvas->writeText(new Point($x, $y), $title);
    } else {
      $this->trace("drawTitle no data" );
      $this->canvas->writeText(new Point($x, $y), $title . " no data");
    }
  }

  function drawLegends($legends, $point=new Point(0.0, -20)) {

    if (!$this->valid) {
       $this->trace("drawLegends NOT VALID" );
       return;
    }

    $this->trace("drawLegends" );

    $col2 =   (double) $this->pixelSize->width / 2;
    $index = 0;
    $yinc = -7;
    $initialYLoc = -20;
    $yloc = $initialYLoc;

    foreach ($legends as $legend) {
      if ($index % 2 == 0) {
	$xloc = 0.0;
      } else {
	$xloc = $col2;
      }
    
      $row = floor($index / 2);
      
      $yloc = ((($row) * $yinc) + $initialYLoc);

      $this->drawLegend($legend->color, $legend->name, new Point($xloc, $yloc));
      $index++;


    }
  }

  function drawLegend($color, $name, $point=new Point(0.0, -20)) {
    if (!$this->valid) {
       return;
    }

 
    $this->trace("drawLegend SINGLE " . $name);

    global $g_canvas;
    global $black;

    $x = $point->x;
    $y = $point->y;

    $g_canvas->setColor($color);
    $this->canvas->moveTo(new Point($x, $y+2.5));
    $this->canvas->lineTo(new Point($x+5, $y+5));
    $this->canvas->lineTo(new Point($x+10, $y+2.5));
    $this->canvas->lineTo(new Point($x+15, $y+2.5));
    $this->canvas->stroke();

    $g_canvas->setColor($black);
    $this->canvas->setFont("Helvetica-Bold", 6);
    $this->canvas->setColor($black);
    $this->canvas->writeText(new Point($x+20, $y), $name);


  }
  
  function draw_line_graph($dataLine)
  {
    if (!$this->valid) {
       return;
    }
    
    $this->trace("drawLine ");

    $this->canvas->moveTo($this->convertPoint($dataLine[0]));

    for ($index = 1; $index < sizeof($dataLine); $index++) {
      $p = $this->convertPoint($dataLine[$index]);
      if (!$this->valid) {
      	 break;
      }
      
      $this->canvas->lineTo($p);
    }

    $this->canvas->stroke();
  }

  function drawGrid()
  {
    $this->canvas->set_line_width(1.5);

    $this->trace("drawGrid ");

    $width = (double) $this->pixelSize->width;
    $height = (double) $this->pixelSize->height;
    $this->canvas->moveTo(new Point(0.0, 0.0));
    $this->canvas->lineTo(new Point($width, 0.0));
    $this->canvas->lineTo(new Point($width, $height));
    $this->canvas->lineTo(new Point(0.0, $height));
    $this->canvas->lineTo(new Point(0.0, 0.0));
    $this->canvas->stroke();
  }

  function drawGridLines($xstep, $ystep)
  {
    $this->canvas->set_line_width(0.5);

    if (!$ystep) {
      $this->valid = false;
      debug("No ystep was passed " .  $this->title);
    }

    if (!$this->valid) {
       return;
    }

    $this->trace("drawGridLines ");

    $width = intval($this->pixelSize->width);
    $height = intval($this->pixelSize->height);

    $xstep_width = $xstep * $this->pixelPerUnit->width;
    $ystep_width = $ystep * $this->pixelPerUnit->height;

    if ($xstep_width <= 0.0 || $ystep_width <= 0.0) {
       debug("          ====== Step width was 0 x $xstep_width y $ystep_width " . $this->title);
       debug("      ppu width    " . $this->pixelPerUnit->width);
       debug("      xstep     $xstep ");

       $this->valid = false;

       return;
    }

    for ($index = 0; $width >= (($index)*$xstep_width); $index++) {
      $currentX = intval($index*$xstep_width);
      $this->canvas->moveTo(new Point($currentX, 0.0));
      $this->canvas->lineTo(new Point($currentX, $height));
      $this->canvas->stroke();
    }    


    for ($index = 0; $height >= ($index*$ystep_width); $index++) {
      $currentY = intval($index*$ystep_width);
      $this->canvas->moveTo(new Point(0.0, $currentY));
      $this->canvas->lineTo(new Point($width, $currentY));
      $this->canvas->stroke();
    }    


  }

  function drawXGridLabels($xstep, $func)
  {
    if (!$this->valid) {
       return;
    }

    $this->trace("X drawXGridLabels xstep $xstep, func $func");


    $this->canvas->setFont("Helvetica-Bold", 9);
    $width =   (double) $this->pixelSize->width;

    $xstep_width = ($xstep) * $this->pixelPerUnit->width;

    for ($index = 0; $width >= ($index*$xstep_width); $index++) {
      $currentX = $index*$xstep_width;
      $stepValue = (int) $index * $xstep;
      $currentValue = $stepValue + (int) $this->xRange->start;
      $currentValue = intval($currentValue);

      if (!$func){
      	$currentLabel = $currentValue;
      } else {
	$currentLabel = $func($currentValue);
      }
      $this->canvas->writeText(new Point($currentX-3, -10), $currentLabel);
    }    
  }

  function drawYGridLabels($step, $func=null, $xpos=-28) {
    if (!$this->valid) {
       return;
    }

    $this->trace("Y drawYGridLabels xstep $step, func $func");


    $this->canvas->setFont("Helvetica-Bold", 9);
    $height = (double) $this->pixelSize->height;

    $step_width = ($step) * $this->pixelPerUnit->height;

    for ($index = 0; $height >= ($index*$step_width); $index++) {
      $currentYPixel = $index*$step_width;
      $currentYValue =	($index * $step) + $this->yRange->start;
      
      if ($func) {
	$currentLabel = $func($currentYValue);
      } else {
      	if ($currentYValue >      1000000000) {
	   $currentLabel = "" . $currentYValue / 1000000000 . "G";
	}
        elseif ($currentYValue > 1000000) {
	   $currentLabel = "" . $currentYValue / 1000000 . "M";
	}
        elseif ($currentYValue > 1000) {
	   $currentLabel = "" . $currentYValue / 1000 . "K";
	}
        else {
	  $currentLabel = $currentYValue; 
	}
      }

      $x = -5;
      
      $this->canvas->write_text_ralign_xy($x, $currentYPixel - 3,
                                          $currentLabel);
    }    
  }
}


function pdf_format_memory($memory)
{
  return sprintf("%.2f M", $memory / (1024 * 1024))
}

function admin_pdf_create_graph(Canvas $canvas,
                                $x, $y,
                                $width, $height,
                                String $title,
                                GraphData $gd,
                                boolean $displayYLabels=true,
                                boolean $trace=false)
{
  global $g_pdf;
  global $g_start;
  global $g_end;
  global $grey;
  global $lightGrey;
  global $medGrey;
  global $darkGrey;
  global $black;
  global $majorTicks, $minorTicks;

  $graph = new Graph($g_pdf, $title,
                     new Point($x, $y),
                     new Size($width, $height),
                     new Range($g_start * 1000, $g_end * 1000),
                     new Range(0, $gd->max),
                     $trace);
  $graph->start();

  $valid = $gd->validate();

  if ($valid) {
    $graph->canvas->setColor($black);
    $graph->canvas->setFont("Helvetica-Bold", 12);
    $graph->drawTitle($title);

    $graph->canvas->setColor($lightGrey);
    $graph->drawGridLines($minorTicks, $gd->yincrement/2);

    $graph->canvas->setColor($medGrey);
    $graph->drawGridLines($majorTicks, $gd->yincrement);

    $canvas->setColor($darkGrey);
    $graph->drawGrid();

    if ($displayYLabels) {
      $graph->drawYGridLabels($gd->yincrement);
    }

    $graph->drawXGridLabels($majorTicks, "displayTimeLabel");
  } else {
    debug("Not displaying graph $title because the data was not valid");
    $canvas->setColor($black);
    $canvas->setFont("Helvetica-Bold", 12);
    $graph->drawTitle($title);
    $canvas->setColor($darkGrey);
    $graph->drawGrid();
  }
  
  return $graph;
}


function admin_pdf_get_dominant_graph_data($gds)
{
  $gdd = $gds[0];
  foreach($gds as $gd) {
    if ($gd->max > $gdd->max) {
      $gdd=$gd;
    }
  }
  return $gdd;
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


function getStatDataForGraphBySubcategory($subcategory,
                                          $category="Resin",
                                          $nameMatch=null) {
  global $g_colors;
  $cindex = 0;
  $gds = array();	
  $map = findStats($category, $subcategory);

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
		//debug(" $name -------------------- ");
  		foreach($data as $d) {  
    			$value = $d->value;
    			$time = $d->time;
			//debug(" $time  --- $value  ");

    			array_push($dataLine, new Point($time, $value));
    			if ($value > $max) $max = $value;
  		}
  		$gd = new GraphData();
  		$gd->name = $name;
  		$gd->dataLine = $dataLine;
  		$gd->yincrement = calcYincrement($max);
  		$gd->max = $max + ($max * 0.05) ;
  		$gd->color=$g_colors[$cindex];
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



function displayTimeLabel($ms)
{
  $time = $ms / 1000;
  $tz = date_offset_get(new DateTime);
 
  if (($time + $tz) % (24 * 3600) == 0) {
    return strftime("%m-%d", $time);
  } else {
    return strftime("%H:%M", $time);
  }
}


function writeFooter()
{
  global $g_canvas;

  $g_canvas->writeFooter();
}


function getRestartTime($stat) {

  global $g_server;

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




function startsWith($haystack, $needle)
{
    $length = strlen($needle);
    return (substr($haystack, 0, $length) === $needle);
}

function endsWith($haystack, $needle)
{
    $length = strlen($needle);
    $start  = $length * -1; //negative
    return (substr($haystack, $start) === $needle);
}

function  my_error_handler($error_type, $error_msg, $errfile, $errline) {
  if(!startsWith($error_msg,"Can't access private field")) {
    debug("ERROR HANDLER: type $error_type, msg $error_msg, file $errfile, lineno $errline");
  }
} 

set_error_handler('my_error_handler'); 

function initPDF()
{
  global $g_pdf, $g_canvas;
  
  $g_pdf = new PDF();
  $g_canvas = new Canvas($g_pdf, new Point(0,0));
}

function startDoc()
{
  global $g_pdf;
  $g_pdf->begin_document();
  $g_pdf->begin_page(595, 842);
}



class Color {

  function doSetColor($canvas) {
  }
}

class RGBColor {
  private $red;
  private $green;
  private $blue;

  function RGBColor($red, $green, $blue)
  {
    if ($red > 1)
      $red = $red / 255;
    if ($green > 1)
      $green = $green / 255;
    if ($blue > 1)
      $blue = $blue / 255;
      
    $this->red = $red;
    $this->green = $green;
    $this->blue = $blue;
  }

  function doSetColor($canvas)
  {
    $canvas->setRGBColor($this->red, $this->green, $this->blue);
  } 
}

class Canvas {
  private $origin;
  private $pdf;

  private $text_y;
  private $text_y_inc = 12;

  private $header_left_text;
  private $header_center_text;
  private $header_right_text;

  private $footer_left_text;
  private $footer_right_text;

  private $width = 595;
  private $height = 842;

  private $graph_cols = 2;
  private $graph_rows = 3;

  private $graph_x;
  private $graph_y;
  private $graph_index;

  function Canvas($pdf, $origin)
  {
    $this->pdf = $pdf;
    $this->origin =  $origin;
    $this->lastTextPos = new Point(0,0); //to fix problem with Resin PDF Lib clone
    $this->initPage();
  }

  function set_header_left($text)
  {
    $this->header_left_text = $text;
  }

  function set_header_center($text)
  {
    $this->header_center_text = $text;
  }

  function set_header_right($text)
  {
    $this->header_right_text = $text;
  }

  function set_footer_left($text)
  {
    $this->footer_left_text = $text;
  }

  function set_footer_right($text)
  {
    $this->footer_right_text = $text;
  }

  function start()
  {
    $this->pdf->save();
    $this->pdf->translate($this->origin->x, $this->origin->y);
  }

  function end()
  {
    $this->pdf->restore();
  }

  function __toString()
  {
    $str = " (CANVAS ORIGIN $origin)";
    
    return $str;
  }

  function moveTo($point)
  {
    $this->pdf->moveto($point->x, $point->y);
  }

  function lineTo($point)
  {
    $this->pdf->lineto($point->x, $point->y);
  }

  function stroke()
  {
    $this->pdf->stroke();
  }

  function __get($name)
  {
    return $this->$name;
  }

  function write_section($title)
  {
    $this->set_header_right($title);

    if ($this->page > 0)
      $this->newPage();
    else {
      $this->writeHeader();
      $this->writeFooter();
    }
    
    $this->setSectionFontAndSize(16);
//    $this->write_text_newline();
    $this->writeTextLine($title);
    $this->write_text_newline();
  }

  function writeText($point, $text)
  {
    $this->pdf->set_text_pos($point->x, $point->y);
    $this->pdf->show($text);
  }

  function write_text_xy($x, $y, $text)
  {
    $this->pdf->set_text_pos($x, $y);
    $this->pdf->show($text);
  }

  function write_text_ralign_xy($x, $y, $text)
  {
    $font_size = $this->fontSize;
    $width = $this->pdf->stringwidth($text, $this->font, $font_size);
    
    $this->pdf->set_text_pos($x - $width, $y);
    $this->pdf->show($text);
  }

  function write_text_center_xy($x, $y, $text)
  {
    $font_size = $this->fontSize;
    
    $width = $this->pdf->stringwidth($text, $this->font, $font_size);
    
    $this->pdf->set_text_pos($x - $width / 2, $y);
    $this->pdf->show($text);
  }

  function isNewLine($count = 1)
  {
    return ($this->text_y - $count * $this->text_y_inc < 40);
  }

  function writeTextLine($text)
  {
    if ($this->isNewLine()) {
      $this->newPage();
    }
    
    $this->pdf->set_text_pos($this->text_x, $this->text_y);
    $this->pdf->show($text);

    $this->text_y -= $this->text_y_inc;
  }

  function write_text_line_x($x, $text)
  {
    if ($this->isNewLine()) {
      $this->newPage();
    }
    
    $this->pdf->set_text_pos($this->text_x + $x, $this->text_y);
    $this->pdf->show($text);

    $this->text_y -= $this->text_y_inc;
  }

  function write_text_block($block)
  {
    $lines = preg_split("/\\n/", $block);

    foreach ($lines as $line) {
      $this->writeTextLine($line);
    }
  }

  function write_text_block_x($x, $block)
  {
    $lines = preg_split("/[\\n]/", $block);

    foreach ($lines as $line) {
      $this->write_text_line_x($x, $line);
    }
  }

  function write_text_x($x, $text)
  {
    $this->pdf->set_text_pos($this->text_x + $x, $this->text_y);
    $this->pdf->show($text);
  }

  function write_text_ralign_x($x, $text)
  {
    $font_size = $this->pdf->get_value("fontsize");
    
    $width = $this->pdf->stringwidth($text, $this->font, $font_size);
    
    $this->pdf->set_text_pos($this->text_x + $x - $width, $this->text_y);
    $this->pdf->show($text);
  }

  function write_hrule()
  {
    $this->pdf->moveto(20, $this->text_y + 5);
    $this->pdf->lineto(560, $this->text_y + 5);
    
    $this->stroke();
    
    $this->text_y -= 5;
  }

  function write_text_newline()
  {
    $this->text_y -= $this->text_y_inc;
  }

  function setColor(Color $color)
  {
    $color->doSetColor($this);
  }

  function setRGBColor($red, $green, $blue)
  {
    $this->pdf->setcolor("fillstroke", "rgb", $red, $green, $blue);
  }

  function set_line_width($width)
  {
    $this->pdf->setlinewidth($width);
  }

  function setDataFontAndSize($fontSize)
  {
    $this->setFont("Courier", $fontSize);
  }

  function setSectionFontAndSize($fontSize)
  {
    $this->setFont("Times-Bold", $fontSize);
  }

  function setFont($fontName, $fontSize)
  {
    $font = $this->pdf->load_font($fontName, "", "");

    $this->setFontByObject($font, $fontSize);
  }

  function setFontByObject($font, $fontSize)
  {
    $this->font = $font;
    $this->fontSize = $fontSize;
    
    $this->pdf->setfont($this->font, $fontSize);

    $this->ascender = $this->pdf->get_value("ascender") / 72;
    $this->descender = $this->pdf->get_value("descender") / 72;
    // $this->text_y_inc = $this->ascender - $this->descender;
    $this->text_y_inc = $this->fontSize - $this->descender;
  }

  function newPage()
  {
    $this->pdf->end_page();
    $this->pdf->begin_page(595, 842);
    
    $this->writeHeader();
    $this->writeFooter();
    
    $this->initPage();
  }

  function initPage()
  {
    $this->text_x = 20;
    $this->text_y = 800;

    $this->graph_x = 0;
    $this->graph_y = 0;
    $this->graph_index = 0;
  }
  
  function writeHeader()
  {
    $font = $this->font;
    $fontSize = $this->fontSize;
    
    $this->setFont("Times-Roman", 8);

    $top = $this->height - 12;
    
    if ($this->header_left_text) {
      $this->write_text_xy(5, $top, $this->header_left_text);
    }
    
    if ($this->header_center_text) {
      $this->write_text_center_xy($this->width / 2, $top,
                                  $this->header_center_text);
    }
    
    if ($this->header_right_text) {
      $this->write_text_ralign_xy($this->width - 10, $top,
                                  $this->header_right_text);
    }

    $this->setFontByObject($font, $fontSize);
  }
  
  function writeFooter()
  {
    $font = $this->font;
    $fontSize = $this->fontSize;
    
    $this->setFont("Times-Roman", 8);

    $bottom = 10;
    
    if ($this->footer_left_text) {
      $this->write_text_xy(5, $bottom, $this->footer_left_text);
    }
    
    $this->page +=1;

    $this->write_text_center_xy($this->width / 2, $bottom,
                                "Page $this->page");
    
    if ($this->footer_right_text) {
      $this->write_text_ralign_xy($this->width - 5, $bottom,
                                  $this->footer_right_text);
    }

    $this->setFontByObject($font, $fontSize);
  }

  function set_graph_rows($rows)
  {
    $this->graph_rows = $rows;
  }

  function set_graph_columns($cols)
  {
    $this->graph_cols = $cols;
  }

  function graph_next_xy()
  {
    if ($this->graph_index == 6) {
      $this->newPage();
    }

    $this->graph_width = (500) / $this->graph_cols;
    $this->graph_height = (660) / $this->graph_rows;

    $x_index = (int) ($this->graph_index % $this->graph_cols);
    $y_index = (int) ($this->graph_index / $this->graph_cols);

    $this->graph_x = 50 + $x_index * $this->graph_width;
    $this->graph_y = 820 - ($y_index + 1) * $this->graph_height;

    $this->graph_index++;
  }

  // graphs

  function draw_graph($name, $gds)
  {
    $this->graph_next_xy();
    
    $gd = admin_pdf_get_dominant_graph_data($gds);
    $graph = admin_pdf_create_graph($this,
                                    $this->graph_x, $this->graph_y,
                                    $this->graph_width - 60,
                                    $this->graph_height - 100,
                                    $name, $gd);
    pdf_graph_draw_lines($gds, $graph);
    $graph->drawLegends($gds);
    $graph->end();
  }
}

function admin_pdf_draw_log()
{
  global $log_mbean, $g_canvas, $g_pdf, $g_end, $g_start;
  debug("DRAW_LOG");
  
  $messages = $log_mbean->findMessages("warning",
                                       $g_start * 1000,
                                       $g_end * 1000);
                                       

  $g_canvas->write_section("Log[Warning]");
  
  $g_canvas->setDataFontAndSize(8);

  foreach ($messages as $message) {
    $ts = strftime("%Y-%m-%d %H:%M:%S", $message->timestamp / 1000);
    $g_canvas->write_text_x(20, $ts);
    $g_canvas->write_text_x(110, $message->level);
    
    $msg = wordwrap($message->message, 75, "\n", true);
    
    $g_canvas->write_text_block_x(150, $msg);
  }
  
  $g_canvas->set_header_right(null);
}

function admin_pdf_log_messages($canvas,
                                $title,
                                $regexp,
                                $start, $end,
                                $max=-1)
{
  global $log_mbean;
  
  $messages = $log_mbean->findMessages("warning",
                                       $start * 1000,
                                       $end * 1000);

  if (! $messages || is_empty($message))
    return;

  array_reverse($messages);

  $i = 0;
  
  foreach ($messages as $message) {
    if (! preg_match($regexp, $message->name))
      continue;
  
    if ($max < $i++ && $max >= 0)
      break;

    if ($i == 1) {
      $canvas->write_text_newline();
      $canvas->writeTextLine($title);
    }
      
    $ts = strftime("%Y-%m-%d %H:%M:%S", $message->timestamp / 1000);
    $canvas->write_text_x(20, $ts);

    $msg = wordwrap($message->message, 75, "\n", true);
    
    $canvas->write_text_block_x(110, $msg);
  }
}

function admin_pdf_heap_dump()
{
  $heap_dump = admin_pdf_snapshot("Resin|HeapDump");

  if (! $heap_dump)
    return;

  $heap =& $heap_dump["heap"];

  admin_pdf_selected_heap_dump($heap, "Heap Dump", 100);

  $class_loader_heap = heap_select_classes($heap_dump["heap"]);
  
  admin_pdf_selected_heap_dump($class_loader_heap,
                               "ClassLoader Heap Dump", 100);
}

function heap_select_classes(&$values)
{
  $selected = array();

  foreach ($values as $name => $value) {
    if (preg_match("/ClassLoader/", $name)) {
      $selected[$name] = $value;
    }
  }

  return $selected;
}

function admin_pdf_selected_heap_dump($heap, $title, $max)
{
  global $g_canvas;
  
  if (! $heap || ! sizeof($heap))
    return;

  uksort($heap, "heap_descendant_cmp");

  $g_canvas->write_section($title);

  $g_canvas->setDataFontAndSize(8);
  admin_pdf_heap_dump_header($g_canvas);

  $i = 0;

  foreach ($heap as $name => $value) {
    if ($max <= $i++)
      break;

    if ($g_canvas->isNewline()) {
      $g_canvas->newPage();

      admin_pdf_heap_dump_header($g_canvas);
    }

    $g_canvas->write_text_x(0, $name);
    $g_canvas->write_text_x(300, admin_pdf_size($value["descendant"]));
    $g_canvas->write_text_x(350, admin_pdf_size($value["size"]));
    $g_canvas->write_text_x(400, $value["count"]);
    $g_canvas->write_text_newline();
  }
  
  $g_canvas->set_header_right(null);
}

function admin_pdf_heap_dump_header($canvas)
{
  $canvas->write_text_x(0, "Class Name");
  $canvas->write_text_x(300, "self+desc");
  $canvas->write_text_x(350, "self");
  $canvas->write_text_x(400, "count");
  $canvas->write_text_newline();
  
  $canvas->write_hrule();
}

function admin_pdf_profile()
{
  $profile = admin_pdf_snapshot("Resin|Profile");

  if (! $profile) {
    return;
  }

  admin_pdf_profile_section("CPU Profile: Active",
                            $profile,
                            "admin_thread_active");
                            
  admin_pdf_profile_section("CPU Profile: Full", $profile);
}

function admin_pdf_profile_section($name, $profile, $filter)
{
  global $g_canvas;
  
  $g_canvas->write_section($name);

  $g_canvas->setDataFontAndSize(10);

  $g_canvas->writeTextLine("Time: " . $profile["total_time"] / 1000 . "s");
  $g_canvas->writeTextLine("GC-Time: " . $profile["gc_time"] / 1000 . "s");
  $g_canvas->writeTextLine("Ticks: " . $profile["ticks"]);
  $g_canvas->writeTextLine("Sample-Period: " . $profile["period"]);
  $g_canvas->writeTextLine("End: " . date("Y-m-d H:i",
                           $profile["end_time"] / 1000));
  $g_canvas->write_text_newline();

  $ticks = $profile["ticks"];
  
  if ($ticks <= 0)
    $ticks = 1;
    
  $period = $profile["period"];

  $profile_entries = $profile["profile"];

  if ($filter)
    $profile_entries = array_filter($profile_entries, $filter);

  usort($profile_entries, "profile_cmp_ticks");

  $max = 60;
  $max_stack = 6;
  $i = 0;

  $g_canvas->setDataFontAndSize(8);
  
  foreach ($profile_entries as $entry) {
    if ($max <= $i++)
      break;

    if ($g_canvas->isNewline($max_stack + 1)) {
      $g_canvas->newPage();

      
      // admin_pdf_heap_dump_header($g_canvas);
    }

    $stack = admin_pdf_stack($entry, $max_stack);

    $g_canvas->write_text_ralign_x(40, sprintf("%.2f%%", 
                                             100 * $entry["ticks"] / $ticks));
                                             
    $g_canvas->write_text_ralign_x(90, sprintf("%.2fs", 
                                             $entry["ticks"] * $period / 1000));
    $g_canvas->write_text_x(110, $entry["name"]);
    $g_canvas->write_text_x(440, $entry["state"]);

    $g_canvas->write_text_newline();

    if ($stack) {
      $g_canvas->setDataFontAndSize(7);
      $g_canvas->write_text_block_x(120, $stack);
      $g_canvas->setDataFontAndSize(8);
    }
    else {
      $g_canvas->write_text_newline();
    }
  }
  
  $g_canvas->set_header_right(null);
}

function admin_pdf_stack(&$profile_entry, $max)
{
  $stack =& $profile_entry["stack"];

  $string = "";

  if (! $stack)
    return $string;

  for ($i = 0; $i < $max && $stack[$i]; $i++) {
    $stack_entry = $stack[$i];

    $string .= $stack_entry["class"] . "." . $stack_entry["method"] . "()\n";
  }

  return $string;
}

function admin_thread_active($item)
{
  $state = $item["state"];

  if ($state == "WAITING")
    return false;

  $name = $item["name"];

  if ($name == "com.caucho.vfs.JniSocketImpl.nativeAccept()")
    return false;

  if ($name == "com.caucho.network.listen.JniSelectManager.selectNative()")
    return false;

  if ($name == "com.caucho.profile.ProProfile.nativeProfile()")
    return false;

  if ($name == "java.net.PlainSocketImpl.accept()")
    return false;

  if ($name == "java.net.PlainSocketImpl.socketAccept()")
    return false;

  if ($name == "unknown")
    return false;
  
  return true;
}

//
// Thread dump
//

function admin_pdf_thread_dump()
{
  global $g_canvas;

  $dump = admin_pdf_snapshot("Resin|ThreadDump");

  if (! $dump) {
    return;
  }
  
  $g_canvas->write_section("Thread Dump");

  $g_canvas->setDataFontAndSize(8);

  $create_time = $dump["create_time"];

  $g_canvas->writeTextLine("Created at: " . $create_time);

  $entries =& $dump["thread_dump"];

  admin_pdf_analyze_thread_dump($entries);

  usort($entries, "thread_dump_cmp");

  $max = 60;
  $max_stack = 32;
  $size = sizeof($entries);

  $i = 0;
  while ($i < $size) {
    if ($g_canvas->isNewline(6)) {
      $g_canvas->newPage();
      
      // admin_pdf_heap_dump_header($g_canvas);
    }

    $entry =& $entries[$i];

    $g_canvas->setDataFontAndSize(8);
    $i = admin_pdf_shared_entries($i, &$entries, $g_canvas);
    
    $stack = admin_pdf_thread_stack($entry, $max_stack);

    $g_canvas->setDataFontAndSize(7);
    $g_canvas->write_text_block_x(50, $stack);
  }
  
  $g_canvas->set_header_right(null);
}

function admin_pdf_analyze_thread_dump(&$entries)
{
  foreach ($entries as &$entry) {
    $lock = $entry["lock"];
    
    if (! $lock)
      continue;

    $owner_id = $lock["owner_id"];

    stack_fill_owner($entries, $owner_id);
    $lock_owner["is_lock_owner"] = true;
  }
}

function stack_fill_owner(&$entries, $id)
{
  foreach ($entries as &$entry) {
    if ($entry["id"] == $id) {
      $entry["is_lock_owner"] = true;
      return;
    }
  }

  return null;
}

function admin_pdf_shared_entries($i, &$entries, $canvas)
{
  $stack = thread_dump_stack($entries[$i]);

  for (;
       ($entry =& $entries[$i]) && $stack == thread_dump_stack($entry);
       $i++) {
    if ($canvas->isNewline(2)) {
      $canvas->newPage();
    }
    
    $canvas->write_text_x(10, sprintf("%.40s", $entry["name"]));
    
    $state = $entry["state"];
    if ($state == "RUNNABLE" && $state["native"])
      $state .= " (JNI)";
      
    $canvas->write_text_x(350, $state);
    $canvas->write_text_x(450, "[" . $entry["id"] . "]");
    $canvas->write_text_newline();

    $lock = $entry["lock"];
    if ($lock) {
      $trace = "waiting on " . $lock["name"];

      if ($lock["owner_name"]) {
        $trace .= " owned by [" . $lock["owner_id"] . "] " . $lock["owner_name"];
      }

      $canvas->write_text_x(20, $trace);
      $canvas->write_text_newline();
    }
  }

  return $i;
}  

function thread_dump_cmp($a, $b)
{
  $cmp = strcmp(thread_dump_stack($a), thread_dump_stack($b));

  if ($cmp)
    return $cmp;
  else
    return strcmp($a["name"], $b["name"]);
}


function thread_dump_stack(&$info)
{
  $trace = $info["cmp"];

  if ($trace)
    return $trace;

  if ($info["is_lock_owner"]) {
    $trace .= "A" . $info["id"] . " ";
  }
  else if ($info["lock"]["owner_id"] > 0) {
    $trace .= "A" . $info["lock"]["owner_id"] . "Z ";
  }
  else if ($info["state"] == "RUNNABLE") {
    if ($info["native"])
      $trace .= "BB ";
    else
      $trace .= "BA ";
  }
  else {
    $trace .= "C " . $info["state"];
  }

  $stack =& $info["stack"];
  $size = sizeof($stack);

  for ($i = $size - 1; $i >= 0; $i--) {
    $elt =& $stack[$i];

    $trace = $trace . " {$elt['class']}.{$elt['method']} ({$elt['file']}:{$elt['line']}) ";
  }

  $info["cmp"] = $trace;

  return $trace;
}


function admin_pdf_thread_stack(&$thread_entry, $max)
{
  $stack =& $thread_entry["stack"];
  $monitors = $thread_entry["monitors"];

  $string = "";

  for ($i = 0; $i < $max && $stack[$i]; $i++) {
    $stack_entry = $stack[$i];

    $string .= $stack_entry["class"] . "." . $stack_entry["method"];

    if ($stack_entry["file"]) {
      $string .= " (" . $stack_entry["file"] . ":" . $stack_entry["line"] . ")\n";
    }

    $string .= stack_find_monitor($monitors, $i);
  }

  return $string;
}

function stack_find_monitor($monitors, $i)
{
  foreach ($monitors as $monitor) {
    if ($monitor["depth"] == $i + 1) {
      return "-- locked " . $monitor["class"] . "@" . $monitor["hash"] . "\n";
    }
  }
  
  return "";
}

function profile_cmp_ticks($a, $b)
{
  return $b["ticks"] - $a["ticks"];
}

//
// JMX dump
//

function admin_pdf_jmx_dump()
{
  global $g_canvas;

  $dump = admin_pdf_snapshot("Resin|JmxDump");

  if (! $dump) {
    return;
  }
  
  $g_canvas->write_section("JMX Dump");

  $entries =& $dump["jmx"];

  ksort($entries);

  foreach ($entries as $name => &$values) {
    if ($g_canvas->isNewline(6)) {
      $g_canvas->newPage();
      
      // admin_pdf_heap_dump_header($g_canvas);
    }

    $g_canvas->write_text_newline();
    $g_canvas->setDataFontAndSize(8);
    $g_canvas->writeTextLine($name);

    $g_canvas->setDataFontAndSize(6);
    admin_pdf_jmx_attributes($g_canvas, $values);
  }
  
  $g_canvas->set_header_right(null);
}

function admin_pdf_jmx_attributes($canvas, &$values)
{
  ksort($values);

  foreach ($values as $key => $value) {
    if ($g_canvas->isNewline(3)) {
      $g_canvas->newPage();
    }
    
    $canvas->write_text_x(20, $key);
    $canvas->write_text_block_x(150, admin_pdf_attribute_value($value));
  }
}

function admin_pdf_attribute_value($value, $depth = 0)
{
  if ($value === true)
    return "true";
  else if ($value === false)
    return "false";
  else if ($value === null)
    return "null";
  else if (is_array($value)) {
    $i = 0;

    $v = "{";

    foreach ($value as $key => $sub_value) {
      if ($key == "java_class")
        continue;
        
      if ($i++ != 0 || $depth > 0 && sizeof($value) > 1) {
        $v .= "\n";
      }
      
      for ($j = 0; $j < $depth + 2; $j++)
        $v .= " ";

      if (is_integer($key)) {
        $v .= admin_pdf_attribute_value($sub_value, $depth + 2);
      }
      else
        $v .= $key . " => " . admin_pdf_attribute_value($sub_value, $depth + 2);
    }
    $v .= "  }";
    
    return $v
  }
  else {
    return wordwrap($value, 75, "\n", true);
  }
}

function admin_pdf_snapshot($name)
{
  global $si, $log_mbean, $g_start, $g_end;
  
  $times = $log_mbean->findMessageTimesByType("$si|$name",
                                              "info",
                                              $g_start * 1000,
                                              $g_end * 1000);
  if (! $times || sizeof($times) == 0)
    return;

  $time = $times[sizeof($times) - 1];

  if (! $time)
    return;
  
  $msgs = $log_mbean->findMessagesByType("$si|$name",
                                         "info", $time, $time);

  $msg = $msgs[0];

  if (! $msg)
    return;

  return json_decode($msg->getMessage(), true);
}

function admin_pdf_size($size)
{
  if (1e9 < $size) {
    return sprintf("%.2fG", $size / 1e9);
  }
  else if (1e6 < $size) {
    return sprintf("%.2fM", $size / 1e6);
  }
  else if (1e3 < $size) {
    return sprintf("%.2fK", $size / 1e3);
  }
  else
    return $size;
}

function heap_descendant_cmp($a, $b)
{
  return $a->descendant - $b->descendant;
}

function admin_pdf_new_page()
{
  global $g_canvas;

  $g_canvas->newPage();
}

?>
