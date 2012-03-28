/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.statement;

import com.caucho.quercus.Location;
import com.caucho.quercus.expr.ExprPro;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.statement.ForStatement;
import com.caucho.quercus.statement.Statement;

import java.io.IOException;

/**
 * Represents a for statement.
 */
public class ProForStatement extends ForStatement
  implements CompilingStatement
{
  public ProForStatement(Location location,
                         Expr init,
                         Expr test,
                         Expr incr,
                         Statement block,
                         String label)
  {
    super(location, init, test, incr, block, label);
  }

  public StatementGenerator getGenerator()
  {
    return GENERATOR;
  }

  private StatementGenerator GENERATOR = new StatementGenerator() {
      protected Location getLocation()
      {
        return ProForStatement.this.getLocation();
      }
      
      /**
       * Analyze the statement
       */
      public boolean analyze(AnalyzeInfo info)
      {
        ExprPro init = (ExprPro) _init;
        
        if (init != null)
          init.getGenerator().analyzeTop(info);

        ExprPro test = (ExprPro) _test;
        if (test != null)
          test.getGenerator().analyze(info);

        AnalyzeInfo contInfo = info.copy();
        AnalyzeInfo breakInfo = info;

        AnalyzeInfo loopInfo = info.createLoop(contInfo, breakInfo);

        CompilingStatement block = (CompilingStatement) _block;
        block.getGenerator().analyze(loopInfo);

        ExprPro incr = (ExprPro) _incr;
        if (incr != null)
          incr.getGenerator().analyzeTop(loopInfo);

        if (test != null)
          test.getGenerator().analyze(loopInfo);

        loopInfo.merge(contInfo);

        // handle loop values

        block.getGenerator().analyze(loopInfo);

        loopInfo.merge(contInfo);

        if (incr != null)
          incr.getGenerator().analyzeTop(loopInfo);

        if (test != null)
          test.getGenerator().analyze(loopInfo);

        info.merge(loopInfo);

        return true;
      }

      /**
       * Generates the Java code for the statement.
       *
       * @param out the writer to the generated Java source.
       */
      protected void generateImpl(PhpWriter out)
        throws IOException
      {
        //String loopLabel = out.createForLabel();
        
        //out.pushLoopLabel(loopLabel);

        out.println(_label + ":");
        
        out.print("for (");
        ExprPro init = (ExprPro) _init;
        if (init != null)
          init.getGenerator().generateTop(out);

        out.println(";");
        out.print("     ");

        ExprPro test = (ExprPro) _test;
        if (test != null)
          test.getGenerator().generateBoolean(out);
        else
          out.print("BooleanValue.TRUE.toBoolean()");

        out.println(";");
        out.print("     ");

        ExprPro incr = (ExprPro) _incr;
        if (incr != null)
          incr.getGenerator().generateTop(out);

        out.println(") {");
        out.pushDepth();
        out.println("env.checkTimeout();");

        CompilingStatement block = (CompilingStatement) _block;
        block.getGenerator().generate(out);
        out.popDepth();
        out.println("}");
        
        //out.popLoopLabel();
      }
    };
}

