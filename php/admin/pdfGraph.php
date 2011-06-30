<?php



import java.lang.System;

function debug($msg) {
  System::out->println($msg);
}

function startsWith($haystack, $needle)
{
    $length = strlen($needle);
    return (substr($haystack, 0, $length) === $needle);
}

function endsWith($haystack, $needle)
{
    $length = strlen($needle);
    $start  = $length * -1; //negative
    return (substr($haystack, $start) === $needle);
}

function  my_error_handler(int $error_type, string $error_msg, string $errfile, int $errline) {
  if(!startsWith($error_msg,"Can't access private field")) {
    debug("ERROR HANDLER: type $error_type, msg $error_msg, file $errfile, lineno $errline");
  }
} 

set_error_handler('my_error_handler'); 

function initPDF() {
  global $pdf;
  $pdf = new PDF();

}

function startDoc() {
  global $pdf;
  $pdf->begin_document();
  $pdf->begin_page(595, 842);
}



class Color {

  function doSetColor($canvas) {
  }
}

class RGBColor {
  private $red;
  private $green;
  private $blue;

  function __construct() {
    $args = func_get_args();
    $this->red =  $args[0];
    $this->green =  $args[1];
    $this->blue =  $args[2];
  }


  function doSetColor($canvas) {
    $canvas->setRGBColor($this->red, $this->green, $this->blue);
  } 

}

$black = new RGBColor(0.0, 0.0, 0.0);
$red = new RGBColor(1.0, 0.0, 0.0);
$green = new RGBColor(0.0, 1.0, 0.0);
$blue = new RGBColor(0.0, 0.0, 1.0);
$darkGrey = new RGBColor(0.2, 0.2, 0.2);
$lightGrey = new RGBColor(0.9, 0.9, 0.9);
$grey = new RGBColor(0.45, 0.45, 0.45);
$purple = new RGBColor(0.45, 0.2, 0.45);
$orange = new RGBColor(1.0, 0.66, 0.0);
$cyan = new RGBColor(0.0, 0.66, 1.0);
$brown = new RGBColor(0.66, 0.20, 0.20);




class Canvas {
  private $origin;
  
  function __construct() {
    $args = func_get_args();
    $this->origin =  $args[0];
    $this->lastTextPos = new Point(0,0); //to fix problem with Resin PDF Lib clone
  }

  function start() {
    global $pdf;
    $pdf->save();
    $pdf->translate($this->origin->x, $this->origin->y);

  }

  function end() {
    global $pdf;
    $pdf->restore();
  }

  function __toString() {
    $str = " (CANVAS ORIGIN $origin)";
    return $str;
  }


  function moveTo($point) {
    global $pdf;
    $pdf->moveto($point->x, $point->y);
  }

  function lineTo($point) {
    global $pdf;
    $pdf->lineto($point->x, $point->y);
  }

  function stroke() {
    global $pdf;
    $pdf->stroke();
  }


  function __get($name) {
    return $this->$name;
  }

  function writeText($point, $text) {
    global $pdf;
    $pdf->set_text_pos($point->x, $point->y);
    $pdf->show($text);
  }

  function setColor(Color $color) {
    $color->doSetColor($this);
  }

  function setRGBColor($red, $green, $blue) {
    global $pdf;
    $pdf->setcolor("fillstroke", "rgb", $red, $green, $blue);
  }

  function setFont($fontName, $fontSize) {
    global $pdf;
    $font = $pdf->load_font($fontName, "", "");
    $pdf->setfont($font, $fontSize);
  }

}

$canvas = new Canvas(new Point(0,0));


class Range {
  private $start;
  private $stop;

  function __construct() {
    $args = func_get_args();
    $this->start = (float) $args[0];
    $this->stop = (float) $args[1];
  }

  function __set($name, $value) {
      $this->$name = (double) $value;
  }

  function __get($name) {
    return $this->$name;
  }

  function __toString() {
    $str = " (RANGE WIDTH:$this->start; HEIGHT:$this->stop;)";
    return $str;
  }

  function size() {
    return $this->stop - $this->start;
  }

}


class Size {
  private $width;
  private $height;

  function __construct() {
    $args = func_get_args();
    $this->width = (float) $args[0];
    $this->height = (float) $args[1];
  }

  function __set($name, $value) {
      $this->$name = (double) $value;
  }

  function __get($name) {
    return $this->$name;
  }

  function __toString() {
    $str = " (SIZE WIDTH:$this->width; HEIGHT:$this->height;)";
    return $str;
  }

}

class Point {
  private $x;
  private $y;

  function __construct() {
    $args = func_get_args();
    $this->x = (float) $args[0];
    $this->y = (float) $args[1];
  }


  function __set($name, $value) {
      $this->$name = (double) $value;
  }


  function __get($name) {
    return $this->$name;
  }


  function __toString() {
    $str = "POINT( X:$this->x; Y:$this->y;)";
    return $str;
  }

}

class Graph {
  private $pixelSize;
  private $xRange;
  private $yRange;
  private $canvas;
  private $title;
  private $pixelPerUnit;

  function __construct(string $title, Point $origin, Size $pixelSize, Range $xRange, Range $yRange, boolean $trace=false) {
    $this->title = $title;
    $this->canvas = new Canvas($origin);
    $this->pixelSize = $pixelSize;
    $this->xRange = $xRange;
    $this->yRange = $yRange;
    $this->trace = $trace;
   

    if ($this->yRange->size()==0.0) {
       debug("YRANGE was 0 for " . $this->title);
       $this->valid=false;
    } else {
      $this->valid=true;
    }

    $this->pixelPerUnit = new Size();
    $this->pixelPerUnit->width = $this->pixelSize->width / $this->xRange->size();
    $this->pixelPerUnit->height = $this->pixelSize->height / $this->yRange->size();

    if ($this->pixelPerUnit->width == 0.0 || $this->pixelPerUnit->height == 0.0) {
       debug("pixel per unit was 0.0 " . $this->title);
       $this->valid=false;
       
    } else {
      $this->xOffsetPixels = $this->xRange->start * $this->pixelPerUnit->width;
      $this->yOffsetPixels = $this->yRange->start * $this->pixelPerUnit->height;
    }
    if ($this->trace) {
       $this->trace("$title graph created --------------------- ");
    }

  }

  function trace($msg) {
	if ($this->trace) debug("GRAPH " . $this->title . " " . $msg);
  }


  function __toString() {
    $str = "(GRAPH Canvas $this->canvas, XRANGE $this->xRange, YRANGE $this->yRange)";
    return $str;
  }

  function start() {
    $this->canvas->start();
  }

  function end() {
    $this->canvas->end();
  }


  function __destruct() {
    $this->canvas = null;
  }


  function convertPoint($point) {
    $convertedPoint = new Point();
    $convertedPoint->x = intval(($point->x  * $this->pixelPerUnit->width) - $this->xOffsetPixels);
    $convertedPoint->y = intval(($point->y  * $this->pixelPerUnit->height) - $this->yOffsetPixels);
    if ($convertedPoint->x > 1000 || $convertedPoint->x < 0) {
       debug("Point out of range x axis $convertedPoint->x  for " .  $this->title);
       $this->valid = false;
    }
    if ($convertedPoint->y > 1000 || $convertedPoint->y < 0) {
       debug("Point out of range y axis $convertedPoint->y for " .  $this->title);
       $this->valid = false;
    }
    return $convertedPoint;
  }

  function drawTitle($title=null) {
    $this->trace("drawTitle " . $title);

    if (!$title) $title = $this->title;
    $y = $this->pixelSize->height + 15;
    $x = 0.0;
    if ($this->valid) {
       $this->trace("drawTitle valid" );
       $this->canvas->writeText(new Point($x, $y), $title);
    } else {
      $this->trace("drawTitle NOT VALID" );
      $this->canvas->writeText(new Point($x, $y), $title . " not valid");
    }
  }

  function drawLegends($legends, $point=new Point(0.0, -20)) {

    if (!$this->valid) {
       $this->trace("drawLegends NOT VALID" );
       return;
    }

    $this->trace("drawLegends" );

    $col2 =   (double) $this->pixelSize->width / 2;
    $index = 0;
    $yinc = -7;
    $initialYLoc = -20;
    $yloc = $initialYLoc;

    foreach ($legends as $legend) {
      if ($index % 2 == 0) {
	$xloc = 0.0;
      } else {
	$xloc = $col2;
      }
    
      $row = floor($index / 2);
      
      $yloc = ((($row) * $yinc) + $initialYLoc);

      $this->drawLegend($legend->color, $legend->name, new Point($xloc, $yloc));
      $index++;


    }
  }

  function drawLegend($color, $name, $point=new Point(0.0, -20)) {
    if (!$this->valid) {
       return;
    }

 
    $this->trace("drawLegend SINGLE " . $name);

    global $canvas;
    global $black;

    $x = $point->x;
    $y = $point->y;

    $canvas->setColor($color);
    $this->canvas->moveTo(new Point($x, $y+2.5));
    $this->canvas->lineTo(new Point($x+5, $y+5));
    $this->canvas->lineTo(new Point($x+10, $y+2.5));
    $this->canvas->lineTo(new Point($x+15, $y+2.5));
    $this->canvas->stroke();

    $canvas->setColor($black);
    $this->canvas->setFont("Helvetica-Bold", 6);
    $this->canvas->setColor($black);
    $this->canvas->writeText(new Point($x+20, $y), $name);


  }
  
  function drawLine($dataLine) {
    if (!$this->valid) {
       return;
    }
    $this->trace("drawLine ");


    $this->canvas->moveTo($this->convertPoint($dataLine[0]));

    for ($index = 1; $index < sizeof($dataLine); $index++) {
      $p = $this->convertPoint($dataLine[$index]);
      if (!$this->valid) {
      	 break;
      }
      $this->canvas->lineTo($p);
    }

    $this->canvas->stroke();
  }



  function drawGrid() {
    $this->trace("drawGrid ");


    $width =   (double) $this->pixelSize->width;
    $height =   (double) $this->pixelSize->height;
    $this->canvas->moveTo(new Point(0.0, 0.0));
    $this->canvas->lineTo(new Point($width, 0.0));
    $this->canvas->lineTo(new Point($width, $height));
    $this->canvas->lineTo(new Point(0.0, $height));
    $this->canvas->lineTo(new Point(0.0, 0.0));
    $this->canvas->stroke();
  }

  function drawGridLines($xstep, $ystep) {

  
   if (!$ystep) {
      $this->valid = false;
      debug("No ystep was passed " .  $this->title);
   }

    if (!$this->valid) {
       return;
    }

    $this->trace("drawGridLines ");

    $width =   intval($this->pixelSize->width);
    $height =   intval($this->pixelSize->height);

    $xstep_width = $xstep * $this->pixelPerUnit->width;
    $ystep_width = $ystep * $this->pixelPerUnit->height;

    if ($xstep_width <= 0.0 || $ystep_width <= 0.0) {
       debug("          ====== Step width was 0 x $xstep_width y $ystep_width " . $this->title);
       debug("      ppu width    " . $this->pixelPerUnit->width);
       debug("      xstep     $xstep ");

       $this->valid = false;
    }


    for ($index = 0; $width >= (($index)*$xstep_width); $index++) {
      $currentX = intval($index*$xstep_width);
      $this->canvas->moveTo(new Point($currentX, 0.0));
      $this->canvas->lineTo(new Point($currentX, $height));
      $this->canvas->stroke();
    }    


    for ($index = 0; $height >= ($index*$ystep_width); $index++) {
      $currentY = intval($index*$ystep_width);
      $this->canvas->moveTo(new Point(0.0, $currentY));
      $this->canvas->lineTo(new Point($width, $currentY));
      $this->canvas->stroke();
    }    


  }

  function drawXGridLabels($xstep, $func) {
    if (!$this->valid) {
       return;
    }

    $this->trace("X drawXGridLabels xstep $xstep, func $func");


    $this->canvas->setFont("Helvetica-Bold", 9);
    $width =   (double) $this->pixelSize->width;

    $xstep_width = ($xstep) * $this->pixelPerUnit->width;

    for ($index = 0; $width >= ($index*$xstep_width); $index++) {
      $currentX = $index*$xstep_width;
      $stepValue = (int) $index * $xstep;
      $currentValue = $stepValue + (int) $this->xRange->start;
      $currentValue = intval($currentValue);

      if (!$func){
      	$currentLabel = $currentValue;
      } else {
	$currentLabel = $func($currentValue);
      }
      $this->canvas->writeText(new Point($currentX-3, -10), $currentLabel);
    }    
  }

  function drawYGridLabels($step, $func=null, $xpos=-28) {
    if (!$this->valid) {
       return;
    }

    $this->trace("Y drawYGridLabels xstep $step, func $func");


    $this->canvas->setFont("Helvetica-Bold", 9);
    $height =   (double) $this->pixelSize->height;

    $step_width = ($step) * $this->pixelPerUnit->height;

    for ($index = 0; $height >= ($index*$step_width); $index++) {
      $currentYPixel = $index*$step_width;
      $currentYValue =	($index * $step) + $this->yRange->start;
      if ($func) {
	$currentLabel = $func($currentYValue);
      } else {
      	if ($currentYValue >      1000000000) {
	   $currentLabel = "" . $currentYValue / 1000000000 . " G";
	}elseif ($currentYValue > 1000000) {
	   $currentLabel = "" . $currentYValue / 1000000 . " M";
	}elseif ($currentYValue > 1000) {
	   $currentLabel = "" . $currentYValue / 1000 . " K";
	} else {
	  $currentLabel = $currentYValue; 
	}
      }
      $this->canvas->writeText(new Point($xpos, $currentYPixel-3), $currentLabel);
    }    
  }



}

?>
