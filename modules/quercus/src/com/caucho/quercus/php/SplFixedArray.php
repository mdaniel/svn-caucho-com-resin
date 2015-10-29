<?php 
/**
 * The SplFixedArray class provides the main functionalities of array. The 
 * main differences between a SplFixedArray and a normal PHP array is that 
 * the SplFixedArray is of fixed length and allows only integers within 
 * the range as indexes. The advantage is that it allows a faster array
 * implementation.
 * @link http://php.net/manual/en/class.splfixedarray.php
 */
class SplFixedArray implements Iterator, Traversable, ArrayAccess, Countable {

        /**
         * Constructs a new fixed array
         * @link http://php.net/manual/en/splfixedarray.construct.php
         * @param int $size [optional]
         * @since 5.3.0
         */
        public function __construct ($size = 0) {}

        /**
         * Returns the size of the array
         * @link http://php.net/manual/en/splfixedarray.count.php
         * @return int the size of the array.
         * @since 5.3.0
         */
        public function count () {}

        /**
         * Returns a PHP array from the fixed array
         * @link http://php.net/manual/en/splfixedarray.toarray.php
         * @return array a PHP array, similar to the fixed array.
         * @since 5.3.0
         */
        public function toArray () {}

        /**
	 * Import a PHP array in a <b>SplFixedArray</b> instance
         * @link http://php.net/manual/en/splfixedarray.fromarray.php
         * @param array $array <p>
         * The array to import.
         * </p>
	 * @param bool $save_indexes [optional] <p>
         * Try to save the numeric indexes used in the original array. 
         * </p>
	 * @return SplFixedArray an instance of <b>SplFixedArray</b>
         * containing the array content.
         * @since 5.3.0
         */
	public static function fromArray (array $array, $save_indexes = true) {}

        /**
         * Gets the size of the array
         * @link http://php.net/manual/en/splfixedarray.getsize.php
         * @return int the size of the array, as an integer.
         * @since 5.3.0
         */
        public function getSize () {}

        /**
         * Change the size of an array
         * @link http://php.net/manual/en/splfixedarray.setsize.php
         * @param int $size <p>
         * The new array size.
         * </p>
         * @return int 
         * @since 5.3.0
         */
        public function setSize ($size) {}

        /**
         * Returns whether the requested index exists
         * @link http://php.net/manual/en/splfixedarray.offsetexists.php
         * @param int $index <p>
         * The index being checked.
         * </p>
	 * @return bool true if the requested <i>index</i> exists, otherwise false
         * @since 5.3.0
         */
        public function offsetExists ($index) {}

        /**
         * Returns the value at the specified index
         * @link http://php.net/manual/en/splfixedarray.offsetget.php
         * @param int $index <p>
         * The index with the value.
         * </p>
	 * @return mixed The value at the specified <i>index</i>.
         * @since 5.3.0
         */
        public function offsetGet ($index) {}

        /**
         * Sets a new value at a specified index
         * @link http://php.net/manual/en/splfixedarray.offsetset.php
         * @param int $index <p>
         * The index being set.
         * </p>
         * @param mixed $newval <p>
	 * The new value for the <i>index</i>.
         * </p>
         * @return void 
         * @since 5.3.0
         */
        public function offsetSet ($index, $newval) {}

        /**
         * Unsets the value at the specified $index
         * @link http://php.net/manual/en/splfixedarray.offsetunset.php
         * @param int $index <p>
         * The index being unset.
         * </p>
         * @return void 
         * @since 5.3.0
         */
        public function offsetUnset ($index) {}

        /**
         * Rewind iterator back to the start
         * @link http://php.net/manual/en/splfixedarray.rewind.php
         * @return void 
         * @since 5.3.0
         */
        public function rewind () {}

        /**
         * Return current array entry
         * @link http://php.net/manual/en/splfixedarray.current.php
         * @return mixed The current element value.
         * @since 5.3.0
         */
        public function current () {}

        /**
         * Return current array index
         * @link http://php.net/manual/en/splfixedarray.key.php
         * @return int The current array index.
         * @since 5.3.0
         */
        public function key () {}

        /**
         * Move to next entry
         * @link http://php.net/manual/en/splfixedarray.next.php
         * @return void 
         * @since 5.3.0
         */
        public function next () {}

        /**
         * Check whether the array contains more elements
         * @link http://php.net/manual/en/splfixedarray.valid.php
         * @return bool true if the array contains any more elements, false otherwise.
         * @since 5.3.0
         */
        public function valid () {}

}
?>