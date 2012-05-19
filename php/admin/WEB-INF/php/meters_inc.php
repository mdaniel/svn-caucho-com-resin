<?php
/**
 * Custom meters
 */

require_once "WEB-INF/php/inc.php";

function meter_display_page($page)
{
  global $g_period, $g_periods;
  
  $count = 0;
  
  enable_graph_controls();
  
  $name = preg_replace('/ /', '_', $page->name);
  $page_name = "pg_" . $name;
  
  $period_ms = $page->period;
  $period = ($period_ms / 1000);
  $period_name;
  
  if ($period > 0) {
    if (! array_key_exists($period, $g_periods)) {
      echo "<script language='javascript' type='text/javascript'>\n";
      echo "<!-- \n";
      echo "	$(\"#graph-control-period\").append($('<option>', {value : ${period}}).text('Other (${period}s)'));\n"
      echo " -->\n";
      echo "</script>";
    }
  }
  
  if (isset($_REQUEST["p"])) {
    $period = $_REQUEST["p"];
  }
  else if ($period <= 0) {
    $period = $g_period;
  } 
  
  $period_name = $g_periods[$period];
  
  echo "<script language='javascript' type='text/javascript'>\n";
  echo "<!-- \n";
  echo "	option = $(\"#graph-control-period\").val('${period}').attr('selected','selected');\n";
  echo " -->\n";
  echo "</script>";
  
  $columns = $page->columns;
  $graph_count = count($page->meterGraphs);

  $width = 800 / $columns;
  $height = $width * 0.66;

  $count = 0;
  $is_column = false;
  
  echo "<div style='display:inline-block;width:800px;padding-top:.5em;padding-bottom:1.5em;'>\n";
  echo " <div style='float:left;font-size:larger;font-weith:bold;'>" . gettext($page->getName()) . " " . gettext('Graphs') . "</div>\n";
  echo " <div style='float:right;font-size:smaller;font-style:italic;'>" . gettext('Period') . ": $period_name</div>\n";
  echo "</div>\n";
  
  foreach ($page->meterSections as $section) {
    if ($section->name) {
      echo "<h2>" . $section->name . "</h2>";
    }
    echo "<table border='0'>\n";

    $section_graph_count = 0;

    foreach ($section->meterGraphs as $graph) {
      $graph_name = $page_name . "_" . $count;

      if ($section_graph_count % $columns == 0) {
        if ($is_column)
          echo "</tr>";
        
        echo "<tr>";

        $is_column = true;
      }

      if ($graph) {
        echo "<td valign='top'>";
        meter_display_graph($graph_name, $graph, $width, $height, $period);
        echo "</td>";
      }

      $count++;
      $section_graph_count++;
    }

    if ($is_column)
      echo "</tr>";
    
    echo "</table>";
  }
}  

function meter_display_graph($name, $graph, $width, $height, $period, $mbean_server = null)
{
//  echo " <div style='float:top;padding:.5em;'>\n"

  global $g_server;
  $si = sprintf("%02d", $g_server->ServerIndex);

  $meters = array();

  if ($graph->name)
    $caption = gettext($graph->name);

  foreach ($graph->meterNames as $meter_name) {
    $full_name = $si . "|" . $meter_name;

    $meters[] = $full_name;

    if (! $caption)
      $caption = $full_name;
  }

  if (! $caption)
    $caption = $name;

	$params = new GraphParams($name, $width, $height);
	$params->period = $period;
	$params->title = $caption;
	$params->alt = "A line graph representing $caption over the last 6 hours. ";

  stat_graph($params, $meters);
                 
//  echo " </div>\n";
}

?>
