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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jstl.rt;

import com.caucho.jsp.BodyContentImpl;
import com.caucho.jsp.PageContextImpl;
import com.caucho.util.L10N;
import com.caucho.vfs.*;
import com.caucho.xml.Xml;
import com.caucho.xml.XmlParser;

import org.w3c.dom.Document;
import org.xml.sax.*;

import javax.xml.parsers.*;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.io.Reader;

public class XmlParseTag extends BodyTagSupport {
  private static L10N L = new L10N(XmlParseTag.class);

  private Object _xml;
  private String _systemId;
  private Object _filter;
  private XMLReader _saxReader;
  
  private String _var;
  private String _scope;
  
  private String _varDom;
  private String _scopeDom;

  /**
   * Sets the xml
   */
  public void setXml(Object xml)
  {
    _xml = xml;
  }

  /**
   * Sets the xml
   */
  public void setDoc(Object xml)
  {
    setXml(xml);
  }

  /**
   * Sets the system id
   */
  public void setSystemId(String systemId)
  {
    _systemId = systemId;
  }

  /**
   * Sets the filter
   */
  public void setFilter(Object filter)
  {
    _filter = filter;
  }

  /**
   * Sets the sax reader
   */
  public void setSaxReader(XMLReader reader)
  {
    _saxReader = reader;
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
   * Sets the variable
   */
  public void setVarDom(String var)
  {
    _varDom = var;
  }

  /**
   * Sets the scope
   */
  public void setScopeDom(String scope)
  {
    _scopeDom = scope;
  }

  public int doEndTag() throws JspException
  {
    try {
      PageContextImpl pageContext = (PageContextImpl) this.pageContext;
      BodyContentImpl body = (BodyContentImpl) getBodyContent();

      Reader reader;

      if (_xml != null) {
        Object obj = _xml;

        if (obj instanceof Reader)
          reader = (Reader) obj;
        else if (obj instanceof String)
          reader = Vfs.openString((String) obj).getReader();
        else
          throw new JspException(L.l("xml must be Reader or String at `{0}'",
                                     obj));
      }
      else if (body != null) {
        TempCharReader tempReader = (TempCharReader) body.getReader();
        int ch;

        while (Character.isWhitespace((ch = tempReader.read()))) {
        }

        if (ch >= 0)
          tempReader.unread();
        
        reader = tempReader;
      }
      else
	throw new JspException(L.l("doc attribute must be a Reader or String at `{0}'",
				   null));

      InputSource is = new InputSource(reader);

      if (_systemId != null)
        is.setSystemId(_systemId);

      Document doc = null;

      XMLFilter filter = (XMLFilter) _filter;

      if (_filter != null && _var == null && _varDom == null) {
	SAXParserFactory factory = SAXParserFactory.newInstance();
	SAXParser saxParser = factory.newSAXParser();
	XMLReader parser = saxParser.getXMLReader();

	filter.setParent(parser);
	
	filter.parse(is);
	
	reader.close();
      }
      else {
	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	DocumentBuilder parser = factory.newDocumentBuilder();

	doc = parser.parse(is);

	reader.close();

	if (_var != null)
	  CoreSetTag.setValue(pageContext, _var, _scope, doc);
	else if (_varDom != null)
	  CoreSetTag.setValue(pageContext, _varDom, _scopeDom, doc);
	else
	  throw new JspException(L.l("x:parse needs either var or varDom"));
      }
    } catch (JspException e) {
      throw e;
    } catch (Exception e) {
      throw new JspException(e);
    }

    return EVAL_PAGE;
  }
}
