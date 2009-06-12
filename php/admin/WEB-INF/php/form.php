<?php
/**
 * Form processor
 *
 * @author Emil
 */

interface FormItem {
  public function get_root_form();
  public function set_root_form($form);
  public function process_input();
  public function render();
  public function generate_xml();
}

abstract class FormField implements FormItem {
  private $_name;
  private $_label;
  private $_required;
  private $_value;
  private $_form;
  private $_default;
  private $_xml;

  public function __construct($name, $label, $required, $xml, $default)
  {
    $this->_name = $name;
    $this->_label = $label;
    $this->_required = $required;
    $this->_default = $default;
    $this->_xml = $xml;
  }

  public function get_default()
  {
    return $this->_default;
  }

  public function get_root_form()
  {
    return $this->_root_form;
  }

  public function set_root_form($form)
  {
    $this->_root_form = $form;
  }

  public function get_name()
  {
    return $this->_name;
  }

  public function get_label()
  {
    return $this->_label;
  }

  public function is_required()
  {
    return $this->_required;
  }

  public function set_value($value)
  {
    $this->_value = $value;
  }

  public function get_value()
  {
    return $this->_value;
  }

  public function process_input()
  {
    $valid = true;

    if ($_POST[$this->get_name()] != NULL) {
      $this->set_value($_POST[$this->get_name()]);

      if (! $this->validate())
        $valid = false;
      else 
        $this->_root_form->set_field($this->get_name(), $this);
    }
    elseif ($this->is_required()) {
      $valid = false;
    }

    return $valid;
  }

  public function render()
  {
    $class = 'form';

    if ($this->is_required()) {
      $label = $this->_label . '*';

      if ($this->get_root_form()->is_submitted() && ! $this->get_value())
        $class = 'missing-value';
    }
    else 
      $label = $this->_label;

    echo "<tr>\n";
    echo "<th class='{$class}'>{$label}</th>\n";
    echo "<td>" . $this->generate_input_tag() "</td>\n";
    echo "<td>(default: " . $this->get_default() . ")</td>\n";
    echo "</tr>\n";
  }

  public function generate_xml()
  {
    if ($this->get_value())
      return "<{$this->_xml}>{$this->get_value()}</{$this->_xml}>";

    return "";
  }

  protected function validate()
  {
    return true;
  }

  abstract protected function generate_input_tag();
}

class TextField extends FormField {
  private $_max_length;
  private $_initial_value;

  public function __construct($name, $label, $required, $xml, $default = 'N/A',
                              $initial_value = null, $max_length = 0)
  {
    parent::__construct($name, $label, $required, $xml, $default);

    $this->_initial_value = $initial_value;
    $this->_max_length = $max_length;
  }

  protected function get_initial_value()
  {
    return $this->_initial_value;
  }

  protected function validate() 
  {
    if ($this->_max_length == 0)
      return true;

    if (strlen($this->_value) > $this->_max_length)
      return false;

    return true;
  }

  public function generate_input_tag()
  {
    $value = $this->_initial_value;

    if ($this->get_root_form()->is_submitted() && $this->get_value())
      $value = $this->get_value();

    if ($value) {
      echo "<input name='{$this->get_name()}' type='text' value='{$value}'/>";
    }
    else {
      echo "<input name='{$this->get_name()}' type='text'/>";
    }
  }
}

class NumberField extends FormField {
  protected function validate() 
  {
    return is_numeric($this->_value);
  }

  public function generate_input_tag()
  {
    $value = $this->_initial_value;

    if ($this->get_root_form()->is_submitted() && $this->get_value())
      $value = $this->get_value();

    if ($value) {
      echo "<input name='{$this->get_name()}' type='text' value='{$value}'/>";
    }
    else {
      echo "<input name='{$this->get_name()}' type='text'/>";
    }
  }
}

class FileUploadField extends FormField {
  public function generate_input_tag()
  {
    echo "<input name='{$this->get_name()}' type='file'/>";
  }
}

class ChoiceField extends TextField {
  private $_title = NULL;
  private $_choices = array();

  public function set_title($title)
  {
    $this->_title = $title;
  }

  public function add_choice($choice)
  {
    $this->_choices[] = $choice; 
  }

  public function generate_input_tag()
  {
    parent::generate_input_tag();

    $name = $this->get_name();
    $root = $this->get_root_form()->get_name();

    echo "<br/>\n";
    echo "<select name='{$name}_choice' type='text'";
    echo " onChange='document.{$root}.{$name}.value=";
    echo            "document.{$root}.{$name}_choice.options";
    echo              "[document.{$root}.{$name}_choice.selectedIndex].value;'>\n";

    if ($this->_title != NULL) {
      if ($this->get_value() == NULL)
        echo "  <option selected value=''>{$this->_title}</option>\n";
      else
        echo "  <option value=''>{$this->_title}</option>\n";
    }

    foreach ($this->_choices as $choice) {
      if ($this->get_value() == $choice)
        echo "  <option selected value='$choice'>$choice</option>\n";
      else
        echo "  <option value='$choice'>$choice</option>\n";
    }
    echo "</select>\n";
  }
}

class SubForm implements FormItem {
  private $_title;
  private $_name;
  private $_root_form;
  private $_validator;
  private $_show_on_valid_input;
  private $_children;
  private $_field_map;
  private $_xml;

  public function __construct($name, $title, $xml = NULL,
                              $validator = NULL, 
                              $show_on_valid_input = false)
  {
    $this->_name = $name;
    $this->_title = $title;
    $this->_validator = $validator;
    $this->_show_on_valid_input = $show_on_valid_input;
    $this->_field_map = array();
    $this->_children = array();
    $this->_xml = $xml;
  }

  public function add_child($child)
  {
    $this->_children[] = $child;
    $child->set_root_form($this);

    if ($child instanceof SubForm)
      $child->_show_on_valid_input = $this->_show_on_valid_input;
  }

  public function get_title()
  {
    return $this->_title;
  }

  public function get_name()
  {
    return $this->_name;
  }

  public function get_root_form()
  {
    return $this->_root_form;
  }

  public function set_root_form($form)
  {
    $this->_root_form = $form;
    
    foreach ($this->_children as $child) {
      $child->set_root_form($form);
    }
  }

  public function process()
  {
    if ($this->is_submitted()) {
      $valid = $this->process_input($this->get_root_form());

      if ($valid && $this->_validator) {
        if (! call_user_func($this->_validator, $this->_field_map))
          $valid = false;
      }

      if ($valid) {
        if ($this->_show_on_valid_input)
          $this->render();

        return $this->_field_map;
      }
    }

    $this->render();

    return NULL;
  }

  protected function is_submitted()
  {
    return $_POST['form-name'] == $this->_name;
  }

  protected function set_field($name, $field)
  {
    $this->_field_map[$name] = $field;
  }

  protected function process_input()
  {
    $valid = true;

    foreach ($this->_children as $child) {
      if (! $child->process_input())
        $valid = false;
    }

    return $valid;
  }

  protected function render()
  {
    echo "<tr><td colspan='3'>";
    echo "<div class='subform'>\n";

    echo "<div class='form-title'>{$this->get_title()}</div>\n";
    echo "<table class='form'>\n";
    foreach ($this->_children as $child) {
      $child->render();
    }
    echo "</table>\n";

    echo "</div>\n";
    echo "</td></tr>";
  }

  public function generate_xml()
  {
    $output = "";

    if ($this->_xml)
      $output .= "<{$this->_xml}>\n";
    
    $child_output = "";
    foreach ($this->_children as $child) {
      $child_xml = $child->generate_xml();
      
      if ($child_xml)
        $child_output .= $child_xml . "\n";
    }

    if ($child_output) {
      if ($this->_xml) {
        $output .= "  ";
        $child_output = substr($child_output, 0, -1);
        $output .= str_replace("\n", "\n  ", $child_output);
      }
      else {
        $output .= $child_output;
      }
    }

    if ($this->_xml)
      $output .= "\n</{$this->_xml}>";
    else
      $output = rtrim($output);

    return $output;
  }
}

class RootForm extends SubForm {
  public function get_root_form()
  {
    return $this;
  }

  public function set_root_form($form)
  {
    // this is always the root form
  }

  protected function render()
  {
    echo "<form name='{$this->_name}' " .
               "method='POST'>";
               //"enctype='multipart/form-data'>\n";
    echo "<input type='hidden' name='form-name' value='{$this->_name}'/>\n";

    echo "<table class='form'>\n";
    parent::render();

    echo "<tr><td colspan='3'>\n";
    echo "<div class='form-submit'>\n";
    echo "* indicates a required field";
    echo "<input type='submit' value='Generate'/>\n";
    echo "</div>\n";
    echo "</td></tr>\n";
    echo "</table>\n";
    echo "</form>\n";
  }
}

class OptionalForm extends SubForm {
  protected function render()
  {
    echo "<tr><td colspan='3'>";

    if ($_POST[$this->get_name() . "_display"] == "true")
      $display = true;
    else
      $display = false;

    if ($display) 
      echo "<input type='hidden' " .
                  "name='{$this->get_name()}_display' " .
                  "value='true'/>\n";
    else
      echo "<input type='hidden' " .
                  "name='{$this->get_name()}_display' " .
                  "value='false'/>\n";

    if ($display) 
      echo "<div style='display: none' id='{$this->get_name()}_show'>\n";
    else
      echo "<div style='display: block' id='{$this->get_name()}_show'>\n";

    echo "<a href='javascript:show(\"{$this->get_name()}_hidden\")," . 
                             "show(\"{$this->get_name()}_hide\")," .
                             "hide(\"{$this->get_name()}_show\")," .
                             "setValue(\"{$this->get_name()}_display\", \"true\")'>" .
                             //"document.getElementById(\"{$this->get_name()}_display\").value=\"true\"'>" .
                             "show: {$this->get_title()}</a>\n";
    echo "</div>\n";

    if ($display) 
      echo "<div style='display: block' id='{$this->get_name()}_hide'>\n";
    else
      echo "<div style='display: none' id='{$this->get_name()}_hide'>\n";

    echo "<a href='javascript:hide(\"{$this->get_name()}_hidden\")," . 
                             "hide(\"{$this->get_name()}_hide\")," .
                             "show(\"{$this->get_name()}_show\")," .
                             "document.getElementById(\"{$this->get_name()}_display\").value=\"fals\"'>" .
                             "hide: {$this->get_title()}</a>\n";
    echo "</div>\n";

    if ($display) 
      echo "<div style='display: block' id='{$this->get_name()}_hidden'>\n";
    else
      echo "<div style='display: none' id='{$this->get_name()}_hidden'>\n";

    echo "  <div class='subform'>\n";
    echo "  <div class='form-title'>{$this->get_title()}</div>\n";
    echo "  <table class='form'>\n";
    foreach ($this->_children as $child) {
      $child->render();
    }
    echo "  </table>\n";
    echo "  </div>\n";

    echo "</div>\n";
    echo "</td></tr>";
  }
}
