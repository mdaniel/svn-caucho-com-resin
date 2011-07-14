<?php

require_once 'pdfGraph.php';


define("STEP", 1);

define("HOUR", 3600);



initPDF();
startDoc();



if (! $pdf_name) {
  $pdf_name = "Summary-PDF";
}

$mPage = getMeterGraphPage($pdf_name);

if ($mPage->columns==1) {
	define("ROW1",     450);
	define("ROW2",     100);
	define("COL1",     50);
	define("COL2",     305);
	define("GRAPH_SIZE", new Size(400, 250));
} elseif ($mPage->columns==2) {
	define("ROW1",     600);
	define("ROW2",     400);
	define("ROW3",     50);

	define("COL1",     50);
	define("COL2",     305);
	define("GRAPH_SIZE", new Size(200, 125));
}

$pageName = $mPage->name;
$period = $_REQUEST['period'] ? (int) $_REQUEST['period'] : ($mPage->period/1000); 

if ($period < HOUR) {
	$majorTicks = HOUR / 6;
}  elseif ($period >= HOUR && $period < 3 * HOUR) {
		$majorTicks = HOUR /2;
} elseif ($period >= 3 * HOUR && $period < 6 * HOUR) {
		$majorTicks = HOUR;
} elseif ($period >= 6 * HOUR && $period < 12 * HOUR) {
		$majorTicks = 2 * HOUR;
} elseif ($period >= 12 * HOUR && $period < 24 * HOUR) {
		$majorTicks = 4 * HOUR;
} else {
		$majorTicks = 24 * HOUR;
}

$majorTicks = $majorTicks * 1000;

$minorTicks = $majorTicks/2;




$canvas->setFont("Helvetica-Bold", 26);
$canvas->writeText(new Point(175,800), "$pageName");

$page = 0;


if ($mPage->hasSummary) {
	drawSummary();
	newPage();
}

$index = $g_server->SelfServer->ClusterIndex;
$si = sprintf("%02d", $index);

$time = (int) (time());

$end = $time;
$start = $end - $period;

$start = $end - $period;

$canvas->setFont("Helvetica-Bold", 16);
$canvas->writeText(new Point(175,775), "Time at " . date("Y-m-d H:i", $time));


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
$gCount=0;
foreach ($graphs as $graphData) {

	$index++;
	
	if($mPage->columns==1) {
		$x = COL1;
		if ($index%2==0) {
			$y = ROW2;
		} else {
			$y = ROW1;
		}
	}

	if($mPage->columns==2) {
		if ($gCount <= 1) {
			$y = ROW1;
		} elseif ($gCount > 1 && $gCount <= 3) {
			$y = ROW2;
		} elseif ($gCount > 3 && $gCount <= 5) {
			$y = ROW3;
		}

		if ($index%6==0) {
			$gCount=0;
		} else {
			$gCount++;
		}

		if ($index%2==0) {
			$x = COL2;
		} else {
			$x = COL1;
		}

	}


	$meterNames = $graphData->getMeterNames();
	$gds = getStatDataForGraphByMeterNames($meterNames);
	$gd = getDominantGraphData($gds);
	$graph = createGraph($graphData->getName(), $gd, new Point($x,$y));
	drawLines($gds, $graph);
	$graph->drawLegends($gds);
	$graph->end();

	if ($index%2==0 && $mPage->columns==1) {
		$pdf->end_page();
		$pdf->begin_page(595, 842);
		writeFooter();
	} elseif ($index%6==0 && $mPage->columns==2) {
		$pdf->end_page();
		$pdf->begin_page(595, 842);
		writeFooter();
	}

}
 


if ($mPage->hasLog) {
	newPage();
	drawLog();
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

// needed for PdfReport health action
return "ok";

?>
