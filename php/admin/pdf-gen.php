<?php

require_once "WEB-INF/php/inc.php";
require_once 'pdfGraph.php';


define("ROW1",     100);
define("ROW2",     500);
define("ROW3",     100);
define("COL1",     50);
define("COL2",     305);
define("MILLION", 1000000);
define("GRAPH_SIZE_6_TO_PAGE", new Size(175, 125));
define("GRAPH_SIZE", new Size(400, 300));
define("BIG_GRAPH_SIZE", new Size(400, 300));

define("PERIOD", 3600/2);
define("X_GRID_MINOR", 300 * 1000);
define("X_GRID_MAJOR", 600 * 1000);
define("STEP", 1);

$period = $_REQUEST['period'] ? (int) $_REQUEST['period'] : PERIOD; 

$majorTicks = $_REQUEST['majorTicks'] ? (int) ($_REQUEST['majorTicks'] * 1000) : X_GRID_MAJOR; 
$minorTicks = $_REQUEST['minorTicks'] ? (int) ($_REQUEST['minorTicks'] * 1000) : X_GRID_MINOR; 

debug("$majorTicks $minorTicks");


if (! admin_init_no_output()) {
  debug("Failed to load admin, die");
  return;
} else {
    debug("admin_init successful");
}


initPDF();
startDoc();


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


$pdfName = "Summary-PDF";


$mPage = getMeterGraphPage($pdfName);
$pageName = $mPage->name;


$canvas->setFont("Helvetica-Bold", 26);
$canvas->writeText(new Point(175,800), "$pageName");

$page = 0;

$index = $g_server->SelfServer->ClusterIndex;
$si = sprintf("%02d", $index);

$restart_time = getRestartTime($stat);

$end = $restart_time;
$start = $end - $period;



$canvas->setFont("Helvetica-Bold", 16);
$canvas->writeText(new Point(175,775), "Restart at " . date("Y-m-d H:i", $restart_time));


$full_names = $stat->statisticsNames();

$statList = array();

foreach ($full_names as $full_name)  {
  array_push($statList,new Stat($full_name)) 
}


$canvas->setColor($black);
$canvas->moveTo(new Point(0, 770));
$canvas->lineTo(new Point(595, 770));
$canvas->stroke();



writeFooter();


$graphs = $mPage->getMeterGraphs();
$index=0;
foreach ($graphs as $graphData) {

	$index++;
	$x = COL1;
	
	if ($index%2==0) {
		$y = ROW2;
	} else {
		$y = ROW1;
	}
	$meterNames = $graphData->getMeterNames();
	$gds = getStatDataForGraphByMeterNames($meterNames);
	$gd = getDominantGraphData($gds);
	$graph = createGraph($graphData->getName(), $gd, new Point($x,$y));
	drawLines($gds, $graph);
	$graph->drawLegends($gds);
	$graph->end();

	if ($index%2==0) {
		$pdf->end_page();
		$pdf->begin_page(595, 842);
		writeFooter();
	}

}
 



$pdf->end_page();
$pdf->end_document();

$document = $pdf->get_buffer();
$length = strlen($document);



$filename = "$pageName" . ".pdf";



header("Content-Type:application/pdf");



header("Content-Length:" . $length);



header("Content-Disposition:inline; filename=" . $filename);



echo($document);



unset($document);



pdf_delete($pdf);

?>
