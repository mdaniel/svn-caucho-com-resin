/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.statement;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.expr.AbstractVarExpr;
import com.caucho.quercus.expr.ExprPro;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.ExprType;
import com.caucho.quercus.expr.ExprGenerator;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.expr.DummyGenerator;
import com.caucho.quercus.statement.ForeachStatement;
import com.caucho.quercus.statement.Statement;

import java.io.IOException;

/**
 * Represents a foreach statement.
 */
public class ProForeachStatement extends ForeachStatement
  implements CompilingStatement
{
  public ProForeachStatement(Location location,
                             Expr objExpr,
                             AbstractVarExpr key,
                             AbstractVarExpr value,
                             boolean isRef,
                             Statement block,
                             String label)
  {
    super(location, objExpr, key, value, isRef, block, label);
  }

  public StatementGenerator getGenerator()
  {
    return GENERATOR;
  }

  private StatementGenerator GENERATOR = new StatementGenerator() {
      protected Location getLocation()
      {
	return ProForeachStatement.this.getLocation();
      }
      
      /**
       * Analyze the statement
       */
      public boolean analyze(AnalyzeInfo info)
      {
	ExprPro obj = (ExprPro) _objExpr;
	
	obj.getGenerator().analyze(info);

	AnalyzeInfo contInfo = info.copy();
	AnalyzeInfo breakInfo = info;

	AnalyzeInfo loopInfo = info.createLoop(contInfo, breakInfo);

	ExprGenerator dummyValue = new DummyGenerator();
	
	ExprPro key = (ExprPro) _key;
	if (_key != null)
	  key.getGenerator().analyzeAssign(loopInfo, dummyValue);

	ExprPro value = (ExprPro) _value;
	if (_value != null) {
	  value.getGenerator().analyzeAssign(loopInfo, dummyValue);

	  if (_isRef)
	    value.getGenerator().analyzeSetReference(loopInfo);
	}

	CompilingStatement block = (CompilingStatement) _block;
	block.getGenerator().analyze(loopInfo);

	loopInfo.merge(contInfo);

	info.merge(loopInfo);

	ExprGenerator valueExpr = new DummyGenerator();

	if (_key != null)
	  key.getGenerator().analyzeAssign(loopInfo, valueExpr);

	if (_value != null)
	  value.getGenerator().analyzeAssign(loopInfo, valueExpr);

	block.getGenerator().analyze(loopInfo);

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
        int id = out.generateId();
        
        //String loopLabel = PhpWriter.getForeachLabelPrefix() + id;
        //out.pushLoopLabel(loopLabel);
        
        ExprPro obj = (ExprPro) _objExpr;
        ExprPro key = (ExprPro) _key;
        ExprPro value = (ExprPro) _value;

        String origObjVar = "quercus_origObj_" + id;
        String objVar = "quercus_obj_" + id;
        String iterVar = "quercus_iterator_" + id;
        String entryVar = "quercus_entry_" + id;
        String keyVar = "quercus_key_" + id;
        String valueVar = "quercus_value_" + id;

        out.print("Value " + origObjVar + " = ");
        obj.getGenerator().generate(out);
        out.println(";");

        // php/3669 copy()
        out.println("Value " + objVar + " = " + origObjVar + ".copy();");
        
        if (_key == null && ! _isRef) {
          out.println("java.util.Iterator<Value> " + iterVar + " = " + objVar + ".getValueIterator(env);");
          out.println(_label + ":");
          out.println("while (" + iterVar + ".hasNext()) {");
          out.pushDepth();
          // php/3662 copy()
          out.println("Value " + valueVar + " = " + iterVar + ".next().copy();");
        }
        else if (_isRef) {
          out.println("java.util.Iterator<Value> " + iterVar + " = " + objVar + ".getKeyIterator(env);");
          out.println(_label + ":");
          out.println("while (" + iterVar + ".hasNext()) {");
          out.pushDepth();
          out.println("Value " + keyVar + " = " + iterVar + ".next();");
        }
        else {
          out.println("java.util.Iterator<java.util.Map.Entry<Value, Value>> " + iterVar + " = " + objVar + ".getIterator(env);");
          out.println(_label + ":");
          out.println("while (" + iterVar + ".hasNext()) {");
          out.pushDepth();
          out.println("java.util.Map.Entry<Value, Value> " + entryVar + " = " + iterVar + ".next();");
          out.println("Value " + keyVar + " = " + entryVar + ".getKey();");
          // php/366w copy()
          out.println("Value " + valueVar + " = " + entryVar + ".getValue().copy();");
        }

	if (_key != null) {
	  key.getGenerator().generateAssign(out,
					    new RawExpr(keyVar),
					    true);
	  out.println(";");
	}

	if (_isRef) {
          // php/3667 use origObjVar
          String valueRef = origObjVar + ".getRef(" + keyVar + ")";

	  value.getGenerator().generateAssignRef(out,
						 new RawExpr(valueRef),
						 true);
	  out.println(";");
	} else {
	  value.getGenerator().generateAssign(out,
					      new RawExpr(valueVar),
					      true);
	  out.println(";");
	}

        CompilingStatement block = (CompilingStatement) _block;
	block.getGenerator().generate(out);

	out.popDepth();
	out.println("}");
	
	//out.popLoopLabel();
      }
    };

  static class RawExpr extends Expr implements ExprPro
  {
    private String _code;

    RawExpr(String code)
    {
      _code = code;
    }

    @Override
    public Value eval(Env env)
    {
      throw new UnsupportedOperationException();
    }

    public ExprGenerator getGenerator()
    {
      return GENERATOR;
    }

    private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
	@Override
	public ExprType analyze(AnalyzeInfo info)
	{
	  return ExprType.VALUE;
	}
	
	@Override
	public void generate(PhpWriter out)
	  throws IOException
	{
	  out.print(_code);
	}
      };
  }
}

