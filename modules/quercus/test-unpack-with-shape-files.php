<?php
ini_set("xdebug.var_display_max_depth", "100");
require '/Users/immanuelscheerer/Documents/web/hume/Uvaluate/app/library/ShapeFile.php';
$shapeFile = new ShapeFile('/Users/immanuelscheerer/Downloads/Buerolagen_A_Staedte_region/Buerolagen_A_Staedte_region.shp', array('noparts' => false));
while ($record = $shapeFile->getNext()) {
	$data = $record->getDbfData();
	// read shape data
	// Array ([0] => xmin [1] => ymin [2] => xmax [3] => ymax [4] => numparts [5] => numpoints [6] => parts)
	$shp_data = $record->getShpData();
	
	if ($record->getError() !== '') {
		throw new Exception($record->getError());
	}
	//var_dump($data);
	//var_dump($shp_data);
}
?>