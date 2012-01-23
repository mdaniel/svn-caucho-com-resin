<?php

require_once "WEB-INF/php/inc.php";
require_once 'PdfCanvas.php';
require_once 'PdfCanvasGraph.php';

//error_reporting(E_ALL);

import java.lang.System;

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

$x = 20;
$y = 750;
$yinc = 12;

if (! mbean_init()) {
  debug("Failed to load admin, die");
  return;
}

$mbean_server = $g_mbean_server;
$resin = $g_resin;
$server = $g_server;

if ($g_mbean_server)
  $stat = $g_mbean_server->lookup("resin:type=StatService");

if (! $stat) {
  debug("Postmortem analysis:: requires Resin Professional and a <resin:StatService/> and <resin:LogService/> defined in the resin.xml.");
  return;
}

$mbean_server = $g_mbean_server;
$resin = $g_resin;
$server = $g_server;
$runtime = $g_mbean_server->lookup("java.lang:type=Runtime");
$os = $g_mbean_server->lookup("java.lang:type=OperatingSystem");
$log_mbean = $mbean_server->lookup("resin:type=LogService");

function debug($msg) 
{
  #System::out->println($msg);
}

function  my_error_handler($error_type, $error_msg, $errfile, $errline) {
  if(!startsWith($error_msg,"Can't access private field")) {
    //debug("ERROR HANDLER: type $error_type, msg $error_msg, file $errfile, lineno $errline");
  }
}

set_error_handler('my_error_handler'); 

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

function initPDF()
{
  global $g_canvas;
  
  $g_canvas = new PdfCanvas();
  $g_canvas->setMargins(40, 30, 40, 30);
  $g_canvas->graph_padding_x = 25;
}

function admin_pdf_summary()
{
  global $x, $y, $yinc, $server, $runtime, $os, $log_mbean, $g_canvas, $resin;
  global $pageName, $si;

  $summary = admin_pdf_summary_fill();

  if (! $summary)
    return;
    
  $g_canvas->writeSubsection("Environment");
  
  $g_canvas->writeTextLine("{$summary['ipAddress']} running as {$summary['userName']}");
  $g_canvas->writeTextLine($summary["jvm"]);
  $g_canvas->writeTextLine($summary["machine"]);
  
  $g_canvas->writeSubsection("Available Resources");
  
  $col1 = 85;  
  $col2 = 300;
  
  $g_canvas->writeTextColumn($col1, 'r', "JVM Heap:");
  $g_canvas->writeTextColumn($col2, 'l', "{$summary['freeHeap']} of {$summary['totalHeap']}");
  $g_canvas->newLine();
  
  $g_canvas->writeTextColumn($col1, 'r', "Physical Memory:");
  $g_canvas->writeTextColumn($col2, 'l', "{$summary['freePhysical']} of {$summary['totalPhysical']}");
  $g_canvas->newLine();
  
  $g_canvas->writeTextColumn($col1, 'r', "Swap Space:");
  $g_canvas->writeTextColumn($col2, 'l', "{$summary['freeSwap']} of {$summary['totalSwap']}");
  $g_canvas->newLine();
  
  $g_canvas->writeTextColumn($col1, 'r', "File Descriptors:");
  $g_canvas->writeTextColumn($col2, 'l', "{$summary['freeFd']} of {$summary['totalFd']}");
  $g_canvas->newLine();

  $g_canvas->writeSubsection("Resin Instance");
  
  $g_canvas->writeTextLine("$si - {$summary['serverID']}, {$summary['cluster']} Cluster");
  
  $col1 = 50;
  $col2 = 300;
  
  $g_canvas->writeTextColumn($col1, 'r', "Home:");
  $g_canvas->writeTextColumn($col2, 'l', "{$summary['resinHome']}");
  $g_canvas->newLine();
  
  $g_canvas->writeTextColumn($col1, 'r', "Root:");
  $g_canvas->writeTextColumn($col2, 'l', "{$summary['resinRoot']}");
  $g_canvas->newLine();
  
  $g_canvas->writeTextColumn($col1, 'r', "Conf:");
  $g_canvas->writeTextColumn($col2, 'l', "{$summary['resinConf']}");
  $g_canvas->newLine();
  
  $g_canvas->writeTextColumn($col1, 'r', "Logs:");
  $g_canvas->writeTextColumn($col2, 'l', "{$summary['resinLog']}");
  $g_canvas->newLine();
  
  $g_canvas->writeTextColumn($col1, 'r', "Data:");
  $g_canvas->writeTextColumn($col2, 'l', "{$summary['resinData']}");
  $g_canvas->newLine();
  
  $g_canvas->newLine();
  
  $g_canvas->writeTextLine($summary["resinVersion"]);
  $g_canvas->writeTextLine("Up {$summary['ups']}");
  $g_canvas->writeTextLine($summary['watchdogStartMessage'] ?: "Normal startup");
  $g_canvas->writeTextLine($summary["server_state"]);
  
  $g_canvas->writeSubsection("Licenses");
  
  $license_array = $summary["licenses"];
  foreach($license_array as $license_data) {
    $g_canvas->writeTextLine($license_data[0]);
    $g_canvas->writeTextLineIndent(20, $license_data[1]);
    $g_canvas->writeTextLineIndent(20, $license_data[2]);
  }
  
  admin_pdf_ports($summary);
}

function admin_pdf_ports($summary)
{
  global $g_canvas;
  
  $g_canvas->writeSubsection("TCP Ports");
  
  $g_canvas->setFont("Courier-Bold", "8");
  
  $col1 = 140;
  $col = 45;
  
  $g_canvas->setFont("Courier", "8");
  
  $g_canvas->writeTextColumn($col1, 'c', "");
  
  $g_canvas->writeTextColumn($col*3, 'c', "Threads");
  $g_canvas->writeTextColumn($col*4, 'c', "Keepalive");
  $g_canvas->newLine();
  
  $g_canvas->writeTextColumnHeader($col1, 'r', "Listener");
  $g_canvas->writeTextColumnHeader($col, 'l', "Status");
  $g_canvas->writeTextColumnHeader($col, 'c', "Active");
  $g_canvas->writeTextColumnHeader($col, 'c', "Idle");
  $g_canvas->writeTextColumnHeader($col, 'c', "Total");
  $g_canvas->writeTextColumnHeader($col, 'c', "Total");
  $g_canvas->writeTextColumnHeader($col, 'c', "Thread");
  $g_canvas->writeTextColumnHeader($col, 'c', "Non-Block");
  $g_canvas->writeTextColumnHeader($col, 'c', "Comet");
  $g_canvas->newLine();
  $g_canvas->newLine();
  
  $ports = $summary["ports"];
  foreach($ports as $port) {
    $g_canvas->writeTextColumn($col1, 'r', $port[0]);
    $g_canvas->writeTextColumn($col, 'l', $port[1]);
    $g_canvas->writeTextColumn($col, 'c', $port[2]);
    $g_canvas->writeTextColumn($col, 'c', $port[3]);
    $g_canvas->writeTextColumn($col, 'c', $port[4]);
    $g_canvas->writeTextColumn($col, 'c', $port[5]);
    $g_canvas->writeTextColumn($col, 'c', $port[6]);
    $g_canvas->writeTextColumn($col, 'c', $port[7]);
    $g_canvas->writeTextColumn($col, 'c', $port[8]);
    $g_canvas->writeTextColumn($col, 'c', $port[9]);
    $g_canvas->newLine();
  }
}

function admin_pdf_summary_fill()
{
  $dump = admin_pdf_snapshot("Resin|JmxDump");
  $jmx =& $dump["jmx"];

  if (! $jmx) {
    $g_canvas->setTextFont();
    $g_canvas->writeTextLineIndent(20, "No Data");
    return;
  }
  
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
  $summary["jvm"] = "{$runtime['VmName']} {$runtime['VmVersion']}, {$runtime['VmVendor']}";
  $summary["libraryPath"] = $runtime["LibraryPath"];
  
  $summary["machine"] = "{$os['AvailableProcessors']} cpu, {$os['Name']} {$os['Arch']} {$os['Version']}";
  $summary["watchdogStartMessage"] = $resin["WatchdogStartMessage"];
  $summary["resinHome"] = $resin["ResinHome"];
  $summary["resinRoot"] = $resin["RootDirectory"];
  $summary["resinConf"] = $resin["ConfigFile"];
  $summary["resinLog"] = $resin["LogDirectory"];
  $summary["resinData"] = $resin["DataDirectory"];
  
  $cluster_mbean_name = $server["Cluster"];
  $cluster = $jmx[$cluster_mbean_name];
  
  $summary["cluster"] = $cluster["Name"];
  
  $q_date = java("com.caucho.util.QDate");
  $start_time = $q_date->parseDate($server["StartTime"]) / 1000;

  $now = $q_date->parseDate($server["CurrentTime"]) / 1000;

  $uptime = $now - $start_time;

  $summary["ups"] = sprintf("%dd %dh %dm %ds",
                 $uptime / (24 * 3600),
                 $uptime / 3600 % 24,
                 $uptime / 60 % 60,
                 $uptime % 60)
                 . " since " . date("Y-m-d H:i", $start_time);

  $summary["server_state"] = $server["State"];

  $summary["totalHeap"] = pdf_format_memory($server["RuntimeMemory"]);
  $summary["freeHeap"] = pdf_format_memory($server["RuntimeMemoryFree"]);
  
  $summary["totalSwap"] = pdf_format_memory($os["TotalSwapSpaceSize"]);
  $summary["freeSwap"] = pdf_format_memory($os["FreeSwapSpaceSize"]);
  
  $summary["totalPhysical"] = pdf_format_memory($os["TotalPhysicalMemorySize"]);
  $summary["freePhysical"] = pdf_format_memory($os["FreePhysicalMemorySize"]);
  
  $summary["totalFd"] = $os["MaxFileDescriptorCount"];
  $summary["openFd"] = $os["OpenFileDescriptorCount"];
  $summary["freeFd"] = $os["MaxFileDescriptorCount"] - $os["OpenFileDescriptorCount"];
  
  $licenses = array();
  
  $license_names = $licenseStore["ValidLicenses"];
  foreach($license_names as $license_name) {
    $license = $jmx[$license_name];
    $license_data = array($license['Description'], $license['ExpireMessage'], $license['Path']);
    array_push($licenses, $license_data);
  }
  
  $summary["licenses"] = $licenses;
  
  $ports = array();
  
  $port_names = $server["Ports"];
  foreach($port_names as $port_name) {
    $port = $jmx[$port_name];
    
    $port_data = array();
    
    $address = ($port['Address'] ?: '*') . ":" . $port['Port'];
    
    if ($port['ProtocolName'] == 'http' && $port['SSL']) 
      $protocol = 'https';
    else
      $protocol = $port['ProtocolName'];
      
    array_push($port_data, "$protocol://$address");
    array_push($port_data, $port["State"]);
    
    array_push($port_data, $port["ThreadActiveCount"]);
    array_push($port_data, $port["ThreadIdleCount"]);
    array_push($port_data, $port["ThreadCount"]);
    
    array_push($port_data, $port["KeepaliveCount"]);
    array_push($port_data, $port["KeepaliveThreadCount"]);
    array_push($port_data, $port["KeepaliveSelectCount"]);
    array_push($port_data, $port["CometIdleCount"]);
    
    array_push($ports, $port_data);
  }
  
  $summary["ports"] = $ports;
  
  return $summary;
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

function admin_pdf_snapshot($name)
{
  global $si, $log_mbean, $g_start, $g_end;
  
  $times = $log_mbean->findMessageTimesByType("$si|$name",
                                              "info",
                                              $g_start * 1000,
                                              $g_end * 1000);
  if (! $times || sizeof($times) == 0)
    return;
    
  //debug("admin_pdf_snapshot found " . count($times) . " logs for " . $name);

  $time = $times[sizeof($times) - 1];

  if (! $time)
    return;
  
  //debug("admin_pdf_snapshot using time $time");
    
  $msgs = $log_mbean->findMessagesByType("$si|$name",
                                         "info", $time, $time);

  $msg = $msgs[0];
  if (! $msg)
    return;
    
  return json_decode($msg->getMessage(), true);
}

function pdf_format_memory($memory)
{
  return sprintf("%.2fMB", $memory / (1024 * 1024))
}

function admin_pdf_health()
{
  global $si, $log_mbean, $g_start, $g_end, $g_canvas;
  
  $g_canvas->writeSection("Health");
  
  $w1 = 65;
  $w2 = 165;
  $w3 = 290;

  $g_canvas->setFont("Courier-Bold", "8");
  
  $g_canvas->writeTextColumnHeader($w1, 'c', "Status");
  $g_canvas->writeTextColumnHeader($w2, 'l', "Check");
  $g_canvas->writeTextColumnHeader($w3, 'l', "Message");
  $g_canvas->newLine();
  $g_canvas->newLine();
  
  $g_canvas->setFont("Courier", "8");
  
  $health_dump = admin_pdf_snapshot("Resin|HealthDump");
  
  if (! $health_dump) {
    $g_canvas->setTextFont();
    $g_canvas->writeTextLineIndent(20, "No Data");
  } else {
    $health =& $health_dump["health"];
  
    foreach ($health as $check) {
      $g_canvas->writeTextColumn($w1, 'c', $check["status"]);
      $g_canvas->writeTextColumn($w2, 'l', $check["name"]);
      $g_canvas->writeTextColumn($w3, 'l', $check["message"]);
      $g_canvas->newLine();
    }
  }
  
  admin_pdf_log_messages("Recent Warnings",
                         "/^com.caucho.health.analysis/",
                         false,
                         $g_start, $g_end,
                         5);
  
  
  admin_pdf_log_messages("Recent Anomolies",
                         "/^com.caucho.health.analysis/",
                         true,
                         $g_start, $g_end,
                         5);
                         
  
}

function admin_pdf_log_messages($title,
                                $regexp,
                                $match,
                                $start, 
                                $end,
                                $max=-1)
{
  global $log_mbean, $g_canvas;
  
  if ($title)
    $g_canvas->writeSubsection($title);
  
  $messages = $log_mbean->findMessages("warning",
                                       $start * 1000,
                                       $end * 1000);

  if (! $messages || is_empty($message)) {
    $g_canvas->setTextFont();
    $g_canvas->writeTextLineIndent(20, "No Data");
    return;
  }
  
  $messages = array_reverse($messages);
  
  $i = 0;
  foreach ($messages as $message) {
    if ($regexp && preg_match($regexp, $message->name) != $match) {
      continue;
    }
    
    if ($max < $i++ && $max >= 0)
      break;
      
    $g_canvas->setFont("Courier-Bold", 8);
    $ts = strftime("%Y-%m-%d %H:%M:%S", $message->timestamp / 1000);
    $g_canvas->writeText($ts);
    
    $g_canvas->setFont("Courier", 8);
    $g_canvas->writeTextWrapIndent(100, $message->message);
    
    $g_canvas->newLine();
  }
}

function draw_cluster_graphs($mPage)
{
  global $g_server, $g_canvas;
  
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

  $g_canvas->writeSection("Cluster Status Graphs");
  
  $g_canvas->allocateGraphSpace(3,2);

  foreach ($cluster->getServers() as $server) {
    $items = array();
    
    pdf_cluster_item($items, $stat, $server, "Uptime|Start Count", 0);
    pdf_cluster_item($items, $stat, $server, "Log|Critical", 1);
    pdf_cluster_item($items, $stat, $server, "Log|Warning", 2);

    $graph_name = sprintf("Server %02d - %s",
                          $server->getClusterIndex(),
                          $server->getName());

    draw_graph($graph_name, $items, $g_canvas);
  }
}

function pdf_cluster_item(&$items, $stat_mbean, $server, $name, $index)
{
  global $g_pdf_colors;
  
  $full_name = sprintf("%02d|Resin|%s", $server->getClusterIndex(), $name);

  // $items[$name] = get_stat_item($stat_mbean, $full_name);
  $data = get_stat_item($stat_mbean, $full_name);
  
  $gd = create_graph_data($name, $data, $g_pdf_colors[$index]);

  $items[] = $gd;
}

function get_stat_item($stat_mbean, $full_name)
{
  global $g_start, $g_end;
  
  $step = ($g_end - $g_start) / 500;
  
  if ($step < 120)
    $step = 1;
  
  $data = $stat_mbean->statisticsData($full_name,
                                      $g_start * 1000, $g_end * 1000,
                                      $step * 1000);
                                      
  //debug("get_stat_item:name=$full_name,data=" . count($data));

  return $data;                                      
}

function create_graph_data($name, $data, $color)
{
  $size = count($data);
  
  //debug("create_graph_data:name=$name,size=$size,color=$color");
  $dataLine = array();
  $max = 0;
  
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
    } else {
      array_push($dataLine, new Point($time + 0, $value_max));
    }

    if ($max < $value_max)
      $max = $value_max;
  }

  $gd = new GraphData();
  $gd->name = $name;
  $gd->dataLine = $dataLine;
  $gd->max = $max;// + ($max * 0.05) ;
  $gd->color=$color;

  return $gd;
}

function calcYIncrement($range) 
{
  $size = $range->size();
  
  $yincrement = (int) ($size / 2);
  
  $div = 5;

  if ($size > 5000000000) {
	  $div = 1000000000;
  } elseif ($size > 5000000000) {
	  $div = 1000000000;
  } elseif ($size > 500000000) {
	  $div = 100000000;
  } elseif ($size > 50000000) {
	  $div = 10000000;
  } elseif ($size > 5000000) {
	  $div = 1000000;
  } elseif ($size > 500000) {
	  $div = 100000;
  } elseif ($size > 50000) {
	  $div = 10000;
  } elseif ($size > 5000) {
	  $div = 1000;
  } elseif ($size > 500) {
	  $div = 100;
  } elseif ($size > 50) {
	  $div = 10;
  }
  
  $yincrement = $yincrement - ($yincrement % $div); //make the increment divisible by 5

  if ($yincrement == 0) {
      $yincrement = round($size / 4, 2);
  }
  
  //debug("calcYincrement:$size = $yincrement");
    
  return $yincrement;
}

function draw_graph($name, $gds)
{
  global $g_start, $g_end, $g_canvas;
  
  if (! hasData($gds)) {
    //debug(" ! Not displaying graph $name because there was no data");
    $graph = $g_canvas->startGraph($name, new Range(0,1), new Range(0,1));
    $graph->drawLegends($gds);
    $graph->setInvalid("No data");
    draw_invalid($graph);
  } else {
    
    $max_gd = get_largest_data($gds);
    $max_y = $max_gd->max + (0.05 * $max_gd->max);
  
    $x_range = new Range($g_start * 1000, $g_end * 1000);
    
    if ($max_y == 0)
      $y_range = new Range(-1,1);
    else
      $y_range = new Range(0, $max_y);
      
    $yincrement = calcYIncrement($y_range);
    
    $graph = $g_canvas->startGraph($name, $x_range, $y_range);
    
    setup_graph($graph, $name, $x_range, $y_range, $yincrement);
  
    draw_graph_lines($graph, $gds);
    
    $graph->drawLegends($gds);
  }
  
  $graph->end();
}

function hasData($gds)
{
  if (count($gds) == 0)
    return false;
    
  foreach ($gds as $gd) {
    if (count($gd->dataLine) > 0)
      return true;
  }
  
  return false;
}

function get_largest_data($gds)
{
  $max_gd = $gds[0];
  $max = 0;
  
  foreach($gds as $gd) {
    //debug(" checking " . $gd->name . ": count=" . count($gd->dataLine) . " max=" . $gd->max);
    
    if ($gd->max > $max) {
      $max = $gd->max;
      $max_gd = $gd; 
    }
  }
  
  //debug(" returning found max of $max from {$max_gd->name}");
  
  return $max_gd;
}

function setup_graph($graph, $title, $x_range, $y_range, $yincrement, $displayYLabels = true)
{
  global $majorTicks, $minorTicks;
  
  //debug("setup_graph:title={$title},valid={$graph->valid}");
  
  $graph->drawTitle("black");

  $graph->drawGridLines($minorTicks, $yincrement, "light_grey");

  $graph->drawGridLines($majorTicks, $yincrement, "med_grey");

  $graph->drawGrid("dark_grey");

  if ($displayYLabels)
    $graph->drawYGridLabels($yincrement);

  $graph->drawXGridLabels($majorTicks, "formatTime");
}

function draw_invalid($graph)
{
  global $g_canvas;
  
  $graph->drawTitle("black");
  $graph->drawGrid("dark_grey");
}

function draw_graph_lines($graph, $gds)
{
  $graph->canvas->setLineWidth(1);

  $gds = array_reverse($gds);

  foreach($gds as $gd) {
    if (sizeof($gd->dataLine) > 0)
    	$graph->drawLineGraph($gd->dataLine, $gd->color);
  }
}

function draw_graphs($mPage)
{
  global $g_server, $g_canvas;

  $title = sprintf("Server Graphs: %02d - %s",
                   $g_server->getClusterIndex(), $g_server->getId());
                   
  $g_canvas->writeSection($title);
  
  $g_canvas->allocateGraphSpace(3,2);

  $graphs = $mPage->getMeterGraphs();
 
  foreach ($graphs as $graphData) {
    $meterNames = $graphData->getMeterNames();
    //debug("Working on graph " . $graphData->getName() . " with " . count($meterNames) . " meters");
    $gds = getStatDataForGraphByMeterNames($meterNames);
    draw_graph($graphData->getName(), $gds, $g_canvas);
  }
}

function getStatDataForGraphByMeterNames($meterNames)
{
  global $g_pdf_colors;
  
  $cindex = 0;

  $gds = array();   
  foreach ($meterNames as $name) {
    $statItem = new Stat();
    $statItem->statFromName($name);
    
    $color = $g_pdf_colors[$cindex++];
    
    $gd = getStatDataForGraphByStat($statItem, $color);
    
    array_push($gds, $gd);
  }

  return $gds;
}

function getStatDataForGraphByStat($theStat, $color)
{
  $data = findStatByStat($theStat);
  return create_graph_data($theStat->name, $data, $color)  
}

function findStatByStat($theStat) 
{
  global $g_start;
  global $g_end;
  global $stat;
  global $si;

  //$step = ($g_end - $g_start) / 1000;
  $step = ($g_end - $g_start) / 500;

  if ($step < 120)
    $step = 1;
    
  $full_names = $stat->statisticsNames();
  
  //debug("findStatByStat:g_start=$g_start,g_end=$g_end,si=$si,step=$step,theStat=$theStat");
  
  $statList = array();
  foreach ($full_names as $full_name)  {
    array_push($statList, new Stat($full_name));
  }
  
  foreach ($statList as $statItem) {
    
    if ($statItem->server != $si) {
      continue;
    }
    
    if ($statItem->eq($theStat)) {
 	    return $stat->statisticsData($statItem->fullName,
                                   $g_start * 1000,
                                   $g_end * 1000,
                                   $step * 1000);
    }
  }
}

function admin_pdf_heap_dump()
{
  global $g_canvas;
  
  $g_canvas->writeSection("Heap Dump");
  
  $dump = admin_pdf_snapshot("Resin|HeapDump");
  if (! $dump) {
    $g_canvas->writeTextLineIndent(20, "No Data");
    return;
  }
  
  $heap =& $dump["heap"];
  if (! $heap || ! sizeof($heap))
    return;
    
  $g_canvas->setFont("Courier-Bold", 8);
    
  $create_time = $dump["create_time"];
  $g_canvas->writeTextLineIndent(10, "Created: " . $create_time);
  
  $max = 100;

  admin_pdf_selected_heap_dump($heap, "Top Classes by Memory Usage", $max);

  $class_loader_heap = heap_select_classloader($heap);
  admin_pdf_selected_heap_dump($class_loader_heap, "ClassLoader Memory Usage ", $max);
}

function admin_pdf_selected_heap_dump($heap, $title, $max)
{
  global $g_canvas;
  
  uksort($heap, "heap_descendant_cmp");
  
  $g_canvas->writeSubSection($title);
  
  $cols = array(325,65,65,65);

  $g_canvas->setFont("Courier-Bold",8);
  $g_canvas->writeTextColumnHeader($cols[0], 'l', "Class Name");
  $g_canvas->writeTextColumnHeader($cols[1], 'l', "self+desc");
  $g_canvas->writeTextColumnHeader($cols[2], 'l', "self");
  $g_canvas->writeTextColumnHeader($cols[3], 'l', "count");
  $g_canvas->newLine();
  $g_canvas->newLine();
  
  $i = 0;

  $g_canvas->setDataFont();
  
  foreach ($heap as $name => $value) {
    if ($max <= $i++) {
      break;
    }
    
    $g_canvas->writeTextColumn($cols[0], 'l', $name);
    $g_canvas->writeTextColumn($cols[1], 'l', admin_pdf_size($value["descendant"]));
    $g_canvas->writeTextColumn($cols[2], 'l', admin_pdf_size($value["size"]));
    $g_canvas->writeTextColumn($cols[3], 'l', $value["count"]);
    $g_canvas->newLine();
  }
}

function heap_descendant_cmp($a, $b)
{
  return $a->descendant - $b->descendant;
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

function heap_select_classloader(&$values)
{
  $selected = array();

  foreach ($values as $name => $value) {
    if (preg_match("/ClassLoader/", $name)) {
      $selected[$name] = $value;
    }
  }

  return $selected;
}

function admin_pdf_profile()
{
  global $g_canvas;
  
  $profile = admin_pdf_snapshot("Resin|Profile");
  if (! $profile || intval($profile["ticks"]) == 0) {
    return;
  }
  
  $g_canvas->writeSection("CPU Profile");

  admin_pdf_profile_section("Active Threads",
                            $profile,
                            "admin_thread_active");
                            
  admin_pdf_profile_section("All Threads", $profile);
}

function admin_pdf_profile_section($name, $profile, $filter=null)
{
  global $g_canvas;
  
  $g_canvas->writeSubSection($name);

  $g_canvas->setFont("Courier-Bold",8);
  
  $col1 = 100;
  $col2 = 300;

  $g_canvas->writeTextColumn($col1, 'r', "Time:");
  $g_canvas->writeTextColumn($col2, 'l', $profile["total_time"] / 1000 . "s");
  $g_canvas->newLine();
  
  $g_canvas->writeTextColumn($col1, 'r', "Ticks:");
  $g_canvas->writeTextColumn($col2, 'l', $profile["ticks"]);
  $g_canvas->newLine();
  
  $g_canvas->writeTextColumn($col1, 'r', "Sample-Period:");
  $g_canvas->writeTextColumn($col2, 'l', $profile["period"]);
  $g_canvas->newLine();
  
  $g_canvas->writeTextColumn($col1, 'r', "End:");
  $g_canvas->writeTextColumn($col2, 'l', date("Y-m-d H:i", $profile["end_time"] / 1000));
  $g_canvas->newLine();
  
  $g_canvas->setDataFont();
  
  $g_canvas->newLine();
  
  $g_canvas->writeTextColumnHeader(60, 'r', "% Time");
  $g_canvas->writeTextColumnHeader(60, 'r', "Time Self");
  $g_canvas->writeTextColumnHeader(310, 'l', "Method Call");
  $g_canvas->writeTextColumnHeader(70, 'l', "State");
  $g_canvas->newLine();
  $g_canvas->newLine();
  
  $ticks = $profile["ticks"];
  
  if ($ticks <= 0)
    $ticks = 1;
    
  $period = $profile["period"];

  $profile_entries = $profile["profile"];
  
  if ($filter)
    $entries = array_filter($profile_entries, $filter);
  else 
    $entries = $profile_entries;
    
  if (count($entries) < 1) {
    $g_canvas->writeTextLineIndent(15, "No Data");
    return;
  }
  
  usort($entries, "profile_cmp_ticks");

  $max = 100;
  $max_stack = 6;
  $i = 0;

  $g_canvas->setDataFont();
  
  foreach ($entries as $entry) {
    if ($max <= $i++)
      break;

    $stack = admin_pdf_stack($entry, $max_stack);

    $g_canvas->setDataFont();
    $g_canvas->writeTextColumn(60, 'r', sprintf("%.2f%%", 100 * $entry["ticks"] / $ticks));
    $g_canvas->writeTextColumn(60, 'r', sprintf("%.2fs",  $entry["ticks"] * $period / 1000));
    $g_canvas->writeTextColumn(310, 'l', $entry["name"]);
    $g_canvas->writeTextColumn(70, 'l', $entry["state"]);
    $g_canvas->newLine();

    if ($stack) {
      $g_canvas->setDataFont(7);
      $g_canvas->writeTextWrapIndent(130, $stack);
    } else {
      $g_canvas->newLine();
    }
  }
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

function profile_cmp_ticks($a, $b)
{
  return $b["ticks"] - $a["ticks"];
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

function admin_pdf_thread_dump()
{
  global $g_canvas;

  $dump = admin_pdf_snapshot("Resin|ThreadDump");
  if (! $dump) {
    $g_canvas->writeTextLineIndent(20, "No Data");
    return;
  }
  
  $g_canvas->writeSection("Thread Dump");
  
  $g_canvas->setFont("Courier-Bold", 8);

  $create_time = $dump["create_time"];
  $g_canvas->writeTextLine("Created: " . $create_time);
  $g_canvas->newLine();

  $entries =& $dump["thread_dump"];

  admin_pdf_analyze_thread_dump($entries);

  usort($entries, "thread_dump_cmp");

  $max = 100;
  $max_stack = 32;
  $size = sizeof($entries);

  $i = 0;
  while ($i < $size) {
    if ($i > $max)
      break;
      
    $entry =& $entries[$i];

    $g_canvas->setDataFont(8);
    $i = admin_pdf_shared_entries($i, &$entries, $g_canvas);
    
    $stack = admin_pdf_thread_stack($entry, $max_stack);

    $g_canvas->setDataFont(7);
    $g_canvas->writeTextWrapIndent(20, $stack);
  }
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

function admin_pdf_shared_entries($i, &$entries, $g_canvas)
{
  $stack = thread_dump_stack($entries[$i]);

  for (;
       ($entry =& $entries[$i]) && $stack == thread_dump_stack($entry);
       $i++) {
    
    $g_canvas->writeTextColumn(350, 'l', sprintf("%.40s", $entry["name"]));
    
    $state = $entry["state"];
    if ($state == "RUNNABLE" && $state["native"])
      $state .= " (JNI)";
      
    $g_canvas->writeTextColumn(85, 'l', $state);
    $g_canvas->writeTextColumn(40, 'l', "[" . $entry["id"] . "]");
    $g_canvas->newLine();

    $lock = $entry["lock"];
    if ($lock) {
      $trace = "waiting on " . $lock["name"];

      if ($lock["owner_name"]) {
        $trace .= " owned by [" . $lock["owner_id"] . "] " . $lock["owner_name"];
      }

      $g_canvas->writeTextIndent(10, $trace);
      $g_canvas->newLine();
    }
  }

  return $i;
}

function admin_pdf_draw_log()
{
  global $log_mbean, $g_canvas, $g_pdf, $g_end, $g_start;
  
  $g_canvas->writeSection("Full Log");
  
  admin_pdf_log_messages(null,
                         null,
                         true,
                         $g_start, $g_end,
                         -1);
}

function admin_pdf_jmx_dump()
{
  global $g_canvas;

  $dump = admin_pdf_snapshot("Resin|JmxDump");
  if (! $dump) {
    return;
  }
  
  $g_canvas->writeSection("JMX Dump");

  $entries =& $dump["jmx"];

  ksort($entries);
  
  $last_domain;
  $last_domain_id;
  
  $last_mbean;

  foreach ($entries as $name => &$values) {
    
    $domain = substr($name, 0, strpos($name, ":"));
    $mbean = substr($name, 0, strpos($name, ",") ?: strlen($name));
    
    if ($domain != $last_domain)
      $last_domain_id = $g_canvas->addToOutline($domain);
    
    if ($mbean != $last_mbean)
      $g_canvas->addToOutline($mbean, $last_domain_id);
      
    $last_domain = $domain;
    $last_mbean = $mbean;
    
    $g_canvas->setFont("Courier-Bold", 8);
    $g_canvas->writeTextWrap($name);

    $g_canvas->setFont("Courier", 8);
    admin_pdf_jmx_attributes($g_canvas, $values);
    $g_canvas->newLine();
  }
}

function admin_pdf_jmx_attributes($g_canvas, &$values)
{
  ksort($values);

  $col1 = 180;
  $col2 = $g_canvas->getLineWidth() - $col1 - 20;
  
  foreach ($values as $key => $value) {
    $g_canvas->writeTextColumn(10, 'l', "");
    $g_canvas->writeTextColumn($col1, 'l', $key);
    $g_canvas->writeTextColumn($col2, 'l', admin_pdf_attribute_value($value));
    $g_canvas->newLine();
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
      
      for ($j = 0; $j < $depth + 1; $j++)
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
    return $value;
  }
}

/*

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
          = get_stat_item($stat, $statItem->fullName);
      }
    }
  }
  return $map;
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
*/

class Stat 
{
  private $server;
  private $category;
  private $subcategory;
  private $fullName;
  private $elements;
  private $name;
  
  public function __construct()
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
  }

  public function statFromName($fullName, $server="00")
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

  public function __get($name) 
  {
    return $this->$name;
  }

  public function __toString()
  {
    return "Stat(name={$this->name},server={$this->server},category={$this->category},subcategory={$this->subcategory})";
  }

  public function eq($that)
  {
    return $this->name == $that->name
           && $this->category == $that->category
           && $this->subcategory == $that->subcategory;
  }
}

?>
