<?php

import java.lang.System;

class PdfCanvasGraph 
{
  private $canvas;
  
  private $title;
  
  private $x_range;
  private $y_range;
  
  private $pixel_size;
  private $ppu; // pixels per unit
  
  private $valid = true;

  public function PdfCanvasGraph($canvas,
                                 $title,
                                 $pixel_size,
                                 $x_range,
                                 $y_range)
  {
    $this->canvas = $canvas;
    
    $this->title = $title;
    
    $this->pixel_size = $pixel_size;
    
    $this->x_range = $x_range;
    $this->y_range = $y_range;
    
    if ($this->y_range->size() == 0.0)
      $this->setInvalid("y_range was 0");

    $this->ppu = new Size();
    $this->ppu->width = $this->pixel_size->width / $this->x_range->size();
    $this->ppu->height = $this->pixel_size->height / $this->y_range->size();
    
    if ($this->ppu->width > 0.0 && $this->ppu->height > 0.0) {
      $this->xOffsetPixels = $this->x_range->start * $this->ppu->width;
      $this->yOffsetPixels = $this->y_range->start * $this->ppu->height;
    } else {
      $this->setInvalid("ppu={$this->ppu}");
    }
  }

  public function __toString() 
  {
    return "PdfCanvasGraph($this->title,x_range={$this->x_range},y_range={$this->y_range},size={$this->pixel_size},ppu={$this->ppu})";
  }
  
  public function setInvalid($msg)
  {
    $this->valid = false;
    $this->debug("WARNING: Graph {$this->title} is invalid: $msg");
  }
  
  public function debug($text) 
  {
    $this->canvas->debug("{$this->title}:$text");
  }
  
  public function end() 
  {
    $this->canvas->endGraph();
  }
  
  public function convertX($x)
  {
    $convertedX = round(($x  * $this->ppu->width) - $this->xOffsetPixels);
    //debug("convertX $x to $convertedX");
    return $convertedX;
  }

  public function convertY($y)
  {
    $convertedY = round(($y  * $this->ppu->height) - $this->yOffsetPixels);
    //debug("convertY $y to $convertedY");
    return $convertedY;
  }
  
  public function convertPoint($point) 
  {
    $convertedPoint = new Point();
    
    $convertedPoint->x = round(($point->x  * $this->ppu->width) - $this->xOffsetPixels);
    $convertedPoint->y = round(($point->y  * $this->ppu->height) - $this->yOffsetPixels);
    
    if ($convertedPoint->x > 1000 || $convertedPoint->x < 0)
       $this->setInvalid("Point out of range x axis: {$convertedPoint->x}");
    
    if ($convertedPoint->y > 1000 || $convertedPoint->y < 0)
       $this->setInvalid("Point out of range y axis: {$convertedPoint->y}");
       
    //debug("convertPoint $point to $convertedPoint");
    
    return $convertedPoint;
  }
  
  public function drawTitle($color) 
  {
    //$this->debug("drawTitle:color=$color");
    
    $this->canvas->setFontAndColor("Helvetica", 12, $color);

    $x = 0.0;
    $y = $this->pixel_size->height + $this->canvas->getLineHeight()/2;
    
    $this->canvas->writeTextXY($x, $y, $this->title);
    
    if (! $this->valid) {
      $x = $this->pixel_size->width/2;
      $y = $this->pixel_size->height/2;
      $this->canvas->setFontAndColor("Courier-Bold", 8, "med_grey");
      $this->canvas->writeTextXYCenter($x, $y, "NO DATA");
    }
  }

  public function drawLegends($gds, $bds=null) 
  {
    if (! $this->valid)
       return;

    //$this->debug("drawLegends");

    $col2 = round($this->pixel_size->width / 2) + 5;
    $index = 0;
    $yinc = -7;
    $initialYLoc = -25;
    $yloc = $initialYLoc;

    foreach ($gds as $gd) {
      if ($index % 2 == 0) {
	      $xloc = 5.0;
      } else {
	      $xloc = $col2;
      }
    
      $row = floor($index / 2);
      $yloc = ((($row) * $yinc) + $initialYLoc);

      $this->drawLineLegend($gd->color, $gd->name, new Point($xloc, $yloc));
      $index++;
    }
    
    foreach($bds as $bd) {
      if ($index % 2 == 0) {
        $xloc = 5.0;
      } else {
        $xloc = $col2;
      }
      
      $row = floor($index / 2);
      $yloc = ((($row) * $yinc) + $initialYLoc);
      
      $this->drawBlockLegend($bd->color, $bd->name, new Point($xloc, $yloc));
      $index++;
    }
  }

  public function drawLineLegend($color, $name, $point=new Point(0.0, -20)) 
  {
    if (! $this->valid)
       return;
 
    //$this->debug("drawLineLegend $name");

    $x = $point->x;
    $y = $point->y;

    $this->canvas->setColor($color);
    
    if ($dashed)
      $this->canvas->setDash();
    
    $this->canvas->moveToXY($x, $y+2.5);
    $this->canvas->lineToXY($x+5, $y+5);
    $this->canvas->lineToXY($x+10, $y+2.5);
    $this->canvas->lineToXY($x+15, $y+2.5);
    $this->canvas->stroke();

    if ($dashed)
      $this->canvas->setSolid();
    
    $this->canvas->setFontAndColor("Helvetica", 7, "black");
    $this->canvas->writeTextXY($x+20, $y, $name);
  }
  
  public function drawBlockLegend($color, $name, $point=new Point(0.0, -20)) 
  {
    if (! $this->valid)
       return;
 
    //$this->debug("drawBlockLegend $name");

    $x = $point->x;
    $y = $point->y;

    $this->canvas->setFillColor($color);
    
    $this->canvas->drawRectXY($x, $y-1, 15, 5);
    
    $this->canvas->setFontAndColor("Helvetica", 7, "black");
    $this->canvas->writeTextXY($x+20, $y, $name);
  }

  public function drawLineGraph($dataLine, $color, $lineWidth)
  {
    if (! $this->valid)
       return;
    
    //$this->debug("drawLineGraph:dataLine=$dataLine,color=$color,lineWidth=$lineWidth");
    
    $this->canvas->setColor($color);
    $this->canvas->setLineWidth($lineWidth);
    
    $this->canvas->moveToPoint($this->convertPoint($dataLine[0]));

    for ($index = 1; $index < sizeof($dataLine); $index++) {
      $p = $this->convertPoint($dataLine[$index]);
      
      if (! $this->valid)
      	 break;
      
      $this->canvas->lineToPoint($p);
    }
    
    $this->canvas->stroke();
  }

  public function drawGraphBlocks($blockData, $lineWidth)
  {
    if (! $this->valid)
      return;
  
    //debug("drawGraphBlocks:name=$blockData->name,color=$blockData->color,lineWidth=$lineWidth");
  
    $this->canvas->setFillColor($blockData->color);
    
    $height = (double) $this->pixel_size->height;
    
    foreach($blockData->blocks as $block) {
      $x1 = $this->convertX($block->start_time);
      $x2 = $this->convertX($block->end_time);
      $w = $x2 - $x1;
      if ($w == 0)
        $w = 1;
      $this->canvas->drawRectXY($x1, 0, $w, $height);
      //debug("drawRectXY:${x1},0,${w},${height}");
    }
  }
  
  public function drawBorder($color)
  {
    //$this->debug("drawBorder:color=$color");
    
    $this->canvas->setLineWidth(1);
    $this->canvas->setColor($color);
    
    $width = (double) $this->pixel_size->width;
    $height = (double) $this->pixel_size->height;
    
    $this->canvas->moveToXY(0.0, 0.0);
    $this->canvas->lineToXY($width, 0.0);
    $this->canvas->lineToXY($width, $height);
    $this->canvas->lineToXY(0.0, $height);
    $this->canvas->lineToXY(0.0, 0.0);
    $this->canvas->stroke();
  }

  public function drawGridLines($xstep, $ystep, $color)
  {
    $this->canvas->setLineWidth(0.5);
    $this->canvas->setColor($color);
    
//    if (! $ystep)
//      $this->setInvalid("No ystep was passed");

    if (!$this->valid)
       return;

    //$this->debug("drawGridLines:xstep=$xstep,ystep=$ystep,color=$color");

    $width = $this->pixel_size->width;
    $height = $this->pixel_size->height;

    $xstep_width = $xstep * $this->ppu->width;
    $ystep_width = $ystep * $this->ppu->height;

    if ($xstep_width <= 0.0 || $ystep_width <= 0.0) {
       $this->setInvalid("Step width was 0");
       return;
    }

    for ($index = 0; $width >= (($index)*$xstep_width); $index++) {
      $currentX = round($index*$xstep_width);
      $this->canvas->moveToXY($currentX, 0.0);
      $this->canvas->lineToXY($currentX, $height);
      $this->canvas->stroke();
    }

    for ($index = 0; $height >= ($index*$ystep_width); $index++) {
      $currentY = round($index*$ystep_width);
      $this->canvas->moveToXY(0.0, $currentY);
      $this->canvas->lineToXY($width, $currentY);
      $this->canvas->stroke();
    }
  }

  public function drawXGridLabels($xstep, $func=null)
  {
    if (! $this->valid)
       return;

    $width = (double) $this->pixel_size->width;
    $xstep_width = $xstep * $this->ppu->width;
    
    $font_size = 8;

    //$this->debug("drawXGridLabels:xstep=$xstep,func=$func,width=$width,xstep_width=$xstep_width");
    
    $this->canvas->setFontAndColor("Helvetica", $font_size, "black");
    
    for ($index = 0; $width >= ($index*$xstep_width); $index++) {
      $currentX = round($index*$xstep_width);
      $stepValue = round($index * $xstep);
      $currentValue = $stepValue + $this->x_range->start;

      if (!$func) {
      	$currentLabel = $currentValue;
      } else {
	      $currentLabel = $this->$func($currentValue);
      }
      
      if (! is_null($currentLabel)) {
        $this->canvas->writeTextXYCenter($currentX, -13, $currentLabel);
        
        $this->canvas->moveToXY($currentX, -4);
        $this->canvas->lineToXY($currentX, 0);
        $this->canvas->stroke();
      }
    }
  }

  public function drawYGridLabels($ystep, $func=null) 
  {
    if (! $this->valid)
       return;
       
    $font_size = 8;

    $this->canvas->setFontAndColor("Helvetica", $font_size, "black");
    $height = (double) $this->pixel_size->height;

    $step_width = $ystep * $this->ppu->height;

    //$this->debug("drawYGridLabels:ystep=$ystep,func=$func,height=$height,step_width=$step_width");
    
    for ($index = 0; $height >= ($index*$step_width); $index++) {
      $currentYPixel = round($index*$step_width);
      $currentYValue =	($index * $ystep) + $this->y_range->start;
      
      if (isset($func)) {
	      $currentLabel = $func($currentYValue);
      } else {
      	if ($currentYValue >      1000000000) {
	        $currentLabel = "" . $currentYValue / 1000000000 . "G";
	      } elseif ($currentYValue > 1000000) {
	        $currentLabel = "" . $currentYValue / 1000000 . "M";
	      } elseif ($currentYValue > 1000) {
	        $currentLabel = "" . $currentYValue / 1000 . "K";
	      } else {
	        $currentLabel = $currentYValue; 
	      }
      }

      if (! is_null($currentLabel)) {
        $x = -6;
        $y = $currentYPixel - ($this->canvas->getTextHeight($currentLabel) / 2);
        $this->canvas->writeTextXYRight($x, $y, $currentLabel);
        
        $this->canvas->moveToXY(-4, $currentYPixel);
        $this->canvas->lineToXY(0, $currentYPixel);
        $this->canvas->stroke();
      }
    }
  }
  
  function formatTime($ms)
  {
    $time = $ms / 1000;
    $tz = date_offset_get(new DateTime);
   
    if (($time + $tz) % (24 * 3600) == 0) {
      return strftime("%m-%d", $time);
    } else {
      return strftime("%H:%M", $time);
    }
  }
}

class GraphData 
{
  private $name;
  private $dataLine;
  private $max;
  private $color;

  public function __set($name, $value)
  {
    $this->$name = $value;
  }

  public function __get($name)
  {
    return $this->$name;
  }
  
  public function __toString() 
  {
    return "GraphData(name={$this->name},dataLine={$this->dataLine},max={$this->max},color=$color)";
  }
}

class BlockData
{
  private $name;
  private $blocks;
  private $color;

  public function __set($name, $value)
  {
    $this->$name = $value;
  }

  public function __get($name)
  {
    return $this->$name;
  }

  public function __toString()
  {
    return "BlockData(name={$this->name},blocks={$this->blocks},color=$color)";
  }
}

class GraphBlock
{
  private $start_time;
  private $end_time;

  public function __set($name, $value)
  {
    $this->$name = $value;
  }

  public function __get($name)
  {
    return $this->$name;
  }

  public function __toString()
  {
    return "GraphBlock(start_time={$this->start_time},end_time={$this->end_time})";
  }
}

?>
