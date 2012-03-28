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
import com.caucho.quercus.parser.QuercusParser;
import com.caucho.quercus.statement.ContinueStatement;
import com.caucho.quercus.statement.Statement;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Represents a continue expression statement in a PHP program.
 */
public class ProContinueStatement extends ContinueStatement
  implements CompilingStatement
{
  //public static final ContinueStatement CONTINUE = new ProContinueStatement();

  public ProContinueStatement(Location location,
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
        
	info.mergeLoopContinueInfo();
	
	return false;
      }

      /**
       * Returns true if control can go past the statement.
       */
      @Override
      public int fallThrough()
      {
	return RETURN; // XXX: php/3650
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
          out.println("env.error(\"No loop/switch statement to continue\");");
        }
        else if (getTarget() == null) {
          String parentLabel = _loopLabelList.get(_loopLabelList.size() - 1);
          
          if (QuercusParser.isSwitchLabel(parentLabel))
            out.println("if (true) break " + parentLabel + ";");
          else
            out.println("if (true) continue;");
        }
        else {
          out.print("switch (");
          getTarget().generateInt(out);
          out.println(") {");
          
          out.pushDepth();
          
          int labels = _loopLabelList.size();
          
          String parentLabel = _loopLabelList.get(labels - 1);
          
          for (int i = labels; i > 0; i--) {
            String label = _loopLabelList.get(labels - i);
            
            if (QuercusParser.isSwitchLabel(label))
              out.println("case " + i + ": break " + label + ";");
            else
              out.println("case " + i + ": continue " + label + ";");
          }
          
          if (parentLabel != null) {
            if (QuercusParser.isSwitchLabel(parentLabel))
              out.println("default: if (true) break " + parentLabel + ";");
            else
              out.println("default: if (true) continue " + parentLabel + ";");
          }

          out.popDepth();
          out.println("}");
        }
      }
    };
}

