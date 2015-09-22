/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
 * @author Sam
 */

package com.caucho.quercus;

/**
 * Records the source file location of a statement or expression.
 */
public class Location {
  public static final Location UNKNOWN = new Location();

  private final String _fileName;
  private final String _userPath;

  private final int _lineNumber;
  private final String _className;
  private final String _functionName;

  public Location(String fileName,
                  int lineNumber, String className,
                  String functionName)
  {
    _fileName = fileName;
    _userPath = fileName;

    _lineNumber = lineNumber;
    _className = className;
    _functionName = functionName;
  }

  public Location(String fileName, String userPath,
                  int lineNumber, String className,
                  String functionName)
  {
    _fileName = fileName;
    _userPath = userPath;

    _lineNumber = lineNumber;
    _className = className;
    _functionName = functionName;
  }

  private Location()
  {
    _fileName = null;
    _userPath = null;

    _lineNumber = 0;
    _className = null;
    _functionName = null;
  }

  public String getFileName()
  {
    return _fileName;
  }

  public String getUserPath()
  {
    return _userPath;
  }

  public int getLineNumber()
  {
    return _lineNumber;
  }

  public String getClassName()
  {
    return _className;
  }

  public String getFunctionName()
  {
    return _functionName;
  }

  /**
   * Returns a prefix of the form "filename:linenumber: ", or the empty string
   * if the filename is not known.
   */
  public String getMessagePrefix()
  {
    if (_fileName == null)
      return "";
    else
      return _fileName + ":" + _lineNumber + ": ";
  }

  public boolean isUnknown()
  {
    return _fileName == null || _lineNumber <= 0;
  }

  public String toString()
  {
    return "Location[" + _fileName + ":" + _lineNumber + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((_className == null) ? 0 : _className.hashCode());
    result = prime * result + ((_fileName == null) ? 0 : _fileName.hashCode());
    result = prime * result
        + ((_functionName == null) ? 0 : _functionName.hashCode());
    result = prime * result + _lineNumber;
    result = prime * result + ((_userPath == null) ? 0 : _userPath.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Location other = (Location) obj;
    if (_className == null) {
      if (other._className != null)
        return false;
    } else if (!_className.equals(other._className))
      return false;
    if (_fileName == null) {
      if (other._fileName != null)
        return false;
    } else if (!_fileName.equals(other._fileName))
      return false;
    if (_functionName == null) {
      if (other._functionName != null)
        return false;
    } else if (!_functionName.equals(other._functionName))
      return false;
    if (_lineNumber != other._lineNumber)
      return false;
    if (_userPath == null) {
      if (other._userPath != null)
        return false;
    } else if (!_userPath.equals(other._userPath))
      return false;
    return true;
  }
  
  
}
