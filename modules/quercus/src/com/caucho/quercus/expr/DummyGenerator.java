/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Represents compilable PHP expression.
 */
public class DummyGenerator extends ExprGenerator {
  /**
   * Analyze the expression
   */
  public ExprType analyze(AnalyzeInfo info)
  {
    return ExprType.VALUE;
  }
}

