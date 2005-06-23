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

package com.caucho.widget;

import java.util.Map;
import java.util.Collection;
import java.util.Set;

public class TestWidgetParameter
  extends WidgetTestCase
{
  public void testBasic()
    throws Exception
  {
    AbstractWidget widget = new AbstractWidget() {
      public void invocation(WidgetInvocation invocation)
        throws WidgetException
      {
        String value = invocation.getParameter();

        addState(value);
      }
    };

    widget.setId("widget");

    Map<String, String[]> parameterMap = new StateMapWrapper(getExternalConnection().getParameterMap());

    getExternalConnection().setParameterMap(parameterMap);

    parameterMap.put("widget", new String[] { "Hello, World" });

    render(widget);

    assertState(
      "GET: widget",
      "Hello, World"
    );
  }

  public void testNamed()
    throws Exception
  {
    AbstractWidget widget = new AbstractWidget() {
      public void invocation(WidgetInvocation invocation)
        throws WidgetException
      {
        String value = invocation.getParameter("message");

        addState(value);
      }
    };

    widget.setId("widget");

    Map<String, String[]> parameterMap = new StateMapWrapper(getExternalConnection().getParameterMap());
    getExternalConnection().setParameterMap(parameterMap);

    parameterMap.put("widget.message", new String[] { "goodbye, World" });

    render(widget);

    assertState(
      "GET: widget.message",
      "goodbye, World"
    );
  }

  public void testIdRequired()
    throws Exception
  {
    // a widget that tries to get an unnamed parameter value must have an id

    AbstractWidget widget = new AbstractWidget() {
      public void invocation(WidgetInvocation invocation)
        throws WidgetException
      {
        invocation.getParameter();
      }
    };

    try {
      render(widget);
    }
    catch (UnsupportedOperationException ex) {
      assertTrue(ex.toString(), ex.getMessage().contains("required"));
      return;
    }

    fail("expecting exception");
  }

  public void testNamedIdRequired()
    throws Exception
  {
    AbstractWidget widget = new AbstractWidget() {
      public void invocation(WidgetInvocation invocation)
        throws WidgetException
      {
        invocation.getParameter("message");
      }
    };

    try {
      render(widget);
    }
    catch (UnsupportedOperationException ex) {
      assertTrue(ex.toString(), ex.getMessage().contains("required"));
      return;
    }

    fail("expecting exception");
  }

  public void testContainment()
    throws Exception
  {
    AbstractWidgetContainer top = new AbstractWidgetContainer() {
      public void invocation(WidgetInvocation invocation)
        throws WidgetException
      {
        super.invocation(invocation);

        addState(getId() + ": (unnamed)=" + invocation.getParameter());
        addState(getId() + ": message=" + invocation.getParameter("message"));
      }
    };

    AbstractWidgetContainer middle = new AbstractWidgetContainer() {
      public void invocation(WidgetInvocation invocation)
        throws WidgetException
      {
        super.invocation(invocation);

        addState(getId() + ": (unnamed)=" + invocation.getParameter());
        addState(getId() + ": message=" + invocation.getParameter("message"));
      }
    };

    AbstractWidgetContainer bottom = new AbstractWidgetContainer() {
      public void invocation(WidgetInvocation invocation)
        throws WidgetException
      {
        super.invocation(invocation);

        addState(getId() + ": (unnamed)=" + invocation.getParameter());
        addState(getId() + ": message=" + invocation.getParameter("message"));
      }
    };

    bottom.setId("bottom");

    middle.setId("middle");
    middle.add(bottom);

    top.setId("top");
    top.add(middle);

    Map<String, String[]> parameterMap = new StateMapWrapper(getExternalConnection().getParameterMap());
    getExternalConnection().setParameterMap(parameterMap);

    parameterMap.put("top", new String[] { "TOP-UNNAMED" });
    parameterMap.put("top.message", new String[] { "TOP-MESSAGE" });
    parameterMap.put("middle.message", new String[] { "TOP>MIDDLE-MESSAGE" });
    parameterMap.put("bottom", new String[] { "TOP>MIDDLE>BOTTOM-UNNAMED" });

    render(top);

    assertState(
      "GET: bottom",
      "bottom: (unnamed)=TOP>MIDDLE>BOTTOM-UNNAMED",
      "GET: bottom.message",
      "bottom: message=null",

      "GET: middle",
      "middle: (unnamed)=null",
      "GET: middle.message",
      "middle: message=TOP>MIDDLE-MESSAGE",

      "GET: top",
      "top: (unnamed)=TOP-UNNAMED",
      "GET: top.message",
      "top: message=TOP-MESSAGE"
    );
  }

  public void testContainmentInterveningIdNotRequired()
    throws Exception
  {
    AbstractWidgetContainer top = new AbstractWidgetContainer() {
      public void invocation(WidgetInvocation invocation)
        throws WidgetException
      {
        super.invocation(invocation);

        addState(getId() + ": (unnamed)=" + invocation.getParameter());
        addState(getId() + ": message=" + invocation.getParameter("message"));
      }
    };

    AbstractWidgetContainer middle = new AbstractWidgetContainer() {
      public void invocation(WidgetInvocation invocation)
        throws WidgetException
      {
        super.invocation(invocation);
      }
    };

    AbstractWidgetContainer bottom = new AbstractWidgetContainer() {
      public void invocation(WidgetInvocation invocation)
        throws WidgetException
      {
        super.invocation(invocation);

        addState(getId() + ": (unnamed)=" + invocation.getParameter());
        addState(getId() + ": message=" + invocation.getParameter("message"));
      }
    };

    bottom.setId("bottom");

    middle.add(bottom);

    top.setId("top");
    top.add(middle);

    Map<String, String[]> parameterMap = new StateMapWrapper(getExternalConnection().getParameterMap());
    getExternalConnection().setParameterMap(parameterMap);

    parameterMap.put("top", new String[] { "TOP-UNNAMED" });
    parameterMap.put("top.message", new String[] { "TOP-MESSAGE" });
    parameterMap.put("bottom", new String[] { "TOP>MIDDLE>BOTTOM-UNNAMED" });
    parameterMap.put("bottom.message", new String[] { "TOP>MIDDLE>BOTTOM-MESSAGE" });

    render(top);

    assertState(
      "GET: bottom",
      "bottom: (unnamed)=TOP>MIDDLE>BOTTOM-UNNAMED",
      "GET: bottom.message",
      "bottom: message=TOP>MIDDLE>BOTTOM-MESSAGE",

      "GET: top",
      "top: (unnamed)=TOP-UNNAMED",
      "GET: top.message",
      "top: message=TOP-MESSAGE"
    );
  }

  public void testNamespace()
    throws Exception
  {
    AbstractWidgetContainer top = new AbstractWidgetContainer() {
      public void invocation(WidgetInvocation invocation)
        throws WidgetException
      {
        super.invocation(invocation);

        addState(getId() + ": (unnamed)=" + invocation.getParameter());
        addState(getId() + ": message=" + invocation.getParameter("message"));
      }
    };

    AbstractWidgetContainer middle = new AbstractWidgetContainer() {
      public void invocation(WidgetInvocation invocation)
        throws WidgetException
      {
        super.invocation(invocation);

        addState(getId() + ": (unnamed)=" + invocation.getParameter());
        addState(getId() + ": message=" + invocation.getParameter("message"));
      }
    };

    AbstractWidgetContainer bottom = new AbstractWidgetContainer() {
      public void invocation(WidgetInvocation invocation)
        throws WidgetException
      {
        super.invocation(invocation);

        addState(getId() + ": (unnamed)=" + invocation.getParameter());
        addState(getId() + ": message=" + invocation.getParameter("message"));
      }
    };

    AbstractWidgetContainer bottom2 = new AbstractWidgetContainer() {
      public void invocation(WidgetInvocation invocation)
        throws WidgetException
      {
        super.invocation(invocation);

        addState(getId() + ": (unnamed)=" + invocation.getParameter());
        addState(getId() + ": message=" + invocation.getParameter("message"));
      }
    };

    bottom2.setId("bottom2");

    bottom.setId("bottom");

    middle.setNamespace(true);
    middle.setId("middle");
    middle.add(bottom);
    middle.add(bottom2);

    top.setId("top");
    top.add(middle);

    Map<String, String[]> parameterMap = new StateMapWrapper(getExternalConnection().getParameterMap());
    getExternalConnection().setParameterMap(parameterMap);

    parameterMap.put("top", new String[] { "TOP-UNNAMED" });
    parameterMap.put("top.message", new String[] { "TOP-MESSAGE" });
    parameterMap.put("middle", new String[] { "TOP>MIDDLE-UNNAMED" });
    parameterMap.put("middle.message", new String[] { "TOP>MIDDLE-MESSAGE" });
    parameterMap.put("middle-bottom", new String[] { "TOP>MIDDLE>BOTTOM-UNNAMED" });
    parameterMap.put("middle-bottom.message", new String[] { "TOP>MIDDLE>BOTTOM-MESSAGE" });
    parameterMap.put("middle-bottom2", new String[] { "TOP>MIDDLE>BOTTOM2-UNNAMED" });
    parameterMap.put("middle-bottom2.message", new String[] { "TOP>MIDDLE>BOTTOM2-MESSAGE" });

    render(top);

    assertState(
      "GET: middle-bottom",
      "bottom: (unnamed)=TOP>MIDDLE>BOTTOM-UNNAMED",
      "GET: middle-bottom.message",
      "bottom: message=TOP>MIDDLE>BOTTOM-MESSAGE",

      "GET: middle-bottom2",
      "bottom2: (unnamed)=TOP>MIDDLE>BOTTOM2-UNNAMED",
      "GET: middle-bottom2.message",
      "bottom2: message=TOP>MIDDLE>BOTTOM2-MESSAGE",

      "GET: middle",
      "middle: (unnamed)=TOP>MIDDLE-UNNAMED",
      "GET: middle.message",
      "middle: message=TOP>MIDDLE-MESSAGE",

      "GET: top",
      "top: (unnamed)=TOP-UNNAMED",
      "GET: top.message",
      "top: message=TOP-MESSAGE"
    );
  }

  private class StateMapWrapper
    implements Map<String, String[]>
  {
    private Map<String, String[]> _map;

    public StateMapWrapper(Map<String, String[]> map)
    {
      _map = map;
    }

    public String[] get(Object key)
    {
      if (key instanceof String) {
        String keyAsString = key.toString();

        if (!keyAsString.contains("--"))
          addState("GET: " + keyAsString);
      }

      return _map.get(key);
    }

    public int size()
    {
      return _map.size();
    }

    public boolean isEmpty()
    {
      return _map.isEmpty();
    }

    public boolean containsKey(Object key)
    {
      return _map.containsKey(key);
    }

    public boolean containsValue(Object value)
    {
      return _map.containsValue(value);
    }

    public String[] put(String key, String[] value)
    {
      return _map.put(key, value);
    }

    public String[] remove(Object key)
    {
      return _map.remove(key);
    }

    public void putAll(Map<? extends String, ? extends String[]> t)
    {
      _map.putAll(t);
    }

    public void clear()
    {
      _map.clear();
    }

    public Set<String> keySet()
    {
      return _map.keySet();
    }

    public Collection<String[]> values()
    {
      return _map.values();
    }

    public Set<Entry<String, String[]>> entrySet()
    {
      return _map.entrySet();
    }

    public boolean equals(Object o)
    {
      return _map.equals(o);
    }

    public int hashCode()
    {
      return _map.hashCode();
    }
  };
}
