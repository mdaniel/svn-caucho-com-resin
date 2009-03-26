/*
 * Copyright (c) 1998-2009 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.lib.regexp;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;

/**
 * XXX: experimental
 */
public class PregReplaceProxy
{
  // 5 to start seeing respectable hit rates for mediawiki
  private final PregReplaceResult []_results = new PregReplaceResult[5];
  
  private int _hits;
  private int _total;
  private boolean _isPassthru;
  
  private final int MIN_HITS = 5;
  private final int MAX_TOTAL = 20;
  
  public PregReplaceProxy()  
  {
  }
  
  public Value preg_replace(Env env, Regexp []regexpList,
                            Value replacement, Value subject,
                            long limit, Value countRef)
  {
    if (_isPassthru) {
      return RegexpModule.preg_replace(env, regexpList, replacement,
                                       subject, limit, countRef);
    }
    else if (_total >= MAX_TOTAL && _hits < MIN_HITS) {
      _isPassthru = true;
      
      for (int i = 0; i < _results.length; i++) {
        _results[i] = null;
      }

      return RegexpModule.preg_replace(env, regexpList, replacement,
                                       subject, limit, countRef);
    }
    
    for (int i = 0; i < regexpList.length; i++) {
      if (regexpList[i].isEval()) {
        if (_total < MAX_TOTAL)
          _total++;
        
        return RegexpModule.preg_replace(env, regexpList, replacement,
                                         subject, limit, countRef);
      }
    }
    
    for (int i = 0; i < _results.length; i++) {
      PregReplaceResult result = _results[i];
      
      if (result == null) {
        Value val = RegexpModule.preg_replace(env, regexpList, replacement,
                                              subject, limit, countRef);
        
        if (val.isNull()) {
          if (_total < MAX_TOTAL)
            _total++;
          
          return val;
        }
        
        _results[i] = new PregReplaceResult(regexpList, replacement,
                                            subject, limit, countRef, val);
        
        return val;
      }
      else if (result.equals(regexpList, replacement, subject, limit)) {
        if (_total < MAX_TOTAL) {
          _total++;
          _hits++;
        }

        return result.get(countRef);
      }
    }

    if (_total < MAX_TOTAL) {
      _total++;
    }
      
    return RegexpModule.preg_replace(env, regexpList, replacement,
                                     subject, limit, countRef);
  }
  
  static class PregReplaceResult
  {
    final Regexp []_regexpList;
    final Value _replacement;
    final Value _subject;
    final long _limit;
    final Value _count;
    
    final Value _result;
    
    PregReplaceResult(Regexp []regexpList, Value replacement,
                      Value subject, long limit, Value countRef,
                      Value result)
    {
      _regexpList = regexpList;
      _replacement = replacement;
      _subject = subject;
      _limit = limit;
      _count = countRef.toValue();
      
      _result = result.copy();
    }
    
    public boolean equals(Regexp []regexpList,
                          Value replacement, Value subject, long limit)
    {
      return regexpList == _regexpList && limit == _limit
             && replacement.equals(_replacement) && subject.equals(_subject);
    }
    
    public Value get(Value countRef)
    {
      countRef.set(_count);
      
      return _result.copy();
    }
  }
}
