package com.caucho.quercus.lib.spl;

import com.caucho.quercus.QuercusException;
import com.caucho.quercus.UnimplementedException;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.Value;
import com.caucho.util.L10N;

public class SplDoublyLinkedList implements ArrayAccess, Countable, Iterator,
    Traversable
{
	private static L10N L = new L10N(SplDoublyLinkedList.class);

	public static final int IT_MODE_LIFO = 2;
	public static final int IT_MODE_FIFO = 0;
	public static final int IT_MODE_DELETE = 1;
	public static final int IT_MODE_KEEP = 0;

	private SplDoublyLinkedListNode _head;
	private SplDoublyLinkedListNode _tail;
	private SplDoublyLinkedListNode _current;
	private int _count = 0;
	protected int _mode = 0;
	private int _key = 0;

	/**
	 * ( excerpt from http://php.net/manual/en/spldoublylinkedlist.isempty.php )
	 * 
	 * 
	 * @return mixed Returns whether the doubly linked list is empty.
	 */
	public boolean isEmpty(Env env) {
		return _count == 0;
	}

	/**
	 * ( excerpt from http://php.net/manual/en/spldoublylinkedlist.bottom.php )
	 * 
	 * 
	 * @return mixed The value of the first node.
	 */
	public Value bottom(Env env) {
		if (_head == null) {
			throw new QuercusException(L.l("Can't shift from an empty datastructure"));
		}
		return _head.data;
	}

	/**
	 * ( excerpt from http://php.net/manual/en/spldoublylinkedlist.top.php )
	 * 
	 * 
	 * @return mixed The value of the last node.
	 */
	public Value top(Env env) {
		if (_tail == null) {
			throw new QuercusException(L.l("Can't pop from an empty datastructure"));
		}
		return _tail.data;
	}

	/**
	 * ( excerpt from http://php.net/manual/en/spldoublylinkedlist.push.php )
	 * 
	 * Pushes value at the end of the doubly linked list.
	 * 
	 * @value mixed The value to push.
	 * 
	 * @return mixed No value is returned.
	 */
	public void push(Env env, Value value) {
		SplDoublyLinkedListNode node = new SplDoublyLinkedListNode();
		node.data = value;

		if (isEmpty(env)) {
			_head = node;
		} else {
			node.prev = _tail;
			_tail.next = node;
		}
		_tail = node;

		_count++;
	}

	/**
	 * ( excerpt from http://php.net/manual/en/spldoublylinkedlist.pop.php )
	 * 
	 * 
	 * @return mixed The value of the popped node.
	 */
	public Value pop(Env env) {
		Value retval = top(env);
		_tail = _tail.prev;
		_count--;
		return retval;
	}

	/**
	 * ( excerpt from http://php.net/manual/en/spldoublylinkedlist.unshift.php )
	 * 
	 * Prepends value at the beginning of the doubly linked list.
	 * 
	 * @value mixed The value to unshift.
	 * 
	 * @return mixed No value is returned.
	 */
	public void unshift(Env env, Value value) {
		SplDoublyLinkedListNode node = new SplDoublyLinkedListNode();
		node.data = value;

		if (isEmpty(env)) {
			_head = _tail = node;
		} else {
			node.next = _head;
			_head.prev = node;
			_head = node;
		}

		_count++;
	}

	/**
	 * ( excerpt from http://php.net/manual/en/spldoublylinkedlist.shift.php )
	 * 
	 * 
	 * @return mixed The value of the shifted node.
	 */
	public Value shift(Env env) {
		Value retval = bottom(env);
		_head = _head.next;
		_count--;
		return retval;
	}

	/**
	 * ( excerpt from http://php.net/manual/en/spldoublylinkedlist.serialize.php )
	 * 
	 * Serializes the storage. WarningThis function is currently not documented;
	 * only its argument list is available.
	 * 
	 * @return mixed The serialized string.
	 */
	public Value serialize(Env env) {
		throw new UnimplementedException("serialize");
	}

	/**
	 * ( excerpt from http://php.net/manual/en/spldoublylinkedlist.unserialize.php
	 * )
	 * 
	 * Unserializes the storage, from SplDoublyLinkedList::serialize().
	 * WarningThis function is currently not documented; only its argument list is
	 * available.
	 * 
	 * @serialized mixed The serialized string.
	 * 
	 * @return mixed No value is returned.
	 */
	public Value unserialize(Env env, Value serialized) {
		throw new UnimplementedException("unserialize");
	}

	/**
	 * ( excerpt from
	 * http://php.net/manual/en/spldoublylinkedlist.setiteratormode.php )
	 * 
	 * 
	 * @mode mixed There are two orthogonal sets of modes that can be set: The
	 *       direction of the iteration (either one or the other):
	 *       SplDoublyLinkedList::IT_MODE_LIFO (Stack style)
	 *       SplDoublyLinkedList::IT_MODE_FIFO (Queue style) The behavior of the
	 *       iterator (either one or the other):
	 *       SplDoublyLinkedList::IT_MODE_DELETE (Elements are deleted by the
	 *       iterator) SplDoublyLinkedList::IT_MODE_KEEP (Elements are traversed
	 *       by the iterator)
	 * 
	 *       The default mode is: SplDoublyLinkedList::IT_MODE_FIFO |
	 *       SplDoublyLinkedList::IT_MODE_KEEP
	 * 
	 * @return mixed No value is returned.
	 */
	public void setIteratorMode(Env env, Value mode) {
		_mode = (int) mode.toLong();
	}

	/**
	 * ( excerpt from
	 * http://php.net/manual/en/spldoublylinkedlist.getiteratormode.php )
	 * 
	 * 
	 * @return mixed Returns the different modes and flags that affect the
	 *         iteration.
	 */
	public Value getIteratorMode() {
		return LongValue.create(_mode);
	}

	@Override
	public int count(Env env) {
		return _count;
	}

	@Override
	public boolean offsetExists(Env env, Value offset) {
		return offset.toInt() < _count;
	}

	@Override
	public Value offsetGet(Env env, Value offset) {
		if (!offset.isLongConvertible()) {
			throw new QuercusException(L.l("Offset invalid or out of range"));
		}
		SplDoublyLinkedListNode node = _head;
		long index = offset.toLong();
		for (int i = 0; i < index && node != null; i++) {
			node = node.next;
		}
		if (node == null) {
			throw new QuercusException(L.l("Offset invalid or out of range"));
		}
		return node.data;
	}

	@Override
	public Value offsetSet(Env env, Value offset, Value value) {
		SplDoublyLinkedListNode node;
		if (offset == null) {
			offset = LongValue.create(_count);
		}
		if (isEmpty(env)) {
			node = new SplDoublyLinkedListNode();
			_head = node;
			_tail = node;
			_count++;
		}
		node = _head;
		long index = offset.toLong();
		for (int i = 0; i < index; i++) {
			if (node.next == null) {
				_count++;
				node.next = new SplDoublyLinkedListNode();
				node.next.prev = node;
			}
			node = node.next;
		}
		node.data = value;
		return null;
	}

	@Override
	public Value offsetUnset(Env env, Value offset) {
		if (!offset.isLongConvertible()) {
			throw new QuercusException(L.l("Offset invalid or out of range"));
		}
		long index = offset.toLong();
		SplDoublyLinkedListNode node = _head;
		for (int i = 0; i < index; i++) {
			if (node == null) {
				throw new QuercusException(L.l("Offset out of range"));
			}
			node = node.next;
		}

		if (node == null) {
			throw new QuercusException(L.l("Offset out of range"));
		}

		_count--;
		if (node.prev != null) {
			node.prev.next = node.next;
		} else {
			_head = node.next;
		}

		if (node.next != null) {
			node.next.prev = node.prev;
		} else {
			_tail = node.prev;
		}
		return null;
	}

	@Override
	public Value current(Env env) {
		if (!valid(env)) {
			return null;
		}
		return _current.data;
	}

	@Override
	public Value key(Env env) {
		return LongValue.create(_key);
	}

	@Override
	public void next(Env env) {
		if ((_mode & IT_MODE_DELETE) > 0) {
			_count--;
			if (_current.prev != null) {
				_current.prev.next = _current.next;
			}
			if (_current.next != null) {
				_current.next.prev = _current.prev;
			}
		}

		if ((_mode & IT_MODE_LIFO) > 0) {
			_key--;
			_current = _current != null ? _current.prev : null;
		} else {
			if (!((_mode & IT_MODE_DELETE) > 0)) {
				++_key;
			}
			_current = _current != null ? _current.next : null;
		}
	}

	@Override
	public void rewind(Env env) {
		if ((_mode & IT_MODE_LIFO) > 0) {
			_key = _count - 1;
			_current = _tail;
		} else {
			_key = 0;
			_current = _head;
		}
	}

	@Override
	public boolean valid(Env env) {
		return _current != null;
	}

	private class SplDoublyLinkedListNode
	{
		public Value data;
		public SplDoublyLinkedListNode next;
		public SplDoublyLinkedListNode prev;
	}
}
