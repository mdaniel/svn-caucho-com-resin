/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.xsl.java;

import com.caucho.java.JavaWriter;
import com.caucho.xml.QName;
import com.caucho.xsl.XslParseException;

/**
 * changes the output
 */
public class XslResultDocument extends XslNode {
  private String _href;
  
  /**
   * Returns the tag name.
   */
  public String getTagName()
  {
    return "xsl:result-document";
  }
  
  /**
   * Adds an attribute.
   */
  public void addAttribute(QName name, String value)
    throws XslParseException
  {
    if (name.getName().equals("href")) {
      _href = value;
    }
    else
      super.addAttribute(name, value);
  }
  
  /**
   * Adds an attribute.
   */
  public void endAttributes()
    throws XslParseException
  {
    if (_href == null)
      throw error(L.l("xsl:result-document requires a 'href' attribute."));
  }

  /**
   * Generates the code for the tag
   *
   * @param out the output writer for the generated java.
   */
  public void generate(JavaWriter out)
    throws Exception
  {
    out.println("XslWriter oldOut = out;");
    out.println("OutputStream os = null;");
    out.println("try {");
    out.pushDepth();
    out.print("os = out.openWrite(env, ");
    generateString(out, _href, '+');
    out.println(");");

    out.println("out = out.openResultDocument(os);");
    
    generateChildren(out);
    
    out.println("out.close();");
    out.popDepth();
    out.println("} finally {");
    out.println("  if (os != null)");
    out.println("    os.close();");
    out.println("  out = oldOut;");
    out.println("}");
  }
}
