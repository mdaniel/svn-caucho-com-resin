<?php

abstract class FilterIterator extends IteratorIterator
{
  function __construct($iter)
  {
    parent::__construct($iter);
  }
  
  abstract function accept();

  function fetch()
  {
    for (; $this->it->valid() && ! $this->accept(); $this->it->next()) {
    }
  }

  function next()
  {
    parent::next();
    $this->fetch();
  }    

  function rewind()
  {
    parent::rewind();
    $this->fetch();
  }
}
