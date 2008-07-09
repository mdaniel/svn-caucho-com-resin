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

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.FactoryFinder;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextFactory;
import javax.faces.lifecycle.Lifecycle;
import javax.faces.lifecycle.LifecycleFactory;
import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class JsfDeveloperAidServlet
  extends GenericServlet
{

  private static final Logger log
    = Logger.getLogger(FacesServletImpl.class.getName());

  private static final L10N L = new L10N(JsfDeveloperAid.class);

  private ServletContext _webApp;

  private FacesContextFactory _facesContextFactory;
  private Lifecycle _lifecycle;

  public void init(ServletConfig config)
    throws ServletException
  {
    _webApp = config.getServletContext();

    _facesContextFactory = (FacesContextFactory)
      FactoryFinder.getFactory(FactoryFinder.FACES_CONTEXT_FACTORY);

    LifecycleFactory lifecycleFactory = (LifecycleFactory)
      FactoryFinder.getFactory(FactoryFinder.LIFECYCLE_FACTORY);

    String name = config.getInitParameter("javax.faces.LIFECYCLE_ID");

    if (name == null)
      name = _webApp.getInitParameter("javax.faces.LIFECYCLE_ID");

    if (name == null)
      name = LifecycleFactory.DEFAULT_LIFECYCLE;

    _lifecycle = lifecycleFactory.getLifecycle(name);
  }

  public void service(ServletRequest req, ServletResponse res)
    throws IOException, ServletException
  {
    FacesContext oldContext = FacesContext.getCurrentInstance();

    FacesContext context = null;

    try {
      context = _facesContextFactory.getFacesContext(_webApp,
                                                     req,
                                                     res,
                                                     _lifecycle);

      res.setCharacterEncoding("UTF-8");

      PrintWriter out = res.getWriter();

      out.println(
        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");

      out.println(
        "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">");

      HttpServletRequest request = (HttpServletRequest) req;

      HttpSession session = request.getSession();

      if (session == null) {
        out.println("</html>");
        return;
      }

      Map<String, Object[]> aidMap
        = (Map<String, Object[]>)
        session.getAttribute("caucho.jsf.developer.aid");

      if (aidMap == null) {
        out.println("</html>");
        return;
      }

      final String viewId = req.getParameter("viewId");

      if (viewId == null) {
        out.println(" <head>");
        out.print("  <title>");
        out.print("Available Views");
        out.println("</title>");
        out.println(" </head>");
        out.println(" <body>");
        out.println("  <em>Available Views</em>");

        out.println("  <ul>");

        for (String view : aidMap.keySet()) {
          out.println("   <li>");
          out.println("    <a href=\"" +
                      request.getContextPath() +
                      "/caucho.jsf.developer.aid;jsessionid=" +
                      session.getId() +
                      "?viewId=" +
                      view + "\">" +
                      view +
                      "</a>");
          out.println("   </li>");
        }
        out.println(" </ul>");
        out.println(" </body>");
        out.println("</html>");

        return;
      }
      else {
        out.println("<head>");
        out.print("<title>View: ");
        out.print(viewId);
        out.println("</title>");
        out.println("<style type=\"text/css\" media=\"all\">");
        out.println("#header ul {list-style: none;padding: 0;margin: 0;}");
        out.println(
          "#header li {float: left;border: 1px solid;border-bottom-width: 0;margin: 0 0.5em 0 0;\t}");
        out.println("#header a {display: block;padding: 0 1em;}");
        out.println(
          "#header #selected {position: relative;top: 1px;background: white;}");
        out.println("#content {border: 1px solid;clear: both;}");
        out.println("h1 {margin: 0;padding: 0 0 1em 0;}");
        out.println("</style>");
        out.println("</head>");
        //
        out.println("<body>");
        out.println(" <div id=\"header\"");
        out.println("  <ul>");

        final String phaseId = request.getParameter("phaseId");
        String valueExpression = req.getParameter("valueExpression");

        if (valueExpression != null)
          valueExpression = URLDecoder.decode(valueExpression, "UTF-8");

        Object []phases = aidMap.get(viewId);

        boolean selectedMarked = false;

        JsfDeveloperAid.Component component = null;

        for (int i = 0; i < phases.length / 2; i++) {
          String phase = (String) phases[i * 2];

          boolean selected = false;

          if (!selectedMarked && valueExpression == null) {
            selected = phaseId == null || phase.equals(phaseId);
            selectedMarked = selected;
            component = (JsfDeveloperAid.Component) phases[i * 2 + 1];
          }

          out.print("   <li" + (selected ? " id=\"selected\"" : "") + ">");
          out.print("<a href=\"" +
                    request.getContextPath() +
                    "/caucho.jsf.developer.aid;jsessionid=" +
                    session.getId() +
                    "?viewId=" +
                    viewId +
                    "&phaseId=" +
                    phase +
                    "\">" +
                    phase +
                    "</a>");
          out.println("</li>");
        }

        if (valueExpression != null) {
          out.print("   <li id=\"selected\">");
          out.print("<a href=\"" +
                    request.getContextPath() +
                    "/caucho.jsf.developer.aid;jsessionid=" +
                    session.getId() +
                    "?viewId=" + viewId +
                    "&phaseId=" + phaseId +
                    "&valueExpression=" +
                    URLEncoder.encode(valueExpression, "UTF-8") +
                    "\">" +
                    valueExpression +
                    "</a>");
          out.println("</li>");
        }

        out.println("  </ul>");
        out.println(" </div>");
        out.println(" <div id=\"content\">");


        if (valueExpression != null) {
          JsfDeveloperAid.ViewRoot root = (JsfDeveloperAid.ViewRoot) phases[1];

          UIViewRoot uiViewRoot = new UIViewRoot();
          uiViewRoot.setLocale(root.getLocale());
          uiViewRoot.setRenderKitId(root.getRenderKitId());
          //need view for resolving property bundles.
          context.setViewRoot(uiViewRoot);

          printEvaluated(context,
                         request,
                         out,
                         valueExpression,
                         viewId,
                         phaseId);

          out.println("<br/>");

          out.println("<em><a href=\"" +
                      request.getContextPath() +
                      "/caucho.jsf.developer.aid;jsessionid=" +
                      session.getId() +
                      "?viewId=" + viewId +
                      "&phaseId=" + phaseId + "\">" +
                      "<<< Back" +
                      "</a></em>");

        }
        else
          printComponentTree(request, out, component, null, viewId, phaseId, 0);

        out.println(" </div>");
        out.println(" </body>");
        out.println("</html>");
      }

      out.flush();
    }
    catch (IOException e) {
      throw e;
    }
    finally {
      context.release();

      FacesContext.setCurrentInstance(oldContext);
    }
  }

  private void printEvaluated(FacesContext context,
                              HttpServletRequest request,
                              PrintWriter out,
                              String expression,
                              String viewId,
                              String phaseId
  )
    throws UnsupportedEncodingException
  {
    ELContext elContext = context.getELContext();

    ValueExpression valueExpression = context.getApplication()
      .getExpressionFactory()
      .createValueExpression(elContext, expression, Object.class);

    Object obj = valueExpression.getValue(elContext);

    out.print("<strong>");
    out.print(expression + "=");
    out.print("</strong>");

    if (obj == null) {
      out.println("null");
      out.println("<br/>");
    }
    else {

      if (obj instanceof String
          || obj instanceof Boolean
          || obj instanceof Character
          || obj instanceof Number
          || obj instanceof Date
        ) {
        out.println(obj.toString());
      }
      else {
        out.print("<strong>");
        out.print(obj.getClass().toString());
        out.print("[" + obj.toString() + "]");
        out.println("</strong>");

        Field []fields = obj.getClass().getDeclaredFields();

        out.println("<br/>");
        for (Field field : fields) {
          try {
            field.setAccessible(true);

            Object value = field.get(obj);
            out.print("&nbsp;&nbsp;&nbsp;");

            printAttribute(request,
                           out,
                           field.getName(),
                           String.valueOf(value),
                           viewId,
                           phaseId);

            out.println("<br/>");
          }
          catch (IllegalAccessException e) {
          }
        }
      }
    }
  }

  private void printComponentTree(HttpServletRequest request,
                                  PrintWriter out,
                                  JsfDeveloperAid.Component component,
                                  String facetName,
                                  String viewId,
                                  String phaseId,
                                  int depth)
    throws UnsupportedEncodingException
  {
    for (int i = 0; i < depth * 3; i++)
      out.print("&nbsp;");

    out.print("&lt;<strong>" + component.getUiComponentClass() + "</strong>");
    printAttribute(request,
                   out,
                   "clientId",
                   component.getClientId(),
                   viewId,
                   phaseId);

    if (component.isValueHolder()) {
      printAttribute(request,
                     out,
                     "value",
                     component.getValue(),
                     viewId,
                     phaseId);
      printAttribute(request,
                     out,
                     "localValue",
                     component.getLocalValue(),
                     viewId, phaseId);
    }

    if (component.isEditableValueHolder())
      printAttribute(request, out, "submittedValue",
                     component.getSubmittedValue(), viewId, phaseId
      );

    if (facetName != null)
      printAttribute(request,
                     out,
                     "enclosingFacet",
                     facetName,
                     viewId,
                     phaseId);

    Map<String, String> attributes = component.getAttributes();
    if (attributes != null)
      for (String attr : attributes.keySet()) {
        String value = attributes.get(attr);

        if (value != null)
          printAttribute(request, out, attr, value, viewId, phaseId);
      }

    out.println("><br/>");

    List<JsfDeveloperAid.Component> children = component.getChildren();
    Map<String, JsfDeveloperAid.Component> facets = component.getFacets();

    if (children != null)
      for (JsfDeveloperAid.Component child : children)
        printComponentTree(request,
                           out,
                           child,
                           null,
                           viewId,
                           phaseId,
                           depth + 1);

    if (facets != null)
      for (String facet : facets.keySet())
        printComponentTree(request,
                           out,
                           facets.get(facet),
                           facet,
                           viewId,
                           phaseId,
                           depth + 1);

  }

  private void printAttribute(HttpServletRequest request,
                              PrintWriter out,
                              String name,
                              String value,
                              String viewId,
                              String phaseId)
    throws UnsupportedEncodingException
  {
    out.print(' ');
    out.print("<em>" + name);
    out.print("</em>=\"");
    if (value == null)
      out.print("null");
    else if (value.indexOf("#{") > -1 && value.indexOf("}") > -1) {
      out.print("<a href=\"" +
                request.getContextPath() +
                "/caucho.jsf.developer.aid;jsessionid=" +
                request.getSession().getId() +
                "?viewId=" +
                viewId +
                "&phaseId=" +
                phaseId +
                "&valueExpression=" +
                URLEncoder.encode(value, "UTF-8") +
                "\">" +
                value +
                "</a>");
    }
    else {

      char []valueChars = value.toCharArray();

      boolean wasSpace = false;

      for (char valueChar : valueChars) {
        switch (valueChar) {
        case ' ':
          wasSpace = true;

          break;
        case '\n':
          if (wasSpace)
            out.print(' ');

          wasSpace = false;
          out.print("0x0A;");

          break;
        case '\r':
          if (wasSpace)
            out.print(' ');

          wasSpace = false;
          out.print("0x0D;");

          break;
        case '<':
          if (wasSpace)
            out.print(' ');
          wasSpace = false;

          out.print("&lt;");
          break;
        default:
          if (wasSpace)
            out.print(' ');

          wasSpace = false;
          out.write(valueChar);
        }
      }
    }
    out.print("\"");
  }
}