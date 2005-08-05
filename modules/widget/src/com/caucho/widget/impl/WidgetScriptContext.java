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

import com.caucho.widget.*;
import com.caucho.vfs.XmlWriter;

import javax.script.GenericScriptContext;
import javax.script.Namespace;
import javax.script.ScriptContext;
import javax.script.SimpleNamespace;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WidgetScriptContext
  extends GenericScriptContext
  implements VarContext, WidgetCallback
{
  private Logger log = Logger.getLogger(WidgetScriptContext.class.getName());

  public Widget _widget;
  private WidgetScripts _widgetScripts;

  private Namespace _globalNamespace = new SimpleNamespace();
  private SimpleNamespace _namespace = new SimpleNamespace();

  private VarContext _varContext;

  public WidgetScriptContext()
  {
    setNamespace(_globalNamespace, ScriptContext.GLOBAL_SCOPE);
    setNamespace(_namespace, ScriptContext.ENGINE_SCOPE);
  }

  public void setWidget(Widget widget)
  {
    _widget = widget;
  }

  public void setWidgetScripts(WidgetScripts widgetScripts)
  {
    _widgetScripts = widgetScripts;
  }

  public void putVar(Map<String, Object> varMap)
  {
    if (varMap != null)
      _globalNamespace.putAll(varMap);
  }

  public void init()
  {
    assert _widget != null;
    assert _widgetScripts != null;

    setWriter(null);
    setReader(null);
  }

  public void destroy()
  {
    _varContext = null;

    _namespace.clear();
    _globalNamespace.clear();

    _widget = null;
  }

  private void execScripts(ArrayList<WidgetScript> widgetScriptList)
    throws WidgetException
  {
    int size = widgetScriptList.size();

    for (int i = 0; i < size; i++) {
      WidgetScript script = widgetScriptList.get(i);
      script.exec(this);
    }
  }

  public void initContext(String name, VarContext context)
  {
    _namespace.clear();

    _namespace.put("widget", _widget);
    _namespace.put("varContext", context);
    _namespace.put(name, context);

    _varContext = context;
  }

  public void init(WidgetInit init)
    throws WidgetException
  {
    ArrayList<WidgetScript> widgetScriptList = _widgetScripts.getInitScriptList();

    if (widgetScriptList == null)
      _widget.init(init);
    else {
      initContext("init", init);

      execScripts(widgetScriptList);
    }
  }

  public void invocation(WidgetInvocation invocation)
    throws WidgetException
  {
    ArrayList<WidgetScript> widgetScriptList = _widgetScripts.getInvocationScriptList();

    if (widgetScriptList == null)
      _widget.invocation(invocation);
    else {
      initContext("invocation", invocation);

      execScripts(widgetScriptList);
    }
  }

  public void request(WidgetRequest request)
    throws WidgetException
  {
    ArrayList<WidgetScript> widgetScriptList = _widgetScripts.getRequestScriptList();

    if (widgetScriptList == null)
      _widget.request(request);
    else {
      initContext("request", request);

      try {
        Reader in = request.getReader();
        _namespace.put("in", in);

        setReader(in);

        execScripts(widgetScriptList);
      }
      catch (IOException e) {
        throw new WidgetException(e);
      }
      finally {
        setReader(null);
      }
    }
  }

  public void url(WidgetURL url)
    throws WidgetException
  {
    ArrayList<WidgetScript> widgetScriptList = _widgetScripts.getUrlScriptList();

    if (widgetScriptList == null)
      _widget.url(url);
    else {
      initContext("url", url);

      execScripts(widgetScriptList);
    }
  }

  public void response(WidgetResponse response)
    throws WidgetException, IOException
  {
    ArrayList<WidgetScript> widgetScriptList = _widgetScripts.getResponseScriptList();

    if (widgetScriptList == null)
      _widget.response(response);
    else {
      initContext("response", response);

      try {
        XmlWriter out = response.getWriter();
        _namespace.put("out", out);

        setWriter(out);

        execScripts(widgetScriptList);
      }
      finally {
        setWriter(null);
      }
    }
  }

  public void destroy(WidgetDestroy destroy)
  {
    ArrayList<WidgetScript> widgetScriptList = _widgetScripts.getDestroyScriptList();

    if (widgetScriptList == null)
      _widget.destroy(destroy);
    else {
      initContext("destroy", destroy);

      try {
        execScripts(widgetScriptList);
      }
      catch (Exception ex) {
        log.log(Level.WARNING, ex.toString(), ex);
      }
    }
  }

  public void setVar(Widget widget, VarDefinition varDefinition, Object value)
    throws UnsupportedOperationException
  {
    _varContext.setVar(widget, varDefinition, value);
  }

  public void setVar(Widget widget, String name, Object value)
    throws UnsupportedOperationException
  {
    _varContext.setVar(widget, name, value);
  }

  public <T> T getVar(Widget widget, VarDefinition varDefinition)
  {
    return (T) _varContext.getVar(widget, varDefinition);
  }

  public <T> T getVar(Widget widget, String name)
  {
    return (T) _varContext.getVar(widget, name);
  }

  public <T> T getVar(Widget widget, VarDefinition varDefinition, T deflt)
  {
    return (T) _varContext.getVar(widget, varDefinition, deflt);
  }

  public <T> T getVar(Widget widget, String name, T deflt)
  {
    return (T) _varContext.getVar(widget, name, deflt);
  }

  public void removeVar(Widget widget, String name)
  {
    _varContext.removeVar(widget, name);
  }

  public void removeVar(Widget widget, VarDefinition varDefinition)
  {
    _varContext.removeVar(widget, varDefinition);
  }
}
