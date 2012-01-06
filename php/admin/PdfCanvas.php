<?php

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
  
  private $origin;
  private $pdf;
  
  private $page = 0;

  private $text_x;
  private $text_y;
  
  private $font;
  private $font_size;
  
  private $line_spacing = 3;

  private $header_left_text;
  private $header_center_text;
  private $header_right_text;

  private $footer_left_text;
  private $footer_center_text;
  private $footer_right_text;

	private $left_margin = 20;
	private $top_margin = 40;
	private $bottom_margin = 40;
	
  private $page_width = 595;
  private $page_height = 842;
  
  private $section_font_size = 16;
  private $subsection_font_size = 12;
  private $text_font_size = 9;
  private $data_font_size = 8;
  private $header_font_size = 8;

  private $graph_rows = 3;
  private $graph_cols = 2;
  
  private $graph_index;
  
  private $graph_x;
  private $graph_y;
  
  private $graph_width;
  private $graph_height;
  
  function PdfCanvas($pdf, $origin)
  {
    $this->pdf = $pdf;
    $this->origin =  $origin;

    //$this->lastTextPos = new Point(0,0); //to fix problem with Resin PDF Lib clone
    
    $this->newPage(false);
  }
  
  function newPage($end_page = true)
  {
    if ($end_page)
      $this->pdf->end_page();
      
    $this->pdf->begin_page($this->page_width, $this->page_height);
    
    $this->setNormalFont();
    
    $this->text_x = $this->left_margin;
    $this->text_y = $this->page_height - $this->top_margin;
    
    $this->graph_x = 0;
    $this->graph_y = 0;
    $this->graph_index = 0;
    
    $this->writeHeaders();
    $this->writeFooters();
  }
  
  function start()
  {
    $this->pdf->save();
    $this->pdf->translate($this->origin->x, $this->origin->y);
  }

  function end()
  {
    $this->pdf->restore();
  }
  
  function __get($name)
  {
    return $this->$name;
  }  
  
  function __toString()
  {
    return " (CANVAS ORIGIN $origin)";
  }
  
  function stroke()
  {
    $this->pdf->stroke();
  }
  
  //
  // text positioning
  //
  
  function moveTo($point)
  {
    $this->pdf->moveto($point->x, $point->y);
  }

  function lineTo($point)
  {
    $this->pdf->lineto($point->x, $point->y);
  }
  
  function setLineWidth($width)
  {
    // what does this do?
    $this->pdf->setlinewidth($width);
  }
  
  function newLine()
  {
    if ($this->willOverflowY()) {
      $this->newPage();
    } else {
      $this->text_y -= $this->getLineHeight();
    }
  }
  
  function willOverflowY($count = 1)
  {
    $size = $count * $this->getLineHeight();
    return ($this->text_y - $size < $this->bottom_margin);
  }
  
  function getLineHeight()
  {
    $ascender = $this->pdf->get_value("ascender") / 72;
    $descender = $this->descender = $this->pdf->get_value("descender") / 72;
    
    return $this->fontSize + $this->line_spacing + $ascender + $descender;
  }
  
  //
  // FONTS
  //
  
  function setRGBColor($red, $green, $blue)
  {
    $this->pdf->setcolor("fillstroke", "rgb", $red, $green, $blue);
  }
  
  function setNormalFont()
  {
    $this->setFont("Courier", $this->text_font_size);
  }
  
  function setDataFont()
  {
    $this->setFont("Courier", $this->data_font_size);
  }

  function setSectionFont()
  {
    $this->setFont("Times-Bold", $this->section_font_size);
  }

  function setSubSectionFont()
  {
    $this->setFont("Times-Bold", $this->subsection_font_size);
  }
  
  function setHeaderFont()
  {
    $this->setFont("Times-Roman", $this->header_font_size);
  }
  
  function setFont($font_name, $font_size)
  {
    $font = $this->pdf->load_font($font_name, "", "");
    
    $this->font = $font;
    $this->font_size = $font_size;
    
    $this->pdf->setfont($this->font, $this->font_size);
  }
  
  //
  // headers and footers
  //

  function setHeaderLeft($text)
  {
    $this->header_left_text = $text;
  }

  function setHeaderCenter($text)
  {
    $this->header_center_text = $text;
  }

  function setHeaderRight($text)
  {
    $this->header_right_text = $text;
  }

  function setFooterLeft($text)
  {
    $this->footer_left_text = $text;
  }

  function setFooterCenter($text)
  {
    $this->footer_center_text = $text;
  }
  
  function setFooterRight($text)
  {
    $this->footer_right_text = $text;
  }
  
  function writeHeaders()
  {
    $top = $this->page_height - ($this->top_margin / 2);
    
    $this->setHeaderFont();
    
    if ($this->header_left_text) {
      $this->writeText($margin, $top, $this->header_left_text);
    }
    
    if ($this->header_center_text) {
      $this->writeTextCenter($this->width / 2, $top,
                               $this->header_center_text);
    }
    
    if ($this->header_right_text) {
      $this->writeTextRAlign($this->width - $margin, $top,
                               $this->header_right_text);
    }

    $this->setNormalFont();
  }
  
  function writeFooters()
  {
    $bottom = ($this->bottom_margin / 2);
    
    $this->setHeaderFont();
    
    if ($this->footer_left_text) {
      $this->writeText_xy($margin, $bottom, $this->footer_left_text);
    }
    
    $this->page +=1;

    $this->writeTextCenter_xy($this->width / 2, $bottom,
                                "Page $this->page");
    
    if ($this->footer_right_text) {
      $this->writeTextRAlignXy($this->width - $margin, $bottom,
                                  $this->footer_right_text);
    }

    $this->setNormalFont();
  }  
  
  //
  // sections
  //

  function writeSection($text)
  {
    $this->setHeaderRight($text);
    
    $this->newPage();
    
    $this->setSectionFont();
    $this->writeTextLine($text);
    $this->writeHrule();
    
    $this->setNormalFont();
  }
  
  function writeSubsection($text)
  {
    $this->setSubSectionFont();
    
    $this->newLine();
    $this->writeTextLine($text);
    $this->writeHrule();
  }
  
  //
  // text
  //

  function writeText($text)
  {
    $this->writeText(0, $text);
  }
  
  function writeText($x, $text)
  {
    $this->writeText($x, 0, $text);
  }
  
  function writeTextPoint($point, $text)
  {
    $this->writeText($point->x, $point->y, $text);
  }
  
  function writeText($x, $y, $text)
  {
    $this->pdf->set_text_pos($this->text_x + $x, $this->text_y + $y);
    $this->pdf->show($text);
  }
  
  //
  // text with newLine
  //
  
  function writeTextLine($text)
  {
    $this->writeTextLine(0, $text);
  }

  function writeTextLine($x, $text)
  {
    $this->writeTextLine($x, 0 ,$text);
  }
  
  function writeTextLinePoint($point, $text)
  {
    $this->writeTextLine($point->x, $point->y, $text);
  }
  
  function writeTextLine($x, $y, $text)
  {
    $this->writeText($x, $y, $text);
    $this->newLine();
  }
  
  // text blocks
  
  function writeTextBlock($block)
  {
    $this->writeTextBlock(0, $block);
  }

  function writeTextBlock($x, $block)
  {
    $this->writeTextBlock($x, 0, $block);
  }
  
  function writeTextBlock($x, $y, $block)
  {
    $lines = preg_split("/[\\n]/", $block);

    foreach ($lines as $line) {
      $this->writeTextLine($x, $y, $line);
    }
  }
  
  // 
  // text with alignment
  //
  
  function writeTextRAlign($text)
  {
    $this->writeTextRAlign(0, $text);
  }
  
  function writeTextRAlign($x, $text)
  {
    $this->writeTextRAlign($x, 0, $text);
  }
  
  function writeTextRAlign($x, $y, $text)
  {
    $width = $this->pdf->stringwidth($text, $this->font, $this->$font_size);
    
    $this->writeText($x - $width, $y, $text);
  }

  function writeTextCenter($text)
  {
    $this->writeTextCenter(0, $text);
  }
  
  function writeTextCenter($x, $text)
  {
    $this->writeTextCenter($x, 0, $text);
  }
  
  function writeTextCenter($x, $y, $text)
  {
    $width = $this->pdf->stringwidth($text, $this->font, $this->$font_size);
    
    $this->writeText($x - $width / 2, $y, $text);
  }
  
  //
  // hrule
  //
  
  function writeHrule()
  {
    $this->writeHrule(0)
  }
  
  function writeHrule($x)
  {
    $this->pdf->moveto($this->text_x + $x, $this->text_y);
    $this->pdf->lineto($this->page_width - $this->left_margin, $this->text_y);
    
    $this->stroke();
    $this->newLine();
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

  // graphs

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
    $graph->end();
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
  
  function createGraph(Canvas $canvas,
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

?>
