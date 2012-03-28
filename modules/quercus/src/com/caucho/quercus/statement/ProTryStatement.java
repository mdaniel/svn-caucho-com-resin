/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.statement;

import com.caucho.quercus.Location;
import com.caucho.quercus.expr.ExprPro;
import com.caucho.quercus.expr.JavaCodeExprPro;
import com.caucho.quercus.expr.ExprGenerator;
import com.caucho.quercus.expr.DummyGenerator;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.statement.Statement;
import com.caucho.quercus.statement.TryStatement;

import java.io.IOException;

/**
 * Represents sequence of statements.
 */
public class ProTryStatement extends TryStatement
  implements CompilingStatement
{
  public ProTryStatement(Location location, Statement block)
  {
    super(location, block);
  }

  public StatementGenerator getGenerator()
  {
    return GENERATOR;
  }

  private StatementGenerator GENERATOR = new StatementGenerator() {
      protected Location getLocation()
      {
	return ProTryStatement.this.getLocation();
      }
      
      /**
       * Analyze the statement
       */
      public boolean analyze(AnalyzeInfo info)
      {
	AnalyzeInfo topInfo = info.copy();

	CompilingStatement block = (CompilingStatement) _block;
	block.getGenerator().analyze(info);

	for (int i = 0; i < _catchList.size(); i++) {
	  Catch item = _catchList.get(i);
      
	  topInfo.setUnknown();

	  ExprPro catchExpr = (ExprPro) item.getExpr();
	  CompilingStatement catchBlock = (CompilingStatement) item.getBlock();

	  ExprGenerator dummyValue = new DummyGenerator();
	  
	  catchExpr.getGenerator().analyzeAssign(topInfo, dummyValue);
	  catchBlock.getGenerator().analyze(topInfo);
	}

	info.setUnknown();

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
        // php/3g07 requires the "if (true)"
        out.println("if (true) try {");
        out.pushDepth();

        CompilingStatement block = (CompilingStatement) _block;
        block.getGenerator().generate(out);
    
        out.popDepth();

        int id = out.generateId();

        String e = "e" + id;
    
        out.println("} catch (QuercusLanguageException " + e + ") {");
        out.pushDepth();

        String v = "v" + id;
    
        out.println("Value " + v + " = " + e + ".getValue();");
        
        int i;
        for (i = 0; i < _catchList.size(); i++) {
          Catch item = _catchList.get(i);

          ExprPro catchExpr = (ExprPro) item.getExpr();
          CompilingStatement catchBlock = (CompilingStatement) item.getBlock();
          
          if (i != 0)
            out.print("else ");
          
          //XXX: move item.Id() part out?
          out.print("if (" + item.getId().equals("Exception") + " || "
                      + v + ".isA(\"");
          out.printJavaString(item.getId());
          out.println("\")) {");
          out.pushDepth();

          catchExpr.getGenerator().generateAssign(out,
                              new JavaCodeExprPro(v),
                              true);
          out.println(";");
          catchBlock.getGenerator().generate(out);
          
          out.popDepth();
          out.println("}");
        }
        
        if (i != 0) {
          out.println("else");
          out.println("  throw " + e + ";");
        }
        else
          out.println("throw " + e + ";");

        out.popDepth();
        
        Catch item = null;
        
        for (i = 0; i < _catchList.size(); i++) {
          if ("QuercusDieException".equals(_catchList.get(i).getId()))
            item = _catchList.get(i);
        }
    
        if (item != null) {
          out.println("} catch (QuercusDieException " + e + ") {");
          out.pushDepth();
          
          ExprPro catchExpr = (ExprPro) item.getExpr();
          CompilingStatement catchBlock = (CompilingStatement) item.getBlock();

          catchExpr.getGenerator().generateAssign(out,
                  new JavaCodeExprPro("env.createException(" + e + ")"),
                  true);

          out.println(";");
          catchBlock.getGenerator().generate(out);

          out.popDepth();
        }
        
        for (i = 0; i < _catchList.size(); i++) {
          if ("QuercusExitException".equals(_catchList.get(i).getId()))
            item = _catchList.get(i);
        }
    
        if (item != null) {
          out.println("} catch (QuercusExitException " + e + ") {");
          out.pushDepth();
          
          ExprPro catchExpr = (ExprPro) item.getExpr();
          CompilingStatement catchBlock = (CompilingStatement) item.getBlock();

          catchExpr.getGenerator().generateAssign(out,
                  new JavaCodeExprPro("env.createException(" + e + ")"),
                  true);

          out.println(";");
          catchBlock.getGenerator().generate(out);

          out.popDepth();
        }

        for (i = 0; i < _catchList.size(); i++) {
          if ("Exception".equals(_catchList.get(i).getId()))
            item = _catchList.get(i);
        }
    
        if (item != null) {
          out.println("} catch (Exception " + e + ") {");
          out.pushDepth();
          
          out.println("if (" + e + " instanceof QuercusExitException) "
                      + "throw (QuercusExitException) " + e + ";");
          
          ExprPro catchExpr = (ExprPro) item.getExpr();
          CompilingStatement catchBlock = (CompilingStatement) item.getBlock();

          catchExpr.getGenerator().generateAssign(out,
                  new JavaCodeExprPro("env.createException(" + e + ")"),
                  true);

          out.println(";");
          catchBlock.getGenerator().generate(out);

          out.popDepth();
        }
    
        out.println("}");
      }
    };
}

