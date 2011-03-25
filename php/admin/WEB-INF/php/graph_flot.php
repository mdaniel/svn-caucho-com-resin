<?php

// saturated primary/secondary
// 0    #ff0000  - red
// 30   #ff8000  - orange
// 60   #ffff00  - yellow
// 90   #80ff00  - chartreuse
// 120  #00ff00  - green
// 150  #00ff80  - spring green
// 180  #00ffff  - cyan
// 210  #0080ff  - azure
// 240  #0000ff  - blue
// 270  #8000ff  - indigo
// 300  #ff00ff  - magenta
// 330  #ff0080  - rose

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

$g_label_width = 180;

function stat_graph_regexp($canvas, $width, $height,
                           $start, $end, $pattern, $label = "bottom",
                           $mbean_server = null, $ticks = null)
{
  global $g_mbean_server;

  if (! $mbean_server) {
    $mbean_server = $g_mbean_server;
  }

  if (! $mbean_server) {
    return;
  }

  $stat = $mbean_server->lookup("resin:type=StatService");

  if (! $stat) {
    return;
  }

  $full_names = $stat->statisticsNames();

  $names = preg_grep($pattern, $full_names);
  sort($names);

  stat_graph($canvas, $width, $height, $start, $end, $names,
             $label, $mbean_server, $ticks);
}

function stat_graph($canvas, $width, $height, $start, $end, $names,
                    $legend = "bottom", $mbean_server = null, $ticks = null)
{
  global $g_mbean_server;

  if (! $mbean_server) {
    $mbean_server = $g_mbean_server;
  }

  if (! $mbean_server) {
    return;
  }

  $stat = $mbean_server->lookup("resin:type=StatService");

  if (! $stat) {
    return;
  }

  $label_height = 20 * sizeof($names);

  $date = new DateTime();
  $tz_offset = $date->getOffset() * 1000;

  if ($legend == "none") {
    echo "<div id='$canvas' style='width:${width}px;height:${height}px'></div>\n";
  }
  else if ($legend == "right") {
    echo "<table border='0' cellspacing='0' cellpadding='0'>\n";
    echo "<tr><td>";
    echo "<div id='$canvas' style='width:${width}px;height:${height}px'></div>\n";
    echo "</td><td>";

    echo "<div id='${canvas}-legend' style='width:${width}px;height:${label_height}px'></div>\n";
    echo "</td></table>\n";
  }
  else {
    echo "<div id='$canvas' style='width:${width}px;height:${height}px'></div>\n";

    echo "<div id='${canvas}-legend' style='width:${width}px;height:${label_height}px'></div>\n";
  }

  echo "<script id='source' language='javascript' type='text/javascript'>\n";
  echo '$(function () {' . "\n";

  $index = null;

  echo 'graphs = [];';

  $i = 0;
  foreach ($names as $name) {
    $values = $stat->statisticsData($name, $start * 1000, $end * 1000,
                                    $step * 1000);

    if ($index === null && preg_match("/^(\d+)\|/", $name, $name_values)) {
      $index = $name_values[1];
    }

    echo 'values = [';

    foreach ($values as $value) {
      $time = $value->time + $tz_offset;
      
      echo "[${time}, ${value->value}],\n";
    }
    
    echo "];\n";

    $name = preg_replace("/\s/", "&nbsp;", $name);

    echo "graphs[$i] = { label : '" . $name . "', data : values };\n";

    $i++;
  }
  
  $labelWidth = 40;
  
  echo "ticks = [];\n"
  if ($ticks) {
	  $i = 0;
	  foreach ($ticks as $tick) {
	  	echo "ticks[$i] = '" . $tick . "';\n";
	  	$i++;
	  }
	  $labelWidth = 60;
  }

  echo '$.plot($("#' . $canvas . '"), graphs,';
  echo '{ ';
  echo 'xaxis: { mode:"time" }, ';
  echo "yaxis: {labelWidth:$labelWidth,tickFormatter: function(val, axis) {\n";
  echo "  if (ticks.length > 0) {\n";
  echo "    if (val >= ticks.length || val < 0 || val % 1 > 0)\n";
  echo "      return '';\n";
  echo "    else\n"
  echo "      return ticks[val];\n";
  echo "  } else {\n"
  echo "    if (val >= 1e9)\n";
  echo "      return (val / 1e9).toFixed(1) + 'G';\n";
  echo "    if (val >= 1e6)\n";
  echo "      return (val / 1e6).toFixed(1) + 'M';\n";
  echo "    if (val >= 1e3)\n";
  echo "      return (val / 1e3).toFixed(1) + 'k';\n";
  echo "    return val.toFixed(axis.tickDecimals);\n";
  echo "  }\n";
  echo "}\n";
  echo "},\n";
  echo 'series: { lines: { lineWidth:1 }}, ';
  echo 'legend: { container: "#' . $canvas . '-legend" }, ';
  echo '});' . "\n";
  
  echo "});\n";
  echo "</script>";
}
