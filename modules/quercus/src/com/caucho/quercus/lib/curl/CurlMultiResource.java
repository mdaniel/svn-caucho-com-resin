/*
 * Copyright (c) 1998-2014 Caucho Technology -- all rights reserved
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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.curl;

import java.util.ArrayList;

import com.caucho.quercus.annotation.ResourceType;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.ResourceValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;

@ResourceType("curl_multi")
public class CurlMultiResource extends ResourceValue
{
  private ArrayList<CurlResource> _curlList
    = new ArrayList<CurlResource>();

  private ArrayList<CurlResource> _msgQueue
    = new ArrayList<CurlResource>();

  private int _runningCount = -1;

  public CurlMultiResource()
  {
  }

  public void addCurl(CurlResource curl)
  {
    _curlList.add(curl);
  }

  public void removeCurl(CurlResource curl)
  {
    boolean isRemoved = _curlList.remove(curl);

    _msgQueue.remove(curl);
  }

  public int execute(Env env, Value stillRunning)
  {
    if (_runningCount == 0) {
      stillRunning.set(LongValue.ZERO);

      return CurlModule.CURLM_OK;
    }
    else if (_runningCount < 0) {
      _runningCount = _curlList.size();

      for (CurlResource curl : _curlList) {
        curl.execute(env, false);

        _msgQueue.add(curl);
      }
    }

    _runningCount--;

    stillRunning.set(LongValue.create(_runningCount));

    if (_runningCount == 0) {
      return CurlModule.CURLM_OK;
    }
    else {
      return CurlModule.CURLM_CALL_MULTI_PERFORM;
    }
  }

  public Value readInfo(Env env, Value msgsInQueue)
  {
    if (_msgQueue.size() == 0) {
      return BooleanValue.FALSE;
    }

    StringValue msgStr = env.createString("msg");
    StringValue resultStr = env.createString("result");
    StringValue handleStr = env.createString("handle");

    CurlResource curl = _msgQueue.remove(0);

    ArrayValue array = new ArrayValueImpl();

    array.put(msgStr, LongValue.create(CurlModule.CURLMSG_DONE));
    array.put(resultStr, LongValue.create(CurlModule.CURLE_OK));
    array.put(handleStr, curl);

    msgsInQueue.set(LongValue.create(_msgQueue.size()));

    return array;
  }

  public ArrayList<CurlResource> getCurlList()
  {
    return _curlList;
  }

  public int getCurlListSize()
  {
    return _curlList.size();
  }
}
