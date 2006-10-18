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

package com.caucho.jsp;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.beans.*;

import javax.servlet.*;
import javax.servlet.jsp.tagext.*;

import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.log.Log;
import com.caucho.server.http.*;
import com.caucho.jsp.cfg.*;

/**
 * Stores the entire information for a tag library.
 */
public class Taglib extends TagLibraryInfo {
  private static final Logger log = Log.open(Taglib.class);
  static final L10N L = new L10N(Taglib.class);

  private TldTaglib _tldTaglib;
  
  TagLibraryValidator _validator;
  private ArrayList<TldFunction> _functionList = new ArrayList<TldFunction>();
  
  Taglib(String prefix, String uri, TldTaglib tldTaglib,
	 TagFileManager tagFileManager)
    throws JspParseException
  {
    super(prefix, uri);

    try {
      _tldTaglib = tldTaglib;
      
      fillTagLibraryInfo(tldTaglib, tagFileManager);
    } catch (JspParseException e) {
      throw e;
    } catch (Exception e) {
      throw new JspParseException(e);
    }
  }

  /**
   * Gets a new instance of the validator to check the page.
   */
  public TagLibraryValidator getValidator()
    throws JspParseException
  {
    return _validator;
  }

  /**
   * Returns the functions.
   */
  public ArrayList<TldFunction> getFunctionList()
  {
    return _functionList;
  }

  /**
   * Gets the path.
   */
  public Path getPath()
  {
    if (_tldTaglib != null)
      return _tldTaglib.getJarPath();
    else
      return null;
  }

  /**
   * Fills the tag library info from the tld
   *
   * <pre>
   * taglib ::= tlib-version, jsp-version?, short-name, uri?,
   *            display-name?, small-icon?, large-icon?, description?,
   *            validator?, listener*, tag+, function*
   * </pre>
   */
  private void fillTagLibraryInfo(TldTaglib taglib,
				  TagFileManager tagFileManager)
    throws Exception
  {
    this.tlibversion = taglib.getTlibVersion();
    
    this.jspversion = taglib.getJspVersion();

    this.shortname = taglib.getShortName();
    
    this.urn = taglib.getURI();
    this.info = taglib.getInfo();

    if (taglib.getDescription() != null)
      this.info = taglib.getDescription();

    TldValidator validator = taglib.getValidator();

    if (validator != null)
      _validator = validator.getValidator();

    ArrayList<TldTag> tagList = taglib.getTagList();

    tags = new TagInfo[tagList.size()];

    for (int i = 0; i < tagList.size(); i++) {
      TldTag tag = tagList.get(i);

      TagInfo tagInfo = new TagInfoImpl(tag, this);

      tags[i] = tagInfo;
    }

    ArrayList<TldTagFile> tagFileList = taglib.getTagFileList();

    this.tagFiles = new TagFileInfo[tagFileList.size()];

    for (int i = 0; i < tagFileList.size(); i++) {
      TldTagFile tagFile = tagFileList.get(i);
      
      TagInfo tagInfo = tagFileManager.getTag("",
					      tagFile.getName(),
					      tagFile.getPath());
      System.out.println("TI: " + tagInfo);

      TagFileInfo tagFileInfo = new TagFileInfo(tagFile.getName(),
						tagFile.getPath(),
						tagInfo);

      this.tagFiles[i] = tagFileInfo;
    }

    _functionList = taglib.getFunctionList();

    this.functions = new FunctionInfo[_functionList.size()];

    for (int i = 0; i < _functionList.size(); i++) {
      this.functions[i] = _functionList.get(i).toFunctionInfo();
    }
  }
    
  /**
   * Returns the tag class for the given tag qname
   *
   * @param tagName the tag's qname
   *
   * @return the matching class or null
   */
  public Class getClass(String tagName)
    throws Exception
  {
    TagInfo info = getTag(tagName);
    String className = info == null ? null : info.getTagClassName();

    if (className != null)
      return CauchoSystem.loadClass(className);
    else
      return null;
  }

  /**
   * Return the class names of all tags that are outside of packages.
   */
  public ArrayList<String> getSingleTagClassNames()
  {
    ArrayList<String> singleTags = new ArrayList<String>();

    TagInfo []tags = getTags();
    for (int i = 0; tags != null && i < tags.length; i++) {
      String name = tags[i].getTagClassName();

      if (name != null && name.indexOf('.') < 0)
        singleTags.add(name);
    }

    return singleTags;
  }

  /**
   * Returns the TagExtraInfo structure for the named tag.
   */
  TagExtraInfo getTagExtraInfo(String tagName)
  {
    TagInfo info = getTag(tagName);
    
    return info != null ? info.getTagExtraInfo() : null;
  }

  /**
   * Hack to avoid JSDK problem.
   */
  public TagInfo getTag(String name)
  {
    if (tags == null)
      return null;
    
    for (int i = 0; i < tags.length; i++) {
      if (tags[i].getTagName().equals(name))
	return tags[i];
    }

    return null;
  }

  /**
   * Returns a matching tag file.
   */
  public String getTagFilePath(String name)
  {
    if (_tldTaglib == null)
      return null;

    ArrayList<TldTagFile> tagFiles = _tldTaglib.getTagFileList();
    
    for (int i = 0; i < tagFiles.size(); i++) {
      TldTagFile tagFile = tagFiles.get(i);

      if (tagFile.getName().equals(name))
	return tagFile.getPath();
    }

    return null;
  }

  @Override
  public TagLibraryInfo []getTagLibraryInfos()
  {
    return new TagLibraryInfo[] { this };
  }

  public String toString()
  {
    return "Taglib[prefix=" + prefix + ",uri=" + uri + "]";
  }
}
