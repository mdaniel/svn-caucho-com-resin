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

package com.caucho.jsp;

import com.caucho.bytecode.ByteCodeParser;
import com.caucho.bytecode.CodeAttribute;
import com.caucho.bytecode.CodeVisitor;
import com.caucho.bytecode.JavaClass;
import com.caucho.bytecode.JavaMethod;
import com.caucho.log.Log;
import com.caucho.util.L10N;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.IterationTag;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.TryCatchFinally;
import javax.xml.ws.WebServiceRef;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Analyzes the class for tag.
 *
 * Resin performs optimizations in the java code it produces from a jsp
 * depending on the nature of the taglib's that are used. For example, if a
 * taglib class does not use doAfterBody() then Resin can optimize the code it
 * produces for the jsp that uses that tag.
 *
 * In order to determine the nature of a certain tag, and thus the
 * optimizations that can be performed, Resin analyzes the tag's class.
 * It does this in two stages: first it uses reflection to look at the class
 * and then it uses bytecode analysis to look at the class.
 *
 * @see com.caucho.jsp.AnalyzedTag
 */
public class TagAnalyzer {
  private static final Logger log = Log.open(TagAnalyzer.class);
  static final L10N L = new L10N(TagAnalyzer.class);

  private HashMap<Class,AnalyzedTag> _analyzedTags =
    new HashMap<Class,AnalyzedTag>();

  /**
   * Analyzes a tag.
   */
  public AnalyzedTag analyze(Class tagClass)
  {
    if (tagClass == null)
      return null;
    AnalyzedTag analyzedTag = _analyzedTags.get(tagClass);
    if (analyzedTag != null)
      return analyzedTag;

    if (! Tag.class.isAssignableFrom(tagClass))
      return null;

    if (tagClass.isInterface())
      return null;

    AnalyzedTag parent = analyze(tagClass.getSuperclass());
    
    String name = tagClass.getName().replace('.', '/') + ".class";
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    AnalyzedTag tag = new AnalyzedTag();

    try {
      analyzeByReflection(tagClass, tag, parent);

      InputStream is = loader.getResourceAsStream(name);

      if (is == null)
	return tag;

      try {
	JavaClass javaClass = new ByteCodeParser().parse(is);

	analyze(javaClass, "doStartTag", "()I", new StartAnalyzer(), tag);
	analyze(javaClass, "doEndTag", "()I", new EndAnalyzer(), tag);

	if (IterationTag.class.isAssignableFrom(tagClass)) {
	  analyze(javaClass, "doAfterBody", "()I", new AfterAnalyzer(), tag);
	}
	
	if (BodyTag.class.isAssignableFrom(tagClass)) {
	  analyze(javaClass, "doInitBody", "()V",
		  new InitAnalyzer(), tag);
	}
	
	if (TryCatchFinally.class.isAssignableFrom(tagClass)) {
	  analyze(javaClass, "doCatch", "(Ljava/lang/Throwable;)V",
		  new CatchAnalyzer(), tag);
	  analyze(javaClass, "doFinally", "()V",
		  new FinallyAnalyzer(), tag);
	}
      } finally {
	is.close();
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    return tag;
  }

  /**
   * Analyzes the tag by reflection.
   */
  public void analyzeByReflection(Class tagClass,
				  AnalyzedTag tag, AnalyzedTag parent)
  {
    tag.setBodyTag(BodyTag.class.isAssignableFrom(tagClass));
		   
    Method doStartMethod = getMethod(tagClass, "doStartTag", new Class[0]);

    if (doStartMethod != null &&
	doStartMethod.getDeclaringClass().equals(tagClass)) {
      if (TagSupport.class.equals(tagClass)) {
	tag.setDoStart(false);
	tag.setStartReturnsSkip(false);
	tag.setStartReturnsInclude(true);
	tag.setStartReturnsBuffered(false);
      }
      else if (BodyTagSupport.class.equals(tagClass)) {
	tag.setDoStart(false);
	tag.setStartReturnsSkip(false);
	tag.setStartReturnsInclude(false);
	tag.setStartReturnsBuffered(true);
      }
      else if (BodyTag.class.isAssignableFrom(tagClass)) {
	tag.setDoStart(true);
	tag.setStartReturnsSkip(true);
	tag.setStartReturnsInclude(true);
	tag.setStartReturnsBuffered(true);
      }
      else {
	tag.setDoStart(true);
	tag.setStartReturnsSkip(true);
	tag.setStartReturnsInclude(true);
	tag.setStartReturnsBuffered(false);
      }
    }
    else if (parent != null) {
      tag.setDoStart(parent.getDoStart());
      tag.setStartReturnsSkip(parent.getStartReturnsSkip());
      tag.setStartReturnsInclude(parent.getStartReturnsInclude());
      tag.setStartReturnsBuffered(parent.getStartReturnsBufferedAsParent());
    }
    
    Method doEndMethod = getMethod(tagClass, "doEndTag", new Class[0]);

    if (doEndMethod != null &&
	doEndMethod.getDeclaringClass().equals(tagClass)) {
      if (TagSupport.class.equals(tagClass) ||
	  BodyTagSupport.class.equals(tagClass)) {
	tag.setDoEnd(false);
	tag.setEndReturnsSkip(false);
	tag.setEndReturnsEval(true);
      }
      else {
	tag.setDoEnd(true);
	tag.setEndReturnsSkip(true);
	tag.setEndReturnsEval(true);
      }
    }
    else if (parent != null) {
      tag.setDoEnd(parent.getDoEnd());
      tag.setEndReturnsSkip(parent.getEndReturnsSkip());
      tag.setEndReturnsEval(parent.getEndReturnsEval());
    }
    
    Method doAfterBody = getMethod(tagClass, "doAfterBody", new Class[0]);

    if (doAfterBody != null &&
	doAfterBody.getDeclaringClass().equals(tagClass)) {
      if (TagSupport.class.equals(tagClass) ||
	  BodyTagSupport.class.equals(tagClass)) {
	tag.setDoAfter(false);
	tag.setAfterReturnsAgain(false);
      }
      else if (! IterationTag.class.isAssignableFrom(tagClass)) {
	tag.setDoAfter(false);
	tag.setAfterReturnsAgain(false);
      }
      else {
	tag.setDoAfter(true);
	tag.setAfterReturnsAgain(true);
      }
    }
    else if (parent != null) {
      tag.setDoAfter(parent.getDoAfter());
      tag.setAfterReturnsAgain(parent.getAfterReturnsAgain());
    }
    
    Method doInitBody = getMethod(tagClass, "doInitBody", new Class[0]);

    if (doInitBody != null &&
	doInitBody.getDeclaringClass().equals(tagClass)) {
      if (BodyTagSupport.class.equals(tagClass)) {
	tag.setDoInit(false);
      }
      else if (! BodyTag.class.isAssignableFrom(tagClass)) {
	tag.setDoInit(false);
      }
      else {
	tag.setDoInit(true);
      }
    }
    else if (parent != null) {
      tag.setDoInit(parent.getDoInit());
    }
    
    Method doCatch = getMethod(tagClass, "doCatch",
			       new Class[] { Throwable.class });

    if (doCatch != null &&
	doCatch.getDeclaringClass().equals(tagClass)) {
      if (! TryCatchFinally.class.isAssignableFrom(tagClass)) {
	tag.setDoCatch(false);
      }
      else {
	tag.setDoCatch(true);
      }
    }
    else if (parent != null) {
      tag.setDoCatch(parent.getDoCatch());
    }
    
    Method doFinally = getMethod(tagClass, "doFinally", new Class[0]);

    if (doFinally != null &&
	doFinally.getDeclaringClass().equals(tagClass)) {
      if (! TryCatchFinally.class.isAssignableFrom(tagClass)) {
	tag.setDoFinally(false);
      }
      else {
	tag.setDoFinally(true);
      }
    }
    else if (parent != null) {
      tag.setDoFinally(parent.getDoFinally());
    }

    // check for @Resource injection
    for (Method method : tagClass.getDeclaredMethods()) {
      if (method.getName().startsWith("set")
	  && (method.isAnnotationPresent(Resource.class)
	      || method.isAnnotationPresent(EJB.class)
	      || method.isAnnotationPresent(WebServiceRef.class))) {
	tag.setHasInjection(true);
      }
    }
    
    for (Field field : tagClass.getDeclaredFields()) {
      if (field.isAnnotationPresent(Resource.class)
	  || field.isAnnotationPresent(EJB.class)
	  || field.isAnnotationPresent(WebServiceRef.class)) {
	tag.setHasInjection(true);
      }
    }
  }

  private Method getMethod(Class tagClass, String name, Class []args)
  {
    try {
      return tagClass.getMethod(name, args);
    } catch (Throwable e) {
      return null;
    }
  }

  /**
   * Analyzes the code for a method
   */
  private void analyze(JavaClass javaClass,
		       String name,
		       String signature,
		       Analyzer analyzer,
		       AnalyzedTag tag)
  {
    JavaMethod method = javaClass.findMethod(name, signature);
    if (method == null)
      return;
    
    CodeAttribute codeAttribute = method.getCode();

    if (codeAttribute == null)
      return;

    CodeVisitor visitor = new CodeVisitor(javaClass, codeAttribute);
    try {
      visitor.analyze(analyzer);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    analyzer.complete(tag);
  }

  static class Analyzer extends com.caucho.bytecode.Analyzer {
    public void analyze(CodeVisitor visitor)
    {
    }
    
    public void complete(AnalyzedTag tag)
    {
    }
  }
  
  /**
   * Callback analyzing the doStartTag method.
   */
  static class StartAnalyzer extends Analyzer {
    private boolean _hasSkip;
    private boolean _hasInclude;
    private boolean _hasBuffered;
    private boolean _hasStart;

    private int _count = 0;
    private int _value = -1;

    public void analyze(CodeVisitor visitor)
    {
      int count = _count++;
      
      switch (visitor.getOpcode()) {
      case CodeVisitor.IRETURN:
	if (count != 1)
	  _hasStart = true;

	if (_value == Tag.SKIP_BODY)
	  _hasSkip = true;
	else if (_value == Tag.EVAL_BODY_INCLUDE)
	  _hasInclude = true;
	else if (_value == BodyTag.EVAL_BODY_BUFFERED)
	  _hasBuffered = true;
	else {
	  _hasSkip = true;
	  _hasInclude = true;
	  _hasBuffered = true;
	}
	break;

      case CodeVisitor.ICONST_M1:
      case CodeVisitor.ICONST_0:
      case CodeVisitor.ICONST_1:
      case CodeVisitor.ICONST_2:
      case CodeVisitor.ICONST_3:
      case CodeVisitor.ICONST_4:
      case CodeVisitor.ICONST_5:
	if (count != 0)
	  _hasStart = true;
	    
	_value = visitor.getOpcode() - CodeVisitor.ICONST_0;
	break;

      case CodeVisitor.BIPUSH:
	if (count != 0)
	  _hasStart = true;
	_value = visitor.getByteArg();
	break;

      case CodeVisitor.SIPUSH:
	if (count != 0)
	  _hasStart = true;
	_value = visitor.getShortArg();
	break;
	  
      default:
	_hasStart = true;
	_value = -1;
	break;
      }
    }

    public void complete(AnalyzedTag tag)
    {
      tag.setDoStart(_hasStart);
      tag.setStartReturnsSkip(_hasSkip);
      tag.setStartReturnsInclude(_hasInclude);
      tag.setStartReturnsBuffered(_hasBuffered);
    }
  }
  
  /**
   * Callback analyzing the doEndTag method.
   */
  static class EndAnalyzer extends Analyzer {
    private boolean _hasSkip;
    private boolean _hasEval;
    private boolean _hasEnd;

    private int _count = 0;
    private int _value = -1;

    public void analyze(CodeVisitor visitor)
    {
      int count = _count++;
      
      switch (visitor.getOpcode()) {
      case CodeVisitor.IRETURN:
	if (count != 1)
	  _hasEnd = true;

	if (_value == Tag.SKIP_PAGE)
	  _hasSkip = true;
	else if (_value == Tag.EVAL_PAGE)
	  _hasEval = true;
	else {
	  _hasEnd = true;
	  _hasSkip = true;
	  _hasEval = true;
	}
	break;

      case CodeVisitor.ICONST_M1:
      case CodeVisitor.ICONST_0:
      case CodeVisitor.ICONST_1:
      case CodeVisitor.ICONST_2:
      case CodeVisitor.ICONST_3:
      case CodeVisitor.ICONST_4:
      case CodeVisitor.ICONST_5:
	if (count != 0)
	  _hasEnd = true;
	    
	_value = visitor.getOpcode() - CodeVisitor.ICONST_0;
	break;

      case CodeVisitor.BIPUSH:
	if (count != 0)
	  _hasEnd = true;
	_value = visitor.getByteArg();
	break;

      case CodeVisitor.SIPUSH:
	if (count != 0)
	  _hasEnd = true;
	_value = visitor.getShortArg();
	break;

      default:
	_hasEnd = true;
	_value = -1;
	break;
      }
    }

    public void complete(AnalyzedTag tag)
    {
      tag.setDoEnd(_hasEnd);
      tag.setEndReturnsSkip(_hasSkip);
      tag.setEndReturnsEval(_hasEval);
    }
  }

  /**
   * Callback analyzing the doAfterBody method.
   */
  static class AfterAnalyzer extends Analyzer {
    private boolean _hasAfter;
    private boolean _hasAgain;

    private int _count = 0;
    private int _value = -1;

    public void analyze(CodeVisitor visitor)
    {
      int count = _count++;
      
      switch (visitor.getOpcode()) {
      case CodeVisitor.IRETURN:
	if (count != 1)
	  _hasAfter = true;

	if (_value == IterationTag.EVAL_BODY_AGAIN)
	  _hasAgain = true;
	else if (_value == IterationTag.SKIP_BODY) {
	}
	else {
	  _hasAgain = true;
	}
	break;

      case CodeVisitor.ICONST_M1:
      case CodeVisitor.ICONST_0:
      case CodeVisitor.ICONST_1:
      case CodeVisitor.ICONST_2:
      case CodeVisitor.ICONST_3:
      case CodeVisitor.ICONST_4:
      case CodeVisitor.ICONST_5:
	if (count != 0)
	  _hasAfter = true;
	    
	_value = visitor.getOpcode() - CodeVisitor.ICONST_0;
	break;

      case CodeVisitor.BIPUSH:
	if (count != 0)
	  _hasAfter = true;
	_value = visitor.getByteArg();
	break;

      case CodeVisitor.SIPUSH:
	if (count != 0)
	  _hasAfter = true;
	_value = visitor.getShortArg();
	break;

      default:
	_hasAfter = true;
	_value = -1;
	break;
      }
    }

    public void complete(AnalyzedTag tag)
    {
      tag.setDoAfter(_hasAfter);
      tag.setAfterReturnsAgain(_hasAgain);
    }
  }

  /**
   * Callback analyzing the doInitBody method.
   */
  static class InitAnalyzer extends Analyzer {
    private boolean _hasCode;

    private int _count = 0;

    public void analyze(CodeVisitor visitor)
    {
      int count = _count++;
      
      switch (visitor.getOpcode()) {
      case CodeVisitor.RETURN:
	if (count != 0)
	  _hasCode = true;
	break;

      default:
	_hasCode = true;
	break;
      }
    }

    public void complete(AnalyzedTag tag)
    {
      tag.setDoInit(_hasCode);
    }
  }

  /**
   * Callback analyzing the doCatch method.
   */
  static class CatchAnalyzer extends Analyzer {
    private boolean _hasCode;

    private int _count = 0;

    public void analyze(CodeVisitor visitor)
    {
      int count = _count++;
      
      switch (visitor.getOpcode()) {
      case CodeVisitor.RETURN:
	if (count != 0)
	  _hasCode = true;
	break;

      default:
	_hasCode = true;
	break;
      }
    }

    public void complete(AnalyzedTag tag)
    {
      tag.setDoCatch(_hasCode);
    }
  }

  /**
   * Callback analyzing the doFinally method.
   */
  static class FinallyAnalyzer extends Analyzer {
    private boolean _hasCode;

    private int _count = 0;

    public void analyze(CodeVisitor visitor)
    {
      int count = _count++;
      
      switch (visitor.getOpcode()) {
      case CodeVisitor.RETURN:
	if (count != 0)
	  _hasCode = true;
	break;

      default:
	_hasCode = true;
	break;
      }
    }

    public void complete(AnalyzedTag tag)
    {
      tag.setDoFinally(_hasCode);
    }
  }
}
