/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
 *
 * Caucho Technology permits modification and use of this file in
 * source and binary form ("the Software") subject to the Caucho
 * Developer Source License 1.1 ("the License") which accompanies
 * this file.  The License is also available at
 *   http://www.caucho.com/download/cdsl1-1.xtp
 *
 * In addition to the terms of the License, the following conditions
 * must be met:
 *
 * 1. Each copy or derived work of the Software must preserve the copyright
 *    notice and this notice unmodified.
 *
 * 2. Each copy of the Software in source or binary form must include 
 *    an unmodified copy of the License in a plain ASCII text file named
 *    LICENSE.
 *
 * 3. Caucho reserves all rights to its names, trademarks and logos.
 *    In particular, the names "Resin" and "Caucho" are trademarks of
 *    Caucho and may not be used to endorse products derived from
 *    this software.  "Resin" and "Caucho" may not appear in the names
 *    of products derived from this software.
 *
 * This Software is provided "AS IS," without a warranty of any kind. 
 * ALL EXPRESS OR IMPLIED REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED.
 *
 * CAUCHO TECHNOLOGY AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE OR ANY THIRD PARTY AS A RESULT OF USING OR
 * DISTRIBUTING SOFTWARE. IN NO EVENT WILL CAUCHO OR ITS LICENSORS BE LIABLE
 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE SOFTWARE, EVEN IF HE HAS BEEN ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGES.      
 *
 * @author Scott Ferguson
 */

package com.caucho.server.admin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;

import com.caucho.util.Alarm;

/**
 * Remote mbean
 */
public class RemoteMBean {
  private AdminClient _client;
  private String _name;
  private HashMap<String,Object> _attr;
  private long _expireTime;
  
  RemoteMBean(AdminClient client, String name, HashMap<String,Object> attr)
  {
    _client = client;
    _name = name;
    _attr = attr;
    _expireTime = Alarm.getCurrentTime() + 5000L;
  }

  public Iterator iterator()
  {
    ArrayList<String> names = new ArrayList<String>(_attr.keySet());

    Collections.sort(names);

    return names.iterator();
  }
  
  /**
   * Returns the given attribute.
   */
  public Object __getField(String name)
  {
    fillAttributes();
    
    return _attr.get(name);
  }

  private void fillAttributes()
  {
    if (Alarm.getCurrentTime() < _expireTime)
      return;
    
    _expireTime = Alarm.getCurrentTime() + 5000L;
  }

  public String toString()
  {
    return "RemoteMBean[" + _name + "]";
  }
}
