/*
 * Copyright (c) 1998-2005 Caucho Technology -- all rights reserved
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

package com.caucho.widget.impl;

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;
import com.caucho.widget.WidgetException;

import java.util.ArrayList;
import java.util.logging.Logger;

public class WidgetScripts
{
  private static final L10N L = new L10N(WidgetScripts.class);
  private static final Logger log = Logger.getLogger(WidgetScripts.class.getName());

  private ArrayList<WidgetScript> _initScriptList;
  private ArrayList<WidgetScript> _invocationScriptList;
  private ArrayList<WidgetScript> _requestScriptList;
  private ArrayList<WidgetScript> _responseScriptList;
  private ArrayList<WidgetScript> _urlScriptList;
  private ArrayList<WidgetScript> _destroyScriptList;

  private String _language = "js";

  /**
   * The scripting language to use, default is "js".
   *
   * @param language
   */
  public void setLanguage(String language)
  {
    _language = language;
  }

  public void addInit(WidgetScript initScript)
  {
    if (_initScriptList == null)
      _initScriptList = new ArrayList<WidgetScript>();

    _initScriptList.add(initScript);
  }

  public void addInvocation(WidgetScript invocationScript)
  {
    if (_invocationScriptList == null)
      _invocationScriptList = new ArrayList<WidgetScript>();

    _invocationScriptList.add(invocationScript);
  }

  public void addRequest(WidgetScript requestScript)
  {
    if (_requestScriptList == null)
      _requestScriptList = new ArrayList<WidgetScript>();

    _requestScriptList.add(requestScript);
  }

  public void addResponse(WidgetScript responseScript)
  {
    if (_responseScriptList == null)
      _responseScriptList = new ArrayList<WidgetScript>();

    _responseScriptList.add(responseScript);
  }

  public void addURL(WidgetScript urlScript)
  {
    if (_urlScriptList == null)
      _urlScriptList = new ArrayList<WidgetScript>();

    _urlScriptList.add(urlScript);
  }

  public void addDestroy(WidgetScript destroyScript)
  {
    if (_destroyScriptList == null)
      _destroyScriptList = new ArrayList<WidgetScript>();

    _destroyScriptList.add(destroyScript);
  }

  public void init()
    throws ConfigException, WidgetException
  {
    if (_initScriptList != null)
      compileScripts(_initScriptList);

    if (_invocationScriptList != null)
      compileScripts(_invocationScriptList);

    if (_requestScriptList != null)
      compileScripts(_requestScriptList);

    if (_responseScriptList != null)
      compileScripts(_responseScriptList);

    if (_urlScriptList != null)
      compileScripts(_urlScriptList);

    if (_destroyScriptList != null)
      compileScripts(_destroyScriptList);
  }

  private void compileScripts(ArrayList<WidgetScript> scriptList)
    throws WidgetException
  {
    scriptList.trimToSize();

    for (int i = 0; i < scriptList.size(); i++) {
      WidgetScript script = scriptList.get(i);
      script.setLanguage(_language);
      script.compile();
    }
  }

  public ArrayList<WidgetScript> getInitScriptList()
  {
    return _initScriptList;
  }

  public ArrayList<WidgetScript> getInvocationScriptList()
  {
    return _invocationScriptList;
  }

  public ArrayList<WidgetScript> getRequestScriptList()
  {
    return _requestScriptList;
  }

  public ArrayList<WidgetScript> getResponseScriptList()
  {
    return _responseScriptList;
  }

  public ArrayList<WidgetScript> getUrlScriptList()
  {
    return _urlScriptList;
  }

  public ArrayList<WidgetScript> getDestroyScriptList()
  {
    return _destroyScriptList;
  }
}
