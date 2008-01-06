/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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


package com.caucho.portal.alpharenderer;

import com.caucho.portal.generic.AbstractRenderer;
import com.caucho.util.L10N;

import javax.portlet.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Preferences:
 *
 * <dl>
 * <dt>html.stylesheet
 * <dd>the css stylesheet to use, overrides the value set with setStylesheet()
 * </dl>
 *
 * ResourceBundle lookups:
 *
 * <dl>
 * <dt>portletMode.<i>portletMode</i>.title
 * <dd>The name to display when referring to the portletMode
 * <dt>portletMode.<i>portletMode</i>.shortDescription
 * <dd>A short description of the portlet mode
 * <dt>windowState.<i>windowState</i>.title
 * <dd>The name to display when referring to the windowState
 * <dt>windowState.<i>windowState</i>.shortDescription
 * <dd>A short description of the window state
 * </dl>
 */
public class HtmlRenderer
  extends AbstractRenderer
{
  private static L10N L = new L10N( HtmlRenderer.class );

  static protected final Logger log = 
    Logger.getLogger( HtmlRenderer.class.getName() );

  public final static String PREFERENCE_STYLESHEET = "html.stylesheet";

  private String _pageTitle = "Resin Documentation";
  private boolean _compact = false;
  private String _doctype = "html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transistional.dtd\"";

  private String _stylesheet = "portal.css";

  private Location _titleLocation;
  private Menu _windowStateMenu;
  private Menu _portletModeMenu;

  /** 
   * The title for the page, included in the html output as:
   * <pre>
   * &lt;head&gt;
   *   &lt;title&gt;<i>pageTitle</i>&lt;/title&gt;
   * &lt;/head&gt;
   * </pre>
   */
  public void setPageTitle( String pageTitle )
  {
    _pageTitle = pageTitle;
  }

  public String getPageTitle( )
  {
    return _pageTitle;
  }

  /**
   * Set to true for compact output without newlines, 
   * default false.
   */
  public void setCompact( boolean compact )
  {
    _compact = compact;
  }

  public boolean isCompact()
  {
    return _compact;
  }

  /** 
   * The doctype.
   * Default is <code>html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transistional.dtd</code>
   */
  public void setDoctype( String doctype )
  {
    _doctype = doctype;
  }

  public String getDoctype()
  {
    return _doctype;
  }

  /** 
   * The stylesheet to use, unless overridden with the portlet preference
   * <code>css.stylesheet</code>.  Default is "/portal.css".  .
   */
  public void setStylesheet( String stylesheet )
  {
    _stylesheet = stylesheet;
  }

  public String getStylesheet()
  {
    return _stylesheet;
  }

  /**
   * Location to place the title of the portlet:
   * "hidden", "frame", "header" (default), or "footer".
   */
  public void setTitleLocation( String titleLocation )
  {
    _titleLocation = Location.getLocation( titleLocation );
  }

  public Location getTitleLocation()
  {
    return _titleLocation;
  }

  /**
   * Default is an instance of {@link HtmlMenu}
   */
  public void setWindowStateMenu( Menu windowStateMenu )
  {
    _windowStateMenu = windowStateMenu;
  }

  public Menu getWindowStateMenu()
  {
    return _windowStateMenu;
  }

  /**
   * Default is an instance of {@link HtmlMenu}
   */
  public void setPortletModeMenu( Menu portletModeMenu )
  {
    _portletModeMenu = portletModeMenu;
  }

  public Menu getPortletModeMenu()
  {
    return _portletModeMenu;
  }

  public void init()
  {
    if ( _windowStateMenu == null )
      _windowStateMenu = new HtmlMenu();

    if ( _portletModeMenu == null )
      _portletModeMenu = new HtmlMenu();
  }

  protected void beginPage( PrintWriter out,
                            RenderRequest request,
                            String namespace )
    throws IOException
  {
    PortletPreferences pref = request.getPreferences();
    PortletResponse response = getRenderResponse( request );

    String stylesheet = pref.getValue( PREFERENCE_STYLESHEET, _stylesheet );

    String pageTitle = _pageTitle; // XXX: page title from ?

    if ( _doctype != null && _doctype.length() > 0 ) {
      out.print("<!DOCTYPE ");
      out.print( _doctype );
      out.print( '>' );

      printNewline( out );
    }

    out.print( "<html>" );
    printNewline( out );
    out.print( "<head>" );
    printNewline( out );

    if ( pageTitle != null ) {
      out.print( "<title>" + pageTitle + "</title>" );
      printNewline( out );
    }

    if ( stylesheet != null && stylesheet.length() > 0 ) {
      String cssUrl = response.encodeURL( stylesheet );
      out.print( "<link rel='StyleSheet' href='" );
      out.print( cssUrl );
      out.print( "' type='text/css' media='all'/>" );
      printNewline( out );
    }

    out.print( "</head>" );
    printNewline( out );
    out.print( "<body>" );
    printNewline( out );
  }

  protected void beginWindow( PrintWriter out,
                              RenderRequest request,
                              String namespace )
    throws IOException
  {
    ResourceBundle resourceBundle = getResourceBundle( request );


    out.print( "<div class='portlet-frame ");
    printEscaped( out, request.getPortletMode().toString() );
    out.print( ' ' );
    printEscaped( out, request.getWindowState().toString() );
    out.print( "' id='" );
    out.print( namespace );
    out.print( "'>" );

    if ( _titleLocation == Location.FRAME )
      htmlTitle( out, request, namespace, resourceBundle );

    if ( _portletModeMenu.getLocation() == Location.FRAME )
      htmlPortletModeMenu( out, request, namespace, resourceBundle );

    if ( _windowStateMenu.getLocation() == Location.FRAME )
      htmlWindowStateMenu( out, request, namespace, resourceBundle );

    printNewline( out );

    out.print( "<div class='portlet-header'>" );

    printNewline( out );

    if ( _titleLocation == Location.HEADER )
      htmlTitle( out, request, namespace, resourceBundle );

    if ( _portletModeMenu.getLocation() == Location.HEADER )
      htmlPortletModeMenu( out, request, namespace, resourceBundle );

    if ( _windowStateMenu.getLocation() == Location.HEADER )
      htmlWindowStateMenu( out, request, namespace, resourceBundle );

    out.print( "</div>" ); // class='portlet-topbar'>
    printNewline( out );

    out.print( "<div class='portlet-content'>" );
    printNewline( out );
  }

  protected void endWindow( PrintWriter out,
                            RenderRequest request,
                            String namespace )
    throws IOException
  {
    ResourceBundle resourceBundle = getResourceBundle( request );

    out.print( "</div>" );
    printNewline( out );
    out.print( "<div class='portlet-footer'>" );
    printNewline( out );

    if ( _titleLocation == Location.FOOTER )
      htmlTitle( out, request, namespace, resourceBundle );

    if ( _portletModeMenu.getLocation() == Location.FOOTER )
      htmlPortletModeMenu( out, request, namespace, resourceBundle );

    if ( _windowStateMenu.getLocation() == Location.FOOTER )
      htmlWindowStateMenu( out, request, namespace, resourceBundle );

    out.print( "</div>" ); // footer
    printNewline( out );
    out.print( "</div>" ); // frame
    out.print( "<!-- " );
    out.print( namespace );
    out.print( " -->" );
    printNewline( out );
  }

  protected void endPage( PrintWriter out,
                          RenderRequest request,
                          String namespace )
    throws IOException
  {
    out.print( "</body>" );
    printNewline( out );
    out.print( "</html>" );
    printNewline( out );
  }

  protected void printNewline( PrintWriter out )
  {
    if ( !_compact )
      out.println();
  }

  protected void htmlTitle( PrintWriter out, 
                            RenderRequest request, 
                            String namespace,
                            ResourceBundle resourceBundle )
    throws IOException
  {
    String title = getTitle( request );

    if ( title != null ) {
      out.print( "<h1>" );
      out.print( title );
      out.print( "</h1>" );
    }
  }

  protected void htmlPortletModeMenu( PrintWriter out, 
                                      RenderRequest request, 
                                      String namespace,
                                      ResourceBundle resourceBundle )
    throws IOException
  {
    PortletMode currentPortletMode = request.getPortletMode();
    Set<PortletMode> portletModes = getPortletModes( request );

    Menu.MenuRenderer menuRenderer = _portletModeMenu.createRenderer();

    for ( PortletMode portletMode : portletModes ) {
      String title = portletMode.toString();
      String shortDescription = null;
      String urlString = null;
      boolean isSelected = true;

      if ( ! portletMode.equals( currentPortletMode ) ) {
        isSelected = false;
        PortletURL url = createControlURL( request );

        try {
          url.setPortletMode( portletMode );
        }
        catch ( PortletModeException ex ) {
          throw new RuntimeException( ex );
        }

        urlString = url.toString();
      }

      if ( resourceBundle != null ) {
        StringBuffer key = new StringBuffer();

        key.append( "portletMode." );
        key.append( portletMode.toString() );
        key.append( ".title" );

        try {
          title = resourceBundle.getString( key.toString() );
        }
        catch ( MissingResourceException ex ) {
        }

        key.setLength( 0 );

        key.append( "portletMode." );
        key.append( portletMode.toString() );
        key.append( ".shortDescription" );

        try {
          shortDescription = resourceBundle.getString( key.toString() );
        }
        catch ( MissingResourceException ex ) {
        }
      }

      menuRenderer.add( title, shortDescription, urlString, isSelected );
    }

    menuRenderer.print( out );
  }

  protected void htmlWindowStateMenu( PrintWriter out, 
                                      RenderRequest request, 
                                      String namespace,
                                      ResourceBundle resourceBundle )
    throws IOException
  {
    WindowState currentWindowState = request.getWindowState();
    Set<WindowState> windowStates = getWindowStates( request );

    Menu.MenuRenderer menuRenderer = _windowStateMenu.createRenderer();

    for ( WindowState windowState : windowStates ) {
      String title = windowState.toString();
      String shortDescription = null;
      String urlString = null;
      boolean isSelected = true;

      if ( ! windowState.equals( currentWindowState ) ) {
        isSelected = false;
        PortletURL url = createControlURL( request );

        try {
          url.setWindowState( windowState );
        }
        catch ( WindowStateException ex ) {
          throw new RuntimeException( ex );
        }

        urlString = url.toString();
      }

      if ( resourceBundle != null ) {
        StringBuffer key = new StringBuffer();

        key.append( "windowState." );
        key.append( windowState.toString() );
        key.append( ".title" );

        try {
          title = resourceBundle.getString( key.toString() );
        }
        catch ( MissingResourceException ex ) {
        }

        key.setLength( 0 );

        key.append( "windowState." );
        key.append( windowState.toString() );
        key.append( ".shortDescription" );

        try {
          shortDescription = resourceBundle.getString( key.toString() );
        }
        catch ( MissingResourceException ex ) {
        }
      }

      menuRenderer.add( title, shortDescription, urlString, isSelected );
    }

    menuRenderer.print( out );
  }
}
