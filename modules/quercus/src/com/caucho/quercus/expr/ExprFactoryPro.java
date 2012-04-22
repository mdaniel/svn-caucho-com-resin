/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.parser.QuercusParser;
import com.caucho.quercus.program.*;
import com.caucho.quercus.statement.*;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Factory for creating PHP expressions and statements
 */
public class ExprFactoryPro extends ExprFactory {
  private static final L10N L = new L10N(ExprFactoryPro.class);
  private static final Logger log
    = Logger.getLogger(ExprFactoryPro.class.getName());

  public ExprFactoryPro()
  {
  }

  /**
   * Creates a null literal expression.
   */
  @Override
  public Expr createNull()
  {
    return LiteralNullExprPro.NULL;
  }

  /**
   * Creates a string literal expression.
   */
  @Override
  public Expr createString(String lexeme)
  {
    return new LiteralStringExprPro(lexeme);
  }

  /**
   * Creates a binary literal expression.
   */
  @Override
  public Expr createBinary(byte []bytes)
  {
    return new LiteralBinaryStringExprPro(bytes);
  }

  /**
   * Creates a binary literal expression.
   */
  @Override
  public Expr createUnicode(String lexeme)
  {
    return new LiteralUnicodeExprPro(lexeme);
  }

  /**
   * Creates a long literal expression.
   */
  @Override
  public Expr createLong(long value)
  {
    return new LiteralLongExprPro(value);
  }

  /**
   * Creates a literal expression.
   */
  @Override
  public Expr createLiteral(Value value)
  {
    return new LiteralExprPro(value);
  }

  /**
   * Creates a var expression.
   */
  @Override
  public VarExpr createVar(VarInfo var)
  {
    return new VarExprPro((InfoVarPro) var);
  }

  /**
   * Creates a var expression.
   */
  @Override
  public VarVarExpr createVarVar(Expr var)
  {
    return new VarVarExprPro(var);
  }

  /**
   * Creates a __FILE__ expression.
   */
  @Override
  public ConstFileExpr createFileNameExpr(String fileName)
  {
    return new ConstFileProExpr(fileName);
  }

  /**
   * Creates a __DIR__ expression.
   */
  @Override
  public ConstDirExpr createDirExpr(String fileName)
  {
    return new ConstDirExprPro(fileName);
  }

  /**
   * Creates a const expression.
   */
  @Override
  public ConstExpr createConst(String name)
  {
    return new ConstExprPro(name);
  }

  /**
   * Creates a this expression.
   */
  @Override
  public ThisExprPro createThis(InterpretedClassDef cl)
  {
    return new ThisExprPro(cl);
  }

  /**
   * Creates a "$this->foo" expression.
   */
  @Override
  public ThisFieldExprPro createThisField(ThisExpr qThis,
                                          StringValue name)
  {
    return new ThisFieldExprPro(qThis, name);
  }

  /**
   * Creates a "$this->$foo" expression.
   */
  @Override
  public ThisFieldVarExprPro createThisField(ThisExpr qThis, Expr name)
  {
    return new ThisFieldVarExprPro(qThis, name);
  }

  /**
   * Creates a $this method call $this->foo(...).
   */
  @Override
  public ThisMethodExprPro createThisMethod(Location loc,
                                            ThisExpr qThis,
                                            String methodName,
                                            ArrayList<Expr> args)
  {
    return new ThisMethodExprPro(loc, qThis, methodName, args);
  }

  /**
   * Creates a $this method call $this->foo(...).
   */
  @Override
  public ThisMethodVarExprPro createThisMethod(Location loc,
                                               ThisExpr qThis,
                                               Expr methodName,
                                               ArrayList<Expr> args)
  {
    return new ThisMethodVarExprPro(loc, qThis, methodName, args);
  }

  /**
   * Creates an array get 'a[0]' expression.
   */
  @Override
  public ArrayGetExpr createArrayGet(Location location,
                                     Expr base,
                                     Expr index)
  {
    /*
    if (base instanceof ArrayGetExpr
        || base instanceof ObjectFieldExpr
        || base instanceof ObjectFieldVarExpr)
      return new ArrayGetGetExprPro(location, base, index);
    else
      return new ArrayGetExprPro(location, base, index);
      */
    return new ArrayGetExprPro(location, base, index);
  }

  /**
   * Creates an array tail 'a[]' expression.
   */
  @Override
  public ArrayTailExpr createArrayTail(Location location, Expr base)
  {
    return new ArrayTailExprPro(location, base);
  }

  /**
   * Creates an object get '$a->b' expression.
   */
  @Override
  public Expr createFieldGet(Expr base, StringValue name)
  {
    return new ObjectFieldExprPro(base, name);
  }

  /**
   * Creates an object get '$a->$b' expression.
   */
  @Override
  public Expr createFieldVarGet(Expr base, Expr name)
  {
    return new ObjectFieldVarExprPro(base, name);
  }

  //
  // class scope deref a::b
  //

  /**
   * Creates a class const expression.
   */
  @Override
  public ClassConstExpr createClassConst(String className, String name)
  {
    return new ClassConstExprPro(className, name);
  }

  /**
   * Creates a class const expression.
   */
  @Override
  public ClassVarConstExpr createClassConst(Expr className, String name)
  {
    return new ClassVarConstExprPro(className, name);
  }

  /**
   * Creates a class const expression (static::FOO).
   */
  @Override
  public ClassVirtualConstExpr createClassVirtualConst(String name)
  {
    return new ClassVirtualConstExprPro(name);
  }

  /**
   * Creates a class field 'a::$b' expression.
   */
  @Override
  public Expr createClassField(String className,
                               String name)
  {
    return new ClassFieldExprPro(className, name);
  }

  /**
   * Creates a class field '$a::$b' expression.
   */
  @Override
  public Expr createClassField(Expr className,
                               String name)
  {
    return new ClassVarFieldExprPro(className, name);
  }

  /**
   * Creates an object get 'static::b' expression.
   */
  @Override
  public Expr createClassVirtualField(String name)
  {
    return new ClassVirtualFieldExprPro(name);
  }

  /**
   * Creates an object get 'a::${b}' expression.
   */
  @Override
  public Expr createClassField(String className,
                               Expr name)
  {
    return new ClassFieldVarExprPro(className, name);
  }

  /**
   * Creates an object get 'a::${b}' expression.
   */
  @Override
  public Expr createClassField(Expr className,
                               Expr name)
  {
    return new ClassVarFieldVarExprPro(className, name);
  }

  /**
   * Creates an object get 'static::${b}' expression.
   */
  @Override
  public Expr createClassVirtualField(Expr name)
  {
    return new ClassVirtualFieldVarExprPro(name);
  }

  //
  // unary expressions
  //

  /**
   * Creates a ref '&$a' expression.
   */
  @Override
  public UnaryRefExpr createRef(Expr base)
  {
    return new UnaryRefExprPro(base);
  }

  /**
   * Creates an unset '$a' expression.
   */
  @Override
  public Expr createUnsetVar(AbstractVarExpr var)
  {
    return new VarUnsetExprPro(var);
  }

  /**
   * Creates a char at 'a{0}' expression.
   */
  @Override
  public BinaryCharAtExpr createCharAt(Expr base, Expr index)
  {
    return new BinaryCharAtExprPro(base, index);
  }

  /**
   * Creates a post increment 'a++' expression.
   */
  @Override
  public UnaryPostIncrementExpr createPostIncrement(Expr expr, int incr)
  {
    return new UnaryPostIncrementExprPro(expr, incr);
  }

  /**
   * Creates a pre increment '++a' expression.
   */
  @Override
  public UnaryPreIncrementExpr createPreIncrement(Expr expr, int incr)
  {
    return new UnaryPreIncrementExprPro(expr, incr);
  }

  /**
   * Creates a unary minus '-a' expression.
   */
  @Override
  public Expr createMinus(Expr expr)
  {
    return new UnaryMinusExprPro(expr);
  }

  /**
   * Creates a unary plus '+a' expression.
   */
  @Override
  public Expr createPlus(Expr expr)
  {
    return new UnaryPlusExprPro(expr);
  }

  /**
   * Creates a clone 'clone a' expression.
   */
  @Override
  public Expr createClone(Expr expr)
  {
    return new FunCloneExprPro(expr);
  }

  /**
   * Creates a clone 'clone a' expression.
   */
  public Expr createCopy(Expr expr)
  {
    return new UnaryCopyExprPro(expr);
  }

  /**
   * Creates a logical not '!a' expression.
   */
  @Override
  public Expr createNot(Expr expr)
  {
    return new UnaryNotExprPro(expr);
  }

  /**
   * Creates a unary inversion '~a' expression.
   */
  @Override
  public Expr createBitNot(Expr expr)
  {
    return new UnaryBitNotExprPro(expr);
  }

  /**
   * Creates an error suppression '@a' expression.
   */
  @Override
  public Expr createSuppress(Expr expr)
  {
    return new UnarySuppressErrorExprPro(expr);
  }

  /**
   * Creates a boolean cast
   */
  @Override
  public Expr createToBoolean(Expr expr)
  {
    return new ToBooleanExprPro(expr);
  }

  /**
   * Creates a long cast
   */
  @Override
  public Expr createToLong(Expr expr)
  {
    return new ToLongExprPro(expr);
  }

  /**
   * Creates a double cast
   */
  @Override
  public Expr createToDouble(Expr expr)
  {
    return new ToDoubleExprPro(expr);
  }

  /**
   * Creates a string cast
   */
  @Override
  public Expr createToString(Expr expr)
  {
    return new ToStringExprPro(expr);
  }

  /**
   * Creates a binary string cast
   */
  @Override
  public Expr createToBinary(Expr expr)
  {
    return new ToBinaryExprPro(expr);
  }

  /**
   * Creates a unicode string cast
   */
  @Override
  public Expr createToUnicode(Expr expr)
  {
    return new ToUnicodeExprPro(expr);
  }

  /**
   * Creates an object cast
   */
  @Override
  public Expr createToObject(Expr expr)
  {
    return new ToObjectExprPro(expr);
  }

  /**
   * Creates an array cast
   */
  @Override
  public Expr createToArray(Expr expr)
  {
    return new ToArrayExprPro(expr);
  }

  /**
   * Creates an die 'die("msg")' expression.
   */
  @Override
  public Expr createDie(Expr expr)
  {
    return new FunDieExprPro(expr);
  }

  /**
   * Creates an exit 'exit("msg")' expression.
   */
  @Override
  public Expr createExit(Expr expr)
  {
    return new FunExitExprPro(expr);
  }

  /**
   * Creates a required
   */
  @Override
  public Expr createRequired()
  {
    return ParamRequiredExprPro.REQUIRED;
  }

  /**
   * Creates a default
   */
  public Expr createDefault()
  {
    return new ParamDefaultExprPro();
  }

  /**
   * Creates an addition expression.
   */
  @Override
  public Expr createAdd(Expr left, Expr right)
  {
    return new BinaryAddExprPro(left, right);
  }

  /**
   * Creates a subtraction expression.
   */
  @Override
  public Expr createSub(Expr left, Expr right)
  {
    return new BinarySubExprPro(left, right);
  }

  /**
   * Creates a multiplication expression.
   */
  @Override
  public Expr createMul(Expr left, Expr right)
  {
    return new BinaryMulExprPro(left, right);
  }

  /**
   * Creates a division expression.
   */
  @Override
  public Expr createDiv(Expr left, Expr right)
  {
    return new BinaryDivExprPro(left, right);
  }

  /**
   * Creates a modulo expression.
   */
  @Override
  public Expr createMod(Expr left, Expr right)
  {
    return new BinaryModExprPro(left, right);
  }

  /**
   * Creates a left-shift expression.
   */
  @Override
  public Expr createLeftShift(Expr left, Expr right)
  {
    return new BinaryLeftShiftExprPro(left, right);
  }

  /**
   * Creates a right-shift expression.
   */
  @Override
  public Expr createRightShift(Expr left, Expr right)
  {
    return new BinaryRightShiftExprPro(left, right);
  }

  /**
   * Creates a bit-and expression.
   */
  @Override
  public Expr createBitAnd(Expr left, Expr right)
  {
    return new BinaryBitAndExprPro(left, right);
  }

  /**
   * Creates a bit-xor expression.
   */
  @Override
  public Expr createBitXor(Expr left, Expr right)
  {
    return new BinaryBitXorExprPro(left, right);
  }

  /**
   * Creates a bit-or expression.
   */
  @Override
  public Expr createBitOr(Expr left, Expr right)
  {
    return new BinaryBitOrExprPro(left, right);
  }

  /**
   * Creates a lt expression.
   */
  @Override
  public Expr createLt(Expr left, Expr right)
  {
    return new BinaryLtExprPro(left, right);
  }

  /**
   * Creates a leq expression.
   */
  @Override
  public Expr createLeq(Expr left, Expr right)
  {
    return new BinaryLeqExprPro(left, right);
  }

  /**
   * Creates a gt expression.
   */
  @Override
  public Expr createGt(Expr left, Expr right)
  {
    return new BinaryGtExprPro(left, right);
  }

  /**
   * Creates a geq expression.
   */
  @Override
  public Expr createGeq(Expr left, Expr right)
  {
    return new BinaryGeqExprPro(left, right);
  }

  /**
   * Creates an eq expression.
   */
  @Override
  public Expr createEq(Expr left, Expr right)
  {
    return new BinaryEqExprPro(left, right);
  }

  /**
   * Creates a neq expression.
   */
  @Override
  public Expr createNeq(Expr left, Expr right)
  {
    return new BinaryNeqExprPro(left, right);
  }

  /**
   * Creates an equals expression.
   */
  @Override
  public Expr createEquals(Expr left, Expr right)
  {
    return new BinaryEqualsExprPro(left, right);
  }

  /**
   * Creates an append expression.
   */
  @Override
  protected BinaryAppendExpr createAppendImpl(Expr left, BinaryAppendExpr right)
  {
    return new BinaryAppendExprPro(left, (BinaryAppendExprPro) right);
  }

  /**
   * Creates an assignment expression.
   */
  @Override
  public Expr createAssign(AbstractVarExpr var, Expr right)
  {
    return new BinaryAssignExprPro(var, right);
  }

  /**
   * Creates an assignment ref expression.
   */
  @Override
  public Expr createAssignRef(AbstractVarExpr var, Expr right)
  {
    return new BinaryAssignRefExprPro(var, right);
  }

  /**
   * Creates an and expression.
   */
  @Override
  public Expr createAnd(Expr left, Expr right)
  {
    return new BinaryAndExprPro(left, right);
  }

  /**
   * Creates an or expression.
   */
  @Override
  public Expr createOr(Expr left, Expr right)
  {
    return new BinaryOrExprPro(left, right);
  }

  /**
   * Creates an xor expression.
   */
  @Override
  public Expr createXor(Expr left, Expr right)
  {
    return new BinaryXorExprPro(left, right);
  }

  /**
   * Creates a comma expression.
   */
  @Override
  public Expr createComma(Expr left, Expr right)
  {
    return new BinaryCommaExprPro(left, right);
  }

  /**
   * Creates an instanceof expression.
   */
  public Expr createInstanceOf(Expr expr, String name)
  {
    return new BinaryInstanceOfExprPro(expr, name);
  }

  /**
   * Creates an instanceof expression.
   */
  public Expr createInstanceOfVar(Expr expr, Expr name)
  {
    return new BinaryInstanceOfVarExprPro(expr, name);
  }

  /**
   * Creates an each expression.
   */
  @Override
  public Expr createEach(Expr expr)
  {
    return new FunEachExprPro(expr);
  }

  /**
   * Creates a list expression.
   */
  @Override
  public ListHeadExpr createListHead(ArrayList<Expr> keys)
  {
    return new ListHeadExprPro(keys);
  }

  /**
   * Creates a list expression.
   */
  @Override
  public Expr createList(ListHeadExpr head, Expr value)
  {
    return new BinaryAssignListExprPro(head, value);
  }

  /**
   * Creates a list expression.
   */
  @Override
  public Expr createListEach(ListHeadExpr head, Expr value)
  {
    return new BinaryAssignListEachExprPro(head, value);
  }

  /**
   * Creates an conditional expression.
   */
  @Override
  public Expr createConditional(Expr test, Expr left, Expr right)
  {
    return new ConditionalExprPro(test, left, right);
  }

  /**
   * Creates an conditional expression.
   */
  @Override
  public Expr createShortConditional(Expr test, Expr right)
  {
    return new ConditionalShortExprPro(test, right);
  }

  /**
   * Creates a array() expression.
   */
  @Override
  public Expr createArrayFun(ArrayList<Expr> keys, ArrayList<Expr> values)
  {
    return new FunArrayExprPro(keys, values);
  }

  /**
   * Creates a new function call.
   */
  @Override
  public Expr createCall(QuercusParser parser,
                         String name,
                         ArrayList<Expr> args)
  {
    Location loc = parser.getLocation();

    String systemName = parser.getSystemFunctionName(name);

    if (systemName != null)
      name = systemName;

    if ("isset".equals(name) && args.size() == 1)
      return new FunIssetExprPro(args.get(0));
    else if ("empty".equals(name) && args.size() == 1)
      return new CallPredicateExprPro(loc, name, args, "isEmpty");
    else if ("is_array".equals(name) && args.size() == 1)
      return new CallPredicateExprPro(loc, name, args, "isArray");
    else if ("is_bool".equals(name) && args.size() == 1)
      return new CallPredicateExprPro(loc, name, args, "isBoolean");
    else if ("is_null".equals(name) && args.size() == 1)
      return new CallPredicateExprPro(loc, name, args, "isNull");
    else if ("is_numeric".equals(name) && args.size() == 1)
      return new CallPredicateExprPro(loc, name, args, "isNumeric");
    else if ("is_object".equals(name) && args.size() == 1)
      return new CallPredicateExprPro(loc, name, args, "isObject");
    else if ("is_resource".equals(name) && args.size() == 1)
      return new CallPredicateExprPro(loc, name, args, "isResource");
    else if ("is_string".equals(name) && args.size() == 1)
      return new CallPredicateExprPro(loc, name, args, "isString");
    else if ("get_called_class".equals(name) && args.size() == 0)
      return new FunGetCalledClassExprPro(loc);
    else if ("get_class".equals(name) && args.size() == 0)
      return new FunGetClassExprPro(parser);
    else if ("each".equals(name) && args.size() == 1) {
      Expr arg = args.get(0);

      return new FunEachExprPro(arg);
    }
    else if ("define".equals(name)
             && args.size() == 2
             && args.get(0).isLiteral())
      return new FunDefineExprPro(loc, name, args);
    else
      return new CallExprPro(loc, name, args);
  }

  /**
   * Creates a new var function call.
   */
  @Override
  public CallVarExpr createVarFunction(Location loc,
                                           Expr name,
                                           ArrayList<Expr> args)
  {
    return new CallVarExprPro(loc, name, args);
  }

  /**
   * Creates a new closure call.
   */
  @Override
  public ClosureExprPro createClosure(Location loc,
                                      Function fun,
                                      ArrayList<VarExpr> useArgs)
  {
    return new ClosureExprPro(loc, fun, useArgs);
  }

  /**
   * Creates a new method call.
   */
  @Override
  public Expr createMethodCall(Location loc,
                               Expr objExpr,
                               Expr name,
                               ArrayList<Expr> args)
  {
    return new ObjectMethodVarExprPro(loc, objExpr, name, args);
  }

  /**
   * Creates a new call.
   */
  @Override
  public ObjectNewExpr createNew(Location loc,
                                 String name,
                                 ArrayList<Expr> args)
  {
    return new ObjectNewExprPro(loc, name, args);
  }

  @Override
  public ObjectNewStaticExpr createNewStatic(Location loc,
                                             ArrayList<Expr> args)
  {
    return new ObjectNewStaticExprPro(loc, args);
  }

  /**
   * Creates a new function call.
   */
  @Override
  public Expr createClassMethodCall(Location loc,
                                    String className,
                                    String name,
                                    ArrayList<Expr> args)
  {
    if ("__construct".equals(name)) {
      return new ClassConstructExprPro(loc, className, args);
    }
    else {
      return new ClassMethodExprPro(loc, className, name, args);
    }
  }

  /**
   * Creates a new function call.
   */
  @Override
  public Expr createClassMethodCall(Location loc,
                                    Expr className,
                                    String name,
                                    ArrayList<Expr> args)
  {
    return new ClassVarMethodExprPro(loc, className, name, args);
  }

  /**
   * Creates a new function call.
   */
  @Override
  public Expr createClassMethodCall(Location loc,
                                    Expr className,
                                    Expr name,
                                    ArrayList<Expr> args)
  {
    return new ClassVarMethodVarExprPro(loc, className, name, args);
  }

  /**
   * Creates a new function call based on the class context.
   */
  @Override
  public Expr createClassVirtualMethodCall(Location loc,
                                                     String methodName,
                                                     ArrayList<Expr> args)
  {
    return new ClassVirtualMethodExprPro(loc, methodName, args);
  }

  /**
   * Creates a new function call.
   */
  @Override
  public Expr createClassMethodCall(Location loc,
                                    String className,
                                    Expr name,
                                    ArrayList<Expr> args)
  {
    return new ClassMethodVarExprPro(loc, className, name, args);
  }

  /**
   * Creates a new method static::$f()
   */
  @Override
  public Expr createClassVirtualMethodCall(Location loc,
                                           Expr methodName,
                                           ArrayList<Expr> args)
  {
    return new ClassVirtualMethodVarExprPro(loc, methodName, args);
  }

  /**
   * Creates a class method call A::foo(...)
   */
  @Override
  public Expr createClassConstructor(Location loc,
                                     String className,
                                     String methodName,
                                     ArrayList<Expr> args)
  {
    return new ClassConstructorExprPro(loc, className, methodName, args);
  }

  /**
   * Creates a new method call.
   */
  @Override
  public Expr createMethodCall(Location loc,
                               Expr objExpr,
                               String name,
                               ArrayList<Expr> args)
  {
    return new ObjectMethodExprPro(loc, objExpr, name, args);
  }

  /**
   * Creates a new function call.
   */
  @Override
  public ObjectNewVarExpr createVarNew(Location loc,
                                 Expr name,
                                 ArrayList<Expr> args)
  {
    return new ObjectNewVarExprPro(loc, name, args);
  }

  /**
   * Creates an include expr
   */
  public Expr createInclude(Location loc,
                            Path source,
                            Expr expr)
  {
    return new FunIncludeExprPro(loc, source, expr, false);
  }

  /**
   * Creates an include expr
   */
  public Expr createRequire(Location loc,
                            Path source,
                            Expr expr)
  {
    return new FunIncludeExprPro(loc, source, expr, true);
  }

  /**
   * Creates an include expr
   */
  public Expr createIncludeOnce(Location loc,
                                Path source,
                                Expr expr)
  {
    return new FunIncludeOnceExprPro(loc, source, expr, false);
  }

  /**
   * Creates an include expr
   */
  public Expr createRequireOnce(Location loc,
                                Path source,
                                Expr expr)
  {
    return new FunIncludeOnceExprPro(loc, source, expr, true);
  }

  /**
   * Creates a Quercus class import.
   */
  public Expr createImport(Location loc,
                           String name,
                           boolean isWildcard)
  {
    return new FunImportExprPro(loc, name, isWildcard);
  }

  /**
   * Creates an echo statement
   */
  @Override
  public Statement createEcho(Location loc, Expr expr)
  {
    return new ProEchoStatement(loc, expr);
  }

  /**
   * Creates an expr statement
   */
  @Override
  public Statement createExpr(Location loc, Expr expr)
  {
    return new ProExprStatement(loc, expr);
  }

  /**
   * Creates an expr statement
   */
  @Override
  public BlockStatement createBlockImpl(Location loc, Statement []statements)
  {
    return new ProBlockStatement(loc, statements);
  }

  /**
   * Creates a text statement
   */
  @Override
  public Statement createText(Location loc, String text)
  {
    return new ProTextStatement(loc, text);
  }

  /**
   * Creates a null statement
   */
  @Override
  public Statement createNullStatement()
  {
    return ProNullStatement.PRO_NULL;
  }

  /**
   * Creates an if statement
   */
  @Override
  public Statement createIf(Location loc,
                            Expr test,
                            Statement trueBlock,
                            Statement falseBlock)
  {
    return new ProIfStatement(loc, test, trueBlock, falseBlock);
  }

  /**
   * Creates a switch statement
   */
  @Override
  public Statement createSwitch(Location loc,
                                Expr value,
                                ArrayList<Expr[]> caseList,
                                ArrayList<BlockStatement> blockList,
                                Statement defaultBlock,
                                String label)
  {
    return new ProSwitchStatement(loc, value, caseList,
                                  blockList, defaultBlock, label);
  }

  /**
   * Creates a for statement
   */
  @Override
  public Statement createFor(Location loc,
                              Expr init,
                              Expr test,
                              Expr incr,
                              Statement block,
                              String label)
  {
    return new ProForStatement(loc, init, test, incr, block, label);
  }

  /**
   * Creates a foreach statement
   */
  @Override
  public Statement createForeach(Location loc,
                                 Expr objExpr,
                                 AbstractVarExpr key,
                                 AbstractVarExpr value,
                                 boolean isRef,
                                 Statement block,
                                 String label)
  {
    return new ProForeachStatement(loc, objExpr, key, value, isRef,
                                   block, label);
  }

  /**
   * Creates a while statement
   */
  @Override
  public Statement createWhile(Location loc,
                               Expr test,
                               Statement block,
                               String label)
  {
    return new ProWhileStatement(loc, test, block, label);
  }

  /**
   * Creates a do-while statement
   */
  @Override
  public Statement createDo(Location loc,
                            Expr test,
                            Statement block,
                            String label)
  {
    return new ProDoStatement(loc, test, block, label);
  }

  /**
   * Creates a break statement
   */
  @Override
  public BreakStatement createBreak(Location location,
                                    Expr target,
                                    ArrayList<String> loopLabelList)
  {
    return new ProBreakStatement(location, target, loopLabelList);
  }

  /**
   * Creates a continue statement
   */
  @Override
  public ContinueStatement createContinue(Location location,
                                          Expr target,
                                          ArrayList<String> loopLabelList)
  {
    return new ProContinueStatement(location, target, loopLabelList);
  }

  /**
   * Creates a global statement
   */
  @Override
  public Statement createGlobal(Location loc,
                                VarExpr var)
  {
    return new ProGlobalStatement(loc, var);
  }

  /**
   * Creates a global var statement
   */
  @Override
  public Statement createVarGlobal(Location loc,
                                   VarVarExpr var)
  {
    return new ProVarGlobalStatement(loc, var);
  }

  /**
   * Creates a static statement inside a class
   */
  @Override
  public Statement createClassStatic(Location loc,
                                     String className,
                                     VarExpr var,
                                     Expr value)
  {
    return new ProClassStaticStatement(loc, className, var, value);
  }

  /**
   * Creates a static statement
   */
  @Override
  public Statement createStatic(Location loc,
                                VarExpr var,
                                Expr value)
  {
    return new ProStaticStatement(loc, var, value);
  }

  /**
   * Creates a throw statement
   */
  @Override
  public Statement createThrow(Location loc,
                               Expr value)
  {
    return new ProThrowStatement(loc, value);
  }

  /**
   * Creates a try statement
   */
  @Override
  public TryStatement createTry(Location loc,
                                Statement block)
  {
    return new ProTryStatement(loc, block);
  }

  /**
   * Creates a return statement
   */
  @Override
  public Statement createReturn(Location loc,
                                Expr value)
  {
    return new ProReturnStatement(loc, value);
  }

  /**
   * Creates a return ref statement
   */
  @Override
  public Statement createReturnRef(Location loc,
                                   Expr value)
  {
    return new ProReturnRefStatement(loc, value);
  }

  /**
   * Creates a new function definition def.
   */
  public Statement createFunctionDef(Location loc,
                                     Function fun)
  {
    return new ProFunctionDefStatement(loc, fun);
  }

  /**
   * Creates a new function def statement
   */
  @Override
  public Statement createClassDef(Location loc,
                                  InterpretedClassDef cl)
  {
    return new ProClassDefStatement(loc, cl);
  }

  /**
   * Creates a new FunctionInfo
   */
  @Override
  public FunctionInfo createFunctionInfo(QuercusContext quercus,
                                         ClassDef classDef,
                                         String name)
  {
    return new ProFunctionInfo(quercus, classDef, name);
  }

  /**
   * Creates a new function call.
   */
  @Override
  public Function createFunction(Location loc,
                                 String name,
                                 FunctionInfo info,
                                 Arg []argList,
                                 Statement []statementList)
  {
    return new ProFunction(this, loc, name, info, argList, statementList);
  }

  /**
   * Creates a new object method definition.
   */
  @Override
  public Function createObjectMethod(Location loc,
                                     InterpretedClassDef cl,
                                     String name,
                                     FunctionInfo info,
                                     Arg []argList,
                                     Statement []statementList)
  {
    return new ProObjectMethod(this, loc, cl, name, info,
                               argList, statementList);
  }

  /**
   * Creates a new object method definition.
   */
  @Override
  public Function createMethodDeclaration(Location loc,
                                          InterpretedClassDef cl,
                                          String name,
                                          FunctionInfo info,
                                          Arg []argList)
  {
    return new ProMethodDeclaration(this, loc, cl, name, info, argList);
  }

  @Override
  public InterpretedClassDef createClassDef(Location location, String name,
                                            String parentName,
                                            String[] ifaceList,
                                            int index)
  {
    return new ProClassDef(location, name, parentName, ifaceList, index);
  }
}

