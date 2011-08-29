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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Sam
 */


package com.caucho.widget;

import com.caucho.util.L10N;

import java.util.ArrayList;
import java.util.logging.Logger;

public class WidgetPreference
{
  private static L10N L = new L10N( WidgetPreference.class );

  static protected final Logger log = 
    Logger.getLogger( WidgetPreference.class.getName() );

  private String _name;
  private boolean _isReadOnly;
  private ArrayList<String> _valuesList;
  private String[] _valuesArray;


  public void setName( String name )
  {
    _name = name;
  }

  public String getName()
  {
    return _name;
  }

  public void setReadOnly( boolean isReadOnly )
  {
    _isReadOnly = isReadOnly;
  }

  public boolean isReadOnly()
  {
    return _isReadOnly;
  }

  public String getValue()
  {
    makeValues();

    if ( _valuesArray == null )
      return null;
    else if ( _valuesArray.length == 0 )
      return "";
    else
      return _valuesArray[0];
  }

  public String[] getValues()
  {
    makeValues();

    return _valuesArray;
  }

  public void addValue( String value )
  {
    if ( _valuesList == null )
      _valuesList = new ArrayList<String>();

    _valuesList.add( value );
  }

  private void makeValues()
  {
    if ( _valuesList != null && _valuesList.size() > 0 ) {

      int arraySize = ( _valuesArray == null ? 0 : _valuesArray.length );
      int listSize = ( _valuesList == null ? 0 : _valuesList.size() );

      String[] newArray = new String[ arraySize + listSize ];

      for ( int i = 0; i < arraySize; i++ )
        newArray[i] = _valuesArray[i];

      int i = arraySize;

      for ( int j = 0; j < listSize; j++ ) {
        newArray[i] = _valuesList.get(j);
        i++;
      }

      _valuesList = null;
      _valuesArray = newArray;
    }
  }
}
