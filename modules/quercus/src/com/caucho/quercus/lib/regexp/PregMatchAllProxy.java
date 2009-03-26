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
public class PregMatchAllProxy
{
  private final PregMatchAllResult []_results = new PregMatchAllResult[5];
  
  private int _hits;
  private int _total;
  private boolean _isPassthru;
  
  private static final int MIN_HITS = 5;
  private static final int MAX_TOTAL = 20;
  
  public PregMatchAllProxy()  
  {
  }
  
  public Value preg_match_all(Env env, Regexp regexp, StringValue subject,
                              Value matchRef, int flags, int offset)
  {
    if (_isPassthru) {
      return RegexpModule.preg_match_all(env, regexp, subject, matchRef,
                                         flags, offset);
    }
    else if (_total >= MAX_TOTAL && _hits < MIN_HITS) {
      _isPassthru = true;
      
      for (int i = 0; i < _results.length; i++) {
        _results[i] = null;
      }
      
      return RegexpModule.preg_match_all(env, regexp, subject, matchRef,
                                         flags, offset);
    }
    
    for (int i = 0; i < _results.length; i++) {
      PregMatchAllResult result = _results[i];
      
      if (result == null) {
        Value val = RegexpModule.preg_match_all(env, regexp, subject, matchRef,
                                                flags, offset);
        
        _results[i] = new PregMatchAllResult(regexp, subject, matchRef,
                                             flags, offset, val);
        
        return val;
      }
      else if (result.equals(regexp, subject, flags, offset)) {
        if (_total < MAX_TOTAL) {
          _total++;
          _hits++;
        }

        return result.get(matchRef);
      }
    }
    
    if (_total < MAX_TOTAL) {
      _total++;
    }
    
    return RegexpModule.preg_match_all(env, regexp, subject, matchRef,
                                       flags, offset);
  }
  
  static class PregMatchAllResult
  {
    final Regexp _regexp;
    final StringValue _subject;
    final Value _matchRef;
    final int _flags;
    final int _offset;
    
    final Value _result;
    
    PregMatchAllResult(Regexp regexp, StringValue subject, Value matchRef,
                       int flags, int offset,
                       Value result)
    {
      _regexp = regexp;
      _subject = subject;
      _matchRef = matchRef.copy();
      
      _flags = flags;
      _offset = offset;
      _result = result.copy();
    }
    
    public boolean equals(Regexp regexp, StringValue subject,
                          int flags, int offset)
    {
      return regexp == _regexp && flags == _flags && offset == _offset
             && subject.equals(_subject);
    }
    
    public Value get(Value matchRef)
    {
      matchRef.set(_matchRef.copy());
      
      return _result.copy();
    }
  }
}
