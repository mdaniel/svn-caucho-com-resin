/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.jsp;

import java.util.*;
import java.util.logging.*;

import com.caucho.util.L10N;
import com.caucho.util.CharBuffer;
import com.caucho.util.StringCharCursor;
import com.caucho.util.CharScanner;

import com.caucho.vfs.Depend;
import com.caucho.vfs.Path;

import com.caucho.log.Log;

import com.caucho.server.webapp.Application;

import com.caucho.java.LineMap;

import com.caucho.xml.QName;

import com.caucho.jsp.cfg.JspPropertyGroup;

/**
 * Represents the current state of the parser.
 */
public class ParseState {
  private static final L10N L = new L10N(ParseState.class);
  static final Logger log = Log.open(ParseState.class);

  private Application _application;

  private JspPropertyGroup _jspPropertyGroup = new JspPropertyGroup();

  private boolean _isELIgnored = false;
  private boolean _isELIgnoredSet = false;
  private boolean _isScriptingInvalid = false;
  
  private boolean _isVelocityEnabled;

  private boolean _isSession = true;
  private boolean _isOptionalSession = false;
  private boolean _isSessionSet = false;
  
  private boolean _isErrorPage = false;
  private boolean _isErrorPageSet = false;
  
  private boolean _isAutoFlush = true;
  private boolean _isAutoFlushSet = false;
  
  private boolean _isThreadSafe = true;
  private boolean _isThreadSafeSet = false;

  private boolean _isTag = false;
  private boolean _isXml = false;

  private int _buffer = 8192;
  private boolean _isBufferSet = false;

  private String _info;
  private String _errorPage;
  private String _contentType;
  private String _charEncoding;
  private String _pageEncoding;
  private Class _extends;

  private boolean _recycleTags = true;

  private JspResourceManager _resourceManager;

  private JspBuilder _jspBuilder;

  private ArrayList<String> _importList = new ArrayList<String>();

  private String _uriPwd;
  
  private ArrayList<Depend> _depends = new ArrayList<Depend>();
  private LineMap _lineMap;
  
  private Namespace _namespaces;

  /**
   * Create a new parse state instance.
   */
  public ParseState()
  {
  }

  /**
   * Sets the JSP property group.
   */
  public void setJspPropertyGroup(JspPropertyGroup group)
  {
    _jspPropertyGroup = group;
  }

  /**
   * Gets the JSP property group.
   */
  public JspPropertyGroup getJspPropertyGroup()
  {
    return _jspPropertyGroup;
  }

  /**
   * Returns true if JSP EL is ignored.
   */
  public boolean isELIgnored()
  {
    return _isELIgnored;
  }

  /**
   * Set if JSP EL is ignored.
   */
  public void setELIgnored(boolean isELIgnored)
  {
    _isELIgnored = isELIgnored;
    _isELIgnoredSet = true;
  }

  /**
   * Set if JSP EL is ignored.
   */
  public void setELIgnoredDefault(boolean isELIgnored)
  {
    if (! _isELIgnoredSet)
      _isELIgnored = isELIgnored;
  }

  /**
   * Returns true if JSP scripting is invalidn.
   */
  public boolean isScriptingInvalid()
  {
    return _isScriptingInvalid;
  }

  /**
   * Set if JSP scripting is ignored.
   */
  public void setScriptingInvalid(boolean isScriptingInvalid)
  {
    _isScriptingInvalid = isScriptingInvalid;
  }

  /**
   * Set if velocity statements are enabled.
   */
  public void setVelocityEnabled(boolean isVelocity)
  {
    _isVelocityEnabled = isVelocity;
  }

  /**
   * Returns true if Velocity statements are enabled.
   */
  public boolean isVelocityEnabled()
  {
    return _isVelocityEnabled;
  }

  /**
   * Returns true if the session is enabled.
   */
  public boolean isSession()
  {
    return _isSession;
  }

  /**
   * Returns true if the optional session is enabled.
   */
  public boolean isOptionalSession()
  {
    return _isOptionalSession;
  }
  
  /**
   * Set if the session is enabled.
   */
  public boolean setSession(boolean session)
  {
    boolean isSession = _isSession;
    
    _isSession = session;
    _isOptionalSession = session;

    return (session == isSession || ! _isSessionSet);
  }

  /**
   * Mark the thread safe attribute as set.
   */
  public void markSessionSet()
  {
    _isSessionSet = true;
  }

  /**
   * Returns true if the autoFlush is enabled.
   */
  public boolean isAutoFlush()
  {
    return _isAutoFlush;
  }
  
  /**
   * Set if the autoFlush is enabled.
   */
  public boolean setAutoFlush(boolean autoFlush)
  {
    boolean isAutoFlush = _isAutoFlush;
    
    _isAutoFlush = autoFlush;

    return (autoFlush == isAutoFlush || ! _isAutoFlushSet);
  }

  /**
   * Mark the thread safe attribute as set.
   */
  public void markAutoFlushSet()
  {
    _isAutoFlushSet = true;
  }

  /**
   * Returns true if the threadSafe is enabled.
   */
  public boolean isThreadSafe()
  {
    return _isThreadSafe;
  }

  /**
   * Set if the threadSafe is enabled.
   */
  public boolean setThreadSafe(boolean threadSafe)
  {
    boolean isThreadSafe = _isThreadSafe;
    
    _isThreadSafe = threadSafe;

    return (threadSafe == isThreadSafe || ! _isThreadSafeSet);
  }

  /**
   * Mark the thread safe attribute as set.
   */
  public void markThreadSafeSet()
  {
    _isThreadSafeSet = true;
  }

  /**
   * Set if the errorPage is enabled.
   */
  public boolean setErrorPage(boolean errorPage)
  {
    boolean isErrorPage = _isErrorPage;
    
    _isErrorPage = errorPage;

    return (errorPage == isErrorPage || ! _isErrorPageSet);
  }

  /**
   * Returns true if the errorPage is enabled.
   */
  public boolean isErrorPage()
  {
    return _isErrorPage;
  }

  /**
   * Mark the error page attribute as set.
   */
  public void markErrorPage()
  {
    _isErrorPageSet = true;
  }

  /**
   * Returns the buffer size in bytes.
   */
  public int getBuffer()
  {
    return _buffer;
  }
  
  /**
   * Set the buffer size.
   */
  public boolean setBuffer(int buffer)
  {
    int oldBuffer = _buffer;
    
    _buffer = buffer;

    return (buffer == oldBuffer || ! _isBufferSet);
  }

  /**
   * Mark the buffer attribute as set.
   */
  public void markBufferSet()
  {
    _isBufferSet = true;
  }

  /**
   * Sets the JSP's error page
   */
  public void setErrorPage(String errorPage)
  {
    _errorPage = errorPage;
  }

  /**
   * Gets the JSP's error page
   */
  public String getErrorPage()
  {
    return _errorPage;
  }

  /**
   * Sets the JSP's content type
   */
  public void setContentType(String contentType)
  {
    _contentType = contentType;
  }

  /**
   * Gets the JSP's content type
   */
  public String getContentType()
  {
    return _contentType;
  }

  /**
   * Sets the JSP's character encoding
   */
  public void setCharEncoding(String charEncoding)
  {
    _charEncoding = charEncoding;
  }

  /**
   * Gets the JSP's character encoding
   */
  public String getCharEncoding()
  {
    return _charEncoding;
  }

  /**
   * Sets the JSP's page encoding
   */
  public void setPageEncoding(String pageEncoding)
  {
    _pageEncoding = pageEncoding;
  }

  /**
   * Gets the JSP's character encoding
   */
  public String getPageEncoding()
  {
    return _pageEncoding;
  }

  /**
   * Returns the JSP's info string.
   */
  public String getInfo()
  {
    return _info;
  }

  /**
   * Sets the JSP's info string
   */
  public void setInfo(String info)
  {
    _info = info;
  }

  /**
   * Returns the JSP's extends
   */
  public Class getExtends()
  {
    return _extends;
  }

  /**
   * Sets the JSP's extends
   */
  public void setExtends(Class extendsValue)
  {
    _extends = extendsValue;
  }

  /**
   * Returns true if parsing is a tag
   */
  public boolean isTag()
  {
    return _isTag;
  }

  /**
   * Set if parsing a tag
   */
  public void setTag(boolean isTag)
  {
    _isTag = isTag;
  }

  /**
   * Returns true if parsing is XML
   */
  public boolean isXml()
  {
    return _isXml;
  }

  /**
   * Set if parsing is xml
   */
  public void setXml(boolean isXml)
  {
    _isXml = isXml;
  }

  /**
   * Returns true if the print-null-as-blank is enabled.
   */
  public boolean isPrintNullAsBlank()
  {
    return _jspPropertyGroup.isPrintNullAsBlank();
  }
  
  /**
   * Gets the resource manager.
   */
  public JspResourceManager getResourceManager()
  {
    return _resourceManager;
  }
  
  /**
   * Sets the resource manager.
   */
  public void setResourceManager(JspResourceManager resourceManager)
  {
    _resourceManager = resourceManager;
  }
  
  /**
   * Gets the builder
   */
  public JspBuilder getBuilder()
  {
    return _jspBuilder;
  }
  
  /**
   * Sets the builder
   */
  public void setBuilder(JspBuilder jspBuilder)
  {
    _jspBuilder = jspBuilder;
  }

  private static CharScanner COMMA_DELIM_SCANNER = new CharScanner(" \t\n\r,");
  
  /**
   * Adds an import string.
   */
  public void addImport(String importString)
    throws JspParseException
  {
    StringCharCursor cursor = new StringCharCursor(importString);
    CharBuffer cb = new CharBuffer();
    while (cursor.current() != cursor.DONE) {
      char ch;
      COMMA_DELIM_SCANNER.skip(cursor);

      cb.clear();
      ch = COMMA_DELIM_SCANNER.scan(cursor, cb);

      if (cb.length() != 0) {
        String value = cb.toString();

        if (! _importList.contains(value))
          _importList.add(value);
      }
      else if (ch != cursor.DONE)
        throw new JspParseException(L.l("`{0}' is an illegal page import directive.",
                                        importString));
    }
  }

  /**
   * Returns the import list.
   */
  public ArrayList<String> getImportList()
  {
    return _importList;
  }

  /**
   * Sets the URI pwd
   */
  public void setUriPwd(String uriPwd)
  {
    _uriPwd = uriPwd;
  }

  /**
   * Gets the URI pwd
   */
  public String getUriPwd()
  {
    return _uriPwd;
  }

  /**
   * Returns the line map.
   */
  public LineMap getLineMap()
  {
    return _lineMap;
  }

  /**
   * Add a dependency.
   */
  public void addDepend(Path path)
  {
    Depend depend = new Depend(path);
    if (! _depends.contains(depend))
      _depends.add(depend);
  }

  /**
   * Returns the dependencies
   */
  public ArrayList<Depend> getDependList()
  {
    return _depends;
  }

  /**
   * Resolves a path.
   *
   * @param uri the uri for the path
   *
   * @return the Path
   */
  public Path resolvePath(String uri)
  {
    return getResourceManager().resolvePath(uri);
  }

  /**
   * Set if recycle-tags is enabled.
   */
  public void setRecycleTags(boolean recycleTags)
  {
    _recycleTags = recycleTags;
  }

  /**
   * Returns true if recycle-tags is enabled.
   */
  public boolean isRecycleTags()
  {
    return _recycleTags;
  }

  /**
   * Returns the QName for the given name.
   */
  public QName getQName(String name)
  {
    int p = name.indexOf(':');

    if (p < 0)
      return new QName(name);
    else {
      String prefix = name.substring(0, p);
      String uri = Namespace.find(_namespaces, prefix);

      if (uri != null)
	return new QName(name, uri);
      else
	return new QName(name);
    }
  }

  public Namespace getNamespaces()
  {
    return _namespaces;
  }

  /**
   * Pushes a namespace.
   */
  public void pushNamespace(String prefix, String uri)
  {
    _namespaces = new Namespace(_namespaces, prefix, uri);
  }

  /**
   * Pops a namespace.
   */
  public void popNamespace(String prefix)
  {
    if (_namespaces._prefix.equals(prefix))
      _namespaces = _namespaces.getNext();
    else
      throw new IllegalStateException();
  }
}

