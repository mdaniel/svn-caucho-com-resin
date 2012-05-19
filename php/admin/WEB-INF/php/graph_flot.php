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
                   1 * 60 * 60 => "1 Hour",
                   30 * 60 => "30 Minutes",
                   15 * 60 => "15 Minutes");
                   
$g_time = 0;
$g_offset = 0;
// $g_period = (6 * 60 * 60);
$g_period = (60 * 60);
$g_labels;

class GraphParams {
	public $canvas;
	public $width;
	public $height;
	public $time;
	public $offset;
	public $period;
	public $alt = "Graph";
	public $legend = "bottom";
	public $labels_mbean;
	public $labels;
	public $title = "Graph";
	public $mbean_server;
	
	function GraphParams($canvas, $width, $height, $time = null, $offset = null, $period = null)
	{
		global $g_mbean_server;
		
		global $g_time;
		global $g_offset;
		global $g_period;
		
		$mbean_server = $g_mbean_server;
		
		$this->canvas = $canvas;
		$this->width = $width;
		$this->height = $height;
		
		$this->time = $time;
		$this->offset = $offset;
		$this->period = $period;
		
		if(! isset($this->time)) {
		  $this->time = $g_time;
		}
		
		if(! isset($this->offset)) {
		  $this->offset = $g_offset;
		}
		
		if(! isset($this->period)) {
		  $this->period = $g_period;
		}
	}
}

class GraphTail {
  private $params;
  private $names;

  function GraphTail($params, $names)
  {
    $this->params = $params;
    $this->names = $names;
  }

  function execute()
  {
    global $g_time;
    
  	$stat = get_stats_service();
    if (! $stat) {
      return;
    }

    if ($g_time == 0) {
      echo "<script language='javascript' type='text/javascript'>\n";
      echo "<!-- \n";
      echo "var resin_graphs;\n";
      echo "if (! resin_graphs) { resin_graphs = new Array(); }\n";
      echo "resin_graphs.push({"
      echo "canvas:'${this->params->canvas}',"
      echo "names:[";
      foreach ($this->names as $name) {
        echo "'${name}',";
      }
      echo "],";
      echo "time:'${this->params->time}',\n";
      echo "offset:'${this->params->offset}',\n";
      echo "period:'${this->params->period}',\n";
      echo "});\n";
      
      create_graph_timeout();
      
      echo "--> \n";
      echo "</script>\n";
    }
    
    stat_graph_script($stat, $this->params->canvas, $this->names, 
      $this->params->time, $this->params->offset, $this->params->period);
  }
}

function create_graph_timeout()
{
  static $is_graph_timeout;

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
  echo "    str += \"\\\"time\\\":\\\"\" + graph.time + \"\\\",\";\n";
  echo "    str += \"\\\"offset\\\":\\\"\" + graph.offset + \"\\\",\";\n";
  echo "    str += \"\\\"period\\\":\\\"\" + graph.period + \"\\\",\";\n";
  echo "    str += \"},\";\n";
  echo "  }\n";
  echo "  str += ']';\n\n";
  echo "  $.ajax({type:\"POST\", url:\"rest.php?q=graph_ext\", data:str,\n";
  echo "  	success:function(canvasHtml) {\n";
  echo "    	$(document).append(canvasHtml);\n"; // updateGraphs(graphDiv, canvasHtml);
  echo "    },\n";
  echo "    contentType:\"unknown/type\"});\n";
  echo "}\n\n";
  echo "setInterval(\"resin_graph_update();\", 60000);\n";
}

function stat_graph_regexp($params, $pattern)
{
	$stat = get_stats_service($params->mbean_server);
	if (! $stat) {
		return;
	}
	
  $full_names = $stat->statisticsNames();

  $names = preg_grep($pattern, $full_names);
  
  if (count($names) > 0)
    sort($names);
    
  stat_graph($params, $names);
}

function stat_graph($params, $names)
{
	$stat = get_stats_service($params->mbean_server);
	if (! $stat) {
		return;
	}
	
  stat_graph_div($params);

  $tail = new GraphTail($params, $names);
  display_add_tail($tail);
}                 

function stat_graph_div($params)
{
  global $g_labels;
  
	#echo "<span title='${params->alt}'>\n";
  if ($params->legend == "none") {
    echo "<div id='${params->canvas}-link' style='width:${params->width}px;'>\n";
    echo " <div id='${params->canvas}-thumb-title' style='width:100%;font-size:1em;text-align:center'>${params->title} <img src='images/maximize.png' alt='maximize'/></div>\n";
    echo " <div id='${params->canvas}-thumb-plot' style='width:${params->width}px;height:${params->height}px;'></div>\n";
    echo "</div>\n";
  }
  else if ($params->legend == "right") {
    echo "<div id='${params->canvas}-link' style='display:inline-block;'>\n";
    echo " <div id='${params->canvas}-thumb-title' style='width:100%;font-size:1em;text-align:center'>${params->title} <img src='images/maximize.png' alt='maximize'/></div>\n";
    echo " <div id='${params->canvas}-thumb-plot' style='float:left;width:${params->width}px;height:${params->height}px;'></div>\n";
    echo " <div id='${params->canvas}-thumb-legend' style='float:right;font-size:.75em;'></div>\n";
    echo "</div>\n";
  }
  else {
    echo "<div id='${params->canvas}-link' style='width:${params->width}px;'>\n";
    echo " <div id='${params->canvas}-thumb-title' style='width:100%;font-size:1em;text-align:center;'>${params->title} <img src='images/maximize.png' alt='maximize'/></div>\n";
    echo " <div id='${params->canvas}-thumb-plot' style='width:${params->width}px;height:${params->height}px;'></div>\n";
    echo " <div id='${params->canvas}-thumb-legend' style='width:100%;font-size:.75em;'></div>\n";
    echo "</div>\n";
  }
	
  echo "<div style='display:none'>\n";
  echo " <div id='${params->canvas}-full-container' style='text-align:center;display:inline-block;'>\n";
  echo "  <div style='text-align:center;display:inline-block;width:100%;'>\n";
	echo "   <div id='${params->canvas}-full-title' style='float:center;font-size:1.5em;text-align:center;margin-bottom:.5em;'>${params->title}</div>\n";
	echo "  </div>\n";
	echo "  <div id='${params->canvas}-full-plot'></div>\n";
  echo "  <div id='${params->canvas}-full-legend' style='display:inline-block;text-align:left;font-size:1.25em;margin-top:1em;'></div>\n";
  echo " </div>\n";
  echo "</div>\n";
  
  if ($params->labels_mbean) {
    $mbean = $params->mbean_server->lookup($params->labels_mbean);
    if ($mbean) {
			$labels = $mbean->Labels;
			if ($labels) {
			  $g_labels[$params->canvas] = $labels;
			} 
    }
  } else if ($params->labels) {
	 $g_labels[$params->canvas] = $params->labels;
  }
  
  #echo "</span>\n";
}

function stat_graph_script($stat, $canvas, $names, 
                           $time = null, $offset = null, $period = null)
{
  global $g_colors;
  global $g_mbean_server;
  global $g_labels;
  
  if (! $time) {
    $time = time();
  }
  
  $end = ($time - $offset);
  $start = ($end - $period);
  
  $date = new DateTime();
  $tz_offset_ms = $date->getOffset() * 1000;
  
//  echo "<script id='${canvas}-script' language='javascript' type='text/javascript'>\n";
  echo "<script language='javascript' type='text/javascript'>\n";
  echo "<!-- \n";
  echo "$(function () {\n";
  
  $index = null;
  
  echo "var thumb_graphs = [];\n";
  echo "var full_graphs = [];\n";

  $color_counter = 0;
  $counter = 0;
  
  $period_ms = $period * 1000;
  $data_end_ms = ($end * 1000);
  $data_start_ms = $data_end_ms - $period_ms;
  
  $has_data = false;
  
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

      for ($i = 1; $i < sizeof($values) - 1 ; $i++) {
        if ($values[$i - 1]->value != $values[$i]->value
            || $values[$i]->value != $values[$i + 1]->value) {
          echo "[" . ($values[$i]->time + $tz_offset_ms) . ", " . $values[$i]->value . "],\n";
        }
      }
			
      echo "[" . ($values[$size-1]->time + $tz_offset_ms) . ", " . $values[$size-1]->value . "]\n";
      
      $has_data = true;
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
    
    $counter++;
		
    echo "// END $name\n";
    echo "\n";
  }
  
  $x_max_ms = $data_end_ms + $tz_offset_ms;
  $x_min_ms = $x_max_ms - $period_ms;
  
  #echo "alert('time=" . date("Y-m-d H:i", $time) . ", offset=${offset}, period=${period}, start=" . date("Y-m-d H:i", $start) . ", end=" . date("Y-m-d H:i", $end) . "');\n";
  
  echo "var x_min_ms = ${x_min_ms}\n";
  echo "var x_max_ms = ${x_max_ms}\n";
  echo "\n";
  
  $labels = $g_labels[$canvas];;
  
  echo "function tickFormatter(val, axis) {\n";
  if ($labels) {
    echo "  var labels = [];\n"
    for ($i=0; $i<sizeof($labels); $i++) {
      echo "  labels[$i] = '" . $labels[$i] . "';\n";
    }
    echo "  if (val >= labels.length || val < 0 || val % 1 > 0)\n";
    echo "    return '';\n";
    echo "  else\n"
    echo "    return labels[val];\n";
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
  
  if (! $has_data) {
    echo "  $('<div class=\"no-data\">NO DATA</div>').appendTo(\"#${canvas}-thumb-plot\");\n";
  } else {
  
    echo "$(\"#${canvas}-thumb-plot\").css('cursor','pointer');\n";
    
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
  }
  
  echo "});\n";
  
  echo " -->\n";
  echo "</script>";
}

function display_graph_control()
{
  global $g_page;
  global $g_server_index;
  global $g_periods;
  global $g_time;
  global $g_offset;
  global $g_period;
  
  if (isset($_REQUEST["t"])) {
    $g_time = $_REQUEST["t"];
  }
  
  if (isset($_REQUEST["o"])) {
    $g_offset = $_REQUEST["o"];
  }

  if (isset($_REQUEST["p"])) {
    $g_period = $_REQUEST["p"];
  }
  
  echo "<div class='status-item' id='graph-control-bar'>\n";
  echo " <form class='status-item' name='graphs' method='get' action='#'>";
  echo " <input type='hidden' name='q' value='${g_page}'/>\n";
  echo " <input type='hidden' name='s' value='${g_server_index}'/>\n";
  if ($g_time) {
    echo " <input type='hidden' name='t' value='${g_time}'/>\n";
  }
  echo " <label for='graph-control-period'>Graphs</label>:"; 
  echo " <select name='p' id='graph-control-period' onchange='document.forms.graphs.submit();' disabled='disabled'>\n";
		
	$found_period = false;
	foreach ($g_periods as $period => $name) {
		echo "  <option value='${period}'";
		if ($g_period == $period) {
			echo "selected='selected'";
			$found_period = true;
		}
		echo ">${name}</option>\n";
	}
	
	if (! $found_period) {
		echo "  <option value='${g_period}' selected='selected'>Other (${g_period}s)</option>\n";
	}
	
	echo " </select> of data \n";
	echo " <label for='graph-control-offset'>ending</label> ";
	echo " <select name='o' id='graph-control-offset' onchange='document.forms.graphs.submit();' disabled='disabled'>\n";

	$found_offset = false;
	foreach ($g_periods as $offset => $name) {
		echo "  <option value='${offset}'";
		if ($g_offset == $offset) {
			echo "selected='selected'";
			$found_offset = true;
		}
		echo ">${name} ago</option>\n";
	}
	
	if (! $found_offset) {
		if ($g_offset == 0) {
			echo "  <option value='0' selected='selected'>Now</option>\n";
		}
		else {
			echo "  <option value='0'>Now</option>\n";
			echo "  <option value='${g_offset}' selected='selected'>Other (${g_offset}s)</option>\n";
		}
	} else {
		echo "  <option value='0'>Now</option>\n";
	}
	
	echo " </select>\n";
	echo " </form>\n";
	echo "</div>\n";
}

function enable_graph_controls()
{
  echo "<script language='javascript' type='text/javascript'>\n";
  echo "<!-- \n";
  echo "	$(\"#graph-control-period\").removeAttr('disabled');\n";
  echo "	$(\"#graph-control-offset\").removeAttr('disabled');\n";
  echo " -->\n";
  echo "</script>";
}


