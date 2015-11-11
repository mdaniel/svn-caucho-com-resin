<?php 
/**
 * This object supports cached iteration over another iterator.
 * @link http://php.net/manual/en/class.cachingiterator.php
 */
class CachingIterator extends IteratorIterator implements OuterIterator, Traversable, Iterator, ArrayAccess, Countable {
    const CALL_TOSTRING = 1;
    const CATCH_GET_CHILD = 16;
    const TOSTRING_USE_KEY = 2;
    const TOSTRING_USE_CURRENT = 4;
    const TOSTRING_USE_INNER = 8;
    const FULL_CACHE = 256;

    private $it;
    private $cache = [];
    private $currentIndex = 0;
    
    /**
     * Construct a new CachingIterator object for the iterator.
     * @link http://php.net/manual/en/cachingiterator.construct.php
     * @param Iterator $iterator
     * @param $flags [optional]
     * @since 5.0
     */
    public function __construct(Iterator $iterator, $flags = self::CALL_TOSTRING) { 
    	$this->it = $iterator;
    }

    /**
     * Rewind the iterator
     * @link http://php.net/manual/en/cachingiterator.rewind.php
     * @return void
     * @since 5.0
     */
    public function rewind() {
    	$this->it->rewind();
    }

    /**
     * Check whether the current element is valid
     * @link http://php.net/manual/en/cachingiterator.valid.php
     * @return bool true on success or false on failure.
     * @since 5.0
     */
    public function valid() { 
    	$this->it->valid();
    }

    /**
     * Return the key for the current element
     * @link http://php.net/manual/en/cachingiterator.key.php
     * @return mixed
     * @since 5.0
     */
    public function key() {
    	$this->it->key();
    }

    /**
     * Return the current element
     * @link http://php.net/manual/en/cachingiterator.current.php
     * @return mixed
     * @since 5.0
     */
    public function current() {
    	$this->it->current();
    }

    /**
     * Move the iterator forward
     * @link http://php.net/manual/en/cachingiterator.next.php
     * @return void
     * @since 5.0
     */
    public function next() {
    	$this->it->next();
    	if ($this->it->valid()) {
    		$this->cache[] = $this->it->current();
    		$currentIndex++;
    	}
    }

    /**
     * Check whether the inner iterator has a valid next element
     * @link http://php.net/manual/en/cachingiterator.hasnext.php
     * @return bool true on success or false on failure.
     * @since 5.0
     */
    public function hasNext() {
    	$this->it->hasNext();
    }

    /**
     * Return the string representation of the current element
     * @link http://php.net/manual/en/cachingiterator.tostring.php
     * @return string The string representation of the current element.
     * @since 5.0
     */
    public function __toString() {
    	$this->it->__toString();
    }

    /**
     * Returns the inner iterator
     * @link http://php.net/manual/en/cachingiterator.getinneriterator.php
     * @return Iterator an object implementing the Iterator interface.
     * @since 5.0
     */
    public function getInnerIterator() { 
    	return $this->it;
    }

    /**
     * Get flags used
     * @link http://php.net/manual/en/cachingiterator.getflags.php
     * @return int Bitmask of the flags
     * @since 5.2.0
     */
    public function getFlags() { }

    /**
     * The setFlags purpose
     * @link http://php.net/manual/en/cachingiterator.setflags.php
     * @param int $flags Bitmask of the flags to set.
     * @return void
     * @since 5.2.0
     */
    public function setFlags($flags) { }

    /**
     * The offsetGet purpose
     * @link http://php.net/manual/en/cachingiterator.offsetget.php
     * @param string $index <p>
     * Description...
     * </p>
     * @return void Description...
     * @since 5.2.0
     */
    public function offsetGet($index) { }

    /**
     * The offsetSet purpose
     * @link http://php.net/manual/en/cachingiterator.offsetset.php
     * @param string $index <p>
     * The index of the element to be set.
     * </p>
     * @param string $newval <p>
     * The new value for the <i>index</i>.
     * </p>
     * @return void
     * @since 5.2.0
     */
    public function offsetSet($index, $newval) { }

    /**
     * The offsetUnset purpose
     * @link http://php.net/manual/en/cachingiterator.offsetunset.php
     * @param string $index <p>
     * The index of the element to be unset.
     * </p>
     * @return void
     * @since 5.2.0
     */
    public function offsetUnset($index) { }

    /**
     * The offsetExists purpose
     * @link http://php.net/manual/en/cachingiterator.offsetexists.php
     * @param string $index <p>
     * The index being checked.
     * </p>
     * @return bool true if an entry referenced by the offset exists, false otherwise.
     * @since 5.2.0
     */
    public function offsetExists($index) { }

    /**
     * The getCache purpose
     * @link http://php.net/manual/en/cachingiterator.getcache.php
     * @return array Description...
     * @since 5.2.0
     */
    public function getCache() {
    	return $this->cache;
	}

    /**
     * The number of elements in the iterator
     * @link http://php.net/manual/en/cachingiterator.count.php
     * @return void The count of the elements iterated over.
     * @since 5.2.2
     */
    public function count() {
    	return $this->it->count();
    }
}