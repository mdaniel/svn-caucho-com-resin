<?php

/**
 * This abstract iterator filters out unwanted values for a <b>RecursiveIterator</b>.
 * This class should be extended to implement custom filters.
 * The <b>RecursiveFilterIterator::accept</b> must be implemented in the subclass.
 * @link http://php.net/manual/en/class.recursivefilteriterator.php
 */
abstract class RecursiveFilterIterator extends FilterIterator implements Iterator, Traversable, OuterIterator, RecursiveIterator {

    /**
     * Create a RecursiveFilterIterator from a RecursiveIterator
     * @link http://php.net/manual/en/recursivefilteriterator.construct.php
     * @param RecursiveIterator $iterator
     * @since 5.1.0
     */
    public function __construct(RecursiveIterator $iterator) { 
    	parent::__construct($iterator);
    }

    /**
     * Check whether the inner iterator's current element has children
     * @link http://php.net/manual/en/recursivefilteriterator.haschildren.php
     * @return bool true if the inner iterator has children, otherwise false
     * @since 5.1.0
     */
    public function hasChildren() {
    	return $this->it->hasChildren();
    }

    /**
     * Return the inner iterator's children contained in a RecursiveFilterIterator
     * @link http://php.net/manual/en/recursivefilteriterator.getchildren.php
     * @return RecursiveFilterIterator containing the inner iterator's children.
     * @since 5.1.0
     */
    public function getChildren() { 
    	return new self($this->it->getChildren());
    }
}
