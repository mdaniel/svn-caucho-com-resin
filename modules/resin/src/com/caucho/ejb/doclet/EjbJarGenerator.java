/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.ejb.doclet;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import com.caucho.util.L10N;
import com.caucho.util.BeanUtil;
import com.caucho.util.CharBuffer;

import com.caucho.log.Log;

import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import com.caucho.make.Make;

import com.caucho.java.JavaWriter;

import com.caucho.config.ConfigException;
import com.caucho.config.LineConfigException;

import com.caucho.doclet.RootDocImpl;
import com.caucho.doclet.ClassDocImpl;
import com.caucho.doclet.MethodDocImpl;
import com.caucho.doclet.DocImpl;
import com.caucho.doclet.TagImpl;

/**
 * Class loader which checks for changes in class files and automatically
 * picks up new jars.
 */
public class EjbJarGenerator {
  private static final Logger log = Log.open(EjbJarGenerator.class);
  private static final L10N L = new L10N(EjbJarGenerator.class);

  private HashMap<String,DocletRelation> _relations;

  public void generate(Path path, RootDocImpl rootDoc)
    throws IOException, ConfigException
  {
    WriteStream os = path.openWrite();

    try {
      JavaWriter out = new JavaWriter(os);

      _relations = new HashMap<String,DocletRelation>();
      
      generate(out, rootDoc);
    } finally {
      os.close();
    }
  }

  /**
   * Generates the top-level data for the *.ejb file.
   */
  public void generate(JavaWriter out, RootDocImpl rootDoc)
    throws IOException, ConfigException
  {
    out.println("<ejb-jar xmlns='http://caucho.com/ns/resin'>");
    out.pushDepth();
    
    out.println("<enterprise-beans>");
    out.pushDepth();

    Collection<ClassDocImpl> classes = rootDoc.getClasses();

    Iterator<ClassDocImpl> iter = classes.iterator();
    while (iter.hasNext()) {
      ClassDocImpl classDoc = iter.next();

      if (classDoc.isAssignableTo("javax.ejb.EntityBean")) {
        generateEntity(out, classDoc);
      }
                         
      // generateClass(classDoc);
    }
    
    out.popDepth();
    out.println("</enterprise-beans>");

    if (_relations.size() > 0) {
      generateRelations(out);
    }
    
    out.popDepth();
    out.println("</ejb-jar>");
  }
  
  /**
   * Generates the entity bean for the *.ejb file.
   */
  public void generateEntity(JavaWriter out, ClassDocImpl classDoc)
    throws IOException, ConfigException
  {
    String ejbName = classDoc.getAttribute("@ejb.bean", "name");

    if (ejbName == null)
      return;
    
    out.println("<entity>");
    out.pushDepth();

    boolean isCMP = ! "Bean".equals(classDoc.getAttribute("@ejb.bean", "type"));
                                    
    out.println("<ejb-name>" + ejbName + "</ejb-name>");

    generateEntityClasses(out, classDoc);

    String sqlTable = classDoc.getAttribute("@ejb.persistence", "table-name");
    if (sqlTable == null)
      sqlTable = classDoc.getAttribute("@resin-ejb.entity-bean", "sql-table");
    if (sqlTable != null)
      out.println("<sql-table>" + sqlTable + "</sql-table>");

    if (isCMP) {
      generateCmpFields(out, classDoc);
      generateCmpRelations(out, classDoc);

      generateCmpFind(out, classDoc);
    }
    
    String readOnly = classDoc.getAttribute("@resin-ejb.bean", "read-only");
    if (readOnly != null && ! readOnly.equalsIgnoreCase("false"))
      out.println("<read-only/>");

    String cacheTimeout = classDoc.getAttribute("@resin-ejb.bean", "cache-timeout");
    if (cacheTimeout != null)
      out.println("<cache-timeout>" + cacheTimeout + "</cache-timeout>");

    String cacheSize = classDoc.getAttribute("@resin-ejb.bean", "cache-size");
    if (cacheSize != null)
      out.println("<cache-size>" + cacheSize + "</cache-size>");
    
    out.popDepth();
    out.println("</entity>");
  }

  /**
   * Generates the top-level common section for the entity bean.
   */
  public void generateEntityClasses(JavaWriter out, ClassDocImpl classDoc)
    throws IOException, ConfigException
  {
    String remoteHome = classDoc.getAttribute("@ejb.home", "remote-class");
    String remote = classDoc.getAttribute("@ejb.interface", "remote-class");

    if (remoteHome != null && remote == null)
      throw error(classDoc, L.l("Remote @ejb.interface expected to match remote @ejb.home `{0}'. ", remoteHome));
    
    if (remote != null && remoteHome == null)
      throw error(classDoc, L.l("Remote @ejb.home expected to match remote @ejb.interface `{0}'. ", remote));
    
    if (remoteHome != null)
      out.println("<home>" + remoteHome + "</home>");
    
    if (remote != null)
      out.println("<remote>" + remote + "</remote>");

    String localHome = classDoc.getAttribute("@ejb.home", "local-class");
    String local = classDoc.getAttribute("@ejb.interface", "local-class");

    if (localHome != null && local == null)
      throw error(classDoc, L.l("Local @ejb.interface expected to match local @ejb.home `{0}'. ", localHome));
    
    if (local != null && localHome == null)
      throw error(classDoc, L.l("Local @ejb.home expected to match local @ejb.interface `{0}'. ", local));
    
    if (localHome != null)
      out.println("<local-home>" + localHome + "</local-home>");
    
    if (local != null)
      out.println("<local>" + local + "</local>");
    
    out.println("<ejb-class>" + classDoc.getName() + "</ejb-class>");

    String persistent = classDoc.getAttribute("@ejb.bean", "type");

    if (persistent == null)
      out.println("<persistence-type>Container</persistence-type>");
    else if (persistent.equals("CMP"))
      out.println("<persistence-type>Container</persistence-type>");
    else
      out.println("<persistence-type>Bean</persistence-type>");

    String keyClass = classDoc.getAttribute("@ejb.pk", "class");

    if (keyClass == null)
      out.println("<prim-key-class>java.lang.String</prim-key-class>");
    else
      out.println("<prim-key-class>" + keyClass + "</prim-key-class>");

    String reentrant = classDoc.getAttribute("@ejb.bean", "reentrant");
    if (reentrant == null)
      out.println("<reentrant>False</reentrant>");
    else if (reentrant.equals("True") || reentrant.equals("False"))
      out.println("<reentrant>" + reentrant + "</reentrant>");
    else
      throw error(classDoc, L.l("@ejb.bean reentrant='{0}' is an unknown value.  reentrant must either be `True' or `False'.",
                                reentrant));

    String schema = classDoc.getAttribute("@ejb.bean", "schema");
    if (schema != null)
      out.println("<abstract-schema-name>" + schema + "</abstract-schema-name>");
  }
    
  /**
   * Generates the cmp-field entries.
   */
  public void generateCmpFields(JavaWriter out, ClassDocImpl classDoc)
    throws IOException, ConfigException
  {
    HashSet<String> fields = new HashSet<String>();
    
    ArrayList<MethodDocImpl> methodList = classDoc.getMethods();

    for (int i = 0; i < methodList.size(); i++) {
      MethodDocImpl method = methodList.get(i);

      if (method.getTagList("@ejb.persistent-field") == null &&
	  method.getTagList("@ejb.persistence") == null)
        continue;

      String fieldName = getFieldName(method);

      if (fields.contains(fieldName))
        continue;

      out.println();
      out.println("<cmp-field>");
      out.pushDepth();

      fields.add(fieldName);
      
      out.println("<field-name>" + fieldName + "</field-name>");

      String sqlColumn = method.getAttribute("@ejb.persistence",
                                             "column-name");

      if (sqlColumn == null)
	sqlColumn = method.getAttribute("@resin-ejb.cmp-field",
					"sql-column");

      if (sqlColumn != null)
        out.println("<sql-column>" + sqlColumn + "</sql-column>");
      
      out.popDepth();
      out.println("</cmp-field>");
    }
    
    String primKeyField = classDoc.getAttribute("@ejb.bean", "primkey-field");

    if (primKeyField != null) {
      out.println();
      out.println("<primkey-field>" + primKeyField + "</primkey-field>");
    }
  }
    
  /**
   * Generates the cmp-field entries.
   */
  public void generateCmpRelations(JavaWriter out, ClassDocImpl classDoc)
    throws IOException, ConfigException
  {
    ArrayList<MethodDocImpl> methodList = classDoc.getMethods();
    HashSet<String> fields = new HashSet<String>();

    for (int i = 0; i < methodList.size(); i++) {
      MethodDocImpl method = methodList.get(i);

      if (method.getTagList("@ejb.relation") == null)
        continue;

      String fieldName = getFieldName(method);

      if (fields.contains(fieldName))
        continue;

      fields.add(fieldName);

      String relationName = method.getAttribute("@ejb.relation",
						   "name");

      if (relationName == null)
	throw error(method, L.l("`{0}' is missing an ejb.relation name",
				method.getName()));

      String targetEJB = method.getAttribute("@ejb.relation",
						"target-ejb");

      DocletRelation rel = _relations.get(relationName);
      String ejbName = classDoc.getAttribute("@ejb.bean", "name");
      boolean isSource = true;
      
      if (rel == null) {
	rel = new DocletRelation();
	rel.setName(relationName);
	_relations.put(relationName, rel);

	isSource = true;
      }
      else
	isSource = false;

      if (targetEJB != null) {
	isSource = true;
	rel.setTargetEJB(targetEJB);
      }

      if (isSource) {
	rel.setSourceEJB(ejbName);
	rel.setSourceField(fieldName);
      }
      else {
	rel.setTargetEJB(ejbName);
	rel.setTargetField(fieldName);
      }
    }
  }
    
  /**
   * Generates the find methods.
   */
  public void generateCmpFind(JavaWriter out, ClassDocImpl classDoc)
    throws IOException, ConfigException
  {
    ArrayList<TagImpl> tagList = classDoc.getTagList("@ejb.finder");

    if (tagList == null)
      return;

    for (int i = 0; i < tagList.size(); i++) {
      TagImpl tag = tagList.get(i);

      String signature = tag.getAttribute("signature");
      String query = tag.getAttribute("query");

      out.println();
      out.println("<query>");
      out.pushDepth();
      out.println("<query-method>" + signature + "</query-method>");
      out.println("<ejb-ql><![CDATA[" + query + "]]></ejb-ql>");
      out.popDepth();
      out.println("</query>");
    }
  }
    
  /**
   * Generates the relationships.
   */
  public void generateRelations(JavaWriter out)
    throws IOException, ConfigException
  {
    if (_relations.size() == 0)
      return;

    out.println();
    out.println("<relationships>");
    out.pushDepth();

    Iterator<DocletRelation> iter = _relations.values().iterator();
    while (iter.hasNext()) {
      DocletRelation rel = iter.next();
      
      out.println();
      out.println("<ejb-relation>");
      out.pushDepth();

      // source
      out.println("<ejb-relationship-role>");
      out.pushDepth();
      
      out.println("<relationship-role-source>");
      out.pushDepth();
      out.println("<ejb-name>" + rel.getSourceEJB() + "</ejb-name>");
      out.popDepth();
      out.println("</relationship-role-source>");

      out.println("<cmr-field>");
      out.pushDepth();
      out.println("<cmr-field-name>" + rel.getSourceField() + "</cmr-field-name>");
      out.popDepth();
      out.println("</cmr-field>");
      
      out.popDepth();
      out.println("</ejb-relationship-role>");

      // target
      out.println();
      out.println("<ejb-relationship-role>");
      out.pushDepth();
      
      out.println("<relationship-role-source>");
      out.pushDepth();
      out.println("<ejb-name>" + rel.getTargetEJB() + "</ejb-name>");
      out.popDepth();
      out.println("</relationship-role-source>");

      if (rel.getTargetField() != null) {
	out.println("<cmr-field>");
	out.pushDepth();
	out.println("<cmr-field-name>" + rel.getTargetField() + "</cmr-field-name>");
	out.popDepth();
	out.println("</cmr-field>");
      }
      
      out.popDepth();
      out.println("</ejb-relationship-role>");
      
      out.popDepth();
      out.println("</ejb-relation>");
    }
    
    out.popDepth();
    out.println("</relationships>");
  }

  private String getFieldName(MethodDocImpl method)
  {
    String name = method.getName();

    return BeanUtil.methodNameToPropertyName(name);
  }

  private String escape(String string)
  {
    CharBuffer cb = CharBuffer.allocate();
    
    for (int i = 0; i < string.length(); i++) {
      char ch = string.charAt(i);

      switch (ch) {
      case '<':
	cb.append("&lt;");
	break;
      case '>':
	cb.append("&gt;");
	break;
      case '&':
	cb.append("&amp;");
	break;
      default:
	cb.append((char) ch);
      }
    }

    return cb.close();
  }

  private ConfigException error(DocImpl loc, String msg)
  {
    if (loc == null)
      return new ConfigException(msg);
    else if (loc.getPosition() == null)
      return new ConfigException(loc.getName() + ": " + msg);
    else
      return new LineConfigException(loc.getPosition() + msg);
  }
}
