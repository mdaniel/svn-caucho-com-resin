/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.jms.selector;

import com.caucho.jms.message.ObjectConverter;
import com.caucho.log.Log;
import com.caucho.util.L10N;

import javax.jms.JMSException;
import javax.jms.Message;
import java.util.logging.Logger;

/**
 * The base selector.
 */
abstract public class Selector  {
  protected static final Logger log = Log.open(Selector.class);
  static final L10N L = new L10N(Selector.class);

  protected final static Object NULL = new Object();

  /**
   * Evaluate the message.
   */
  abstract Object evaluate(Message message)
    throws JMSException;

  public boolean isMatch(Message message)
    throws JMSException
  {
    Object obj = evaluate(message);

    if (! (obj instanceof Boolean))
      return false;
    
    Boolean bool = (Boolean) obj;

    return bool.booleanValue();
  }

  protected static Boolean toBoolean(boolean value)
  {
    return value ? Boolean.TRUE : Boolean.FALSE;
  }

  boolean isBoolean()
  {
    return false;
  }

  boolean isUnknown()
  {
    return true;
  }

  boolean isNumber()
  {
    return false;
  }

  static boolean isDouble(Object obj)
  {
    return obj instanceof Double || obj instanceof Float;
  }

  static boolean isInteger(Object obj)
  {
    return (obj instanceof Integer ||
            obj instanceof Long ||
            obj instanceof Short ||
            obj instanceof Byte);
  }

  static boolean isNumber(Object obj)
  {
    return (obj instanceof Number);
  }

  protected long toLong(Object obj)
    throws JMSException
  {
    return ObjectConverter.toLong(obj);
  }

  protected double toDouble(Object obj)
    throws JMSException
  {
    return ObjectConverter.toDouble(obj);
  }
}
