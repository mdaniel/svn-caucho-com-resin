<?php

import java.lang.System;

class PdfCanvas 
{
  //
  // public color contants
  //
  
  public $black = new RGBColor(0.0, 0.0, 0.0);
  public $red = new RGBColor(1.0, 0.0, 0.0);
  public $green = new RGBColor(0.0, 1.0, 0.0);
  public $blue = new RGBColor(0.0, 0.0, 1.0);
  public $dark_grey = new RGBColor(0.2, 0.2, 0.2);
  public $light_grey = new RGBColor(0.9, 0.9, 0.9);
  public $grey = new RGBColor(0.45, 0.45, 0.45);
  public $med_grey = new RGBColor(0.6, 0.6, 0.6);
  public $purple = new RGBColor(0.45, 0.2, 0.45);
  public $orange = new RGBColor(1.0, 0.66, 0.0);
  public $cyan = new RGBColor(0.0, 0.66, 1.0);
  public $brown = new RGBColor(0.66, 0.20, 0.20);
  
  public $pdf_colors = array(
      new RGBColor(0xff, 0x30, 0x30), // red
      new RGBColor(0x30, 0xb0, 0xff), // azure
      new RGBColor(0x90, 0x60, 0x00), // brown
      new RGBColor(0xff, 0x90, 0x30), // orange
      new RGBColor(0x30, 0x30, 0xff), // blue
      new RGBColor(0x00, 0x00, 0x00), // black
      new RGBColor(0x50, 0xb0, 0x00), // green
      new RGBColor(0xd0, 0x30, 0xd0), // magenta
      new RGBColor(0x00, 0x80, 0x80), // cyan
      new RGBColor(0xb0, 0x30, 0x60), // rose
      new RGBColor(0xe0, 0x90, 0xff), // indigo
      new RGBColor(0xc0, 0xc0, 0xc0), // gray
      new RGBColor(0x40, 0x80, 0x40)); // forest green
      
  private $pdf;
  
  private $page_number = 0;

  private $text_y;
  
  private $font;
  
  private $font_name;
  private $font_size;
  
  private $line_spacing = 3;

  private $header_left_text;
  private $header_center_text;
  private $header_right_text;

  private $footer_left_text;
  private $footer_center_text;
  private $footer_right_text;

	private $left_margin_width = 40;
	private $top_margin_width = 40;
	private $bottom_margin_width = 40;
	private $right_margin_width = 40;
	
  private $page_width = 595;
  private $page_height = 842;
  
  private $section_font_name = "Times-Bold";
  private $section_font_size = 14;
  
  private $subsection_font_name = "Times-Bold";
  private $subsection_font_size = 12;

  private $text_font_name = "Helvetica";
  private $text_font_size = 9;
  
  private $data_font_name = "Courier";
  private $data_font_size = 8;
  
  private $header_font_name = "Helvetica"
  private $header_font_size = 8;
  
  private $column_spacing = 5;
  private $column_x;

  private $graph_rows = 3;
  private $graph_cols = 2;
  
  private $graph_index;
  
  private $graph_x;
  private $graph_y;
  
  private $graph_width;
  private $graph_height;
  
  private $is_debug = false; 
  
  function PdfCanvas()
  {
    $this->pdf = new PDF();

    //$this->lastTextPos = new Point(0,0); //to fix problem with Resin PDF Lib clone
    
    $this->pdf->begin_document();
    
    $this->newPage(false);
    
    //$this->pdf->save();
  }
  
  function newPage($end_page = true)
  {
    $this->debug("newPage");
    
    if ($end_page)
    {
      $this->writeHeaders();
      $this->writeFooters();
      
      $this->debug("end_page");
      $this->pdf->end_page();
    }
    
    $this->debug("begin_page");
    $this->pdf->begin_page($this->page_width, $this->page_height);
    
    $this->page_number++;
    
    $this->setTextFont();
    
    $this->text_y = $this->getTopMargin() + $this->font_size;
    
    $this->graph_x = 0;
    $this->graph_y = 0;
    $this->graph_index = 0;
  }

  function end()
  {
    $this->debug("end");
    
    $this->writeHeaders();
    $this->writeFooters();
    
    $this->debug("end_page");
    $this->pdf->end_page();
    
    $this->debug("end_document");
    $this->pdf->end_document();
    //$this->pdf->restore();
  }
  
  function writeSelfHttp($file_name)
  {
    $buffer = $this->pdf->get_buffer();
    $length = strlen($buffer);
    
    header("Content-Type:application/pdf");
    header("Content-Length:${length}");
    header("Content-Disposition:inline; filename=${file_name}");
    
    echo($buffer);
  }
  
  function writeSelf()
  {
    echo($this->pdf->get_buffer());
  }
  
  function __get($name)
  {
    return $this->$name;
  }  
  
  function __toString()
  {
    return "PdfCanvas";
  }
  
  function debug($text) 
  {
    if ($is_debug)
      System::out->println($text);
  }
  
  // 
  // lines
  //
  
  function setRGBColor($red, $green, $blue)
  {
    $this->pdf->setcolor("fillstroke", "rgb", $red, $green, $blue);
  }
  
  function drawLine($p1, $p2)
  {
    $this->pdf->moveto($p1->x, $this->translateY($p1->y));
    $this->pdf->lineto($p2->x, $this->translateY($p2->y));
    $this->pdf->stroke();
  }
  
  function writeHrule($indent = 0)
  {
    $height = $this->getLineHeight()/2;
    $this->drawLine(new Point($this->getLeftMargin() + $indent, $this->text_y - $height),
                    new Point($this->getRightMargin(), $this->text_y - $height));
    $this->newLine();
  }
  
  //
  // text positioning
  //
  
  function newLine($count = 1)
  {
    for($i=0;$i<$count;$i++) {
      $this->text_y += $this->getLineHeight();
      $this->$column_x = $this->getLeftMargin();
      
      if ($this->text_y > $this->translateY($this->bottom_margin_width)) {
        $this->newPage();
      }
    }
    
    return 0;
  }
  
  function willOverflowY($count = 1)
  {
    $size = $count * $this->getLineHeight();
    return ($this->text_y + $size) > $this->getBottomMargin();
  }
  
  function getLineHeight()
  {
    //$ascender = $this->pdf->get_value("ascender") / 72;
    //$descender = $this->descender = $this->pdf->get_value("descender") / 72;
    //return $this->font_size + $this->line_spacing + $ascender + $descender;
    
    return $this->font_size + $this->line_spacing;
  }
  
  function translateY($y)
  {
    return $this->page_height - $y; 
  }
  
  //
  // boundaries
  //
  
  function setMargins($top, $right, $bottom, $left)
  {
    $this->top_margin_width = $top;
    $this->right_margin_width = $right;
    $this->bottom_margin_width = $bottom;
    $this->left_margin_width = $left;
  }
  
  function getLeftMargin()
  {
    return $this->left_margin_width;
  }

  function getRightMargin()
  {
    return $this->getPageWidth() - $this->left_margin_width;
  }
  
  function getTopMargin()
  {
    return $this->top_margin_width;
  }
  
  function getBottomMargin()
  {
    return $this->getPageHeight() - $this->bottom_margin_width;
  }  
  
  function getPageCenter()
  {
    return $this->getPageWidth()/2;
  }
  
  function getLineCenter()
  {
    return $this->getLineWidth()/2;
  }
  
  function getLineWidth()
  {
    return $this->getPageWidth() - $this->left_margin_width - $this->right_margin_width;
  }
  
  function getPageWidth()
  {
    return $this->page_width;
  }
  
  function getPageHeight()
  {
    return $this->page_height;
  }

  //
  // FONTS
  //
  
  function setTextFont()
  {
    $this->setFont($this->text_font_name, $this->text_font_size);
  }
  
  function setDataFont()
  {
    $this->setFont($this->data_font_name, $this->data_font_size);
  }

  function setSectionFont()
  {
    $this->setFont($this->section_font_name, $this->section_font_size);
  }

  function setSubSectionFont()
  {
    $this->setFont($this->subsection_font_name, $this->subsection_font_size);
  }
  
  function setHeaderFont()
  {
    $this->setFont($this->header_font_name, $this->header_font_size);
  }
  
  function setFont($font_name, $font_size)
  {
    //if ($this->font_name == $font_name && $this->font_size == $font_size)
    //  return;
    
    $font = $this->pdf->load_font($font_name, "", "");
    
    $this->font = $font;
    $this->font_name = $font_name;
    $this->font_size = $font_size;
    
    $this->debug("setFont:{$this->font}");
    
    $this->pdf->setfont($this->font, $this->font_size);
  }
  
  //
  // headers and footers
  //
  
  function writeHeaders()
  {
    $this->setHeaderFont();

    $line = ($this->top_margin_width/2) + ($this->font_size/2);
    
    if ($this->header_left_text) {
      $this->writeTextXY($this->getLeftMargin(), 
                         $line, 
                         $this->header_left_text);
    }
    
    if ($this->header_center_text) {
      $this->writeTextXYCenter($this->getPageCenter(), 
                               $line,
                               $this->header_center_text);
    }
    
    if ($this->header_right_text) {
      $this->writeTextXYRight($this->getRightMargin(), 
                              $line,
                              $this->header_right_text);
    }

    $this->setTextFont();
  }
  
  function writeFooters()
  {
    $this->setHeaderFont();
    
    $line = $this->page_height - ($this->bottom_margin_width/2) + ($this->font_size/2);
    
    if ($this->footer_left_text) {
      $this->writeTextXY($this->getLeftMargin(), 
                         $line, 
                         $this->footer_left_text);
    }
    
    if ($this->footer_center_text) {
      $this->writeTextXYCenter($this->getPageCenter(), 
                               $line,
                               $this->footer_center_text);
    } else {
      $this->writeTextXYCenter($this->getPageCenter(), 
                               $line,
                               "Page {$this->page_number}");
    }
    
    if ($this->footer_right_text) {
      $this->writeTextXYRight($this->getRightMargin(), 
                              $line,
                              $this->footer_right_text);
    }
    
    $this->setTextFont();
  }  
  
  //
  // sections
  //

  function writeSection($text, $newPage = true)
  {
    if ($newPage) {
      $this->newPage();
      $this->newLine();
    }      
    
    $this->header_right_text = $text;
    
    $this->setSectionFont();
    $this->writeTextLine($text);

    $this->setTextFont();
    $this->writeHrule();
  }
  
  function writeSubsection($text)
  {
    $this->setSubSectionFont();
    
    $this->newLine();
    $this->writeTextLine($text);
    $this->setTextFont();
    
    $this->writeHrule();
  }
  
  // 
  // text metrics
  //
  
  function getTextWidth($text)
  {
    return $this->pdf->stringwidth($text, $this->font, $this->font_size);
  }
  
  function alignRight($x, $text)
  {
    return $x - $this->getTextWidth($text);
  }  
  
  function alignCenter($x, $text)
  {
    return $x - ($this->getTextWidth($text)/2);
  }
    
  //
  // text writing to current line
  //
  
  function writeText($text)
  {
    return $this->writeTextOpts(array(), $text);
  }
  
  function writeTextLine($text)
  {
    return $this->writeTextOpts(array('newline'=>true), $text);
  }
  
  function writeTextIndent($x, $text)
  {
    return $this->writeTextOpts(array('indent'=>$x), $text);
  }
  
  function writeTextLineIndent($x, $text)
  {
    return $this->writeTextOpts(array('indent'=>$x,'newline'=>true), $text);
  }
  
  function writeTextBlock($text)
  {
    return $this->writeTextOpts(array('newline'=>true,'block'=>true), $text);
  }
  
  function writeTextBlockIndent($x, $text)
  {
    return $this->writeTextOpts(array('newline'=>true,'block'=>true,'indent'=>$x), $text);
  }
  
  function writeTextOpts($opts = array(), $text)
  {
    $x = $opts['x'] ?: 0;
    $indent = $opts['indent'] ?: 0;
    $align = $opts['align'] ?: 'l';
    $width = $opts['width'];
    $newline = $opts['newline'] ?: 0;
    $block = $opts['block'] ?: 0;
    
    $line_count = 0;
    
    if ($block) {
      $lines = preg_split("/[\\n]/", $text);
      $line_count = count($lines);
    }
    
    if ($line_count > 1) {
      $ret = 0;
      for($i=0; $i<$line_count; $i++) {
        $this->debug("RECURS");
        $ret = $this->writeTextOpts(array(
                                   'x'=>$x,
        													 'indent'=>$indent,
                            		   'align'=>$align,
                                   'width'=>$width,
                                   'newline'=>false,
                                   'block'=>false), $lines[$i]);

        if ($newline || $i < ($line_count -1)) {
          $this->debug("<newline>");
          $ret = $this->newLine();
        }
      }
      
      return $ret;
    } else {
      
      if (is_null($width))
        $width = $this->getLineWidth();
        
      $align = substr(strtolower($align), 0, 1);
      
      if(! $x)
        $x = $this->getLeftMargin();
     
      if($align == 'c') {
        $x_pos = $x + ($width/2);
        $x_pos = $this->alignCenter($x_pos, $text) + $indent;
      } else if($align == 'r') {
        $x_pos = $x + $width;
        $x_pos = $this->alignRight($x_pos, $text) + $indent;
      } else {
        $x_pos = $x + $indent;
      }
      
      $point = $this->writeTextXY($x_pos,
                                  $this->text_y,
                                  $text);
                               
      if ($newline)
        $this->newLine();
          
      return $point->x;
    }
  }
  
  //
  // writing column text
  //
  
  function writeTextColumn($width, $align, $text)
  {
    $this->writeTextOpts(array('width'=>$width, 
    	                         'align'=>$align,
                               'x'=>$this->$column_x), $text);
    
    $this->$column_x = $this->$column_x + $width + $this->column_spacing;
  }
  
  function writeTextLineCenter($text)
  {
    $this->writeTextOpts(array('align'=>'c','newline'=>true), $text)
  }
  
  function writeTextLineRight($text)
  {
    $this->writeTextOpts(array('align'=>'r','newline'=>true), $text)
  }
  
  // 
  // text with absolute positioning
  //
  
  function writeTextAbs($opts = array(), $text)
  {
    $origin = $opts['origin'] ?: new Point(0,0);
    $offset = $opts['offset'] ?: new Point(0,0);
    $indent = $opts['indent'] ?: 0;
    $align = $opts['align'] ?: 'l';
    $width = $opts['width'] ?: 0;
    $block = $opts['block'] ?: 0;
    
    $line_count = 0;
    
    if ($block) {
      $lines = preg_split("/[\\n]/", $text);
      $line_count = count($lines);
    }
    
    if ($line_count > 1) {
      $ret = 0;
      for($i=0; $i<$line_count; $i++) {
        $ret = $this->writeTextAbs(array(
                                   'origin'=>$origin,
                                   'offset'=>$offset,
        										       'indent'=>$ident,
                                   'align'=>$align,
                                   'width'=>$width,
                                   'block'=>false), $lines[$i]);
        
        if ($i < ($line_count -1))
          $origin->y += $this->getLineHeight();
      }
      
      return $ret;
    } else {
      
      $align = substr(strtolower($align), 0, 1);
      
      $x = $origin->x + $offset->x;
      $y = $origin->y + $offset->y;
     
      if($align == 'c') {
        $x_pos = $x + ($width/2);
        $x_pos = $this->alignCenter($x_pos, $text) + $indent;
      } else if($align == 'r') {
        $x_pos = $x + $width;
        $x_pos = $this->alignRight($x_pos, $text) + $indent;
      } else {
        $x_pos = $x + $indent;
      }
      
      $y_pos = $y;
      
      return $this->writeTextXY($x_pos,
                                $y_pos,
                                $text);
    }
  }
  
  function writeTextXY($x, $y, $text)
  {
    $this->debug("writeTextXY:$x,$y,$text");
    
    $this->pdf->set_text_pos($x, $this->translateY($y));
    $this->pdf->show($text);
    
    return new Point(($x + $this->getTextWidth($text)), $y);
  } 
  
  function writeTextXYCenter($x, $y, $text)
  {
    return $this->writeTextXY($this->alignCenter($x, $text), $y, $text);
  } 
  
  function writeTextXYRight($x, $y, $text)
  {
    return $this->writeTextXY($this->alignRight($x, $text), $y, $text);
  }
  
  //
  // graphs
  //

  function setGraphRows($rows)
  {
    $this->graph_rows = $rows;
  }

  function setGraphColumns($cols)
  {
    $this->graph_cols = $cols;
  }
  /*
  function graphNextXY()
  {
    if ($this->graph_index == 6) {
      $this->newPage();
    }

    $this->graph_width = (500) / $this->graph_cols;
    $this->graph_height = (660) / $this->graph_rows;

    $x_index = (int) ($this->graph_index % $this->graph_cols);
    $y_index = (int) ($this->graph_index / $this->graph_cols);

    $this->graph_x = 50 + $x_index * $this->graph_width;
    $this->graph_y = 820 - ($y_index + 1) * $this->graph_height;

    $this->graph_index++;
  }

  function drawGraph($name, $gds)
  {
    $this->graphNextXY();
    
    $gd = $this->getDominantGraphData($gds);
    $graph = $this->createGraph($this,
                    $this->graph_x, 
                    $this->graph_y,
                    $this->graph_width - 60,
                    $this->graph_height - 100,
                    name, 
                    $gd);
                    
    $this->createGraphData($gds, $graph);
    
    $graph->drawLegends($gds);
    //$graph->end();
  }
  
  function getDominantGraphData($gds)
  {
    $gdd = $gds[0];
    foreach($gds as $gd) {
      if ($gd->max > $gdd->max) {
        $gdd=$gd;
      }
    }
    return $gdd;
  }
  
  function createGraph(PdfCanvas $canvas,
                       $x, $y,
                       $width, $height,
                       String $title,
                       GraphData $gd,
                       boolean $displayYLabels=true,
                       boolean $trace=false)
  {
    global $g_pdf;
    global $g_start;
    global $g_end;
    global $grey;
    global $lightGrey;
    global $medGrey;
    global $darkGrey;
    global $black;
    global $majorTicks, $minorTicks;
  
    $graph = new Graph($g_pdf, $title,
                       new Point($x, $y),
                       new Size($width, $height),
                       new Range($g_start * 1000, $g_end * 1000),
                       new Range(0, $gd->max),
                       $trace);
    $graph->start();
  
    $valid = $gd->validate();
  
    if ($valid) {
      $graph->canvas->setColor($black);
      $graph->canvas->setFont("Helvetica-Bold", 12);
      $graph->drawTitle($title);
  
      $graph->canvas->setColor($lightGrey);
      $graph->drawGridLines($minorTicks, $gd->yincrement/2);
  
      $graph->canvas->setColor($medGrey);
      $graph->drawGridLines($majorTicks, $gd->yincrement);
  
      $canvas->setColor($darkGrey);
      $graph->drawGrid();
  
      if ($displayYLabels) {
        $graph->drawYGridLabels($gd->yincrement);
      }
  
      $graph->drawXGridLabels($majorTicks, "displayTimeLabel");
    } else {
      debug("Not displaying graph $title because the data was not valid");
      $canvas->setColor($black);
      $canvas->setFont("Helvetica-Bold", 12);
      $graph->drawTitle($title);
      $canvas->setColor($darkGrey);
      $graph->drawGrid();
    }
    
    return $graph;
  }
  
  function createGraphData($name, $data, $color=$blue)
  {
    $dataLine = array();
    $max = -100;
    
    foreach($data as $d) {
      $time = $d->time;
      
      $value_avg = $d->value;
      $value_min = $d->min;
      $value_max = $d->max;
  
      if ($value_min < $value_max) {
        array_push($dataLine, new Point($time, $value_avg));
        array_push($dataLine, new Point($time + 0, $value_max));
        array_push($dataLine, new Point($time + 0, $value_min));
        array_push($dataLine, new Point($time + 0, $value_avg));
      }
      else {
        array_push($dataLine, new Point($time + 0, $value_max));
      }
  
      if ($max < $value_max)
        $max = $value_max;
    }
  
    $gd = new GraphData();
    $gd->name = $name;
    $gd->dataLine = $dataLine;
    $gd->max = $max + ($max * 0.05) ;
    $gd->yincrement = calcYincrement($max);
    $gd->color=$color;
  
    return $gd;
  }
  
  function pdf_graph_draw_lines($gds, $graph)
  {
    global $g_canvas;
  
    $g_canvas->set_line_width(1);
  
    $gds = array_reverse($gds);
  
    foreach($gds as $gd) {
      if ($gd->validate()) {
        $g_canvas->setColor($gd->color);
        
        if (sizeof($gd->dataLine) != 0) {
        	$graph->draw_line_graph($gd->dataLine);
        }
      }
    }
  }
  */
}

class Point 
{
  private $x;
  private $y;

  function Point($x = 0, $y = 0)
  {
    $this->x = (float) $x;
    $this->y = (float) $y;
  }

  function __get($name)
  {
    return $this->$name;
  }  
  
  function __set($name, $value)
  {
    $this->$name = (double) $value;
  }
  
  function __toString()
  {
    return "POINT({$this->x},{$this->y})";
  }
}


class GraphData 
{
  public $name;
  public $dataLine;
  public $max;
  public $yincrement;
  public $color;

  function __toString() {
    return "GraphData name $this->name dataLine $this->dataLine max $this->max yincrement $this->yincrement";
  }

  function validate() {

    if (sizeof($this->dataLine)==0) {
      debug(" no data in " . $this->name);
      return false;
    }

    if ($this->max==0) {
      $this->max=10;
      $this->yincrement=1;
    }
    
    return true;
  }
}

class RGBColor {
  private $red;
  private $green;
  private $blue;

  function RGBColor($red, $green, $blue)
  {
    if ($red > 1)
      $red = $red / 255;
    if ($green > 1)
      $green = $green / 255;
    if ($blue > 1)
      $blue = $blue / 255;
      
    $this->red = $red;
    $this->green = $green;
    $this->blue = $blue;
  }

  function doSetColor($canvas)
  {
    $canvas->setRGBColor($this->red, $this->green, $this->blue);
  } 
}
/*
class Graph {
  private $pixelSize;
  private $xRange;
  private $yRange;
  private $g_canvas;
  private $title;
  private $pixelPerUnit;

  function Graph($pdf,
                 string $title,
                 Point $origin,
                 Size $pixelSize,
                 Range $xRange,
                 Range $yRange,
                 boolean $trace=false)
  {
    $this->title = $title;
    $this->canvas = new Canvas($pdf, $origin);
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
      $this->trace("drawTitle no data" );
      $this->canvas->writeText(new Point($x, $y), $title . " no data");
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

    global $g_canvas;
    global $black;

    $x = $point->x;
    $y = $point->y;

    $g_canvas->setColor($color);
    $this->canvas->moveTo(new Point($x, $y+2.5));
    $this->canvas->lineTo(new Point($x+5, $y+5));
    $this->canvas->lineTo(new Point($x+10, $y+2.5));
    $this->canvas->lineTo(new Point($x+15, $y+2.5));
    $this->canvas->stroke();

    $g_canvas->setColor($black);
    $this->canvas->setFont("Helvetica-Bold", 6);
    $this->canvas->setColor($black);
    $this->canvas->writeText(new Point($x+20, $y), $name);
  }
  
  function draw_line_graph($dataLine)
  {
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

  function drawGrid()
  {
    $this->canvas->set_line_width(1.5);

    $this->trace("drawGrid ");

    $width = (double) $this->pixelSize->width;
    $height = (double) $this->pixelSize->height;
    $this->canvas->moveTo(new Point(0.0, 0.0));
    $this->canvas->lineTo(new Point($width, 0.0));
    $this->canvas->lineTo(new Point($width, $height));
    $this->canvas->lineTo(new Point(0.0, $height));
    $this->canvas->lineTo(new Point(0.0, 0.0));
    $this->canvas->stroke();
  }

  function drawGridLines($xstep, $ystep)
  {
    $this->canvas->set_line_width(0.5);

    if (!$ystep) {
      $this->valid = false;
      debug("No ystep was passed " .  $this->title);
    }

    if (!$this->valid) {
       return;
    }

    $this->trace("drawGridLines ");

    $width = intval($this->pixelSize->width);
    $height = intval($this->pixelSize->height);

    $xstep_width = $xstep * $this->pixelPerUnit->width;
    $ystep_width = $ystep * $this->pixelPerUnit->height;

    if ($xstep_width <= 0.0 || $ystep_width <= 0.0) {
       debug("          ====== Step width was 0 x $xstep_width y $ystep_width " . $this->title);
       debug("      ppu width    " . $this->pixelPerUnit->width);
       debug("      xstep     $xstep ");

       $this->valid = false;

       return;
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

  function drawXGridLabels($xstep, $func)
  {
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
    $height = (double) $this->pixelSize->height;

    $step_width = ($step) * $this->pixelPerUnit->height;

    for ($index = 0; $height >= ($index*$step_width); $index++) {
      $currentYPixel = $index*$step_width;
      $currentYValue =	($index * $step) + $this->yRange->start;
      
      if ($func) {
	$currentLabel = $func($currentYValue);
      } else {
      	if ($currentYValue >      1000000000) {
	   $currentLabel = "" . $currentYValue / 1000000000 . "G";
	}
        elseif ($currentYValue > 1000000) {
	   $currentLabel = "" . $currentYValue / 1000000 . "M";
	}
        elseif ($currentYValue > 1000) {
	   $currentLabel = "" . $currentYValue / 1000 . "K";
	}
        else {
	  $currentLabel = $currentYValue; 
	}
      }

      $x = -5;
      
      $this->canvas->writeText_ralign_xy($x, $currentYPixel - 3,
                                          $currentLabel);
    }    
  }
}
*/

?>
