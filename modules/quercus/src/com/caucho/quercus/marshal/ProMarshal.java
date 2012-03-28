/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.marshal;

import com.caucho.bytecode.CodeWriterAttribute;
import com.caucho.quercus.expr.ExprGenerator;
import com.caucho.quercus.gen.PhpWriter;

import java.io.IOException;

/**
 * Code for marshalling (PHP to Java) and unmarshalling (Java to PHP)
 * arguments.
 */
public interface ProMarshal
{
  public void generate(PhpWriter out,
				       ExprGenerator expr,
				       Class argClass)
    throws IOException;

  public void generateResultStart(PhpWriter out)
    throws IOException;

  public void generateResultEnd(PhpWriter out)
    throws IOException;

  public boolean isByteCodeGenerator();

  public void generateMarshal(CodeWriterAttribute code, int argIndex);

  public void generateUnmarshal(CodeWriterAttribute code);
}

