/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.lib;

import com.caucho.quercus.env.*;
import com.caucho.quercus.env.ArrayValue.AbstractGet;
import com.caucho.quercus.env.ArrayValue.GetKey;
import com.caucho.quercus.env.ArrayValue.KeyComparator;
import com.caucho.quercus.env.ArrayValue.ValueComparator;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.ReadOnly;
import com.caucho.quercus.module.Reference;
import com.caucho.quercus.module.UsesSymbolTable;
import com.caucho.quercus.program.AbstractFunction;
import com.caucho.util.L10N;
import com.caucho.util.RandomUtil;

import java.text.Collator;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PHP array routines.
 */
public class ArrayModule
  extends AbstractQuercusModule
{
  private static final L10N L = new L10N(ArrayModule.class);

  private static final Logger log =
    Logger.getLogger(ArrayModule.class.getName());

  public static final int CASE_UPPER = 2;
  public static final int CASE_LOWER = 1;

  public static final int SORT_REGULAR = 0;
  public static final int SORT_NUMERIC = 1;
  public static final int SORT_STRING = 2;
  public static final int SORT_LOCALE_STRING = 5;
  public static final int SORT_NORMAL = 1;
  public static final int SORT_REVERSE = -1;

  public static final int SORT_DESC = 3;
  public static final int SORT_ASC = 4;
  
  public static final int EXTR_OVERWRITE = 0;
  public static final int EXTR_SKIP = 1;
  public static final int EXTR_PREFIX_SAME = 2;
  public static final int EXTR_PREFIX_ALL = 3;
  public static final int EXTR_PREFIX_INVALID = 4;
  public static final int EXTR_IF_EXISTS = 6;
  public static final int EXTR_PREFIX_IF_EXISTS = 5;
  public static final int EXTR_REFS = 256;

  public static final boolean CASE_SENSITIVE = true;
  public static final boolean CASE_INSENSITIVE = false;
  public static final boolean KEY_RESET = true;
  public static final boolean NO_KEY_RESET = false;
  public static final boolean STRICT = true;
  public static final boolean NOT_STRICT = false;

  private static final CompareString CS_VALUE_NORMAL
    = new CompareString(ArrayValue.GET_VALUE, SORT_NORMAL);
  private static final CompareString CS_VALUE_REVERSE
    = new CompareString(ArrayValue.GET_VALUE, SORT_REVERSE);
  private static final CompareString CS_KEY_NORMAL
    = new CompareString(ArrayValue.GET_KEY, SORT_NORMAL);
  private static final CompareString CS_KEY_REVERSE
    = new CompareString(ArrayValue.GET_KEY, SORT_REVERSE);

  private static final CompareNumeric CN_VALUE_NORMAL
    = new CompareNumeric(ArrayValue.GET_VALUE, SORT_NORMAL);
  private static final CompareNumeric CN_VALUE_REVERSE
    = new CompareNumeric(ArrayValue.GET_VALUE, SORT_REVERSE);
  private static final CompareNumeric CN_KEY_NORMAL
    = new CompareNumeric(ArrayValue.GET_KEY, SORT_NORMAL);
  private static final CompareNumeric CN_KEY_REVERSE
    = new CompareNumeric(ArrayValue.GET_KEY, SORT_REVERSE);

  private static final CompareNormal CNO_VALUE_NORMAL
    = new CompareNormal(ArrayValue.GET_VALUE, SORT_NORMAL);
  private static final CompareNormal CNO_VALUE_REVERSE
    = new CompareNormal(ArrayValue.GET_VALUE, SORT_REVERSE);
  private static final CompareNormal CNO_KEY_NORMAL
    = new CompareNormal(ArrayValue.GET_KEY, SORT_NORMAL);
  private static final CompareNormal CNO_KEY_REVERSE
    = new CompareNormal(ArrayValue.GET_KEY, SORT_REVERSE);

  private static final CompareNatural CNA_VALUE_NORMAL_SENSITIVE
    = new CompareNatural(ArrayValue.GET_VALUE, SORT_NORMAL, CASE_SENSITIVE);
  private static final CompareNatural CNA_VALUE_NORMAL_INSENSITIVE
    = new CompareNatural(ArrayValue.GET_VALUE, SORT_NORMAL, CASE_INSENSITIVE);

  /**
   * Returns true for the mysql extension.
   */
  public String []getLoadedExtensions()
  {
    return new String[] { "standard" };
  }

  /**
   * Changes the key case
   */
  public Value array_change_key_case(ArrayValue array,
                                     @Optional("CASE_LOWER") int toCase)
  {
    if (array == null)
      return BooleanValue.FALSE;

    ArrayValue newArray = new ArrayValueImpl();

    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      Value keyValue = entry.getKey();

      if (keyValue instanceof StringValue) {
        String key = keyValue.toString();

        if (toCase == CASE_UPPER)
          key = key.toUpperCase();
        else
          key = key.toLowerCase();

        newArray.put(new StringValueImpl(key), entry.getValue());
      }
      else
        newArray.put(keyValue, entry.getValue());
    }

    return newArray;
  }

  /**
   * Chunks the array
   */
  public Value array_chunk(Env env,
                           ArrayValue array,
                           int size,
                           @Optional boolean preserveKeys)
  {
    if (array == null)
      return NullValue.NULL;

    ArrayValue newArray = new ArrayValueImpl();
    ArrayValue currentArray = null;

    if (size < 1) {
      env.warning("Size parameter expected to be greater than 0");

      return NullValue.NULL;
    }

    int i = 0;
    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      Value key = entry.getKey();
      Value value = entry.getKey();

      if (i % size == 0) {
        currentArray = new ArrayValueImpl();
        newArray.put(currentArray);
      }

      if (preserveKeys)
        currentArray.put(key, value);
      else
        currentArray.put(new LongValue(i % size), value);

      i++;
    }

    return newArray;
  }

  /**
   * Combines array
   */
  public Value array_combine(Env env, ArrayValue keys,
                             ArrayValue values)
  {
    if (keys == null || values == null)
      return BooleanValue.FALSE;

    if (keys.getSize() < 1 || values.getSize() < 1) {
      env.warning("Both parameters should have at least 1 element");

      return BooleanValue.FALSE;
    }

    if (keys.getSize() != values.getSize()) {
      env.warning("Both parameters should have equal number of elements");

      return BooleanValue.FALSE;
    }

    Iterator<Value> keyIter = keys.values().iterator();
    Iterator<Value> valueIter = values.values().iterator();

    ArrayValue array = new ArrayValueImpl();

    while (keyIter.hasNext() && valueIter.hasNext()) {
      array.put(keyIter.next(), valueIter.next());
    }

    return array;
  }

  /**
   * Counts the values
   */
  public Value array_count_values(Env env, ArrayValue array)
    throws Throwable
  {
    if (array == null)
      return NullValue.NULL;

    ArrayValue result = new ArrayValueImpl();

    for (Value value : array.values()) {
      if (! (value instanceof LongValue) && ! (value instanceof StringValue))
        env.warning("Can only count STRING and INTEGER values!");
      else {
        Value count = result.get(value);

        if (count == null)
          count = new LongValue(1);
        else
          count = count.add(1);

        result.put(value, count);
      }
    }

    return result;
  }

  /**
   * Pops off the top element
   */
  public Value array_pop(Value value)
  {
    if (value instanceof ArrayValue) {
      ArrayValue array = (ArrayValue) value;

      return array.pop();
    }
    else
      return BooleanValue.FALSE;
  }

  /**
   * Returns the size of the array.
   */
  public static Value count(@ReadOnly Value value)
  {
    return new LongValue(value.getSize());
  }

  /**
   * Returns the current value of the array.
   */
  public static Value current(@ReadOnly Value value)
  {
    if (value instanceof ArrayValue) {
      ArrayValue array = (ArrayValue) value;

      return array.current();
    }
    else
      return BooleanValue.FALSE;
  }

  /**
   * Returns the current key of the array.
   */
  public static Value key(@ReadOnly Value value)
  {
    if (value instanceof ArrayValue) {
      ArrayValue array = (ArrayValue) value;

      return array.key();
    }
    else
      return BooleanValue.FALSE;
  }

  /**
   * Returns the current value of the array.
   */
  public static Value pos(@ReadOnly Value value)
  {
    return current(value);
  }

  /**
   * Returns the next value of the array.
   */
  public static Value next(Value value)
  {
    if (value instanceof ArrayValue) {
      ArrayValue array = (ArrayValue) value;

      return array.next();
    }
    else
      return BooleanValue.FALSE;
  }

  /**
   * Returns the next value of the array.
   */
  public static Value each(Value value)
  {
    if (value instanceof ArrayValue) {
      ArrayValue array = (ArrayValue) value;

      return array.each();
    }
    else
      return BooleanValue.FALSE;
  }

  /**
   * Returns the previous value of the array.
   */
  public static Value prev(Value value)
  {
    if (value instanceof ArrayValue) {
      ArrayValue array = (ArrayValue) value;

      return array.prev();
    }
    else
      return BooleanValue.FALSE;
  }

  /**
   * Resets the pointer
   */
  public static Value reset(Value value)
  {
    if (value instanceof ArrayValue) {
      ArrayValue array = (ArrayValue) value;

      return array.reset();
    }
    else
      return BooleanValue.FALSE;
  }

  /**
   * Returns the current value of the array.
   */
  public static Value shuffle(ArrayValue array)
  {
    if (array == null)
      return BooleanValue.FALSE;

    array.shuffle();

    return BooleanValue.TRUE;
  }

  /**
   * Resets the pointer to the end
   */
  public static Value end(Value value)
  {
    if (value instanceof ArrayValue) {
      ArrayValue array = (ArrayValue) value;

      return array.end();
    }
    else
      return BooleanValue.FALSE;
  }

  /**
   * Checks if the key is in the given array
   *
   * @param key a key to check for in the array
   * @param searchArray the array to search for the key in
   * @return true if the key is in the array, and false otherwise
   */
  public static boolean array_key_exists(Env env,
                                         @ReadOnly Value key,
                                         @ReadOnly Value searchArray)
  {
    if (! searchArray.isset() || ! key.isset())
      return false;

    if (!((searchArray instanceof ArrayValue) || (searchArray instanceof ObjectValue))) {
      env.warning(L.l("'" + searchArray.toString() + "' is an unexpected argument, expected ArrayValue or ObjectValue"));
      return false;
    }

    if (!((key instanceof StringValue) || (key instanceof LongValue))) {
      env.warning(L.l(
        "The first argument (a '{0}') should be either a string or an integer",
	key.getType()));
      return false;
    }
    if (searchArray instanceof ArrayValue)
      return ! ((ArrayValue) searchArray).containsKey(key).isNull();
    else
      return ! searchArray.getField(key.toString()).isNull();
  }

  /**
   * Returns an array of the keys in the given array
   *
   * @param array the array to obtain the keys for
   * @param searchValue the corresponding value of the returned key array
   * @return an array containing the keys
   * @throws NullPointerException
   */
  public Value array_keys(Env env,
                          @ReadOnly ArrayValue array,
                          @Optional @ReadOnly Value searchValue,
                          @Optional boolean isStrict)
    throws Throwable
  {
    if (array == null)
      return NullValue.NULL;

    ArrayValue newArray = new ArrayValueImpl(array.getSize());

    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      Value entryValue = entry.getValue();
      Value entryKey = entry.getKey();


      if (searchValue == null || searchValue instanceof DefaultValue)
        newArray.put(entryKey);
      else if (entryValue.eq(searchValue))
        newArray.put(entryKey);
    }

    return newArray;
  }

  /**
   * Returns an array with a number of indices filled with the given value,
   * starting at the start index.
   *
   * @param start the index to start filling the array
   * @param num the number of entries to fill
   * @param value the value to fill the entries with
   * @return an array filled with the given value starting from the given start
   *         index
   */
  public Value array_fill(Env env, long start, long num, Value value)
  {

    if (num < 0) {
      env.warning("Number of elements must be positive");

      return BooleanValue.FALSE;
    }

    ArrayValue array = new ArrayValueImpl();

    for (long k = start; k < num + start; k++)
      array.put(LongValue.create(k), value);

    return array;
  }

  /**
   * Returns an array with the given array's keys as values and its values as
   * keys.  If the given array has matching values, the latest value will be
   * transfered and the others will be lost.
   *
   * @param array the array to flip
   * @return an array with it's keys and values swapped
   */
  public Value array_flip(Env env,
                          ArrayValue array)
  {
    if (array == null)
      return BooleanValue.FALSE;

    ArrayValue newArray = new ArrayValueImpl();

    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      Value entryValue = entry.getValue();

      if ((entryValue instanceof LongValue) ||
          (entryValue instanceof StringValue))
        newArray.put(entryValue, entry.getKey());
      else
        env.warning("Can only flip STRING and INTEGER values!");
    }

    return newArray;
  }

  /**
   * Returns an array with either the front/end padded with the pad value.  If
   * the pad size is positive, the padding is performed on the end.  If
   * negative, then the array is padded on the front.  The pad size is the new
   * array size.  If this size is not greater than the current array size, then
   * the original input array is returned.
   *
   * @param input the array to pad
   * @param padSize the amount to pad the array by
   * @param padValue determines front/back padding and the value to place in the
   * padded space
   * @return a padded array
   */
  public Value array_pad(Env env, ArrayValue input, long padSize,
                         Value padValue)
  {
    if (input == null)
      return NullValue.NULL;

    long inputSize = input.getSize();

    long size = Math.abs(padSize);

    if (input.getSize() >= size)
      return input;

    if (size - inputSize > 1048576) {
      env.warning("You may only pad up to 1048576 elements at a time");

      return BooleanValue.FALSE;
    }

    ArrayValue paddedArray = new ArrayValueImpl();

    boolean padFront = padSize < 0;

    Iterator<Value> keyIterator = input.keySet().iterator();

    for (long ctr = 0; ctr < size; ctr++) {
      Value newValue;

      if (padFront && ctr < size - inputSize)
        newValue = padValue;
      else if ((! padFront) && ctr >= inputSize)
        newValue = padValue;
      else
        newValue = input.get(keyIterator.next());

      paddedArray.put(LongValue.create(ctr), newValue);
    }

    return paddedArray;
  }

  /**
   * Returns an array that filters out any values that do not hold true when
   * used in the callback function.
   *
   * @param array the array to filter
   * @param callback the function name for filtering
   * @return a filtered array
   */
  public Value array_filter(Env env,
                            ArrayValue array,
                            @Optional Callback callback)
  {
    if (array == null)
      return NullValue.NULL;

    ArrayValue filteredArray = new ArrayValueImpl();

    if (callback != null) {

      if (!callback.isValid()) {
        env.warning("The second argument, '" + ((CallbackFunction) callback).getFunctionName() + "', should be a valid callback");
        return NullValue.NULL;
      }

      for (Map.Entry<Value, Value> entry : array.entrySet()) {
        try {
          boolean isMatch = callback.eval(env, entry.getValue()).toBoolean();

          if (isMatch)
            filteredArray.put(entry.getKey(), entry.getValue());
        }
        catch (Throwable t) {
          log.log(Level.WARNING, t.toString(), t);
          env.warning("An error occurred while invoking the filter callback");

          return NullValue.NULL;
        }
      }
    }
    else {

      for (Map.Entry<Value, Value> entry : array.entrySet()) {
        if (entry.getValue().toBoolean())
          filteredArray.put(entry.getKey(), entry.getValue());
      }
    }

    return filteredArray;
  }

  /**
   * Returns the product of the input array's elements as a double.
   *
   * @param array the array for who's product is to be found
   * @return the produce of the array's elements
   */
  public Value array_product(Env env,
                             ArrayValue array)
  {
    if (array == null)
      return NullValue.NULL;

    if (array.getSize() == 0)
      return DoubleValue.create(0);

    double product = 1;

    for (Map.Entry<Value, Value> entry : array.entrySet())
      product *= entry.getValue().toDouble();

    return DoubleValue.create(product);
  }

  /**
   * Appends a value to the array
   *
   * @return the number of elements in the final array
   */
  public int array_push(Env env, ArrayValue array, Value []values)
  {
    for (Value value : values) {
      array.put(value);
    }

    return array.getSize();
  }

  /**
   * Returns num sized array of random keys from the given array
   *
   * @param array the array from which the keys will come from
   * @param num the number of random keys to return
   * @return the produce of the array's elements
   * @throws NullPointerException
   */
  public Value array_rand(Env env,
                          ArrayValue array,
                          @Optional("1") long num)
    throws Throwable
  {
    if (array == null)
      return NullValue.NULL;

    if (array.getSize() == 0)
      return NullValue.NULL;

    if (num < 1 || array.getSize() < num) {
      env.warning("Second argument has to be between 1 and the number of " +
                  "elements in the array");

      return NullValue.NULL;
    }

    long arraySize = array.getSize();

    Value[] keys = new Value[(int) arraySize];

    array.keySet().toArray(keys);

    if (num == 1) {
      int index = (int) (RandomUtil.getRandomLong() % arraySize);

      if (index < 0)
        index *= -1;

      return keys[index];
    }

    int length = keys.length;
    for (int i = 0; i < length; i++) {
      int rand = RandomUtil.nextInt(length);

      Value temp = keys[rand];
      keys[rand] = keys[i];
      keys[i] = temp;
    }

    ArrayValue randArray = new ArrayValueImpl();

    for (int i = 0; i < num; i++) {
      randArray.put(keys[i]);
    }

    return randArray;
  }

  /**
   * Returns the value of the array when its elements have been reduced using
   * the callback function.
   *
   * @param array the array to reduce
   * @param callback the function to use for reducing the array
   * @param initialValue used as the element before the first element of the
   * array for purposes of using the callback function
   * @return the result from reducing the input array with the callback
   *         function
   */
  public Value array_reduce(Env env,
                            ArrayValue array,
                            String callback,
                            @Optional("NULL") Value initialValue)
  {
    if (array == null)
      return NullValue.NULL;

    AbstractFunction func = env.findFunction(callback.intern());

    if (func == null) {
      env.warning("The second argument, '" + callback +
                  "', should be a valid callback");

      return NullValue.NULL;
    }

    Value result = initialValue;

    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      try {
        result = func.eval(env, result, entry.getValue());
      }
      catch (Throwable t) {
        // XXX: may be used for error checking later
        log.log(Level.WARNING, t.toString(), t);
        env.warning("An error occurred while invoking the reduction callback");

        return NullValue.NULL;
      }
    }

    return result;
  }

  /**
   * Returns the inputted array reversed, preserving the keys if keyed is true
   *
   * @param inputArray the array to reverse
   * @param keyed true if the keys are to be preservered
   * @return the array in reverse
   */
  public Value array_reverse(Env env,
                             ArrayValue inputArray,
                             @Optional("false") boolean keyed)
  {
    if (inputArray == null)
      return NullValue.NULL;

    Map.Entry<Value, Value>[] entryArray =
      new Map.Entry[inputArray.getSize()];

    inputArray.entrySet().toArray(entryArray);

    ArrayValue newArray = new ArrayValueImpl();

    int newIndex = 0;

    for (int index = entryArray.length - 1; index > -1; index--) {
      Value currentKey = entryArray[index].getKey();

      Value currentValue = entryArray[index].getValue();

      if (keyed || (currentKey instanceof StringValue))
        newArray.put(currentKey, currentValue);
      else {
        newArray.put(LongValue.create(newIndex), currentValue);

        newIndex++;
      }
    }

    return newArray;
  }

  /**
   * Returns the key of the needle being searched for or false if it's not
   * found
   *
   * @param needle the value to search for
   * @param array the array to search
   * @param strict checks for type aswell
   * @return the key of the needle
   * @throws NullPointerException
   */
  public Value array_search(Env env,
                            @ReadOnly Value needle,
                            @ReadOnly ArrayValue array,
                            @Optional("false") boolean strict)
    throws Throwable
  {
    if (array == null)
      return BooleanValue.FALSE;

    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      Value entryValue = entry.getValue();

      Value entryKey = entry.getKey();

      if (needle.eq(entryValue)) {
        if (strict) {
          if ((entryValue.getType()).equals(needle.getType()))
            return entryKey;
        }
        else
          return entryKey;
      }
    }

    return BooleanValue.FALSE;
  }

  /**
   * Shifts the elements in the array left by one, returning the leftmost value
   *
   * @param array the array to shift
   * @return the left most value in the array
   */
  public Value array_shift(Env env,
                           ArrayValue array)
    throws Throwable
  {
    if (array == null)
      return NullValue.NULL;

    if (array.getSize() < 1)
      return NullValue.NULL;

    Iterator<Value> iterator = array.keySet().iterator();

    Value firstValue = array.remove(iterator.next());

    array.keyReset(0, NOT_STRICT);

    return firstValue;
  }

  /**
   * Returns a chunk of the array.  The offset is the start index, elements is
   * the number of values to take, and presKeys is if the keys are to be
   * preserved. If offset is negative, then it's that number from the end of the
   * array.  If elements is negative, then the new array will have from offset
   * to elements number of values.
   *
   * @param array the array to take the chunk from
   * @param offset the start index for the new array chunk
   * @param elements the number of elements in the array chunk
   * @param presKeys true if the keys of the elements are to be preserved, false
   * otherwise
   * @return the array chunk
   */
  public Value array_slice(Env env,
                           ArrayValue array,
                           long offset,
                           @Optional("NULL") Value elements,
                           @Optional("false") boolean presKeys)
  {
    if (array == null)
      return NullValue.NULL;

    long size = array.getSize();

    long startIndex = offset;

    if (offset < 0)
      startIndex = size + offset;

    long endIndex = size;

    if (elements != NullValue.NULL) {
      endIndex = elements.toLong();

      if (endIndex < 0)
        endIndex += size;
      else
        endIndex += startIndex;
    }

    Iterator<Map.Entry<Value, Value>> iterator = array.entrySet().iterator();

    ArrayValue slicedArray = new ArrayValueImpl();

    for (int k = 0; k < endIndex && iterator.hasNext(); k++) {
      Map.Entry<Value, Value> entry = iterator.next();

      if (k >= startIndex) {
        Value entryKey = entry.getKey();

        Value entryValue = entry.getValue();

        if ((entryKey instanceof StringValue) || presKeys)
          slicedArray.put(entryKey, entryValue);
        else
          slicedArray.put(entryValue);
      }
    }

    return slicedArray;
  }

  /**
   * Returns the removed chunk of the arrayV and splices in replace.  If offset
   * is negative, then the start index is that far from the end.  Otherwise, it
   * is the start index.  If length is not given then from start index to the
   * end is removed.  If length is negative, that is the index to stop removing
   * elements.  Otherwise that is the number of elements to remove.  If replace
   * is given, replace will be inserted into the arrayV at offset.
   *
   * @param array the arrayV to splice
   * @param offset the start index for the new arrayV chunk
   * @param length the number of elements to remove / stop index
   * @param replace the elements to add to the arrayV
   * @return the part of the arrayV removed from input
   */
  public Value array_splice(Env env,
			    ArrayValue array, //array gets spliced at offset
                            int offset,
                            @Optional("NULL") Value length,
                            @Optional ArrayValue replace)
  {
    if (array == null)
      return NullValue.NULL;
    
    int size = array.getSize();

    int startIndex = offset;

    if (startIndex < 0)
      startIndex += size;

    int endIndex = size;

    if (length != NullValue.NULL) {
      endIndex = length.toInt();

      if (endIndex < 0)
        endIndex += size;
      else
        endIndex += startIndex;
    }

    return array.splice(startIndex, endIndex, replace);
  }

  /**
   * Returns the sum of the elements in the array
   *
   * @param array the array to sum
   * @return the sum of the elements
   */
  public Value array_sum(Env env,
                         @ReadOnly ArrayValue array)
  {
    if (array == null)
      return NullValue.NULL;

    double sum = 0;

    for (Map.Entry<Value, Value> entry : array.entrySet())
      sum += entry.getValue().toDouble();

    return DoubleValue.create(sum);
  }

  // XXX: array_udiff
  // XXX: array_udiff_assoc
  // XXX: array_udiff_uassoc

  // XXX: array_uintersect
  // XXX: array_uintersect_assoc
  // XXX: array_uintersect_uassoc

  /**
   * Returns the inputted array without duplicates
   *
   * @param array the array to get rid of the duplicates from
   * @return an array without duplicates
   * @throws ClassCastException
   */
  public Value array_unique(Env env,
                            ArrayValue array)
    throws Throwable
  {
    if (array == null)
      return BooleanValue.FALSE;

    array.sort(CNO_VALUE_NORMAL, NO_KEY_RESET, NOT_STRICT);

    Map.Entry<Value, Value> lastEntry = null;

    ArrayValue uniqueArray = new ArrayValueImpl();

    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      Value entryValue = entry.getValue();

      if (lastEntry == null) {
        uniqueArray.put(entry.getKey(), entryValue);

        lastEntry = entry;

        continue;
      }

      Value lastEntryValue = lastEntry.getValue();

      if (! entryValue.toString().equals(lastEntryValue.toString()))
        uniqueArray.put(entry.getKey(), entryValue);

      lastEntry = entry;
    }

    uniqueArray.sort(CNO_KEY_NORMAL, NO_KEY_RESET, NOT_STRICT);

    return uniqueArray;
  }

  /**
   * Prepends the elements to the array
   *
   * @param array the array to shift
   * @param values
   * @return the left most value in the array
   */
  public Value array_unshift(Env env,
                             ArrayValue array,
                             Value []values)
    throws Throwable
  {
    if (array == null)
      return NullValue.NULL;

    for (int i = values.length - 1; i >= 0; i--) {
      array.unshift(values[i]);
    }

    array.keyReset(0, NOT_STRICT);

    return array;
  }

  /**
   * Returns the values in the passed array with numerical indices.
   *
   * @param array the array to get the values from
   * @return an array with the values of the passed array
   */
  public Value array_values(Env env,
                            ArrayValue array)
  {
    if (array == null)
      return NullValue.NULL;

    ArrayValue arrayValues = new ArrayValueImpl();

    for (Map.Entry<Value, Value> entry : array.entrySet())
      arrayValues.put(entry.getValue());

    return arrayValues;
  }

  /**
   * Recursively executes a callback function on all elements in the array,
   * including elements of elements (i.e., arrays within arrays).  Returns true
   * if the process succeeded, otherwise false.
   *
   * @param array the array to walk
   * @param call the name of the callback function
   * @param extra extra parameter required by the callback function
   * @return true if the walk succedded, false otherwise
   */
  public boolean array_walk_recursive(Env env,
                                      ArrayValue array,
                                      Value call,
                                      @Optional("NULL") Value extra)
  {
    if (array == null)
      return false;

    if (! (call instanceof StringValue)) {
      env.warning("Wrong syntax for function name");

      return false;
    }

    AbstractFunction callback = env.findFunction(call.toString().intern());

    if (callback == null)
      return true;

    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      Value entryValue = entry.getValue();

      if (entryValue instanceof ArrayValue)
        array_walk_recursive(env, (ArrayValue) entryValue, call, extra);
      else {
        try {
          arrayWalkImpl(env, entry, extra, callback);
        }
        catch (Throwable t) {
          log.log(Level.WARNING, t.toString(), t);
          env.warning("An error occured while invoking the callback");

          return false;
        }
      }
    }

    return true;
  }

  /**
   * array_walk_recursive helper function.
   *
   * @param entry the entry to evaluate
   * @param callback the callback function
   */
  private void arrayWalkImpl(Env env, Map.Entry<Value, Value> entry,
                             Value extra, AbstractFunction callback)
    throws Throwable
  {
    callback.eval(env, entry.getValue(), entry.getKey(), extra);
  }

  /**
   * Executes a callback on each of the elements in the array.
   *
   * @param array the array to walk along
   * @param call the name of the callback function
   * @param extra extra parameter required by the callback function
   * @return true if the walk succedded, false otherwise
   */
  public boolean array_walk(Env env,
                            ArrayValue array,
                            Value call,
                            @Optional("NULL") Value extra)
  {
    if (array == null)
      return false;

    if (! (call instanceof StringValue)) {
      env.warning("Wrong syntax for function name");

      return false;
    }

    AbstractFunction callback = env.findFunction(call.toString().intern());

    if (callback == null)
      return true;

    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      try {
        arrayWalkImpl(env, entry, extra, callback);
      }
      catch (Throwable t) {
        // XXX: may be used later for error implementation
        log.log(Level.WARNING, t.toString(), t);
        env.warning("An error occured while invoking the callback");

        return false;
      }
    }

    return true;
  }

  /**
   * Sorts the array based on values in reverse order, preserving keys
   *
   * @param array the array to sort
   * @param sortFlag provides optional methods to process the sort
   * @return true if the sort works, false otherwise
   * @throws ClassCastException if the elements are not mutually comparable
   */
  public boolean arsort(Env env, ArrayValue array,
                        @Optional long sortFlag)
    throws Throwable
  {
    if (array == null)
      return false;

    switch ((int) sortFlag) {
    case SORT_STRING:
      array.sort(CS_VALUE_REVERSE, NO_KEY_RESET, NOT_STRICT);
      break;
    case SORT_NUMERIC:
      array.sort(CN_VALUE_REVERSE, NO_KEY_RESET, NOT_STRICT);
      break;
    case SORT_LOCALE_STRING:
      Locale locale = env.getLocaleInfo().getCollate();
      array.sort(new CompareLocale(ArrayValue.GET_VALUE, SORT_REVERSE,
                                   Collator.getInstance(locale)),
                 NO_KEY_RESET, NOT_STRICT);
      break;
    default:
      array.sort(CNO_VALUE_REVERSE, NO_KEY_RESET, NOT_STRICT);
      break;
    }

    return true;
  }

  /**
   * Sorts the array based on values in ascending order, preserving keys
   *
   * @param array the array to sort
   * @param sortFlag provides optional methods to process the sort
   * @return true if the sort works, false otherwise
   * @throws ClassCastException if the elements are not mutually comparable
   */
  public boolean asort(Env env, ArrayValue array,
                       @Optional long sortFlag)
    throws Throwable
  {
    if (array == null)
      return false;

    switch ((int) sortFlag) {
    case SORT_STRING:
      array.sort(CS_VALUE_NORMAL, NO_KEY_RESET, NOT_STRICT);
      break;
    case SORT_NUMERIC:
      array.sort(CN_VALUE_NORMAL, NO_KEY_RESET, NOT_STRICT);
      break;
    case SORT_LOCALE_STRING:
      Locale locale = env.getLocaleInfo().getCollate();
      array.sort(new CompareLocale(ArrayValue.GET_VALUE, SORT_NORMAL,
                                   Collator.getInstance(locale)),
                 NO_KEY_RESET, NOT_STRICT);
      break;
    default:
      array.sort(CNO_VALUE_NORMAL, NO_KEY_RESET, NOT_STRICT);
      break;
    }

    return true;
  }

  /**
   * Sorts the array based on keys in ascending order, preserving keys
   *
   * @param array the array to sort
   * @param sortFlag provides optional methods to process the sort
   * @return true if the sort works, false otherwise
   * @throws ClassCastException if the elements are not mutually comparable
   */
  public boolean ksort(Env env, ArrayValue array,
                       @Optional long sortFlag)
    throws Throwable
  {
    if (array == null)
      return false;

    switch ((int) sortFlag) {
    case SORT_STRING:
      array.sort(CS_KEY_NORMAL, NO_KEY_RESET, NOT_STRICT);
      break;
    case SORT_NUMERIC:
      array.sort(CN_KEY_NORMAL, NO_KEY_RESET, NOT_STRICT);
      break;
    case SORT_LOCALE_STRING:
      Locale locale = env.getLocaleInfo().getCollate();
      array.sort(new CompareLocale(ArrayValue.GET_KEY, SORT_NORMAL,
                                   Collator.getInstance(locale)),
                 NO_KEY_RESET, NOT_STRICT);
      break;
    default:
      array.sort(CNO_KEY_NORMAL, NO_KEY_RESET, NOT_STRICT);
      break;
    }

    return true;
  }

  /**
   * Sorts the array based on keys in reverse order, preserving keys
   *
   * @param array the array to sort
   * @param sortFlag provides optional methods to process the sort
   * @return true if the sort works, false otherwise
   * @throws ClassCastException if the elements are not mutually comparable
   */
  public boolean krsort(Env env, ArrayValue array,
                        @Optional long sortFlag)
    throws Throwable
  {
    if (array == null)
      return false;

    switch ((int) sortFlag) {
    case SORT_STRING:
      array.sort(CS_KEY_REVERSE, NO_KEY_RESET, NOT_STRICT);
      break;
    case SORT_NUMERIC:
      array.sort(CN_KEY_REVERSE, NO_KEY_RESET, NOT_STRICT);
      break;
    case SORT_LOCALE_STRING:
      Locale locale = env.getLocaleInfo().getCollate();
      array.sort(new CompareLocale(ArrayValue.GET_KEY, SORT_REVERSE,
                                   Collator.getInstance(locale)),
                 NO_KEY_RESET, NOT_STRICT);
      break;
    default:
      array.sort(CNO_KEY_REVERSE, NO_KEY_RESET, NOT_STRICT);
      break;
    }

    return true;
  }

  /**
   * Sorts the array based on string values using natural order, preserving
   * keys, case sensitive
   *
   * @param array the array to sort
   * @return true if the sort works, false otherwise
   * @throws ClassCastException if the elements are not mutually comparable
   */
  public Value natsort(ArrayValue array)
    throws Throwable
  {
    if (array == null)
      return NullValue.NULL;

    trimArrayStrings(array);

    array.sort(CNA_VALUE_NORMAL_SENSITIVE, NO_KEY_RESET, NOT_STRICT);

    return BooleanValue.TRUE;
  }

  /**
   * Sorts the array based on string values using natural order, preserving
   * keys, case insensitive
   *
   * @param array the array to sort
   * @return true if the sort works, false otherwise
   * @throws ClassCastException if the elements are not mutually comparable
   */
  public Value natcasesort(ArrayValue array)
    throws Throwable
  {
    if (array == null)
      return NullValue.NULL;

    trimArrayStrings(array);

    array.sort(CNA_VALUE_NORMAL_INSENSITIVE, NO_KEY_RESET, NOT_STRICT);

    return BooleanValue.TRUE;
  }

  /**
   * Helper function for natsort and natcasesort to trim the string in the
   * array
   *
   * @param array the array to trim strings from
   */
  private void trimArrayStrings(ArrayValue array)
  {
    if (array != null) {

      for (Map.Entry<Value, Value> entry : array.entrySet()) {
        Value entryValue = entry.getValue();

        if (entryValue instanceof StringValue)
          array.put(entry.getKey(),
                    StringValue.create(entryValue.toString().trim()));
      }
    }
  }

  // XXX: compact

  /**
   * Determines if the key is in the array
   *
   * @param needle the array to sort
   * @return true if the sort works, false otherwise
   * @throws ClassCastException if the elements are not mutually comparable
   */
  public boolean in_array(@ReadOnly Value needle,
                          @ReadOnly ArrayValue stack,
                          @Optional("false") boolean strict)
    throws Throwable
  {
    if (stack == null)
      return false;

    if (strict)
      return stack.containsStrict(needle) != NullValue.NULL;
    else
      return stack.contains(needle) != NullValue.NULL;
  }

  /**
   * Sorts the array based on values in ascending order
   *
   * @param array the array to sort
   * @param sortFlag provides optional methods to process the sort
   * @return true if the sort works, false otherwise
   * @throws ClassCastException if the elements are not mutually comparable
   */
  public boolean sort(Env env, ArrayValue array, @Optional long sortFlag)
    throws Throwable
  {
    if (array == null)
      return false;

    switch ((int) sortFlag) {
    case SORT_STRING:
      array.sort(CS_VALUE_NORMAL, KEY_RESET, STRICT);
      break;
    case SORT_NUMERIC:
      array.sort(CN_VALUE_NORMAL, KEY_RESET, STRICT);
      break;
    case SORT_LOCALE_STRING:
      Locale locale = env.getLocaleInfo().getCollate();
      array.sort(new CompareLocale(ArrayValue.GET_VALUE, SORT_NORMAL,
                                   Collator.getInstance(locale)),
                 KEY_RESET, STRICT);
      break;
    default:
      array.sort(CNO_VALUE_NORMAL, KEY_RESET, STRICT);
      break;
    }

    return true;
  }

  /**
   * Sorts the array based on values in reverse order
   *
   * @param array the array to sort
   * @param sortFlag provides optional methods to process the sort
   * @return true if the sort works, false otherwise
   * @throws ClassCastException if the elements are not mutually comparable
   */
  public boolean rsort(Env env, ArrayValue array, @Optional long sortFlag)
    throws Throwable
  {
    if (array == null)
      return false;

    switch ((int) sortFlag) {
    case SORT_STRING:
      array.sort(CS_VALUE_REVERSE, KEY_RESET, STRICT);
      break;
    case SORT_NUMERIC:
      array.sort(CN_VALUE_REVERSE, KEY_RESET, STRICT);
      break;
    case SORT_LOCALE_STRING:
      Locale locale = env.getLocaleInfo().getCollate();
      array.sort(new CompareLocale(ArrayValue.GET_VALUE, SORT_REVERSE,
                                   Collator.getInstance(locale)),
                 KEY_RESET, STRICT);
      break;
    default:
      array.sort(CNO_VALUE_REVERSE, KEY_RESET, STRICT);
      break;
    }

    return true;
  }

  /**
   * Sorts the array based on values in ascending order using a callback
   * function
   *
   * @param array the array to sort
   * @param func the name of the callback function
   * @param sortFlag provides optional methods to process the sort
   * @return true if the sort works, false otherwise
   * @throws ClassCastException if the elements are not mutually comparable
   */
  public boolean usort(Env env,
                       ArrayValue array,
                       Callback func,
                       @Optional long sortFlag)
    throws Throwable
  {
    if (array == null)
      return false;

    if (!func.isValid()) {
      env.warning(L.l("Invalid comparison function"));
      return false;
    }

    CompareCallBack cmp;

    cmp = new CompareCallBack(ArrayValue.GET_VALUE, SORT_NORMAL, func, env);

    array.sort(cmp, KEY_RESET, STRICT);

    return true;
  }

  /**
   * Sorts the array based on values in ascending order using a callback
   * function
   *
   * @param array the array to sort
   * @param func the name of the callback function
   * @param sortFlag provides optional methods to process the sort
   * @return true if the sort works, false otherwise
   * @throws ClassCastException if the elements are not mutually comparable
   */
  public boolean uasort(Env env,
                        ArrayValue array,
                        Callback func,
                        @Optional long sortFlag)
    throws Throwable
  {
    if (array == null)
      return false;

    if (!func.isValid()) {
      env.warning(L.l("Invalid comparison function"));
      return false;
    }

    array.sort(new CompareCallBack(ArrayValue.GET_VALUE, SORT_NORMAL, func,
                                   env), NO_KEY_RESET, NOT_STRICT);

    return true;
  }

  /**
   * Sorts the array based on values in ascending order using a callback
   * function
   *
   * @param array the array to sort
   * @param func the name of the callback function
   * @param sortFlag provides optional methods to process the sort
   * @return true if the sort works, false otherwise
   * @throws ClassCastException if the elements are not mutually comparable
   */
  public boolean uksort(Env env,
                        ArrayValue array,
                        Callback func,
                        @Optional long sortFlag)
    throws Throwable
  {
    if (array == null)
      return false;

    if (!func.isValid()) {
      env.warning(L.l("Invalid comparison function"));
      return false;
    }

    CompareCallBack cmp;

    cmp = new CompareCallBack(ArrayValue.GET_KEY, SORT_NORMAL, func, env);

    array.sort(cmp, NO_KEY_RESET, NOT_STRICT);

    return true;
  }

  /**
   * Creates an array using the start and end values provided
   *
   * @param start the 0 index element
   * @param end the length - 1 index element
   * @param step the new value is increased by this to determine the value for
   * the next element
   * @return the new array
   */
  public Value range(Env env,
                     @ReadOnly Value start,
                     @ReadOnly Value end,
                     @Optional("1") long step)
    throws Throwable
  {
    if (step < 1)
      step = 1;

    if (!start.getType().equals(end.getType())) {
      start = LongValue.create(start.toLong());
      end = LongValue.create(end.toLong());
    }
    else if (Character.isDigit(start.toChar())) {
      start = LongValue.create(start.toLong());
      end = LongValue.create(end.toLong());
    }
    else {
      start = rangeIncrement(start, 0);
      end = rangeIncrement(end, 0);
    }

    if (start.eq(end)) {
    }
    else if (start instanceof StringValue &&
             (Math.abs(end.toChar() - start.toChar()) < step)) {
      env.warning("steps exceeds the specified range");

      return BooleanValue.FALSE;
    }
    else if (start instanceof LongValue &&
             (Math.abs(end.toLong() - start.toLong()) < step)) {
      env.warning("steps exceeds the specified range");

      return BooleanValue.FALSE;
    }

    boolean increment = true;

    if (! end.geq(start)) {
      step *= -1;
      increment = false;
    }

    ArrayValue array = new ArrayValueImpl();

    do {
      array.put(start);

      start = rangeIncrement(start, step);
    } while ((increment && start.leq(end)) ||
             (!increment && start.geq(end)));

    return array;
  }

  private Value rangeIncrement(Value value, long step)
  {
    if (value instanceof StringValue)
      return StringValue.create((char) (value.toChar() + step));

    return LongValue.create(value.toLong() + step);
  }

  // XXX:You'll need to mark the function as XXX:, because I need to add an
  // attribute like @ModifiedSymbolTable and change some analysis of the
  // compilation based on that attribute.
  //
  // Basically, the compiled mode uses Java variables to store PHP
  // variables.  The extract() call messes that up, or at least forces the
  // compiler to synchronize its view of the variables.
  // (email Re:extract: symbol table)

  /**
   * Inputs new variables into the symbol table from the passed array
   *
   * @param array the array contained the new variables
   * @param rawType flag to determine how to handle collisions
   * @param valuePrefix used along with the flag
   * @return the number of new variables added from the array to the symbol
   *         table
   */
  @UsesSymbolTable
  public static Value extract(Env env,
                              ArrayValue array,
                              @Optional("EXTR_OVERWRITE") long rawType,
                              @Optional("NULL") Value valuePrefix)
  {
    if (array == null)
      return NullValue.NULL;

    long extractType = rawType & ~EXTR_REFS;

    boolean extrRefs = (rawType & EXTR_REFS) != 0;

    if (extractType < EXTR_OVERWRITE || extractType > EXTR_IF_EXISTS &&
                                        extractType != EXTR_REFS) {
      env.warning("Unknown extract type");

      return NullValue.NULL;
    }

    if (extractType >= EXTR_PREFIX_SAME &&
        extractType <= EXTR_PREFIX_IF_EXISTS &&
        (valuePrefix == null || (! (valuePrefix instanceof StringValue)))) {
      env.warning("Prefix expected to be specified");

      return NullValue.NULL;
    }

    String prefix = "";

    if (valuePrefix instanceof StringValue)
      prefix = valuePrefix.toString() + "_";

    int completedSymbols = 0;

    for (Value entryKey : array.keySet()) {
      Value entryValue;

      if (extrRefs)
        entryValue = array.getRef(entryKey);
      else
        entryValue = array.get(entryKey);

      String symbolName = entryKey.toString();

      Value tableValue = env.getValue(symbolName);

      switch ((int) extractType) {
      case EXTR_SKIP:
        if (tableValue != NullValue.NULL)
          symbolName = "";

        break;
      case EXTR_PREFIX_SAME:
        if (tableValue != NullValue.NULL)
          symbolName = prefix + symbolName;

        break;
      case EXTR_PREFIX_ALL:
        symbolName = prefix + symbolName;

        break;
      case EXTR_PREFIX_INVALID:
        if (! validVariableName(symbolName))
          symbolName = prefix + symbolName;

        break;
      case EXTR_IF_EXISTS:
        if (tableValue == NullValue.NULL)
          symbolName = "";//entryValue = tableValue;

        break;
      case EXTR_PREFIX_IF_EXISTS:
        if (tableValue != NullValue.NULL)
          symbolName = prefix + symbolName;
        else
          symbolName = "";

        break;
      default:

        break;
      }

      if (validVariableName(symbolName)) {
        env.setValue(symbolName, entryValue);

        completedSymbols++;
      }
    }

    return LongValue.create(completedSymbols);
  }

  /**
   * Helper function for extract to determine if a variable name is valid
   *
   * @param variableName the name to check
   * @return true if the name is valid, false otherwise
   */
  private static boolean validVariableName(String variableName)
  {
    if (variableName.length() < 1)
      return false;

    char checkChar = variableName.charAt(0);

    if (! Character.isLetter(checkChar) && checkChar != '_')
      return false;

    for (int k = 1; k < variableName.length(); k++) {
      checkChar = variableName.charAt(k);

      if (!Character.isLetterOrDigit(checkChar) && checkChar != '_')
        return false;
    }

    return true;
  }

  /**
   * Returns an array with everything that is in array and not in the other
   * arrays using a passed callback function for comparing
   *
   * @param array the primary array
   * @param arrays the vector of arrays to check the primary array's values
   * against
   * @return an array with all of the values that are in the primary array but
   *         not in the other arrays
   */
  public Value array_diff(Env env, ArrayValue array, Value []arrays)
    throws Throwable
  {
    if (array == null)
      return NullValue.NULL;

    if (arrays.length < 1) {
      env.warning("Wrong parameter count for array_diff()");

      return NullValue.NULL;
    }

    ArrayValue diffArray = new ArrayValueImpl();

    boolean valueFound;

    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      valueFound = false;

      Value entryValue = entry.getValue();

      for (int k = 0; k < arrays.length && ! valueFound; k++) {
        if (! (arrays[k] instanceof ArrayValue)) {
          env.warning("Argument #" + (k + 2) + " is not an array");

          return NullValue.NULL;
        }

        valueFound =
          ((ArrayValue) arrays[k]).contains(entryValue) != NullValue.NULL;
      }

      if (! valueFound)
        diffArray.put(entry.getKey(), entryValue);
    }

    return diffArray;
  }


  /**
   * Returns an array with everything that is in array and not in the other
   * arrays, keys also used
   *
   * @param array the primary array
   * @param arrays the vector of arrays to check the primary array's values
   * against
   * @return an array with all of the values that are in the primary array but
   *         not in the other arrays
   */
  public Value array_diff_assoc(Env env, ArrayValue array, Value []arrays)
    throws Throwable
  {
    if (array == null)
      return NullValue.NULL;

    if (arrays.length < 1) {
      env.warning("Wrong parameter count for array_diff()");

      return NullValue.NULL;
    }

    ArrayValue diffArray = new ArrayValueImpl();

    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      boolean valueFound = false;

      Value entryValue = entry.getValue();

      Value entryKey = entry.getKey();

      for (int k = 0; k < arrays.length && ! valueFound; k++) {
        if (! (arrays[k] instanceof ArrayValue)) {
          env.warning("Argument #" + (k + 2) + " is not an array");

          return NullValue.NULL;
        }

        valueFound =
          ((ArrayValue) arrays[k]).contains(entryValue).eq(entryKey);
      }

      if (! valueFound)
        diffArray.put(entryKey, entryValue);
    }

    return diffArray;
  }

  /**
   * Returns an array with everything that is in array and not in the other
   * arrays, keys used for comparison
   *
   * @param array the primary array
   * @param arrays the vector of arrays to check the primary array's values
   * against
   * @return an array with all of the values that are in the primary array but
   *         not in the other arrays
   */
  public Value array_diff_key(Env env, ArrayValue array, Value []arrays)
    throws Throwable
  {
    if (array == null)
      return NullValue.NULL;

    if (arrays.length < 1) {
      env.warning("Wrong parameter count for array_diff()");

      return NullValue.NULL;
    }

    ArrayValue diffArray = new ArrayValueImpl();

    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      boolean keyFound = false;

      Value entryKey = entry.getKey();

      for (int k = 0; k < arrays.length && ! keyFound; k++) {
        if (! (arrays[k] instanceof ArrayValue)) {
          env.warning("Argument #" + (k + 2) + " is not an array");

          return NullValue.NULL;
        }

        keyFound =
          ((ArrayValue) arrays[k]).containsKey(entryKey) != NullValue.NULL;
      }

      if (! keyFound)
        diffArray.put(entryKey, entry.getValue());
    }

    return diffArray;
  }

  /**
   * Returns an array with everything that is in array and not in the other
   * arrays, keys used for comparison aswell
   *
   * @param array the primary array
   * @param arrays the vector of arrays to check the primary array's values
   * against.  The last element is the callback function.
   * @return an array with all of the values that are in the primary array but
   *         not in the other arrays
   */
  public Value array_diff_uassoc(Env env, ArrayValue array, Value []arrays)
    throws Throwable
  {
    if (array == null)
      return NullValue.NULL;

    if (arrays.length < 2) {
      env.warning("Wrong parameter count for array_diff()");

      return NullValue.NULL;
    }

    AbstractFunction func =
      env.findFunction(arrays[arrays.length - 1].toString().intern());

    if (func == null) {
      env.warning("Invalid comparison function");

      return NullValue.NULL;
    }

    ArrayValue diffArray = new ArrayValueImpl();

    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      boolean ValueFound = false;

      Value entryValue = entry.getValue();

      Value entryKey = entry.getKey();

      for (int k = 0; k < arrays.length - 1 && ! ValueFound; k++) {
        if (! (arrays[k] instanceof ArrayValue)) {
          env.warning("Argument #" + (k + 2) + " is not an array");

          return NullValue.NULL;
        }

        Value searchKey = ((ArrayValue) arrays[k]).contains(entryValue);

        if (searchKey != NullValue.NULL)
          ValueFound = ((int) func.eval(env, searchKey, entryKey).toLong()) ==
                       0;
      }

      if (! ValueFound)
        diffArray.put(entryKey, entryValue);
    }

    return diffArray;
  }

  /**
   * Returns an array with everything that is in array and not in the other
   * arrays, keys used for comparison only
   *
   * @param array the primary array
   * @param arrays the vector of arrays to check the primary array's values
   * against.  The last element is the callback function.
   * @return an array with all of the values that are in the primary array but
   *         not in the other arrays
   */
  public Value array_diff_ukey(Env env, ArrayValue array, Value []arrays)
    throws Throwable
  {
    if (array == null)
      return NullValue.NULL;

    if (arrays.length < 2) {
      env.warning("Wrong parameter count for array_diff()");

      return NullValue.NULL;
    }

    AbstractFunction func =
      env.findFunction(arrays[arrays.length - 1].toString().intern());

    if (func == null) {
      env.warning("Invalid comparison function");

      return NullValue.NULL;
    }

    ArrayValue diffArray = new ArrayValueImpl();

    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      boolean keyFound = false;

      Value entryKey = entry.getKey();

      for (int k = 0; k < arrays.length - 1 && ! keyFound; k++) {
        if (! (arrays[k] instanceof ArrayValue)) {
          env.warning("Argument #" + (k + 2) + " is not an array");

          return NullValue.NULL;
        }

        Iterator<Value> keyItr = ((ArrayValue) arrays[k]).keySet().iterator();

        keyFound = false;

        while (keyItr.hasNext() && ! keyFound) {
          Value currentKey = keyItr.next();

          keyFound = ((int) func.eval(env, entryKey, currentKey).toLong()) == 0;
        }
      }

      if (! keyFound)
        diffArray.put(entryKey, entry.getValue());
    }

    return diffArray;
  }

  /**
   * Returns an array with everything that is in array and also in the other
   * arrays
   *
   * @param array the primary array
   * @param arrays the vector of arrays to check the primary array's values
   * against.  The last element is the callback function.
   * @return an array with all of the values that are in the primary array and
   *         in the other arrays
   */
  public Value array_intersect(Env env, ArrayValue array, Value []arrays)
    throws Throwable
  {
    if (array == null)
      return NullValue.NULL;

    if (arrays.length < 1) {
      env.warning("Wrong parameter count for array_diff()");

      return NullValue.NULL;
    }

    ArrayValue interArray = new ArrayValueImpl();

    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      boolean valueFound = false;

      Value entryValue = entry.getValue();

      for (int k = 0; k < arrays.length; k++) {
        if (! (arrays[k] instanceof ArrayValue)) {
          env.warning("Argument #" + (k + 2) + " is not an array");

          return NullValue.NULL;
        }

        if (k > 0 && ! valueFound)
          break;

        valueFound =
          ((ArrayValue) arrays[k]).contains(entryValue) != NullValue.NULL;
      }

      if (valueFound)
        interArray.put(entry.getKey(), entryValue);
    }

    return interArray;
  }

  /**
   * Returns an array with everything that is in array and also in the other
   * arrays, keys are also used in the comparison
   *
   * @param array the primary array
   * @param arrays the vector of arrays to check the primary array's values
   * against.  The last element is the callback function.
   * @return an array with all of the values that are in the primary array and
   *         in the other arrays
   */
  public Value array_intersect_assoc(Env env, ArrayValue array, Value []arrays)
    throws Throwable
  {
    if (array == null)
      return NullValue.NULL;

    if (arrays.length < 1) {
      env.warning("Wrong parameter count for array_diff()");

      return NullValue.NULL;
    }

    ArrayValue interArray = new ArrayValueImpl();

    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      boolean valueFound = false;

      Value entryKey = entry.getKey();

      Value entryValue = entry.getValue();

      for (int k = 0; k < arrays.length; k++) {
        if (! (arrays[k] instanceof ArrayValue)) {
          env.warning("Argument #" + (k + 2) + " is not an array");

          return NullValue.NULL;
        }

        if (k > 0 && ! valueFound)
          break;

        Value searchValue = ((ArrayValue) arrays[k]).containsKey(entryKey);

        if (searchValue != NullValue.NULL)
          valueFound = searchValue.eq(entryValue);
        else
          valueFound = false;
      }

      if (valueFound)
        interArray.put(entryKey, entryValue);
    }

    return interArray;
  }

  /**
   * Returns an array with everything that is in array and also in the other
   * arrays, keys are only used in the comparison
   *
   * @param array the primary array
   * @param arrays the vector of arrays to check the primary array's values
   * against.  The last element is the callback function.
   * @return an array with all of the values that are in the primary array and
   *         in the other arrays
   */
  public Value array_intersect_key(Env env, ArrayValue array, Value []arrays)
    throws Throwable
  {
    if (array == null)
      return NullValue.NULL;

    if (arrays.length < 1) {
      env.warning("Wrong parameter count for array_diff()");

      return NullValue.NULL;
    }

    ArrayValue interArray = new ArrayValueImpl();

    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      boolean keyFound = false;

      Value entryKey = entry.getKey();

      for (int k = 0; k < arrays.length; k++) {
        if (! (arrays[k] instanceof ArrayValue)) {
          env.warning("Argument #" + (k + 2) + " is not an array");

          return NullValue.NULL;
        }

        if (k > 0 && ! keyFound)
          break;

        keyFound =
          ((ArrayValue) arrays[k]).containsKey(entryKey) != NullValue.NULL;
      }

      if (keyFound)
        interArray.put(entryKey, entry.getValue());
    }

    return interArray;
  }

  /**
   * Returns an array with everything that is in array and also in the other
   * arrays, keys are also used in the comparison.  Uses a callback function for
   * evalutation the keys.
   *
   * @param array the primary array
   * @param arrays the vector of arrays to check the primary array's values
   * against.  The last element is the callback function.
   * @return an array with all of the values that are in the primary array and
   *         in the other arrays
   */
  public Value array_intersect_uassoc(Env env, ArrayValue array, Value []arrays)
    throws Throwable
  {
    if (array == null)
      return NullValue.NULL;

    if (arrays.length < 2) {
      env.warning("Wrong parameter count for array_diff()");

      return NullValue.NULL;
    }

    AbstractFunction func =
      env.findFunction(arrays[arrays.length - 1].toString().intern());

    if (func == null) {
      env.warning("Invalid comparison function");

      return NullValue.NULL;
    }

    ArrayValue interArray = new ArrayValueImpl();

    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      boolean valueFound = false;

      Value entryKey = entry.getKey();

      Value entryValue = entry.getValue();

      for (int k = 0; k < arrays.length - 1; k++) {
        if (! (arrays[k] instanceof ArrayValue)) {
          env.warning("Argument #" + (k + 2) + " is not an array");

          return NullValue.NULL;
        }

        if (k > 0 && ! valueFound)
          break;

        Value searchValue = ((ArrayValue) arrays[k]).containsKey(entryKey);

        if (searchValue != NullValue.NULL)
          valueFound = func.eval(env, searchValue, entryValue).toLong() == 0;
        else
          valueFound = false;
      }

      if (valueFound)
        interArray.put(entryKey, entryValue);
    }

    return interArray;
  }

  /**
   * Returns an array with everything that is in array and also in the other
   * arrays, keys are only used in the comparison.  Uses a callback function for
   * evalutation the keys.
   *
   * @param array the primary array
   * @param arrays the vector of arrays to check the primary array's values
   * against.  The last element is the callback function.
   * @return an array with all of the values that are in the primary array and
   *         in the other arrays
   */
  public Value array_intersect_ukey(Env env, ArrayValue array, Value []arrays)
    throws Throwable
  {
    if (array == null)
      return NullValue.NULL;

    if (arrays.length < 2) {
      env.warning("Wrong parameter count for array_diff()");

      return NullValue.NULL;
    }

    AbstractFunction func =
      env.findFunction(arrays[arrays.length - 1].toString().intern());

    if (func == null) {
      env.warning("Invalid comparison function");

      return NullValue.NULL;
    }

    ArrayValue interArray = new ArrayValueImpl();

    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      boolean keyFound = false;

      Value entryKey = entry.getKey();

      for (int k = 0; k < arrays.length - 1; k++) {
        if (! (arrays[k] instanceof ArrayValue)) {
          env.warning("Argument #" + (k + 2) + " is not an array");

          return NullValue.NULL;
        }

        if (k > 0 && ! keyFound)
          break;

        Iterator<Value> keyItr = ((ArrayValue) arrays[k]).keySet().iterator();

        keyFound = false;

        while (keyItr.hasNext() && ! keyFound) {
          Value currentKey = keyItr.next();

          keyFound = ((int) func.eval(env, entryKey, currentKey).toLong()) == 0;
        }

      }

      if (keyFound)
        interArray.put(entryKey, entry.getValue());
    }

    return interArray;
  }

  /**
   * Maps the given function with the array arguments.
   *
   * @param fun the function name
   * @param args the vector of array arguments
   * @return an array with all of the mapped values
   */
  public Value array_map(Env env, Callback fun,
                         ArrayValue arg, Value []args)
    throws Throwable
  {
    // quercus/1730
    Iterator<Map.Entry<Value, Value>> argIter = arg.entrySet().iterator();

    Iterator []iters = new Iterator[args.length];
    for (int i = 0; i < args.length; i++) {
      if (! (args[i] instanceof ArrayValue))
        throw env.errorException(L.l("expected array"));

      ArrayValue argArray = (ArrayValue) args[i];

      iters[i] = argArray.values().iterator();
    }

    ArrayValue resultArray = new ArrayValueImpl();

    Value []param = new Value[args.length + 1];
    while (argIter.hasNext()) {
      Map.Entry<Value, Value> entry = argIter.next();

      param[0] = entry.getValue();

      for (int i = 0; i < iters.length; i++) {
        param[i + 1] = (Value) iters[i].next();

        if (param[i + 1] == null)
          param[i + 1] = NullValue.NULL;
      }

      resultArray.put(entry.getKey(), fun.eval(env, param));
    }

    return resultArray;
  }

  /**
   * Maps the given function with the array arguments.
   *
   * @param args the vector of array arguments
   * @return an array with all of the mapped values
   */
  public Value array_merge(Value []args)
    throws Throwable
  {
    // quercus/1731

    ArrayValue result = new ArrayValueImpl();

    for (Value arg : args) {
      if (arg.isNull())
	return NullValue.NULL;
      
      if (! (arg.toValue() instanceof ArrayValue))
        continue;

      ArrayValue array = (ArrayValue) arg.toValue();

      for (Map.Entry<Value, Value> entry : array.entrySet()) {
        Value key = entry.getKey();
        Value value = entry.getValue();

        if (key.isNumberConvertible())
          result.put(value);
        else
          result.put(key, value);
      }
    }

    return result;
  }

  /**
   * Maps the given function with the array arguments.
   *
   * @param args the vector of array arguments
   * @return an array with all of the mapped values
   */
  public Value array_merge_recursive(Value []args)
    throws Throwable
  {
    // quercus/173a

    ArrayValue result = new ArrayValueImpl();

    for (Value arg : args) {
      if (! (arg.toValue() instanceof ArrayValue))
        continue;

      arrayMergeRecursiveImpl(result, (ArrayValue) arg.toValue());
    }

    return result;
  }

  private static void arrayMergeRecursiveImpl(ArrayValue result,
                                              ArrayValue array)
  {
    for (Map.Entry<Value, Value> entry : array.entrySet()) {
      Value key = entry.getKey();
      Value value = entry.getValue().toValue();

      if (key.isNumberConvertible()) {
        result.put(value);
      }
      else {
        Value oldValue = result.get(key).toValue();

        if (oldValue != null && oldValue.isset()) {
          if (oldValue.isArray() && value.isArray()) {
            arrayMergeRecursiveImpl((ArrayValue) oldValue, (ArrayValue) value);
          }
          else if (oldValue.isArray()) {
            oldValue.put(value);
          }
          else if (value.isArray()) {
            // XXX: s/b insert?
            value.put(oldValue);
          }
          else {
            ArrayValue newArray = new ArrayValueImpl();

            newArray.put(oldValue);
            newArray.put(value);

            result.put(key, newArray);
          }
        }
        else {
          result.put(key, value);
        }
      }
    }
  }

  /**
   * Sort the arrays like rows in a database.
   * @param arrays  arrays to sort
   *
   * @return true on success, and false on failure
   */
  public boolean array_multisort(Env env, Value[] arrays)
    throws Throwable
  {
    int maxsize = 0;
    for(int i=0; i<arrays.length; i++)
      if (arrays[i] instanceof ArrayValue)
	maxsize = Math.max(maxsize,
			   ((ArrayValue)arrays[i]).getSize());

    // create the identity permutation [1..n]
    LongValue []rows = new LongValue[maxsize];
    for(int i=0; i<rows.length; i++)
      rows[i] = LongValue.create(i);

    java.util.Arrays.sort(rows, new MultiSortComparator(env, arrays));

    // apply the permuation
    for(int i=0; i<arrays.length; i++)
      if (arrays[i] instanceof ArrayValue)
	permute(env, (ArrayValue)arrays[i], rows);

    return true;
  }

  /*
   *  Apply a permutation to an array; on return, each element of
   *  array[i] holds the value that was in array[permutation[i]]
   *  before the call.
   */
  private static void permute(Env env, ArrayValue array,
			      LongValue[] permutation)
  {
    Value[] values = array.getValueArray(env);
    for(int i=0; i<permutation.length; i++)
      array.put(LongValue.create(i),
		values[(int)permutation[i].toLong()]);
  }


  // XXX: Performance Test asort
  /**
   * Sorts the array.
   */
  /*public Value asort(Env env,
		     Value value,
		     @Optional int mode)
    throws Throwable
  {
    if (! (value instanceof ArrayValue)) {
      env.warning(L.l("asort requires array at '{0}'", value));
      return BooleanValue.FALSE;
    }

    ArrayValue array = (ArrayValue) value;

    array.asort();

    return BooleanValue.TRUE;
  }*/

  // XXX: Performance Test ksort
  /**
   * Sorts the array.
   */
  /*public Value ksort(Env env,
		     Value value,
		     @Optional int mode)
    throws Throwable
  {
    if (! (value instanceof ArrayValue)) {
      env.warning(L.l("asort requires array at '{0}'", value));
      return BooleanValue.FALSE;
    }

    ArrayValue array = (ArrayValue) value;

    array.ksort();

    return BooleanValue.TRUE;
  }*/

  /**
   * Creates an array with all the values of the first array that are not
   * present in the other arrays, using a provided callback function to
   * determine equivalence.
   *
   * @param arrays first array is checked against the rest.  Last element is the
   * callback function.
   * @return an array with all the values of the first array that are not in the
   *         rest
   */
  public Value array_udiff(Env env, Value[] arrays)
  {
    if (arrays.length < 3) {
      env.warning("Wrong paremeter count for array_udiff()");

      return NullValue.NULL;
    }

    if (! (arrays[0] instanceof ArrayValue)) {
      env.warning("Argument #1 is not an array");

      return NullValue.NULL;
    }

    ArrayValue array = (ArrayValue) arrays[0];

    Value callbackValue = arrays[arrays.length - 1];

    Callback cmp;

    try {
      cmp = env.createCallback(callbackValue);
    }
    catch (Throwable t) {
      log.log(Level.WARNING, t.toString(), t);

      env.warning("Not a valid callback " + callbackValue.toString());

      return NullValue.NULL;
    }

    if (cmp == null) {
      env.warning("Not a valid callback " + callbackValue.toString());

      return NullValue.NULL;
    }

    ArrayValue diffArray = new ArrayValueImpl();

    boolean isFound = false;

    for (Value entryKey : array.keySet()) {
      Value entryValue = array.get(entryKey);

      for (int k = 1; k < arrays.length - 1 && ! isFound; k++) {
        if (! (arrays[k] instanceof ArrayValue)) {
          env.warning("Argument #" + (k + 1) + " is not an array");

          return NullValue.NULL;
        }

        ArrayValue checkArray = (ArrayValue) arrays[k];

        for (Map.Entry<Value, Value> entry : checkArray.entrySet()) {
          try {
            isFound = cmp.eval(env, entryValue, entry.getValue()).toLong() == 0;
          }
          catch (Throwable t) {
            log.log(Level.WARNING, t.toString(), t);

            env.warning("An error occurred while invoking the filter callback");

            return NullValue.NULL;
          }

          if (isFound)
            break;
        }
      }

      if (! isFound)
        diffArray.put(entryKey, entryValue);

      isFound = false;
    }

    return diffArray;
  }

  /**
   * Creates an array with all the values of the first array that are not
   * present in the other arrays, using a provided callback function to
   * determine equivalence.  Also checks the key for equality using an internal
   * comparison function.
   *
   * @param arrays first array is checked against the rest.  Last element is the
   * callback function.
   * @return an array with all the values of the first array that are not in the
   *         rest
   */
  public Value array_udiff_assoc(Env env, Value[] arrays)
  {
    if (arrays.length < 3) {
      env.warning("Wrong paremeter count for array_udiff_assoc()");

      return NullValue.NULL;
    }

    if (! (arrays[0] instanceof ArrayValue)) {
      env.warning("Argument #1 is not an array");

      return NullValue.NULL;
    }

    ArrayValue array = (ArrayValue) arrays[0];

    Value callbackValue = arrays[arrays.length - 1];

    Callback cmp;

    try {
      cmp = env.createCallback(callbackValue);
    }
    catch (Throwable t) {
      log.log(Level.WARNING, t.toString(), t);

      env.warning("Not a valid callback " + callbackValue.toString());

      return NullValue.NULL;
    }

    if (cmp == null) {
      env.warning("Not a valid callback " + callbackValue.toString());

      return NullValue.NULL;
    }

    ArrayValue diffArray = new ArrayValueImpl();

    boolean isFound = false;

    for (Value entryKey : array.keySet()) {
      Value entryValue = array.get(entryKey);

      for (int k = 1; k < arrays.length - 1 && ! isFound; k++) {
        if (! (arrays[k] instanceof ArrayValue)) {
          env.warning("Argument #" + (k + 1) + " is not an array");

          return NullValue.NULL;
        }

        ArrayValue checkArray = (ArrayValue) arrays[k];

        for (Map.Entry<Value, Value> entry : checkArray.entrySet()) {
          try {
            boolean keyFound = entryKey.eql(entry.getKey());

            boolean valueFound = false;

            if (keyFound)
              valueFound = cmp.eval(env, entryValue, entry.getValue())
                .toLong() == 0;

            isFound = keyFound && valueFound;
          }
          catch (Throwable t) {
            log.log(Level.WARNING, t.toString(), t);

            env.warning("An error occurred while invoking the filter callback");

            return NullValue.NULL;
          }

          if (isFound)
            break;
        }
      }

      if (! isFound)
        diffArray.put(entryKey, entryValue);

      isFound = false;
    }

    return diffArray;
  }

  /**
   * Creates an array with all the values of the first array that are not
   * present in the other arrays, using a provided callback function to
   * determine equivalence.  Also checks keys using a provided callback
   * function.
   *
   * @param arrays first array is checked against the rest.  Last two elementare
   * the callback functions.
   * @return an array with all the values of the first array that are not in the
   *         rest
   */
  public Value array_udiff_uassoc(Env env, Value[] arrays)
  {
    if (arrays.length < 4) {
      env.warning("Wrong paremeter count for array_udiff_uassoc()");

      return NullValue.NULL;
    }

    if (! (arrays[0] instanceof ArrayValue)) {
      env.warning("Argument #1 is not an array");

      return NullValue.NULL;
    }

    ArrayValue array = (ArrayValue) arrays[0];

    Value callbackValue = arrays[arrays.length - 2];

    Callback cmpValue;

    try {
      cmpValue = env.createCallback(callbackValue);
    }
    catch (Throwable t) {
      log.log(Level.WARNING, t.toString(), t);

      env.warning("Not a valid callback " + callbackValue.toString());

      return NullValue.NULL;
    }

    if (cmpValue == null) {
      env.warning("Not a valid callback " + callbackValue.toString());

      return NullValue.NULL;
    }

    Value callbackKey = arrays[arrays.length - 1];

    Callback cmpKey;

    try {
      cmpKey = env.createCallback(callbackKey);
    }
    catch (Throwable t) {
      log.log(Level.WARNING, t.toString(), t);

      env.warning("Not a valid callback " + callbackKey.toString());

      return NullValue.NULL;
    }

    if (cmpKey == null) {
      env.warning("Not a valid callback " + callbackKey.toString());

      return NullValue.NULL;
    }

    ArrayValue diffArray = new ArrayValueImpl();

    boolean isFound = false;

    for (Value entryKey : array.keySet()) {
      Value entryValue = array.get(entryKey);

      for (int k = 1; k < arrays.length - 2 && ! isFound; k++) {
        if (! (arrays[k] instanceof ArrayValue)) {
          env.warning("Argument #" + (k + 1) + " is not an array");

          return NullValue.NULL;
        }

        ArrayValue checkArray = (ArrayValue) arrays[k];

        for (Map.Entry<Value, Value> entry : checkArray.entrySet()) {
          try {
            boolean valueFound =
              cmpValue.eval(env, entryValue, entry.getValue()).toLong() == 0;

            boolean keyFound = false;

            if (valueFound)
              keyFound = cmpKey.eval(env, entryKey, entry.getKey()).toLong() ==
                         0;

            isFound = valueFound && keyFound;
          }
          catch (Throwable t) {
            log.log(Level.WARNING, t.toString(), t);

            env.warning("An error occurred while invoking the filter callback");

            return NullValue.NULL;
          }

          if (isFound)
            break;
        }
      }

      if (! isFound)
        diffArray.put(entryKey, entryValue);

      isFound = false;
    }

    return diffArray;
  }

  /**
   * Creates an array with all the values of the first array that are present in
   * the other arrays, using a provided callback function to determine
   * equivalence.
   *
   * @param arrays first array is checked against the rest.  Last element is the
   * callback function.
   * @return an array with all the values of the first array that are in the
   *         rest
   */
  public Value array_uintersect(Env env, Value[] arrays)
  {
    if (arrays.length < 3) {
      env.warning("Wrong paremeter count for array_uintersect()");

      return NullValue.NULL;
    }

    if (! (arrays[0] instanceof ArrayValue)) {
      env.warning("Argument #1 is not an array");

      return NullValue.NULL;
    }

    ArrayValue array = (ArrayValue) arrays[0];

    Value callbackValue = arrays[arrays.length - 1];

    Callback cmp;

    try {
      cmp = env.createCallback(callbackValue);
    }
    catch (Throwable t) {
      log.log(Level.WARNING, t.toString(), t);

      env.warning("Not a valid callback " + callbackValue.toString());

      return NullValue.NULL;
    }

    if (cmp == null) {
      env.warning("Not a valid callback " + callbackValue.toString());

      return NullValue.NULL;
    }

    ArrayValue interArray = new ArrayValueImpl();

    boolean isFound = true;

    for (Value entryKey : array.keySet()) {
      Value entryValue = array.get(entryKey);

      for (int k = 1; k < arrays.length - 1 && isFound; k++) {
        if (! (arrays[k] instanceof ArrayValue)) {
          env.warning("Argument #" + (k + 1) + " is not an array");

          return NullValue.NULL;
        }

        ArrayValue checkArray = (ArrayValue) arrays[k];

        for (Map.Entry<Value, Value> entry : checkArray.entrySet()) {
          try {
            isFound = cmp.eval(env, entryValue, entry.getValue()).toLong() == 0;
          }
          catch (Throwable t) {
            log.log(Level.WARNING, t.toString(), t);

            env.warning("An error occurred while invoking the filter callback");

            return NullValue.NULL;
          }

          if (isFound)
            break;
        }
      }

      if (isFound)
        interArray.put(entryKey, entryValue);
    }

    return interArray;
  }

  /**
   * Creates an array with all the values of the first array that are present in
   * the other arrays, using a provided callback function to determine
   * equivalence. Also checks the keys for equivalence using an internal
   * comparison.
   *
   * @param arrays first array is checked against the rest.  Last element is the
   * callback function.
   * @return an array with all the values of the first array that are in the
   *         rest
   */
  public Value array_uintersect_assoc(Env env, Value[] arrays)
  {
    if (arrays.length < 3) {
      env.warning("Wrong paremeter count for array_uintersect_assoc()");

      return NullValue.NULL;
    }

    if (! (arrays[0] instanceof ArrayValue)) {
      env.warning("Argument #1 is not an array");

      return NullValue.NULL;
    }

    ArrayValue array = (ArrayValue) arrays[0];

    Value callbackValue = arrays[arrays.length - 1];

    Callback cmp;

    try {
      cmp = env.createCallback(callbackValue);
    }
    catch (Throwable t) {
      log.log(Level.WARNING, t.toString(), t);

      env.warning("Not a valid callback " + callbackValue.toString());

      return NullValue.NULL;
    }

    if (cmp == null) {
      env.warning("Not a valid callback " + callbackValue.toString());

      return NullValue.NULL;
    }

    ArrayValue interArray = new ArrayValueImpl();

    boolean isFound = true;

    for (Value entryKey : array.keySet()) {
      Value entryValue = array.get(entryKey);

      for (int k = 1; k < arrays.length - 1 && isFound; k++) {
        if (! (arrays[k] instanceof ArrayValue)) {
          env.warning("Argument #" + (k + 1) + " is not an array");

          return NullValue.NULL;
        }

        ArrayValue checkArray = (ArrayValue) arrays[k];

        for (Map.Entry<Value, Value> entry : checkArray.entrySet()) {
          try {
            boolean keyFound = entryKey.eql(entry.getKey());

            boolean valueFound = false;

            if (keyFound)
              valueFound = cmp.eval(env, entryValue, entry.getValue())
                .toLong() == 0;

            isFound = keyFound && valueFound;
          }
          catch (Throwable t) {
            log.log(Level.WARNING, t.toString(), t);

            env.warning("An error occurred while invoking the filter callback");

            return NullValue.NULL;
          }

          if (isFound)
            break;
        }
      }

      if (isFound)
        interArray.put(entryKey, entryValue);
    }

    return interArray;
  }

  /**
   * Creates an array with all the values of the first array that are present in
   * the other arrays, using a provided callback function to determine
   * equivalence. Also checks the keys for equivalence using a pass callback
   * function
   *
   * @param arrays first array is checked against the rest.  Last two elements
   * are the callback functions.
   * @return an array with all the values of the first array that are in the
   *         rest
   */
  public Value array_uintersect_uassoc(Env env, Value[] arrays)
  {
    if (arrays.length < 4) {
      env.warning("Wrong paremeter count for array_uintersect_uassoc()");

      return NullValue.NULL;
    }

    if (! (arrays[0] instanceof ArrayValue)) {
      env.warning("Argument #1 is not an array");

      return NullValue.NULL;
    }

    ArrayValue array = (ArrayValue) arrays[0];

    Value callbackValue = arrays[arrays.length - 2];

    Callback cmpValue;

    try {
      cmpValue = env.createCallback(callbackValue);
    }
    catch (Throwable t) {
      log.log(Level.WARNING, t.toString(), t);

      env.warning("Not a valid callback " + callbackValue.toString());

      return NullValue.NULL;
    }

    if (cmpValue == null) {
      env.warning("Not a valid callback " + callbackValue.toString());

      return NullValue.NULL;
    }

    Value callbackKey = arrays[arrays.length - 1];

    Callback cmpKey;

    try {
      cmpKey = env.createCallback(callbackKey);
    }
    catch (Throwable t) {
      log.log(Level.WARNING, t.toString(), t);

      env.warning("Not a valid callback " + callbackKey.toString());

      return NullValue.NULL;
    }

    if (cmpKey == null) {
      env.warning("Not a valid callback " + callbackKey.toString());

      return NullValue.NULL;
    }

    ArrayValue interArray = new ArrayValueImpl();

    boolean isFound = true;

    for (Value entryKey : array.keySet()) {
      Value entryValue = array.get(entryKey);

      for (int k = 1; k < arrays.length - 2 && isFound; k++) {
        if (! (arrays[k] instanceof ArrayValue)) {
          env.warning("Argument #" + (k + 1) + " is not an array");

          return NullValue.NULL;
        }

        ArrayValue checkArray = (ArrayValue) arrays[k];

        for (Map.Entry<Value, Value> entry : checkArray.entrySet()) {
          try {
            boolean valueFound =
              cmpValue.eval(env, entryValue, entry.getValue()).toLong() == 0;

            boolean keyFound = false;

            if (valueFound)
              keyFound = cmpKey.eval(env, entryKey, entry.getKey()).toLong() ==
                         0;

            isFound = valueFound && keyFound;
          }
          catch (Throwable t) {
            log.log(Level.WARNING, t.toString(), t);

            env.warning("An error occurred while invoking the filter callback");

            return NullValue.NULL;
          }

          if (isFound)
            break;
        }
      }

      if (isFound)
        interArray.put(entryKey, entryValue);
    }

    return interArray;
  }

  /**
   * Creates an array of corresponding values to variables in the symbol name.
   * The passed parameters are the names of the variables to be added to the
   * array.
   *
   * @param variables contains the names of variables to add to the array
   * @return an array with the values of variables that match those passed
   */
  @UsesSymbolTable
  public ArrayValue compact(Env env, Value[] variables)
  {
    ArrayValue compactArray = new ArrayValueImpl();

    for (Value variableName : variables) {
      if (variableName instanceof StringValue) {
        Value tableValue = env.getValue(variableName.toString());

        if (tableValue.isset())
          compactArray.put(variableName, tableValue);
      }
      else if (variableName instanceof ArrayValue) {
        ArrayValue array = (ArrayValue) variableName;

        ArrayValue innerArray = compact(env, array.valuesToArray());

        compactArray.putAll(innerArray);
      }
    }

    return compactArray;
  }

  /**
   * Returns the size of the array.
   */
  public static Value sizeof(@ReadOnly Value value)
  {
    return count(value);
  }

  private static class CompareString
    implements
    Comparator<Map.Entry<Value, Value>>
  {
    private AbstractGet _getter;

    private int _order;

    CompareString(AbstractGet getter, int order)
    {
      _getter = getter;
      _order = order;
    }

    public int compare(Map.Entry<Value, Value> aEntry,
                       Map.Entry<Value, Value> bEntry)
    {
      String aElement = _getter.get(aEntry).toString();
      String bElement = _getter.get(bEntry).toString();

      return aElement.compareTo(bElement) * _order;
    }
  }

  private static class CompareNumeric
    implements
    Comparator<Map.Entry<Value, Value>>
  {
    private AbstractGet _getter;

    private int _order;

    CompareNumeric(AbstractGet getter, int order)
    {
      _getter = getter;
      _order = order;
    }

    public int compare(Map.Entry<Value, Value> aEntry,
                       Map.Entry<Value, Value> bEntry)
    {
      try {
        long aElement = _getter.get(aEntry).toLong();
        long bElement = _getter.get(bEntry).toLong();

        if (aElement == bElement)
          return 0;
        else if (aElement < bElement)
          return -1 * _order;
        else
          return _order;
      }
      catch (Throwable e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static class CompareLocale
    implements
    Comparator<Map.Entry<Value, Value>>
  {
    private AbstractGet _getter;

    private int _order;

    private Collator _collator;

    CompareLocale(AbstractGet getter, int order, Collator collator)
    {
      _getter = getter;
      _order = order;
      _collator = collator;
    }

    public int compare(Map.Entry<Value, Value> aEntry,
                       Map.Entry<Value, Value> bEntry)
    {
      String aElement = _getter.get(aEntry).toString();
      String bElement = _getter.get(bEntry).toString();

      return _collator.compare(aElement, bElement) * _order;
    }
  }

  private static class CompareNormal
    implements
    Comparator<Map.Entry<Value, Value>>
  {
    private AbstractGet _getter;

    private int _order;

    CompareNormal(AbstractGet getter, int order)
    {
      _getter = getter;
      _order = order;
    }

    public int compare(Map.Entry<Value, Value> aEntry,
                       Map.Entry<Value, Value> bEntry)
    {
      if (_getter instanceof GetKey) {
        KeyComparator k = new KeyComparator();

        return k.compare(aEntry, bEntry) * _order;
      }

      ValueComparator c = new ValueComparator();

      return c.compare(aEntry, bEntry) * _order;
    }
  }

  private static class CompareNatural
    implements
    Comparator<Map.Entry<Value, Value>>
  {
    private AbstractGet _getter;

    private int _order;

    private boolean _isCaseSensitive;

    CompareNatural(AbstractGet getter, int order, boolean isCaseSensitive)
    {
      _getter = getter;
      _order = order;
      _isCaseSensitive = isCaseSensitive;
    }

    public int compare(Map.Entry<Value, Value> aEntry,
                       Map.Entry<Value, Value> bEntry)
    {
      try {
        String aElement = _getter.get(aEntry).toString();
        String bElement = _getter.get(bEntry).toString();

        if (! _isCaseSensitive) {
          aElement = aElement.toLowerCase();
          bElement = bElement.toLowerCase();
        }

        StringParser aParser = new StringParser(aElement);
        StringParser bParser = new StringParser(bElement);

        while (aParser.hasNext() && bParser.hasNext()) {
          String aPart = aParser.next();
          String bPart = bParser.next();

          int comparison;

          try {
            Long aLong = Long.valueOf(aPart);
            Long bLong = Long.valueOf(bPart);

            comparison = aLong.compareTo(bLong);
          }
          catch (NumberFormatException e) {
            comparison = aPart.compareTo(bPart);
          }

          if (comparison < 0)
            return -1;
          else if (comparison > 0)
            return 1;
        }

        if (bParser.hasNext())
          return 1;
        else if (aParser.hasNext())
          return -1;
        else
          return 0;

      }
      catch (Throwable e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static class CompareCallBack
    implements Comparator<Map.Entry<Value, Value>>
  {
    private AbstractGet _getter;

    private int _order;

    private Callback _func;

    private Env _env;

    CompareCallBack(AbstractGet getter, int order, Callback func,
                    Env env)
    {
      _getter = getter;
      _order = order;
      _func = func;
      _env = env;
    }

    public int compare(Map.Entry<Value, Value> aEntry,
                       Map.Entry<Value, Value> bEntry)
    {
      try {
        Value aElement = _getter.get(aEntry);
        Value bElement = _getter.get(bEntry);

        return (int) _func.eval(_env, aElement, bElement).toLong();
      }
      catch (Throwable e) {
        throw new RuntimeException(e);
      }
    }
  }

  /*
   *  A comparator used to sort a permutation based on a set of
   *  column-arrays.
   */
  private static class MultiSortComparator
    implements Comparator<LongValue>
  {

    private final Env _env;
    private final Value[] _arrays;

    public MultiSortComparator(Env env, Value[] arrays)
    {
      this._env = env;
      this._arrays = arrays;
    }

    /*
     *  Examine the "row" consisting of arrays[x][index1] and
     *  arrays[x][index2] for all indices "x"; the permutation will be
     *  sorted according to this comparison.
     */
    public int compare(LongValue index1, LongValue index2)
    {
      for(int i=0; i<_arrays.length; i++) {

	// reset direction/mode for each array (per the php.net spec)
	int direction = SORT_ASC;
	int mode      = SORT_REGULAR;
	ArrayValue av = (ArrayValue)_arrays[i];

	// process all flags appearing *after* an array but before the next one
	while((i+1)<_arrays.length && _arrays[i+1] instanceof LongValue) {
	  switch(_arrays[++i].toInt()) {
	    case SORT_ASC:      direction = SORT_ASC; break;
	    case SORT_DESC:     direction = SORT_DESC; break;
	    case SORT_REGULAR:  mode      = SORT_REGULAR; break;
	    case SORT_STRING:   mode      = SORT_STRING; break;
	    case SORT_NUMERIC:  mode      = SORT_NUMERIC; break;
	    default: _env.warning("Unknown sort flag: " + _arrays[i]);
	  }
	}

	Value v1 = av.get(index1);
	Value v2 = av.get(index2);

	if (mode==SORT_STRING) {
	  v1 = v1.toStringValue();
	  v2 = v2.toStringValue();
	} else if (mode==SORT_NUMERIC) {
	  v1 = LongValue.create(v1.toLong());
	  v2 = LongValue.create(v2.toLong());
	}

	if (v1.lt(v2)) return direction==SORT_ASC ? -1 : 1;
	if (v1.gt(v2)) return direction==SORT_ASC ? 1 : -1;

      }
      return 0;
    }
  }

  private static class StringParser {
    private int _current;
    private int _length;

    private String _string;

    private static final int SYMBOL = 1;
    private static final int LETTER = 2;
    private static final int DIGIT = 3;

    StringParser(String string)
    {
      _string = string;
      _length = string.length();
      _current = 0;
    }

    public boolean hasNext()
    {
      return _current < _length;
    }

    public String next()
    {
      int start;
      int type;

      try {
        char character = _string.charAt(_current);

        if (character == '0') {
          _current++;
          return "0";
        }
        else if (Character.isLetter(character))
          type = LETTER;
        else if (Character.isDigit(character))
          type = DIGIT;
        else
          type = SYMBOL;

        for (start = _current; _current < _length; _current++) {
          if (type == LETTER && Character.isLetter(_string.charAt(_current)))
          {
          }
          else if (type == DIGIT && Character.isDigit(_string.charAt(_current)))
          {
          }
          else if (type == SYMBOL &&
                   !Character.isLetterOrDigit(_string.charAt(_current))) {
          }
          else
            break;
        }

        return _string.substring(start, _current);
      }
      catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
        return null;
      }
    }
  }
}
