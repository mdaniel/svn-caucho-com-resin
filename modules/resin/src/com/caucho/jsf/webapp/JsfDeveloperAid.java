/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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
 * @author Alex Rojkov
 */

package com.caucho.jsf.webapp;

import com.caucho.util.L10N;

import javax.el.ValueExpression;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.component.UIViewRoot;
import javax.faces.component.ValueHolder;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JsfDeveloperAid
  implements PhaseListener
{

  public static final String URL_PATTERN = "caucho.jsf.developer.aid";

  private static final Logger log
    = Logger.getLogger(FacesServletImpl.class.getName());

  private static final L10N L = new L10N(JsfDeveloperAid.class);

  public void afterPhase(PhaseEvent event)
  {
    FacesContext context = event.getFacesContext();

    Map sessionMap = context.getExternalContext().getSessionMap();

    Map<String, Object[]> aidMap
      = (Map<String, Object[]>) sessionMap.get(
      "caucho.jsf.developer.aid");

    if (aidMap == null) {
      aidMap = new HashMap<String, Object[]>();

      sessionMap.put("caucho.jsf.developer.aid", aidMap);
    }

    try {
      UIViewRoot viewRoot = context.getViewRoot();

      if (viewRoot != null) {
        final String viewId = viewRoot.getViewId();
        final String phaseId = event.getPhaseId().toString();

        final Component component = reflect(context, viewRoot);

        Object []phases;

        if (PhaseId.RESTORE_VIEW.equals(event.getPhaseId())) {
          phases = new Object[]{phaseId, component};
        }
        else {
          phases = aidMap.get(viewId);

          Object []newPhases = new Object[phases.length + 2];

          System.arraycopy(phases, 0, newPhases, 0, phases.length);

          newPhases[newPhases.length - 2] = phaseId;
          newPhases[newPhases.length - 1] = component;

          phases = newPhases;
        }

        aidMap.put(viewId, phases);
      }
    }
    catch (IllegalStateException e) {
      log.log(Level.FINER, e.getMessage(), e);
    }
  }

  public void beforePhase(PhaseEvent event)
  {//
  }

  public PhaseId getPhaseId()
  {
    return PhaseId.ANY_PHASE;
  }

  public Component reflect(FacesContext facesContext, UIComponent uiComponent)
  {
    final Component result;

    if (uiComponent instanceof UIViewRoot) {
      UIViewRoot viewRoot = (UIViewRoot) uiComponent;
      result = new ViewRoot(viewRoot.getLocale(), viewRoot.getRenderKitId());
    }
    else
      result = new Component();

    result._uiComponentClass = uiComponent.getClass().getSimpleName();
    result._clientId = uiComponent.getClientId(facesContext);
    result._family = uiComponent.getFamily();

    final int childCount = uiComponent.getChildCount();

    if (childCount > 0) {
      List<UIComponent> children = uiComponent.getChildren();

      result._children = new ArrayList<Component>(children.size());

      for (int i = 0; i < childCount; i++)
        result._children.add(reflect(facesContext, children.get(i)));
    }

    final int facetCount = uiComponent.getFacetCount();

    if (facetCount > 0) {
      Map<String, UIComponent> facets = uiComponent.getFacets();

      result._facets = new HashMap<String, Component>(facets.size());

      Set<String> names = facets.keySet();

      for (String name : names)
        result._facets.put(name, reflect(facesContext, uiComponent));
    }

    if (uiComponent instanceof ValueHolder) {
      result._isValueHolder = true;

      Object value = ((ValueHolder) uiComponent).getValue();
      result._value = (value == null ? null : value.toString());

      Object localValue = ((ValueHolder) uiComponent).getLocalValue();
      result._localValue = (localValue == null ? null : localValue.toString());
    }

    if (uiComponent instanceof EditableValueHolder) {
      result._isEditableValueHolder = true;

      Object submittedValue = ((EditableValueHolder) uiComponent).getValue();

      if (submittedValue instanceof Object[]) {

        StringBuilder sb = new StringBuilder('[');

        Object []values = (Object[]) submittedValue;

        for (int i = 0; i < values.length; i++) {
          Object value = values[i];

          sb.append(String.valueOf(value));

          if ((i + 1) < values.length)
            sb.append(',');
        }

        sb.append(']');

        result._submittedValue = sb.toString();
      }
      else {
        result._submittedValue = (submittedValue == null
                                  ? null
                                  : submittedValue.toString());
      }
    }

    for (Method method : uiComponent.getClass().getMethods()) {
      if (!method.getName().startsWith("get")
          && !method.getName().startsWith("is"))
        continue;
      else if (method.getParameterTypes().length != 0)
        continue;

      String name;

      if (method.getName().startsWith("get"))
        name = method.getName().substring(3);
      else if (method.getName().startsWith("is"))
        name = method.getName().substring(2);
      else
        continue;

      name = Character.toLowerCase(name.charAt(0)) + name.substring(1);

      ValueExpression expr = uiComponent.getValueExpression(name);

      Class type = method.getReturnType();

      if (expr != null) {
        result.setAttribute("expr:" + name, expr.getExpressionString());
      }
      else if (method.getDeclaringClass().equals(UIComponent.class)
               || method.getDeclaringClass().equals(UIComponentBase.class)) {
      }
      else if (name.equals("family") ||
               name.equals("value") ||
               name.equals("localValue") ||
               name.equals("submittedValue")) {
      }
      else if (String.class.equals(type)) {
        try {
          Object value = method.invoke(uiComponent);

          if (value != null)
            result.setAttribute(name, String.valueOf(value));
        }
        catch (Exception e) {
        }
      }
    }

    return result;
  }

  public static class ViewRoot
    extends Component
  {
    private Locale _locale;
    private String _renderKitId;

    public ViewRoot(Locale locale, String renderKitId)
    {
      _locale = locale;
      _renderKitId = renderKitId;
    }

    public Locale getLocale()
    {
      return _locale;
    }

    public String getRenderKitId()
    {
      return _renderKitId;
    }
  }

  public static class Component
    implements Serializable
  {
    private String _uiComponentClass;
    private String _clientId;
    private String _family;
    private String _value;
    private String _localValue;
    private String _submittedValue;
    private boolean _isValueHolder;
    private boolean _isEditableValueHolder;

    private List<Component> _children;
    private Map<String, Component> _facets;
    private Map<String, String> _attributes;


    public List<Component> getChildren()
    {
      return _children;
    }

    public Map<String, Component> getFacets()
    {
      return _facets;
    }

    public String getUiComponentClass()
    {
      return _uiComponentClass;
    }

    public String getClientId()
    {
      return _clientId;
    }

    public String getFamily()
    {
      return _family;
    }

    public String getValue()
    {
      return _value;
    }

    public String getLocalValue()
    {
      return _localValue;
    }

    public String getSubmittedValue()
    {
      return _submittedValue;
    }

    public boolean isValueHolder()
    {
      return _isValueHolder;
    }

    public boolean isEditableValueHolder()
    {
      return _isEditableValueHolder;
    }

    public void setAttribute(String name, String value)
    {
      if (_attributes == null)
        _attributes = new HashMap<String, String>();

      _attributes.put(name, value);
    }

    public Map<String, String> getAttributes()
    {
      return _attributes;
    }
  }
}