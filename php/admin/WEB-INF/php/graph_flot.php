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
                           $start, $end, $pattern, $alt, 
                           $legend = "bottom",
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

  stat_graph($canvas, $width, $height, $start, $end, $names, $alt, 
             $legend, $mbean_server, $ticks);
}

function stat_graph($canvas, $width, $height, $start, $end, $names, $alt, 
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

	#echo "<span title='${alt}'>\n";
  	
  if ($legend == "none") {
    echo "<div id='$canvas-plot' style='width:${width}px;height:${height}px'></div>\n";
  }
  else if ($legend == "right") {
    echo "<table border='0' cellspacing='0' cellpadding='0' summary='asdf'>\n";
    echo "<tr><td>";
    echo "<div id='$canvas-plot' style='width:${width}px;height:${height}px'></div>\n";
    echo "</td><td>";

    echo "<div id='${canvas}-legend' style='width:${width}px;height:${label_height}px'></div>\n";
    echo "</td></tr></table>\n";
  }
  else {
    echo "<div id='$canvas-plot' style='width:${width}px;height:${height}px'></div>\n";
    echo "<div id='${canvas}-legend' style='width:${width}px;height:${label_height}px'></div>\n";
  }
  
  #echo "</span>\n";

  echo "<script id='$canvas-script' language='javascript' type='text/javascript'>\n";
  echo "<!-- \n";
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

    echo "graphs[$i] = { label : '${name}', data : values, points: { radius: 2, symbol: \"circle\" } };\n";

    $i++;
  }
  
  $labelWidth = 40;
  
  $ticks_var = str_replace("-", "_", "ticks_$canvas");
  
  echo "${ticks_var} = [];\n"
  if ($ticks) {
	  $i = 0;
	  foreach ($ticks as $tick) {
	  	echo "${ticks_var}[$i] = '" . $tick . "';\n";
	  	$i++;
	  }
	  $labelWidth = 60;
  }

  echo "$.plot($(\"#${canvas}-plot\"), graphs,\n";
  echo "{\n";
  echo " xaxis: { mode:\"time\" },\n";
  echo " yaxis: {\n";
  echo "  labelWidth: ${labelWidth},\n";
  echo "  tickFormatter: \n";
  echo "   function (val, axis) {\n";
  echo "    if (${ticks_var}.length > 0) {\n";
  echo "     if (val >= ${ticks_var}.length || val < 0 || val % 1 > 0)\n";
  echo "      return '';\n";
  echo "     else\n"
  echo "      return ${ticks_var}[val];\n";
  echo "    }\n"; 
  echo "    else {\n"
  echo "     if (val >= 1e9)\n";
  echo "      return (val / 1e9).toFixed(1) + 'G';\n";
  echo "     if (val >= 1e6)\n";
  echo "      return (val / 1e6).toFixed(1) + 'M';\n";
  echo "     if (val >= 1e3)\n";
  echo "      return (val / 1e3).toFixed(1) + 'k';\n";
  echo "     return val.toFixed(axis.tickDecimals);\n";
  echo "    }\n";
  echo "   }\n";  
  echo " },\n";
  echo " series: { lines: { lineWidth:2 } },\n";
  echo " legend: { container: \"#${canvas}-legend\" },\n";
//  echo " zoom: { interactive: true },\n";
  echo " pan: { interactive: true },\n";
  echo " grid: { hoverable: true, autoHighlight: true },\n";
  echo "});\n";
  
  echo "});\n";
  
  echo "function showTooltip(x, y, contents) {\n";
	echo " $('<div id=\"tooltip\">' + contents + '</div>').css( {\n";
	echo "  position: 'absolute',\n";
	echo "  display: 'none',\n";
	echo "  top: y + 10,\n";
	echo "  left: x + 10,\n";
	echo "  border: '1px solid #4c2004',\n";
	echo "  padding: '2px',\n";
	echo "  'background-color': '#f4f4f4',\n";
	echo " }).appendTo(\"body\").fadeIn(200);\n";
	echo "}\n";
 
	echo "var previousPoint = null;\n";
	echo "$(\"#${canvas}-plot\").bind(\"plothover\", function (event, pos, item) {\n";
	echo " if (item) {\n";
	echo "  if (previousPoint != item.dataIndex) {\n";
	echo "   previousPoint = item.dataIndex;\n";
	echo "   $(\"#tooltip\").remove();\n";
	echo "   showTooltip(item.pageX, item.pageY, item.series.label + \": \" + item.series.yaxis.tickFormatter(item.datapoint[1], item.series.yaxis) + \" at \" + item.series.xaxis.tickFormatter(item.datapoint[0], item.series.xaxis));\n";
	echo "  }\n";
  echo " }\n";
	echo " else {\n";
	echo "  $(\"#tooltip\").remove();\n";
	echo "  previousPoint = null;\n";            
	echo " }\n";
	echo "});\n";
  
  echo " -->\n";
  echo "</script>";
}
