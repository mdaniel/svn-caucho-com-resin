/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.ql;

import com.caucho.config.ConfigException;
import com.caucho.ejb.cfg.FunctionSignature;
import com.caucho.util.CharBuffer;

import java.util.ArrayList;

/**
 * A builtin SQL function expression
 */
public class FunExpr extends Expr {
  static ArrayList<FunctionSignature> _standardFunctions;
  
  // function name
  private String _name;
  // arguments
  private ArrayList<Expr> _args;

  private ArrayList<FunctionSignature> _functions;

  private FunctionSignature _sig;

  /**
   * Creates a function expression.
   *
   * @param name the function name
   * @param args the function arguments
   */
  FunExpr(String name,
          ArrayList<Expr> args,
          ArrayList<FunctionSignature> functions)
    throws ConfigException
  {
    _name = name;
    _args = args;
    _functions = functions;

    evalTypes();
  }

  /**
   * Evaluates the types for the expression
   */
  void evalTypes()
    throws ConfigException
  {
    Class []argTypes = getArgTypes();

    _sig = null;

    for (int i = 0; i < _functions.size(); i++) {
      FunctionSignature sig = _functions.get(i);

      if (! _name.equalsIgnoreCase(sig.getName()))
        continue;

      _sig = sig;

      Class []funArgs = sig.getParameterTypes();
      
      if (funArgs.length != argTypes.length)
        continue;

      boolean isMatch = true;
      for (int j = 0; isMatch && j < funArgs.length; j++) {
        if (argTypes[j].equals(funArgs[j]))
          continue;
        else if (Object.class.equals(funArgs[j]))
          continue;
        else if ((double.class.equals(funArgs[j]) ||
                  int.class.equals(funArgs[j])) &&
                 (double.class.equals(argTypes[j]) ||
                  int.class.equals(argTypes[j])))
          continue;
        else
          isMatch = false;
      }

      if (! isMatch)
        continue;

      _sig = sig;
      setJavaType(sig.getReturnType());
      return;
    }

    if (_sig != null)
      throw error(L.l("`{0}' signature does not match `{1}'",
                      _name, _sig.getSignature()));
    else
      throw error(L.l("unknown function `{0}'", _name));
  }

  /**
   * Calculate the argument types.
   */
  private Class []getArgTypes()
  {
    ArrayList<Class> argTypes = new ArrayList<Class>();

    for (int i = 0; i < _args.size(); i++) {
      Expr expr = _args.get(i);

      if (expr.isBoolean())
        argTypes.add(boolean.class);
      else if (expr.isInteger())
        argTypes.add(int.class);
      else if (expr.isNumeric())
        argTypes.add(double.class);
      else if (expr.isDate())
        argTypes.add(java.util.Date.class);
      else if (expr.isString())
        argTypes.add(String.class);
      else
        argTypes.add(Object.class);
    }

    return argTypes.toArray(new Class[argTypes.size()]);
  }

  /**
   * Prints the where SQL for this expression
   *
   * @param gen the java code generator
   */
  void generateWhere(CharBuffer cb)
  {
    if (_sig != null && _sig.getSQL() != null)
      generateWhereSQL(cb, _sig.getSQL());
    else {
      cb.append(_name);

      cb.append("(");
      for (int i = 0; i < _args.size(); i++) {
	if (i != 0)
	  cb.append(", ");
      
	Expr expr = _args.get(i);

	expr.generateWhere(cb);
      }
    
      cb.append(")");
    }
  }

  /**
   * Prints the where SQL for this expression
   *
   * @param gen the java code generator
   */
  void generateWhereSQL(CharBuffer cb, String sql)
  {
    for (int i = 0; i < sql.length(); i++) {
      char ch = sql.charAt(i);
      char ch1;

      if (ch == '?' && i + 1 < sql.length() &&
	  (ch1 = sql.charAt(i + 1)) >= '1' && ch1 <= '9') {
	int index = ch1 - '0';
	i++;

	if (index <= 0 || _args.size() < index)
	  throw new IllegalStateException(L.l("illegal argument for sql `{0}'", sql));

	_args.get(index - 1).generateWhere(cb);
      }
      else
	cb.append(ch);
    }
  }

  /**
   * Prints the select SQL for this expression
   *
   * @param gen the java code generator
   */
  void generateSelect(CharBuffer cb)
  {
    generateWhere(cb);
  }

  /**
   * Returns a printable version of the function.
   */
  public String toString()
  {
    CharBuffer cb = new CharBuffer();
    cb.append(_name);

    cb.append("(");
    for (int i = 0; i < _args.size(); i++) {
      if (i != 0)
        cb.append(", ");

      cb.append(_args.get(i));
    }
    
    cb.append(")");

    return cb.toString();
  }

  public static ArrayList<FunctionSignature> getStandardFunctions()
  {
    return (ArrayList) _standardFunctions.clone();
  }
  
  static void addFunction(String signature)
  {
    try {
      FunctionSignature sig = new FunctionSignature(signature);

      _standardFunctions.add(sig);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  static {
    _standardFunctions = new ArrayList<FunctionSignature>();
    
    addFunction("int abs(int)");
    addFunction("double abs(double)");

    addFunction("double acos(double)");
    addFunction("double sin(double)");
    addFunction("double atan(double)");
    addFunction("double cos(double)");
    addFunction("double cot(double)");
    addFunction("double degrees(double)");
    addFunction("double exp(double)");
    addFunction("double log(double)");
    addFunction("double log10(double)");
    addFunction("double radians(double)");
    addFunction("double sin(double)");
    addFunction("double sqrt(double)");
    addFunction("double tan(double)");
    addFunction("double pi()");
    
    addFunction("int ceiling(double)");
    addFunction("int floor(double)");
    addFunction("int sign(double)");
    
    addFunction("double atan2(double, double)");
    addFunction("double power(double, double)");
    addFunction("double round(double, double)");
    addFunction("double truncate(double, double)");
    
    addFunction("int mod()");
    addFunction("int rand()");
    
    addFunction("int count(any)");
    addFunction("double sum(any)");
    
    addFunction("int min(int)");
    addFunction("int max(int)");
    
    addFunction("double min(double)");
    addFunction("double max(double)");
    
    addFunction("int ascii(String)");
    addFunction("int length(String)");
    
    addFunction("String char(int)");
    addFunction("String space(int)");
    
    addFunction("String concat(String, String)");
    
    addFunction("int difference(String, String)");
    
    addFunction("String insert(String, int, int, String)");
    
    addFunction("String lcase(String)");
    addFunction("String ltrim(String)");
    addFunction("String rtrim(String)");
    addFunction("String ucase(String)");
    addFunction("String soundex(String)");
    
    addFunction("String left(String, int)");
    addFunction("String repeat(String, int)");
    addFunction("String right(String, int)");
    
    addFunction("int locate(String, String)");
    addFunction("int locate(String, String, int)");
    
    addFunction("String replace(String, String, String)");
    
    addFunction("String substring(String, int, int)");
    
    addFunction("String database()");
    addFunction("String user()");
    
    addFunction("Date curdate()");
    addFunction("Date curtime()");
    addFunction("Date now()");
    
    addFunction("String dayname(Date)");
    addFunction("String monthname(Date)");
    
    addFunction("int dayofmonth(Date)");
    addFunction("int dayofweek(Date)");
    addFunction("int dayofyear(Date)");
    addFunction("int hour(Date)");
    addFunction("int minute(Date)");
    addFunction("int month(Date)");
    addFunction("int quarter(Date)");
    addFunction("int second(Date)");
    addFunction("int week(Date)");
    addFunction("int year(Date)");
    
    addFunction("Date timestampadd(Date, Date)");
    addFunction("Date timestampdiff(Date, Date)");
  }
}
