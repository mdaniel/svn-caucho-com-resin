/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

import com.caucho.quercus.QuercusRuntimeException;
import com.caucho.quercus.env.*;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.Reference;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PHP regexp routines.
 */
public class QuercusRegexpModule
  extends AbstractQuercusModule
{
  private static final L10N L = new L10N(QuercusRegexpModule.class);

  private static final int REGEXP_EVAL = 0x01;

  public static final int PREG_PATTERN_ORDER = 0x01;
  public static final int PREG_SET_ORDER = 0x02;
  public static final int PREG_OFFSET_CAPTURE = 0x04;

  public static final int PREG_SPLIT_NO_EMPTY = 0x01;
  public static final int PREG_SPLIT_DELIM_CAPTURE = 0x02;
  public static final int PREG_SPLIT_OFFSET_CAPTURE = 0x04;

  public static final int PREG_GREP_INVERT = 1;

  public static final boolean [] PREG_QUOTE = new boolean[256];

  private static final LruCache<String, Pattern> _patternCache
    = new LruCache<String, Pattern>(1024);

  private static final LruCache<String, ArrayList<Replacement>> _replacementCache
    = new LruCache<String, ArrayList<Replacement>>(1024);

  private static final HashMap<String, Value> _constMap
    = new HashMap<String, Value>();

  /**
   * Returns the index of the first match.
   *
   * @param env the calling environment
   */
  public static Value ereg(Env env,
                           String pattern,
                           String string,
                           @Optional Value regsV)
    throws Throwable
  {
    return ereg(env, pattern, string, regsV, 0);
  }

  /**
   * Returns the index of the first match.
   *
   * @param env the calling environment
   */
  public static Value eregi(Env env,
                            String pattern,
                            String string,
                            @Optional Value regsV)
    throws Throwable
  {
    return ereg(env, pattern, string, regsV, Pattern.CASE_INSENSITIVE);
  }

  /**
   * Returns the index of the first match.
   *
   * @param env the calling environment
   */
  private static Value ereg(Env env,
                            String rawPattern,
                            String string,
                            Value regsV,
                            int flags)
    throws Throwable
  {
    String cleanPattern = cleanRegexp(rawPattern, false);

    Pattern pattern = Pattern.compile(cleanPattern, flags);
    Matcher matcher = pattern.matcher(string);

    if (! (matcher.find()))
      return BooleanValue.FALSE;

    if (regsV != null)
      regsV = regsV.toValue();

    if (regsV instanceof ArrayValue) {
      ArrayValue regs = (ArrayValue) regsV;

      regs.put(LongValue.ZERO, new StringValue(matcher.group()));
      int count = matcher.groupCount();

      for (int i = 1; i <= count; i++) {
        regs.put(new LongValue(i), new StringValue(matcher.group(i)));
      }

      int len = matcher.end() - matcher.start();

      if (len == 0)
        return LongValue.ONE;
      else
        return new LongValue(len);
    }
    else {
      return LongValue.ONE;
    }
  }

  /**
   * Returns the index of the first match.
   *
   * @param env the calling environment
   */
  public static int preg_match(Env env,
                               String patternString,
                               String string,
                               @Optional @Reference Value matchRef,
                               @Optional int flags,
                               @Optional int offset)
    throws Throwable
  {
    if (patternString.length() < 2) {
      env.warning(L.l("  Pattern must have at least opening and closing delimiters"));
      return 0;
    }
    
    Pattern pattern = compileRegexp(patternString);
    Matcher matcher = pattern.matcher(string);

    ArrayValue regs;
    
    if (matchRef instanceof DefaultValue)
      regs = null;
    else
      regs = new ArrayValueImpl();
    
    if (! (matcher.find(offset))) {
      matchRef.set(regs);
      return 0;
    }
    
    boolean isOffsetCapture = (flags & PREG_OFFSET_CAPTURE) != 0;

    if (regs != null) {
      if (isOffsetCapture) {
        ArrayValueImpl part = new ArrayValueImpl();
        part.append(new StringValue(matcher.group()));
        part.append(new LongValue(matcher.start()));

        regs.put(LongValue.ZERO, part);
      }
      else
        regs.put(LongValue.ZERO, new StringValue(matcher.group()));

      int count = matcher.groupCount();

      for (int i = 1; i <= count; i++) {
        Value value;
        if (matcher.group(i) != null)
          value = new StringValue(matcher.group(i));
        else
          value = NullValue.NULL;

        if (isOffsetCapture) {
          ArrayValueImpl part = new ArrayValueImpl();
          part.append(value);
          part.append(new LongValue(matcher.start(i)));

          regs.put(new LongValue(i), part);
        }
        else
          regs.put(new LongValue(i), value);
      }

      matchRef.set(regs);
    }

    return 1;
  }

  /**
   * Returns the index of the first match.
   *
   * @param env the calling environment
   */
  public static int preg_match_all(Env env,
                                   String patternString,
                                   String subject,
                                   @Reference Value matchRef,
                                   @Optional("PREG_PATTERN_ORDER") int flags,
                                   @Optional int offset)
    throws Throwable
  {
     if (patternString.length() < 2) {
      env.warning(L.l("Pattern must have at least opening and closing delimiters"));
      return 0;
    }

    if (((flags & PREG_PATTERN_ORDER) != 0) && ((flags & PREG_SET_ORDER) != 0)) {
      env.warning((L.l("Cannot combine PREG_PATTER_ORDER and PREG_SET_ORDER")));
      return 0;
    }
    
    Pattern pattern = compileRegexp(patternString);
    Matcher matcher = pattern.matcher(subject);

    ArrayValue matches;

    if (matchRef instanceof ArrayValue)
      matches = (ArrayValue) matchRef;
    else
      matches = new ArrayValueImpl();

    matches.clear();

    matchRef.set(matches);

    ArrayList<ArrayValue> matchList = new ArrayList<ArrayValue>();

    if ((flags & PREG_PATTERN_ORDER) != 0) {
      for (int j = 0; j <= matcher.groupCount(); j++) {
        ArrayValue values = new ArrayValueImpl();
        matches.put(values);
        matchList.add(values);
      }
    }

    if (! (matcher.find())) {
      return 0;
    }

    int count = 0;

    do {
      count++;

      if ((flags & PREG_PATTERN_ORDER) != 0) {
        for (int j = 0; j <= matcher.groupCount(); j++) {
          ArrayValue values = matchList.get(j);

          String groupValue = matcher.group(j);

          Value result = NullValue.NULL;
          
          if (groupValue != null) {
            if ((flags & PREG_OFFSET_CAPTURE) != 0) {
              result = new ArrayValueImpl();
              result.put(new StringValue(groupValue));
              result.put(new LongValue(matcher.start(j)));
            } else {
                result = new StringValue(groupValue);
            }
          }
          
          values.put(result);
        }
      }
      else if ((flags & PREG_SET_ORDER) != 0) {
        ArrayValue matchResult = new ArrayValueImpl();
        matches.put(matchResult);

        for (int j = 0; j <= matcher.groupCount(); j++) {
          String groupValue = matcher.group(j);

          Value result = NullValue.NULL;
          
          if (groupValue != null) {
            if ((flags & PREG_OFFSET_CAPTURE) != 0) {
              result = new ArrayValueImpl();
              result.put(new StringValue(groupValue));
              result.put(new LongValue(matcher.start(j)));
            } else {
              result = new StringValue(groupValue);
            }
          }
          matchResult.put(result);
        }
      }
      else {
        throw new UnsupportedOperationException();
      }
    } while (matcher.find());

    return count;
  }

  /**
   * Quotes regexp values
   */
  public static String preg_quote(String string,
                                  @Optional String delim)
    throws Throwable
  {
    StringBuilder sb = new StringBuilder();

    boolean []extra = null;

    if (delim != null && ! delim.equals("")) {
      extra = new boolean[256];

      for (int i = 0; i < delim.length(); i++)
        extra[delim.charAt(i)] = true;
    }

    int length = string.length();
    for (int i = 0; i < length; i++) {
      char ch = string.charAt(i);

      if (ch >= 256)
        sb.append(ch);
      else if (PREG_QUOTE[ch]) {
        sb.append('\\');
        sb.append(ch);
      }
      else if (extra != null && extra[ch]) {
        sb.append('\\');
        sb.append(ch);
      }
      else
        sb.append(ch);
    }

    return sb.toString();
  }

  /**
   * Replaces values using regexps
   */
  public static Value preg_replace(Env env,
                                   Value patternValue,
                                   Value replacement,
                                   String subject,
                                   @Optional("4294967296") long limit,
                                   @Optional @Reference Value countV)
    throws Throwable
  {
    String string = subject;

    if (patternValue.isArray() && replacement.isArray()) {
      ArrayValue patternArray = (ArrayValue) patternValue;
      ArrayValue replacementArray = (ArrayValue) replacement;

      Iterator<Value> patternIter = patternArray.values().iterator();
      Iterator<Value> replacementIter = replacementArray.values().iterator();

      while (patternIter.hasNext() && replacementIter.hasNext()) {
        string = pregReplaceString(env,
                                   patternIter.next().toString(),
                                   replacementIter.next().toString(),
                                   string,
                                   limit,
                                   countV);
      }
    }
    else if (patternValue.isArray()) {
      ArrayValue patternArray = (ArrayValue) patternValue;

      Iterator<Value> patternIter = patternArray.values().iterator();

      while (patternIter.hasNext()) {
        string = pregReplaceString(env,
                                   patternIter.next().toString(),
                                   replacement.toString(),
                                   string,
                                   limit,
                                   countV);
      }
    }
    else {
      string = pregReplaceString(env,
                                 patternValue.toString(),
                                 replacement.toString(),
                                 string,
                                 limit,
                                 countV);
    }

    return new StringValue(string);
  }

  /**
   * Replaces values using regexps
   */
  private static String pregReplaceString(Env env,
                                          String patternString,
                                          String replacement,
                                          String subject,
                                          long limit,
                                          Value countV)
    throws Throwable
  {
    Pattern pattern = compileRegexp(patternString);

    int patternFlags = regexpFlags(patternString);

    ArrayList<Replacement> replacementProgram
      = _replacementCache.get(replacement);

    if (replacementProgram == null) {
      replacementProgram = compileReplacement(env, replacement);
      _replacementCache.put(replacement, replacementProgram);
    }

    return preg_replace_impl(env, pattern, replacementProgram,
                             subject, countV,
                             (patternFlags & REGEXP_EVAL) != 0);
  }

  /**
   * Replaces values using regexps
   */
  public static Value ereg_replace(Env env,
                                   String patternString,
                                   String replacement,
                                   String subject)
    throws Throwable
  {
    Pattern pattern = Pattern.compile(cleanRegexp(patternString, false));

    ArrayList<Replacement> replacementProgram
      = _replacementCache.get(replacement);

    if (replacementProgram == null) {
      replacementProgram = compileReplacement(env, replacement);
      _replacementCache.put(replacement, replacementProgram);
    }

    String result = preg_replace_impl(env, pattern, replacementProgram,
                                      subject, NullValue.NULL, false);

    return new StringValue(result);
  }

  /**
   * Replaces values using regexps
   */
  public static Value eregi_replace(Env env,
                                    String patternString,
                                    String replacement,
                                    String subject)
    throws Throwable
  {
    Pattern pattern = Pattern.compile(cleanRegexp(patternString, false),
                                      Pattern.CASE_INSENSITIVE);

    ArrayList<Replacement> replacementProgram
      = _replacementCache.get(replacement);

    if (replacementProgram == null) {
      replacementProgram = compileReplacement(env, replacement);
      _replacementCache.put(replacement, replacementProgram);
    }

    String result = preg_replace_impl(env, pattern, replacementProgram,
                                      subject, NullValue.NULL, false);

    return new StringValue(result);
  }

  /**
   * Replaces values using regexps
   */
  private static String preg_replace_impl(Env env,
                                          Pattern pattern,
                                          ArrayList<Replacement> replacementList,
                                          String subject,
                                          Value countV,
                                          boolean isEval)
    throws Throwable
  {
    Matcher matcher = pattern.matcher(subject);

    StringBuilder result = new StringBuilder();
    int tail = 0;

    int replacementLen = replacementList.size();

    while (matcher.find()) {
      if (tail < matcher.start())
        result.append(subject.substring(tail, matcher.start()));

      if (isEval) {
        StringBuilder evalString = new StringBuilder();

        for (int i = 0; i < replacementLen; i++) {
          Replacement replacement = replacementList.get(i);

          replacement.eval(evalString, matcher);
        }

        result.append(env.evalCode(evalString.toString()));
      }
      else {
        for (int i = 0; i < replacementLen; i++) {
          Replacement replacement = replacementList.get(i);

          replacement.eval(result, matcher);
        }
      }

      tail = matcher.end();
    }

    if (tail < subject.length())
      result.append(subject.substring(tail));

    return result.toString();
  }

  /**
   * Replaces values using regexps
   */
  public static Value preg_replace_callback(Env env,
                                            String patternString,
                                            Callback fun,
                                            String subject,
                                            @Optional("4294967296") long limit,
                                            @Optional @Reference Value countV)
    throws Throwable
  {
    Pattern pattern = compileRegexp(patternString);

    Matcher matcher = pattern.matcher(subject);

    StringBuilder result = new StringBuilder();
    int tail = 0;

    while (matcher.find()) {
      if (tail < matcher.start())
        result.append(subject.substring(tail, matcher.start()));

      ArrayValue regs = new ArrayValueImpl();

      for (int i = 0; i <= matcher.groupCount(); i++) {
        String group = matcher.group(i);

        if (group != null)
          regs.put(new StringValue(group));
        else
          regs.put(StringValue.EMPTY);
      }

      Value replacement = fun.eval(env, regs);

      result.append(replacement);

      tail = matcher.end();
    }

    if (tail < subject.length())
      result.append(subject.substring(tail));

    return new StringValue(result.toString());
  }

  /**
   * Returns array of substrings or
   * of arrays ([0] => substring [1] => offset) if
   * PREG_SPLIT_OFFSET_CAPTURE is set
   *
   * @param env the calling environment
   */
  public static Value preg_split(Env env,
                                 String patternString,
                                 String string,
                                 @Optional("-1") long limit,
                                 @Optional int flags)
    throws Throwable
  {
    Pattern pattern = compileRegexp(patternString);

    if (limit < 0)
      limit = Long.MAX_VALUE;

    ArrayValue result = new ArrayValueImpl();
    int head = 0;
    Matcher matcher = pattern.matcher(string);
    long count = 0;
    
    while ((matcher.find()) && (count < limit)) {
      // If empty and we are to skip empty strings, then skip
      if ((flags & PREG_SPLIT_NO_EMPTY) != 0) {
        if (head == matcher.start()) {
          head = matcher.end();
          continue;
        }
      }
      
      String value;
      
      // If at limit, then just output the rest of string
      if (count == limit - 1) {
        value = string.substring(head);
        head = string.length();
      } else {
        value = string.substring(head, matcher.start());
        head = matcher.end();
      }
      if ((flags & PREG_SPLIT_OFFSET_CAPTURE) != 0) {
        
        ArrayValue part = new ArrayValueImpl();
        part.put(new StringValue(value));
        part.put(new LongValue(head));
        
        result.put(part);
      } else {       
        result.put(new StringValue(value));
      }

      count++;
      
      if ((flags & PREG_SPLIT_DELIM_CAPTURE) != 0) {
       for (int i = 1; i <= matcher.groupCount(); i++) {
          if ((flags & PREG_SPLIT_OFFSET_CAPTURE) != 0) {
            ArrayValue part = new ArrayValueImpl();
            part.put(new StringValue(matcher.group(i)));
            part.put(new LongValue(matcher.start()));
            
            result.put(part);
          } else {
            result.put(new StringValue(matcher.group(i)));
          }
        }
      }
    }
    
    if (head < string.length()) {
      
      if ((flags & PREG_SPLIT_OFFSET_CAPTURE) != 0) {
        
        ArrayValue part = new ArrayValueImpl();
        part.put(new StringValue(string.substring(head)));
        part.put(new LongValue(head));
  
        result.put(part);
      } else {
        result.put(new StringValue(string.substring(head)));
      }
    }

    return result;
  }

  /**
   * Returns the index of the first match.
   *
   * @param env the calling environment
   */
  public static Value split(Env env,
                            String patternString,
                            String string,
                            @Optional long limit)
    throws Throwable
  {
    patternString = cleanRegexp(patternString, false);

    Pattern pattern = Pattern.compile(patternString);

    String []value = pattern.split(string);

    ArrayValue result = new ArrayValueImpl();

    for (int i = 0; i < value.length; i++)
      result.append(new StringValue(value[i]));

    return result;
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
  public static ArrayValue preg_grep(Env env,
                                     String patternString,
                                     ArrayValue input,
                                     @Optional("0") int flag)
    throws Throwable
  {
    // php/151b

    Pattern pattern = compileRegexp(patternString);

    Matcher matcher = null;

    ArrayValue matchArray = new ArrayValueImpl();

    for (Map.Entry<Value, Value> entry : input.entrySet()) {
      Value entryValue = entry.getValue();
      Value entryKey = entry.getKey();

      matcher = pattern.matcher(entryValue.toString());

      boolean found = matcher.find();

      if (!found && (flag == PREG_GREP_INVERT))
        matchArray.append(entryKey, entryValue);
      else if (found && (flag != PREG_GREP_INVERT))
        matchArray.append(entryKey, entryValue);
    }

    return matchArray;
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
  public static ArrayValue spliti(Env env,
                                  String patternString,
                                  String string,
                                  @Optional("-1") long limit)
  {
    // php/151c

    patternString = cleanRegexp(patternString, false);

    Pattern pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);

    String groups[] = pattern.split(string, (int) limit);

    ArrayValue result = new ArrayValueImpl();

    for (int k = 0; k < groups.length; k++)
      result.append(new StringValue(groups[k]));

    return result;
  }

  private static Pattern compileRegexp(String rawRegexp)
  {
    Pattern pattern = _patternCache.get(rawRegexp);

    if (pattern != null)
      return pattern;

    char delim = rawRegexp.charAt(0);
    if (delim == '{')
      delim = '}';
    else if (delim == '[')
      delim = ']';

    int tail = rawRegexp.lastIndexOf(delim);

    if (tail <= 0)
      throw new IllegalStateException(L.l(
        "Can't find second {0} in regexp '{1}'.",
        String.valueOf((char) delim),
        rawRegexp));

    int len = rawRegexp.length();

    int flags = 0;
    boolean isExt = false;
    boolean isGreedy = true;

    for (int i = tail + 1; i < len; i++) {
      char ch = rawRegexp.charAt(i);

      switch (ch) {
      case 'i':
        flags |= Pattern.CASE_INSENSITIVE;
        break;
      case 's':
        flags |= Pattern.DOTALL;
        break;
      case 'x':
        flags |= Pattern.COMMENTS;
        break;
      case 'm':
        flags |= Pattern.MULTILINE;
        break;
      case 'U':
        isGreedy = false;
        break;
      }
    }

    String regexp = rawRegexp.substring(1, tail);

    regexp = cleanRegexp(regexp, (flags & Pattern.COMMENTS) != 0);

    if (! isGreedy)
      regexp = toNonGreedy(regexp);

    pattern = Pattern.compile(regexp, flags);

    _patternCache.put(rawRegexp, pattern);

    return pattern;
  }

  private static int regexpFlags(String rawRegexp)
  {
    char delim = rawRegexp.charAt(0);
    if (delim == '{')
      delim = '}';
    else if (delim == '[')
      delim = ']';

    int tail = rawRegexp.lastIndexOf(delim);

    if (tail <= 0)
      throw new IllegalStateException(L.l(
        "Can't find second {0} in regexp '{1}'.",
        String.valueOf((char) delim),
        rawRegexp));

    int len = rawRegexp.length();

    int flags = 0;

    for (int i = tail + 1; i < len; i++) {
      char ch = rawRegexp.charAt(i);

      switch (ch) {
      case 'e':
        flags |= REGEXP_EVAL;
        break;
      }
    }

    return flags;
  }

  private static ArrayList<Replacement>
    compileReplacement(Env env, String replacement)
    throws Exception
  {
    ArrayList<Replacement> program = new ArrayList<Replacement>();
    StringBuilder text = new StringBuilder();

    for (int i = 0; i < replacement.length(); i++) {
      char ch = replacement.charAt(i);

      if ((ch == '\\' || ch == '$') && i + 1 < replacement.length()) {
        char digit;

        if ('0' <= (digit = replacement.charAt(i + 1)) && digit <= '9') {
          int group = digit - '0';
          i++;

          if (i + 1 < replacement.length() &&
              '0' <= (digit = replacement.charAt(i + 1)) && digit <= '9') {
            group = 10 * group + digit - '0';
            i++;
          }

          if (text.length() > 0)
            program.add(new TextReplacement(text.toString()));

          program.add(new GroupReplacement(group));

          text.setLength(0);
        }
        else if (ch == '\\') {
          i++;

          text.append(digit);
        }
        else if (ch == '$' && digit == '{') {
          i += 2;

          int group = 0;

          while (i < replacement.length() &&
                 '0' <= (digit = replacement.charAt(i)) && digit <= '9') {
            group = 10 * group + digit - '0';

            i++;
          }

          if (digit != '}') {
            env.warning(L.l("bad regexp {0}", replacement));
            throw new Exception("bad regexp");
          }


          if (text.length() > 0)
            program.add(new TextReplacement(text.toString()));

          program.add(new GroupReplacement(group));
          text.setLength(0);
        }
        else
          text.append(ch);
      }
      else
        text.append(ch);
    }

    if (text.length() > 0)
      program.add(new TextReplacement(text.toString()));

    return program;
  }

  private static final String [] POSIX_CLASSES = {
    "[:alnum:]", "[:alpha:]", "[:blank:]", "[:cntrl:]",
    "[:digit:]", "[:graph:]", "[:lower:]", "[:print:]",
    "[:punct:]", "[:space:]", "[:upper:]", "[:xdigit:]"
  };

  private static final String [] REGEXP_CLASSES = {
    "\\p{Alnum}", "\\p{Alpha}", "\\p{Blank}", "\\p{Cntrl}",
    "\\p{Digit}", "\\p{Graph}", "\\p{Lower}", "\\p{Print}",
    "\\p{Punct}", "\\p{Space}", "\\p{Upper}", "\\p{XDigit}"
  };

  /**
   * Cleans the regexp from valid values that the Java regexps can't handle.
   * Currently "+?".
   */
  // XXX: not handling '['
  private static String cleanRegexp(String regexp, boolean isComments)
  {
    int len = regexp.length();

    StringBuilder sb = new StringBuilder();
    char quote = 0;

    for (int i = 0; i < len; i++) {
      char ch = regexp.charAt(i);

      switch (ch) {
      case '\\':
        sb.append(ch);

        if (i + 1 < len) {
          i++;

          ch = regexp.charAt(i);

          if ('1' <= ch && ch <= '3') {
            // Java's regexp requires \0 for octal

            sb.append('\\');
            sb.append('0');
            sb.append(ch);
          }
          else if (ch == 'x' && i + 1 < len && regexp.charAt(i + 1) == '{') {
            int tail = regexp.indexOf('}', i + 1);

            if (tail > 0) {
              String hex = regexp.substring(i + 2, tail);

              if (hex.length() == 2)
                sb.append("x" + hex);
              else if (hex.length() == 4)
                sb.append("u" + hex);
              else
                throw new QuercusRuntimeException(L.l("illegal hex escape"));

              i = tail;
            }
            else {
              sb.append("\\x");
            }
          }
          else
            sb.append(ch);
        }
        break;

      case '[':
        if (quote == '[') {
          if (i + 1 < len && regexp.charAt(i + 1) == ':') {
            String test = regexp.substring(i);
            boolean hasMatch = false;

            for (int j = 0; j < POSIX_CLASSES.length; j++) {
              if (test.startsWith(POSIX_CLASSES[j])) {
                hasMatch = true;

                sb.append(REGEXP_CLASSES[j]);

                i += POSIX_CLASSES[j].length() - 1;
              }
            }

            if (! hasMatch)
              sb.append("\\[");
          }
          else
            sb.append("\\[");
        }
        else
          sb.append(ch);

        if (quote == 0)
          quote = ch;
        break;

      case '#':
        if (quote == '[' && isComments)
          sb.append("\\#");
        else
          sb.append(ch);
        break;

      case ']':
        sb.append(ch);

        if (quote == '[')
          quote = 0;
        break;

      case '{':
        if (i + 1 < len &&
            ('0' <= (ch = regexp.charAt(i + 1)) && ch <= '9' || ch == ',')) {
          sb.append("{");
          for (i++;
               i < len &&
               ('0' <= (ch = regexp.charAt(i)) && ch <= '9' || ch == ',');
               i++) {
            sb.append(ch);
          }

          if (i < len)
            sb.append(regexp.charAt(i));
        }
        else {
          sb.append("\\{");
        }
        break;

      case '}':
        sb.append("\\}");
        break;

      default:
        sb.append(ch);
      }
    }

    return sb.toString();
  }

  /**
   * Converts to non-greedy.
   */
  private static String toNonGreedy(String regexp)
  {
    int len = regexp.length();

    StringBuilder sb = new StringBuilder();
    char quote = 0;

    for (int i = 0; i < len; i++) {
      char ch = regexp.charAt(i);

      switch (ch) {
      case '\\':
        sb.append(ch);

        if (i + 1 < len) {
          sb.append(regexp.charAt(i + 1));
          i++;
        }
        break;

      case '[':
        sb.append(ch);

        if (quote == 0)
          quote = ch;
        break;

      case ']':
        sb.append(ch);

        if (quote == '[')
          quote = 0;
        break;

      case '*':
      case '?':
      case '+':
        sb.append(ch);

        if (i + 1 < len && (ch = regexp.charAt(i + 1)) != '?') {
          sb.append('?');
        }
        break;

      default:
        sb.append(ch);
      }
    }

    return sb.toString();
  }

  static class Replacement {
    void eval(StringBuilder sb, Matcher matcher)
    {
    }
  }

  static class TextReplacement
    extends Replacement
  {
    private String _text;

    TextReplacement(String text)
    {
      _text = text;
    }

    void eval(StringBuilder sb, Matcher matcher)
    {
      sb.append(_text);
    }
  }

  static class GroupReplacement
    extends Replacement
  {
    private int _group;

    GroupReplacement(int group)
    {
      _group = group;
    }

    void eval(StringBuilder sb, Matcher matcher)
    {
      if (_group <= matcher.groupCount())
        sb.append(matcher.group(_group));
    }
  }

  static {
    PREG_QUOTE['\\'] = true;
    PREG_QUOTE['+'] = true;
    PREG_QUOTE['*'] = true;
    PREG_QUOTE['?'] = true;
    PREG_QUOTE['['] = true;
    PREG_QUOTE['^'] = true;
    PREG_QUOTE[']'] = true;
    PREG_QUOTE['$'] = true;
    PREG_QUOTE['('] = true;
    PREG_QUOTE[')'] = true;
    PREG_QUOTE['{'] = true;
    PREG_QUOTE['}'] = true;
    PREG_QUOTE['='] = true;
    PREG_QUOTE['!'] = true;
    PREG_QUOTE['<'] = true;
    PREG_QUOTE['>'] = true;
    PREG_QUOTE['|'] = true;
    PREG_QUOTE[':'] = true;
  }
}
