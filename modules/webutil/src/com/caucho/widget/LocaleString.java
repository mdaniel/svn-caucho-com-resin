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

import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

/**
 * A locale String maps locales to String
 */
public class LocaleString
{
  private static L10N L = new L10N( LocaleString.class );

  static protected final Logger log = 
    Logger.getLogger( LocaleString.class.getName() );

  private static ThreadLocal _threadLocale = new ThreadLocal<Locale>();

  private Locale _locale;

  private String _text = null; // the text for _locale

  private String _anyText = null; // the first item in _textMap

  // any locale-->text mappings other than _locale --> _text
  private HashMap<Locale,String> _textMap;

  /**
   * Set the default locale to use for this thread when using
   * the getValue() method.
   */
  public static void setThreadLocale( Locale locale )
  {
    _threadLocale.set( locale );
  }

  public static LocaleString add( LocaleString localeString, 
                                  LocaleString value )
  {
    if ( localeString == null )
      localeString = new LocaleString();

    localeString.add( value );

    return localeString;
  }

  public static LocaleString add( LocaleString localeString, 
                                  String value )
  {
    if ( localeString == null )
      localeString = new LocaleString();

    localeString.addText( value );

    return localeString;
  }

  public static LocaleString add( LocaleString localeString, 
                                  Locale locale, String value )
  {
    if ( localeString == null )
      localeString = new LocaleString();

    localeString.put( locale, value );

    return localeString;
  }

  public static String get( LocaleString localeString, Locale locale )
  {
    return localeString == null ? null : localeString.get( locale );
  }

  public static String get( LocaleString localeString )
  {
    return localeString == null ? null : localeString.getValue();
  }

  public LocaleString()
  {
    setLocale( null );
  }

  /**
   * Construct and set the default locale.
   */
  public LocaleString( Locale locale )
  {
    setLocale( locale );
  }

  /**
   * Construct and set the text for the default locale.
   */
  public LocaleString( String text )
  {
    setLocale( null );
    addText( text );
  }

  /**
   * Construct, set the default locale, and set the text for the default locale.
   */
  public LocaleString( Locale locale, String text )
  {
    setLocale( locale );
    addText( text );
  }

  /**
   * Set the default locale for text set with addText, the default is
   * the platform default.
   */
  public void setLocale( Locale locale )
  {
    if ( locale == null ) {
      String lang = System.getProperty( "user.language" );
      String country = System.getProperty( "user.region" );

      if ( lang != null && country != null )
        locale = new Locale( lang, country );
      else if ( lang != null )
        locale = new Locale( lang );
      else
        locale = Locale.ENGLISH;
    }

    _locale = locale;
  }

  public Locale getLocale()
  {
    return _locale;
  }

  /**
   * Convert the xml style <i>lang</i> to a {@link java.util.Locale}
   * and then call {@link setLocale(Locale)}.
   */
  public void setLang( String lang )
  {
    int index = lang.indexOf('-');

    if  (index > -1 ) {
      String language = lang.substring( 0, index );
      String country = lang.substring( index + 1 );
      setLocale( new Locale( language, country ) );
    }
    else
      setLocale( new Locale( lang ) );
  }

  public void addText( String text )
  {
    _text = text;
  }

  public void put( Locale locale, String text )
  {
    if ( locale == null )
      _text = text;
    else {
      if ( locale.equals( _locale ) )
        _text = text;
      else {
        if ( _textMap == null ) {
          _textMap = new HashMap<Locale,String>();
          _anyText = text;
        }

        _textMap.put( locale, text );
      }
    }
  }

  /**
   * Return the most specific text available.
   * <ul>
   * <li>the text for the thread locale set with setThreadLocale()
   * <li>the text for the thread locale without variant
   * <li>the text for the thread locale wthout variant or country
   * <li>the text for the default locale
   * <li>any text available
   * </ul>
   */
  public String getValue()
  {
    Locale locale = (Locale) _threadLocale.get();

    return get( locale );
  }

  /**
   * Return the most specific text available.
   * <ul>
   * <li>the text that matches the full locale
   * <li>the text that matches the locale without variant
   * <li>the text that matches the locale without variant or country
   * <li>the text for the default locale
   * <li>any text available
   * </ul>
   */
  public String get( Locale locale )
  {
    if ( locale == null ) {
      if ( _text != null )
        return _text;
      else if ( _anyText != null )
        return _anyText;
      else
        return null;
    }

    String text = null;

    if ( _text != null && locale.equals( _locale ) )
      text = _text;
    else if ( _textMap != null )
      text = _textMap.get( locale );

    if ( text != null )
      return text;

    Locale reducedLocale = reduceLocale( locale );

    return get( reducedLocale );
  }

  /** 
   * Add the strings from another locale to this locale.
   * In the case of duplicate locales, the current values are overridden
   * by the new values
   */
  public void add( LocaleString localeString )
  {
    if ( localeString._text != null )
      put( localeString._locale, localeString._text );

    if ( localeString._textMap != null ) {
      for ( Map.Entry<Locale,String> entry : localeString._textMap.entrySet() )
      {
        put( entry.getKey(), entry.getValue() );
      }
    }
  }

  private static Locale reduceLocale( Locale locale )
  {
    String language = locale.getLanguage();
    String country = locale.getCountry();
    String variant = locale.getVariant();

    if ( variant.length() > 0 )
      return new Locale( language, country );
    else if ( country.length() > 0 )
      return new Locale( language );
    else 
      return null;
  }

  public String toString()
  {
    return getValue();
  }
}
