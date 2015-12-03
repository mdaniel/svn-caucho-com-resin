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
  public $white = new RGBColor(1, 1, 1);
  
  public $pdf;
  
  public $state_stack = Array();
  
  public $page_number = 0;
  public $has_page = false;

  public $text_y;
  
  public $section_id;
  public $subsection_id;
  
  // state
  public $font;
  public $color;
  public $line_width;
  public $origin;
  
  public $font_name;
  public $font_size;
  
  public $line_spacing = 3;

  public $header_left_text;
  public $header_center_text;
  public $header_right_text;

  public $footer_left_text;
  public $footer_center_text;
  public $footer_right_text;

	public $left_margin_width = 40;
	public $top_margin_width = 40;
	public $bottom_margin_width = 40;
	public $right_margin_width = 40;
	
  public $page_width = 595;
  public $page_height = 842;
  
  public $section_font_name = "Times-Bold";
  public $section_font_size = 14;
  
  public $subsection_font_name = "Times-Bold";
  public $subsection_font_size = 12;

  public $text_font_name = "Helvetica";
  public $text_font_size = 9;
  
  public $data_font_name = "Courier";
  public $data_font_size = 8;
  
  public $header_font_name = "Helvetica";
  public $header_font_size = 8;
  
  public $column_spacing = 5;
  public $column_x;
  public $row_y;
  public $row_max_y;
  public $in_column = false;
  
  public $graph_rows = 3;
  public $graph_columns = 2;
  
  public $graph_space_start_y;
  
  public $graph_padding_x = 10;
  public $graph_padding_y = 20;
  public $legend_size = 35;
  
  public $in_graph = false;
  
  public $graph;
  public $graph_index;
  public $graph_origin;
  public $graph_size;
  
  public function PdfCanvas()
  {
    $this->pdf = new PDF();
    
    //$this->lastTextPos = new Point(0,0); //to fix problem with Resin PDF Lib clone
    
    $this->pdf->begin_document();
    
    $this->newPage();
  }
  
  public function newPage()
  {
    $this->debug("newPage");
    
    if ($this->has_page)
    {
      $this->saveState();
      
      $this->writeHeaders();
      $this->writeFooters();
      
      $this->debug("end_page");
      $this->pdf->end_page();
    }
    
    $this->debug("begin_page");
    $this->pdf->begin_page($this->page_width, $this->page_height);
    
    $this->page_number++;
    
    if (! $this->has_page) {
      $this->has_page = true;

      // these set the base graphics state
      $this->setTextFont();
      $this->setLineWidth(1);
      $this->origin = new Point(0,0);
    } else {
      $this->restoreState();    
    }
    
    $this->text_y = $this->getTopMargin() + $this->font_size;
    $this->row_y = $this->text_y;
    $this->row_max_y = $this->row_y;

    $this->graph_index = 0;
    $this->graph_x = 0;
    $this->graph_y = 0;
    
    $this->graph_space_start_y = $this->text_y;
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
  
  public function __toString()
  {
    return "PdfCanvas()";
  }
  
  public function debug($text) 
  {
    if (preg_match("/^WARNING/", $text))
      System::out->println($text);
  }
  
  public function saveState($pdf_save = false)
  {
    // pdf does not save state between pages
    if ($pdf_save) {
      $this->debug("save");
      $this->pdf->save();
    }
    
    $state = new GraphicsState($this->page_number,
                               $this->font_name,
                               $this->font_size,
                               $this->color,
                               $this->line_width,
                               $this->origin);
    
    
    $stack_size = array_push($this->state_stack, $state);
    
    $this->debug("saveState:stack_size=$stack_size,$state");
  }
  
  public function restoreState($pdf_restore = false)
  {
    // doh!  our pdfstream does not save state between pages!
    if ($pdf_restore) {
      $this->debug("restore");
      $this->pdf->restore();
    }
    
    $state = array_pop($this->state_stack);
    $this->debug("pop state: {$state->id}");
    
    if(! $state) {
      $this->debug("Warning: no saved state to restore!");
    //} elseif ($state->page_number == $this->page_number) {
    //  $this->debug("Skipping internal restore... still on page {$this->page_number}");
    } else {
      $this->debug("restoreState:$state");
      $this->setFontAndColor($state->font_name, $state->font_size, $state->color);
      $this->setLineWidth($state->line_width);
      
      // not sure...
      //$revert_origin = new Point($state->origin->x*-1, $state->origin->y*-1);
      //$this->translate($revert_origin);
    }
  }
  
  public function translate($origin)
  {
    $this->origin = $origin;
    $this->pdf->translate($origin->x, $origin->y);
  }
  
  // 
  // lines
  //
  
  public function setLineWidth($width)
  {
    $this->line_width = line_width;
    $this->pdf->setlinewidth($width);
  }
  
  public function setDash()
  {
    $this->setDashPattern(2, 2);
  }
  
  public function setDashPattern($b, $w)
  {
    $this->pdf->setdash($b, $w);
  }
  
  public function setSolid()
  {
    $this->pdf->setsolid();
  }
  
  public function drawLine($p1, $p2)
  {
    $this->pdf->moveto($p1->x, $this->invertY($p1->y));
    $this->pdf->lineto($p2->x, $this->invertY($p2->y));
    $this->pdf->stroke();
  }
  
  public function drawRectXY($x, $y, $w, $h)
  {
    $this->pdf->rect($x, $this->invertY($y), $w, $h);
    $this->pdf->fill();
  }
  
  public function writeHrule($indent = 0, $line_width = 1, $color="black")
  {
    $this->saveState();
    
    $this->setLineWidth($line_width);
    $this->setColor($color);
    $height = $this->getLineHeight()/2;
    
    $this->drawLine(new Point($this->getLeftMargin() + $indent, $this->text_y - $height),
                    new Point($this->getRightMargin(), $this->text_y - $height));
    $this->newLine();
    
    $this->restoreState();
  }
  
  // raw graphics
  
  public function setFillColor($color)
  {
    if (is_string($color)) {
      $this->color = $this->nameToColor($color);
    } else {
      $this->color = $color;
    }
  
    $this->debug("setFillColor:{$this->color}");
  
    $this->pdf->setrgbcolor_fill($this->color->red,
        $this->color->green,
        $this->color->blue);
  }
  
  public function setColor($color)
  {
    if (is_string($color)) {
      $this->color = $this->nameToColor($color);
    } else {
      $this->color = $color;
    }
    
    $this->debug("setColor:{$this->color}");
    
    $this->pdf->setrgbcolor($this->color->red, 
                            $this->color->green, 
                            $this->color->blue);
  }  
  
  public function nameToColor($name)
  {
    $s = strtolower($name);
    $color = $this->$s;
    
    if (! $color) {
      $this->debug("WARNING:color not found: $name");
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
    $this->pdf->moveto($x, $this->invertY($y));
  }
  
  public function lineToPoint($p)
  {
    $this->lineToXY($p->x, $p->y);
  }
  
  public function lineToXY($x, $y)
  {
    $this->debug("lineToXY($x,$y)");
    $this->pdf->lineto($x, $this->invertY($y));
  }
  
  public function stroke()
  {
    $this->pdf->stroke();
  }
  
  //
  // text positioning
  //
  
  public function newLine()
  {
    if ($this->row_max_y && ! $this->in_column) {
      $this->text_y = $this->row_max_y + $this->getLineHeight();
      $this->row_max_y = 0;
      $this->row_y = 0;
      $this->column_x = $this->getLeftMargin();
    } else {
      $this->text_y += $this->getLineHeight();
    }
      
    if ($this->text_y > $this->invertY($this->bottom_margin_width)) {
      $this->newPage();
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
  
  public function invertY($y)
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
  
  public function setTextFont($size=$this->text_font_size, $color="black")
  {
    $this->setFontAndColor($this->text_font_name, 
                           $size, 
                           $color);
  }
  
  public function setDataFont($size=$this->data_font_size, $color="black")
  {
    $this->setFontAndColor($this->data_font_name, 
                           $size, 
                           $color);
  }

  public function setSectionFont($size=$this->section_font_size, $color="black")
  {
    $this->setFontAndColor($this->section_font_name, 
                           $size, 
                           $color);
  }

  public function setSubSectionFont($size=$this->subsection_font_size, $color="black")
  {
    $this->setFontAndColor($this->subsection_font_name, 
                           $size, 
                           $color);
  }
  
  public function setHeaderFont()
  {
    $this->setFontAndColor($this->header_font_name, 
                           $this->header_font_size, 
                           "black");
  }
  
  public function setFont($font_name, $font_size)
  {
    //if ($this->font_name == $font_name && $this->font_size == $font_size)
    //  return;
    
    $font = $this->pdf->load_font($font_name, "", "");
    if (! $font) {
      $this->debug("WARNING:Font not found: $font_name");
    } else {
      $this->font = $font;
      $this->font_name = $font_name;
      $this->font_size = $font_size;
    
      $this->debug("setFont:$font_name,$font_size");
    
      $this->pdf->setfont($this->font, $this->font_size);
    }
  }
  
  public function setFontAndColor($font_name, $font_size, $color_name)
  {
    if ($font_name) {
      $this->setFont($font_name, $font_size);
    }
    $this->setColor($color_name);
  } 
   
  //
  // headers and footers
  //
  
  public function writeHeaders()
  {
    $this->debug("writeHeaders");
    
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
  }
  
  public function writeFooters()
  {
    $this->debug("writeFooters");
    
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
  }  
  
  //
  // sections
  //

  public function writeSection($text, $new_page = true)
  {
    if ($new_page)
      $this->newPage();
      
    $this->section_id = $this->pdf->add_page_to_outline($text);
    $this->subsection_id = 0;
      
    $this->header_right_text = $text;
    
    $this->saveState();
    $this->setSectionFont();
    $this->writeTextLine($text);

    $this->setTextFont();
    $this->writeHrule(0,2);
    
    $this->restoreState();
  }
  
  public function writeSubsection($text)
  {
    $this->subsection_id = $this->pdf->add_page_to_outline($text, 
                                          $this->invertY($this->text_y), 
                                          $this->section_id);
    
    $this->saveState();
    $this->setSubSectionFont();
    
    $this->newLine();
    $this->writeTextLine($text);
    
    $this->setTextFont();
    $this->writeHrule(0,1);
    
    $this->restoreState();
  }
  
  public function addToOutline($text, $parent_id=0)
  {
    if (! $parent_id) {
      if ($this->subsection_id)
        $parent_id = $this->subsection_id;
      elseif ($this->section_id)
        $parent_id = $this->section_id;
    }
    
    return $this->pdf->add_page_to_outline($text, 
                                           $this->invertY($this->text_y - $this->getLineHeight()), 
                                           $parent_id);
  }
  
  // 
  // text metrics
  //
  
  public function getTextWidth($text)
  {
    return $this->pdf->stringwidth($text, $this->font, $this->font_size);
  }
  
  public function getTextHeight($text)
  {
    return $this->pdf->stringheight($text, $this->font, $this->font_size);
  }
  
  public function getCharsRemaining($indent=0)
  {
    return $this->getCharsInWidth($this->getLineWidth() - $indent);
  }
  
  public function getCharsInWidth($width)
  {
    return $this->pdf->charCount($width, $this->font, $this->font_size);
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
  
  public function writeTextRight($width, $text)
  {
    return $this->writeTextOpts(array('width'=>$width,'align'=>'r'), $text);
  }
  
  public function writeTextCenter($width, $text)
  {
    return $this->writeTextOpts(array('width'=>$width,'align'=>'c'), $text);
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
  
  public function writeTextWrap($text)
  {
    return $this->writeTextOpts(array('wrap'=>true,'newline'=>true), $text);
  }
  
  public function writeTextWrapIndent($x, $text)
  {
    return $this->writeTextOpts(array('wrap'=>true,'indent'=>$x,'newline'=>true), $text);
  }
  
  public function writeTextOpts($opts = array(), $text)
  {
    $x = $opts['x'] ?: 0;
    $indent = $opts['indent'] ?: 0;
    $align = $opts['align'] ?: 'l';
    $width = $opts['width'];
    $newline = $opts['newline'] ?: 0;
    $wrap = $opts['wrap'] ?: 0;
    $block = $opts['block'] ?: 0;
    
    $line_count = 0;
    
    if ($wrap) {
      $wrap_size = $width ? $this->getCharsInWidth($width) : 
                            $this->getCharsRemaining($x + $indent);
      $text = wordwrap($text, $wrap_size, "\n", true);
      $block = true;
    }
    
    if ($block) {
      $lines = preg_split("/[\\n]/", $text);
      $line_count = count($lines);
    }
    
    if ($line_count > 1) {
      $ret = 0;
      for($i=0; $i<$line_count; $i++) {
        $ret = $this->writeTextOpts(array(
                                   'x'=>$x,
        													 'indent'=>$indent,
                            		   'align'=>$align,
                                   'width'=>$width,
                                   'newline'=>false,
                                   'wrap'=>false,
                                   'block'=>false), $lines[$i]);

        if ($newline || $i < ($line_count -1)) {
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
      
      // this is a hack... just in case text_y gets too far down the page
      if ($this->text_y >= $this->getBottomMargin()) {
        $this->newPage();
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
    $this->in_column = true;
     
    if ($this->row_y) {
      $this->text_y = $this->row_y;
    } else {
      $this->row_y = $this->text_y;
    }
    
    if (! $this->column_x)
      $this->column_x = $this->getLeftMargin();
    
    $this->writeTextOpts(array('width'=>$width, 
    	                         'align'=>$align,
                               'wrap'=>true,
                               'x'=>$this->column_x), $text);
    
    $this->column_x = $this->column_x + $width + $this->column_spacing;
    
    if ($this->text_y >= $this->row_max_y)
      $this->row_max_y = $this->text_y;
      
    $this->in_column = false;
  }
  
  public function writeTextColumnHeader($width, $align, $text)
  {
    if ($this->row_y) {
      $this->text_y = $this->row_y;
    } else {
      $this->row_y = $this->text_y;
    }
    
    if (! $this->column_x)
      $this->column_x = $this->getLeftMargin();
    
    $col_x = $this->column_x;
    
    $this->writeTextColumn($width, $align, $text);
    
    $p1 = new Point($col_x, 
                    $this->row_y + $this->getLineHeight()/4);

    $p2 = new Point($col_x + $width, 
                    $this->row_y + $this->getLineHeight()/4);
                    
    $this->drawLine($p1, $p2);
  }
  
  public function writeTextLineCenter($text)
  {
    $this->writeTextOpts(array('align'=>'c','newline'=>true), $text);
  }
  
  public function writeTextLineRight($text)
  {
    $this->writeTextOpts(array('align'=>'r','newline'=>true), $text);
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
    
    if ($wrap) {
      $width = $this->getCharsRemaining($x);
      $text = wordwrap($text, $width, "\n", true);
      $block = true;
    }
    
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
    
    $this->pdf->set_text_pos($x, $this->invertY($y));
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
  
  public function allocateGraphSpace($rows = $this->graph_rows, $columns = $this->graph_columns)
  {
    $this->graph_index = 0;
    
    $this->graph_space_start_y = $this->text_y;
    
    $this->graph_rows = $rows;
    $this->graph_columns = $columns;
    
    // calcuate the vizible size for each graph based on the amount of
    // remaining space on the page and the number of rows and columns
    
    $x_size = ($this->getLineWidth() / $this->graph_columns) - ($this->graph_padding_x * 2);
    $y_size = ($this->getPageRemaining() / $this->graph_rows) - ($this->graph_padding_y * 2) - $this->legend_size;
      
    $this->graph_size = new Size(round($x_size), round($y_size));
    
    $this->debug("allocateGraphSpace:rows={$this->graph_rows},columns={$this->graph_columns},graph_size={$this->graph_size}");
  }

  public function startGraph($title, $x_range, $y_range)
  {
    if(! $this->graph_size)
      $this->allocateGraphSpace();
    
    // start a new page if this page is full
    if ($this->graph_index == ($this->graph_rows * $this->graph_columns))
      $this->newPage();

    // calculate the origin for this graph (bottom-left coordinate)
    $index = $this->graph_index;
                                 
    $x_index = (int) ($index % $this->graph_columns);
    $y_index = (int) ($index / $this->graph_columns);
    
    $x_origin = $this->getLeftMargin() + ($this->graph_padding_x) + ($x_index * ($this->graph_size->width + ($this->graph_padding_x * 2)));
    $y_origin = $this->graph_space_start_y + $this->graph_padding_y + ($this->graph_size->height + $y_index * ($this->graph_size->height + $this->legend_size + ($this->graph_padding_y * 2)));
    $y_origin = $this->invertY($y_origin);

    $this->graph_origin = new Point($x_origin, $y_origin);

    // set a flag that we are currently writing a graph - this change the 
    // y orientation from top/bottom to bottom/top (see invertY)
    $this->in_graph = true;
    
    // increment the graphs counter for this page
    $this->graph_index++;
                                      
    // save the previous pdf settings (old origin)
    $this->saveState(true);
    
    // tell pdf to translate our coordinates based on the origin of the graph
    $this->translate($this->graph_origin);
                                    
    // construct the new graph
    $this->graph = new PdfCanvasGraph($this,
                                      $title,
                                      $this->graph_size,
                                      $x_range,
                                      $y_range);
                                      
    $this->debug("startGraph:origin={$this->graph_origin},index=$index,graph={$this->graph}");
                                      
    if (! $this->graph->valid) {
      $this->debug(" graph $title is invalid:size={$this->graph_size},x_range={$x_range},y_range={$y_range}");
    }
    
    return $this->graph;
  }

  public function endGraph()
  {
    $this->debug("endGraph:{$this->graph->title}");
    
    // restore the old origin
    $this->restoreState(true);
    
    // reset the flag that we are currently writing a graph - this changes the 
    // y orientation from bottom/top to top/bottom (see invertY)
    $this->in_graph = false;
    
    $this->text_y = $this->invertY($this->graph_origin->y - $this->legend_size - $this->graph_padding_y);
    
    unset($this->graph);
  }
}

class RGBColor 
{
  public $red;
  public $green;
  public $blue;

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
  
  public function __toString()
  {
    return "RGBColor({$this->red},{$this->green},{$this->blue})";
  }
}

class Point 
{
  public $x;
  public $y;

  public function Point($x = 0, $y = 0)
  {
    $this->x = (float) $x;
    $this->y = (float) $y;
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
  public $start;
  public $stop;

  public function Range($start, $stop)
  {
    $this->start = (float) $start;
    $this->stop = (float) $stop;
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
  public $width;
  public $height;

  public function Size($width = 0, $height = 0)
  {
    $this->width = $width;
    $this->height = $height;
  }

  public function __toString()
  {
    return "Size({$this->width},{$this->height})";
  }
}

$state_counter = 1;
  
class GraphicsState
{
  public $id;
  
  public $page_number;
  public $font_name;
  public $font_size;
  public $color;
  public $line_width;
  public $origin;
  // there's more we could add but these are the only ones we use right now
  
  public function GraphicsState($page_number, $font_name, $font_size, $color, $line_width, $origin = null)
  {
    global $state_counter;
    
    $this->page_number = $page_number;
    $this->font_name = $font_name;
    $this->font_size = $font_size;
    $this->color = $color;
    $this->line_width = $line_width;
    $this->origin = $origin;
    
    $this->id = $state_counter++;
  }

  public function __toString()
  {
    return "GraphicsState(id={$this->id},page_number={$this->page_number},font_name={$this->font_name},font_size={$this->font_size},color={$this->color},line_width={$this->line_width},origin={$this->origin})";
  }
}


?>
