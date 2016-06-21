/*
 * @author Immanuel Scheerer
 */

package com.caucho.quercus.lib.dom;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.ObjectValue;
import com.caucho.quercus.env.TraversableDelegate;
import com.caucho.quercus.env.Value;

import java.util.Iterator;
import java.util.Map;

public class DOMNamedNodeMapDelegate
  implements TraversableDelegate
{
  public DOMNamedNodeMapDelegate()
  {
  }

  public Iterator<Value> getKeyIterator(Env env, ObjectValue obj)
  {
    return new DOMNamedNodeMapKeyIterator(env, (DOMNamedNodeMap) obj.toJavaObject());
  }
  
  public Iterator<Value> getValueIterator(Env env, ObjectValue obj)
  {
    return new DOMNamedNodeMapValueIterator(env, (DOMNamedNodeMap) obj.toJavaObject());
  }
  
  public Iterator<Map.Entry<Value, Value>> getIterator(Env env, ObjectValue obj)
  {
    return new DOMNamedNodeMapIterator(env, (DOMNamedNodeMap) obj.toJavaObject());
  }
  
  public class DOMNamedNodeMapKeyIterator
    implements Iterator<Value>
  {
    private Env _env;
    private DOMNamedNodeMap _list;
    private int _index;

    public DOMNamedNodeMapKeyIterator(Env env, DOMNamedNodeMap list)
    {
      _env = env;
      _list = list;
    }
    
    public boolean hasNext()
    {
      return _index < _list.getLength();
    }

    public Value next()
    {
      return _env.createString(_list.item(_index++).getNodeName());
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
  
  public class DOMNamedNodeMapValueIterator
    implements Iterator<Value>
  {
    private Env _env;
    private DOMNamedNodeMap _list;
    private int _index;

    public DOMNamedNodeMapValueIterator(Env env, DOMNamedNodeMap list)
    {
      _env = env;
      _list = list;
    }
    
    public boolean hasNext()
    {
      return _index < _list.getLength();
    }

    public Value next()
    {
      return _env.wrapJava(_list.item(_index++));
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
  
  public class DOMNamedNodeMapIterator
    implements Iterator<Map.Entry<Value, Value>>
  {
    private Env _env;
    private DOMNamedNodeMap _list;
    private int _index;

    public DOMNamedNodeMapIterator(Env env, DOMNamedNodeMap list)
    {
      _env = env;
      _list = list;
    }
    
    public boolean hasNext()
    {
      return _index < _list.getLength();
    }

    public Map.Entry<Value, Value> next()
    {
      DOMNode<?> node = _list.item(_index++);
      return new DOMNamedNodeMapEntry(_env.createString(node.getNodeName()), _env.wrapJava(node));
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
  
  public class DOMNamedNodeMapEntry
    implements Map.Entry<Value,Value>
  {
    private Value _key;
    private Value _value;
    
    public DOMNamedNodeMapEntry(Value key, Value value)
    {
      _key = key;
      _value = value;
    }
    
    public Value getKey()
    {
      return _key;
    }
    
    public Value getValue()
    {
      return _value;
    }
    
    public Value setValue(Value value)
    {
      throw new UnsupportedOperationException();
    }
  }
}
