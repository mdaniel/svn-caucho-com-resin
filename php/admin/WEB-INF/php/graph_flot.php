<?php

### All temporal values are expressed in terms of SECONDS unless otherwise named ###

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

$g_periods = array(2 * 7 * 24 * 60 * 60	=> "2 Weeks",
                   1 * 7 * 24 * 60 * 60 => "1 Week",
                   5 * 24 * 60 * 60 => "5 Days",
                   3 * 24 * 60 * 60 => "3 Days",
                   1 * 24 * 60 * 60 => "1 Day",
                   12 * 60 * 60 => "12 Hours",
                   6 * 60 * 60 => "6 Hours",
                   3 * 60 * 60 => "3 Hours",
                   1 * 60 * 60 => "1 Hour");

class GraphTail {
  private $canvas;
  private $names;
  private $end;
  private $period;

  function GraphTail($canvas, $names, $end, $period)
  {
    $this->canvas = $canvas;
    $this->names = $names;
    $this->end = $end;
    $this->period = $period;
  }

  function execute()
  {
  	$stat = get_stats_service();
    if (! $stat) {
      return;
    }

    echo "<script language='javascript' type='text/javascript'>\n";
    echo "<!-- \n";
    echo "var resin_graphs;\n";
    echo "if (! resin_graphs) { resin_graphs = new Array(); }\n";
    echo "resin_graphs.push({"
    echo "canvas: '${this->canvas}',"
    echo "names: [";
    foreach ($this->names as $name) {
      echo "'${name}',";
    }
    echo "],";
    echo "end: ${this->end},";
    echo "period: ${this->period},";
    echo "});\n";

    create_graph_timeout();
    echo "--> \n";
    echo "</script>\n";
    
    stat_graph_script($stat, $this->canvas, $this->names, $this->end, $this->period);
  }
}

function get_stats_service($mbean_server = null)
{
  global $g_mbean_server;

  if (! $mbean_server) {
    $mbean_server = $g_mbean_server;
  }

  if (! $mbean_server) {
    return;
  }

  return $mbean_server->lookup("resin:type=StatService");
}

function create_graph_timeout()
{
  global $g_static_graphs;
  static $is_graph_timeout;

  if ($g_static_graphs)
   return;
  
  if ($is_graph_timeout)
    return;

  $is_graph_timeout = true;

  echo "function resin_graph_update() {\n";
  echo "  var str = '[';\n";
  echo "  for (i in resin_graphs) {\n";
  echo "    var graph = resin_graphs[i];\n";
  echo '    str += "{\\"canvas\\":\\"" + graph.canvas + "\\",";';
  echo "    str += \"\\\"names\\\":[\";\n";
  echo "    for (j in graph.names) {\n";
  echo '      str += "\\"" + graph.names[j] + "\\",";';
  echo "    }\n";
  echo "    str += \"],\";\n";
  echo "    str += \"\\\"end\\\":\" + graph.end + \",\";";
  echo "    str += \"\\\"period\\\":\" + graph.period + \",\";";
  echo "    str += \"},\";";
  echo "  }\n";
  echo " str += ']';\n";
  echo "\n";
  echo '  $.ajax({type:"POST", url:"rest.php?q=graph_ext", data:str,';
  echo '      success:function(canvasHtml) {';
  echo '        $(document).append(canvasHtml);';// updateGraphs(graphDiv, canvasHtml);
  echo '        setTimeout("resin_graph_update();", 60000);';
  echo '      },';
  echo '      contentType:"unknown/type"});';

  echo "}\n";
  
  echo "resin_graph_timeout = setTimeout(\"resin_graph_update();\", 60000);\n";
}

function stat_graph_regexp($canvas, $width, $height,
                           $end, $period, $pattern, $alt, 
                           $legend = "bottom",
                           $mbean_server = null, $ticks = null, 
                           $title = null, $now_label="Now")
{
	$stat = get_stats_service($mbean_server);
	if (! $stat) {
		return;
	}
	
  $full_names = $stat->statisticsNames();

  $names = preg_grep($pattern, $full_names);
  sort($names);

  stat_graph_div($canvas, $width, $height, $alt, $legend, $title, $now_label);

  $tail = new GraphTail($canvas, $names, $end, $period);

  display_add_tail($tail);
  
  // stat_graph_script($stat, $canvas, $names, $period);
}

function stat_graph($canvas, $width, $height, $end, $period, $names, $alt, 
                    $legend = "bottom", $mbean_server = null, $ticks = null,
                    $title = null, $now_label="Now")
{
	$stat = get_stats_service($mbean_server);
	if (! $stat) {
		return;
	}
		
  stat_graph_div($canvas, $width, $height, $alt, $legend, $title, $now_label);

  $tail = new GraphTail($canvas, $names, $end, $period);

  display_add_tail($tail);

  // stat_graph_script($stat, $canvas, $names, $period);
}                 

function stat_graph_div($canvas, $width, $height, $alt, $legend, $title, 
												$now_label="Now")
{
	global $g_periods;
	
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
  echo "  <div style='text-align:center;display:inline-block;width:100%;'>\n";
	echo "   <div id='${canvas}-full-title' style='float:center;font-size:1.5em;text-align:center;margin-bottom:.5em;'>${title}</div>\n";
	echo "  </div>\n";
	echo "  <div id='${canvas}-full-plot'></div>\n";
  echo "  <div id='${canvas}-full-legend' style='display:inline-block;text-align:left;font-size:1.25em;margin-top:1em;'></div>\n";
  echo " </div>\n";
  echo "</div>\n";
  
  #echo "</span>\n";
}

function stat_graph_script($stat, $canvas, $names, $end, $period)
{
  global $g_colors;

  $date = new DateTime();
  $tz_offset_ms = $date->getOffset() * 1000;
  
  $period_ms = $period * 1000;

//  echo "<script id='${canvas}-script' language='javascript' type='text/javascript'>\n";
  echo "<script language='javascript' type='text/javascript'>\n";
  echo "<!-- \n";
  echo "\$(function () {\n";

  $index = null;
  
  echo "var thumb_graphs = [];\n";
  echo "var full_graphs = [];\n";

  $color_counter = 0;
  $counter = 0;
  
  $data_end_ms = ($end * 1000);
  $data_start_ms = $data_end_ms - $period_ms;
  
  $x_max_ms = 0;
  
  foreach ($names as $name) {
    echo "// START $name\n";
    
    $values = $stat->statisticsData($name, $data_start_ms, $data_end_ms, 1);
    
    if ($index === null && preg_match("/^(\d+)\|/", $name, $name_values)) {
      $index = $name_values[1];
    }

    $color = $g_colors[$color_counter++];
    if ($color_counter == sizeof($g_colors))
    	$color_counter = 0;
    
    echo "values = [\n";
    
    $size = sizeof($values);
    if ($size > 1) {
      echo "[" . ($values[0]->time + $tz_offset_ms) . ", " . $values[0]->value . "],\n";

      for ($i=1; $i<sizeof($values)-1; $i++) {
        if ($values[$i]->value != $values[$i-1]->value) {
          echo "[" . ($values[$i]->time + $tz_offset_ms) . ", " . $values[$i]->value . "],\n";
        }
      }
			
      echo "[" . ($values[$size-1]->time + $tz_offset_ms) . ", " . $values[$size-1]->value . "]\n";
    }
    
    echo "];\n";
    
    echo "thumb_graphs[${counter}] = { label : '" . preg_replace("/\s/", "&nbsp;", $name) . "', data : values, color: \"${color}\", lines: { lineWidth: 2 } };\n";
    echo "full_graphs[${counter*2}] = { label : '" . preg_replace("/\s/", "&nbsp;", $name) . "', data : values, color: \"${color}\", lines: { lineWidth: 2 }, points: { radius: 2, symbol: \"circle\" } };\n";
		
    $has_baseline = false;
    if ($size > 1) {
      # don't generate a baseline unless we have at least half as many historical samples
      $baseline = $stat->getBaseline($name, $data_start_ms, $data_end_ms, ($size/2));
      
      if ($baseline) {
        $baseline_name = preg_replace("/\s/", "&nbsp;", "${name}|Baseline|${baseline->desc}");
        $baseline_value = round($baseline->value);
	    	
        echo "baseline_values = [\n";
        echo "[" . ($values[0]->time + $tz_offset_ms) . ", " . $baseline_value . "],\n";
  
        for ($i=1; $i<sizeof($values)-1; $i++) {
          if ($values[$i]->value != $values[$i-1]->value) {
            echo "[" . ($values[$i]->time + $tz_offset_ms) . ", " . $baseline_value . "],\n";
          }
        }
					
        echo "[" . ($values[$size-1]->time + $tz_offset_ms) . ", " . $baseline_value . "]\n";
        echo "];\n";

        echo "full_graphs[${counter*2+1}] = { label : '${baseline_name}', data : baseline_values, color: color_baseline($.color.parse(\"${color}\")).toString(), lines: { lineWidth: 1 }, points: { radius: 1, symbol: \"square\" } };\n";
      $has_baseline = true;
      }
    }
	 	
    if (! $has_baseline) {
      echo "baseline_values = [];\n"
      echo "full_graphs[${counter*2+1}] = { data : baseline_values, color: \"${color}\", lines: { lineWidth: 1 } };\n";
    }
    
    if ($values[sizeof($values)-1]->time > $x_max_ms) {
    	$x_max_ms = $values[sizeof($values)-1]->time;
    }
    
    $counter++;
		
    echo "// END $name\n";
    echo "\n";
  }

  # Note: using the max data values makes the graphs look better does not
  # show missing data which is particularly bad for postmortem graphs
  
  #if ($x_max_ms == 0) {
  #	$x_max_ms = $data_end_ms;
  #}
  	
  #$x_max_ms = $x_max_ms + $tz_offset_ms;
  
  $x_max_ms = $data_end_ms + $tz_offset_ms;
  $x_min_ms = $x_max_ms - $period_ms;
  
  echo "var x_min_ms = ${x_min_ms}\n";
  echo "var x_max_ms = ${x_max_ms}\n";
  
  echo "\n";
  
  echo "function tickFormatter(val, axis) {\n";
  if ($ticks) {
    echo "  var ticks = [];\n"
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
  
  echo "var thumb_options = {\n";
  echo "	xaxis: { mode:\"time\", min: x_min_ms, max: x_max_ms },\n";
  echo "  yaxis: {\n";
  echo "		tickFormatter: tickFormatter \n";
  echo "  },\n";
  echo "  legend: { container: \"#${canvas}-thumb-legend\" }\n";
  echo "};\n\n";
    
  echo "var thumb_plot = $.plot(\"#${canvas}-thumb-plot\", thumb_graphs, thumb_options);\n\n";
  
  echo "$(\"#${canvas}-thumb-plot\").bind(\"updateRange\", function (e, start, end) {\n";
  echo "	var end_ms = (end * 1000) + ${tz_offset_ms};\n"
  echo "	var start_ms = (start * 1000) + ${tz_offset_ms};\n"
  echo "	thumb_plot = $.plot($(this), thumb_graphs, $.extend(true, {}, thumb_options, {\n"; 
  echo "		xaxis: { min: start_ms, max: end_ms }\n"; 
  echo "	}))\n";
  echo "});\n\n";
  
  echo "$(function() {\n";
  echo "  $('#${canvas}-link').colorbox({\n"; 
  echo "    width:'85%', height:'85%', inline:true, scrolling:false, href:'#${canvas}-full-container', onComplete:function() {\n";
  echo "			$(\"#${canvas}-full-container\").width('95%');\n";
  echo "			$(\"#${canvas}-full-container\").height('95%');\n";
  echo "			$(\"#${canvas}-full-plot\").width('100%');\n";
  echo "			$(\"#${canvas}-full-plot\").height('70%');\n\n";
  
  echo "			var placeholder = $(\"#${canvas}-full-plot\");\n\n";
  
  echo "			var full_options = {\n";
  echo "  			xaxis: { mode:\"time\", min: x_min_ms, max: x_max_ms },\n";
  echo "  			yaxis: {\n";
  echo "    			tickFormatter: tickFormatter \n";
  echo "  			},\n";
  echo "  			legend: { container: \"#${canvas}-full-legend\", noColumns: 2 },\n";
  echo "				grid: { hoverable: true, autoHighlight: true, interactive: true },\n";
  echo "				selection: { mode: \"x\" }\n"
  echo "			};\n\n";

  echo "			var plot = $.plot(placeholder, full_graphs, full_options);\n\n";
  
  echo "			placeholder.bind(\"plotselected\", function (event, ranges) {\n"; 
  echo "				plot = $.plot(placeholder, full_graphs, $.extend(true, {}, full_options, {\n"; 
  echo "					xaxis: { min: ranges.xaxis.from, max: ranges.xaxis.to }\n"; 
  echo "				}))\n";
  echo "			});\n\n";
  
  echo "			var previousPoint = null;\n";
  echo "			placeholder.bind(\"plothover\", function (event, pos, item) {\n";
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
  echo "			});\n\n";
  echo "		}\n";
  echo "  })\n"; 
  echo "});\n";
  
  echo "});\n";
  
  echo " -->\n";
  echo "</script>";
}
