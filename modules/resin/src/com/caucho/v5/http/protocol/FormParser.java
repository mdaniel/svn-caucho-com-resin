/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.CharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.http.protocol.BadRequestException;
import com.caucho.v5.io.i18n.ByteToChar;
import com.caucho.v5.util.CharCursor;
import com.caucho.v5.util.FreeList;
import com.caucho.v5.util.HashMapImpl;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.StringCharCursor;

/**
 * Form handling.
 */
public class FormParser
{
  private static final L10N L = new L10N(FormParser.class);
  private static final Logger log = Logger.getLogger(FormParser.class.getName());

  private static final FreeList<FormParser> _freeList = new FreeList<FormParser>(32);

  private final ByteToChar _converter = ByteToChar.create();


  public static FormParser allocate()
  {
    FormParser form = _freeList.allocate();

    if (form == null)
      form = new FormParser();

    return form;
  }

  public static void free(FormParser form)
  {
    _freeList.free(form);
  }
  
  public static HashMapImpl<String,String[]> parseQueryString(String query)
  {
    try {
      HashMapImpl<String,String[]> map = new HashMapImpl<>();
    
      new FormParser().parseQueryString(map, query, "UTF-8", true);
    
      return map;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Parses the values from a query string.
   *
   * @param table the hashtable which will contain the results
   * @param query the query string to evaluate
   * @param javaEncoding the Java name for the charset
   */
  public void parseQueryString(HashMapImpl<String,String[]> table,
                               String query,
                               String javaEncoding,
                               boolean isTop)
    throws IOException
  {
    CharCursor is = new StringCharCursor(query);

    ByteToChar converter = _converter;
    try {
      converter.setEncoding(javaEncoding);
    } catch (UnsupportedEncodingException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    
    parseQueryString(is, table, query, javaEncoding, isTop);
  }
  
  private void parseQueryString(CharCursor is,
                                HashMapImpl<String,String[]> table,
                                String query,
                                String javaEncoding,
                                boolean isTop)
    throws IOException
  {
    ByteToChar converter = _converter;
    int ch;
    
    while ((ch = is.current()) != CharacterIterator.DONE) {
      for (; Character.isWhitespace((char) ch) || ch == '&'; ch = is.next()) {
      }

      converter.clear();
      for (; ch != CharacterIterator.DONE && ch != '=' && ch != '&'; ch = is.next())
        readChar(converter, is, ch, isTop);

      String key = converter.getConvertedString();

      converter.clear();
      if (ch == '=') {
        ch = is.next();
      }
      
      for (; ch != CharacterIterator.DONE && ch != '&'; ch = is.next()) {
        readChar(converter, is, ch, isTop);
      }
      
      String value = converter.getConvertedString();
      
      // jsp/15n4, server/10yl
      if (! isTop) {
        parseQueryString(is, table, query, javaEncoding, isTop);
      }
      
      if (log.isLoggable(Level.FINE))
        log.fine("query: " + key + "=" + value);
      
      String []oldValue = table.get(key);
      
      if (key == null || key.equals("")) {
      }
      else if (oldValue == null) {
        table.put(key, new String[] { value });
      }
      else if (! isTop) {
        String []newValue = new String[oldValue.length + 1];
        System.arraycopy(oldValue, 0, newValue, 1, oldValue.length);
        newValue[0] = value;
        table.put(key, newValue);
      }
      else {
        String []newValue = new String[oldValue.length + 1];
        System.arraycopy(oldValue, 0, newValue, 0, oldValue.length);
        newValue[oldValue.length] = value;
        table.put(key, newValue);
      }
    }
  }
  
  public static Map<String,List<String>> parseQueryString2(String query,
                                                    String enc)
    throws IOException
  {
    CharCursor is = new StringCharCursor(query);

    HashMap<String,List<String>> map = new HashMap<>();
    
    parseQueryString(is, map, query, enc);
    
    return map;
  }
  
  private static void parseQueryString(CharCursor is,
                                Map<String,List<String>> map,
                                String query,
                                String enc)
    throws IOException
  {
    ByteToChar converter = ByteToChar.create();
    
    try {
      converter.setEncoding(enc);
    } catch (UnsupportedEncodingException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    
    boolean isTop = true;
    int ch;
    
    while ((ch = is.current()) != CharacterIterator.DONE) {
      for (; Character.isWhitespace((char) ch) || ch == '&'; ch = is.next()) {
      }

      converter.clear();
      for (; ch != CharacterIterator.DONE && ch != '=' && ch != '&'; ch = is.next()) {
        readChar(converter, is, ch, isTop);
      }

      String key = converter.getConvertedString();

      converter.clear();
      if (ch == '=') {
        ch = is.next();
      }
      
      for (; ch != CharacterIterator.DONE && ch != '&'; ch = is.next()) {
        readChar(converter, is, ch, isTop);
      }
      
      String value = converter.getConvertedString();
      
      if (log.isLoggable(Level.FINE))
        log.fine("query: " + key + "=" + value);
      
      List<String> list = map.get(key);
      
      if (key == null || key.equals("")) {
      }
      else if (list == null) {
        list = new ArrayList<>();
        list.add(value);
        
        map.put(key, list);
      }
      else {
        list.add(value);
      }
    }
  }

  /**
   * Scans the next character from the input stream, adding it to the
   * converter.
   *
   * @param converter the byte-to-character converter
   * @param is the form's input stream
   * @param ch the next character
   */
  private static void readChar(ByteToChar converter, CharCursor is,
                               int ch, boolean isTop)
    throws IOException
  {
    if (ch == '+') {
      if (isTop)
        converter.addByte(' ');
      else
        converter.addChar(' ');
    }
    else if (ch == '%') {
      int ch1 = is.next();

      if (ch1 == 'u') {
        ch1 = is.next();
        int ch2 = is.next();
        int ch3 = is.next();
        int ch4 = is.next();

        converter.addChar((char) ((toHex(ch1) << 12) +
                                  (toHex(ch2) << 8) + 
                                  (toHex(ch3) << 4) + 
                                  (toHex(ch4))));
      }
      else {
        int ch2 = is.next();
        
        converter.addByte(((toHex(ch1) << 4) + toHex(ch2)));
      }
    }
    else if (isTop) {
      converter.addByte((byte) ch);
    }
    else {
      converter.addChar((char) ch);
    }
  }

  /**
   * Parses the values from a post data
   *
   * @param table the hashtable which will contain the results
   * @param is an input stream containing the data
   * @param javaEncoding the Java name for the charset
   */
  public void parsePostData(HashMapImpl<String,String[]> table,
                     InputStream is,
                     String javaEncoding,
                     int paramMax)
    throws IOException
  {
    ByteToChar converter = _converter;
    
    try {
      converter.setEncoding(javaEncoding);
    } catch (UnsupportedEncodingException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    
    int paramCount = 0;

    int ch = is.read();
    while (ch >= 0) {
      for (; Character.isWhitespace((char) ch) || ch == '&'; ch = is.read()) {
      }

      converter.clear();
      for (;
           ch >= 0 && ch != '=' && ch != '&' &&
             ! Character.isWhitespace((char) ch);
           ch = is.read()) {
        readChar(converter, is, ch);
      }

      String key = converter.getConvertedString();

      for (; Character.isWhitespace((char) ch); ch = is.read()) {
      }
      
      converter.clear(); 
      if (ch == '=') {
        ch = is.read();
        for (; Character.isWhitespace((char) ch); ch = is.read()) {
        }
      }
      
      for (; ch >= 0 && ch != '&'; ch = is.read())
        readChar(converter, is, ch);
      
      String value = converter.getConvertedString();
      
      if (paramMax < paramCount) {
        throw new BadRequestException(L.l("Too many request parameters: '{0}'",
                                          paramCount));
      }
      
      paramCount++;

      /* Could show passwords
      if (log.isLoggable(Level.FINE))
        log.fine("post: " + key + "=" + value);
      */
      
      String []oldValue = table.get(key);
      
      if (key == null || key.equals("")) {
      }
      else if (oldValue == null)
        table.put(key, new String[] { value });
      else {
        String []newValue = new String[oldValue.length + 1];
        System.arraycopy(oldValue, 0, newValue, 0, oldValue.length);
        newValue[oldValue.length] = value;
        table.put(key, newValue);
      }
    }
  }

  /**
   * Scans the next character from the input stream, adding it to the
   * converter.
   *
   * @param converter the byte-to-character converter
   * @param is the form's input stream
   * @param ch the next character
   */
  private static void readChar(ByteToChar converter, InputStream is, int ch)
    throws IOException
  {
    if (ch == '+')
      converter.addByte(' ');
    else if (ch == '%') {
      int ch1 = is.read();

      if (ch1 == 'u') {
        ch1 = is.read();
        int ch2 = is.read();
        int ch3 = is.read();
        int ch4 = is.read();

        converter.addChar((char) ((toHex(ch1) << 12) +
                                  (toHex(ch2) << 8) + 
                                  (toHex(ch3) << 4) + 
                                  (toHex(ch4))));
      }
      else {
        int ch2 = is.read();

        converter.addByte(((toHex(ch1) << 4) + toHex(ch2)));
      }
    }
    else
      converter.addByte(ch);
  }

  /**
   * Converts a hex character to a byte
   */
  private static int toHex(int ch)
  {
    if (ch >= '0' && ch <= '9')
      return ch - '0';
    else if (ch >= 'a' && ch <= 'f')
      return ch - 'a' + 10;
    else if (ch >= 'A' && ch <= 'F')
      return ch - 'A' + 10;
    else
      return -1;
  }
}
