/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.program;

/**
 * Represents a compilable PHP function.
 */
public interface CompilingFunction {
  /**
   * Returns the function's generator.
   */
  public FunctionGenerator getGenerator();
}

