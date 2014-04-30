<?php

class AppendIterator
  extends IteratorIterator
  implements OuterIterator
{
  var $iters = new ArrayIterator();
  var $index = 0;

  function __construct()
  {
    parent::__construct(NULL);
  }
  
  function append(Iterator $iterator)
  {
    $this->iters[] = $iterator;
  }
  
  function getArrayIterator()
  {
    return $this->iters;
  }

  function getInnerIterator()
  {
    return $this->_getIterator();
  }
  
  function getIteratorIndex()
  {
    $i = $this->index;
    
    if ($i < count($this->iters)) {
      return $i;
    }
    else {
      return NULL;
    }
  }

  function current()
  {
    $iter = $this->_getIterator();
    
    if ($iter == NULL) {
      return NULL;
    }
  
    return $iter->current();
  }

  function key()
  {
    $iter = $this->_getIterator();
    
    if ($iter == NULL) {
      return NULL;
    }
  
    return $iter->key();
  }

  function next()
  {
    $iter = $this->_getIterator();
    
    if ($iter == NULL) {
      return;
    }
    
    $iter->next();
    
    while ($iter != NULL && ! $iter->valid()) {
      $this->index++;
      
      $iter = $this->_getIterator();
    }
  }

  function rewind()
  {  
    $count = $this->iters->count();
  
    for ($i = 0; $i < $count; $i++) {
      $iter = $this->iters[$i];
      
      $iter->rewind();
    }
    
    $this->iters->rewind();
    
    $this->index = 0;
  }

  function valid()
  {
    return $this->_getIterator() != NULL;
  }
  
  function _getIterator()
  {
    if ($this->index < 0) {
      return NULL;
    }
  
    if (count($this->iters) <= $this->index) {
      return NULL;
    }
    
    return $this->iters[$this->index];
  }
}
