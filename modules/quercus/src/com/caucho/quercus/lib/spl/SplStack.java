package com.caucho.quercus.lib.spl;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;

public class SplStack extends SplDoublyLinkedList
{
	public SplStack() {
		_mode = SplDoublyLinkedList.IT_MODE_LIFO;
  }
	
	/**
	 * Changes the iteration mode. There are two orthogonal sets of modes that can
	 * be set:
	 * 
	 * - The behavior of the iterator (either one or the other) -
	 * SplDoublyLnkedList::IT_MODE_DELETE (Elements are deleted by the iterator) -
	 * SplDoublyLnkedList::IT_MODE_KEEP (Elements are traversed by the iterator)
	 * 
	 * The default mode is 0 : SplDoublyLnkedList::IT_MODE_LIFO |
	 * SplDoublyLnkedList::IT_MODE_KEEP
	 * 
	 * @note The iteration's direction is not modifiable for stack instances
	 * @param $mode
	 *          New mode of iteration
	 * @throw RuntimeException If the new mode affects the iteration's direction.
	 */
	@Override
	public void setIteratorMode(Env env, Value mode) {
		if ((mode.toLong() & SplDoublyLinkedList.IT_MODE_LIFO) != SplDoublyLinkedList.IT_MODE_LIFO) {
			throw new RuntimeException(
			    "Iterators' LIFO/FIFO modes for SplStack/SplQueue objects are frozen");
		}
		super.setIteratorMode(env, mode);
	}
}
