package com.caucho.quercus.lib.spl;

import com.caucho.quercus.env.ArrayDelegate;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.UnsetValue;
import com.caucho.quercus.env.ObjectValue;

/**
 * A delegate that intercepts array acces methods on the
 * target objects that implement
 * the {@link com.caucho.quercus.lib.spl.ArrayAccess} interface.
 */
public class ArrayAccessDelegate
  extends ArrayDelegate
{
  @Override
  public Value get(Env env, ObjectValue obj, Value offset)
  {
    return obj.findFunction("offsetGet").callMethod(env, obj, offset);
  }

  @Override
  public Value put(Env env, ObjectValue obj, Value offset, Value value)
  {
    return obj.findFunction("offsetSet").callMethod(env, obj, offset, value);
  }

  @Override
  public Value put(Env env, ObjectValue obj, Value offset)
  {
    return obj.findFunction("offsetSet").callMethod(env, obj, UnsetValue.UNSET, offset);
  }

  @Override
  public Value remove(Env env, ObjectValue obj, Value offset)
  {
    return obj.findFunction("offsetUnset").callMethod(env, obj, offset);
  }
}
