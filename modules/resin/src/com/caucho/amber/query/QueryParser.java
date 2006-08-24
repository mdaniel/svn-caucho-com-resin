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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber.query;

import java.util.ArrayList;
import java.util.HashMap;

import java.util.logging.Logger;

import java.sql.SQLException;

import com.caucho.util.L10N;
import com.caucho.util.IntMap;
import com.caucho.util.CharBuffer;

import com.caucho.log.Log;

import com.caucho.amber.AmberException;

import com.caucho.amber.entity.AmberEntityHome;

import com.caucho.amber.manager.AmberPersistenceUnit;

import com.caucho.amber.table.Table;
import com.caucho.amber.table.LinkColumns;

import com.caucho.amber.type.EntityType;

import com.caucho.amber.field.AmberField;

/**
 * Contains the parser for EJB 3.0 style queries and stores
 * the parsed expressions.
 */
public class QueryParser {
  static final Logger log = Log.open(QueryParser.class);
  static final L10N L = new L10N(QueryParser.class);

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
  final static int UPDATE = SELECT + 1;
  final static int DELETE = UPDATE + 1;
  final static int DISTINCT = DELETE + 1;
  final static int WHERE = DISTINCT + 1;
  final static int AS = WHERE + 1;
  final static int SET = AS + 1;
  final static int ORDER = SET + 1;
  final static int GROUP = ORDER + 1;
  final static int BY = GROUP + 1;
  final static int ASC = BY + 1;
  final static int DESC = ASC + 1;
  final static int LIMIT = DESC + 1;
  final static int OFFSET = LIMIT + 1;

  final static int JOIN = OFFSET + 1;
  final static int INNER = JOIN + 1;
  final static int LEFT = INNER + 1;
  final static int OUTER = LEFT + 1;
  final static int FETCH = OUTER + 1;

  final static int BETWEEN = FETCH + 1;
  final static int LIKE = BETWEEN + 1;
  final static int ESCAPE = LIKE + 1;
  final static int IS = ESCAPE + 1;

  final static int CONCAT_OP = IS + 1;

  final static int EQ = CONCAT_OP + 1;
  final static int NE = EQ + 1;
  final static int LT = NE + 1;
  final static int LE = LT + 1;
  final static int GT = LE + 1;
  final static int GE = GT + 1;

  final static int AND = GE + 1;
  final static int OR = AND + 1;
  final static int NOT = OR + 1;

  final static int LENGTH = NOT + 1;
  final static int LOCATE = LENGTH + 1;

  final static int ABS = LOCATE + 1;
  final static int SQRT = ABS + 1;
  final static int MOD = SQRT + 1;
  final static int SIZE = MOD + 1;

  final static int CONCAT = SIZE + 1;
  final static int LOWER = CONCAT + 1;
  final static int UPPER = LOWER + 1;
  final static int SUBSTRING = UPPER + 1;
  final static int TRIM = SUBSTRING + 1;

  final static int CURRENT_DATE = TRIM + 1;
  final static int CURRENT_TIME = CURRENT_DATE + 1;
  final static int CURRENT_TIMESTAMP = CURRENT_TIME + 1;

  final static int EXTERNAL_DOT = CURRENT_TIMESTAMP + 1;

  final static int ARG = EXTERNAL_DOT + 1;
  final static int NAMED_ARG = ARG + 1;

  final static int NEW = NAMED_ARG + 1;

  final static int THIS = NEW + 1;

  final static int NOT_NULL = THIS + 1;

  final static int HAVING = NOT_NULL + 1;

  private static IntMap _reserved;

  private AmberPersistenceUnit _amberPersistenceUnit;

  // The query
  private String _sql;

  /*
  // list of the relation links
  private ArrayList<LinkItem> _linkList;
  // select expression
  private Expr _selectExpr;
  // is distinct (set)
  private boolean _isDistinct;
  */

  // True if entities should be lazily loaded
  private boolean _isLazyResult;

  // The select query
  private AbstractQuery _query;

  // list of relations
  private HashMap<PathExpr,PathExpr> _pathMap
    = new HashMap<PathExpr,PathExpr>();

  // parse index
  private int _parseIndex;
  // current token
  private int _token;
  // unique
  private int _unique;
  // parameter count
  private int _parameterCount;
  // temp for parsing
  private String _lexeme;

  private ArrayList<ArgExpr> _argList = new ArrayList<ArgExpr>();

  private HashMap<AmberExpr, String> _joinFetchMap;

  private int _sqlArgCount;

  private FromItem.JoinSemantics _joinSemantics
    = FromItem.JoinSemantics.UNKNOWN;

  private boolean _isJoinFetch = false;

  /**
   * Creates the query parser.
   */
  public QueryParser(String query)
  {
    _sql = query;
  }

  /**
   * Sets the entity home manager.
   */
  public void setAmberManager(AmberPersistenceUnit amberPersistenceUnit)
  {
    _amberPersistenceUnit = amberPersistenceUnit;
  }

  /**
   * Sets true for lazy loading.
   */
  public void setLazyResult(boolean isLazy)
  {
    _isLazyResult = isLazy;
  }

  /**
   * Returns the query string
   */
  public String getQuery()
  {
    return _sql;
  }

  /**
   * Returns the query string
   */
  public AbstractQuery getSelectQuery()
  {
    return _query;
  }

  /**
   * Initialize the parse.
   */
  private void init()
  {
    _parseIndex = 0;
    _unique = 0;
    _token = -1;
    _joinFetchMap = new HashMap<AmberExpr, String>();
  }

  /**
   * Generates a new arg.
   */
  int generateSQLArg()
  {
    return _sqlArgCount++;
  }

  /**
   * Parses the query.
   */
  public AbstractQuery parse()
    throws AmberException
  {
    /*
      _query = query;
      _fromList = new ArrayList<FromItem>();
      _pathMap = new HashMap<Expr,Expr>();
      _idMap = new HashMap<String,PathExpr>();
      _argList = new ArrayList<Expr>();
      _linkList = new ArrayList<LinkItem>();
    */

    init();

    int token = scanToken();
    if (token == UPDATE)
      return parseUpdate();
    else if (token == DELETE)
      return parseDelete();

    _token = token;
    return parseSelect(false);
  }

  private SelectQuery parseSelect(boolean innerSelect)
    throws QueryParseException
  {
    int oldParseIndex = _parseIndex;
    int oldToken = _token;
    FromItem.JoinSemantics oldJoinSemantics = _joinSemantics;
    boolean oldIsJoinFetch = _isJoinFetch;

    SelectQuery query = new SelectQuery(_sql);
    query.setParentQuery(_query);
    _query = query;

    int token;
    while ((token = scanToken()) >= 0 && token != FROM) {
    }

    boolean hasFrom = (token == FROM) ? true : false;

    _token = token;

    if (hasFrom) {

      do {

        scanToken();

        _isJoinFetch = false;

        if (token == JOIN) {
          if ((token = peekToken()) == FETCH) {
            scanToken();
            _isJoinFetch = true;
          }
        }

        FromItem from = parseFrom();

        token = peekToken();

        _joinSemantics = FromItem.JoinSemantics.UNKNOWN;

        if (token == INNER) {
          scanToken();

          token = peekToken();

          if (token != JOIN) {
            throw error(L.l("expected JOIN at {0}", tokenName(token)));
          }

          _joinSemantics = FromItem.JoinSemantics.INNER;
        }
        else if (token == LEFT) {
          scanToken();

          token = peekToken();

          if (token == OUTER) {
            scanToken();

            token = peekToken();
          }

          if (token != JOIN)
            throw error(L.l("expected JOIN at {0}", tokenName(token)));

          _joinSemantics = FromItem.JoinSemantics.OUTER;
        }
        else if (token == JOIN) {
          _joinSemantics = FromItem.JoinSemantics.INNER;
        }

      } while ((token == ',') ||
               (token == JOIN));
    }

    int fromParseIndex = _parseIndex;
    int fromToken = _token;

    _parseIndex = oldParseIndex;
    _token = oldToken;

    ArrayList<AmberExpr> resultList = new ArrayList<AmberExpr>();

    if (peekToken() == SELECT) {
      scanToken();

      if (peekToken() == DISTINCT) {
        scanToken();
        query.setDistinct(true);
      }

      String constructorName = null;

      if (peekToken() == NEW) {

        scanToken();

        // Scans the fully qualified constructor

        String s = "";

        boolean isDot = false;

        while ((token = scanToken()) != '(') {

          if (! isDot) {
            s += _lexeme;
            isDot = true;
          }
          else if (token == '.') {
            s += '.';
            isDot = false;
          }
          else
            throw error(L.l("Constructor with SELECT NEW must be fully qualified. Expected '.' at {0}", tokenName(token)));
        }

        constructorName = s;
      }

      do {

        AmberExpr expr = parseExpr();

        if (! hasFrom) {
          if (! (expr instanceof DateTimeFunExpr))
            throw error(L.l("expected FROM clause when the select clause has not date/time functions only"));
        }
        else {

          expr = expr.bindSelect(this);

          if (_isLazyResult) {
          }
          else if (expr instanceof PathExpr) {
            PathExpr pathExpr = (PathExpr) expr;

            expr = new LoadEntityExpr(pathExpr);

            expr = expr.bindSelect(this);
          }
        }

        resultList.add(expr);
      } while ((token = scanToken()) == ',');

      query.setHasFrom(hasFrom);

      if (hasFrom && (constructorName != null)) {

        if (token != ')')
          throw error(L.l("Expected ')' at {0} when calling constructor with SELECT NEW", tokenName(token)));

        token = scanToken();

        try {

          ClassLoader loader = Thread.currentThread().getContextClassLoader();

          Class cl = Class.forName(constructorName, false, loader);

          query.setConstructorClass(cl);

        } catch (ClassNotFoundException ex) {
          throw error(L.l("Unable to find class {0}. Make sure the class is fully qualified.", constructorName));
        }
      }

      _token = token;
    }

    if (hasFrom && (peekToken() != FROM))
      throw error(L.l("expected FROM at {0}", tokenName(token)));

    if (resultList.size() == 0) {

      if (_joinFetchMap.size() > 0)
        throw error(L.l("All associations referenced by JOIN FETCH must belong to an entity that is returned as a result of the query"));

      ArrayList<FromItem> fromList = _query.getFromList();

      if (fromList.size() > 0) {
        FromItem fromItem = fromList.get(0);

        AmberExpr expr = fromItem.getIdExpr();

        if (_isLazyResult) {
        }
        else if (expr instanceof PathExpr) {
          PathExpr pathExpr = (PathExpr) expr;

          expr = new LoadEntityExpr(pathExpr);
          expr = expr.bindSelect(this);
        }

        resultList.add(expr);
      }
    }
    else if (hasFrom) {

      int size = resultList.size();

      int matches = 0;

      for (int i = 0; i < size; i++) {

        AmberExpr expr = resultList.get(i);

        if (expr instanceof LoadEntityExpr) {

          expr = ((LoadEntityExpr) expr).getExpr();

          if (_joinFetchMap.get(expr) != null) {
            matches++;
          }
        }
      }

      if (matches < _joinFetchMap.size())
        throw error(L.l("All associations referenced by JOIN FETCH must belong to an entity that is returned as a result of the query"));

    }

    query.setResultList(resultList);

    _parseIndex = fromParseIndex;
    _token = fromToken;

    token = peekToken();

    if (token == WHERE) {
      scanToken();

      query.setWhere(parseExpr().createBoolean().bindSelect(this));
    }

    boolean hasGroupBy = false;

    token = peekToken();
    if (token == GROUP) {
      scanToken();

      if (peekToken() == BY) {
        scanToken();
        hasGroupBy = true;
      }

      ArrayList<AmberExpr> groupList = new ArrayList<AmberExpr>();

      while (true) {
        groupList.add(parseExpr());

        if (peekToken() == ',')
          scanToken();
        else
          break;
      }

      query.setGroupList(groupList);
    }

    token = peekToken();
    if (token == HAVING) {

      if (! hasGroupBy)
        throw error(L.l("Use of HAVING without GROUP BY is not currently supported"));

      scanToken();

      query.setHaving(parseExpr().createBoolean().bindSelect(this));
    }

    token = peekToken();
    if (token == ORDER) {
      scanToken();

      if (peekToken() == BY)
        scanToken();

      ArrayList<AmberExpr> orderList = new ArrayList<AmberExpr>();
      ArrayList<Boolean> ascList = new ArrayList<Boolean>();

      while (true) {
        AmberExpr expr = parseExpr();
        expr = expr.bindSelect(this);

        orderList.add(expr);

        if (peekToken() == DESC) {
          scanToken();
          ascList.add(Boolean.FALSE);
        }
        else if (peekToken() == ASC) {
          scanToken();
          ascList.add(Boolean.TRUE);
        }
        else
          ascList.add(Boolean.TRUE);

        if (peekToken() == ',')
          scanToken();
        else
          break;
      }

      query.setOrderList(orderList, ascList);
    }

    token = peekToken();

    if (! innerSelect) {

      query.setJoinFetchMap(_joinFetchMap);

      if (token > 0)
        throw error(L.l("expected end of query at {0}", tokenName(token)));

      if (! query.setArgList(_argList.toArray(new ArgExpr[_argList.size()])))
        throw error(L.l("Unable to parse all query parameters. Make sure named parameters are not mixed with positional parameters"));

    }

    try {
      query.init();
    } catch (SQLException e) {
      throw new QueryParseException(e);
    }

    _joinSemantics = oldJoinSemantics;
    _isJoinFetch = oldIsJoinFetch;

    return query;
  }

  private AbstractQuery parseUpdate()
    throws QueryParseException
  {
    UpdateQuery query = new UpdateQuery(_sql);
    _query = query;

    FromItem fromItem = parseFrom();

    int token = scanToken();
    if (token != SET)
      throw error(L.l("expected 'SET' at {0}", tokenName(token)));

    ArrayList<AmberField> fields = new ArrayList<AmberField>();
    ArrayList<AmberExpr> values = new ArrayList<AmberExpr>();

    parseSetValues(fromItem, fields, values);

    query.setFieldList(fields);
    query.setValueList(values);

    token = scanToken();
    if (token == WHERE) {
      query.setWhere(parseExpr().createBoolean().bindSelect(this));

      token = scanToken();
    }

    if (token >= 0)
      throw error(L.l("'{0}' not expected at end of query.", tokenName(token)));

    query.init();

    query.setArgList(_argList.toArray(new ArgExpr[_argList.size()]));

    return query;
  }

  private AbstractQuery parseDelete()
    throws QueryParseException
  {
    DeleteQuery query = new DeleteQuery(_sql);
    _query = query;

    int token = peekToken();
    if (token == FROM)
      scanToken();

    FromItem fromItem = parseFrom();

    token = scanToken();
    if (token == WHERE) {
      AmberExpr where = parseExpr();

      where = where.bindSelect(this);

      query.setWhere(where);

      token = scanToken();
    }

    if (token >= 0)
      throw error(L.l("'{0}' not expected at end of query.", tokenName(token)));

    query.init();

    query.setArgList(_argList.toArray(new ArgExpr[_argList.size()]));

    return query;
  }

  /**
   * Parses the set values.
   */
  private void parseSetValues(FromItem fromItem,
                              ArrayList<AmberField> fields,
                              ArrayList<AmberExpr> values)
    throws QueryParseException
  {
    EntityType entity = fromItem.getEntityType();

    int token;

    do {
      String fieldName = parseIdentifier();

      AmberField field = entity.getField(fieldName);

      if (field == null)
        throw error(L.l("'{0}' is an unknown field in entity {1}.",
                        fieldName, entity.getName()));

      token = scanToken();
      if (token != EQ)
        throw error(L.l("expected '=' at {0}", tokenName(token)));

      AmberExpr expr = parseExpr();

      fields.add(field);
      values.add(expr);
    } while ((token = scanToken()) == ',');

    _token = token;
  }

  /**
   * Parses the FROM block.  parseFrom's effect is to populate the
   * core identifiers.
   *
   * <pre>
   * from-item ::= schema AS? IDENTIFIER
   * </pre>
   */
  private FromItem parseFrom()
    throws QueryParseException
  {
    SchemaExpr schema = parseSchema();

    String id;

    int token = peekToken();
    if (token == AS) {
      scanToken();
      token = peekToken();
      id = parseIdentifier();
    }
    else if (token == IDENTIFIER)
      id = parseIdentifier();
    else {
      id = schema.getTailName();
    }

    /*
      AmberEntityHome home = _amberPersistenceUnitenceUnitenceUnit.getHomeBySchema(schema);

      if (home == null)
      throw error(L.l("`{0}' is an unknown persistent class.",
      schema));
    */

    return schema.addFromItem(this, id);
  }

  /**
   * Adds a new FromItem.
   */
  FromItem addFromItem(Table table)
  {
    return addFromItem(table, createTableName());
  }

  /**
   * Returns a unique table name
   */
  String createTableName()
  {
    return "caucho" + _unique++;
  }

  /**
   * Adds a new FromItem.
   */
  FromItem addFromItem(Table table, String id)
  {
    if (id == null)
      id = createTableName();

    FromItem item = _query.createFromItem(table, id);

    item.setJoinSemantics(_joinSemantics);

    return item;
  }

  /**
   * Adds a new FromItem.
   */
  FromItem createDependentFromItem(FromItem item, LinkColumns link)
  {
    item = _query.createDependentFromItem(item, link, createTableName());

    item.setJoinSemantics(_joinSemantics);

    return item;
  }

  /**
   * Adds a new link
   */
  void addLink(AmberExpr expr)
  {
    // _andExpr.add(expr);
    throw new IllegalStateException();
  }

  /**
   * Adds an entity path
   */
  PathExpr addPath(PathExpr path)
  {
    PathExpr oldPath = _pathMap.get(path);

    if (oldPath != null)
      return oldPath;

    _pathMap.put(path, path);

    return path;
  }

  /**
   * Adds a new argument
   */
  void addArg(ArgExpr arg)
  {
    _argList.add(arg);
  }

  /**
   * Parses a schema.
   */
  private SchemaExpr parseSchema()
    throws QueryParseException
  {
    int token = peekToken();
    boolean isIn = token == IN;

    if (isIn) {

      scanToken();

      _joinSemantics = FromItem.JoinSemantics.INNER;

      if ((token = scanToken()) != '(')
        throw error(L.l("expected '(' at '{0}'", tokenName(token)));
    }

    String name = parseIdentifier();

    SchemaExpr schema = null;

    if (! isIn) {
      AmberEntityHome home = _amberPersistenceUnit.getHomeBySchema(name);

      if (home != null) {
        EntityType type = home.getEntityType();

        schema = new TableIdExpr(home.getEntityType(),
                                 type.getTable().getName());
      }
    }

    IdExpr id = null;

    if (schema == null) {
      id = getIdentifier(name);

      if (id != null)
        schema = new FromIdSchemaExpr(id);
    }

    if (! isIn && schema == null) {
      while (peekToken() == '.') {
        scanToken();
        String segment = parseIdentifier();

        name = name + '.' + segment;

        AmberEntityHome home = _amberPersistenceUnit.getHomeBySchema(name);

        if (home != null) {
          schema = new TableIdExpr(home.getEntityType(), name);
          break;
        }
      }
    }

    if (schema == null) {
      throw error(L.l("'{0}' is an unknown entity.",
                      name));
    }

    name = "";
    boolean isFirst = true;

    while (peekToken() == '.') {
      scanToken();
      String segment = parseIdentifier();

      if (isFirst) {
        name += segment;
        isFirst = false;
      }
      else
        name += "." + segment;

      schema = schema.createField(this, segment);
    }

    if (_isJoinFetch && (! name.equals(""))) {
      _joinFetchMap.put(id, name);
    }

    if (isIn) {
      if ((token = scanToken()) != ')')
        throw error(L.l("expected ')' at '{0}'", tokenName(token)));
    }

    return schema;
  }

  /**
   * Parses an expression.
   */
  private AmberExpr parseExpr()
    throws QueryParseException
  {
    if (peekToken() == SELECT) {
      SelectQuery select = parseSelect(true);

      return new SubSelectExpr(select);
    }

    AmberExpr expr = parseOrExpr();

    return expr; // .bindSelect(this);
  }

  /**
   * Parses an or expression.
   */
  private AmberExpr parseOrExpr()
    throws QueryParseException
  {
    AmberExpr expr = parseAndExpr();
    OrExpr orExpr = null;

    while (peekToken() == OR) {
      scanToken();

      if (orExpr == null) {
        orExpr = new OrExpr();
        orExpr.add(expr);
      }

      orExpr.add(parseAndExpr());
    }

    return orExpr == null ? expr : orExpr;
  }

  /**
   * Parses an and expression.
   */
  private AmberExpr parseAndExpr()
    throws QueryParseException
  {
    AmberExpr expr = parseNotExpr();
    AndExpr andExpr = null;

    while (peekToken() == AND) {
      scanToken();

      if (andExpr == null) {
        andExpr = new AndExpr();
        andExpr.add(expr);
      }

      andExpr.add(parseNotExpr());
    }

    return andExpr == null ? expr : andExpr;
  }

  /**
   * Parses a NOT expression.
   *
   */
  private AmberExpr parseNotExpr()
    throws QueryParseException
  {
    if (peekToken() == NOT) {
      scanToken();

      return new UnaryExpr(NOT, parseCmpExpr());
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
  private AmberExpr parseCmpExpr()
    throws QueryParseException
  {
    AmberExpr expr = parseConcatExpr();

    int token = peekToken();
    boolean isNot = false;

    if (token == NOT) {
      scanToken();
      isNot = true;
      token = peekToken();

      if (token != BETWEEN &&
          token != LIKE &&
          token != MEMBER &&
          token != IN)
        throw error(L.l("'NOT' is not expected here."));
    }

    if (token >= EQ && token <= GE) {
      scanToken();

      return parseIs(new BinaryExpr(token, expr, parseConcatExpr()));
    }
    else if (token == BETWEEN) {
      scanToken();

      AmberExpr min = parseConcatExpr();

      if ((token = scanToken()) != AND)
        throw error(L.l("Expected 'AND' at {0}", tokenName(token)));

      AmberExpr max = parseConcatExpr();

      return new BetweenExpr(expr, min, max, isNot);
    }
    else if (token == LIKE) {
      scanToken();

      AmberExpr pattern = parseConcatExpr();

      String escape = null;
      if (peekToken() == ESCAPE) {
        scanToken();

        if ((token = scanToken()) != STRING)
          throw error(L.l("Expected string at {0}", tokenName(token)));

        escape = _lexeme.toString();
      }

      return parseIs(new LikeExpr(expr, pattern, escape, isNot));
    }
    else if (token == IN) {
      scanToken();
      token = scanToken();

      if (token != '(')
        throw error(L.l("Expected '(' after IN at {0}", tokenName(token)));

      ArrayList<AmberExpr> args = new ArrayList<AmberExpr>();
      while ((token = peekToken()) > 0 && token != ')') {
        AmberExpr arg = parseExpr();

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

      return new InExpr(expr, args, isNot);
    }
    else if (token == MEMBER) {
      scanToken();

      token = peekToken();
      if (token == OF)
        token = scanToken();

      AmberExpr collection = parseExpr();

      if (! (expr instanceof PathExpr))
        throw error(L.l("MEMBER OF requires an entity-valued item."));

      // ManyToMany is implemented as a
      // ManyToOne[embeddeding OneToMany]
      if ((collection instanceof ManyToOneExpr) &&
          (((ManyToOneExpr) collection).getParent() instanceof OneToManyExpr)) {
      }
      else if (collection instanceof OneToManyExpr) {
      }
      else if (collection instanceof CollectionIdExpr) {
      }
      else {
        throw error(L.l("MEMBER OF requires an entity-valued collection at '{0}'.",
                        collection.getClass().getName()));
      }

      return parseIs(MemberExpr.create(this,
                                       (PathExpr) expr,
                                       collection,
                                       isNot));
    }
    else
      return parseIs(expr);
  }

  private AmberExpr parseIs(AmberExpr expr)
    throws QueryParseException
  {
    int token = peekToken();

    if (token != IS)
      return expr;

    scanToken();

    boolean isNot = false;
    token = scanToken();

    if (token == NOT) {
      isNot = true;
      token = scanToken();
    }

    if (token == NULL) {

      if (expr instanceof KeyColumnExpr)
        expr = ((KeyColumnExpr) expr).getParent();

      if (isNot)
        return new UnaryExpr(NOT_NULL, expr);
      else
        return new UnaryExpr(NULL, expr);
    }
    else if (token == EMPTY) {

      expr = new EmptyExpr(expr);

      if (! isNot)
        expr = new UnaryExpr(NOT, expr);

      return expr;
    }
    else
      throw error(L.l("expected NULL at '{0}'", tokenName(token)));
  }

  /**
   * Parses a concat expression.
   */
  private AmberExpr parseConcatExpr()
    throws QueryParseException
  {
    AmberExpr expr = parseAddExpr();

    while (true) {
      int token = peekToken();

      switch (token) {
      case CONCAT_OP:
        scanToken();
        expr = new ConcatExpr(expr, parseAddExpr());
        break;
      default:
        return expr;
      }
    }
  }

  /**
   * Parses an add expression.
   */
  private AmberExpr parseAddExpr()
    throws QueryParseException
  {
    AmberExpr expr = parseMulExpr();

    while (true) {
      int token = peekToken();

      switch (token) {
      case '+':
      case '-':
        scanToken();
        expr = new BinaryExpr(token, expr, parseMulExpr());
        break;
      default:
        return expr;
      }
    }
  }

  /**
   * Parses a mul expression.
   */
  private AmberExpr parseMulExpr()
    throws QueryParseException
  {
    AmberExpr expr = parseTerm();

    while (true) {
      int token = peekToken();

      switch (token) {
      case '*':
      case '/':
        scanToken();
        expr = new BinaryExpr(token, expr, parseTerm());
        break;
      default:
        return expr;
      }
    }
  }

  /**
   * Parses a term
   *
   * <pre>
   * term ::= - term
   *      ::= + term
   *      ::= NOT term
   * </pre>
   */
  private AmberExpr parseTerm()
    throws QueryParseException
  {
    int token = peekToken();

    switch (token) {
    case '+':
    case '-':
    case NOT:
      scanToken();

      return new UnaryExpr(token, parseTerm());

    default:
      return parseSimpleTerm();
    }
  }

  /**
   * Parses a simple term
   *
   * <pre>
   * term ::= INTEGER | LONG | DOUBLE | STRING
   *      ::= THIS
   *      ::= IDENTIFIER
   *      ::= IDENTIFIER '(' args ')'
   *      ::= '(' expr ')'
   * </pre>
   */
  private AmberExpr parseSimpleTerm()
    throws QueryParseException
  {
    int token = scanToken();

    switch (token) {
    case IDENTIFIER:
    case LOCATE:
    case LENGTH:
    case ABS:
    case SQRT:
    case MOD:
    case SIZE:
    case CONCAT:
    case LOWER:
    case UPPER:
    case SUBSTRING:
    case TRIM:
      {
        String name = _lexeme.toString();

        if (peekToken() != '(') {
          IdExpr tableExpr = getIdentifier(name);

          if (tableExpr != null)
            return parsePath(tableExpr);

          FromItem fromItem = _query.getFromList().get(0);

          tableExpr = fromItem.getIdExpr();

          AmberExpr next = tableExpr.createField(this, name);

          if (next instanceof PathExpr)
            return addPath((PathExpr) next);
          else if (next != null)
            return next;

          throw error(L.l("'{0}' is an unknown table or column", name));
        }
        else {

          name = name.toLowerCase();

          // EXISTS | ALL | ANY | SOME
          if (name.equals("exists") ||
              name.equals("all") ||
              name.equals("any") ||
              name.equals("some")) {

            scanToken();

            if (peekToken() != SELECT && peekToken() != FROM)
              throw error(L.l(name.toUpperCase() + " requires '(SELECT'"));

            SelectQuery select = parseSelect(true);

            if (peekToken() != ')')
              throw error(L.l(name.toUpperCase() + " requires ')'"));

            scanToken();

            if (name.equals("exists"))
              return new ExistsExpr(select);
            else if (name.equals("all"))
              return new AllExpr(select);
            else // SOME is a synonymous with ANY
              return new AnyExpr(select);
          }
          else
            return parseFunction(name, token);
        }
      }

    case CURRENT_DATE:
    case CURRENT_TIME:
    case CURRENT_TIMESTAMP:
      {
        String name = _lexeme.toString();

        return parseFunction(name, token);
      }

    case FALSE:
      return new LiteralExpr(_lexeme, boolean.class);

    case TRUE:
      return new LiteralExpr(_lexeme, boolean.class);

    case NULL:
      return new NullExpr();

    case INTEGER:
      return new LiteralExpr(_lexeme, int.class);

    case LONG:
      return new LiteralExpr(_lexeme, long.class);

    case DOUBLE:
      return new LiteralExpr(_lexeme, double.class);

    case STRING:
      return new LiteralExpr(_lexeme, String.class);

    case ARG:
      {
        ArgExpr arg = new ArgExpr(this, Integer.parseInt(_lexeme));
        /*
          if (_addArgToQuery)
          addArg(arg);
        */
        return arg;
      }

    case NAMED_ARG:
      {
        ArgExpr arg = new ArgExpr(this, _lexeme, _parameterCount);
        return arg;
      }

      /*
        case THIS:
        {
        if (_thisExpr == null) {
        _thisExpr = new IdExpr(this, "caucho_this", _bean);
        addFromItem("caucho_this", _bean.getSQLTable());
        _argList.add(0, new ThisExpr(this, _bean));
        }

        return _thisExpr;
        }
      */

    case '(':
      AmberExpr expr = parseExpr();
      if ((token = scanToken()) != ')')
        throw error(L.l("expected `)' at {0}", tokenName(token)));

      return expr;


    default:
      throw error(L.l("expected term at {0}", tokenName(token)));
    }
  }

  /**
   * Parses a path
   *
   * <pre>
   * path ::= IDENTIFIER
   *      ::= path . IDENTIFIER
   * </pre>
   */
  private AmberExpr parsePath(PathExpr path)
    throws QueryParseException
  {
    while (peekToken() == '.') {
      scanToken();

      String field = parseIdentifier();

      AmberExpr next = path.createField(this, field);

      if (next == null)
        throw error(L.l("'{0}' does not have a field '{1}'",
                        path, field));

      if (! (next instanceof PathExpr))
        return next;

      PathExpr nextPath = addPath((PathExpr) next);

      if (peekToken() == '[') {
        scanToken();

        AmberExpr index = parseExpr();

        next = nextPath.createArray(index);

        if (next == null)
          throw error(L.l("'{0}' does not have a map field '{1}'",
                          path, field));

        if (peekToken() != ']') {
          throw error(L.l("expected ']' at '{0}'", tokenName(peekToken())));
        }

        scanToken();
      }

      if (next instanceof PathExpr)
        path = addPath((PathExpr) next);
      else
        return next;
    }

    return path;
  }

  /**
   * Parses a function
   *
   * <pre>
   * fun ::= IDENTIFIER ( expr* )
   *     ::= IDENTIFIER ( DISTINCT expr* )
   * </pre>
   */
  private AmberExpr parseFunction(String id,
                                  int functionToken)
    throws QueryParseException
  {
    // Function with no arguments.
    switch (functionToken) {

    case CURRENT_DATE:
      return CurrentDateFunExpr.create();

    case CURRENT_TIME:
      return CurrentTimeFunExpr.create();

    case CURRENT_TIMESTAMP:
      return CurrentTimestampFunExpr.create();
    }

    // Function with arguments.

    scanToken();

    boolean distinct = false;

    if (peekToken() == DISTINCT) {
      distinct = true;
      scanToken();
    }

    ArrayList<AmberExpr> args = new ArrayList<AmberExpr>();

    while (peekToken() != ')') {
      AmberExpr arg = parseExpr();

      if (id.equalsIgnoreCase("object")) {
        if (arg instanceof PathExpr) {
          PathExpr pathExpr = (PathExpr) arg;

          arg = new LoadEntityExpr(pathExpr);

          arg = arg.bindSelect(this);

          int token = scanToken();

          if (token != ')')
            throw error(L.l("expected ')' at '{0}'", tokenName(token)));

          return arg;
        }
      }

      args.add(arg);

      if (peekToken() != ',')
        break;

      scanToken();
    }

    if (peekToken() != ')')
      throw error(L.l("expected ')' at '{0}'", tokenName(scanToken())));

    scanToken();

    FunExpr funExpr;

    switch (functionToken) {

    case LOCATE:
      funExpr = LocateFunExpr.create(args);
      break;

    case LENGTH:
      funExpr = LengthFunExpr.create(args);
      break;

    case ABS:
      funExpr = AbsFunExpr.create(args);
      break;

    case SQRT:
      funExpr = SqrtFunExpr.create(args);
      break;

    case MOD:
      funExpr = ModFunExpr.create(args);
      break;

    case SIZE:
      funExpr = SizeFunExpr.create(args);
      break;

    case CONCAT:
      funExpr = ConcatFunExpr.create(args);
      break;

    case LOWER:
      funExpr = LowerFunExpr.create(args);
      break;

    case UPPER:
      funExpr = UpperFunExpr.create(args);
      break;

    case SUBSTRING:
      funExpr = SubstringFunExpr.create(args);
      break;

    case TRIM:
      funExpr = TrimFunExpr.create(args);
      break;

    default:
      funExpr = FunExpr.create(id, args, distinct);
    }

    return funExpr;
  }

  /**
   * Returns the matching identifier.
   */
  private IdExpr getIdentifier(String name)
    throws QueryParseException
  {
    AbstractQuery query = _query;

    for (; query != null; query = query.getParentQuery()) {
      ArrayList<FromItem> fromList = query.getFromList();

      for (int i = 0; i < fromList.size(); i++) {
        FromItem from = fromList.get(i);

        if (from.getName().equals(name))
          return from.getIdExpr();
      }
    }

    return null;

    // throw error(L.l("`{0}' is an unknown table", name));
  }

  /**
   * Parses an identifier.
   */
  private String parseIdentifier()
    throws QueryParseException
  {
    int token = scanToken();

    if (token != IDENTIFIER) {
      throw error(L.l("expected identifier at `{0}'", tokenName(token)));
    }

    return _lexeme;
  }

  /**
   * Peeks the next token
   *
   * @return integer code for the token
   */
  private int peekToken()
    throws QueryParseException
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
    throws QueryParseException
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
    case '+':
    case '-':
    case '[':
    case ']':
      return ch;

    case '=':
      if ((ch = read()) == '>')
        return EXTERNAL_DOT;
      else {
        unread(ch);
        return EQ;
      }

    case '!':
      if ((ch = read()) == '=')
        return NE;
      else {
        unread(ch);
        return '!';
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

      _lexeme = cb.close();

      if (_lexeme.length() == 0) {
        _lexeme = String.valueOf(++_parameterCount);
      }
      else if (index <= 0)
        throw error(L.l("`{0}' must refer to a positive argument",
                        "?" + _lexeme));

      return ARG;

    case ':':
      if (Character.isJavaIdentifierStart((char) (ch = read()))) {
        cb = CharBuffer.allocate();

        for (; ch > 0 && Character.isJavaIdentifierPart((char) ch); ch = read())
          cb.append((char) ch);

        unread(ch);

        _lexeme = cb.close();

        _parameterCount++;
      }
      else
        throw error(L.l("`{0}' must be a valid parameter identifier",
                        ":" + ((char) ch)));

      return NAMED_ARG;

    case '|':
      if ((ch = read()) == '|')
        return CONCAT_OP;
      else
        throw error(L.l("unexpected char at {0}", String.valueOf((char) ch)));

      // @@ is useless?
    case '@':
      if ((ch = read()) != '@')
        throw error(L.l("`@' expected at {0}", charName(ch)));
      return scanToken();
    }

    if (Character.isJavaIdentifierStart((char) ch)) {
      CharBuffer cb = CharBuffer.allocate();

      for (; ch > 0 && Character.isJavaIdentifierPart((char) ch); ch = read())
        cb.append((char) ch);

      unread(ch);

      _lexeme = cb.close();
      String lower = _lexeme.toLowerCase();

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

      _lexeme = cb.close();

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

      _lexeme = cb.close();

      return STRING;
    }

    throw error(L.l("unexpected char at {0}", "" + (char) ch));
  }

  /**
   * Returns the next character.
   */
  private int read()
  {
    if (_parseIndex < _sql.length())
      return _sql.charAt(_parseIndex++);
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
   * Creates an error.
   */
  public QueryParseException error(String msg)
  {
    msg += "\nin \"" + _sql + "\"";

    return new QueryParseException(msg);
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
        return "'" + _lexeme + "'";
    }
  }

  /**
   * Returns a debuggable description of the select.
   */
  public String toString()
  {
    return "QueryParser[]";
  }

  static {
    _reserved = new IntMap();
    _reserved.put("as", AS);
    _reserved.put("from", FROM);
    _reserved.put("in", IN);
    _reserved.put("select", SELECT);
    _reserved.put("update", UPDATE);
    _reserved.put("delete", DELETE);
    _reserved.put("set", SET);
    _reserved.put("distinct", DISTINCT);
    _reserved.put("where", WHERE);
    _reserved.put("order", ORDER);
    _reserved.put("group", GROUP);
    _reserved.put("by", BY);
    _reserved.put("having", HAVING);
    _reserved.put("asc", ASC);
    _reserved.put("desc", DESC);
    _reserved.put("limit", LIMIT);
    _reserved.put("offset", OFFSET);

    _reserved.put("join", JOIN);
    _reserved.put("inner", INNER);
    _reserved.put("left", LEFT);
    _reserved.put("outer", OUTER);
    _reserved.put("fetch", FETCH);

    _reserved.put("or", OR);
    _reserved.put("and", AND);
    _reserved.put("not", NOT);

    _reserved.put("length", LENGTH);
    _reserved.put("locate", LOCATE);

    _reserved.put("abs", ABS);
    _reserved.put("sqrt", SQRT);
    _reserved.put("mod", MOD);
    _reserved.put("size", SIZE);

    _reserved.put("concat", CONCAT);
    _reserved.put("lower", LOWER);
    _reserved.put("upper", UPPER);
    _reserved.put("substring", SUBSTRING);
    _reserved.put("trim", TRIM);

    _reserved.put("current_date", CURRENT_DATE);
    _reserved.put("current_time", CURRENT_TIME);
    _reserved.put("current_timestamp", CURRENT_TIMESTAMP);

    _reserved.put("between", BETWEEN);
    _reserved.put("like", LIKE);
    _reserved.put("escape", ESCAPE);
    _reserved.put("is", IS);

    _reserved.put("new", NEW);

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
