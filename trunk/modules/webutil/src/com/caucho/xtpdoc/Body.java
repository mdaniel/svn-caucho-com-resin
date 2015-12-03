/*
 * Copyright (c) 1998-2000 Caucho Technology -- all rights reserved
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
 * @author Emil Ong
 */

package com.caucho.xtpdoc;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.logging.Logger;

public class Body extends ContainerNode {
  private static Logger log = Logger.getLogger(Body.class.getName());

  private Summary _summary;
  private Navigation _navigation;
  private Index _index;
  private String _class;
  private String _title = "Resin Documentation";
  private boolean _isCauchoSite = true;

  public Body(Document document)
  {
    super(document);
  }

  private void parseNavigation()
  {
    NavigationItem item = getDocument().getNavigation();

    if (item != null)
      _navigation = item.getNavigation();
  }

  public void setCauchoSite(boolean cauchoSite) {
      _isCauchoSite  = cauchoSite;
  }
  public void setClass(String styleClass)
  {
    _class = styleClass;
  }

  public Summary createSummary()
  {
    _summary = new Summary(getDocument());

    addItem(_summary);

    return _summary;
  }

  public Localtoc createLocaltoc()
  {
    Localtoc toc = new Localtoc(getDocument());

    addItem(toc);

    return toc;
  }

  public ReferenceLegend createReferenceLegend()
  {
    ReferenceLegend legend = new ReferenceLegend();

    addItem(legend);

    return legend;
  }

  public Faq createFaq()
  {
    Faq faq = new Faq(getDocument());
    addItem(faq);
    return faq;
  }

  public S1 createS1()
  {
    S1 s1 = new S1(getDocument());
    addItem(s1);
    return s1;
  }

  public Defun createDefun()
  {
    Defun defun = new Defun(getDocument());
    addItem(defun);
    return defun;
  }

  public IncludeDefun createIncludeDefun()
  {
    IncludeDefun includeDefun = new IncludeDefun(getDocument());
    addItem(includeDefun);
    return includeDefun;
  }

  public Index createIxx()
  {
    _index = new Index(getDocument());
    return _index;
  }


  public void setTitle(String title)
  {
    _title = title;
  }

  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    out.writeStartElement("body");

    if (_class != null)
      out.writeAttribute("class", _class);


    NavigationItem item = writeHeaderTable(out);




    /* Body table */
    out.writeStartElement("table");
    out.writeAttribute("width", "98%");
    out.writeAttribute("border", "0");
    out.writeAttribute("cellspacing", "0");
    out.writeAttribute("cellpadding", "0");
    out.writeAttribute("summary", "");



    // left

    // left navigation
    out.writeStartElement("tr");
    out.writeAttribute("valign", "top");

    out.writeStartElement("td");
    out.writeAttribute("class", "leftnav");

    parseNavigation();

    getDocument().writeLeftNav(out);

    out.writeEndElement(); // td

    //Space
    out.writeStartElement("td");
    out.writeAttribute("width","5%");
    out.writeEntityRef("nbsp");
    out.writeEndElement(); // td



    out.writeStartElement("td");
    out.writeAttribute("width","85%");
    // actual body

    out.writeStartElement("h1");
    out.writeAttribute("class", "title");

    if (getDocument().getHeader() != null)
      out.writeCharacters(getDocument().getHeader().getTitle().toLowerCase(Locale.ENGLISH));

    out.writeEndElement();

    out.writeStartElement("hr");
    out.writeEndElement();

    if (item != null) {
      writeThreadNavigation(out, item, false);
    }

    Header header = getDocument().getHeader();

    if (header != null && header.getDescription() != null) {
      header.getDescription().writeHtml(out);
    }

    if (header != null
        && header.getTutorialStartPage() != null
        && ! getDocument().isDisableAction()) {
      out.writeStartElement("p");
      out.writeStartElement("a");
      out.writeAttribute("href", header.getTutorialStartPage());
      out.writeCharacters("Demo");
      out.writeEndElement();
      out.writeEndElement();
    }

    if (_index != null)
      _index.writeHtml(out);

    writeContent(out);

    if (header != null
        && header.getTutorialStartPage() != null
        && ! getDocument().isDisableAction()) {
      out.writeStartElement("p");
      out.writeStartElement("a");
      out.writeAttribute("href", header.getTutorialStartPage());
      out.writeCharacters("Demo");
      out.writeEndElement();
      out.writeEndElement();
    }

    out.writeStartElement("hr");
    out.writeEndElement();

    if (item != null) {
      writeThreadNavigation(out, item, true);
    }

    // nav

    out.writeStartElement("table");
    out.writeAttribute("border", "0");
    out.writeAttribute("cellspacing", "0");
    out.writeAttribute("width", "100%");
    out.writeStartElement("tr");
    out.writeStartElement("td");
    out.writeStartElement("em");
    out.writeStartElement("small");
    out.writeCharacters("Copyright ");
    out.writeEntityRef("copy");
    out.writeCharacters(" 1998-2015 Caucho Technology, Inc. All rights reserved.");
    //out.writeEmptyElement("br");
    out.writeEntityRef("nbsp");
    out.writeCharacters("Resin ");
    out.writeStartElement("sup");
    out.writeStartElement("font");
    out.writeAttribute("size", "-1");
    out.writeEntityRef("#174");
    out.writeEndElement();
    out.writeEndElement();
    out.writeCharacters(" is a registered trademark. Quercus");
    out.writeStartElement("sup");
    out.writeCharacters("tm");
    out.writeEndElement();
    out.writeCharacters(", and Hessian");
    out.writeStartElement("sup");
    out.writeCharacters("tm");
    out.writeEndElement();
    out.writeCharacters(" are trademarks of Caucho Technology.");
    if (this._isCauchoSite ) {
        out.writeEmptyElement("br");
        out.writeEmptyElement("br");
        out.writeCharacters("Cloud-optimized Resin Server is a Java EE certified Java Application Server, and Web Server, and Distributed Cache Server (Memcached).");
        out.writeEmptyElement("br");
        out.writeCharacters("Leading companies worldwide with demand for reliability and high performance web applications "+
                "including SalesForce.com, CNET, DZone and many more are powered by Resin.");
        out.writeEmptyElement("br");
        out.writeEmptyElement("br");
    }
    out.writeEndElement(); // small
    out.writeEndElement(); // em
    out.writeEndElement(); // td
    out.writeEndElement(); // tr


    /////////////////////////////////////////////////////////////////////////////////////////////////////
    if (_isCauchoSite ) {
        out.writeStartElement("tr");
        out.writeStartElement("td");
        out.writeStartElement("center");
        out.writeStartElement("small");
        writeTopLinks(out);


        out.writeEndElement(); // small
        out.writeEndElement(); // center
        out.writeEndElement(); // td
        out.writeEndElement(); // tr


    }

    out.writeEndElement(); // table

    out.writeEndElement(); // td
    out.writeEndElement(); // tr


    out.writeEndElement(); // table

    out.writeEmptyElement("div");
    out.writeAttribute("id", "popup");

    out.writeEndElement(); //body
  }

  private NavigationItem writeHeaderTable(XMLStreamWriter out)
        throws XMLStreamException
  {
    //header table

    out.writeStartElement("table");
    out.writeAttribute("width", "98%");
    out.writeAttribute("border", "0");
    out.writeAttribute("cellspacing", "0");
    out.writeAttribute("cellpadding", "0");
    out.writeAttribute("summary", "");



    out.writeStartElement("tr");

    /* LOGO Cell */
    out.writeStartElement("td");
    out.writeStartElement("a");
    out.writeAttribute("href", "http://www.caucho.com/");
    out.writeAttribute("title", "Caucho maker of Resin Server | Application Server (Java EE Certified) and Web Server");
    out.writeStartElement("img");
    out.writeAttribute("src", getDocument().getContextPath() + "/images/caucho-white.jpg");
    out.writeAttribute("alt", "Caucho maker of Resin Server | Application Server (Java EE Certified) and Web Server");
    //out.writeAttribute("width", "300");
    //out.writeAttribute("height", "50");
    out.writeEndElement();//img
    out.writeEndElement();//a


    /* Write Bread Crumb in Logo cell */
    out.writeEmptyElement("br");
    out.writeEmptyElement("br");
    out.writeEmptyElement("br");
    out.writeStartElement("div");
    out.writeAttribute("class", "breadcrumb");
    NavigationItem item = getDocument().getNavigation();
    writeBreadcrumb(out, item);
    out.writeEndElement();//end breadcrumb div
    out.writeEndElement();//td end logo cell


    //Space
    out.writeStartElement("td");
    out.writeAttribute("width","5%");
    out.writeEntityRef("nbsp");
    out.writeEndElement(); // td

    //Marketing blurb area
    out.writeStartElement("td");
    out.writeAttribute("width","70%");
    out.writeStartElement("h1");

    // out.writeCharacters("Resin Documentation");
    out.writeCharacters(_title);

    /*
    out.writeEndElement();
    out.writeStartElement("div");
    out.writeAttribute("style","float: right; border: 1px solid; padding: 10px;  margin: 10px; opacity:1.0; background-color:white;");
    out.writeStartElement("small");

    out.writeStartElement("a");
    out.writeAttribute("style", "text-decoration: none");
    out.writeAttribute("href","http://wiki4.caucho.com/NginX_1.2.0_versus_Resin_4.0.29_performance_tests");
    out.writeAttribute("title","Resin outscales C-based web server nginx in AutoBench benchmark");
    out.writeCharacters("Aug 2012: Resin outscales C-based web server nginx in AutoBench benchmark");
    out.writeEndElement(); // a

    out.writeEmptyElement("br");

    out.writeStartElement("a");
    out.writeAttribute("style", "text-decoration: none");
    out.writeAttribute("href","http://www.caucho.com/resin-application-server/press/netcraft-says-resin-application-server-grows-10x-over-last-year/");
    out.writeAttribute("title","Resin Web Server Powers Over 4.7 Million Sites and grows by near 10x states NetCraft Feb 2012 Web Survey");
    out.writeCharacters("Feb 2012: NetCraft survey says Resin experiencing strong growth in last year and used in number of the Million Busiest Sites.");
    out.writeEndElement(); // a

    out.writeEndElement(); // small
    out.writeEndElement(); // div
    out.writeEndElement(); // td
    */

    //Top links
    out.writeStartElement("td");
    out.writeAttribute("width","20%");
    writeTopLinks(out);
    out.writeEndElement(); // td

    //Space
    out.writeStartElement("td");
    out.writeAttribute("width","5%");
    out.writeEntityRef("nbsp");
    out.writeEndElement(); // td

    //Resin Logo
    out.writeStartElement("td");
    out.writeAttribute("width","10%");
    out.writeAttribute("valign","bottom");
    out.writeStartElement("a");
    out.writeAttribute("href", "http://www.caucho.com/");
    out.writeAttribute("title", "Resin Server | Application Server (Java EE Certified) and Web Server");
    out.writeStartElement("img");
    out.writeAttribute("src", getDocument().getContextPath() + "/images/resin_logo_small.jpg");
    out.writeAttribute("alt", "Resin Server | Application Server (Java EE Certified) and Web Server");
    out.writeEndElement();//img
    out.writeEndElement();//a
    out.writeEndElement(); // td


    /* End Header table. */

    out.writeEndElement(); //tr
    out.writeEndElement(); //table
    return item;
  }

  private void writeTopLinks(XMLStreamWriter out) throws XMLStreamException
  {
    out.writeStartElement("a");
    out.writeAttribute("href", "http://www.caucho.com/");
    out.writeAttribute("title", "Caucho home, makers of Resin Server, Java Application Server, Web Server and Querus PHP engine.");
    out.writeCharacters("home");
    out.writeEndElement(); // a
    out.writeEntityRef("nbsp");

    out.writeStartElement("a");
    out.writeAttribute("href", "http://www.caucho.com/about/");
    out.writeCharacters("company");
    out.writeEndElement(); // a
    out.writeEntityRef("nbsp");

    out.writeStartElement("a");
    out.writeAttribute("href", "http://blog.caucho.com/");
    out.writeAttribute("title", "Blog to discuss Resin Server, a Java EE certified Java Application Server, Web Server and Querus PHP engine");
    out.writeCharacters("blog");
    out.writeEndElement(); // a
    out.writeEntityRef("nbsp");

    out.writeStartElement("a");
    out.writeAttribute("href", "http://www.caucho.com/resin-4.0/");
    out.writeCharacters("docs");
    out.writeEndElement(); // a
    out.writeEntityRef("nbsp");

    out.writeEmptyElement("br");

    out.writeStartElement("a");
    out.writeAttribute("href", "http://www.caucho.com/");
    out.writeAttribute("title", "Resin | Java Application Server | Java EE Certified | Cloud Optimized ");
    out.writeCharacters("app server");
    out.writeEndElement(); // a
    out.writeEntityRef("nbsp");
    /*

    out.writeStartElement("a");
    out.writeAttribute("href", "http://www.caucho.com/resin-web-server/");
    out.writeAttribute("title", "Resin | Web Server | Proxy Cache | Cluster/Cloud aware Load balancing | FastCGI | Reverse Proxy | Recoverability and non-stop Reliability ");
    out.writeCharacters("web server");
    out.writeEndElement(); // a
    out.writeEntityRef("nbsp");

    out.writeEmptyElement("br");

    out.writeStartElement("a");
    out.writeAttribute("href", "http://www.caucho.com/");
    out.writeAttribute("title", "Resin Pro | Health System | Watchdog for recoverability and self healing | Server Monitoring | JVM Monitoring | Anomaly detection for detecting issues before they become problems | Server status reports");
    out.writeCharacters("health");
    out.writeEndElement(); // a
    out.writeEntityRef("nbsp");

    out.writeStartElement("a");
    out.writeAttribute("href", "http://www.caucho.com/resin-application-server/3g-java-clustering-cloud/");
    out.writeAttribute("title", "Resin Pro | Cloud System | 3rd generation clustering technology | optimized for EC2, OpenStack | Cloud deployment | true elasticity | operational predictability | PaaS and DevOps ready");
    out.writeCharacters("cloud");
    out.writeEndElement(); // a
    out.writeEntityRef("nbsp");


    out.writeStartElement("a");
    out.writeAttribute("href", "http://www.caucho.com/resin-application-server/java-ee-web-profile/");
    out.writeAttribute("title", "Resin  | Web Profile | Java EE Web Profile certified");
    out.writeCharacters("java ee");
    out.writeEndElement(); // a
    out.writeEntityRef("nbsp");


    out.writeStartElement("a");
    out.writeAttribute("href", "http://www.caucho.com/resin-application-server/resin-professional-application-server/");
    out.writeAttribute("title", "Resin  Pro | support, health and cloud  | choice of high traffic web sites");
    out.writeCharacters("pro");
    out.writeEndElement(); // a
    out.writeEntityRef("nbsp");
    */
  }

  protected void writeContent(XMLStreamWriter out)
    throws XMLStreamException
  {
    super.writeHtml(out);
  }


  public void writeBreadcrumb(XMLStreamWriter out, NavigationItem item)
    throws XMLStreamException
  {
    out.writeCharacters("\n");
    out.writeStartElement("a");
    out.writeAttribute("href", "/");
    out.writeCharacters("home");
    out.writeEndElement();

    writeBreadcrumbRec(out, item);
  }

  public void writeBreadcrumbRec(XMLStreamWriter out, NavigationItem item)
    throws XMLStreamException
  {
    if (item == null || item.getParent() == null)
      return;

    writeBreadcrumbRec(out, item.getParent());

    out.writeCharacters(" / ");
    out.writeStartElement("a");
    out.writeAttribute("href", item.getLink());
    out.writeCharacters(item.getTitle().toLowerCase(Locale.ENGLISH));
    out.writeEndElement();
  }

  public void writeThreadNavigation(XMLStreamWriter out,
                                    NavigationItem item,
                                    boolean writeCenter)
    throws XMLStreamException
  {
    out.writeCharacters("\n");
    out.writeStartElement("table");
    out.writeAttribute("class", "breadcrumb");
    out.writeAttribute("border", "0");
    out.writeAttribute("cellspacing", "0");
    out.writeAttribute("width", "99%");
    out.writeStartElement("tr");

    out.writeStartElement("td");
    out.writeAttribute("width", "30%");
    out.writeAttribute("align", "left");
    if (item.getPrevious() != null) {
      item.getPrevious().writeLink(out);
    }
    out.writeEndElement();

    out.writeStartElement("td");
    out.writeAttribute("width", "40%");
    out.writeStartElement("center");
    if (item.getParent() != null && writeCenter) {
      item.getParent().writeLink(out);
    }
    out.writeEndElement();
    out.writeEndElement();

    out.writeStartElement("td");
    out.writeAttribute("width", "30%");
    out.writeAttribute("align", "right");
    if (item.getNext() != null) {
      item.getNext().writeLink(out);
    }
    out.writeEndElement();

    out.writeEndElement();
    out.writeEndElement();
  }


  public void writeLaTeXTop(PrintWriter out)
    throws IOException
  {
    out.println("\\begin{document}");

    super.writeLaTeXTop(out);

    out.println("\\end{document}");
  }

}
