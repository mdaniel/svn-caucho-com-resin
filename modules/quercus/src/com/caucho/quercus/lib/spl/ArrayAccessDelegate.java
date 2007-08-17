package com.caucho.quercus.lib.spl;

import com.caucho.quercus.env.AbstractDelegate;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;

/**
 * A delegate that intercepts array acces methods on the
 * target objects that implement
 * the {@link com.caucho.quercus.lib.spl.ArrayAccess} interface.
 */
public class ArrayAccessDelegate
  extends AbstractDelegate
{
  @Override
  public boolean offsetExists(Env env, Value obj, Value offset)
  {
    return obj.findFunction("offsetExists").callMethod(env, obj, offset).toBoolean();
  }

  @Override
  public Value offsetGet(Env env, Value obj, Value offset)
  {
    return obj.findFunction("offsetGet").callMethod(env, obj, offset);
  }

  @Override
  public Value offsetSet(Env env, Value obj, Value offset, Value value)
  {
    return obj.findFunction("offsetSet").callMethod(env, obj, offset, value);
  }

  @Override
  public Value offsetUnset(Env env, Value obj, Value offset)
  {
    return obj.findFunction("offsetUnset").callMethod(env, obj, offset);
  }
}
