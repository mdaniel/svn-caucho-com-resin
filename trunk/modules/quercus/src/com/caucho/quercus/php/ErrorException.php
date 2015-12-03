<?php

class ErrorException extends Exception
{
  protected $severity;

  function __construct($message = "", $code = 0, $severity = 1,
                       $filename = __FILE__, $lineno = __LINE__,
                       $previous = NULL)
  {
    parent::__construct($message, $code, $previous);
    
    $this->severity = $severity;
  }
  
  function getSeverity()
  {
    return $this->severity;
  }
}

?>