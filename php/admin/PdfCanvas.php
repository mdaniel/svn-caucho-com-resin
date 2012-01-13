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
  private $graph_columns = 3;
  
  private $graph_padding_x = 10;
  private $graph_padding_y = 20;
  private $legend_size = 30;
  
  private $in_graph = false;
  
  private $graph;
  private $graph_index;
  private $graph_origin;
  private $graph_size;
  
  private $is_debug = false; 
  
  public function PdfCanvas()
  {
    $this->pdf = new PDF();
    
    //$this->lastTextPos = new Point(0,0); //to fix problem with Resin PDF Lib clone
    
    $this->pdf->begin_document();
    
    $this->newPage(false);
  }
  
  public function newPage($end_page = true)
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
    
    $this->graph_index = 0;
    $this->graph_x = 0;
    $this->graph_y = 0;
  }

  public function end()
  {
    $this->debug("end");
    
    $this->writeHeaders();
    $this->writeFooters();
    
    $this->debug("end_page");
    $this->pdf->end_page();
    
    $this->debug("end_document");
    $this->pdf->end_document();
  }
  
  public function writeSelfHttp($file_name)
  {
    $buffer = $this->pdf->get_buffer();
    $length = strlen($buffer);
    
    header("Content-Type:application/pdf");
    header("Content-Length:${length}");
    header("Content-Disposition:inline; filename=${file_name}");
    
    echo($buffer);
  }
  
  public function writeSelf()
  {
    echo($this->pdf->get_buffer());
  }
  
  public function __get($name)
  {
    return $this->$name;
  }
  
  public function __set($name, $value)
  {
    $this->$name = $value;
  }
  
  public function __toString()
  {
    return "PdfCanvas()";
  }
  
  public function debug($text) 
  {
    //if ($is_debug)
      System::out->println($text);
  }
  
  // 
  // lines
  //
  
  public function setLineWidth($width)
  {
    $this->pdf->setlinewidth($width);
  }
  
  public function drawLine($p1, $p2)
  {
    $this->pdf->moveto($p1->x, $this->translateY($p1->y));
    $this->pdf->lineto($p2->x, $this->translateY($p2->y));
    $this->pdf->stroke();
  }
  
  public function writeHrule($indent = 0)
  {
    $height = $this->getLineHeight()/2;
    $this->drawLine(new Point($this->getLeftMargin() + $indent, $this->text_y - $height),
                    new Point($this->getRightMargin(), $this->text_y - $height));
    $this->newLine();
  }
  
  // raw graphics
  
  public function setColor($name)
  {
    $this->setRGBColor($this->getRGBColor($name));
  }  
  
  public function setRGBColor(RGBColor $color)
  {
    $this->pdf->setrgbcolor($color->red, $color->green, $color->blue);
  } 

  public function getRGBColor($name)
  {
    $s = strtolower($name);
    $color = $this->$s;
    
    if (! $color) {
      $this->debug("color not found: $name");
      return $this->black;
    } else {
      return $color;
    }
  }
  
  public function moveToPoint($p)
  {
    $this->moveToXY($p->x, $p->y);
  }
  
  public function moveToXY($x, $y)
  {
    $this->debug("moveToXY($x,$y)");
    $this->pdf->moveto($x, $this->translateY($y));
  }
  
  public function lineToPoint($p)
  {
    $this->lineToXY($p->x, $p->y);
  }
  
  public function lineToXY($x, $y)
  {
    $this->debug("lineToXY($x,$y)");
    $this->pdf->lineto($x, $this->translateY($y));
  }
  
  public function stroke()
  {
    $this->pdf->stroke();
  }
  
  //
  // text positioning
  //
  
  public function newLine($count = 1)
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
  
  public function willOverflowY($count = 1)
  {
    $size = $count * $this->getLineHeight();
    return ($this->text_y + $size) > $this->getBottomMargin();
  }
  
  public function getLineHeight()
  {
    //$ascender = $this->pdf->get_value("ascender") / 72;
    //$descender = $this->descender = $this->pdf->get_value("descender") / 72;
    //return $this->font_size + $this->line_spacing + $ascender + $descender;
    
    return $this->font_size + $this->line_spacing;
  }
  
  public function translateY($y)
  {
    // Note:
    // Graphs are drawn bottom to top (0,0 is bottom-left corner)
    // Text is written top to bottom, like a normal word processor
    return ($this->in_graph ? $y : ($this->page_height - $y));
  }
  
  //
  // boundaries
  //
  
  public function setMargins($top, $right, $bottom, $left)
  {
    $this->top_margin_width = $top;
    $this->right_margin_width = $right;
    $this->bottom_margin_width = $bottom;
    $this->left_margin_width = $left;
  }
  
  public function getLeftMargin()
  {
    return $this->left_margin_width;
  }

  public function getRightMargin()
  {
    return $this->getPageWidth() - $this->left_margin_width;
  }
  
  public function getTopMargin()
  {
    return $this->top_margin_width;
  }
  
  public function getBottomMargin()
  {
    return $this->getPageHeight() - $this->bottom_margin_width;
  }  
  
  public function getPageCenter()
  {
    return $this->getPageWidth()/2;
  }
  
  public function getLineCenter()
  {
    return $this->getLineWidth()/2;
  }
  
  public function getLineWidth()
  {
    return $this->getPageWidth() - $this->left_margin_width - $this->right_margin_width;
  }
  
  public function getPageWidth()
  {
    return $this->page_width;
  }
  
  public function getPageHeight()
  {
    return $this->page_height;
  }
  
  // need better name?
  public function getPageRemaining()
  {
    return $this->getPageHeight() - $this->text_y - $this->bottom_margin_width;
  }

  //
  // FONTS
  //
  
  public function setTextFont()
  {
    $this->setFont($this->text_font_name, $this->text_font_size);
  }
  
  public function setDataFont()
  {
    $this->setFont($this->data_font_name, $this->data_font_size);
  }

  public function setSectionFont()
  {
    $this->setFont($this->section_font_name, $this->section_font_size);
  }

  public function setSubSectionFont()
  {
    $this->setFont($this->subsection_font_name, $this->subsection_font_size);
  }
  
  public function setHeaderFont()
  {
    $this->setFont($this->header_font_name, $this->header_font_size);
  }
  
  public function setFont($font_name, $font_size)
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
  
  public function setFontAndColor($font_name, $font_size, $color_name)
  {
    $this->setFont($font_name, $font_size);
    $this->setColor($color_name);
  }
  
  //
  // headers and footers
  //
  
  public function writeHeaders()
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
  
  public function writeFooters()
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

  public function writeSection($text)
  {
    if ($this->page_number > 1)
      $this->newPage();
    
    $this->header_right_text = $text;
    
    $this->setSectionFont();
    $this->writeTextLine($text);

    $this->setTextFont();
    $this->writeHrule();
  }
  
  public function writeSubsection($text)
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
  
  public function getTextWidth($text)
  {
    return $this->pdf->stringwidth($text, $this->font, $this->font_size);
  }
  
  public function alignRight($x, $text)
  {
    return $x - $this->getTextWidth($text);
  }  
  
  public function alignCenter($x, $text)
  {
    return $x - ($this->getTextWidth($text)/2);
  }
    
  //
  // text writing to current line
  //
  
  public function writeText($text)
  {
    return $this->writeTextOpts(array(), $text);
  }
  
  public function writeTextLine($text)
  {
    return $this->writeTextOpts(array('newline'=>true), $text);
  }
  
  public function writeTextIndent($x, $text)
  {
    return $this->writeTextOpts(array('indent'=>$x), $text);
  }
  
  public function writeTextLineIndent($x, $text)
  {
    return $this->writeTextOpts(array('indent'=>$x,'newline'=>true), $text);
  }
  
  public function writeTextBlock($text)
  {
    return $this->writeTextOpts(array('newline'=>true,'block'=>true), $text);
  }
  
  public function writeTextBlockIndent($x, $text)
  {
    return $this->writeTextOpts(array('newline'=>true,'block'=>true,'indent'=>$x), $text);
  }
  
  public function writeTextOpts($opts = array(), $text)
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
  
  public function writeTextColumn($width, $align, $text)
  {
    $this->writeTextOpts(array('width'=>$width, 
    	                         'align'=>$align,
                               'x'=>$this->$column_x), $text);
    
    $this->$column_x = $this->$column_x + $width + $this->column_spacing;
  }
  
  public function writeTextLineCenter($text)
  {
    $this->writeTextOpts(array('align'=>'c','newline'=>true), $text)
  }
  
  public function writeTextLineRight($text)
  {
    $this->writeTextOpts(array('align'=>'r','newline'=>true), $text)
  }
  
  // 
  // text with absolute positioning
  //
  
  public function writeTextAbs($opts = array(), $text)
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
  
  public function writeTextXY($x, $y, $text)
  {
    $this->debug("writeTextXY:$x,$y,$text");
    
    $this->pdf->set_text_pos($x, $this->translateY($y));
    $this->pdf->show($text);
    
    return new Point(($x + $this->getTextWidth($text)), $y);
  } 
  
  public function writeTextXYCenter($x, $y, $text)
  {
    return $this->writeTextXY($this->alignCenter($x, $text), $y, $text);
  } 
  
  public function writeTextXYRight($x, $y, $text)
  {
    return $this->writeTextXY($this->alignRight($x, $text), $y, $text);
  }
  
  //
  // graphs
  //

  public function startGraph($title, $x_range, $y_range)
  {
    // start a new page if this page is full
    if ($this->graph_index == ($this->graph_rows * $this->graph_columns))
      $this->newPage();
      
    /*
    $this->graph_width = (500) / $this->graph_cols;
    $this->graph_height = (660) / $this->graph_rows;

    $x_index = (int) ($this->graph_index % $this->graph_cols);
    $y_index = (int) ($this->graph_index / $this->graph_cols);

    $this->graph_x = 50 + $x_index * $this->graph_width;
    $this->graph_y = 820 - ($y_index + 1) * $this->graph_height;      
    */
      
    // calcuate the vizible size for this graph based on the amount of
    // usable space on the page and the number of graphs per row/column
    $x_size = ($this->getLineWidth() / $this->graph_columns) - ($this->graph_padding_x * 2);
    $y_size = ($this->getPageRemaining() / $this->graph_rows) - ($this->graph_padding_y * 2) - $this->legend_size;
      
    $this->graph_size = new Size($x_size, $y_size);

    // calculate the origin for this graph (bottom-left coordinate)
    $index = $this->graph_index;
                                 
    $x_index = (int) ($index % $this->graph_columns);
    $y_index = (int) ($index / $this->graph_columns);
    
    $this->debug("x_index=$x_index");
    $this->debug("y_index=$y_index");
    
    $x_origin = $this->getLeftMargin() + ($this->graph_padding_x) + ($x_index * ($this->graph_size->width + ($this->graph_padding_x * 2)));
    $y_origin = $this->text_y + $this->graph_padding_y + ($this->graph_size->height + $y_index * ($this->graph_size->height + $this->legend_size + ($this->graph_padding_y * 2)));
    $y_origin = $this->translateY($y_origin);
    
    //($this->graph_padding_y) - ($this->text_y + ($y_index * $this->graph_size->height)) - $this->graph_size->height - (($y_index*2) * $this->graph_padding_y) - ($y_index * $this->legend_size);
    
    //$x_origin += ($this->graph_padding_x / 2);

    $this->graph_origin = new Point($x_origin, $y_origin);

    // set a flag that we are currently writing a graph - this change the 
    // y orientation from top/bottom to bottom/top (see translateY)
    $this->in_graph = true;
    
    // increment the graphs counter for this page
    $this->graph_index++;
                                      
    // save the previous pdf settings (old origin)
    $this->pdf->save();
    
    // tell pdf to translate our coordinates based on the origin of the graph
    //$this->pdf->translate($this->graph_origin->x, $this->translateY($this->graph_origin->y));
    $this->pdf->translate($this->graph_origin->x, $this->graph_origin->y);
                                    
    // construct the new graph
    $this->graph = new PdfCanvasGraph($this,
                                      $title,
                                      $this->graph_size,
                                      $x_range,
                                      $y_range);
                                      
    $this->debug("startGraph:origin={$this->graph_origin},index=$index,graph={$this->graph}");
                                      
    return $this->graph;
  }

  public function endGraph()
  {
    $this->debug("endGraph:{$this->graph->title}");
    
    unset($this->graph);
    
    // restore the old origin
    $this->pdf->restore();
    
    // reset the flat that we are currently writing a graph - this changes the 
    // y orientation from bottom/top to top/bottom (see translateY)
    $this->in_graph = false;
  }
}

class RGBColor 
{
  private $red;
  private $green;
  private $blue;

  public function RGBColor($red, $green, $blue)
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
  
  public function __get($name)
  {
    return $this->$name;
  }
  
  public function __toString()
  {
    return "RGBColor({$this->red},{$this->green},{$this->blue})";
  }
}

class Point 
{
  private $x;
  private $y;

  public function Point($x = 0, $y = 0)
  {
    $this->x = (float) $x;
    $this->y = (float) $y;
  }

  public function __get($name)
  {
    return $this->$name;
  }  
  
  public function __set($name, $value)
  {
    $this->$name = (double) $value;
  }
  
  public function __toString()
  {
    return "Point({$this->x},{$this->y})";
  }
}

class Range 
{
  private $start;
  private $stop;

  public function Range($start, $stop)
  {
    $this->start = (float) $start;
    $this->stop = (float) $stop;
  }

  public function __set($name, $value)
  {
    $this->$name = (double) $value;
  }

  public function __get($name)
  {
    return $this->$name;
  }

  public function __toString()
  {
    return "Range({$this->start},{$this->stop})";
  }

  public function size()
  {
    return $this->stop - $this->start;
  }
}

class Size 
{
  private $width;
  private $height;

  public function Size($width = 0, $height = 0)
  {
    $this->width = $width;
    $this->height = $height;
  }

  public function __set($name, $value)
  {
    $this->$name = (double) $value;
  }

  public function __get($name)
  {
    return $this->$name;
  }

  public function __toString()
  {
    return "Size({$this->width},{$this->height})";
  }
}


?>
