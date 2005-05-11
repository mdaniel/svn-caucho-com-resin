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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jsp;

import com.caucho.java.JavaCompiler;
import com.caucho.java.LineMap;
import com.caucho.jsp.cfg.JspConfig;
import com.caucho.jsp.cfg.JspPropertyGroup;
import com.caucho.jsp.cfg.JspTaglib;
import com.caucho.jsp.java.JspTagSupport;
import com.caucho.jsp.java.TagTaglib;
import com.caucho.log.Log;
import com.caucho.make.PersistentDependency;
import com.caucho.server.webapp.Application;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.xml.Xml;
import org.xml.sax.SAXException;

import javax.servlet.jsp.tagext.TagInfo;
import javax.servlet.jsp.tagext.TagLibraryInfo;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Compilation interface for JSP pages.
 */
public class JspCompilerInstance {
  private static final Logger log = Log.open(JspCompilerInstance.class);

  // The underlying compiler
  private JspCompiler _jspCompiler;

  // The path to the JSP source
  private Path _jspPath;

  // The JSP uri (user-name)
  private String _uri;

  // The JSP class name
  private String _className;

  private JspPropertyGroup _jspPropertyGroup;

  // The builder
  private JspBuilder _jspBuilder;

  // true for XML parsing
  private boolean _isXml;

  // true for prototype parsing.
  private boolean _isPrototype;

  // true for generated source (like XTP)
  private boolean _isGeneratedSource;

  // The parse state
  private ParseState _parseState;

  // The tag manager
  private ParseTagManager _tagManager;

  // The parser
  private JspParser _parser;

  // The compiled page
  private Page _page;

  // The generator
  private JspGenerator _generator;

  private ArrayList<String> _preludeList = new ArrayList<String>();
  private ArrayList<String> _codaList = new ArrayList<String>();

  private ArrayList<PersistentDependency> _dependList =
    new ArrayList<PersistentDependency>();

  /**
   * Creates a JSP compiler instance.
   */
  JspCompilerInstance(JspCompiler compiler)
  {
    _jspCompiler = compiler;

    _isXml = _jspCompiler.isXml();
  }

  /**
   * Sets the builder.
   */
  void setJspBuilder(JspBuilder builder)
  {
    _jspBuilder = builder;
  }

  /**
   * Sets the path.
   */
  void setJspPath(Path path)
  {
    _jspPath = path;
  }

  /**
   * Sets the uri
   */
  void setURI(String uri)
  {
    _uri = uri;
  }

  /**
   * Sets true for xml
   */
  void setXML(boolean isXml)
  {
    _isXml = isXml;
  }

  /*
   * Sets true for generated source
   */
  void setGeneratedSource(boolean isGeneratedSource)
  {
    _isGeneratedSource = isGeneratedSource;
  }

  /*
   * Sets true for generated source
   */
  public boolean isGeneratedSource()
  {
    return _isGeneratedSource;
  }

  /**
   * Sets the class name.
   */
  void setClassName(String className)
  {
    _className = className;
  }

  /**
   * Adds a dependency.
   */
  public void addDepend(PersistentDependency depend)
  {
    _dependList.add(depend);
  }

  /**
   * Adds a dependency.
   */
  public void addDependList(ArrayList<PersistentDependency> dependList)
  {
    if (dependList != null)
      _dependList.addAll(dependList);
  }

  /**
   * Returns the jsp configuration.
   */
  public JspPropertyGroup getJspPropertyGroup()
  {
    return _jspPropertyGroup;
  }

  /**
   * Returns true for prototype compilation.
   */
  public boolean isPrototype()
  {
    return _isPrototype;
  }

  /**
   * Set true for prototype compilation.
   */
  public void setPrototype(boolean prototype)
  {
    _isPrototype = prototype;
  }

  /**
   * Initialize the instance.
   */
  void init()
    throws Exception
  {
    _parseState = new ParseState();

    if (_className == null)
      _className = JavaCompiler.mangleName("jsp/" + _uri);

    Application app = _jspCompiler.getApplication();
    Path appDir = _jspCompiler.getAppDir();
    if (appDir == null && app != null)
      appDir = app.getAppDir();

    JspConfig jspConfig = null;

    if (jspConfig == null && app != null)
      jspConfig = (JspConfig) app.getExtension("jsp-config");

    _jspPropertyGroup = null;

    if (_jspPropertyGroup == null)
      _jspPropertyGroup = _jspCompiler.getJspPropertyGroup();

    if (_jspPropertyGroup == null && jspConfig != null)
      _jspPropertyGroup = jspConfig.findJspPropertyGroup(_uri);

    if (_jspPropertyGroup == null && app != null)
      _jspPropertyGroup = app.getJsp();

    if (_jspPropertyGroup == null)
      _jspPropertyGroup = _jspCompiler.getJspPropertyGroup();

    JspResourceManager resourceManager = _jspCompiler.getResourceManager();
    if (resourceManager != null) {
    }
    else if (app != null)
      resourceManager = new AppResourceManager(app);
    else {
      resourceManager = new JspResourceManager();
      resourceManager.setAppDir(appDir);
    }

    TaglibManager taglibManager = _jspCompiler.getTaglibManager();

    if (taglibManager == null) {
      taglibManager = new TaglibManager(resourceManager, app);
      taglibManager.setApplication(app);

      if (jspConfig != null) {
        ArrayList<JspTaglib> tldMapList = jspConfig.getTaglibList();
        for (int i = 0; i < tldMapList.size(); i++) {
          JspTaglib taglib = tldMapList.get(i);

          taglibManager.addLocationMap(taglib.getTaglibUri(),
                                        taglib.getTaglibLocation());
        }
      }

      if (app != null) {
        ArrayList<JspTaglib> taglibs = app.getTaglibList();
        for (int i = 0; taglibs != null && i < taglibs.size(); i++) {
          JspTaglib taglib = taglibs.get(i);

          taglibManager.addLocationMap(taglib.getTaglibUri(),
                                       taglib.getTaglibLocation());
        }
      }
    }

    JspPageConfig pageConfig = new JspPageConfig();

    if (_jspPropertyGroup != null) {
      ArrayList<String> preludeList = _jspPropertyGroup.getIncludePreludeList();
      for (int i = 0; preludeList != null && i < preludeList.size(); i++) {
        String prelude = preludeList.get(i);
        _preludeList.add(prelude);
      }

      ArrayList<String> codaList = _jspPropertyGroup.getIncludeCodaList();
      for (int i = 0; codaList != null && i < codaList.size(); i++) {
        String coda = codaList.get(i);
        _codaList.add(coda);
      }

      _parseState.setJspPropertyGroup(_jspPropertyGroup);
      _parseState.setSession(_jspPropertyGroup.isSession());
      _parseState.setScriptingInvalid(_jspPropertyGroup.isScriptingInvalid());
      _parseState.setELIgnored(_jspPropertyGroup.isELIgnored());
      _parseState.setVelocityEnabled(_jspPropertyGroup.isVelocityEnabled());
      _parseState.setPageEncoding(_jspPropertyGroup.getPageEncoding());

      pageConfig.setStaticEncoding(_jspPropertyGroup.isStaticEncoding());

      _parseState.setRecycleTags(_jspPropertyGroup.isRecycleTags());

      if (_jspPropertyGroup.getTldDir() != null)
        taglibManager.setTldDir(_jspPropertyGroup.getTldDir());

      if (_jspPropertyGroup.getTldFileSet() != null)
        taglibManager.setTldFileSet(_jspPropertyGroup.getTldFileSet());
    }

    _parseState.setResourceManager(resourceManager);
    LineMap lineMap = null;

    TagFileManager tagFileManager = new TagFileManager(_jspCompiler);

    _tagManager = new ParseTagManager(resourceManager,
                                      taglibManager,
                                      tagFileManager);

    _jspBuilder = new com.caucho.jsp.java.JavaJspBuilder();
    _jspBuilder.setParseState(_parseState);
    _jspBuilder.setJspCompiler(_jspCompiler);
    _jspBuilder.setJspPropertyGroup(_jspPropertyGroup);
    _jspBuilder.setTagManager(_tagManager);
    _jspBuilder.setPageConfig(pageConfig);
    _jspBuilder.setPrototype(_isPrototype);

    _parser = new JspParser();
    _parser.setJspBuilder(_jspBuilder);
    _parser.setParseState(_parseState);
    _parser.setTagManager(_tagManager);

    _jspBuilder.setJspParser(_parser);

    for (int i = 0; i < _preludeList.size(); i++)
      _parser.addPrelude(_preludeList.get(i));

    for (int i = 0; i < _codaList.size(); i++)
      _parser.addCoda(_codaList.get(i));
  }

  public Page compile()
    throws Exception
  {
    LineMap lineMap = null;
    if (_page != null)
      throw new IllegalStateException("JspCompilerInstance cannot be reused");

    try {
      JspGenerator generator = generate();

      lineMap = generator.getLineMap();

      String encoding = _parseState.getCharEncoding();

      _jspCompiler.compilePending();

      boolean isAutoCompile = true;
      if (_jspPropertyGroup != null)
	isAutoCompile = _jspPropertyGroup.isAutoCompile();

      Page page;
      if (! generator.isStatic()) {
        compileJava(_jspPath, _className, lineMap, encoding);

        page = _jspCompiler.loadPage(_className, isAutoCompile);
      }
      else {
        page = _jspCompiler.loadStatic(_className,
                                       _parseState.isOptionalSession());
        page._caucho_addDepend(generator.getDependList());
        page._caucho_setContentType(_parseState.getContentType());
      }

      return page;
    } catch (JspParseException e) {
      e.setLineMap(lineMap);
      e.setErrorPage(_parseState.getErrorPage());
      throw e;
    } catch (SAXException e) {
      if (e.getCause() instanceof JspParseException) {
	JspParseException subE = (JspParseException) e.getCause();

	subE.setLineMap(lineMap);
	subE.setErrorPage(_parseState.getErrorPage());
	throw subE;
      }
      else {
	JspParseException exn = new JspParseException(e);
	exn.setErrorPage(_parseState.getErrorPage());
	exn.setLineMap(lineMap);

	throw exn;
      }
    } catch (FileNotFoundException e) {
      throw e;
    } catch (IOException e) {
      JspParseException exn = new JspParseException(e);
      exn.setLineMap(lineMap);
      exn.setErrorPage(_parseState.getErrorPage());

      throw exn;
    } catch (Throwable e) {
      JspParseException exn = new JspParseException(e);
      exn.setLineMap(lineMap);
      exn.setErrorPage(_parseState.getErrorPage());

      throw exn;
    }
  }

  public JspGenerator generate()
    throws Exception
  {
    if (_page != null)
      throw new IllegalStateException("JspCompilerInstance cannot be reused");

    boolean isXml = _isXml;
    if (_jspPropertyGroup != null && ! isXml)
      isXml = _jspPropertyGroup.isXml();

    try {
      if (isXml) {
	Xml xml = new Xml();
	xml.setContentHandler(new JspContentHandler(_jspBuilder));
	_jspPath.setUserPath(_uri);
	xml.setNamespaceAware(true);
	xml.parse(_jspPath);
      }
      else
	_parser.parse(_jspPath, _uri);

      JspGenerator generator = _jspBuilder.getGenerator();
      generator.setJspCompilerInstance(this);

      for (int i = 0; i < _dependList.size(); i++)
	generator.addDepend(_dependList.get(i));

      generator.validate();

      generator.generate(_jspPath, _className);

      return generator;
    } catch (JspParseException e) {
      e.setErrorPage(_parseState.getErrorPage());
      throw e;
    } catch (SAXException e) {
      if (e.getCause() instanceof JspParseException) {
	JspParseException subE = (JspParseException) e.getCause();

	subE.setErrorPage(_parseState.getErrorPage());
	throw subE;
      }
      else {
	JspParseException exn = new JspParseException(e);
	exn.setErrorPage(_parseState.getErrorPage());

	throw exn;
      }
    } catch (FileNotFoundException e) {
      throw e;
    } catch (IOException e) {
      JspParseException exn = new JspParseException(e);
      exn.setErrorPage(_parseState.getErrorPage());

      throw exn;
    }
  }

  public TagInfo compileTag(TagLibraryInfo taglib)
    throws Exception
  {
    TagInfo preloadTag = preloadTag(taglib);

    if (preloadTag != null)
      return preloadTag;

    return generateTag(taglib );
  }

  private TagInfo preloadTag(TagLibraryInfo taglib)
  {
    try {
      JspTagSupport tag = (JspTagSupport) _jspCompiler.loadClass(_className, true);

      if (tag == null)
	return null;

      tag.init(_jspCompiler.getAppDir());

      if (tag._caucho_isModified())
	return null;

      return tag._caucho_getTagInfo(taglib);
    } catch (Throwable e) {
      return null;
    }
  }

  private TagInfo generateTag(TagLibraryInfo taglib)
    throws Exception
  {
    LineMap lineMap = null;
    if (_page != null)
      throw new IllegalStateException("JspCompilerInstance cannot be reused");

    try {
      boolean isXml = _isXml;
      if (_jspPropertyGroup != null && ! isXml)
	isXml = _jspPropertyGroup.isXml();

      if (_jspCompiler.addTag(_className)) {
	_isPrototype = true;
	_jspBuilder.setPrototype(true);
      }

      _parseState.setTag(true);
      if (isXml) {
	Xml xml = new Xml();
	xml.setContentHandler(new JspContentHandler(_jspBuilder));
	_jspPath.setUserPath(_uri);
	xml.setNamespaceAware(true);
	xml.parse(_jspPath);
      }
      else
	_parser.parseTag(_jspPath, _uri);

      _generator = _jspBuilder.getGenerator();

      if (_isPrototype) {
	return _generator.generateTagInfo(_className, taglib);
      }

      _generator.generate(_jspPath, _className);

      if (_jspCompiler.hasRecursiveCompile()) {
	_jspCompiler.addPending(this);

	return _generator.generateTagInfo(_className, taglib);
      }

      return completeTag(taglib);
    } catch (JspParseException e) {
      e.setLineMap(lineMap);
      e.setErrorPage(_parseState.getErrorPage());
      throw e;
    } catch (FileNotFoundException e) {
      throw e;
    } catch (IOException e) {
      JspParseException exn = new JspParseException(e);
      exn.setErrorPage(_parseState.getErrorPage());
      exn.setLineMap(lineMap);
      throw exn;
    }
  }

  TagInfo completeTag()
    throws Exception
  {
    return completeTag(new TagTaglib("x", "uri"));
  }

  TagInfo completeTag(TagLibraryInfo taglib)
    throws Exception
  {
    LineMap lineMap = null;

    try {
      lineMap = _generator.getLineMap();

      String encoding = _parseState.getCharEncoding();

      compileJava(_jspPath, _className, lineMap, encoding);

      JspTagSupport tag = (JspTagSupport) _jspCompiler.loadClass(_className, true);

      tag.init(_jspCompiler.getAppDir());

      return tag._caucho_getTagInfo(taglib);
      // Page page = _jspCompiler.loadClass(_className);
    } catch (JspParseException e) {
      e.setLineMap(lineMap);
      e.setErrorPage(_parseState.getErrorPage());
      throw e;
    } catch (FileNotFoundException e) {
      throw e;
    } catch (IOException e) {
      JspParseException exn = new JspParseException(e);
      exn.setErrorPage(_parseState.getErrorPage());
      exn.setLineMap(lineMap);
      throw exn;
    } catch (Throwable e) {
      JspParseException exn = new JspParseException(e);
      exn.setErrorPage(_parseState.getErrorPage());
      exn.setLineMap(lineMap);
      throw exn;
    }
  }

  private void compileJava(Path path, String className,
                           LineMap lineMap, String charEncoding)
    throws Exception
  {
    JavaCompiler compiler = JavaCompiler.create(null);
    compiler.setClassDir(_jspCompiler.getClassDir());
    // compiler.setEncoding(charEncoding);
    String fileName = className.replace('.', '/') + ".java";

    boolean remove = true;

    try {
      compiler.compile(fileName, lineMap);
    } finally {
      if (remove)
        Vfs.lookup(fileName).remove();
    }

    Path classDir = _jspCompiler.getClassDir();
    Path classPath = classDir.lookup(className.replace('.', '/') + ".class");
    Path smapPath =  classDir.lookup(fileName + ".smap");

    compiler.mergeSmap(classPath, smapPath);
  }

  private void readSmap(ClassLoader loader, String className)
  {
    if (loader == null)
      return;

    String smapName = className.replace('.', '/') + ".java.smap";

    InputStream is = null;
    try {
      is = loader.getResourceAsStream(smapName);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
        }
      }
    }
  }
}
