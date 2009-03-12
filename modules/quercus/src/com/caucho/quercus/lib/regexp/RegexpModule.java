/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.lib.regexp;

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.Reference;
import com.caucho.quercus.annotation.UsesSymbolTable;
import com.caucho.quercus.env.*;
import com.caucho.quercus.module.AbstractQuercusModule;

/**
 * PHP regexp routines.
 */
public class RegexpModule
  extends AbstractQuercusModule
{
  public static final int PREG_REPLACE_EVAL = 0x01;
  public static final int PCRE_UTF8 = 0x02;

  public static final int PREG_PATTERN_ORDER = 0x01;
  public static final int PREG_SET_ORDER = 0x02;
  public static final int PREG_OFFSET_CAPTURE = 0x04;

  public static final int PREG_SPLIT_NO_EMPTY = 0x01;
  public static final int PREG_SPLIT_DELIM_CAPTURE = 0x02;
  public static final int PREG_SPLIT_OFFSET_CAPTURE = 0x04;

  public static final int PREG_GREP_INVERT = 1;
  
  public String []getLoadedExtensions()
  {
    return new String[] { "ereg", "pcre" };
  }

  /**
   * Returns the index of the first match.
   *
   * @param env the calling environment
   */
  public static Value ereg(Env env,
                           Value pattern,
                           StringValue string,
                           @Optional @Reference Value regsV)
  {
    if (useJavaRegexp(env))
      return JavaRegexpModule.ereg(env, pattern, string, regsV);
    else
      return CauchoRegexpModule.ereg(env, pattern, string, regsV);
  }

  /**
   * Returns the index of the first match.
   *
   * @param env the calling environment
   */
  public static Value eregi(Env env,
                            Value pattern,
                            StringValue string,
                            @Optional @Reference Value regsV)
  {
    if (useJavaRegexp(env))
      return JavaRegexpModule.eregi(env, pattern, string, regsV);
    else
      return CauchoRegexpModule.eregi(env, pattern, string, regsV);
  }

  /**
   * Returns the index of the first match.
   *
   * php/151u
   * The array that preg_match (PHP 5) returns does not have trailing unmatched
   * groups. Therefore, an unmatched group should not be added to the array
   * unless a matched group appears after it.  A couple applications like
   * Gallery2 expect this behavior in order to function correctly.
   * 
   * Only preg_match and preg_match_all(PREG_SET_ORDER) exhibits this odd
   * behavior.
   *
   * @param env the calling environment
   */
  /*
  public static Value preg_match(Env env,
				 StringValue regexp,
				 StringValue subject,
				 @Optional @Reference Value matchRef,
				 @Optional int flags,
				 @Optional int offset)
  {
    if (useJavaRegexp(env)) {
      return JavaRegexpModule.preg_match(env, regexp, subject, matchRef, flags, offset);
    }
    else
      return CauchoRegexpModule.preg_match(env, regexp, subject, matchRef, flags, offset);
  }
  */
  
  public static Value preg_match(Env env,
				 Regexp regexp,
				 StringValue subject,
				 @Optional @Reference Value matchRef,
				 @Optional int flags,
				 @Optional int offset)
  {
    return CauchoRegexpModule.cauchoPregMatch(env, regexp, subject,
					      matchRef, flags, offset);
  }

  /**
   * Returns the index of the first match.
   *
   * @param env the calling environment
   */
  public static Value preg_match_all(Env env,
				     StringValue regexp,
				     StringValue subject,
				     @Reference Value matchRef,
				     @Optional("PREG_PATTERN_ORDER") int flags,
				     @Optional int offset)
  {
    if (useJavaRegexp(env))
      return JavaRegexpModule.preg_match_all(env, regexp, subject, matchRef, flags, offset);
    else
      return CauchoRegexpModule.preg_match_all(env, regexp, subject, matchRef, flags, offset);
  }

  /**
   * Quotes regexp values
   */
  public static StringValue preg_quote(Env env,
				       StringValue string,
				       @Optional StringValue delim)
  {
    if (useJavaRegexp(env))
      return JavaRegexpModule.preg_quote(string, delim);
    else
      return CauchoRegexpModule.preg_quote(string, delim);
  }

  /**
   * Loops through subject if subject is array of strings
   *
   * @param env
   * @param pattern string or array
   * @param replacement string or array
   * @param subject string or array
   * @param limit
   * @param count
   * @return
   */
  @UsesSymbolTable
  public static Value preg_replace(Env env,
                                   Value pattern,
                                   Value replacement,
                                   Value subject,
                                   @Optional("-1") long limit,
                                   @Optional @Reference Value count)
  {
    if (useJavaRegexp(env))
      return JavaRegexpModule.preg_replace(env, pattern, replacement, subject, limit, count);
    else
      return CauchoRegexpModule.preg_replace(env, pattern, replacement, subject, limit, count);

  }

  /**
   * Simple preg_replace
   */
  /* XXX: resolution isn't handling this properly
  @UsesSymbolTable
  public static Value preg_replace(Env env,
                                   Regexp pattern,
                                   StringValue replacement,
                                   StringValue subject,
                                   @Optional("-1") long limit,
                                   @Optional @Reference Value count)
  {
    return CauchoRegexpModule.pregReplaceString(env,
						pattern,
						replacement,
						subject,
						limit,
						count);
  }
  */

  /**
   * Replaces values using regexps
   */
  public static Value ereg_replace(Env env,
                                   Value pattern,
                                   Value replacement,
                                   StringValue subject)
  {
    if (useJavaRegexp(env))
      return JavaRegexpModule.ereg_replace(env, pattern,
        replacement, subject);
    else
      return CauchoRegexpModule.ereg_replace(env, pattern,
        replacement, subject);
  }

  /**
   * Replaces values using regexps
   */
  public static Value eregi_replace(Env env,
                                    Value pattern,
                                    Value replacement,
                                    StringValue subject)
  {
    if (useJavaRegexp(env))
      return JavaRegexpModule.eregi_replace(env, pattern,
        replacement, subject);
    else
      return CauchoRegexpModule.eregi_replace(env, pattern,
        replacement, subject);
  }

  /**
   * Loops through subject if subject is array of strings
   *
   * @param env
   * @param pattern
   * @param fun
   * @param subject
   * @param limit
   * @param count
   * @return
   */
  public static Value preg_replace_callback(Env env,
                                            Value pattern,
                                            Callback fun,
                                            Value subject,
                                            @Optional("-1") long limit,
                                            @Optional @Reference Value count)
  {
    if (useJavaRegexp(env))
      return JavaRegexpModule.preg_replace_callback(env, pattern, fun, subject, limit, count);
    else
      return CauchoRegexpModule.preg_replace_callback(env, pattern, fun, subject, limit, count);
  }

  /**
   * Returns array of substrings or
   * of arrays ([0] => substring [1] => offset) if
   * PREG_SPLIT_OFFSET_CAPTURE is set
   *
   * @param env the calling environment
   */
  public static Value preg_split(Env env,
                                 StringValue patternString,
                                 StringValue string,
                                 @Optional("-1") long limit,
                                 @Optional int flags)
  {
    if (useJavaRegexp(env))
      return JavaRegexpModule.preg_split(env, patternString, string, limit, flags);
    else
      return CauchoRegexpModule.preg_split(env, patternString, string, limit, flags);
  }

  /**
   * Makes a regexp for a case-insensitive match.
   */
  public static StringValue sql_regcase(Env env, StringValue string)
  {
    if (useJavaRegexp(env))
      return JavaRegexpModule.sql_regcase(string);
    else
      return CauchoRegexpModule.sql_regcase(string);
  }

  /**
   * Returns the index of the first match.
   *
   * @param env the calling environment
   */
  public static Value split(Env env,
                            StringValue patternString,
                            StringValue string,
                            @Optional("-1") long limit)
  {
    if (useJavaRegexp(env))
      return JavaRegexpModule.split(env, patternString, string, limit);
    else
      return CauchoRegexpModule.split(env, patternString, string, limit);
  }

  /**
   * Returns an array of all the values that matched the given pattern if the
   * flag no flag is passed.  Otherwise it will return an array of all the
   * values that did not match.
   *
   * @param patternString the pattern
   * @param input the array to check the pattern against
   * @param flag 0 for matching and 1 for elements that do not match
   * @return an array of either matching elements are non-matching elements
   */
  public static Value preg_grep(Env env,
                                     StringValue patternString,
                                     ArrayValue input,
                                     @Optional("0") int flag)
  {
    if (useJavaRegexp(env))
      return JavaRegexpModule.preg_grep(env, patternString, input, flag);
    else
      return CauchoRegexpModule.preg_grep(env, patternString, input, flag);
  }

  /**
   * Returns an array of strings produces from splitting the passed string
   * around the provided pattern.  The pattern is case insensitive.
   *
   * @param patternString the pattern
   * @param string the string to split
   * @param limit if specified, the maximum number of elements in the array
   * @return an array of strings split around the pattern string
   */
  public static Value spliti(Env env,
                                  StringValue patternString,
                                  StringValue string,
                                  @Optional("-1") long limit)
  {
    if (useJavaRegexp(env))
      return JavaRegexpModule.spliti(env, patternString, string, limit);
    else
      return CauchoRegexpModule.spliti(env, patternString, string, limit);
  }
  
  private static boolean useJavaRegexp(Env env)
  {
    //return false;
    return false;
    // return ! env.getIniBoolean("caucho.resin_regexp");
  }
}
