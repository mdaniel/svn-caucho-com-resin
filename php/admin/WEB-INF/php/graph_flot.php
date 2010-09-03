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
                           $start, $end, $pattern, $expand_height = true)
{
  global $g_mbean_server;
  global $g_label_width;

  if (! $g_mbean_server)
    return;

  $stat = $g_mbean_server->lookup("resin:type=StatService");

  if (! $stat)
    return;

  $full_names = $stat->statisticsNames();

  $names = preg_grep($pattern, $full_names);
  sort($names);

  stat_graph($canvas, $width, $height, $start, $end, $names, $expand_height);
}

function stat_graph($canvas, $width, $height, $start, $end, $names,
                    $expand_height = true)
{
  global $g_mbean_server;

  if (! $g_mbean_server) {
    return;
  }

  $stat = $g_mbean_server->lookup("resin:type=StatService");

  if (! $stat) {
    return;
  }

  echo "<div id='$canvas' style='width:${width}px;height:${height}px'></div>\n";

  $label_height = 20 * sizeof($names);

  $date = new DateTime();
  $tz_offset = $date->getOffset() * 1000;

  echo "<div id='${canvas}-legend' style='width:${width}px;height:${label_height}px'></div>\n";

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

    echo "graphs[$i] = { label : '${name}', data : values };\n";

    $i++;
  }

  echo '$.plot($("#' . $canvas . '"), graphs,';
  echo '{ ';
  echo 'xaxis: {mode:"time"}, ';
  echo "yaxis: {tickFormatter: function(val, axis) {\n";
  echo "  if (val > 1000000000)\n";
  echo "    return (val / 1000000000).toFixed(1) + 'G';\n";
  echo "  if (val > 1000000)\n";
  echo "    return (val / 1000000).toFixed(1) + 'M';\n";
  echo "  if (val > 1000)\n";
  echo "    return (val / 1000).toFixed(1) + 'k';\n";
  echo "  return val.toFixed(axis.tickDecimals);\n";
  echo '}}, ' . "\n";
  echo 'series: { lines: { lineWidth:1 }}, ';
  echo 'legend: { container: "#' . $canvas . '-legend" }, ';
  echo '});' . "\n";
  
  echo "});\n";
  echo "</script>";
}
