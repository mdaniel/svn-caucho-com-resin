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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jsp.java;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.lang.reflect.*;
import java.beans.*;

import javax.el.*;

import javax.servlet.http.*;
import javax.servlet.*;
import javax.servlet.jsp.tagext.*;
import javax.servlet.jsp.el.ELException;

import com.caucho.util.*;
import com.caucho.vfs.*;

import com.caucho.make.PersistentDependency;
import com.caucho.make.ClassDependency;

import com.caucho.log.Log;

import com.caucho.loader.DynamicClassLoader;

import com.caucho.xml.QName;
import com.caucho.xml.XmlChar;

import com.caucho.server.http.*;
import com.caucho.java.*;
import com.caucho.config.types.*;
import com.caucho.jsp.*;
import com.caucho.jsp.el.*;
import com.caucho.xpath.*;
import com.caucho.el.*;

import com.caucho.jsp.cfg.TldFunction;

/**
 * Generates JSP code.  JavaGenerator, JavaScriptGenerator, and
 * StaticGenerator specialize the JspGenerator for language-specific
 * requirements.
 *
 * <p>JspParser parses the JSP file into an XML-DOM tree.  JspGenerator
 * generates code from that tree.
 */
public class JavaJspGenerator extends JspGenerator {
  static final L10N L = new L10N(JavaJspGenerator.class);
  static final Logger log = Log.open(JavaJspGenerator.class);
  
  static final String IE_CLSID = "clsid:8AD9C840-044E-11D1-B3E9-00805F499D93";
  static final String IE_URL = "http://java.sun.com/products/plugin/1.2.2/jinstall-1_2_2-win.cab#Version=1,2,2,0";
  static final String NS_URL = "http://java.sun.com/products/plugin/";

  static HashMap<String,Class> _primitiveClasses;
  static HashMap<String,String> _primitives;

  protected JspNode _rootNode;

  protected ParseState _parseState;

  /*
   * Variables storing the JSP directives.
   */
  protected boolean _ideHack = false;

  /*
   * Variables controlling caching
   * isUncacheable overrides isCacheable.
   */
  protected boolean _isCacheable;
  protected boolean _isUncacheable;
  protected ArrayList<Depend> _cacheDepends = new ArrayList<Depend>();
  
  // dependencies for the source file itself
  protected ArrayList<PersistentDependency> _depends =
  new ArrayList<PersistentDependency>();

  long _lastModified; // XXX: obsolete?
  
  protected TagInstance _topTag;
  protected int _tagId;
  
  // XXX: needed in combination with XTP
  boolean _alwaysModified;
  
  protected ParseTagManager _tagManager;

  protected JspPageConfig _config = new JspPageConfig();

  protected String _fullClassName;
  protected String _className;
  private HashMap<String,Class> _classes;
  private ClassLoader _parentLoader;

  private HashSet<String> _declaredVariables = new HashSet<String>();

  private String _filename;

  private final JspGenELContext _elContext;
  
  private HashMap<String,Method> _elFunctionMap = new HashMap<String,Method>();
  private ArrayList<Taglib> _tagLibraryList =
    new ArrayList<Taglib>();
  private PageData _pageData;
  
  protected IntMap _strings = new IntMap();
  
  private ArrayList<com.caucho.el.Expr> _exprList =
  new ArrayList<com.caucho.el.Expr>();
  
  private ArrayList<com.caucho.el.Expr> _valueExprList =
  new ArrayList<com.caucho.el.Expr>();
  
  private ArrayList<MethodExpr> _methodExprList =
  new ArrayList<MethodExpr>();
  
  private ArrayList<com.caucho.xpath.Expr> _xpathExprList =
  new ArrayList<com.caucho.xpath.Expr>();
  
  private ArrayList<JspFragmentNode> _fragmentList =
    new ArrayList<JspFragmentNode>();

  private String _workPath;
  private String _sourceName;
  protected String _pkg;
  private int _uniqueId = 0;
  private int _jspId = 1;

  private boolean _hasReleaseTag;
  private boolean _hasBundle = false;
  private boolean _hasBundlePrefix = false;
  private boolean _requireSource = false;
  
  private boolean _isOmitXmlDeclaration = false;
  
  private String _doctypeSystem;
  private String _doctypePublic;
  private String _doctypeRootElement;

  private boolean _isStatic = false;

  protected ArrayList<JspDeclaration> _declarations =
  new ArrayList<JspDeclaration>();

  public JavaJspGenerator(ParseTagManager tagManager)
  {
    _elContext = new JspGenELContext(this);
    
    _tagManager = tagManager;
    
    _topTag = new TagInstance(tagManager);
  }

  public TagInstance getRootTag()
  {
    return _topTag;
  }
  
  protected void setParseState(ParseState parseState)
  {
    _parseState = parseState;
  }
  
  public ParseState getParseState()
  {
    return _parseState;
  }

  public void setPageConfig(JspPageConfig pageConfig)
  {
    _config = pageConfig;
  }

  void setStaticEncoding(boolean staticEncoding)
  {
    _config.setStaticEncoding(staticEncoding);
  }

  void setRequireSource(boolean requireSource)
  {
    _requireSource = requireSource;
  }

  void setIdeHack(boolean ideHack)
  {
    _ideHack = ideHack;
  }

  String getPackagePrefix()
  {
    return "";
  }

  Path getAppDir()
  {
    return _jspCompiler.getAppDir();
  }

  /**
   * Returns true for XML.
   */
  boolean isXml()
  {
    return _jspCompiler.isXml();
  }

  /**
   * Returns true if the XML declaration should be set.
   */
  boolean isOmitXmlDeclaration()
  {
    return _isOmitXmlDeclaration;
  }

  /**
   * Returns true if the XML declaration should be set.
   */
  void setOmitXmlDeclaration(boolean omitXml)
  {
    _isOmitXmlDeclaration = omitXml;
  }

  /**
   * Sets the dtd system name
   */
  void setDoctypeSystem(String doctypeSystem)
  {
    _doctypeSystem = doctypeSystem;
  }

  /**
   * Gets the dtd system name
   */
  String getDoctypeSystem()
  {
    return _doctypeSystem;
  }

  /**
   * Sets the dtd public name
   */
  void setDoctypePublic(String doctypePublic)
  {
    _doctypePublic = doctypePublic;
  }

  /**
   * Gets the dtd public name
   */
  String getDoctypePublic()
  {
    return _doctypePublic;
  }

  /**
   * Gets the dtd root element name
   */
  void setDoctypeRootElement(String doctypeRootElement)
  {
    _doctypeRootElement = doctypeRootElement;
  }

  /**
   * Gets the dtd root element name
   */
  String getDoctypeRootElement()
  {
    return _doctypeRootElement;
  }

  /**
   * Returns the character encoding.
   */
  String getCharacterEncoding()
  {
    return _parseState.getCharEncoding();
  }

  Path getClassDir()
  {
    return _jspCompiler.getClassDir();
  }

  /**
   * Sets the root JSP node.
   */
  public void setRootNode(JspNode node)
  {
    _rootNode = node;
  }

  public JspPageConfig getConfig()
  {
    return _config;
  }

  public boolean isTag()
  {
    return false;
  }

  public void init()
  {
    _isOmitXmlDeclaration = ! isXml();
  }

  public boolean hasScripting()
  {
    return _rootNode.hasScripting();
  }

  /**
   * Adds a taglib.
   */
  public void addTaglib(String prefix, String uri)
    throws JspParseException
  {
    addTaglib(prefix, uri, false);
  }

  /**
   * Adds a taglib.
   */
  public void addOptionalTaglib(String prefix, String uri)
    throws JspParseException
  {
    addTaglib(prefix, uri, true);
  }
  
  /**
   * Adds a taglib.
   */
  public void addTaglib(String prefix, String uri, boolean isOptional)
    throws JspParseException
  {
    if (log.isLoggable(Level.FINEST))
      log.finest("taglib prefix=" + prefix + " uri:" + uri);

    Taglib taglib;

    try {
      taglib = _tagManager.addTaglib(prefix, uri);
    } catch (JspParseException e) {
      if (isOptional) {
	log.log(Level.FINE, e.toString(), e);
	return;
      }

      throw e;
    }

    if (taglib == null && isOptional && 
	! uri.startsWith("urn:jsptld:") && ! uri.startsWith("urn:jsptagdir:"))
      return;

    if (taglib == null)
      throw error(L.l("`{0}' has no matching taglib-uri.  The taglib uri must match a taglib-uri for a taglib specified in the web.xml or implicitly in a taglib.tld in the tag jar.", uri));

    if (! _tagLibraryList.contains(taglib))
      _tagLibraryList.add(taglib);
			 
    ArrayList<TldFunction> functions = taglib.getFunctionList();

    for (int i = 0; i < functions.size(); i++) {
      TldFunction function = functions.get(i);

      String name = taglib.getPrefixString() + ":" + function.getName();

      _elFunctionMap.put(name, function.getMethod());
    }
  }

  Method resolveFunction(String prefix, String localName)
  {
    if (prefix.equals(""))
      return _elFunctionMap.get(localName);
    else
      return _elFunctionMap.get(prefix + ':' + localName);
  }

  /**
   * Adds a taglib.
   */
  public void addTaglibDir(String prefix, String tagdir)
    throws JspParseException
  {
    Taglib taglib = _tagManager.addTaglibDir(prefix, tagdir);

    ArrayList<TldFunction> functions = taglib.getFunctionList();

    for (int i = 0; i < functions.size(); i++) {
      TldFunction function = functions.get(i);

      String name = taglib.getPrefixString() + ":" + function.getName();

      _elFunctionMap.put(name, function.getMethod());
    }
  }

  /**
   * Returns true if the JSP compilation has produced a static file.
   */
  public boolean isStatic()
  {
    return _isStatic;
  }

  /**
   * Returns the page's XML view.
   */
  public PageData getPageData()
    throws IOException
  {
    if (_pageData != null)
      return _pageData;
    
    TempStream ts = new TempStream(null);

    ts.openWrite();
    WriteStream ws = new WriteStream(ts);
    ws.setEncoding("UTF-8");

    _rootNode.printXml(ws);
    
    ws.close();
    
    _pageData = new QPageData(ts);

    return _pageData;
  }

  public ELContext getELContext()
  {
    return _elContext;
  }
  
  /**
   * Validates the JSP page.
   */
  public void validate()
    throws Exception
  {
    for (int i = 0; i < _tagLibraryList.size(); i++) {
      Taglib taglib = _tagLibraryList.get(i);
      TagLibraryValidator validator = taglib.getValidator();

      if (validator != null) {
	ValidationMessage []messages;

	messages = validator.validate(taglib.getPrefixString(),
				      taglib.getURI(),
				      getPageData());

	if (messages != null && messages.length > 0) {
          CharBuffer message = CharBuffer.allocate();
          for (int j = 0; j < messages.length; j++) {
            if (j != 0)
              message.append("\n");
            message.append(messages[j].getMessage());
          }
          
          throw _rootNode.error(message.close());
	}
      }
    }
  }

  /**
   * Generates the JSP page.
   */
  protected void generate(Path path, String className)
    throws Exception
  {
    init(className);

    if (_jspCompilerInstance == null ||
	! _jspCompilerInstance.isGeneratedSource())
      addDepend(path);

    _cacheDepends = new ArrayList<Depend>();

    _tagId = 1;

    if (_ideHack)
      _config.setStaticEncoding(false);

    // disable static pages.  No longer needed and complicates
    // precompilation
    if (isGenerateStatic() &&
	! _parseState.getJspPropertyGroup().getStaticPageGeneratesClass()) {
      generateStatic();
    }
    else {
      WriteStream os = openWriteStream();
      JspJavaWriter out = new JspJavaWriter(os, this);

      try {
        generate(out);
      } finally {
        if (os != null)
          os.close();
      }
    }

    if (_lineMap != null) {
      Path javaPath = getGeneratedPath();
      String tail = javaPath.getTail();
      tail = tail + ".smap";
      WriteStream os = javaPath.getParent().lookup(tail).openWrite();

      LineMapWriter writer = new LineMapWriter(os);
      writer.write(_lineMap);
      os.close();
    }
  }

  public void addDepend(Path path)
  {
    addDepend(new Depend(path));
  }

  /**
   * Adds a dependency based on a class.
   */
  public void addDepend(Class cl)
  {
    addDepend(new ClassDependency(cl));
  }

  public void addDepend(PersistentDependency depend)
  {
    if (! _depends.contains(depend))
      _depends.add(depend);
  }

  public ArrayList<PersistentDependency> getDependList()
  {
    return _depends;
  }

  public boolean isStaticEncoding()
  {
    return _config.isStaticEncoding();
  }

  public boolean getRecycleTags()
  {
    return _parseState.isRecycleTags();
  }

  /**
   * Adds a new Java declaration to the list.
   */
  public void addDeclaration(JspDeclaration decl)
  {
    _declarations.add(decl);
  }

  /**
   * Sets the taglib manager.
   */
  public void setTagManager(ParseTagManager tagManager)
  {
    _tagManager = tagManager;
  }

  /**
   * Returns the taglib manager.
   */
  public ParseTagManager getTagManager()
  {
    return _tagManager;
  }

  protected void init(String className)
  {
    _fullClassName = className; 
    _className = className;

    String prefix = getPackagePrefix();
    if (prefix.endsWith("."))
      prefix = prefix.substring(0, prefix.length() - 1);

    int p = className.lastIndexOf('.');
    if (p > 0) {
      _pkg = className.substring(0, p);
      _className = className.substring(p + 1);
    }
    else
      _pkg = "";

    if (prefix.length() > 0 && _pkg.length() > 0)
      _pkg = prefix + "." + _pkg;
    else if (prefix.length() > 0)
      _pkg = prefix;

    _workPath = _pkg.replace('.', '/');

    _lineMap = new LineMap(className.replace('.', '/') + ".java");
  }

  /**
   * True if it's a declared variable.
   */
  public boolean isDeclared(String var)
  {
    return _declaredVariables.contains(var);
  }

  /**
   * Adds a declared variable.
   */
  public void addDeclared(String var)
  {
    _declaredVariables.add(var);
  }

  /**
   * Generates the Java code.
   */
  protected void generate(JspJavaWriter out)
    throws Exception
  {
    out.setLineMap(_lineMap);
    
    generateClassHeader(out);

    generatePageHeader(out);
    printTry(out);

    _rootNode.generate(out);
      
    generatePageFooter(out);

    // _rootNode.generateDeclaration(out);

    generateClassFooter(out);
  }

  /**
   * Generates a static file.
   */
  protected void generateStatic()
    throws Exception
  {
    _isStatic = true;
    
    Path javaPath = getGeneratedPath();
    String tail = javaPath.getTail();
    int p = tail.indexOf('.');
    tail = tail.substring(0, p);

    Path staticPath = javaPath.getParent().lookup(tail + ".static");

    WriteStream os = staticPath.openWrite();
    //os.setEncoding(_parseState.getCharEncoding());
    os.setEncoding("UTF-8");

    try {
      JspJavaWriter out = new JspJavaWriter(os, this);

      _rootNode.generateStatic(out);
    } finally {
      os.close();
    }
    
    Path dependPath = javaPath.getParent().lookup(tail + ".depend");
    StaticPage.writeDepend(dependPath, getDependList());
  }

  /**
   * Generates the class header.
   *
   * @param doc the XML document representing the JSP page.
   */
  protected void generateClassHeader(JspJavaWriter out)
    throws IOException, JspParseException
  {
    out.println("/*");
    out.println(" * JSP generated by " + com.caucho.Version.FULL_VERSION);
    out.println(" */" );
    out.println();

    if (_pkg != null && ! _pkg.equals(""))
      out.println("package " + _pkg + ";");

    out.println("import javax.servlet.*;");
    out.println("import javax.servlet.jsp.*;");
    out.println("import javax.servlet.http.*;");

    fillSingleTaglibImports();

    ArrayList<String> imports = _parseState.getImportList();
    for (int i = 0; i < imports.size(); i++) {
      String name = imports.get(i);
      out.print("import ");
      out.print(name);
      out.println(";");
    }
    _parseState.addImport("javax.servlet.*");
    _parseState.addImport("javax.servlet.jsp.*");
    _parseState.addImport("javax.servlet.http.*");
    _parseState.addImport("java.lang.*");
    out.println();

    if (_parseState.getExtends() != null) {
      //if (extendsLocation != null)
      //setLocation(extendsLocation.srcFilename, extendsLocation.srcLine, 0);

      out.print("public class ");
      out.print(_className);
      out.print(" extends ");
      out.print(_parseState.getExtends().getName());
      out.print(" implements com.caucho.jsp.CauchoPage");
      if (! _parseState.isThreadSafe())
        out.print(", javax.servlet.SingleThreadModel");
    } else {
      out.print("public class ");
      out.print(_className);
      out.print(" extends com.caucho.jsp.JavaPage");
      if (! _parseState.isThreadSafe())
        out.print(" implements javax.servlet.SingleThreadModel");
    }

    out.println("{");
    out.pushDepth();

    out.println("private boolean _caucho_isDead;");

    String info = _parseState.getInfo();
    if (info != null) {
      out.println();
      out.print("public String getServletInfo() { return \"");
      for (int i = 0; i < info.length(); i++) {
	char ch = info.charAt(i);
	if (ch == '\\')
	  out.print("\\\\");
	else if (ch == '\n')
	  out.print("\\n");
	else if (ch == '\r')
	  out.print("\\r");
	else if (ch == '"')
	  out.print("\\\"");
	else
	  out.print(ch);
      }
      out.println("\"; }");
    }
    
    for (int i = 0; i < _declarations.size(); i++) {
      JspDeclaration decl = _declarations.get(i);

      out.println();
      decl.generateDeclaration(out);
    }
  }

  /**
   * As a convenience, when the Tag isn't in a package, import
   * it automatically.
   */
  protected void fillSingleTaglibImports()
    throws JspParseException
  {
    /*
    Iterator<Taglib> iter = _taglibMap.values().iterator();
    
    while (iter.hasNext()) {
      Taglib taglib = iter.next();

      if (taglib == null)
        continue;
      
      ArrayList<String> singleTags = taglib.getSingleTagClassNames();

      for (int i = 0; i < singleTags.size(); i++) {
        String className = singleTags.get(i);

        _parseState.addImport(className);
      }
    }
    */
  }

  /**
   * Prints the _jspService header
   */
  protected void generatePageHeader(JspJavaWriter out) throws Exception
  {
    out.println("");
    out.println("public void");
    out.println("_jspService(javax.servlet.http.HttpServletRequest request,");
    out.println("            javax.servlet.http.HttpServletResponse response)");
    out.println("  throws java.io.IOException, javax.servlet.ServletException");
    out.println("{");
    out.pushDepth();

    // static shouldn't start up a session
    boolean isSession = _parseState.isSession() && ! _rootNode.isStatic();
    
    if (isSession) {
      out.println("javax.servlet.http.HttpSession session = request.getSession(true);");
    }
    out.println("com.caucho.server.webapp.WebApp _jsp_application = _caucho_getApplication();");
    out.println("javax.servlet.ServletContext application = _jsp_application;");

    out.print("com.caucho.jsp.PageContextImpl pageContext = com.caucho.jsp.QJspFactory.allocatePageContext(");
    out.print("this, _jsp_application, request, response, ");
    if (_parseState.getErrorPage() == null) 
      out.print("null");
    else
      out.print("\"" + _parseState.getErrorPage() + "\"");
    out.print(", ");
    if (isSession) {
      out.print("session");
    }
    else
      out.print("null");
    out.print(", ");
    out.print(_parseState.getBuffer());
    out.print(", ");
    out.print(_parseState.isAutoFlush());
    out.print(", ");
    out.print(_parseState.isPrintNullAsBlank());
    out.println(");");
    out.println("javax.servlet.jsp.JspWriter out = pageContext.getOut();");
    out.println("final javax.el.ELContext _jsp_env = pageContext.getELContext();");
    out.println("javax.servlet.ServletConfig config = getServletConfig();");
    out.println("javax.servlet.Servlet page = this;");
    if (_parseState.isErrorPage()) {
      out.println("java.lang.Throwable exception = ((com.caucho.jsp.PageContextImpl) pageContext).getThrowable();");
    }

    generateContentType(out);

    /*
    for (int i = 0; i < _fragmentList.size(); i++) {
      JspNode node = _fragmentList.get(i);

      if (node.isStatic())
	out.println("com.caucho.jsp.StaticJspFragmentSupport _jsp_frag_" + i + " = null;");
      else
	out.println("_CauchoFragment _jsp_frag_" + i + " = null;");
    }
    */
    
    _rootNode.generatePrologue(out);
  }

  /**
   * Generates the content type of the page.
   */
  private void generateContentType(JspJavaWriter out)
    throws IOException
  {
    String encoding = Encoding.getMimeName(_parseState.getCharEncoding());
    
    if (encoding != null && encoding.equals("ISO-8859-1"))
      encoding = null;

    String contentType = _parseState.getContentType();
    if (contentType != null && contentType.equals("text/html"))
      contentType = null;

    out.print("response.setContentType(\"");
    if (contentType == null)
      out.print("text/html");
    else {
      out.printJavaString(contentType);
    }
    out.println("\");");
    
    if (encoding != null)
      out.println("request.setCharacterEncoding(\"" + encoding + "\");");
  }

  private void printTry(JspJavaWriter out) throws IOException
  {
    out.println("try {");
    out.pushDepth();
    //    out.println("_caucho_init_tags(pageContext, _jsp_tags);");
  }

  public int addString(String string)
  {
    int index = _strings.get(string);
    if (index < 0) {
      index = _strings.size();
      _strings.put(string, index);
    }
    return index;
  }

  /**
   * Saves a bean's class for later introspection.
   *
   * @param id the bean id
   * @param typeName the bean's type
   */
  public void addBeanClass(String id, String typeName)
    throws Exception
  {
    if (_classes == null)
      _classes = new HashMap<String,Class>();

    try {
      if (_primitives.get(typeName) != null)
        return;

      Class cl = getBeanClass(typeName);

      if (cl == null)
        throw error(L.l("Can't find class '{0}'",
                        typeName));

      _classes.put(id, cl);
    } catch (CompileClassNotFound e) {
      log.log(Level.WARNING, e.toString(), e);

      throw error(L.l("Can't find class '{0}'\n{1}",
		      typeName, e.getMessage()));
    } catch (ClassNotFoundException e) {
      log.log(Level.FINE, e.toString(), e);

      throw error(L.l("Can't find class '{0}'", typeName));
    }
  }

  /**
   * Loads a bean based on the class name.
   */
  public Class getBeanClass(String typeName)
    throws ClassNotFoundException
  {
    
    // Arrays need to use Array.newInstance(cl, new int[]);
    int p = typeName.indexOf('[');
    if (p > 0) {
      Class cl = getBeanClass(typeName.substring(0, p));
      int count = 0;
      for (int i = 0; i < typeName.length(); i++)
        if (typeName.charAt(i) == '[')
          count++;
      int []dims = new int[count];
      for (int i = 0; i < count; i++)
        dims[i] = 1;

      Object obj = Array.newInstance(cl, dims);
      
      return obj.getClass();
    }

    Class cl = loadBeanClass(typeName);
    if (cl != null)
      return cl;

    // Inner classes need rewriting Foo.Bar -> Foo$Bar
    int i = typeName.lastIndexOf('.');
    for (; i >= 0; i = typeName.lastIndexOf('.', i - 1)) {
      String mainClassName = typeName.substring(0, i);
      Class mainClass = loadBeanClass(mainClassName);
      
      typeName = mainClassName + '$' + typeName.substring(i + 1);
      
      if (mainClass != null)
        return getBeanClass(typeName);
    }

    return null;
  }
  
  Class loadBeanClass(String typeName)
  {
    Class cl = _primitiveClasses.get(typeName);

    if (cl != null)
      return cl;

    try {
      return CauchoSystem.loadClass(typeName);
    } catch (CompileClassNotFound e) {
      log.log(Level.FINE, e.toString(), e);
    } catch (ClassNotFoundException e) {
    }

    // qualified names don't use the imports
    if (typeName.indexOf('.') >= 0)
      return null;

    ArrayList<String> imports = _parseState.getImportList();
    for (int i = 0; i < imports.size(); i++) {
      String pkg = imports.get(i);
      String fullName = null;

      if (pkg.endsWith("." + typeName))
        fullName = pkg;
      else if (pkg.endsWith(".*"))
        fullName = pkg.substring(0, pkg.length() - 1) + typeName;
      else
        continue;
    
      try {
        return CauchoSystem.loadClass(fullName);
      } catch (CompileClassNotFound e) {
        log.log(Level.WARNING, e.toString(), e);
      } catch (ClassNotFoundException e) {
      }
    }

    return null;
  }

  public Class getClass(String id)
  {
    if (_classes == null)
      return null;

    return _classes.get(id);
  }

  protected void generatePageFooter(JspJavaWriter out) throws IOException
  {
    out.popDepth();
    out.println("} catch (java.lang.Throwable _jsp_e) {");
    out.println("  pageContext.handlePageException(_jsp_e);");
    out.println("} finally {");
    out.pushDepth();

    for (int i = 0; i < _topTag.size(); i++) {
      TagInstance tag = _topTag.get(i);

      if (tag.getTagClass() == null) {
      }
      else if (Tag.class.isAssignableFrom(tag.getTagClass())) {
        out.println("if (" + tag.getId() + " != null)");
        out.println("  " + tag.getId() + ".release();");
      }
    }
    
    if (_hasReleaseTag) {
      out.popDepth();
      out.println("} finally {");
      out.pushDepth();
    }
    
    out.println("com.caucho.jsp.QJspFactory.freePageContext(pageContext);");

    if (_hasReleaseTag) {
      out.popDepth();
      out.println("}");
    }
    
    // close finally
    out.popDepth();
    out.println("}");
    out.popDepth();
    out.println("}");
  }

  /**
   * Completes the generated class: closing the main method, generating
   * the dependencies and generating the constant strings.
   *
   * @param doc the XML document representing the JSP page.
   */
  protected void generateClassFooter(JspJavaWriter out) throws Exception
  {
    // fragments must be first because they may create others.
    generateFragments(out);
    
    generateDepends(out);

    generateExprs(out);
    generateXPath(out);
    generateConstantStrings(out);

    out.popDepth();
    out.println("}");
  }

  public int addFragment(JspFragmentNode node)
  {
    int index = _fragmentList.indexOf(node);

    if (index >= 0)
      return index;
    
    _fragmentList.add(node);
    
    return _fragmentList.size() - 1;
  }

  public JspFragmentNode getFragment(int index)
  {
    return _fragmentList.get(index);
  }
  
  /**
   * Adds an expression to the expression list.
   */
  public int addExpr(String value)
    throws JspParseException, ELException
  {
    return addExpr(genExpr(value));
  }
  
  public com.caucho.el.Expr genExpr(String value)
    throws JspParseException, ELException
  {
    JspELParser parser = new JspELParser(_elContext, value);

    return parser.parse();
  }

  /**
   * Adds an expression to the expression list.
   */
  public int addExpr(com.caucho.el.Expr expr)
    throws JspParseException
  {
    int index = _exprList.indexOf(expr);
    if (index >= 0)
      return index;

    index = _exprList.size();
    _exprList.add(expr);

    return index;
  }
  
  /**
   * Adds an expression to the expression list.
   */
  public int addValueExpr(String value)
    throws JspParseException, ELException
  {
    JspELParser parser = new JspELParser(_elContext, value);

    com.caucho.el.Expr expr = parser.parse();

    int index = _valueExprList.indexOf(expr);
    if (index >= 0)
      return index;

    index = _valueExprList.size();
    _valueExprList.add(expr);

    return index;
  }
  
  /**
   * Adds an expression to the expression list.
   */
  public int addMethodExpr(String value, String sigString)
    throws JspParseException, ELException
  {
    JspELParser parser = new JspELParser(_elContext, value);

    com.caucho.el.Expr expr = parser.parse();

    Class []args = new Class[0];

    try {
      if (sigString != null && ! sigString.equals("")) {
	Signature sig = new Signature(sigString);

	String []types = sig.getParameterTypes();

	args = new Class[types.length];

	for (int i = 0; i < types.length; i++) {
	  args[i] = getBeanClass(types[i]);
	}
      }
    } catch (ClassNotFoundException e) {
      throw new ELException(e);
    }

    MethodExpr methodExpr = new MethodExpr(expr, args);

    int index = _methodExprList.indexOf(methodExpr);
    if (index >= 0)
      return index;

    index = _methodExprList.size();
    _methodExprList.add(methodExpr);

    return index;
  }

  /**
   * out.Prints the expressions
   */
  private void generateExprs(JspJavaWriter out) throws IOException
  {
    for (int i = 0; i < _exprList.size(); i++) {
      com.caucho.el.Expr expr = _exprList.get(i);
      
      out.println("private final static com.caucho.el.Expr _caucho_expr_" + i + " =");
      out.print("  ");
      expr.printCreate(out.getWriteStream());
      out.println(";");
    }
    
    for (int i = 0; i < _valueExprList.size(); i++) {
      com.caucho.el.Expr expr = _valueExprList.get(i);
      
      out.println("private final static javax.el.ValueExpression _caucho_value_expr_" + i + " = new com.caucho.el.ObjectValueExpression(");
      out.print("  ");
      expr.printCreate(out.getWriteStream());
      out.println(");");
    }
    
    for (int i = 0; i < _methodExprList.size(); i++) {
      MethodExpr expr = _methodExprList.get(i);
      
      out.println("private final static javax.el.MethodExpression _caucho_method_expr_" + i + " = new com.caucho.el.MethodExpressionImpl(");
      out.print("  ");
      expr.getExpr().printCreate(out.getWriteStream());
      out.print(", \"");
      out.printJavaString(expr.getExpr().toString());
      out.print("\", String.class, new Class[] {");

      Class []args = expr.getArgs();
      for (int j = 0; j < args.length; j++) {
	if (j != 0)
	  out.print(", ");

	out.printClass(args[j]);
	out.print(".class");
      }
      
      out.println("});");
    }
  }

  /**
   * Adds an expression to the expression list.
   */
  public String addXPathExpr(String value, NamespaceContext ns)
    throws JspParseException, XPathParseException
  {
    return addXPathExpr(XPath.parseExpr(value, ns));
  }

  /**
   * Adds an expression to the expression list.
   */
  public String addXPathExpr(com.caucho.xpath.Expr expr)
    throws JspParseException
  {
    int index = _xpathExprList.indexOf(expr);
    if (index >= 0)
      return "_caucho_xpath_" + index;

    index = _xpathExprList.size();
    _xpathExprList.add(expr);

    return "_caucho_xpath_" + index;
  }

  /**
   * out.Prints the expressions
   */
  private void generateXPath(JspJavaWriter out) throws IOException
  {
    if (_xpathExprList.size() == 0)
      return;
    
    for (int i = 0; i < _xpathExprList.size(); i++) {
      com.caucho.xpath.Expr expr = _xpathExprList.get(i);
      
      out.println("private static com.caucho.xpath.Expr _caucho_xpath_" + i + ";");
    }

    out.println("static {");
    out.pushDepth();
    out.println("try {");
    out.pushDepth();
    
    for (int i = 0; i < _xpathExprList.size(); i++) {
      com.caucho.xpath.Expr expr =  _xpathExprList.get(i);
      
      out.print("_caucho_xpath_" + i + " =");
      out.println(" com.caucho.xpath.XPath.parseExpr(\"" + expr + "\");");
    }

    out.popDepth();
    out.println("} catch (Exception e) {");
    out.println("  e.printStackTrace();");
    out.println("}");
    out.popDepth();
    out.println("}");
  }

  /**
   * out.Prints the fragments
   */
  private void generateFragments(JspJavaWriter out) throws Exception
  {
    boolean hasFragment = false;

    for (int i = 0; i < _fragmentList.size(); i++) {
      JspFragmentNode node = _fragmentList.get(i);

      if (node.isStatic()) {
      }
      /*
      else if (node.isValueFragment()) {
	node.generateValueMethod(out);
      }
      */
      else
	hasFragment = true;
    }
    
    if (! hasFragment)
      return;

    out.println("public static class _CauchoFragment extends com.caucho.jsp.JspFragmentSupport {");
    out.pushDepth();
    out.println("private int _frag_code;");

    out.println();
    out.println("static _CauchoFragment create(_CauchoFragment frag, int code,");
    out.println("                              com.caucho.jsp.PageContextImpl pageContext,");
    out.println("                              javax.servlet.jsp.tagext.JspTag parent,");
    out.println("                              javax.servlet.jsp.tagext.JspFragment jspBody)");
    out.println("{");
    out.pushDepth();
    out.println("if (frag == null)");
    out.println("  frag = new _CauchoFragment();");
    out.println();
    out.println("frag._frag_code = code;");
    out.println("frag.pageContext = pageContext;");
    out.println("frag._jsp_env = pageContext.getELContext();");
    out.println("frag._jsp_parent_tag = parent;");
    out.println("frag._jspBody = jspBody;");
    out.println();
    out.println("return frag;");
    out.popDepth();
    out.println("}");
    
      
    for (int i = 0; i < _fragmentList.size(); i++) {
      JspFragmentNode frag = _fragmentList.get(i);

      if (frag.isStatic())
	continue;

      if (frag.isValueFragment())
	frag.generateValueMethod(out);
      else {
	out.println();
	out.println("private void " + frag.getFragmentName() + "(JspWriter out)");
	out.println("  throws Throwable");
	out.println("{");
	out.pushDepth();
	frag.generatePrologueChildren(out);
	frag.generate(out);
	out.popDepth();
	out.println("}");
      }
    }

    out.println();
    out.println("protected void _jsp_invoke(JspWriter out)");
    out.println("  throws Throwable");
    out.println("{");
    out.pushDepth();
    out.println("switch (_frag_code) {");
    
    for (int i = 0; i < _fragmentList.size(); i++) {
      JspFragmentNode frag = _fragmentList.get(i);
      
      if (frag.isStatic() || frag.isValueFragment())
	continue;
      
      out.println("case " + i + ":");
      out.println("  " + frag.getFragmentName() + "(out);");
      out.println("  break;");
    }

    out.println("}");
    
    out.popDepth();
    out.println("}");
    
    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the dependency methods.  Since we can't assume the
   * underlying class is Page when the JSP page uses "extends"
   * each JSP page needs to generate this code.
   */
  private void generateDepends(JspJavaWriter out) throws IOException
  {
    out.println();
    // out.println("private com.caucho.java.LineMap _caucho_line_map;");
    out.println("private java.util.ArrayList _caucho_depends = new java.util.ArrayList();");
    if (_isCacheable && ! _isUncacheable)
      out.println("private java.util.ArrayList _caucho_cacheDepends = new java.util.ArrayList();");
    
    out.println();
    out.println("public java.util.ArrayList _caucho_getDependList()");
    out.println("{");
    out.println("  return _caucho_depends;");
    out.println("}");
    
    out.println();
    out.println("public void _caucho_addDepend(com.caucho.make.PersistentDependency depend)");
    out.println("{");
    if (_parseState.getExtends() == null)
      out.println("  super._caucho_addDepend(depend);");
    out.println("  com.caucho.jsp.JavaPage.addDepend(_caucho_depends, depend);");
    out.println("}");

    out.println();
    out.println("public boolean _caucho_isModified()");
    out.println("{");
    out.pushDepth();
    out.println("if (_caucho_isDead)");
    out.println("  return true;");
    out.println("if (com.caucho.util.CauchoSystem.getVersionId() != " +
            CauchoSystem.getVersionId() + "L)");
    out.println("  return true;");
    
    ArrayList<PersistentDependency> depends;
    depends = new ArrayList<PersistentDependency>();
    depends.addAll(_parseState.getDependList());

    for (int i = 0; i < _depends.size(); i++) {
      PersistentDependency depend = _depends.get(i);

      if (! depends.contains(depend))
	depends.add(depend);
    }

    if (_alwaysModified)
      out.println("return true;");

    else if (depends.size() == 0)
      out.println("return false;");

    else {
      out.println("for (int i = _caucho_depends.size() - 1; i >= 0; i--) {");
      out.pushDepth();
      out.println("com.caucho.make.Dependency depend;");
      out.println("depend = (com.caucho.make.Dependency) _caucho_depends.get(i);");
      out.println("if (depend.isModified())");
      out.println("  return true;");
      out.popDepth();
      out.println("}");
      out.println("return false;");
    }
    
    out.popDepth();
    out.println("}");

    if (_rootNode.isStatic() && CauchoSystem.isTest())
      out.println("private static long _caucho_lastModified = com.caucho.util.Alarm.getCurrentTime();");
    
    out.println();
    out.println("public long _caucho_lastModified()");
    out.println("{");
    out.pushDepth();
    /*
    if (! _isCacheable || _isUncacheable)
      out.println("return 0;");
    else {
      out.println("return com.caucho.jsp.Page.calculateLastModified(_caucho_depends, _caucho_cacheDepends);");
    }
    */

    if (! isGenerateStatic()) {
      out.println("return 0;");
    }
    else if (CauchoSystem.isTest()) {
      out.println("return _caucho_lastModified;");
    }
    else {
      out.println("long lastModified = 0;");
      
      out.println("for (int i = _caucho_depends.size() - 1; i >= 0; i--) {");
      out.pushDepth();
      out.println("Object oDepend = _caucho_depends.get(i);");
      out.println("if (oDepend instanceof com.caucho.vfs.Depend) {");
      out.println("  com.caucho.vfs.Depend depend = (com.caucho.vfs.Depend) oDepend;");
      out.println("  if (lastModified < depend.getLastModified())");
      out.println("    lastModified = depend.getLastModified();");
      out.println("}");
      out.popDepth();
      out.println("}");

      out.println();
      out.println("return lastModified;");
    }
    
    out.popDepth();
    out.println("}");

    /*
    out.println();
    out.println("public com.caucho.java.LineMap _caucho_getLineMap()");
    out.println("{");
    out.pushDepth();
    out.println("return _caucho_line_map;");
    out.popDepth();
    out.println("}");
    */

    if (_parseState.getExtends() == null && ! _parseState.isTag()) {
      out.println();
      out.println("public void destroy()");
      out.println("{");
      out.pushDepth();
      out.println("  _caucho_isDead = true;");
      out.println("  super.destroy();");
      out.popDepth();
      out.println("}");
    }

    if (_parseState.getExtends() != null && ! _parseState.isTag()) {
      out.println();
      out.println("public com.caucho.server.webapp.WebApp _caucho_getApplication()");
      out.println("{");
      out.pushDepth();
      out.println(" return (com.caucho.server.webapp.WebApp) getServletConfig().getServletContext();");
      out.popDepth();
      out.println("}");
    }

    printDependInit(out, depends);
  }

  private boolean isGenerateStatic()
  {
    return (_rootNode.isStatic() &&
	    ! _parseState.isTag() &&
	    _parseState.getExtends() == null);
  }

  /**
   * out.Prints the initialization methods to track dependencies.
   */
  private void printDependInit(JspJavaWriter out,
			       ArrayList<PersistentDependency> depends)
    throws IOException
  {
    out.println();
    out.println("public void init(com.caucho.vfs.Path appDir)");
    out.println("  throws javax.servlet.ServletException");
    out.println("{");
    out.pushDepth();

    if (_ideHack) {
      out.println("_jsp_init_strings();");
    }

    out.println("com.caucho.vfs.Path resinHome = com.caucho.util.CauchoSystem.getResinHome();");
    out.println("com.caucho.vfs.MergePath mergePath = new com.caucho.vfs.MergePath();");
    out.println("mergePath.addMergePath(appDir);");
    out.println("mergePath.addMergePath(resinHome);");
    out.println("com.caucho.loader.DynamicClassLoader loader;");
    out.println("loader = (com.caucho.loader.DynamicClassLoader) getClass().getClassLoader();");
    out.println("String resourcePath = loader.getResourcePathSpecificFirst();");
    out.println("mergePath.addClassPath(resourcePath);");

    MergePath classPath = new MergePath();
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    if (classLoader instanceof DynamicClassLoader) {
      DynamicClassLoader loader = (DynamicClassLoader) classLoader;
    
      String resourcePath = loader.getResourcePathSpecificFirst();
      classPath.addClassPath(resourcePath);
    }
    
    String srcName = _filename;
    if (srcName == null)
      srcName = "foo";

    /*
    out.print("_caucho_line_map = new com.caucho.java.LineMap(\"");
    out.printJavaString(_lineMap.getDestFilename());
    out.print("\", \"");
    out.printJavaString(srcName);
    out.println("\");");

    Iterator<LineMap.Line> iter = _lineMap.iterator();
    String lastSrcFilename = srcName;
    while (iter.hasNext()) {
      LineMap.Line line = iter.next();

      if (lastSrcFilename != null &&
          lastSrcFilename.equals(line.getSourceFilename())) {
        out.println("_caucho_line_map.add(" + line.getSourceLine() + ", " +
                line.getDestLine() + ");");
      } else {
        out.print("_caucho_line_map.add(\"");
        out.printJavaString(line.getSourceFilename());
        out.println("\", " + line.getSourceLine() + ", " +
                line.getDestLine() + ");");
        lastSrcFilename = line.getSourceFilename();
      }
    }
    */

    out.println("com.caucho.vfs.Depend depend;");
    
    Path appDir = getAppDir();
    for (int i = 0; i < depends.size(); i++) {
      PersistentDependency dependency = depends.get(i);

      if (dependency instanceof Depend) {
        Depend depend = (Depend) dependency;
	
	if (depend.getPath().isDirectory())
	  continue;
	
        out.print("depend = new com.caucho.vfs.Depend(");
        printPathDir(out, depend.getPath().getFullPath(), appDir, classPath);
        out.println(", " + depend.getDigest() + "L, " +
                    _requireSource + ");");
        out.println("com.caucho.jsp.JavaPage.addDepend(_caucho_depends, depend);");
      }
      else {
        out.print("com.caucho.jsp.JavaPage.addDepend(_caucho_depends, ");
        out.print(dependency.getJavaCreateString());
        out.println(");");
      }
    }

    if (_isCacheable && ! _isUncacheable) {
      for (int i = 0; i < _cacheDepends.size(); i++) {
        Depend depend = _cacheDepends.get(i);

	if (depend.getPath().isDirectory())
	  continue;

        out.print("depend = new com.caucho.vfs.Depend(");
        printPathDir(out, depend.getPath().getFullPath(), appDir, classPath);
        out.println(", \"" + depend.getDigest() + "\", " +
                    _requireSource + ");");
        out.println("_caucho_cacheDepends.add((Object) depend);");
      }
    }
    out.popDepth();
    out.println("}");
  }

  /**
   * Prints an expression to lookup the path directory
   */
  private void printPathDir(JspJavaWriter out, String path,
                            Path appDir, MergePath classPath)
    throws IOException
  {
    String resinHome = CauchoSystem.getResinHome().getFullPath();

    String prefix = getAppDir().getFullPath();
    
    if (prefix.length() > 1 && ! prefix.endsWith("/"))
      prefix = prefix + "/";

    if (path.startsWith(prefix)) {
      path = path.substring(prefix.length());
      out.print("appDir.lookup(\"");
      out.printJavaString(path);
      out.print("\")");
      return;
    }

    ArrayList<Path> classPathList = classPath.getMergePaths();
    
    for (int i = 0; i < classPathList.size(); i++) {
      Path dir = classPathList.get(i);
      prefix = dir.getFullPath();

      if (! prefix.endsWith("/"))
	prefix = prefix + "/";

      if (path.startsWith(prefix)) {
        String tail = path.substring(prefix.length());

	if (dir.lookup(tail).canRead() &&
	    classPath.lookup(tail).equals(dir.lookup(tail))) {
	  out.print("mergePath.lookup(\"");
	  out.printJavaString(tail);
	  out.print("\")");
	  return;
	}
      }
    }
      
    if (path.startsWith(resinHome + "/")) {
      path = path.substring(resinHome.length() + 1);
      out.print("mergePath.lookup(\"");
      out.printJavaString(path);
      out.print("\")");
    }
    /* XXX: why?  This messes up tests like server/0526
    else if (path.startsWith("/")) {
      out.print("mergePath.lookup(\"file:");
      out.printJavaString(path);
      out.print("\")");
    }
    */
    /*
    else if (classPath.lookup("./" + path).canRead()) {
      out.print("mergePath.lookup(\"./");
      out.printJavaString(path);
      out.print("\")");
    }
    */
    else {
      out.print("mergePath.lookup(\"");
      out.printJavaString(path);
      out.print("\")");
    }
  }

  private void generateConstantStrings(JspJavaWriter out)
    throws IOException
  {
    if (_strings.size() == 0)
      return;

    out.println();
    Iterator iter = _strings.iterator();
    while (iter.hasNext()) {
      Object key = iter.next();
      int j = _strings.get(key);

      if (_ideHack)
        out.println("private final char []_jsp_string" + j + ";");
      else
        out.println("private final static char []_jsp_string" + j + ";");
    }

    if (_ideHack) {
      out.println("private void _jsp_init_strings() {");
      out.pushDepth();
    }
    else {
      out.println("static {");
      out.pushDepth();
    }
    
    String enc = out.getWriteStream().getJavaEncoding();
    if (enc == null || enc.equals("ISO8859_1"))
      enc = null;

    if (_config.isStaticEncoding() && enc != null) {
      out.println("try {");
      out.pushDepth();
    }

    iter = _strings.iterator();
    while (iter.hasNext()) {
      String text = (String) iter.next();
      int j = _strings.get(text);

      out.print("_jsp_string" + j + " = \"");
      
      for (int i = 0; i < text.length(); i++) {
	char ch = text.charAt(i);
	switch (ch) {
	case '\n':
	  out.print("\\n");
	  break;
	case '\r':
	  out.print("\\r");
	  break;
	case '"':
	  out.print("\\\"");
	  break;
	case '\\':
	  out.print("\\\\");
	  break;
	default:
	  out.print(ch);
	}
      }

      out.println("\".toCharArray();");
    }
    if (_config.isStaticEncoding() && enc != null) {
      out.popDepth();
      out.println("} catch (java.io.UnsupportedEncodingException e) {");
      out.println("  e.printStackTrace();");
      out.println("}");
    }
    out.popDepth();
    out.println("}");
  }

  /**
   * Opens a write stream to the *.java file we're generating.
   *
   * @param path work directory path
   *
   * @return the write stream
   */
  WriteStream openWriteStream()
    throws IOException
  {
    Path javaPath = getGeneratedPath();
    
    WriteStream os = javaPath.openWrite();

    os.setEncoding("JAVA");
    
    return os;
  }

  Path getGeneratedPath()
    throws IOException
  {
    String name = _pkg + "." + _className;

    Path dir = getJspCompiler().getClassDir().lookup(_workPath);
    Path javaPath = dir.lookup(_className + ".java");
    
    try {
      javaPath.getParent().mkdirs();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    return javaPath;
  }
  
  public int uniqueId()
  {
    return _uniqueId++;
  }
  
  public int generateJspId()
  {
    return _jspId++;
  }

  protected void addImport(String name)
  {
  }

  boolean hasTags()
  {
    // size() == 1 is the jsp: tags.
    return _tagManager.hasTags();
  }

  /**
   * Returns the tag with the given qname.
   */
  public TagInfo getTag(QName qname)
    throws JspParseException
  {
    return _tagManager.getTag(qname);
  }

  /**
   * Returns the tag with the given qname.
   */
  public Class getTagClass(QName qname)
    throws Exception
  {
    return _tagManager.getTagClass(qname);
  }

  public Taglib addTaglib(QName qname)
    throws JspParseException
  {
    return _tagManager.addTaglib(qname);
  }

  public JspParseException error(String message)
  {
    JspParseException e = new JspParseException(message);
    e.setErrorPage(_parseState.getErrorPage());

    return e;
  }

  public JspParseException error(Exception e)
  {
    JspParseException exn = new JspParseException(e);
    
    exn.setErrorPage(_parseState.getErrorPage());

    return exn;
  }

  static class MethodExpr {
    com.caucho.el.Expr _expr;
    Class []_args;

    MethodExpr(com.caucho.el.Expr expr, Class []args)
    {
      _expr = expr;
      _args = args;
    }

    com.caucho.el.Expr getExpr()
    {
      return _expr;
    }

    Class []getArgs()
    {
      return _args;
    }
  }

  static {
    _primitives = new HashMap<String,String>();
    _primitives.put("boolean", "boolean");
    _primitives.put("byte", "byte");
    _primitives.put("short", "short");
    _primitives.put("char", "char");
    _primitives.put("int", "int");
    _primitives.put("long", "long");
    _primitives.put("float", "float");
    _primitives.put("double", "double");
    
    _primitiveClasses = new HashMap<String,Class>();
    _primitiveClasses.put("boolean", boolean.class);
    _primitiveClasses.put("byte", byte.class);
    _primitiveClasses.put("short", short.class);
    _primitiveClasses.put("char", char.class);
    _primitiveClasses.put("int", int.class);
    _primitiveClasses.put("long", long.class);
    _primitiveClasses.put("float", float.class);
    _primitiveClasses.put("double", double.class);
  }
}
