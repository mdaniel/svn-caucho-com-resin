<?php

require_once "WEB-INF/php/inc.php";


import java.lang.System;

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


function drawSummary() {
	global $x, $y, $yinc, $server, $runtime, $os, $log_mbean, $canvas, $resin;

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

}

function drawLines($gds, $graph) {
  global $canvas;
  foreach($gds as $gd) {
    if ($gd->validate()) {
      $canvas->setColor($gd->color);
      if (sizeof($gd->dataLine)!=0) {
      	$graph->drawLine($gd->dataLine);
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
 


  function statFromName($fullName, $server="01") {
    $this->fullName = $fullName;
    $arr = explode("|", $this->fullName);
    $this->elements = $arr;

    
    $this->category = $arr[0];
    $this->subcategory = $arr[1];  

    $arr = array_slice($arr, 2); 

    $this->name = implode(" ", $arr);

    $this->server = $server;
  }

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
    //debug("name " . $this->name);
  }

  function __get($name) {
    return $this->$name;
  }


  function __toString() {
    return " name=" . $this->name . "\t\t\t\tserver=" . $this->server .  " category=" . $this->category . " subcategory=" . $this->subcategory ;
  }

   function eq($that) {
	return $this->name == $that->name && $this->category == $that->category && 
	$this->subcategory == $that->subcategory;
   }
}


function getStatDataForGraphByMeterNames($meterNames) {


  global $blue, $red, $orange, $purple, $green, $cyan, $brown, $black;
  $cindex = 0;
  $colors = array($blue, $red, $orange, $purple, $green, $cyan, $brown, $black, $blue, $red, $orange, $purple, $green, $cyan, $brown, $black);

  $gds = array();   
  foreach ($meterNames as $name) {
	$statItem = new Stat();
	$statItem->statFromName($name);
	$gd = getStatDataForGraphByStat($statItem);
	array_push($gds, $gd);
  	$gd->color=$colors[$cindex];
	$cindex++;
  }

  return $gds;
}


function getStatDataForGraphByStat($theStat, $color=$blue) {

  $data=findStatByStat($theStat);
  debug("DATA " . sizeof($data));
  $dataLine = array();
  $max = -100;
  foreach($data as $d) {
    
    $value = $d->value;
    $hour = $d->time;
    array_push($dataLine, new Point($hour, $value));
    if ($value > $max) $max = $value;
  }

  $gd = new GraphData();
  $gd->name = $theStat->name;
  $gd->dataLine = $dataLine;
  $gd->max = $max + ($max * 0.05) ;
  $gd->yincrement = calcYincrement($max);
  $gd->color=$color;

  return $gd;
}

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
	$map[$statItem->name]= 
		$stat->statisticsData($statItem->fullName, $start * 1000, $end * 1000,
                                    STEP * 1000);
      }
    }
  }
  return $map;
}


function findStatByStat($theStat) {
  global $start;
  global $end;
  global $stat;
  global $statList;
  global $si;

  foreach ($statList as $statItem) {
    if ($statItem->server != $si) continue;
    if ($statItem->eq($theStat)) {
 	return $stat->statisticsData($statItem->fullName, $start * 1000, $end * 1000,
                                    STEP * 1000);
      }
    
  }
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
      //debug(" NAME " . $statItem->name); 
    }
    if ($name == $statItem->name && $category == $statItem->category) {
	$arr = $stat->statisticsData($statItem->fullName, $start * 1000, $end * 1000,
                                    STEP * 1000);
    }
  }
  return $arr;
}


function getMeterGraphPage($pdfName) {
	global $stat;
	$mpages = $stat->getMeterGraphPages();
	foreach($mpages as $mg)	{
		if ($mg->name == $pdfName){
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

  function __construct() {
    $args = func_get_args();
    $this->start = (float) $args[0];
    $this->stop = (float) $args[1];
  }

  function __set($name, $value) {
      $this->$name = (double) $value;
  }

  function __get($name) {
    return $this->$name;
  }

  function __toString() {
    $str = " (RANGE WIDTH:$this->start; HEIGHT:$this->stop;)";
    return $str;
  }

  function size() {
    return $this->stop - $this->start;
  }

}


class Size {
  private $width;
  private $height;

  function __construct() {
    $args = func_get_args();
    $this->width = (float) $args[0];
    $this->height = (float) $args[1];
  }

  function __set($name, $value) {
      $this->$name = (double) $value;
  }

  function __get($name) {
    return $this->$name;
  }

  function __toString() {
    $str = " (SIZE WIDTH:$this->width; HEIGHT:$this->height;)";
    return $str;
  }

}

class Point {
  private $x;
  private $y;

  function __construct() {
    $args = func_get_args();
    $this->x = (float) $args[0];
    $this->y = (float) $args[1];
  }


  function __set($name, $value) {
      $this->$name = (double) $value;
  }


  function __get($name) {
    return $this->$name;
  }


  function __toString() {
    $str = "POINT( X:$this->x; Y:$this->y;)";
    return $str;
  }

}

class Graph {
  private $pixelSize;
  private $xRange;
  private $yRange;
  private $canvas;
  private $title;
  private $pixelPerUnit;

  function __construct(string $title, Point $origin, Size $pixelSize, Range $xRange, Range $yRange, boolean $trace=false) {
    $this->title = $title;
    $this->canvas = new Canvas($origin);
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
      $this->trace("drawTitle NOT VALID" );
      $this->canvas->writeText(new Point($x, $y), $title . " not valid");
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

    global $canvas;
    global $black;

    $x = $point->x;
    $y = $point->y;

    $canvas->setColor($color);
    $this->canvas->moveTo(new Point($x, $y+2.5));
    $this->canvas->lineTo(new Point($x+5, $y+5));
    $this->canvas->lineTo(new Point($x+10, $y+2.5));
    $this->canvas->lineTo(new Point($x+15, $y+2.5));
    $this->canvas->stroke();

    $canvas->setColor($black);
    $this->canvas->setFont("Helvetica-Bold", 6);
    $this->canvas->setColor($black);
    $this->canvas->writeText(new Point($x+20, $y), $name);


  }
  
  function drawLine($dataLine) {
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



  function drawGrid() {
    $this->trace("drawGrid ");


    $width =   (double) $this->pixelSize->width;
    $height =   (double) $this->pixelSize->height;
    $this->canvas->moveTo(new Point(0.0, 0.0));
    $this->canvas->lineTo(new Point($width, 0.0));
    $this->canvas->lineTo(new Point($width, $height));
    $this->canvas->lineTo(new Point(0.0, $height));
    $this->canvas->lineTo(new Point(0.0, 0.0));
    $this->canvas->stroke();
  }

  function drawGridLines($xstep, $ystep) {

  
   if (!$ystep) {
      $this->valid = false;
      debug("No ystep was passed " .  $this->title);
   }

    if (!$this->valid) {
       return;
    }

    $this->trace("drawGridLines ");

    $width =   intval($this->pixelSize->width);
    $height =   intval($this->pixelSize->height);

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
    $height =   (double) $this->pixelSize->height;

    $step_width = ($step) * $this->pixelPerUnit->height;

    for ($index = 0; $height >= ($index*$step_width); $index++) {
      $currentYPixel = $index*$step_width;
      $currentYValue =	($index * $step) + $this->yRange->start;
      if ($func) {
	$currentLabel = $func($currentYValue);
      } else {
      	if ($currentYValue >      1000000000) {
	   $currentLabel = "" . $currentYValue / 1000000000 . " G";
	}elseif ($currentYValue > 1000000) {
	   $currentLabel = "" . $currentYValue / 1000000 . " M";
	}elseif ($currentYValue > 1000) {
	   $currentLabel = "" . $currentYValue / 1000 . " K";
	} else {
	  $currentLabel = $currentYValue; 
	}
      }
      $this->canvas->writeText(new Point($xpos, $currentYPixel-3), $currentLabel);
    }    
  }



}


function pdf_format_memory($memory)
{
  return sprintf("%.2f M", $memory / (1024 * 1024))
}




function createGraph(String $title,
                     GraphData $gd,
                     Point $origin,
                     boolean $displayYLabels=true,
                     Size $gsize=GRAPH_SIZE,
                     boolean $trace=false)
{
  global $start;
  global $end;
  global $canvas;
  global $lightGrey;
  global $grey;
  global $darkGrey;
  global $black;
  global $majorTicks, $minorTicks;

  $graph = new Graph($title, $origin, $gsize, new Range($start * 1000, $end * 1000), new Range(0,$gd->max), $trace);
  $graph->start();

  $valid = $gd->validate();

  if ($valid) {
    $canvas->setColor($black);
    $canvas->setFont("Helvetica-Bold", 12);
    $graph->drawTitle($title);

    $canvas->setColor($lightGrey);
    $graph->drawGridLines($minorTicks, $gd->yincrement/2);

    $canvas->setColor($grey);
    $graph->drawGridLines($majorTicks, $gd->yincrement);

    $canvas->setColor($black);
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


function getDominantGraphData($gds) {
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

function initPDF() {
  global $pdf;
  $pdf = new PDF();

}

function startDoc() {
  global $pdf;
  $pdf->begin_document();
  $pdf->begin_page(595, 842);
}



class Color {

  function doSetColor($canvas) {
  }
}

class RGBColor {
  private $red;
  private $green;
  private $blue;

  function __construct() {
    $args = func_get_args();
    $this->red =  $args[0];
    $this->green =  $args[1];
    $this->blue =  $args[2];
  }


  function doSetColor($canvas) {
    $canvas->setRGBColor($this->red, $this->green, $this->blue);
  } 

}

$black = new RGBColor(0.0, 0.0, 0.0);
$red = new RGBColor(1.0, 0.0, 0.0);
$green = new RGBColor(0.0, 1.0, 0.0);
$blue = new RGBColor(0.0, 0.0, 1.0);
$darkGrey = new RGBColor(0.2, 0.2, 0.2);
$lightGrey = new RGBColor(0.9, 0.9, 0.9);
$grey = new RGBColor(0.45, 0.45, 0.45);
$purple = new RGBColor(0.45, 0.2, 0.45);
$orange = new RGBColor(1.0, 0.66, 0.0);
$cyan = new RGBColor(0.0, 0.66, 1.0);
$brown = new RGBColor(0.66, 0.20, 0.20);




class Canvas {
  private $origin;
  
  function __construct() {
    $args = func_get_args();
    $this->origin =  $args[0];
    $this->lastTextPos = new Point(0,0); //to fix problem with Resin PDF Lib clone
  }

  function start() {
    global $pdf;
    $pdf->save();
    $pdf->translate($this->origin->x, $this->origin->y);

  }

  function end() {
    global $pdf;
    $pdf->restore();
  }

  function __toString() {
    $str = " (CANVAS ORIGIN $origin)";
    return $str;
  }


  function moveTo($point) {
    global $pdf;
    $pdf->moveto($point->x, $point->y);
  }

  function lineTo($point) {
    global $pdf;
    $pdf->lineto($point->x, $point->y);
  }

  function stroke() {
    global $pdf;
    $pdf->stroke();
  }


  function __get($name) {
    return $this->$name;
  }

  function writeText($point, $text) {
    global $pdf;
    $pdf->set_text_pos($point->x, $point->y);
    $pdf->show($text);
  }

  function setColor(Color $color) {
    $color->doSetColor($this);
  }

  function setRGBColor($red, $green, $blue) {
    global $pdf;
    $pdf->setcolor("fillstroke", "rgb", $red, $green, $blue);
  }

  function setFont($fontName, $fontSize) {
    global $pdf;
    $font = $pdf->load_font($fontName, "", "");
    $pdf->setfont($font, $fontSize);
  }

}

$canvas = new Canvas(new Point(0,0));


function drawLog() {
	global $log_mbean, $canvas, $yinc, $pdf, $end, $start;
	debug("DRAW_LOG");
	$messages = $log_mbean->findMessages("warning",
                                       ($start) * 1000,
                                       ($end) * 1000);

	$y = 800;

	$canvas->setFont("Helvetica-Bold", 8);

	$index=1;
	foreach ($messages as $message) {
  		$ts = strftime("%Y-%m-%d %H:%M:%S", $message->timestamp / 1000);
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

}

function newPage() {
	global $pdf;
	$pdf->end_page();
	$pdf->begin_page(595, 842);
	writeFooter();

}
?>
