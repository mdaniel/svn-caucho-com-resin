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

import java.io.*;
import java.util.*;
import java.util.logging.*;

import javax.servlet.http.*;
import javax.servlet.*;

import org.w3c.dom.*;

import com.caucho.Version;
import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.java.*;

import com.caucho.log.Log;

import com.caucho.server.webapp.Application;

import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.Environment;

import com.caucho.make.PersistentDependency;

import com.caucho.server.connection.CauchoRequest;
import com.caucho.server.connection.CauchoResponse;
import com.caucho.server.connection.RequestAdapter;

import com.caucho.jsp.cfg.JspPropertyGroup;

/**
 * Parent template manager for both JspManager and XtpManager.  PageManager
 * is responsible for caching pages until the underlying files change.
 */
abstract public class PageManager {
  private final static Logger log = Log.open(PageManager.class);
  
  static final long ACCESS_INTERVAL = 60000L;

  protected Application _application;
  private Path _classDir;
  private long _updateInterval = 1000;
  private boolean _isAdapter;
  private boolean _omitInitLog;
  private int _jspMax = 1024;
  private LruCache<String,Entry> _cache;

  // true if the manager should detect page changes and automatically recompile
  protected boolean _autoCompile = true;

  /**
   * Create a new PageManager
   *
   * @param context the servlet application.
   */
  PageManager(Application application)
  {
    _application = application;

    _classDir = CauchoSystem.getWorkPath();

    long interval = Environment.getDependencyCheckInterval();
  
    JspPropertyGroup jspPropertyGroup = _application.getJsp();

    if (jspPropertyGroup != null) {
      _autoCompile = jspPropertyGroup.isAutoCompile();
      _jspMax = jspPropertyGroup.getJspMax();

      if (jspPropertyGroup.getDependencyCheckInterval() != Long.MIN_VALUE)
	interval = jspPropertyGroup.getDependencyCheckInterval();
    }

    if (interval < 0)
      interval = Integer.MAX_VALUE / 2;
    
    _updateInterval = interval;
  }

  public Path getClassDir()
  {
    if (_classDir != null)
      return _classDir;
    else {
      Path appDir = _application.getAppDir();

      if (appDir instanceof MemoryPath) {
        String workPathName = ("./" + _application.getURL());
        Path path = CauchoSystem.getWorkPath().lookup(workPathName);

        return path;
      }
      else
        return appDir.lookup("WEB-INF/work");
    }
  }

  public Path getAppDir()
  {
    return _application.getAppDir();
  }

  /**
   * Returns the CauchoApplication for the manager.
   */
  Application getApplication()
  {
    return _application;
  }
  
   /**
    * Compiles and returns the page at the given path and uri.  The uri
    * is needed for jsp:include, etc. in the JSP pages.
    *
    * @param path Path to the page.
    * @param uri uri of the page.
    *
    * @return the compiled JSP (or XTP) page.
    */
  public Page getPage(String uri, Path path)
    throws Exception
  {
    return getPage(uri, uri, path, null);
  }
  
   /**
    * Compiles and returns the page at the given path and uri.  The uri
    * is needed for jsp:include, etc. in the JSP pages.
    *
    * @param path Path to the page.
    * @param uri uri of the page.
    *
    * @return the compiled JSP (or XTP) page.
    */
  public Page getPage(String uri, String pageURI, Path path)
    throws Exception
  {
    return getPage(uri, pageURI, path, null);
  }
  
   /**
    * Compiles and returns the page at the given path and uri.  The uri
    * is needed for jsp:include, etc. in the JSP pages.
    *
    * @param uri uri of the page.
    * @param uri uri of the page.
    * @param path Path to the page.
    *
    * @return the compiled JSP (or XTP) page.
    */
  public Page getPage(String uri, String pageURI, Path path,
		      ArrayList<PersistentDependency> dependList)
    throws Exception
  {
    LruCache<String,Entry> cache = _cache;

    if (cache == null) {
      synchronized (this) {
	if (_cache == null)
	  _cache = new LruCache<String,Entry>(_jspMax);
	cache = _cache;
      }
    }

    Entry entry = null;

    synchronized (cache) {
      entry = cache.get(uri);

      if (entry == null) {
	entry = new Entry(uri);
	cache.put(uri, entry);
      }
    }

    synchronized (entry) {
      Page page = entry.getPage();
      
      if (page != null && ! page.cauchoIsModified())
        return page;
      else if (page != null && ! page.isDead()) {
        try {
          page.destroy();
        } catch (Exception e) {
          log.log(Level.FINE, e.toString(), e);
        }
      }

      if (log.isLoggable(Level.FINE)) {
        log.fine("uri:" + uri +
                 "(cp:" + _application.getContextPath() + 
                 ",app:" + _application.getAppDir() +
                 ") -> " + path);
      }
     
      Path appDir = getApplication().getAppDir();

      String rawClassName = pageURI;

      if (path.getPath().startsWith(appDir.getPath()))
	rawClassName = path.getPath().substring(appDir.getPath().length());

      String className = JavaCompiler.mangleName("jsp/" + rawClassName);

      page = createPage(path, pageURI, className, dependList);

      if (page == null)
        throw new FileNotFoundException(pageURI);

      if (_autoCompile == false)
        page._caucho_setNeverModified(true);

      page._caucho_setUpdateInterval(_updateInterval);

      entry.setPage(page);

      return page;
    }
  }
  
  /**
   * Implementation-specific method to create the actual page.  JspManager
   * and XtpManager define this for their specific needs.
   */
  abstract Page createPage(Path path, String uri, String className,
			   ArrayList<PersistentDependency> dependList)
    throws Exception;

  void killPage(HttpServletRequest request,
                HttpServletResponse response,
                Page page)
  {
  }

  /**
   * Clean up the pages when the server shuts down.
   */
  void destroy()
  {
    LruCache<String,Entry> cache = _cache;
    _cache = null;

    if (cache == null)
      return;
    
    synchronized (cache) {
      Iterator<Entry> iter = cache.values();

      while (iter.hasNext()) {
        Entry entry = iter.next();

        Page page = entry != null ? entry.getPage() : null;

        try {
          if (page != null && ! page.isDead()) {
            page.destroy();
	  }
        } catch (Exception e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }
    }
  }
  
  class Entry implements CacheListener {
    private String _key;
    Page _page;
    
    private long _lastAccessTime;
 
    Entry(String key)
    {
      _key = key;
    }

    void setPage(Page page)
    {
      _page = page;
      
      if (page != null)
	page._caucho_setEntry(this);
    }
    
    Page getPage()
    {
      return _page;
    }

    public void accessPage()
    {
      long now = Alarm.getCurrentTime();

      if (now < _lastAccessTime + ACCESS_INTERVAL)
	return;

      _lastAccessTime = now;

      if (_cache != null)
	_cache.get(_key);
    }

    public void removeEvent()
    {
      Page page = _page;
      _page = null;
      
      if (page != null && ! page.isDead()) {
        if (log.isLoggable(Level.FINE))
          log.fine("dropping page " + page);
        
	page.setDead();
        page.destroy();
      }
    }
  }
}
