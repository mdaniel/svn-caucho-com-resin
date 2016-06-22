package com.caucho.quercus.lib.simplexml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import com.caucho.quercus.annotation.Name;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.ReturnNullAsFalse;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.JavaValue;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.ObjectExtJavaValue;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.util.IoUtil;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

public abstract class SimpleXMLNode
{
  private static final Logger log
    = Logger.getLogger(SimpleXMLNode.class.getName());

  private static final L10N L = new L10N(SimpleXMLNode.class);

  protected final QuercusClass _cls;
  protected final SimpleView _view;

  private SimpleNamespaceContext _xpathNamespaceContext;

  public SimpleXMLNode(QuercusClass cls, SimpleView view)
  {
    _cls = cls;
    _view = view;
  }

  /**
   * public string getName()
   */
  @Name("getName")
  public String simplexml_getName()
  {
    return _view.getNodeName();
  }

  /**
   * public string __toString()
   */
  public String __toString(Env env)
  {
    return _view.toString(env);
  }

  /**
   * Implementation for getting the fields of this class.
   * i.e. <code>$a->foo</code>
   */
  public Value __getField(Env env, StringValue name)
  {
    SimpleView view = _view.getField(env, name);

    if (view == null) {
      return NullValue.NULL;
    }

    SimpleXMLElement e = new SimpleXMLElement(_cls, view);

    return e.wrapJava(env);
  }

  /**
   * Implementation for setting the fields of this class.
   * i.e. <code>$a->foo = "hello"</code>
   */
  public void __setField(Env env, StringValue name, Value value)
  {
    SimpleView view = _view.setField(env, name, value);
  }

  /**
   * Implementation for getting the indices of this class.
   * i.e. <code>$a->foo[0]</code>
   */
  public Value __get(Env env, Value indexV)
  {
    SimpleView view = _view.getIndex(env, indexV);

    if (view == null) {
      return NullValue.NULL;
    }

    SimpleXMLElement e = new SimpleXMLElement(_cls, view);

    return e.wrapJava(env);
  }

  /**
   * Implementation for setting the indices of this class.
   * i.e. <code>$a->foo[0] = "hello"</code>
   */
  public void __set(Env env, StringValue nameV, StringValue value)
  {
    _view.setIndex(env, nameV, value);
  }

  public int __count(Env env)
  {
    return _view.getCount();
  }

  /**
   * public SimpleXMLElement addChild( string $name [, string $value [, string $namespace ]] )
   */
  public SimpleXMLElement addChild(Env env,
                                   String name,
                                   @Optional String value,
                                   @Optional String namespace)
  {
    SimpleView view = _view.addChild(env, name, value, namespace);

    SimpleXMLElement e = new SimpleXMLElement(_cls, view);

    return e;
  }

  /**
   * public void SimpleXMLElement::addAttribute ( string $name [, string $value [, string $namespace ]] )
   */
  public void addAttribute(Env env,
                           String name,
                           @Optional String value,
                           @Optional String namespace)
  {
    if (namespace != null) {
      if (namespace.length() == 0) {
        namespace = null;
      }
      else if (name.indexOf(':') <= 0) {
        env.warning(L.l("Adding attributes with namespaces requires attribute name with a prefix"));

        return;
      }
    }

    _view.addAttribute(env, name, value, namespace);
  }

  /**
   * public mixed SimpleXMLElement::asXML([ string $filename ])
   */
  public final Value asXML(Env env, @Optional Value filename)
  {
    StringBuilder sb = new StringBuilder();
    if (! _view.toXml(env, sb)) {
      return BooleanValue.FALSE;
    }

    String encoding = _view.getEncoding();

    if (filename.isDefault()) {
      StringValue value = env.createStringBuilder();

      if (env.isUnicodeSemantics()) {
        value.append(sb.toString());
      }
      else {
        byte []bytes;

        try {
          bytes = sb.toString().getBytes(encoding);
        }
        catch (UnsupportedEncodingException e) {
          log.log(Level.FINE, e.getMessage(), e);
          env.warning(e);

          return BooleanValue.FALSE;
        }

        value.append(bytes);
      }

      return value;
    }
    else {
      Path path = env.lookupPwd(filename);

      OutputStream os = null;

      try {
        os = path.openWrite();

        byte []bytes = sb.toString().getBytes(encoding);
        os.write(bytes);

        return BooleanValue.TRUE;
      }
      catch (IOException e) {
        log.log(Level.FINE, e.getMessage(), e);
        env.warning(e);

        return BooleanValue.FALSE;
      }
      finally {
        if (os != null) {
          IoUtil.close(os);
        }
      }
    }
  }

  /**
   * public SimpleXMLElement SimpleXMLElement::attributes([ string $ns = NULL [, bool $is_prefix = false ]])
   */
  public Value attributes(Env env,
                          @Optional Value namespaceV,
                          @Optional boolean isPrefix)
  {
    String namespace = null;
    if (! namespaceV.isNull()) {
      namespace = namespaceV.toString();

      if (namespace != null && namespace.length() == 0) {
        namespace = null;
      }
    }

    AttributeListView view = _view.getAttributes(namespace);

    SimpleXMLElement e = new SimpleXMLElement(getQuercusClass(), view);

    return e.wrapJava(env);
  }

  /**
   * public SimpleXMLElement SimpleXMLElement::children([ string $ns [, bool $is_prefix = false ]])
   */
  public Value children(Env env,
                        @Optional Value namespaceV,
                        @Optional boolean isPrefix)
  {
    String namespace = null;
    String prefix = null;

    if (! namespaceV.isNull()) {
      if (isPrefix) {
        prefix = namespaceV.toString();

        if (prefix != null && prefix.length() == 0) {
          prefix = null;
        }
      }
      else {
        namespace = namespaceV.toString();

        if (namespace != null && namespace.length() == 0) {
          namespace = null;
        }
      }
    }

    ChildrenView view = _view.getChildren(namespace, prefix);

    SimpleXMLElement e = new SimpleXMLElement(_cls, view);

    return e.wrapJava(env);
  }

  /**
   * public array SimpleXMLElement::getNamespaces ([ bool $recursive = false ] )
   */
  public ArrayValue getNamespaces(Env env, @Optional boolean isRecursive)
  {
    ArrayValue array = new ArrayValueImpl();

    HashMap<String,String> usedMap = _view.getNamespaces(isRecursive, false, true);

    for (Map.Entry<String,String> entry : usedMap.entrySet()) {
      StringValue key = env.createString(entry.getKey());
      StringValue value = env.createString(entry.getValue());

      array.append(key, value);
    }

    return array;
  }

  /**
    * public array SimpleXMLElement::getDocNamespaces ([ bool $recursive = false [, bool $from_root = true ]] )
    */
  public ArrayValue getDocNamespaces(Env env,
                                     @Optional boolean isRecursive,
                                     @Optional boolean isFromRoot)
  {
    ArrayValue array = new ArrayValueImpl();

    HashMap<String,String> usedMap = _view.getNamespaces(isRecursive, isFromRoot, false);

    for (Map.Entry<String,String> entry : usedMap.entrySet()) {
      StringValue key = env.createString(entry.getKey());
      StringValue value = env.createString(entry.getValue());

      array.append(key, value);
    }

    return array;
  }

  /**
   *  public bool SimpleXMLElement::registerXPathNamespace ( string $prefix , string $ns )
   */
  public boolean registerXPathNamespace(Env env, String prefix, String ns)
  {
    if (_xpathNamespaceContext == null) {
      _xpathNamespaceContext = new SimpleNamespaceContext();
    }

    _xpathNamespaceContext.addPrefix(prefix, ns);

    return true;
  }

  /**
   *  public array SimpleXMLElement::xpath( string $path )
   */
  public Value xpath(Env env, String expression)
  {
    if (_xpathNamespaceContext == null) {
      _xpathNamespaceContext = new SimpleNamespaceContext();
    }

    List<SimpleView> viewList = _view.xpath(env, _xpathNamespaceContext, expression);

    if (viewList == null) {
      return NullValue.NULL;
    }

    ArrayValue array = new ArrayValueImpl();

    for (SimpleView view : viewList) {
      SimpleXMLElement e = new SimpleXMLElement(_cls, view);

      Value value = e.wrapJava(env);

      array.append(value);
    }

    return array;
  }

  protected QuercusClass getQuercusClass()
  {
    return _cls;
  }

  protected Value wrapJava(Env env)
  {
    if (! "SimpleXMLElement".equals(_cls.getName())) {
      return new ObjectExtJavaValue(env, _cls, this, _cls.getJavaClassDef());
    }
    else {
      return new JavaValue(env, this, _cls.getJavaClassDef());
    }
  }
}
