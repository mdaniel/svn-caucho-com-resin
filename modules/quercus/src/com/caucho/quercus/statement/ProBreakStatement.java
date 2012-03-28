/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.statement;

import com.caucho.quercus.Location;
import com.caucho.quercus.expr.ExprPro;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.ExprGenerator;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.statement.BreakStatement;
import com.caucho.quercus.statement.Statement;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Represents a break expression statement in a PHP program.
 */
public class ProBreakStatement extends BreakStatement
  implements CompilingStatement
{
  //public static final BreakStatement BREAK = new ProBreakStatement();
  
  public ProBreakStatement(Location location,
                           Expr target,
                           ArrayList<String> loopLabelList)
  {
    super(location, target, loopLabelList);
  }


  public StatementGenerator getGenerator()
  {
    return GENERATOR;
  }

  private StatementGenerator GENERATOR = new StatementGenerator() {
      protected Location getLocation()
      {
	return Location.UNKNOWN;
      }
      
      private ExprGenerator getTarget()
      {
        if (_target != null)
          return ((ExprPro) _target).getGenerator();
        else
          return null;
      }
      
      /**
       * Analyze the statement
       *
       * @return true if the following statement can be executed
       */
      public boolean analyze(AnalyzeInfo info)
      {
        if (getTarget() != null)
          getTarget().analyze(info);
        
	info.mergeLoopBreakInfo();

	// quercus/067i
	return true;
      }

      /**
       * Returns true if the statement can fallthrough.
       */
      @Override
      public int fallThrough()
      {
	return BREAK_FALL_THROUGH;
      }

      /**
       * Generates the Java code for the statement.
       *
       * @param out the writer to the generated Java source.
       */
      protected void generateImpl(PhpWriter out)
        throws IOException
      {
        if (_loopLabelList.size() == 0) {
          // need to send a php error instead of a compile error for buggy code
          // required for Wordpress OpenID plugin (and others)
          out.println("env.error(\"No loop/switch statement to break out of\");");
        }
        else if (getTarget() == null) {
          String label = _loopLabelList.get(_loopLabelList.size() - 1);
          
          out.println("if (true) break " + label + ";");
        }
        else {
          out.print("switch (");
          getTarget().generateInt(out);
          out.println(") {");
          
          out.pushDepth();
          
          int labels = _loopLabelList.size();
          
          String parentLoopLabel = _loopLabelList.get(labels - 1);
          
          for (int i = labels; i > 0; i--) {
            out.println("case " + i
                        + ": break " + _loopLabelList.get(labels - i) + ";");
          }
          
          if (parentLoopLabel != null) {
            out.println("default: if (true) break " + parentLoopLabel + ";");
          }

          out.popDepth();
          out.println("}");
        }
      }
    };
}

