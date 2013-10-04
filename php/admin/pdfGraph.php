<?php

require_once "WEB-INF/php/inc.php";
require_once 'PdfCanvas.php';
require_once 'PdfCanvasGraph.php';

//error_reporting(E_ALL);

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
    
    
$g_health_colors = array(
    new RGBColor(0x90, 0x90, 0x90), // UNKNOWN
    new RGBColor(0x00, 0xc0, 0x00), // OK
    new RGBColor(0xCC, 0x88, 0x11), // WARNING
    new RGBColor(0xC0, 0x00, 0x00), // CRITICAL
    new RGBColor(0xC0, 0x00, 0x00)); // FATAL

if (! mbean_init()) {
  debug("Failed to load admin, die");
  return;
}

$stat = get_stats_service();
if (! $stat) {
  debug("Postmortem analysis:: requires Resin Professional and a <resin:StatService/> and <resin:LogService/> defined in the resin.xml.");
  return;
}

$g_log_mbean = $g_mbean_server->getLogService();

function  my_error_handler($error_type, $error_msg, $errfile, $errline) 
{
  #if(!startsWith($error_msg,"Can't access private field")) {
  #  debug("ERROR HANDLER: type $error_type, msg $error_msg, file $errfile, lineno $errline");
  #}
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
}

function pdf_header()
{
  global $g_start, $g_end, $g_title, $g_canvas, $g_jmx_dump_time;
  global $g_pdf_warnings;
    
  $g_canvas->writeSection("$g_title Report", false);
  
  $col1 = 85;
  $col2 = 300;
  
  $g_canvas->writeTextColumn($col1, 'r', "Report Generated:");
  $g_canvas->writeTextColumn($col2, 'l', date("Y-m-d H:i", time()));
  $g_canvas->newLine();
  
  $g_canvas->writeTextColumn($col1, 'r', "Snapshot Time:");
  $g_canvas->writeTextColumn($col2, 'l', $g_jmx_dump_time);
  $g_canvas->newLine();
  
  $g_canvas->writeTextColumn($col1, 'r', "Data Range:");
  $g_canvas->writeTextColumn($col2, 'l', date("Y-m-d H:i", $g_start) . " through " . date("Y-m-d H:i", $g_end));
  $g_canvas->newLine();
  
  if (count($g_pdf_warnings) > 0) {
    $g_canvas->newLine();
    $g_canvas->setFontAndColor("Helvetica-Bold", "10", "red");
    $g_canvas->writeTextLine("WARNING");
    
    $g_canvas->setFontAndColor("Helvetica-Bold", "9", "black");
    
    foreach($g_pdf_warnings as $warning) {
      $g_canvas->writeTextLineIndent(20, $warning);
    }
    
    $g_canvas->setTextFont();
  }
}

function pdf_summary()
{
  global $g_si, $g_canvas;

  $summary = pdf_summary_fill();
  if (! $summary) {
    return;
  }
    
  $g_canvas->writeSubsection("Environment");
  
  $g_canvas->writeTextLine("{$summary['ipAddress']} running as {$summary['userName']}");
  $g_canvas->writeTextLine($summary["jvm"]);
  $g_canvas->writeTextLine($summary["machine"]);
  
  $g_canvas->writeSubsection("System Resources");
  
  $col1 = 85;
  $col2 = 300;
  
  $g_canvas->writeTextColumn($col1, 'r', "JVM Heap:");
  $g_canvas->writeTextColumn($col2, 'l', "{$summary['usedHeap']} used of {$summary['totalHeap']} ({$summary['freeHeap']} free)");
  $g_canvas->newLine();
  
  $g_canvas->writeTextColumn($col1, 'r', "Physical Memory:");
  $g_canvas->writeTextColumn($col2, 'l', "{$summary['usedPhysical']} used of {$summary['totalPhysical']} ({$summary['freePhysical']} free)");
  $g_canvas->newLine();
  
  $g_canvas->writeTextColumn($col1, 'r', "Swap Space:");
  $g_canvas->writeTextColumn($col2, 'l', "{$summary['usedSwap']} used of {$summary['totalSwap']} ({$summary['freeSwap']} free)");
  $g_canvas->newLine();
  
  $g_canvas->writeTextColumn($col1, 'r', "File Descriptors:");
  $g_canvas->writeTextColumn($col2, 'l', "{$summary['usedFd']} used of {$summary['totalFd']} ({$summary['freeFd']} free)");
  $g_canvas->newLine();

  $g_canvas->writeSubsection("Resin Instance");
  
  $g_canvas->writeTextLine("$g_si - {$summary['serverID']}, {$summary['cluster']} Cluster");
  
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
  $g_canvas->writeTextLine("Up for {$summary['ups']}");
  $g_canvas->writeTextLine("Startup Message: " . ($summary['watchdogStartMessage'] ?: "Normal Startup"));
  $g_canvas->writeTextLine("Load Balance Status: {$summary["server_state"]}");
  
  $g_canvas->writeSubsection("Licenses");
  
  $license_array = $summary["licenses"];
  foreach($license_array as $license_data) {
    $g_canvas->writeTextLine($license_data[0]);
    $g_canvas->writeTextLineIndent(20, $license_data[1]);
    $g_canvas->writeTextLineIndent(20, $license_data[2]);
  }
}

function pdf_threads()
{
  global $g_canvas, $g_jmx_dump;
  
  $g_canvas->writeSection("Threads", true);
  
  $ports = pdf_ports_fill();
  
  if ($ports) {
    $g_canvas->writeSubsection("Port Threads");
  
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
  
  $g_canvas->newLine();
  
  $g_canvas->writeSubsection("Thread Scoreboards");
  
  $dump = pdf_load_json_dump("Resin|Scoreboard|resin");
  if (! $dump) {
    $g_canvas->setTextFont();
    $g_canvas->newLine();
    $g_canvas->writeTextLineIndent(20, "A scoreboard report was not generated during the selected timeframe.");
  } else {
    $timestamp = create_timestamp($dump);
    $g_canvas->setFont("Courier-Bold", "8");
    $g_canvas->writeTextLine("Timestamp: $timestamp");
    $g_canvas->newLine();
  
    $scoreboards =& $dump["scoreboards"];
    $keys =& $dump["keys"];
    
    foreach ($scoreboards as $name => $value) {
      $g_canvas->setFont("Courier-Bold", "10");
      $g_canvas->writeTextColumnHeader($g_canvas->getLineWidth(), 'l', $name);
      $g_canvas->newLine();
      
      $g_canvas->setFont("Courier", "10");
      $g_canvas->writeTextColumn($g_canvas->getLineWidth(), 'l', $value);
      $g_canvas->newLine();
      $g_canvas->newLine();
    }
    
    $g_canvas->setFont("Courier-Bold", "10");
    $g_canvas->writeTextColumnHeader(220, 'l', "Scoreboard Key");
    $g_canvas->newLine();
    
    $g_canvas->setFont("Courier", "10");
        
    foreach ($keys as $name => $value) {
      $g_canvas->writeTextColumn(20,  'c', $name);
      $g_canvas->writeTextColumn(100, 'l', $value);
      $g_canvas->newLine();
    }
  }
}

function pdf_summary_fill()
{
  global $g_jmx_dump;
  
  if (!$g_jmx_dump)
    return null;
  
  $server = $g_jmx_dump["resin:type=Server"];
  $resin = $g_jmx_dump["resin:type=Resin"];  
  $runtime = $g_jmx_dump["java.lang:type=Runtime"];
  $os = $g_jmx_dump["java.lang:type=OperatingSystem"];
  $licenseStore = $g_jmx_dump["resin:type=LicenseStore"];

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
  $cluster = $g_jmx_dump[$cluster_mbean_name];
  
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
  $summary["usedHeap"] = pdf_format_memory($server["RuntimeMemory"] - $server["RuntimeMemoryFree"]);
  
  $summary["totalSwap"] = pdf_format_memory($os["TotalSwapSpaceSize"]);
  $summary["freeSwap"] = pdf_format_memory($os["FreeSwapSpaceSize"]);
  $summary["usedSwap"] = pdf_format_memory($os["TotalSwapSpaceSize"] - $os["FreeSwapSpaceSize"]);
  
  $summary["totalPhysical"] = pdf_format_memory($os["TotalPhysicalMemorySize"]);
  $summary["freePhysical"] = pdf_format_memory($os["FreePhysicalMemorySize"]);
  $summary["usedPhysical"] = pdf_format_memory($os["TotalPhysicalMemorySize"] - $os["FreePhysicalMemorySize"]);
  
  $summary["totalFd"] = $os["MaxFileDescriptorCount"];
  $summary["usedFd"] = $os["OpenFileDescriptorCount"];
  $summary["freeFd"] = $os["MaxFileDescriptorCount"] - $os["OpenFileDescriptorCount"];
  
  $licenses = array();
  
  $license_names = $licenseStore["ValidLicenses"];
  foreach($license_names as $license_name) {
    $license = $g_jmx_dump[$license_name];
    $license_data = array($license['Description'], $license['ExpireMessage'], $license['Path']);
    array_push($licenses, $license_data);
  }
  
  $summary["licenses"] = $licenses;
  
  return $summary;
}

function pdf_ports_fill()
{
  global $g_jmx_dump;
  
  if (!$g_jmx_dump)
    return null;
  
  $server = $g_jmx_dump["resin:type=Server"];
    
  $ports = array();
  
  $port_names = $server["Ports"];
  foreach($port_names as $port_name) {
    $port = $g_jmx_dump[$port_name];
  
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
  
  return $ports;
}

function getMeterGraphPage($pdfName)
{
  global $stat;

  if ($stat == null) {
    return null;
  }
  
  $mpages = $stat->getMeterGraphPages();
  
  foreach($mpages as $mg) {
    if ($mg->name == $pdfName) {
      return $mg;
    }
  }
}

function pdf_load_json_dump($name, $start=0, $end=0)
{
  global $g_si, $g_log_mbean, $g_start, $g_end;

  if (! $g_log_mbean) {
    return;
  }
  
  if (! $end) {
   $end = $g_end;
  }
 
  if (! $start) {
    $start = $end - (2 * WEEK);
  }
    
  $key = "$g_si|$name";
  
  $times = $g_log_mbean->findMessageTimesByType($key,
                                              "info",
                                              $start * 1000,
                                              $end * 1000);
                                              
  //debug("pdf_load_json_dump found " . count($times). " logs for $key (" . date('Y-m-d H:i', $start) . " - " . date('Y-m-d H:i', $end) . ")");
  
  if (! $times || sizeof($times) == 0) {
    return;
  }

  sort($times);

  $time = $times[sizeof($times) - 1];

  if (! $time) {
    return;
  }
  
  //debug("pdf_load_json_dump using time " . date('Y-m-d H:i:s', $time/1000));
    
  $msgs = $g_log_mbean->findMessagesByType("$g_si|$name",
                                         "info", $time, $time);

  $msg = $msgs[0];
  if (! $msg)
    return;
    
  return json_decode($msg->getMessage(), true);
}

function pdf_get_dump_data(&$json, $key=null) 
{
  $data =& $json["data"];
  
  if (! $data && $key) // for BC
    $data =& $json[$key];
  
  return $data;
}

function pdf_format_memory($memory)
{
  return sprintf("%.2fMB", $memory / (1024 * 1024));
}

function pdf_health_status_label($val) 
{
  global $g_jmx_dump;
  
  if (!$g_jmx_dump)
    return $val;
  
  $index = (double) $val;
  
  if ($index < 0 || floor($index) != $index)
    return $val;
  
  $labels_mbean = $g_jmx_dump["resin:type=HealthSystem"];
  if ($labels_mbean) {
    $labels = $labels_mbean["Labels"];
      if ($labels && $index <= count($labels)) {
        return $labels[$index];
      }
  }
  
  return $val;
}

function pdf_availability_label($val)
{
  if ($val == 0)
    return "DOWN";
  else if ($val == 3)
    return "UP";
  else return null;
}

function pdf_health()
{
  global $g_jmx_dump, $g_jmx_dump_time, $g_si, $g_start, $g_end, $g_canvas, $g_health_colors;
  
  $g_canvas->writeSection("Health");
  
  if ($g_jmx_dump) {
    $resin_health = $g_jmx_dump["resin:type=HealthCheck,name=Resin"];
    
    if ($resin_health) {
      $status_index = $resin_health["StatusOrdinal"];
      $color = $g_health_colors[$status_index];
      
      $g_canvas->setFontAndColor("Courier-Bold", 16, "white");
      
      $y = $g_canvas->text_y - ($g_canvas->getLineHeight()/4);
  
      $g_canvas->setColor($color);
      $g_canvas->setLineWidth($g_canvas->getLineHeight());
  
      $g_canvas->moveToXY($g_canvas->getLeftMargin(), $y);
      $g_canvas->lineToXY($g_canvas->getRightMargin(), $y);
      $g_canvas->stroke();
      
      $g_canvas->setFontAndColor("Courier-Bold", 16, "white");
      $g_canvas->writeTextLineCenter($resin_health["Status"]);
    }
    
    $g_canvas->setFontAndColor("Courier-Bold", 8, "black");
    $g_canvas->writeText("Timestamp: $g_jmx_dump_time");
    
    $g_canvas->setTextFont();
    
    $g_canvas->graph_padding_x = 55;
    $g_canvas->allocateGraphSpace(4,1);
    
    $pattern = "/Resin\|Health/";
  
    pdf_stat_graph_regexp("", $pattern, $g_si, "pdf_health_status_label");
    
    $g_canvas->newLine();
    $g_canvas->writeSubSection("Health Check Status");
    
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
    
    $health_dump = preg_grep_keys("/type=HealthCheck/", $g_jmx_dump);
    
    if (! $health_dump) {
      $g_canvas->setTextFont();
      $g_canvas->newLine();
      $g_canvas->writeTextLineIndent(20, "No health checks were enabled in the selected timeframe.");
    } else {
      foreach ($health_dump as $check) {
        $g_canvas->writeTextColumn($w1, 'c', $check["Status"]);
        $g_canvas->writeTextColumn($w2, 'l', $check["Name"]);
        $g_canvas->writeTextColumn($w3, 'l', $check["Message"]);
        $g_canvas->newLine();
      }
    }
  }
  
  pdf_availability();
    
  pdf_health_events($g_start, $g_end);
}

function pdf_health_events($start, $end)
{
  global $g_canvas, $g_mbean_server, $g_server_index;
  
  $g_canvas->newLine();
  $g_canvas->writeSubSection("Health Events");
  
  $health_service = $g_mbean_server->lookup("resin:type=HealthSystem");
  if ($health_service)
    $events = $health_service->findEvents($g_server_index, 
                                          $start * 1000, $end * 1000, 9999);
  
  if (count($events) > 0) {
    $w1 = 95;
    $w2 = 85;
    $w3 = 100;
    $w4 = 245;
  
    $g_canvas->setFont("Courier-Bold", "8");
    
    $g_canvas->writeTextColumnHeader($w1, 'c', "Date");
    $g_canvas->writeTextColumnHeader($w2, 'c', "Event Type");
    $g_canvas->writeTextColumnHeader($w3, 'c', "Source");
    $g_canvas->writeTextColumnHeader($w4, 'c', "Message");
    $g_canvas->newLine();
    $g_canvas->newLine();
    
    $g_canvas->setFont("Courier", "8");
    
    foreach ($events as $event) {
      $ts = strftime("%Y-%m-%d %H:%M:%S", $event->timestamp / 1000);
      
      $g_canvas->writeTextColumn($w1, 'l', $ts);
      $g_canvas->writeTextColumn($w2, 'l', $event->typeDescription);
      $g_canvas->writeTextColumn($w3, 'l', $event->source);
      $g_canvas->writeTextColumn($w4, 'l', $event->message);
      $g_canvas->newLine();
      
      if ($event->type == "START_TIME") {
        $g_canvas->writeHrule(0, .5, "grey");
        $g_canvas->newLine();
      }
    }    
  } else {
    $g_canvas->setTextFont();
    $g_canvas->writeTextLineIndent(20, "No Events");
  }
}

function pdf_availability()
{
  global $g_si, $g_start, $g_end, $g_period, $g_canvas, $g_label, $g_downtimes;

  $stat = get_stats_service();
  if (! $stat)
    return;
  
  
  $downtimes = $g_downtimes;
  if (is_null($downtimes)) {
    $downtimes = $stat->getDownTimes($g_si, $g_start * 1000, $g_end * 1000);
    $g_downtimes = $downtimes;
  }
  
  if (is_null($downtimes)) {
    $g_canvas->newLine();
    $g_canvas->writeTextLineIndent(20, "No Data");
    return;
  }
  
  $downtimes = array_reverse($downtimes);
  
  $total = 0;
  $count = 0;
  
  foreach($downtimes as $downtime) {
    $et = $downtime->ET;
    if ($et > 0) {
      $total += $et;
      $count++;
    }
  }
  
  $total /= 1000;
  $avg = 0;
  if ($count > 0)
    $avg = $total / $count;
  $uptime = 100 - (($total / $g_period) * 100);  
  
  $g_canvas->writeSubsection("Availability");
  
  $col1 = 120;
  $col2 = 300;
  
  $g_canvas->writeTextColumn($col1, 'r', "Period:");
  $g_canvas->writeTextColumn($col2, 'l', format_seconds($g_period));
  $g_canvas->newLine();
  
  $g_canvas->writeTextColumn($col1, 'r', "Data Range:");
  $g_canvas->writeTextColumn($col2, 'l', date("Y-m-d H:i", $g_start) . " through " . date("Y-m-d H:i", $g_end));
  $g_canvas->newLine();
  
  $g_canvas->writeTextColumn($col1, 'r', "Number of Downtimes:");
  $g_canvas->writeTextColumn($col2, 'l', $count);
  $g_canvas->newLine();
  
  $g_canvas->writeTextColumn($col1, 'r', "Total Downtime:");
  $g_canvas->writeTextColumn($col2, 'l', format_seconds($total));
  $g_canvas->newLine();

  $g_canvas->writeTextColumn($col1, 'r', "Average Downtime Period:");
  $g_canvas->writeTextColumn($col2, 'l', format_seconds($avg));
  $g_canvas->newLine();
  
  $g_canvas->writeTextColumn($col1, 'r', "Availability: ");
  $g_canvas->writeTextColumn($col2, 'l', number_format($uptime, 4) . "%");
  $g_canvas->newLine();
  
  $g_canvas->writeSubsection("Downtimes");
  
  $col1 = 120;
  $col2 = 120;
  $col3 = 100;
  $col4 = 185;
  
  $g_canvas->setFont("Courier-Bold", 9);
  
  $g_canvas->writeTextColumnHeader($col1, 'l', "Stop Time");
  $g_canvas->writeTextColumnHeader($col2, 'l', "Restart Time");
  $g_canvas->writeTextColumnHeader($col3, 'l', "Elapsed Time");
  $g_canvas->writeTextColumnHeader($col3, 'l', "Notes");
  $g_canvas->newLine();
  $g_canvas->newLine();
  
  $g_canvas->setFont("Courier", 9);
    
  foreach($downtimes as $downtime) {
    $et = $downtime->ET/1000;
    
    if ($downtime->isDataAbsent()) {
      $note = '* No data: using report start time';
    } else if ($downtime->isEstimated()) {
      $note = '* Approximate';
    } else {
      $note = '';
    }
    
    $g_canvas->writeTextColumn($col1, 'l', date("Y-m-d H:i:s", $downtime->startTime / 1000) . ($note ? " *" : ""));
    $g_canvas->writeTextColumn($col2, 'l', date("Y-m-d H:i:s", $downtime->endTime / 1000));
    $g_canvas->writeTextColumn($col3, 'l', format_seconds($et));
    $g_canvas->writeTextColumn($col4, 'l', $note);      
      
    $g_canvas->newLine();
  }
}

function pdf_log_messages($title,
                          $regexp,
                          $match,
                          $start, 
                          $end,
                          $max=-1)
{
  global $g_log_mbean, $g_canvas;

  if (! $g_log_mbean) {
    return;
  }
  
  if ($title) {
    $g_canvas->writeSubsection($title);
  }
  
  $messages = $g_log_mbean->findMessages("warning",
                                         $start * 1000,
                                         $end * 1000);
                                       
                                       
  //debug("pdf_log_messages:$title,start=$start,end=$end,max=$max,count=" . count($messages));

  $wrote = false;
  if (count($messages) > 0) {
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
      
      $wrote = true;
    }
  }
  
  if (! $wrote) {
    $g_canvas->setTextFont();
    $g_canvas->writeTextLineIndent(20, "No Logs");
  }
}

function pdf_config()
{
  global $g_canvas;
  global $g_jmx_dump, $g_jmx_dump_time;
  global $g_start, $g_end;
  global $g_si, $g_label;
  
  if (!$g_jmx_dump)
    return;

  $g_canvas->writeSection("Configuration");
  
  $g_canvas->setFont("Courier-Bold", "8");
  $g_canvas->writeTextLine("Timestamp: $g_jmx_dump_time");
  $g_canvas->newLine();
  
  $server = $g_jmx_dump["resin:type=Server"];

  $cluster_name = $server["Cluster"];
  $cluster = $g_jmx_dump[$cluster_name];
  
  $cluster_server_name = $server["SelfServer"];
  $cluster_server = $g_jmx_dump[$cluster_server_name];
  
  $col1 = 40;
  $col2 = 500;
  
  $g_canvas->setTextFont();  
  
  $g_canvas->writeTextColumn($col1, 'r', "Cluster:");
  $g_canvas->writeTextColumn($col2, 'l', $cluster["Name"]);
  $g_canvas->newLine();
  
  $g_canvas->writeTextColumn($col1, 'r', "Server:");
  $g_canvas->writeTextColumn($col2, 'l', $g_label);
  $g_canvas->newLine();
  
  $g_canvas->writeSubSection("Server Configuration Files");
  
  $resin = $g_jmx_dump["resin:type=Resin"];
  
  pdf_display_configs($resin["Configs"]);
  
  #$g_canvas->writeSubSection("Web Applications");
  
  $webapps = preg_grep_keys("/type=WebApp[$,]/", $g_jmx_dump); 
  
  foreach($webapps as $webapp) {
    if ($webapp["State"] == "STOPPED_IDLE")
      continue;
      
     pdf_webapp($webapp);
     $g_canvas->newLine();
  }
}

function pdf_webapp($webapp)
{
  global $g_canvas, $g_jmx_dump;
  
  $session_manager_name = $webapp["SessionManager"];
  $session_manager = $g_jmx_dump[$session_manager_name];
  
  $host_name = $webapp["Host"];
  $host = $g_jmx_dump[$host_name];
  
  $host_name = empty($host["HostName"]) ? "default" : $host["HostName"];
  
  $col1 = 60;
  $col2 = 90;
  $col3 = 60;
  $col4 = 100;
  $col5 = 90;
  $col6 = 90;
  
  $context_path = empty($webapp["ContextPath"]) ? "/" : $webapp["ContextPath"];
  
  $start_time = $webapp["StartTime"];
  if ($start_time) {
    $start_time = java_iso8601_to_date($start_time);
    $start_time = format_ago_unixtime($start_time);
  }
  
  $last_500_time = $webapp["Status500LastTime"];
  if ($last_500_time) {
    $last_500_time = java_iso8601_to_date($last_500_time);
    $last_500_time = format_ago_unixtime($last_500_time);
  }
  
  $g_canvas->writeSubSection("Host: $host_name, WebApp: $context_path");
  $g_canvas->newLine();
  
  $g_canvas->setTextFont();
  
  $g_canvas->writeTextColumnHeader($col1, 'c', "State");
  $g_canvas->writeTextColumnHeader($col2, 'c', "Startup Mode");
  $g_canvas->writeTextColumnHeader($col3, 'c', "Uptime");
  $g_canvas->writeTextColumnHeader($col4, 'c', "500 Errors");
  $g_canvas->writeTextColumnHeader($col5, 'c', "Active Requests");
  $g_canvas->writeTextColumnHeader($col6, 'c', "Active Sessions");
  $g_canvas->newLine();
  
  $g_canvas->writeTextColumn($col1, 'c', $webapp["State"]);
  $g_canvas->writeTextColumn($col2, 'c', $webapp["StartupMode"]);
  $g_canvas->writeTextColumn($col3, 'c', $start_time);
  $g_canvas->writeTextColumn($col4, 'c', $webapp["Status500CountTotal"] . ($last_500_time ? " (" . $last_500_time . " ago)" : ""));
  $g_canvas->writeTextColumn($col5, 'c', $webapp["RequestCount"]);
  $g_canvas->writeTextColumn($col6, 'c', $session_manager["SessionActiveCount"]);
  $g_canvas->newLine();
  
  $configs = $webapp["Configs"];
  if (count($configs) > 0) {
    $g_canvas->newLine();
    
    $g_canvas->setFont("Helvetica-Bold", 9);
    $g_canvas->writeTextLine("WebApp Configuration Files");
    $g_canvas->newLine();
    
    $g_canvas->setTextFont();
    
    pdf_display_configs($webapp["Configs"]);
  }
}

function pdf_display_configs($config_names)
{
  global $g_jmx_dump, $g_canvas;
  
  $col1 = 65;
  $col2 = 525;
  
  for($i = 0; $i<count($config_names); $i++) {
    $config = $g_jmx_dump[$config_names[$i]];
    
    $g_canvas->writeTextColumn($col1, 'r', "Path:");
    $g_canvas->writeTextColumn($col2, 'l', $config["Path"]);
    $g_canvas->newLine();

    $g_canvas->writeTextColumn($col1, 'r', "Length:");
    $g_canvas->writeTextColumn($col2, 'l', $config["Length"]);
    $g_canvas->newLine();
    
    $last_modified = $config["LastModified"];
    if ($last_modified > 0)
      $last_modified = date("Y-m-d H:i:s", $last_modified/1000);
    else
      $last_modified = "Unavailable";
    
    $g_canvas->writeTextColumn($col1, 'r', "Last Modified:");
    $g_canvas->writeTextColumn($col2, 'l', $last_modified);
    $g_canvas->newLine();
    
    $g_canvas->writeTextColumn($col1, 'r', "CRC-64:");
    $g_canvas->writeTextColumn($col2, 'l', sprintf("%x", $config["Crc64"]));
    $g_canvas->newLine();
    
    if ($i < count($config_names)-1)
      $g_canvas->newLine();
  }  
}

function pdf_draw_cluster_graphs()
{
  global $g_jmx_dump, $g_jmx_dump_time, $g_end, $g_canvas, $g_label;
  
  if (!$g_jmx_dump)
    return;
  
  $server = $g_jmx_dump["resin:type=Server"];

  $cluster_name = $server["Cluster"];
  $cluster = $g_jmx_dump[$cluster_name];
  
  $cluster_server_name = $server["SelfServer"];
  $cluster_server = $g_jmx_dump[$cluster_server_name];
  
  $cluster_server_names = $cluster["Servers"];
  
  $g_canvas->writeSection("Cluster Status");
  
  $g_canvas->setFont("Courier-Bold", "8");
  $g_canvas->writeTextLine("Timestamp: $g_jmx_dump_time");
  $g_canvas->newLine();
  
  $col1 = 75;
  $col2 = 500;
  
  $g_canvas->setTextFont();
  
  $g_canvas->writeTextColumn($col1, 'r', "Cluster Name:");
  $g_canvas->writeTextColumn($col2, 'l', $cluster["Name"]);
  $g_canvas->newLine();

  $this_server = $g_label;
  
  if ($cluster_server["DynamicServer"]) 
    $this_server .= " (Dynamic Server)";
  
  if ($cluster_server["TriadServer"]) 
    $this_server .= " (Triad Server)";
    
  $g_canvas->writeTextColumn($col1, 'r', "This Server:");
  $g_canvas->writeTextColumn($col2, 'l', $this_server);
  $g_canvas->newLine();

  $g_canvas->writeTextColumn($col1, 'r', "Total Servers:");
  $g_canvas->writeTextColumn($col2, 'l', count($cluster_server_names));
  $g_canvas->newLine();
  
  $triad_count = 0;
  $triad_line = "";
  
  $dynamic_count = 0;
  $dynamic_line = "";

  foreach ($cluster_server_names as $cluster_server_name) {
    
    $server = $g_jmx_dump[$cluster_server_name];
    
    $si = sprintf("%02d", $server["ClusterIndex"]);
    $name = "{$si} - {$server["Name"]}";
    
    if ($server["TriadServer"]) {
      $triad_count++;
      if (strlen($triad_line) > 0) {
        $triad_line = $triad_line . ", " . $name;
      } else {
        $triad_line = $name;
      }
    }
    
    if ($server["DynamicServer"]) {
      $dynamic_count++;
      if (strlen($dynamic_line) > 0) {
        $dynamic_line = $dynamic_line . ", " . $name;
      } else {
        $dynamic_line = $name;
      }
    }
  }
  
  $g_canvas->writeTextColumn($col1, 'r', "Triad Servers:");
  $g_canvas->writeTextColumn($col2, 'l', $triad_count > 0 ? "$triad_count ($triad_line)" : "None");
  $g_canvas->newLine();
  
  $g_canvas->writeTextColumn($col1, 'r', "Dynamic Servers:");
  $g_canvas->writeTextColumn($col2, 'l', $dynamic_count > 0 ? "$dynamic_count ($dynamic_line)" : "None");
  $g_canvas->newLine();

  $g_canvas->writeSubSection("Cluster Health");
  
  $g_canvas->graph_padding_x = 55;
  $g_canvas->allocateGraphSpace(4,1);
  
  $pattern = "/Resin\|Health\|Resin$/";

  pdf_stat_graph_regexp("Cluster Health", $pattern, null, "pdf_health_status_label");
    
  $g_canvas->writeSubSection("Cluster Members");
  
  $g_canvas->graph_padding_x = 25;
  $g_canvas->allocateGraphSpace(2,2);
  
  $stat_names = Array("Resin|Uptime|Start Count", 
  									  "Resin|Log|Critical", 
  									  "Resin|Log|Warning");

  foreach ($cluster_server_names as $cluster_server_name) {
    
    $server = $g_jmx_dump[$cluster_server_name];

    $si = sprintf("%02d", $server["ClusterIndex"]);
    $title = "{$si} - {$server["Name"]}";
    
    pdf_stat_graph($title, $stat_names, $si);
  }
}

function calcYIncrement($size) 
{
  $size = round($size);
  if ($size == 0)
    $size = 1;
  
  $yincrement = (int) ($size / 4);
  
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
  
  $yincrement = $yincrement - ($yincrement % $div);
  
  if ($yincrement == 0) {
      $yincrement = round($size / 4, 2);
  }
    
  return $yincrement;
}

function pdf_stat_graph_regexp($title, $pattern, $si, $label_func=null)
{
  $stat = get_stats_service();
  if (! $stat)
    return;
    
  $stat_names = $stat->statisticsNames();
  
  if (! is_null($si)) {
    $stat_names = preg_grep("/^{$si}\|.*/", $stat_names);
  }
  
  $stat_names = preg_grep($pattern, $stat_names);
  
  sort($stat_names);
  
  pdf_stat_graph($title, $stat_names, $si, $label_func);
}

function pdf_stat_graph($title, $stat_names, $si, $label_func=null)
{
  global $g_pdf_colors;
  
  $stat = get_stats_service();
  if (! $stat)
    return;
  
  $color_counter = 0;
  $counter = 0;
  $data_array = array();
  
  foreach($stat_names as $stat_name) {
    if(! is_null($si) && ! preg_match("/^{$si}\|.*/", $stat_name)) {
      $stat_name = "${si}|{$stat_name}";
    }
    
    if(! is_null($si)) {
      $name = substr($stat_name, strrpos($stat_name, "|", -1)+1);
    } else {
      $name = $stat_name;
    }
    
    $stat_data = pdf_get_stat_data($stat, $stat_name);
    
    $color = $g_pdf_colors[$color_counter++];
    if ($color_counter == sizeof($g_pdf_colors))
    	$color_counter = 0;
    
    $graph_data = pdf_create_graph_data($name, $stat_data, $color);
    array_push($data_array, $graph_data);
  }
  
  $blockdata_array = pdf_get_blockdata($stat);

  pdf_draw_graph($title, $data_array, $blockdata_array, $label_func);
}

function pdf_get_stat_data($stat, $full_name)
{
  global $g_start, $g_end;
  
  $step = ($g_end - $g_start) / 500;
  
  if ($step < 120)
    $step = 1;
  
  $data = $stat->statisticsData($full_name,
                                $g_start * 1000, 
                                $g_end * 1000,
                                $step * 1000);
  
  //debug("pdf_get_stat_data:name=$full_name,step=$step,data=" . count($data));

  return $data;
}

function pdf_get_blockdata($stat)
{
  global $g_si, $g_start, $g_end, $g_downtimes;
  
  $bda = array();
  
  $downtime_blocks_array = array();
  
  $downtimes = $g_downtimes;
  if (is_null($downtimes)) {
    $downtimes = $stat->getDownTimes($g_si, $g_start * 1000, $g_end * 1000);
    $g_downtimes = $downtimes;
  }
  
  if (! is_null($downtimes)) {
    foreach($downtimes as $downtime) {
      $block = new GraphBlock();
      $block->start_time = $downtime->startTime;
      $block->end_time = $downtime->endTime;
      array_push($downtime_blocks_array, $block);
    }
  }
  
  $downtime_data = new BlockData();
  $downtime_data->name = "Downtime";
  $downtime_data->blocks = $downtime_blocks_array;
  $downtime_data->color="light_grey";
  
  array_push($bda, $downtime_data);
  
  // can add other blocks to dba
  
  return $bda;
}

function pdf_create_graph_data($name, $data, $color)
{
  $size = count($data);
  
  //debug("pdf_create_graph_data:name=$name,size=$size,color=$color");
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

function pdf_draw_graph($name, $gds, $bds=null, $label_func=null)
{
  global $g_start, $g_end, $g_canvas;
  
  if (! hasData($gds)) {
    //debug(" ! Not displaying graph $name because there was no data");
    $graph = $g_canvas->startGraph($name, new Range(0,1), new Range(0,1));
    $graph->drawLegends($gds);
    $graph->setInvalid("No data");
    pdf_draw_invalid($graph);
  } else {
    
    $x_range = new Range($g_start * 1000, $g_end * 1000);
    
    $max_gd = get_largest_data($gds);
    $max_y = $max_gd->max + (0.05 * $max_gd->max);
    
    $y_range = new Range(0, 1);
    $yincrement = .25;
    
    if ($max_y > 0) {
      $yincrement = calcYIncrement($max_y);
      
      $new_max = $yincrement;
      while($new_max < $max_y) {
        $new_max += $yincrement;
      }
    
      $max_y = $new_max;
    
      $y_range = new Range(0, $max_y);
    }
    
    if($label_func) 
      $yincrement = 1;
    
    $graph = $g_canvas->startGraph($name, $x_range, $y_range);
    
    pdf_draw_graph_blocks($graph, $bds);
    
    setup_graph($graph, $name, $x_range, $y_range, $yincrement, true, $label_func);
    
    pdf_draw_graph_lines($graph, $gds);
    
    $graph->drawLegends($gds, $bds);
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

function setup_graph($graph, $title, $x_range, $y_range, $yincrement, $displayYLabels=true, $label_func=null)
{
  global $majorTicks, $minorTicks;
  
  //debug("setup_graph:title={$title},valid={$graph->valid},x_range=$x_range,y_range=$y_range,yincrement=$yincrement");
  
  $graph->drawTitle("black");

  $graph->drawGridLines($minorTicks, $yincrement, "light_grey");

  $graph->drawGridLines($majorTicks, $yincrement/2, "med_grey");

  $graph->drawBorder("dark_grey");

  if ($displayYLabels)
    $graph->drawYGridLabels($yincrement, $label_func);

  $graph->drawXGridLabels($majorTicks, "formatTime");
}

function pdf_draw_invalid($graph)
{
  global $g_canvas;
  
  $graph->drawTitle("black");
  $graph->drawBorder("dark_grey");
}

function pdf_draw_graph_lines($graph, $gds)
{
  //$gds = array_reverse($gds);

  foreach($gds as $gd) {
    if (sizeof($gd->dataLine) > 0)
      $graph->drawLineGraph($gd->dataLine, $gd->color, 1);
  }
}

function pdf_draw_graph_blocks($graph, $bds)
{
  foreach($bds as $blockdata) {
    //debug("pdf_draw_graph_blocks:$blockdata");
    $graph->drawGraphBlocks($blockdata, 1);
  }
}

function pdf_draw_graphs($mPage)
{
  global $g_si, $g_label, $g_canvas;

  foreach ($mPage->getMeterSections() as $section) {
    $graphs = $section->getMeterGraphs();

    $g_canvas->writeSection("Server Graphs: $g_label : " . $section->name);

    $g_canvas->graph_padding_x = 25;
    $g_canvas->allocateGraphSpace(3,2);

    foreach ($graphs as $graphData) {
      $meterNames = $graphData->getMeterNames();
      //debug("Working on graph " . $graphData->getName() . " with " . count($meterNames) . " meters");
      pdf_stat_graph($graphData->getName(), $meterNames, $g_si);
    }
  }
}

function pdf_heap_dump()
{
  global $g_canvas;
  
  $g_canvas->writeSection("Heap Dump");
  
  $dump = pdf_load_json_dump("Resin|HeapDump");
  if (! $dump) {
    $g_canvas->setTextFont();
    $g_canvas->newLine();
    $g_canvas->writeTextLineIndent(20, "A heap dump was not generated during the selected timeframe.");
    return;
  }
  
  $heap =& $dump["heap"];
  if (! $heap || ! sizeof($heap)) {
    $g_canvas->setTextFont();
    $g_canvas->newLine();
    $g_canvas->writeTextLineIndent(20, "A heap dump was not generated during the selected timeframe.");
    return;
  }
    
  $timestamp = create_timestamp($dump);
  $g_canvas->setFont("Courier-Bold", "8");
  $g_canvas->writeTextLine("Timestamp: $timestamp");
    
  $primitive_filter = "/^(byte|short|int|long|float|double|boolean|char)\[\]/";
  $java_filter = "/^(java|javax|sun|com\.sun)\./";
  $caucho_filter = "/^com\.caucho\./";
  $classloader_filter = "/classloader/i";

  pdf_selected_heap_dump($heap, "Top Classes by Memory Usage", 59);
  
  $primitive_heap = preg_grep_keys($primitive_filter, $heap);
  pdf_selected_heap_dump($primitive_heap, "Primitive Memory Usage ", 50);
  
  $classloader_heap = preg_grep_keys($classloader_filter, $heap);
  pdf_selected_heap_dump($classloader_heap, "ClassLoader Memory Usage ", 50);
  
  $user_heap = preg_grep_keys($primitive_filter, $heap, 1);
  $user_heap = preg_grep_keys($java_filter, $user_heap, 1);
  $user_heap = preg_grep_keys($caucho_filter, $user_heap, 1);
  pdf_selected_heap_dump($user_heap, "User Memory Usage ", 50);
}

function pdf_selected_heap_dump($heap, $title, $max)
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
    $g_canvas->writeTextColumn($cols[1], 'l', pdf_size($value["descendant"]));
    $g_canvas->writeTextColumn($cols[2], 'l', pdf_size($value["size"]));
    $g_canvas->writeTextColumn($cols[3], 'l', $value["count"]);
    $g_canvas->newLine();
  }
}

function heap_descendant_cmp($a, $b)
{
  return $a->descendant - $b->descendant;
}

function pdf_size($size)
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

function preg_grep_keys($regexp, &$values, $invert=false)
{
  $selected = array();

  foreach ($values as $name => $value) {
    $match = false;
    if (preg_match($regexp, $name))
      $match = true;
    if (($match && ! $invert) || (! $match && $invert))
      $selected[$name] = $value;
  }

  return $selected;
}

function create_timestamp($dump) 
{
  global $g_end_unadjusted;
  
  $timestamp = $dump["timestamp"]/1000;
  if ($timestamp) {
    $age = round($g_end_unadjusted - $timestamp);
    return date("Y-m-d H:i", $timestamp) . " (${age}s old)";
  } else {
    return $dump["create_time"]; // for bc
  }
}

function pdf_profile()
{
  global $g_canvas;
  
  $g_canvas->writeSection("CPU Profile");
  
  $profile = pdf_load_json_dump("Resin|Profile");
  if (! $profile || intval($profile["ticks"]) == 0) {
    $g_canvas->setTextFont();
    $g_canvas->newLine();
    $g_canvas->writeTextLineIndent(20, "A CPU profile was not generated during the selected timeframe.");
    return;
  }
  
  $timestamp = create_timestamp($profile);
  if (! $timestamp) { 
    $timestamp = date("Y-m-d H:i", $profile["end_time"] / 1000); // for bc
  }
  
  $g_canvas->setFont("Courier-Bold", "8");
  $g_canvas->writeTextLine("Timestamp: $timestamp");
  $g_canvas->newLine();
      
  $col1 = 70;
  $col2 = 400;

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

  pdf_profile_section("Active Threads",
                            $profile,
                            "admin_thread_active");
                            
  pdf_profile_section("All Threads", $profile);
}

function pdf_profile_section($name, $profile, $filter=null)
{
  global $g_canvas;
  
  $g_canvas->writeSubSection($name);

  $g_canvas->setDataFont();
  
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
    $g_canvas->setTextFont();
    $g_canvas->newLine();
    $g_canvas->writeTextLineIndent(20, "No Data");
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

    $stack = pdf_stack($entry, $max_stack);

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

function pdf_stack(&$profile_entry, $max)
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

function pdf_thread_dump()
{
  global $g_canvas;

  $g_canvas->writeSection("Thread Dump");
  
  $dump = pdf_load_json_dump("Resin|ThreadDump");
  if (! $dump) {
    $g_canvas->setTextFont();
    $g_canvas->newLine();
    $g_canvas->writeTextLineIndent(20, "A thread dump was not generated during the selected timeframe.");
    return;
  }
  
  $timestamp = create_timestamp($dump);
  $g_canvas->setFont("Courier-Bold", "8");
  $g_canvas->writeTextLine("Timestamp: $timestamp");
  $g_canvas->newLine();
    
  $entries =& $dump["thread_dump"];

  pdf_analyze_thread_dump($entries);

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
    $i = pdf_shared_entries($i, &$entries, $g_canvas);
    
    $stack = pdf_thread_stack($entry, $max_stack);

    $g_canvas->setDataFont(7);
    $g_canvas->writeTextWrapIndent(20, $stack);
  }
}

function pdf_analyze_thread_dump(&$entries)
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

function pdf_thread_stack(&$thread_entry, $max)
{
  $stack =& $thread_entry["stack"];
  $monitors = $thread_entry["monitors"];

  $string = "";

  for ($i = 0; $i < $max && $stack[$i]; $i++) {
    $stack_entry = $stack[$i];

    $string .= $stack_entry["class"] . "." . $stack_entry["method"];

    if ($stack_entry["file"]) {
      $string .= " (" . $stack_entry["file"] . ":" . $stack_entry["line"] . ")";
    }

    $string .= "\n";

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

function pdf_shared_entries($i, &$entries, $g_canvas)
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

function pdf_write_log()
{
  global $g_canvas, $g_end, $g_start;
  
  $g_canvas->writeSection("Logs");
  
  pdf_log_messages(null,
                   null,
                   true,
                   $g_start, $g_end,
                   -1);
}

function pdf_jmx_dump()
{
  global $g_canvas, $g_jmx_dump, $g_jmx_dump_time;
  
  $g_canvas->writeSection("JMX Dump");
  
  if (!$g_jmx_dump) {
    $g_canvas->setTextFont();
    $g_canvas->newLine();
    $g_canvas->writeTextLineIndent(20, "A JMX dump was not generated during the selected timeframe.");
    return;
  }
  
  $g_canvas->setFont("Courier-Bold", "8");
  $g_canvas->writeTextLine("Timestamp: $g_jmx_dump_time");
  $g_canvas->newLine();
  
  ksort($entries);
  
  $last_domain;
  $last_domain_id;
  
  $last_mbean;

  foreach ($g_jmx_dump as $name => &$values) {
    
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
    pdf_jmx_attributes($g_canvas, $values);
    $g_canvas->newLine();
  }
}

function pdf_jmx_attributes($g_canvas, &$values)
{
  ksort($values);

  $col1 = 180;
  $col2 = $g_canvas->getLineWidth() - $col1 - 20;
  
  foreach ($values as $key => $value) {
    $g_canvas->writeTextColumn(10, 'l', "");
    $g_canvas->writeTextColumn($col1, 'l', $key);
    $g_canvas->writeTextColumn($col2, 'l', pdf_attribute_value($value));
    $g_canvas->newLine();
  }
}

function pdf_attribute_value($value, $depth = 0)
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
      
      if (is_string($key) && $key == "java_class") {
        continue;
      }
        
      if ($i++ != 0 || $depth > 0 && sizeof($value) > 1) {
        $v .= "\n";
      }
      
      for ($j = 0; $j < $depth + 1; $j++) {
        $v .= " ";
      }
        
      if (is_integer($key)) {
        $v .= pdf_attribute_value($sub_value, $depth + 2);
      }
      else {
        $v .= $key . " => " . pdf_attribute_value($sub_value, $depth + 2);
      }
    }
    
    $v .= "  }";
    
    return $v;
  }
  else {
    return $value;
  }
}

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

function pdf_sort_host($a, $b)
{
  return strcmp($a['URL'], $b['URL']);
}

function pdf_sort_webapp($a, $b)
{
  return strcmp($a['ContextPath'], $b['ContextPath']);
}

?>
