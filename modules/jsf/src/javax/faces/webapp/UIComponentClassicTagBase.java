/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
 * @author Scott Ferguson
 */

package javax.faces.webapp;

import java.io.*;

import java.util.*;

import javax.el.*;

import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.context.*;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

public abstract class UIComponentClassicTagBase extends UIComponentTagBase
  implements JspIdConsumer, BodyTag
{
  protected static final String UNIQUE_ID_PREFIX = "_id_";

  private String _id;
  private String _jspId;

  protected PageContext pageContext;
  protected BodyContent bodyContent;

  private FacesContext _facesContext;

  private UIComponentClassicTagBase _parentUIComponentTag;
  
  private Tag _parent;

  private UIComponent _component;
  private boolean _created;

  protected String getFacetName()
  {
    return null;
  }

  public void setJspId(String id)
  {
    _jspId = id;
  }

  public String getJspId()
  {
    return _jspId;
  }

  public void setPageContext(PageContext pageContext)
  {
    this.pageContext = pageContext;
  }

  public Tag getParent()
  {
    return _parent;
  }

  public void setParent(Tag parent)
  {
    _parent = parent;
  }

  public void setBodyContent(BodyContent bodyContent)
  {
    this.bodyContent = bodyContent;
  }

  public BodyContent getBodyContent()
  {
    return this.bodyContent;
  }
  
  public JspWriter getPreviousOut()
  {
    throw new UnsupportedOperationException();
  }

  public void setId(String id)
  {
    if (id.startsWith(UNIQUE_ID_PREFIX))
      throw new IllegalArgumentException("id may not begin with " +
					 UNIQUE_ID_PREFIX);

    _id = id;
  }

  public String getId()
  {
    return _id;
  }

  protected abstract boolean hasBinding();

  public UIComponent getComponentInstance()
  {
    return _component;
  }

  public boolean getCreated()
  {
    return _created;
  }

  public int doStartTag()
    throws JspException
  {
    PageContext pageContext = this.pageContext;

    _facesContext = FacesContext.getCurrentInstance();

    _parentUIComponentTag
      = getParentUIComponentClassicTagBase(pageContext);

    _component = findComponent(_facesContext);

    pageContext.setAttribute("caucho.jsf.parent", this);
    
    return getDoStartValue();
  }

  /**
   * Returns the doStart value for the tag.  Defaults to
   * EVAL_BODY_BUFFERED.
   */
  protected int getDoStartValue()
    throws JspException
  {
    return BodyTag.EVAL_BODY_BUFFERED;
  }

  public void doInitBody()
    throws JspException
  {
  }

  public int doAfterBody()
    throws JspException
  {
    UIComponent verbatim = createVerbatimComponentFromBodyContent();

    if (verbatim != null) {
      UIComponent component = getComponentInstance();

      if (component != null)
	component.getChildren().add(verbatim);
    }

    return getDoAfterBodyValue();
  }

  protected int getDoAfterBodyValue()
    throws JspException
  {
    return BodyTag.SKIP_PAGE;
  }

  public int doEndTag()
    throws JspException
  {
    pageContext.setAttribute("caucho.jsf.parent", _parentUIComponentTag);
    
    return getDoEndValue();
  }

  protected int getDoEndValue()
    throws JspException
  {
    return Tag.EVAL_PAGE;
  }

  protected abstract UIComponent createComponent(FacesContext context,
						 String newId)
    throws JspException;
    
  protected abstract void setProperties(UIComponent component);
  
  protected UIComponent findComponent(FacesContext context)
    throws JspException
  {
    if (_component != null)
      return _component;

    UIComponentClassicTagBase parentTag = _parentUIComponentTag;

    if (parentTag == null) {
      _component = context.getViewRoot();
      return _component;
    }

    UIComponent verbatim = parentTag.createVerbatimComponentFromBodyContent();

    UIComponent parent = parentTag.getComponentInstance();

    // XXX: facet

    String id = getId();

    if (id == null)
      id = UIViewRoot.UNIQUE_ID_PREFIX + getJspId();

    _component = parent.findComponent(id);

    if (_component != null) {
      if (verbatim != null)
	addVerbatimBeforeComponent(parentTag, verbatim, _component);
      
      return _component;
    }

    if (verbatim != null) {
      parent.getChildren().add(verbatim);
    }

    String componentType = getComponentType();

    // XXX binding

    _component = context.getApplication().createComponent(componentType);

    _component.setId(id);

    setProperties(_component);

    parent.getChildren().add(_component);

    return _component;
  }

  protected int getIndexOfNextChildTag()
  {
    throw new UnsupportedOperationException();
  }

  protected void addChild(UIComponent child)
  {
    getComponentInstance().getChildren().add(child);
  }

  protected void addFacet(String name)
  {
    throw new UnsupportedOperationException();
  }

  protected UIComponent createVerbatimComponentFromBodyContent()
  {
    BodyContent bodyContent = this.bodyContent;

    if (bodyContent == null)
      return null;

    String text = bodyContent.getString();
    bodyContent.clearBody();
    boolean isWhitespace = true;

    for (int i = text.length() - 1; i >= 0; i--) {
      char ch = text.charAt(i);

      if (! Character.isWhitespace(ch)) {
	isWhitespace = false;
	break;
      }
    }

    if (isWhitespace)
      return null;

    UIOutput verbatim = createVerbatimComponent();

    verbatim.setValue(text);

    return verbatim;
  }

  protected UIOutput createVerbatimComponent()
  {
    Application app = _facesContext.getApplication();
    UIOutput output
      = (UIOutput) app.createComponent(HtmlOutputText.COMPONENT_TYPE);

    output.setId(_facesContext.getViewRoot().createUniqueId());
    output.setTransient(true);
    if (output instanceof HtmlOutputText)
      ((HtmlOutputText) output).setEscape(false);
    output.setRendered(true);

    return output;
  }

  protected void addVerbatimBeforeComponent(UIComponentClassicTagBase parent,
					    UIComponent verbatim,
					    UIComponent component)
  {
    throw new UnsupportedOperationException();
  }

  protected void addVerbatimAfterComponent(UIComponentClassicTagBase parent,
					    UIComponent verbatim,
					    UIComponent component)
  {
    throw new UnsupportedOperationException();
  }

  public List<String> getCreatedComponents()
  {
    throw new UnsupportedOperationException();
  }

  protected FacesContext getFacesContext()
  {
    return _facesContext;
  }

  @Deprecated
  protected void setupResponseWriter()
  {
  }

  @Deprecated
  protected void encodeBegin()
    throws IOException
  {
    UIComponent component = getComponentInstance();
    FacesContext context = getFacesContext();

    if (component != null && context != null)
      component.encodeBegin(context);
  }

  @Deprecated
  protected void encodeChildren()
    throws IOException
  {
    UIComponent component = getComponentInstance();
    FacesContext context = getFacesContext();

    if (component != null && context != null)
      component.encodeChildren(context);
  }

  @Deprecated
  protected void encodeEnd()
    throws IOException
  {
    UIComponent component = getComponentInstance();
    FacesContext context = getFacesContext();

    if (component != null && context != null)
      component.encodeEnd(context);
  }

  public void release()
  {
  }
  
  public static UIComponentClassicTagBase
    getParentUIComponentClassicTagBase(PageContext pageContext)
  {
    return (UIComponentClassicTagBase)
      pageContext.getAttribute("caucho.jsf.parent");
  }
}
