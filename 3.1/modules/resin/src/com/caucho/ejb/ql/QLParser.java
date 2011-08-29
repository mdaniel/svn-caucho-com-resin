/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.ql;

import com.caucho.ejb.cfg21.EjbEntityBean;
import com.caucho.config.ConfigException;
import com.caucho.config.LineConfigException;
import com.caucho.ejb.cfg.*;
import com.caucho.util.CharBuffer;
import com.caucho.util.IntMap;
import com.caucho.util.L10N;
import com.caucho.util.Log;

import javax.ejb.EJBLocalObject;
import javax.ejb.EntityBean;
import javax.ejb.FinderException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Contains the parser for EJB-QL queries and stores the parsed expressions.
 *
 * <p>The expression tree is rooted at Expr.
 */
public class QLParser extends Query {
  static final Logger log = Log.open(QLParser.class);
  static final L10N L = new L10N(QLParser.class);
  
  final static int IDENTIFIER = 128;
  final static int INTEGER = IDENTIFIER + 1;
  final static int LONG = INTEGER + 1;
  final static int DOUBLE = LONG + 1;
  final static int STRING = DOUBLE + 1;
  final static int TRUE = STRING + 1;
  final static int FALSE = TRUE + 1;
  final static int UNKNOWN = FALSE + 1;
  final static int MEMBER = UNKNOWN + 1;
  final static int OF = MEMBER + 1;
  final static int EMPTY = OF + 1;
  final static int NULL = EMPTY + 1;
  
  final static int FROM = NULL + 1;
  final static int IN = FROM + 1;
  final static int SELECT = IN + 1;
  final static int DISTINCT = SELECT + 1;
  final static int WHERE = SELECT + 1;
  final static int AS = WHERE + 1;
  final static int ORDER = AS + 1;
  final static int BY = ORDER + 1;
  final static int ASC = BY + 1;
  final static int DESC = ASC + 1;
  final static int LIMIT = DESC + 1;
  final static int OFFSET = LIMIT + 1;
  
  final static int BETWEEN = OFFSET + 1;
  final static int LIKE = BETWEEN + 1;
  final static int ESCAPE = LIKE + 1;
  final static int IS = ESCAPE + 1;
  
  final static int EQ = IS + 1;
  final static int NE = EQ + 1;
  final static int LT = NE + 1;
  final static int LE = LT + 1;
  final static int GT = LE + 1;
  final static int GE = GT + 1;
  
  final static int AND = GE + 1;
  final static int OR = AND + 1;
  final static int NOT = OR + 1;

  final static int EXTERNAL_DOT = NOT + 1;
  
  final static int ARG = EXTERNAL_DOT + 1;
  final static int THIS = ARG + 1;

  private static IntMap _reserved;

  private String _location;
  
  // The owning bean
  private EjbEntityBean _bean;
  
  // The target bean
  private EjbEntityBean _target;

  // the functions
  private ArrayList<FunctionSignature> _functions;
  
  // Method name to generate
  private String _methodName;
  // Return class
  private Class _returnType;
  // Return ejb
  private String _returnEJB;
  // EJB-QL
  private String _query;

  // list of the identifier
  private ArrayList<FromItem> _fromList;
  // list of the identifiers
  private ArrayList<PathExpr> _fromIds = new ArrayList<PathExpr>();
  
  // list of the relation links
  //private ArrayList<LinkItem> _linkList;
  // select expression
  private Expr _selectExpr;
  // is distinct (set)
  private boolean _isDistinct;
  
  // from table
  private String _fromTable;
  // from identifier
  private String _fromId;
  // this expression
  private IdExpr _thisExpr;
  // where expression
  private Expr _whereExpr;
  // arguments
  private ArrayList<Expr> _argList;
  // order by expression
  private ArrayList<Expr> _orderExprList;
  // order by ascending/descending
  private ArrayList<Boolean> _orderAscendingList;
  // order by limit max
  private Expr _limitMax;
  // order by limit offset
  private Expr _limitOffset;

  private AndExpr _andExpr;
  private boolean _isWhere;

  private HashMap<String,PathExpr> _idMap;
  private HashMap<Expr,Expr> _pathMap;
  
  // parse index
  private int _parseIndex;
  // current token
  private int _token;
  // temp for parsing
  private String lexeme;

  private int _unique;

  private String _booleanTrue = "1";
  private String _booleanFalse = "0";

  private boolean _addArgToQuery = true;

  private boolean _queryLoadsBean = true;

  private int _maxArg;

  private QLParser(EjbEntityBean bean)
  {
    _bean = bean;

    // _functions = FunExpr.getStandardFunctions();
    _functions = bean.getConfig().getFunctions();
  }
  
  /**
   * Creates a new select method.
   *
   * @param bean the owning persistent bean
   * @param methodName the method name to implement
   * @param method the method signature
   */
  public QLParser(EjbEntityBean bean,
		  String methodName,
		  ApiMethod method,
		  Class returnType)
    throws ConfigException
  {
    this(bean);

    setMethod(method);
    
    _methodName = methodName;
    _returnType = returnType;

    Class []exn = method.getExceptionTypes();
    for (int i = 0; i < exn.length; i++)
      if (FinderException.class.isAssignableFrom(exn[i]))
        return;

    throw new ConfigException(L.l("{0}: '{1}' must throw javax.ejb.FinderException.",
				  method.getDeclaringClass().getName(),
				  getFullMethodName(method)));
  }

  public static void parseOrderBy(EjbEntityBean bean,
				  String orderBy,
				  ArrayList<String> orderList,
				  ArrayList<Boolean> orderAscendingList)
    throws ConfigException
  {
    QLParser query = new QLParser(bean);

    query.parseOrderBy(orderBy, orderList, orderAscendingList);
  }

  /**
   * Sets the location.
   */
  public void setLocation(String location)
  {
    _location = location;
  }
  
  /**
   * Returns the owning bean
   */
  public EjbEntityBean getPersistentBean()
  {
    return _bean;
  }

  /**
   * Gets a persistent bean by its type.
   */
  public EjbEntityBean getBeanByName(String ejbName)
  {
    // return _bean.getManager().getBeanInfoByName(ejbName);
    throw new UnsupportedOperationException();
  }

  /**
   * Gets a persistent bean by its type.
   */
  public EjbEntityBean getBeanByType(Class type)
  {
    //return _bean.getManager().getBeanInfoByRemote(type);
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the name of the select method.
   */
  public String getName()
  {
    return _methodName;
  }

  /**
   * Returns the function map.
   */
  public ArrayList<FunctionSignature> getFunctions()
  {
    return _functions;
  }

  /**
   * Sets the function map.
   */
  public void setFunctions(ArrayList<FunctionSignature> functions)
  {
    _functions = functions;
  }

  /**
   * Sets the boolean true.
   */
  public void setBooleanTrue(String literal)
  {
    _booleanTrue = literal;
  }

  /**
   * Sets the boolean false.
   */
  public void setBooleanFalse(String literal)
  {
    _booleanFalse = literal;
  }
  
  /**
   * Returns the return type of the select method.
   */
  public Class getReturnType()
  {
    return _returnType;
  }
  
  /**
   * Returns the return type of the select method.
   */
  public String getReturnEJB()
  {
    return _selectExpr.getReturnEJB();
  }
  
  /**
   * Sets the return type of the select method
   */
  void setReturnType(Class returnType)
  {
    _returnType = returnType;
  }

  /**
   * Returns the EJB-QL string
   */
  public String getQuery()
  {
    return _query;
  }

  /**
   * Gets the select expression
   */
  public Expr getSelectExpr()
  {
    return _selectExpr;
  }

  public boolean isDistinct()
  {
    return _isDistinct;
  }

  public boolean getQueryLoadsBean()
  {
    return _queryLoadsBean;
  }

  public void setQueryLoadsBean(boolean loadBean)
  {
    _queryLoadsBean = loadBean;
  }
  
  /**
   * Gets the this expression.
   */
  IdExpr getThisExpr()
  {
    return _thisExpr;
  }
  
  /**
   * Gets the where expression.
   */
  Expr getWhereExpr()
  {
    return _whereExpr;
  }
  
  /**
   * Gets the order by expression.
   */
  ArrayList<Expr> getOrderExprList()
  {
    return _orderExprList;
  }

  /**
   * Returns true if the order is ascending.
   */
  ArrayList<Boolean> getAscendingList()
  {
    return _orderAscendingList;
  }

  /**
   * Returns any limit max expression
   */
  Expr getLimitMax()
  {
    return _limitMax;
  }

  /**
   * Returns any limit offset expression
   */
  Expr getLimitOffset()
  {
    return _limitOffset;
  }

  /**
   * Gets the from table
   */
  ArrayList<FromItem> getFromList()
  {
    return _fromList;
  }

  void addFromItem(String id, String table)
  {
    _fromList.add(new FromItem(id, table));
  }

  public int getUnique()
  {
    return _unique++;
  }
  
  /**
   * Adds a relation expression
   */
  /*
  public void addLink(String tableA, String columnA,
                      String tableB, String columnB,
		      boolean isCommon)
  {
    if (isCommon)
      addCommonLink(tableA, columnA, tableB, columnB);
    else
      addLink(tableA, columnA, tableB, columnB);
  }
  */
  
  /**
   * Adds a relation expression
   */
  /*
  public void addLink(String tableA, String columnA,
                      String tableB, String columnB)
  {
    if (_andExpr != null) {
      LinkExpr expr = new LinkExpr(tableA, columnA, tableB, columnB);

      _andExpr.add(expr);

      return;
    }

    addCommonLink(tableA, columnA, tableB, columnB);
  }
  */
  
  /**
   * Adds a relation expression
   */
  /*
  public void addCommonLink(String tableA, String columnA,
			    String tableB, String columnB)
  {
    if (_linkList == null)
      _linkList = new ArrayList<LinkItem>();

    LinkItem item = new LinkItem(tableA, columnA, tableB, columnB);

    if (! _linkList.contains(item))
      _linkList.add(item);
  }
  */

  /**
   * Returns the auxiliary relation expressions
   */
  /*
  ArrayList<LinkItem> getRelations()
  {
    return _linkList;
  }
  */

  /**
   * Adds a select method argument
   */
  public void addArg(Expr expr)
  {
    _argList.add(expr);
  }

  /**
   * Gets the select method arguments in SQL order.
   */
  ArrayList<Expr> getArgList()
  {
    return _argList;
  }
  
  /**
   * Parses the select method's query.
   *
   * @param query the source query string.
   */
  public void parseOrderBy(String orderBy,
                           ArrayList<String> orderList,
                           ArrayList<Boolean> orderAscendingList)
    throws ConfigException
  {
    _query = orderBy;

    _parseIndex = 0;
    _unique = 0;
    _token = -1;

    int token = -1;
    
    do {
      token = scanToken();

      if (token == IDENTIFIER)
        orderList.add(lexeme.toString());
      else
        throw error(L.l("unexpected token '{0}' in order-by",
                        tokenName(token)));

      token = scanToken();
      if (token == DESC) {
        token = scanToken();
        orderAscendingList.add(Boolean.FALSE);
      }
      else if (token == ASC) {
        token = scanToken();
        orderAscendingList.add(Boolean.TRUE);
      }
      else
        orderAscendingList.add(Boolean.TRUE);
    } while (token == ',');

    if (token >= 0)
      throw error(L.l("extra token {0} at end of order-by",
                      tokenName(peekToken())));
  }
  
  /**
   * Parses the select method's query.
   *
   * @param query the source query string.
   */
  public EjbQuery parseQuery(String query)
    throws ConfigException
  {
    int token;

    _query = query;
    _fromList = new ArrayList<FromItem>();
    _pathMap = new HashMap<Expr,Expr>();
    _idMap = new HashMap<String,PathExpr>();
    _argList = new ArrayList<Expr>();
    //_linkList = new ArrayList<LinkItem>();

    _parseIndex = 0;
    _unique = 0;
    _token = -1;

    setConfig(_bean.getConfig());
    
    // First pass parses the from
    for (; (token = peekToken()) >= 0 && token != FROM; token = scanToken()) {
    }
    
    if (token != FROM)
      throw error(L.l("expected FROM at {0}",
                      tokenName(token)));

    scanToken();

    parseFrom();

    token = peekToken();
    if (token >= 0 && token != WHERE && token != ORDER)
      throw error(L.l("expected WHERE or ORDER at {0}",
                      tokenName(token)));

    _parseIndex = 0;
    _token = -1;
    token = scanToken();

    if (token != SELECT)
      throw error(L.l("expected SELECT at {0}", tokenName(token)));
    
    if (peekToken() == DISTINCT) {
      scanToken();
      _isDistinct = true;
    }

    _selectExpr = parseExpr();

    /*
    if (_selectExpr instanceof CollectionExpr) {
      CollectionExpr expr = (CollectionExpr) _selectExpr;
      
      _selectExpr = new CollectionIdExpr(this, "foo", expr);
    }
    */

    if (_selectExpr instanceof PathExpr)
      ((PathExpr) _selectExpr).setUsesField();

    /*
    if (! (selectExpr instanceof PathExpr) &&
        ! (selectExpr instanceof FieldExpr))
      throw error(L.l("'{0}' is an illegal SELECT expression.  Only path expressions are allowed in SELECT.", selectExpr));
    */
    
    token = peekToken();
    if (token != FROM)
      throw error(L.l("expected FROM at {0}", 
                      tokenName(token)));

    // skip over the from since it's been parsed
    for (;
         (token = peekToken()) >= 0 && token != WHERE && token != ORDER;
         token = scanToken()) {
    }

    _addArgToQuery = true;
    
    if ((token = scanToken()) == WHERE) {
      _isWhere = true;
      _whereExpr = parseExpr();
      _isWhere = false;
      token = scanToken();
    }

    if (_whereExpr != null && ! _whereExpr.isBoolean())
      throw error(L.l("WHERE must be a boolean expression at {0}",
                      _whereExpr));

    _addArgToQuery = false;
    
    int oldMaxArg = _maxArg;
    if (token == ORDER) {
      if (peekToken() == BY)
        scanToken();
      if (_orderExprList == null)
        _orderExprList = new ArrayList<Expr>();
      
      if (_orderAscendingList == null)
        _orderAscendingList = new ArrayList<Boolean>();

      do {
        _orderExprList.add(parseExpr());

        token = peekToken();
        if (token == DESC) {
          token = scanToken();
          _orderAscendingList.add(Boolean.FALSE);
        }
        else if (token == ASC) {
          token = scanToken();
          _orderAscendingList.add(Boolean.TRUE);
        }
        else
          _orderAscendingList.add(Boolean.TRUE);
      } while ((token = scanToken()) == ',');
    }

    if (token == OFFSET) {
      _limitOffset = parseExpr();
      token = scanToken();

      if (! _limitOffset.getJavaType().getName().equals("int"))
        throw error(L.l("OFFSET '{0}' must be an integer expression",
                        _limitMax));
    }

    if (token == LIMIT) {
      _limitMax = parseExpr();
      token = scanToken();

      if (! _limitMax.getJavaType().getName().equals("int"))
        throw error(L.l("LIMIT '{0}' must be an integer expression",
                        _limitMax));
    }

    if (token >= 0)
      throw error(L.l("extra token {0} at end of query",
                      tokenName(peekToken())));
    _maxArg = oldMaxArg;

    EjbSelectQuery ejbQuery = new EjbSelectQuery(_query);

    ejbQuery.setDistinct(_isDistinct);
    ejbQuery.setFromList(_fromIds);
    ejbQuery.setSelectExpr(_selectExpr);
    ejbQuery.setWhereExpr(_whereExpr);

    ejbQuery.setMaxArg(_maxArg);
    ejbQuery.setThisExpr(_thisExpr);

    ejbQuery.setOrderBy(_orderExprList, _orderAscendingList);
    
    ejbQuery.setOffset(_limitOffset);
    ejbQuery.setLimit(_limitMax);

    return ejbQuery;
  }

  /**
   * Parses the FROM block.  parseFrom's effect is to populate the
   * core identifiers.
   *
   * <pre>
   * from-list ::= from-list ',' from-item
   *           ::= from-item
   *
   * from-item ::= IDENTIFIER AS? IDENTIFIER
   *           ::= IN(collection-path) AS? IDENTIFIER
   * </pre>
   */
  private void parseFrom()
    throws ConfigException
  {
    boolean moreTables = true;

    while (moreTables) {
      int token = scanToken();

      if (token == IN) {
        if (scanToken() != '(')
          throw error(L.l("expected '(' at {0} while parsing IN(<collection-path>).",
                          tokenName(token)));

        parseFromCollection();
      }
      else if (token == IDENTIFIER) {
        String id = lexeme;
        if (peekToken() == AS)
          scanToken();
        
        parseFromTable(id);
      }
      else
        throw error(L.l("expected identifier at {0} while parsing FROM",
                        tokenName(token)));

      if (peekToken() == ',') {
        scanToken();
        moreTables = true;
      }
      else
        moreTables = false;
    }
  }

  /**
   * Parses a single-valued table identifier.
   */
  private void parseFromTable(String table)
    throws ConfigException
  {
    int token = scanToken();
    
    if (token != IDENTIFIER) {
      throw error(L.l("expected identifier at {0} while parsing FROM {1} [AS] var", tokenName(token), table));
    }

    String name = lexeme;

    EjbConfig ejbConfig = _bean.getConfig();

    EjbEntityBean entity = ejbConfig.findEntityBySchema(table);

    if (entity == null)
      throw error(L.l("'{0}' is an unknown entity-bean schema in 'FROM {0} AS {1}'",
                      table, name));

    // _bean.addBeanDepend(info.getEJBName());

    IdExpr id = new IdExpr(this, name, entity);

    addIdentifier(name, id);
    addFromItem(name, entity.getSQLTable());

    _fromIds.add(id);
  }

  /**
   * Parses a collection-valued table identifier.
   */
  private void parseFromCollection()
    throws ConfigException
  {
    Expr expr = parseDotExpr();

    if (scanToken() != ')')
      throw error(L.l("expected ')' at {0} while parsing IN(<collection-path>).",
                      tokenName(_token)));
    
    if (! (expr instanceof CollectionExpr))
      throw error(L.l("expected <collection-path> expression at '{0}'", expr));

    CollectionExpr collectionExpr = (CollectionExpr) expr;

    if (peekToken() == AS)
      scanToken();

    int token = scanToken();
    if (token != IDENTIFIER)
      throw error(L.l("expected identifier expression at {0} while parsing 'IN({1}) AS id'",
                      tokenName(token), expr));
      
    String name = lexeme;

    CollectionIdExpr idExpr;
    idExpr = new CollectionIdExpr(this, name, collectionExpr);

    addIdentifier(name, idExpr);
    
    _fromIds.add(idExpr);
  }
  
  /**
   * Parses the next expression.
   *
   * <pre>
   * expr ::= or-expr
   * </pre>
   *
   * @return the parsed expression
   */
  private Expr parseExpr()
    throws ConfigException
  {
    return parseOrExpr();
  }

  /**
   * Parses an or expression.
   *
   * <pre>
   * or-expr ::= or-expr AND and-expr
   *         ::= and-expr
   * </pre>
   *
   * @return the parsed expression
   */
  private Expr parseOrExpr()
    throws ConfigException
  {
    Expr expr = parseAndExpr();
    
    int token = peekToken();
    while ((token = peekToken()) == OR) {
      scanToken();

      expr = new BinaryExpr(this, token, expr, parseAndExpr());
    }

    return expr;
  }

  /**
   * Parses an and expression.
   *
   * <pre>
   * and-expr ::= and-expr AND not-expr
   *          ::= not-expr
   * </pre>
   *
   * @return the parsed expression
   */
  private Expr parseAndExpr()
    throws ConfigException
  {
    AndExpr oldAnd = _andExpr;
    
    AndExpr andExpr = new AndExpr(this);

    if (_isWhere)
      _andExpr = andExpr;
    
    Expr expr = parseNotExpr();

    andExpr.add(expr);
    
    int token = peekToken();
    while ((token = peekToken()) == AND) {
      scanToken();

      expr = parseNotExpr();

      andExpr.add(expr);
    }

    _andExpr = oldAnd;

    return andExpr.getSingleExpr();
  }

  /**
   * Parses a not expression.
   *
   * <pre>
   * not-expr ::= NOT? cmp-expr
   * </pre>
   *
   * @return the parsed expression
   */
  private Expr parseNotExpr()
    throws ConfigException
  {
    int token = peekToken();
    if (token == NOT) {
      scanToken();

      Expr expr = parseCmpExpr();
      
      return new UnaryExpr(NOT, expr);
    }
    else
      return parseCmpExpr();
  }

  /**
   * Parses a comparison expression.
   *
   * <pre>
   * cmp-expr ::= add-expr '=' add-expr is-term?
   *          ::= add-expr 'NOT'? 'BETWEEN' add-expr 'AND' add-expr is-term?
   *          ::= add-expr 'NOT'? 'LIKE' string ('ESCAPE' string)? is-term?
   *          ::= add-expr 'NOT'? 'IN' ('lit-1', ..., 'lit-n')
   *          ::= add-expr
   * </pre>
   *
   * @return the parsed expression
   */
  private Expr parseCmpExpr()
    throws ConfigException
  {
    int token = peekToken();
    boolean isNot = false;
    
    Expr expr = parseArithmeticExpr();
    
    token = peekToken();
    
    if (token == NOT) {
      scanToken();
      isNot = true;
      token = peekToken();
    }
    
    if (token >= EQ && token <= GE) {
      scanToken();
      
      return parseIs(new BinaryExpr(this, token, expr, parseAddExpr()));
    }
    else if (token == BETWEEN) {
      scanToken();
      
      Expr a = parseArithmeticExpr();

      if ((token = scanToken()) != AND)
        throw error(L.l("Expected 'AND' at {0}", tokenName(token)));

      Expr b = parseArithmeticExpr();

      return parseIs(new BetweenExpr(expr, a, b, isNot));
    }
    else if (token == LIKE) {
      scanToken();

      Expr pattern = parseArithmeticExpr();
      
      String escape = null;
      if (peekToken() == ESCAPE) {
        scanToken();
        
        if ((token = scanToken()) != STRING)
          throw error(L.l("Expected string at {0}", tokenName(token)));

        escape = lexeme.toString();
      }

      return parseIs(new LikeExpr(expr, pattern, escape, isNot));
    }
    else if (token == IN) {
      scanToken();
      token = scanToken();

      if (token != '(')
        throw error(L.l("Expected '(' after IN at {0}", tokenName(token)));

      ArrayList<Expr> args = new ArrayList<Expr>();
      while ((token = peekToken()) > 0 && token != ')') {
        Expr arg = parseArithmeticExpr();

        args.add(arg);

        token = peekToken();
        if (token == ',') {
          scanToken();
          token = peekToken();
        }
      }

      if (peekToken() != ')')
        throw error(L.l("Expected ')' after IN at {0}", tokenName(token)));

      scanToken();

      return new InExpr(this, expr, args, isNot);
    }
    else if (token == IS) {
      scanToken();

      if (isNot)
        throw error(L.l("'NOT IS' is an invalid expression."));

      token = scanToken();
      if (token == NOT) {
        isNot = true;
        token = scanToken();
      }

      if (token == NULL)
        return parseIs(new IsExpr(this, expr, NULL, isNot));
      else if (token == EMPTY) {
        if (! (expr instanceof CollectionExpr))
          throw error(L.l("IS EMPTY requires collection path at '{0}'",
                          expr));
        return parseIs(new EmptyExpr(expr, isNot));
      }
      else
        throw error(L.l("'{0}' unexpected after IS.", tokenName(token)));
    }
    else if (token == MEMBER) {
      scanToken();
      
      token = peekToken();
      if (token == OF)
        token = scanToken();

      Expr collection = parseDotExpr();
        
      return parseIs(new MemberExpr(isNot, expr, collection));
    }
    else
      return expr;
  }

  /**
   * Parses an 'IS' term
   *
   * <pre>
   * is-term ::= IS NOT? (TRUE|FALSE|UNKNOWN)
   * </pre>
   */
  private Expr parseIs(Expr base)
    throws ConfigException
  {
    if (peekToken() != IS)
      return base;

    scanToken();
    boolean isNot = peekToken() == NOT;
    if (isNot)
      scanToken();

    int token = scanToken();
    if (token == UNKNOWN)
      return new IsExpr(this, base, NULL, isNot);
    else if (token == TRUE)
      return isNot ? new UnaryExpr(NOT, base) : base;
    else if (token == FALSE)
      return isNot ? base : new UnaryExpr(NOT, base);

    throw error(L.l("expected TRUE or FALSE at {0}", tokenName(token)));
  }

  /**
   * Parses an arithmetic expression.
   *
   * <pre>
   * arithmetic-expr ::= add-expr
   * </pre>
   */
  private Expr parseArithmeticExpr()
    throws ConfigException
  {
    return parseAddExpr();
  }

  /**
   * Parses an addition expression.
   *
   * <pre>
   * add-expr ::= add-expr ('+' | '-') mul-expr
   *          ::= mul-expr
   * </pre>
   *
   * @return the parsed expression
   */
  private Expr parseAddExpr()
    throws ConfigException
  {
    Expr expr = parseMulExpr();
    
    int token = peekToken();
    while ((token = peekToken()) == '-' || token == '+') {
      scanToken();

      expr = new BinaryExpr(this, token, expr, parseMulExpr());
    }

    return expr;
  }

  /**
   * Parses a multiplication/division expression.
   *
   * <pre>
   * mul-expr ::= mul-expr ('*' | '/') unary-expr
   *          ::= unary-expr
   * </pre>
   *
   * @return the parsed expression
   */
  private Expr parseMulExpr()
    throws ConfigException
  {
    Expr expr = parseUnaryExpr();
    
    int token = peekToken();
    while ((token = peekToken()) == '*' || token == '/') {
      scanToken();

      expr = new BinaryExpr(this, token, expr, parseUnaryExpr());
    }

    return expr;
  }

  /**
   * Parses a unary +/-
   *
   * <pre>
   * unary-expr ::= ('+'|'-')? path-expr
   * </pre>
   *
   * @return the parsed expression
   */
  private Expr parseUnaryExpr()
    throws ConfigException
  {
    int token = peekToken();

    if (token == '+' || token == '-') {
      scanToken();
      return new UnaryExpr(token, parseRefExpr());
    }
    else
      return parseRefExpr();
  }

  /**
   * Parses a path expression.
   *
   * <pre>
   * ref-expr ::= path-expr '=>' IDENTIFIER
   *          ::= path-expr
   * </pre>
   *
   * @return the parsed expression
   */
  private Expr parseRefExpr()
    throws ConfigException
  {
    Expr expr = parseDotExpr();
    
    int token;
    if ((token = peekToken()) == EXTERNAL_DOT) {
      scanToken();

      token = scanToken();

      if (token != IDENTIFIER)
        throw error(L.l("expected field identifier at {0}",
                        tokenName(token)));

      expr = expr.newField(lexeme);
      expr.evalTypes();

      if (! expr.isExternal())
        throw error(L.l("'{0}' must refer to an external entity bean", expr));
    }

    return expr;
  }

  /**
   * Parses a path expression.
   *
   * <pre>
   * path-expr ::= path-expr '.' IDENTIFIER
   *           ::= term
   * </pre>
   *
   * @return the parsed expression
   */
  private Expr parseDotExpr()
    throws ConfigException
  {
    Expr expr = parseTerm();
    
    int token;
    while ((token = peekToken()) == '.') {
      scanToken();

      token = scanToken();

      if (token != IDENTIFIER)
        throw error(L.l("expected field identifier at {0}",
                        tokenName(token)));

      expr.evalTypes();
      if (expr.isExternal())
        throw error(L.l("'{0}' must not refer to an external entity bean", expr));
      Expr field = expr.newField(lexeme);
      expr = field;

      Expr equiv = _pathMap.get(field);
      if (equiv != null)
        expr = equiv;
      else
        _pathMap.put(field, field);
    }

    return expr;
  }

  /**
   * Parses a term
   *
   * <pre>
   * term ::= IDENTIFIER | INTEGER | LONG | DOUBLE | STRING
   *      ::= THIS '.' IDENTIFIER
   *      ::= IDENTIFIER '(' args ')'
   *      ::= '(' args ')'
   * </pre>
   */
  private Expr parseTerm()
    throws ConfigException
  {
    int token = scanToken();

    switch (token) {
    case IDENTIFIER:
      String name = lexeme.toString();
      if (peekToken() != '(')
        return getIdentifier(name);
      else
        return parseFunction(name);

    case FALSE:
      return new LiteralExpr(_booleanFalse, boolean.class);
      
    case TRUE:
      return new LiteralExpr(_booleanTrue, boolean.class);

    case INTEGER:
      return new LiteralExpr(lexeme, int.class);

    case LONG:
      return new LiteralExpr(lexeme, long.class);

    case DOUBLE:
      return new LiteralExpr(lexeme, double.class);

    case STRING:
      return new LiteralExpr(lexeme, String.class);

    case ARG:
    {
      ArgExpr arg = new ArgExpr(this, Integer.parseInt(lexeme));
      if (_addArgToQuery)
        addArg(arg);
      return arg;
    }

    case THIS:
    {
      if (_thisExpr == null) {
        _thisExpr = new IdExpr(this, "caucho_this", _bean);
        addFromItem("caucho_this", _bean.getSQLTable());
	_fromIds.add(_thisExpr);
        _argList.add(0, new ThisExpr(this, _bean));
      }

      return _thisExpr;
    }

    case '(':
      Expr expr = parseExpr();
      if ((token = scanToken()) != ')')
        throw error(L.l("expected ')' at {0}", tokenName(token)));

      return expr;
        

    default:
      throw error(L.l("expected term at {0}", tokenName(token)));
    }
  }

  /**
   * Parses a function
   *
   * <pre>
   * function ::= IDENTIFIER '(' expr (',' expr)* ')'
   *          ::= IDENTIFIER '(' ')'
   * </pre>
   */
  private Expr parseFunction(String name)
    throws ConfigException
  {
    ArrayList<Expr> args = new ArrayList<Expr>();

    int token;
    if ((token = scanToken()) != '(')
      throw error(L.l("expected '(' at {0} while parsing function {1}()", tokenName(token), name));

    while ((token = peekToken()) != ')' && token > 0) {
      Expr expr = parseExpr();

      args.add(expr);

      if ((token = peekToken()) == ',')
        scanToken();
    }

    if (token != ')')
      throw error(L.l("expected ')' at {0} while parsing function {1}", tokenName(token), name));

    scanToken();

    if (name.equalsIgnoreCase("object")) {
      if (args.size() != 1)
        throw error(L.l("OBJECT() requires a single argument"));
      
      Expr expr = args.get(0);

      if (! EntityBean.class.isAssignableFrom(expr.getJavaType())
          && ! EJBLocalObject.class.isAssignableFrom(expr.getJavaType()))
        throw error(L.l("OBJECT({0}) requires an entity bean as its argument at '{1}'",
                        expr, expr.getJavaType()));

      return expr;
    }
    else {
      try {
        return new FunExpr(name, args, _functions);
      } catch (ConfigException e) {
        throw error(e.getMessage());
      }
    }
  }

  /**
   * Adds a new identifier
   *
   * @param name the name of the identifier
   *
   * @return the IdExpr corresponding to the identifier
   */
  void addIdentifier(String name, PathExpr expr)
    throws ConfigException
  {
    Expr oldExpr = _idMap.get(name);
    
    if (oldExpr != null)
      throw error(L.l("'{0}' is defined twice", name));

    _idMap.put(name, expr);
  }

  /**
   * Adds a new identifier
   *
   * @param name the name of the identifier
   *
   * @return the IdExpr corresponding to the identifier
   */
  PathExpr getIdentifier(String name)
    throws ConfigException
  {
    PathExpr expr = _idMap.get(name);
    
    if (expr == null)
      throw error(L.l("'{0}' is an unknown identifier", name));

    return expr;
  }

  /**
   * Peeks the next token
   *
   * @return integer code for the token
   */
  private int peekToken()
    throws ConfigException
  {
    if (_token > 0)
      return _token;

    _token = scanToken();

    return _token;
  }
  
  /**
   * Scan the next token.  If the lexeme is a string, its string
   * representation is in "lexeme".
   *
   * @return integer code for the token
   */
  private int scanToken()
    throws ConfigException
  {
    if (_token > 0) {
      int value = _token;
      _token = -1;
      return value;
    }

    int sign = 1;
    int ch;

    for (ch = read(); Character.isWhitespace((char) ch); ch = read()) {
    }

    switch (ch) {
    case -1:
    case '(':
    case ')':
    case '.':
    case '*':
    case '/':
    case ',':
      return ch;
      
    case '+':
      if ((ch = read()) >= '0' && ch <= '9')
        break;
      else {
        unread(ch);
        return '+';
      }
        
    case '-':
      if ((ch = read()) >= '0' && ch <= '9') {
        sign = -1;
        break;
      }
      else {
        unread(ch);
        return '-';
      }
      
    case '=':
      if ((ch = read()) == '>')
        return EXTERNAL_DOT;
      else {
        unread(ch);
        return EQ;
      }

    case '<':
      if ((ch = read()) == '=')
        return LE;
      else if (ch == '>')
        return NE;
      else {
        unread(ch);
        return LT;
      }

    case '>':
      if ((ch = read()) == '=')
        return GE;
      else {
        unread(ch);
        return GT;
      }

    case '?':
      CharBuffer cb = CharBuffer.allocate();
      int index = 0;
      for (ch = read(); ch >= '0' && ch <= '9'; ch = read()) {
        cb.append((char) ch);
        index = 10 * index + ch - '0';
      }
      unread(ch);

      lexeme = cb.close();

      if (index <= 0)
        throw error(L.l("'{0}' must refer to a positive argument",
                        "?" + lexeme));

      if (_maxArg < index)
	_maxArg = index;
      
      return ARG;

      // @@ is useless?
    case '@':
      if ((ch = read()) != '@')
        throw error(L.l("'@' expected at {0}", charName(ch)));
      return scanToken();
    }

    if (Character.isJavaIdentifierStart((char) ch)) {
      CharBuffer cb = CharBuffer.allocate();

      for (; ch > 0 && Character.isJavaIdentifierPart((char) ch); ch = read())
        cb.append((char) ch);

      unread(ch);

      lexeme = cb.close();
      String lower = lexeme.toLowerCase();

      int token = _reserved.get(lower);

      if (token > 0)
        return token;
      else
        return IDENTIFIER; 
    }
    else if (ch >= '0' && ch <= '9') {
      CharBuffer cb = CharBuffer.allocate();

      int type = INTEGER;
      
      if (sign < 0)
        cb.append('-');

      for (; ch >= '0' && ch <= '9'; ch = read())
        cb.append((char) ch);

      if (ch == '.') {
        type = DOUBLE;
        
        cb.append('.');
        for (ch = read(); ch >= '0' && ch <= '9'; ch = read())
          cb.append((char) ch);
      }

      if (ch == 'e' || ch == 'E') {
        type = DOUBLE;

        cb.append('e');
        if ((ch = read()) == '+' || ch == '-') {
          cb.append((char) ch);
          ch = read();
        }
        
        if (! (ch >= '0' && ch <= '9'))
          throw error(L.l("exponent needs digits at {0}",
                          charName(ch)));
          
        for (; ch >= '0' && ch <= '9'; ch = read())
          cb.append((char) ch);
      }

      if (ch == 'F' || ch == 'D')
        type = DOUBLE;
      else if (ch == 'L') {
        type = LONG;
      }
      else
        unread(ch);

      lexeme = cb.close();

      return type;
    }
    else if (ch == '\'') {
      CharBuffer cb = CharBuffer.allocate();

      cb.append("'");
      for (ch = read(); ch >= 0; ch = read()) {
        if (ch == '\'') {
          if ((ch = read()) == '\'')
            cb.append("''");
          else {
            unread(ch);
            break;
          }
        }
        else
          cb.append((char) ch);
      }
      cb.append("'");

      lexeme = cb.close();

      return STRING;
    }

    throw error(L.l("unexpected char at {0}", "" + (char) ch));
  }

  /**
   * Returns the next character.
   */
  private int read()
  {
    if (_parseIndex < _query.length())
      return _query.charAt(_parseIndex++);
    else
      return -1;
  }

  /**
   * Unread the last character.
   */
  private void unread(int ch)
  {
    if (ch >= 0)
      _parseIndex--;
  }

  /**
   * Returns a full method name with arguments.
   */
  private String getFullMethodName(ApiMethod method)
  {
    return method.getFullName();
  }
  
  /**
   * Returns a full method name with arguments.
   */
  private String getFullMethodName(String methodName, Class []params)
  {
    String name = methodName + "(";

    for (int i = 0; i < params.length; i++) {
      if (i != 0)
        name += ", ";

      name += params[i].getSimpleName();
    }

    return name + ")";
  }

  /**
   * Returns a printable version of a class.
   */
  private String getClassName(Class cl)
  {
    if (cl.isArray())
      return getClassName(cl.getComponentType()) + "[]";
    else if (cl.getName().startsWith("java")) {
      int p = cl.getName().lastIndexOf('.');

      return cl.getName().substring(p + 1);
    }
    else
      return cl.getName();
  }

  /**
   * Creates an error.
   */
  public ConfigException error(String msg)
  {
    msg += "\nin \"" + _query + "\"";
    /*
    if (_qlConfig != null)
      return new SelectLineParseException(_qlConfig.getFilename() + ":" +
					  _qlConfig.getLine() + ": " +
					  msg);
    */
    if (_location != null)
      return new LineConfigException(_location + msg);
    else
      return new ConfigException(msg);
  }

  /**
   * Returns the name for a character
   */
  private String charName(int ch)
  {
    if (ch < 0)
      return L.l("end of query");
    else
      return String.valueOf((char) ch);
  }
  
  /**
   * Returns the name of a token
   */
  private String tokenName(int token)
  {
    switch (token) {
    case AS: return "AS";
    case FROM: return "FROM";
    case IN: return "IN";
    case SELECT: return "SELECT";
    case WHERE: return "WHERE";
    case OR: return "OR";
    case AND: return "AND";
    case NOT: return "NOT";
    case BETWEEN: return "BETWEEN";
    case THIS: return "THIS";
    case TRUE: return "FALSE";
    case EMPTY: return "EMPTY";
    case MEMBER: return "MEMBER";
    case OF: return "OF";
    case NULL: return "NULL";
    case ORDER: return "ORDER";
    case BY: return "BY";
    case ASC: return "ASC";
    case DESC: return "DESC";
    case LIMIT: return "LIMIT";
      
    case EXTERNAL_DOT: return "=>";

    case -1:
      return L.l("end of query");
      
    default:
      if (token < 128)
        return "'" + String.valueOf((char) token) + "'";
      else
        return "'" + lexeme + "'";
    }
  }

  public static ArrayList<FunctionSignature> getStandardFunctions()
  {
    return FunExpr.getStandardFunctions();
  }

  /**
   * Returns a debuggable description of the select.
   */
  public String toString()
  {
    return "QLParser[" + getMethod() + "]";
  }

  public boolean equals(Object b)
  {
    if (! (b instanceof QLParser))
      return false;

    QLParser bSel = (QLParser) b;

    if (_bean != bSel._bean)
      return false;

    return getMethod().equals(bSel.getMethod());
  }

  static boolean methodEquals(ApiMethod a, ApiMethod b)
  {
    return a.equals(b);
  }

  static class FromItem {
    String _id;
    String _table;

    FromItem(String id, String table)
    {
      _id = id;
      _table = table;
    }
  }
  
  static class LinkItem {
    String columnA;
    String tableA;
    
    String columnB;
    String tableB;

    LinkItem(String tableA, String columnA,
             String tableB, String columnB)
    {
      this.columnA = columnA;
      this.tableA = tableA;
      this.columnB = columnB;
      this.tableB = tableB;
    }

    public boolean equals(Object o)
    {
      if (! (o instanceof LinkItem))
        return false;

      LinkItem link = (LinkItem) o;

      if (tableA.equals(link.tableA) && columnA.equals(link.columnA) &&
          tableB.equals(link.tableB) && columnB.equals(link.columnB))
        return true;
      else if (tableA.equals(link.tableB) && columnA.equals(link.columnB) &&
          tableB.equals(link.tableA) && columnB.equals(link.columnA))
        return true;
      else
        return false;
    }
  }

  static {
    _reserved = new IntMap();
    _reserved.put("as", AS);
    _reserved.put("from", FROM);
    _reserved.put("in", IN);
    _reserved.put("select", SELECT);
    _reserved.put("distinct", DISTINCT);
    _reserved.put("where", WHERE);
    _reserved.put("order", ORDER);
    _reserved.put("by", BY);
    _reserved.put("asc", ASC);
    _reserved.put("desc", DESC);
    _reserved.put("limit", LIMIT);
    _reserved.put("offset", OFFSET);
    
    _reserved.put("or", OR);
    _reserved.put("and", AND);
    _reserved.put("not", NOT);
    
    _reserved.put("between", BETWEEN);
    _reserved.put("like", LIKE);
    _reserved.put("escape", ESCAPE);
    _reserved.put("is", IS);
    
    _reserved.put("this", THIS);
    _reserved.put("true", TRUE);
    _reserved.put("false", FALSE);
    _reserved.put("unknown", UNKNOWN);
    _reserved.put("empty", EMPTY);
    _reserved.put("member", MEMBER);
    _reserved.put("of", OF);
    _reserved.put("null", NULL);
  }
}
