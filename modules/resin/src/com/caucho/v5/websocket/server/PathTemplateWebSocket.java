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

package com.caucho.v5.websocket.server;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class PathTemplateWebSocket {
  private String _path;
  private Pattern _pattern;
  private ArrayList<String> _names;
  
  PathTemplateWebSocket(String path)
  {
    _path = path;
    
    parseTemplate(path);
  }
  
  public ArrayList<String> getNames()
  {
    return _names;
  }
  
  public Pattern getPattern()
  {
    return _pattern;
  }
  
  private void parseTemplate(String path)
  {
    int length = path.length();
    
    StringBuilder sb = new StringBuilder();
    
    ArrayList<String> names = new ArrayList<>();
    
    for (int i = 0; i < length; i++) {
      char ch = path.charAt(i);
      
      if (ch == '{') {
        sb.append("(.*)");
        
        StringBuilder varBuilder = new StringBuilder();
        for (i++; i < length && (ch = path.charAt(i)) != '}'; i++) {
          varBuilder.append(ch);
        }
        
        String var = varBuilder.toString();
        
        names.add(var);
      }
      else if (ch == '\\') {
        sb.append("\\\\");
      }
      else if (ch == '?') {
        sb.append("\\?");
      }
      else if (ch == '*') {
        sb.append("\\*");
      }
      else if (ch == '+') {
        sb.append("\\+");
      }
      else if (ch == '.') {
        sb.append("\\.");
      }
      else if (ch == '(') {
        sb.append("\\(");
      }
      else if (ch == ')') {
        sb.append("\\)");
      }
      else {
        sb.append(ch);
      }
    }
    
    _pattern = Pattern.compile(sb.toString());
    _names = names;
  }

  public String[] match(String uri)
  {
    String []paths = new String[_names.size()];
    
    Matcher matcher = _pattern.matcher(uri);
    
    if (! matcher.matches()) {
      return new String[0];
    }
    
    for (int i = 0; i < paths.length && i < matcher.groupCount(); i++) {
      paths[i] = matcher.group(i + 1);
    }
    
     return paths;
  }
}
