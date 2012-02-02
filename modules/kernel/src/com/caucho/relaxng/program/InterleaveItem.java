/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.relaxng.program;

import java.util.HashSet;
import java.util.Iterator;

import com.caucho.relaxng.RelaxException;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.xml.QName;

/**
 * Generates programs from patterns.
 */
public class InterleaveItem extends Item {
  protected final static L10N L = new L10N(InterleaveItem.class);

  private boolean _allEmpty = true;
  
  private Item []_items = new Item[8];
  private int _size;

  public InterleaveItem()
  {
  }

  public static Item create(Item left, Item right)
  {
    InterleaveItem item = new InterleaveItem();

    item.addItem(left);
    item.addItem(right);
    
    return item.getMin();
  }

  public void addItem(Item item)
  {
    if (item == null) {
      _allEmpty = false;
      return;
    }
    else if (item instanceof EmptyItem) {
      return;
    }
    else if (item instanceof InterleaveItem) {
      InterleaveItem interleave = (InterleaveItem) item;

      for (int i = 0; i < interleave._size; i++) {
        addItem(interleave._items[i]);
      }

      return;
    }

    _allEmpty = false;
    
    while (_items.length <= _size) {
      Item []newItems = new Item[_items.length * 2];
      
      System.arraycopy(_items, 0, newItems, 0, _items.length);
      
      _items = newItems;
    }
    
    _items[_size++] = item;
  }

  public Item getMin()
  {
    if (_size == 0)
      return _allEmpty ? EmptyItem.create() : null;
    else if (_size == 1)
      return _items[0];
    else
      return this;
  }

  /**
   * Returns the first set, the set of element names possible.
   */
  @Override
  public void firstSet(HashSet<QName> set)
  {
    for (int i = 0; i < _size; i++) {
      _items[i].firstSet(set);
    }
  }

  /**
   * Returns the first set, the set of element names possible.
   */
  @Override
  public void requiredFirstSet(HashSet<QName> set)
  {
    if (allowEmpty())
      return;
    
    for (int i = 0; i < _size; i++) {
      _items[i].requiredFirstSet(set);
    }
  }
  
  /**
   * Only allow empty if all allow empty.
   */
  @Override
  public boolean allowEmpty()
  {
    for (int i = 0; i < _size; i++) {
      if (! _items[i].allowEmpty())
        return false;
    }
      
    return true;
  }

  /**
   * Interleaves a continuation.
   */
  @Override
  public Item interleaveContinuation(Item cont)
  {
    InterleaveItem item = new InterleaveItem();

    for (int i = 0; i < _size; i++) {
      item.addItem(_items[i].interleaveContinuation(cont));
    }

    return item.getMin();
  }

  /**
   * Adds an inElement continuation.
   */
  @Override
  public Item inElementContinuation(Item cont)
  {
    InterleaveItem item = new InterleaveItem();

    for (int i = 0; i < _size; i++) {
      item.addItem(_items[i].inElementContinuation(cont));
    }

    return item.getMin();
  }

  /**
   * Adds a group continuation.
   */
  @Override
  public Item groupContinuation(Item cont)
  {
    InterleaveItem item = new InterleaveItem();

    for (int i = 0; i < _size; i++) {
      item.addItem(_items[i].groupContinuation(cont));
    }

    return item.getMin();
  }
    
  /**
   * Return all possible child items or null
   */
  @Override
  public Iterator<Item> getItemsIterator()
  {
    if (_size == 0) {
      return emptyItemIterator();
    }
    else {
      return new ArrayIterator(_items, _size);
    }
  }


  /**
   * Returns the next item on the match.
   */
  @Override
  public Item startElement(QName name)
    throws RelaxException
  {
    Item result = null;
    ChoiceItem choice = null;
    
    int size = _size;
    Item []items = _items;

    for (int i = 0; i < size; i++) {
      Item item = items[i];

      Item nextItem = item.startElement(name);

      if (nextItem == null)
        continue;

      Item resultItem;

      if (nextItem == item) {
        resultItem = this;
      }
      else {
        InterleaveItem rest = new InterleaveItem();
        for (int j = 0; j < size; j++) {
          if (i != j) {
            rest.addItem(items[j]);
          }
        }

        resultItem = nextItem.interleaveContinuation(rest);
      }
      
      if (result == null)
        result = resultItem;
      else {
        if (choice == null) {
          choice = new ChoiceItem();
          choice.addItem(result);
        }
        
        choice.addItem(resultItem);
      }
    }

    if (choice != null)
      return choice.getMin();
    else
      return result;
  }
  
  /**
   * Returns true if the attribute is allowed.
   *
   * @param name the name of the attribute
   * @param value the value of the attribute
   *
   * @return true if the attribute is allowed
   */
  @Override
  public boolean allowAttribute(QName name, String value)
    throws RelaxException
  {
    for (int i = _size - 1; i >= 0; i--)
      if (_items[i].allowAttribute(name, value))
        return true;

    return false;
  }

  /**
   * Returns the first set, the set of attribute names possible.
   */
  @Override
  public void attributeSet(HashSet<QName> set)
  {
    for (int i = 0; i < _size; i++) {
      _items[i].attributeSet(set);
    }
  }
  
  /**
   * Sets an attribute.
   *
   * @param name the name of the attribute
   * @param value the value of the attribute
   *
   * @return the program for handling the element
   */
  @Override
  public Item setAttribute(QName name, String value)
    throws RelaxException
  {
    if (! allowAttribute(name, value))
      return this;

    InterleaveItem interleave = new InterleaveItem();

    for (int i = _size - 1; i >= 0; i--) {
      Item next = _items[i].setAttribute(name, value);

      if (next != null)
        interleave.addItem(next);
    }

    return interleave.getMin();
  }

  /**
   * Returns true if the item can match empty.
   */
  @Override
  public Item attributeEnd()
  {
    InterleaveItem interleave = new InterleaveItem();

    for (int i = _size - 1; i >= 0; i--) {
      Item next = _items[i].attributeEnd();

      if (next == null)
        return null;

      interleave.addItem(next);
    }

    if (interleave.equals(this))
      return this;
    else
      return interleave.getMin();
  }
    
  /**
   * Returns the next item on some text
   */
  @Override
  public Item text(CharSequence string)
    throws RelaxException
  {
    Item result = null;
    ChoiceItem choice = null;
    
    Item []items = _items;

    for (int i = 0; i < _size; i++) {
      Item item = items[i];

      Item nextItem = item.text(string);

      if (nextItem == null)
        continue;

      Item resultItem;

      if (nextItem == item)
        resultItem = this;
      else {
        InterleaveItem rest = new InterleaveItem();
        for (int j = 0; j < _size; j++) {
          if (i != j) {
            rest.addItem(items[j]);
          }
        }

        resultItem = nextItem.interleaveContinuation(rest);
      }
      
      if (result == null)
        result = resultItem;
      else {
        if (choice == null) {
          choice = new ChoiceItem();
          choice.addItem(result);
        }
        choice.addItem(resultItem);
      }
    }

    if (choice != null)
      return choice.getMin();
    else
      return result;
  }
  
  /**
   * Returns true if the element is allowed somewhere in the item.
   * allowsElement is used for error messages to give more information
   * in cases of order dependency.
   *
   * @param name the name of the element
   *
   * @return true if the element is allowed somewhere
   */
  @Override
  public boolean allowsElement(QName name)
  {
    for (int i = 0; i < _size; i++) {
      Item subItem = _items[i];

      if (subItem.allowsElement(name))
        return true;
    }

    return false;
  }

  /**
   * Returns the pretty printed syntax.
   */
  @Override
  public String toSyntaxDescription(int depth)
  {
    if (_size == 1)
      return _items[0].toSyntaxDescription(depth);
    
    CharBuffer cb = CharBuffer.allocate();

    cb.append("(");
    
    boolean isSimple = true;
    for (int i = 0; i < _size; i++) {
      Item item = _items[i];

      if (! item.isSimpleSyntax())
        isSimple = false;
      
      if (i == 0) {
        if (! isSimple)
          cb.append(" ");
      }
      else if (isSimple) {
        cb.append(" & ");
      }
      else {
        addSyntaxNewline(cb, depth);
        cb.append("& ");
      }
      
      cb.append(item.toSyntaxDescription(depth + 2));
    }

    cb.append(')');

    return cb.close();
  }

  /**
   * Returns the hash code for the empty item.
   */
  @Override
  public int hashCode()
  {
    int hash = 37;

    for (int i = 0; i < _size; i++) {
      hash += _items[i].hashCode();
    }

    return hash;
  }

  /**
   * Returns true if the object is an empty item.
   */
  @Override
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    
    if (! (o instanceof InterleaveItem))
      return false;

    InterleaveItem interleave = (InterleaveItem) o;
    
    if (_size != interleave._size) {
      return false;
    }
    
    // return isSubset(interleave) && interleave.isSubset(this);
    return isSubset(interleave);
  }

  private boolean isSubset(InterleaveItem item)
  {
    int size = _size;
    
    /*
    if (size != item._size)
      return false;
      */

    Item []items = _items;
    
    for (int i = size - 1; i >= 0; i--) {
      Item subItem = items[i];

      if (! (item._items[i].equals(subItem) || item.contains(subItem))) {
        return false;
      }
    }

    return true;
  }
  
  private boolean contains(Item subItem)
  {
    Item []items = _items;
    
    for (int i = _size - 1; i >= 0; i--) {
      Item item = items[i];
      
      if (item.equals(subItem))
        return true;
    }
    
    return false;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    
    sb.append("InterleaveItem[");
    for (int i = 0; i < _size; i++) {
      if (i != 0)
        sb.append(", ");
      
      sb.append(_items[i]);
      
    }
    sb.append("]");
    
    return sb.toString();
  }
  
  static class ArrayIterator implements Iterator<Item> {
    private Item []_items;
    private int _size;
    private int _index;
    
    ArrayIterator(Item []items, int size)
    {
      _items = items;
      _size = size;
    }

    @Override
    public boolean hasNext()
    {
      return _index < _size;
    }

    @Override
    public Item next()
    {
      if (_index < _size)
        return _items[_index++];
      else
        return null;
    }

    @Override
    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
}

