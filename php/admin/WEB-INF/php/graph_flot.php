<?php

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

function stat_graph_regexp($canvas, $width, $height,
                           $start, $end, $pattern, $alt, 
                           $legend = "bottom",
                           $mbean_server = null, $ticks = null, $title = null)
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
             $legend, $mbean_server, $ticks, $title);
}

function stat_graph($canvas, $width, $height, $start, $end, $names, $alt, 
                    $legend = "bottom", $mbean_server = null, $ticks = null, $title = null)
{
  global $g_mbean_server;
  global $g_colors;

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

  $date = new DateTime();
  $tz_offset = $date->getOffset() * 1000;
  
	#echo "<span title='${alt}'>\n";

  if ($legend == "none") {
  	echo "<div id='${canvas}-link' style='width:${width}px;cursor:pointer'>\n";
		echo " <div id='${canvas}-thumb-title' style='width:100%;font-size:1em;text-align:center'>${title} <img src='images/maximize.png' alt='maximize'/></div>\n";
  	echo " <div id='${canvas}-thumb-plot' style='width:${width}px;height:${height}px;'></div>\n";
    echo "</div>\n";
  	  }
  else if ($legend == "right") {
  	echo "<div id='${canvas}-link' style='cursor:pointer;display:inline-block;'>\n";
		echo " <div id='${canvas}-thumb-title' style='width:100%;font-size:1em;text-align:center'>${title} <img src='images/maximize.png' alt='maximize'/></div>\n";
  	echo " <div id='${canvas}-thumb-plot' style='float:left;width:${width}px;height:${height}px;'></div>\n";
    echo " <div id='${canvas}-thumb-legend' style='float:right;font-size:.75em;'></div>\n";
    echo "</div>\n";
	}
  else {
  	echo "<div id='${canvas}-link' style='width:${width}px;cursor:pointer'>\n";
		echo " <div id='${canvas}-thumb-title' style='width:100%;font-size:1em;text-align:center'>${title} <img src='images/maximize.png' alt='maximize'/></div>\n";
  	echo " <div id='${canvas}-thumb-plot' style='width:${width}px;height:${height}px;'></div>\n";
    echo " <div id='${canvas}-thumb-legend' style='width:100%;font-size:.75em;'></div>\n";
    echo "</div>\n";
  }
	
  echo "<div style='display:none'>\n";
  echo " <div id='${canvas}-full-container' style='text-align:center;display:inline-block;'>\n";
	echo "  <div id='${canvas}-full-title' style='width:100%;font-size:1.5em;text-align:center;margin-bottom:.5em;'>${title}</div>\n";
 	echo "  <div id='${canvas}-full-plot'></div>\n";
  echo "  <div id='${canvas}-full-legend' style='display:inline-block;text-align:left;font-size:1.25em;margin-top:1em;'></div>\n";
  echo " </div>\n";
  echo "</div>\n";
  
  #echo "</span>\n";

  echo "<script id='$canvas-script' language='javascript' type='text/javascript'>\n";
  echo "<!-- \n";
  echo '$(function () {' . "\n";

  $index = null;

  echo "var thumb_graphs = [];\n";
  echo "var full_graphs = [];\n";

  $color_counter = 0;
  $counter = 0;
  foreach ($names as $name) {
  	
  	echo "// START $name\n";
  	
    $values = $stat->statisticsData($name, $start * 1000, $end * 1000, $step * 1000);
    
    if ($index === null && preg_match("/^(\d+)\|/", $name, $name_values)) {
      $index = $name_values[1];
    }

    $color = $g_colors[$color_counter++];
    if ($color_counter > count($g_colors))
    	$color_counter = 0;
    
    echo "values = [\n";
    
    if (sizeof($values) > 1) {
    	$skipped_last = false;
	    $prev_value = null;
	    foreach ($values as $value) {
	    	if ($value->value != $prev_value) {
	      	$time = $value->time + $tz_offset;
	      	echo "[${time}, ${value->value}],\n";
	      	$prev_value = $value->value;
	      	$skipped_last = false;
	  		} else {
	  			$skipped_last = true;
	  		}
	    }

	    if ($skipped_last) {
		    $last_value = $values[sizeof($values)-1];
	    	$last_time = $last_value->time + $tz_offset;
	    	echo "[${last_time}, ${last_value->value}],\n";
    	}
    }
    
    echo "];\n";
    
		echo "thumb_graphs[${counter}] = { label : '" . preg_replace("/\s/", "&nbsp;", $name) . "', data : values, color: \"${color}\", lines: { lineWidth: 2 } };\n";
		echo "full_graphs[${counter*2}] = { label : '" . preg_replace("/\s/", "&nbsp;", $name) . "', data : values, color: \"${color}\", lines: { lineWidth: 2 }, points: { radius: 2, symbol: \"circle\" } };\n";
		
    if (sizeof($values) > 1) {
    	# don't generate a baseline unless we have at least half as many historical samples
      $baseline = $stat->getBaseline($name, $start * 1000, $end * 1000, (sizeof($values)/2));
      
	    if ($baseline) {
	    	$baseline_name = preg_replace("/\s/", "&nbsp;", "${name}|Baseline|${baseline->desc}");
	    	$baseline_value = round($baseline->value);
	    	
    		echo "baseline_values = [\n";
    		
    		$skipped_last = false;
    		$prev_value = null;
		    foreach ($values as $value) {
		    	if ($value->value != $prev_value) {
		      	$time = $value->time + $tz_offset;
		      	echo "[${time}, ${baseline_value}],\n";
		      	$prev_value = $value->value;
	      		$skipped_last = false;
	  			} else {
	  				$skipped_last = true;
	  			}
		    }
		
	    	if ($skipped_last) {
		    	$last_value = $values[sizeof($values)-1];
	    		$last_time = $last_value->time + $tz_offset;
	    		echo "[${last_time}, ${baseline_value}],\n";
	    	}
				
    		echo "];\n";
		    echo "full_graphs[${counter*2+1}] = { label : '${baseline_name}', data : baseline_values, color: \"${color}\", lines: { lineWidth: 1 }, points: { radius: 1, symbol: \"square\" } };\n";
			} else {
				echo "baseline_values = [];\n";
				echo "full_graphs[${counter*2+1}] = { data : baseline_values, color: \"${color}\", lines: { lineWidth: 1 } };\n";
			}
		}
    
		$counter++;
		
		echo "// END $name\n";
    echo "\n";
  }
  
  
  $ticks_var = str_replace("-", "_", "ticks_$canvas");
  
  echo "\n";
  
  echo "function tickFormatter(val, axis) {\n";
  if ($ticks) {
	  echo "  ticks = [];\n"
	  for ($i=0; $i<sizeof($ticks); $i++) {
	  	echo "  ticks[$i] = '" . $ticks[$i] . "';\n";
	  }
  	echo "  if (val >= ticks.length || val < 0 || val % 1 > 0)\n";
    echo "    return '';\n";
    echo "  else\n"
    echo "    return ticks[val];\n";
  } else {
	  echo "  if (val >= 1e9)\n";
	  echo "    return (val / 1e9).toFixed(1) + 'G';\n";
	  echo "  if (val >= 1e6)\n";
	  echo "    return (val / 1e6).toFixed(1) + 'M';\n";
	  echo "  if (val >= 1e3)\n";
	  echo "    return (val / 1e3).toFixed(1) + 'k';\n";
	  echo "  return val.toFixed(axis.tickDecimals);\n";
	}
	echo "}\n\n";  
	
	echo "function showTooltip(item, contents) {\n";
	echo "  $('<div id=\"tooltip\">' + contents + '</div>').css({\n";
	echo "    position: 'absolute',\n";
	echo "    display: 'none',\n";
	echo "    top: item.pageY + 10,\n";
	echo "    left: item.pageX + 10,\n";
	echo "    border: '1px solid #4c2004',\n";
	echo "    padding: '2px',\n";
	echo "    'z-index': '999999',\n";
	echo "    'background-color': '#f4f4f4'\n";
	echo "  }).appendTo(\"body\").fadeIn(200);\n";
	echo "}\n\n";
  
  echo "$.plot($(\"#${canvas}-thumb-plot\"), thumb_graphs, {\n";
  echo "  xaxis: { mode:\"time\" },\n";
  echo "  yaxis: {\n";
  echo "    tickFormatter: tickFormatter \n";
  echo "  },\n";
  echo "  legend: { container: \"#${canvas}-thumb-legend\" },\n";
  echo "});\n";
  
  echo "\n";

  echo "$(function() {\n";
  echo "  $('#${canvas}-link').colorbox({\n"; 
  echo "    width:'85%', height:'85%', inline:true, scrolling:true, href:'#${canvas}-full-container', onComplete:function() {\n";
  echo"				$(\"#${canvas}-full-container\").width('90%');\n";
  echo"				$(\"#${canvas}-full-container\").height('95%');\n";
  echo"				$(\"#${canvas}-full-plot\").width('100%');\n";
  echo"				$(\"#${canvas}-full-plot\").height('75%');\n";
  echo "			$.plot($(\"#${canvas}-full-plot\"), full_graphs, {\n";
  echo "  			xaxis: { mode:\"time\" },\n";
  echo "  			yaxis: {\n";
  echo "    			tickFormatter: tickFormatter \n";
  echo "  			},\n";
  echo "  			legend: { container: \"#${canvas}-full-legend\", noColumns: 2 },\n";
  echo "  			grid: { hoverable: true, autoHighlight: true, interactive: true },\n";
  echo "  			pan: { interactive: true },\n";
  echo "  			zoom: { interactive: true },\n";
  echo "			});\n";
	echo "			var previousPoint = null;\n";
	echo "			$(\"#${canvas}-full-plot\").bind(\"plothover\", function (event, pos, item) {\n";
	echo "				if (item) {\n";
	echo "					if (previousPoint != item.dataIndex) {\n";
	echo "						previousPoint = item.dataIndex;\n";
	echo "						$(\"#tooltip\").remove();\n";
	echo "						showTooltip(item, item.series.label + \": \" + item.series.yaxis.tickFormatter(item.datapoint[1], item.series.yaxis) + \" at \" + item.series.xaxis.tickFormatter(item.datapoint[0], item.series.xaxis));\n";
	echo "					}\n";
  echo "				}\n";
	echo "				else {\n";
	echo "					$(\"#tooltip\").remove();\n";
	echo "					previousPoint = null;\n";            
	echo "				}\n";
	echo "			});\n";
  echo "		}\n";
  echo "  })\n"; 
  echo "});\n";
  
	echo "});\n";
  
	echo " -->\n";
  echo "</script>";
}
