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
 * @author Scott Ferguson
 */

package com.caucho.jstl.rt;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.dom.*;

import org.w3c.dom.*;

import com.caucho.log.Log;
import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.jsp.PageContextImpl;
import com.caucho.el.*;
import com.caucho.xsl.*;
import com.caucho.jstl.NameValueTag;
import com.caucho.server.webapp.Application;

public class XmlTransformTag extends BodyTagSupport implements NameValueTag {
  private static final Logger log = Log.open(XmlTransformTag.class);
  private static final L10N L = new L10N(XmlTransformTag.class);
  
  private Object _xml;
  private Object _xslt;

  private String _xmlSystemId;
  private String _xsltSystemId;
  
  private String _var;
  private String _scope;

  private Object _result;
  
  private ArrayList<String> _paramNames = new ArrayList<String>();
  private ArrayList<String> _paramValues = new ArrayList<String>();
  
  /**
   * Sets the JSP-EL XML value.
   */
  public void setDoc(Object xml)
  {
    _xml = xml;
  }
  
  /**
   * Sets the JSP-EL XML value.
   */
  public void setXml(Object xml)
  {
    setDoc(xml);
  }

  /**
   * Sets the JSP-EL XML value.
   */
  public void setXslt(Object xslt)
  {
    _xslt = xslt;
  }

  /**
   * Sets the JSP-EL XML system id expr.
   */
  public void setXmlSystemId(String xmlSystemId)
  {
    _xmlSystemId = xmlSystemId;
  }

  /**
   * Sets the JSP-EL XML system id expr.
   */
  public void setDocSystemId(String docSystemId)
  {
    _xmlSystemId = docSystemId;
  }

  /**
   * Sets the JSP-EL XSLT system id.
   */
  public void setXsltSystemId(String xsltSystemId)
  {
    _xsltSystemId = xsltSystemId;
  }

  /**
   * Sets the variable
   */
  public void setVar(String var)
  {
    _var = var;
  }

  /**
   * Sets the scope
   */
  public void setScope(String scope)
  {
    _scope = scope;
  }

  /**
   * Sets the result
   */
  public void setResult(Object result)
  {
    _result = result;
  }
  
  /**
   * Adds a parameter.
   */
  public void addParam(String name, String value)
  {
    _paramNames.add(name);
    _paramValues.add(value);
  }

  /**
   * Process the tag.
   */
  public int doStartTag()
    throws JspException
  {
    _paramNames.clear();
    _paramValues.clear();

    return super.doStartTag();
  }

  /**
   * Process the tag.
   */
  public int doEndTag()
    throws JspException
  {
    try {
      PageContextImpl pageContext = (PageContextImpl) this.pageContext;
      
      JspWriter out = pageContext.getOut();

      TransformerFactory factory = TransformerFactory.newInstance();

      String xsltSystemId = getCanonicalURL(pageContext, _xsltSystemId);
      
      Source source = getSource(_xslt, xsltSystemId);

      Transformer transformer = factory.newTransformer(source);

      for (int i = 0; i < _paramNames.size(); i++) {
        String name = _paramNames.get(i);
        String value = _paramValues.get(i);

        transformer.setParameter(name, value);
      }

      if (_xml != null)
	source = getSource(_xml, getCanonicalURL(pageContext, _xmlSystemId));
      else {
	BodyContent bodyContent = getBodyContent();

	source = new StreamSource(bodyContent.getReader());

	if (_xmlSystemId != null)
	  source.setSystemId(getCanonicalURL(pageContext, _xmlSystemId));
	else
	  source.setSystemId(((HttpServletRequest) pageContext.getRequest()).getRequestURI());
      }

      Result result;
      Node top = null;

      if (_result != null) {
        result = (Result) _result;
      }
      else if (_var != null) {
        top = new com.caucho.xml.QDocument();
        
        result = new DOMResult(top);
      }
      else
        result = new StreamResult(out);

      transformer.transform(source, result);

      if (_var != null)
        CoreSetTag.setValue(pageContext, _var, _scope, top);
    } catch (Exception e) {
      throw new JspException(e);
    }

    return SKIP_BODY;
  }

  /**
   * Returns the ML source.
   */
  private Source getSource(Object xml, String systemId)
    throws JspException
  {
    PageContextImpl pageContext = (PageContextImpl) this.pageContext;
      
    Object xmlObj = xml;
    Source source = null;

    if (xmlObj instanceof String) {
      ReadStream is = Vfs.openString((String) xmlObj);

      source = new StreamSource(is, systemId);
    }
    else if (xmlObj instanceof InputStream) {
      source = new StreamSource((InputStream) xmlObj, systemId);
    }
    else if (xmlObj instanceof Reader) {
      source = new StreamSource((Reader) xmlObj, systemId);
    }
    else if (xmlObj instanceof Node) {
      source = new DOMSource((Node) xmlObj, systemId);
    }
    else if (xmlObj instanceof NodeList) {
      source = new DOMSource(((NodeList) xmlObj).item(0), systemId);
    }
    else if (xmlObj instanceof Source)
      source = (Source) xmlObj;
    else
      throw new JspException(L.l("unknown xml object type `{0}'", xmlObj));

    return source;
  }

  private String getCanonicalURL(PageContextImpl pageContext, String url)
  {
    Application app = pageContext.getApplication();
    Path appDir = pageContext.getApplication().getAppDir();
      
    if (url != null) {
      if (url.startsWith("/"))
	url = app.getRealPath(url);
      else if (url.indexOf(':') > 0 &&
	       url.indexOf(':') < url.indexOf('/')) {
      }
      else {
	url = pageContext.getRequest().getRealPath(url);
      }
	
      url = appDir.lookup(url).getURL();
    }

    return url;
  }
}
