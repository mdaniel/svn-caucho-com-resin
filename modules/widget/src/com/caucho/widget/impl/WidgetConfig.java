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
import com.caucho.config.NodeBuilder;
import com.caucho.util.L10N;
import com.caucho.widget.*;

import java.io.IOException;
import java.util.Map;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.w3c.dom.Node;

public class WidgetConfig
  extends AbstractWidget
{
  private static final L10N L = new L10N(WidgetConfig.class);
  private static final Logger log = Logger.getLogger(WidgetConfig.class.getName());

  private static final String STANDARD_NAMESPACE = "http://caucho.com/ns/widget";

  private final WidgetScripts _widgetScripts = new WidgetScripts();

  private WidgetConfig _parent;
  private String _namespaceURI;
  private String _localName;

  private Class _type;

  private LinkedList<WidgetConfig> _childWidgetList;
  private LinkedList<Node> _initList;

  private WidgetFactory _widgetFactory;

  private Widget _widget;

  private Map<String, Object> _varMap;
  private WidgetScriptContext _scriptContext;

  private WidgetConfig getParent()
  {
    return _parent;
  }

  public void setConfigNode(Node node)
  {
    _namespaceURI = node.getNamespaceURI();
    _localName = node.getLocalName();

    if (_namespaceURI == null)
      _namespaceURI = STANDARD_NAMESPACE;
  }

  public void setParent(WidgetConfig parent)
  {
    _parent = parent;
  }

  public void setVarMap(Map<String, Object> varMap)
  {
    _varMap = varMap;
  }

  public Map<String, Object> getVarMap()
  {
    return _varMap;
  }

  public void setType(Class type)
    throws ConfigException
  {
    if (type != null) {
      if (!Widget.class.isAssignableFrom(type))
        throw new ConfigException(L.l("type `{0}' must implement interface `{1}'", _type.getName(), Widget.class.getName()));
    }

    _type = type;
  }

  public void addWidget(WidgetConfig widget)
  {
    if (_childWidgetList == null)
      _childWidgetList = new LinkedList<WidgetConfig>();

    _childWidgetList.add(widget);
  }

  public void set(String name, Node node)
    throws ConfigException
  {
    if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
      addInit(node);
    }
    else {
      String namespaceURI = node.getNamespaceURI();
      String localName = node.getLocalName();
      Class<? extends Widget> cl = getWidgetClass(namespaceURI, localName);

      if (cl == null) {
        if (isNamespaceURI(namespaceURI))
          addInit(node);
        else if (node.getNodeType() == Node.TEXT_NODE) {
          addInit(node);
        }
        else
          throw new ConfigException(L.l("`{0}' in namespaceURI `{1}' is unknown", localName, namespaceURI));
      }
      else {
        WidgetConfig widgetConfig = new WidgetConfig();
        widgetConfig.setType(cl);
        widgetConfig.setParent(this);

        NodeBuilder.getCurrentBuilder().configure(widgetConfig, node);

        assert widgetConfig.getParent() == this;

        addWidget(widgetConfig);
      }
    }
  }

  private boolean isNamespaceURI(String namespaceURI)
  {
    if (namespaceURI == null)
      namespaceURI = STANDARD_NAMESPACE;

    return _namespaceURI.equals(namespaceURI);
  }

  private void addInit(Node node)
  {
    if (_initList == null)
      _initList = new LinkedList<Node>();

    _initList.add(node);
  }

  private Class<? extends Widget> getWidgetClass(String namespaceURI,
                                                 String localName)
  {
    if (_parent != null)
      return _parent.getWidgetClass(namespaceURI, localName);
    else {
      return getWidgetFactory().getWidgetClass(namespaceURI, localName);
    }
  }

  private WidgetFactory getWidgetFactory()
  {
    if (_widgetFactory == null) {
      if (_parent != null)
        _widgetFactory = _parent.getWidgetFactory();
      else
        _widgetFactory = new WidgetFactory();
    }

    return _widgetFactory;
  }

  public WidgetScripts createScripts()
  {
    return _widgetScripts;
  }

  public void init()
    throws ConfigException, WidgetException
  {
    if (_type != null) {
      try {
        Object widget = _type.newInstance();

        _widget = (Widget) widget;
      }
      catch (Exception e) {
        throw new ConfigException(L.l("error instantiating `{0}': {1}", _type.getName(), e.toString()));
      }
    }
    else
      _widget = new WidgetImpl();

    if (_initList != null) {
      for (Node node : _initList) {
        NodeBuilder.getCurrentBuilder().configureAttribute(_widget, node);
      }

      _initList = null;
    }

    if (_childWidgetList != null) {
      for (Widget widget : _childWidgetList) {
        _widget.add(widget);
      }

      _childWidgetList = null;
    }
  }

  private WidgetScriptContext createWidgetScriptContext()
  {
    WidgetScriptContext scriptContext = new WidgetScriptContext();

    scriptContext.setWidget(_widget);
    scriptContext.setWidgetScripts(_widgetScripts);
    scriptContext.putVar(_varMap);

    scriptContext.init();

    return scriptContext;
  }

  private void destroyWidgetScriptContext(WidgetScriptContext scriptContext)
  {
    scriptContext.destroy();
  }

  public void init(WidgetInit init)
    throws WidgetException
  {
    super.init(init);

    _scriptContext = createWidgetScriptContext();

    WidgetInitChain initChain = init.getInitChain();
    initChain.init(_widget, _scriptContext);
  }

  public void invocation(WidgetInvocation invocation)
    throws WidgetException
  {
    super.invocation(invocation);

    WidgetInvocationChain invocationChain = invocation.getInvocationChain();

    invocationChain.invocation(_widget, _scriptContext);
  }

  public void request(WidgetRequest request)
    throws WidgetException
  {
    super.request(request);

    WidgetRequestChain requestChain = request.getRequestChain();

    requestChain.request(_widget, _scriptContext);
  }

  public void response(WidgetResponse response)
    throws WidgetException, IOException
  {
    WidgetResponseChain responseChain = response.getResponseChain();

    responseChain.response(_widget, _scriptContext);
  }

  public void url(WidgetURL url)
    throws WidgetException
  {
    super.url(url);

    WidgetURLChain urlChain = url.getURLChain();

    urlChain.url(_widget, _scriptContext);
  }

  public void destroy(WidgetDestroy destroy)
  {
    super.destroy(destroy);

    WidgetDestroyChain destroyChain = destroy.getDestroyChain();

    destroyChain.destroy(_widget, _scriptContext);

    destroyWidgetScriptContext(_scriptContext);
  }

  public class WidgetImpl
    extends AbstractWidgetContainer
  {
    public void addText(String text)
      throws ConfigException
    {
      int len = text.length();

      boolean isShortened = false;

      String shortText = text;

      if (len > 24)
        shortText = text.substring(0, 24) + "...";

      throw new ConfigException(L.l("text `{0}' is not allowed", shortText));

    }
  }
}
