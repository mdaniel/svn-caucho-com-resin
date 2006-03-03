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

package com.caucho.ejb.cfg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Enumeration;

import java.io.IOException;

import javax.sql.DataSource;

import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBHome;
import javax.ejb.EJBObject;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.CreateException;
import javax.ejb.FinderException;

import com.caucho.bytecode.JField;
import com.caucho.bytecode.JMethod;
import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JClassWrapper;
import com.caucho.bytecode.JClassLoader;

import com.caucho.util.L10N;

import com.caucho.config.ConfigException;
import com.caucho.config.LineConfigException;
import com.caucho.config.types.Period;

import com.caucho.java.JavaWriter;

import com.caucho.java.gen.MethodCallChain;
import com.caucho.java.gen.BaseClass;
import com.caucho.java.gen.CallChain;
import com.caucho.java.gen.BaseMethod;
import com.caucho.java.gen.JavaClassGenerator;

import com.caucho.ejb.EjbServerManager;
import com.caucho.ejb.AbstractServer;

import com.caucho.ejb.entity.EntityServer;

import com.caucho.ejb.gen.BeanAssembler;
import com.caucho.ejb.gen.EntityAssembler;
import com.caucho.ejb.gen.AmberAssembler;
import com.caucho.ejb.gen.EntityFindMethod;
import com.caucho.ejb.gen.EntityHomePoolChain;
import com.caucho.ejb.gen.EntityCreateMethod;

import com.caucho.ejb.amber.AmberConfig;

import com.caucho.amber.manager.AmberPersistenceUnit;

import com.caucho.amber.type.EntityType;

/**
 * Configuration for an ejb entity bean.
 */
public class EjbEntityBean extends EjbBean {
  private final static L10N L = new L10N(EjbEntityBean.class);

  private JClassWrapper _primKeyClass;
  private String _primKeyField;
  
  private JClass _compositeKeyClass;

  private String _cmpVersion = "2.x";
  private boolean _isCMP = false;

  private DataSource _dataSource;
  
  private String _abstractSchemaName;
  private String _sqlTable;

  private long _cacheTimeout = 2000L;
  private int _cacheSize = -666;

  private boolean _loadLazyOnTransaction = true;

  private boolean _isReadOnly = false;
  private boolean _isReentrant = true;

  private ArrayList<CmpField> _fields = new ArrayList<CmpField>();
  
  private ArrayList<CmrRelation> _relations = new ArrayList<CmrRelation>();
  private ArrayList<JMethod> _stubMethods = new ArrayList<JMethod>();
  
  /**
   * Creates a new entity bean configuration.
   */
  public EjbEntityBean(EjbConfig ejbConfig)
  {
    super(ejbConfig);

    EjbServerManager ejbManager = ejbConfig.getEJBManager();

    _cacheTimeout = ejbManager.getCacheTimeout();
    _loadLazyOnTransaction = ejbManager.isEntityLoadLazyOnTransaction();
  }

  /**
   * Returns the kind of bean.
   */
  public String getEJBKind()
  {
    return "entity";
  }

  /**
   * Sets the ejb implementation class.
   */
  public void setEJBClass(Class ejbClass)
    throws ConfigException
  {
    super.setEJBClass(ejbClass);

    if (! EntityBean.class.isAssignableFrom(ejbClass) && ! isAllowPOJO())
      throw error(L.l("`{0}' must implement EntityBean.  Entity beans must implement javax.ejb.EntityBean.", ejbClass.getName()));

    
    validateNonFinalMethod("setEntityContext",
                           new JClass[] { JClassLoader.systemForName(EntityContext.class.getName()) }, isAllowPOJO());
    validateNonFinalMethod("unsetEntityContext", new JClass[0], isAllowPOJO());
    validateNonFinalMethod("ejbActivate", new JClass[0], isAllowPOJO());
    validateNonFinalMethod("ejbPassivate", new JClass[0], isAllowPOJO());

    validateNonFinalMethod("ejbRemove", new JClass[0], isAllowPOJO());
    // XXX: spec doesn't enforce this
    // validator.validateException(implMethod, RemoveException.class);
    validateNonFinalMethod("ejbLoad", new JClass[0], isAllowPOJO());
    validateNonFinalMethod("ejbStore", new JClass[0], isAllowPOJO());
  }

  /**
   * Returns the amber entity-type.
   */
  public EntityType getEntityType()
  {
    AmberPersistenceUnit amberPersistenceUnit = getConfig().getEJBManager().getAmberManager();

    EntityType type = amberPersistenceUnit.createEntity(getAbstractSchemaName(),
						getEJBClassWrapper());

    type.setProxyClass(getLocal());

    return type;
  }

  /**
   * Returns the primary key class.
   */
  public JClass getPrimKeyClass()
  {
    return _primKeyClass;
  }

  /**
   * Sets the primary key class.
   */
  public void setPrimKeyClass(Class cl)
  {
    _primKeyClass = new JClassWrapper(cl, _jClassLoader);
  }

  /**
   * Returns the primary key class.
   */
  public JClass getCompositeKeyClass()
  {
    return _compositeKeyClass;
  }

  /**
   * Returns the primary key field.
   */
  public String getPrimKeyField()
  {
    return _primKeyField;
  }

  /**
   * Sets the primary key field.
   */
  public void setPrimKeyField(String field)
  {
    _primKeyField = field;
  }

  /**
   * Sets the persistence type.
   */
  public void setPersistenceType(String type)
    throws ConfigException
  {
    if ("Bean".equals(type))
      _isCMP = false;
    else if ("Container".equals(type)) {
      _isCMP = true;

      /* ejb/0880
      if (getConfig().getEJBManager().getDataSource() == null) {
	throw new ConfigException(L.l("No DataSource found.  The EJB server can't find a configured database."));
      }
      */
    }
    else
      throw new ConfigException(L.l("`{0}' is an known persistence-type.  <persistence-type> must either be `Bean' or `Container'.", type));
  }

  /**
   * Returns true if the entity bean is a CMP.
   */
  public boolean isCMP()
  {
    return _isCMP && "2.x".equals(_cmpVersion);
  }

  /**
   * Sets true if the entity bean is CMP.
   */
  public void setCMP(boolean isCMP)
  {
    _isCMP = isCMP;
  }

  /**
   * Returns true if the entity bean is a CMP.
   */
  public boolean isCMP1()
  {
    return _isCMP && "1.x".equals(_cmpVersion);
  }

  /**
   * Gets the implementation class name.
   */
  public String getFullImplName()
  {
    if (isCMP()) {
      String name = "_ejb." + getEJBName() + "." + getEJBClassName() + "__Amber";
      return JavaClassGenerator.cleanClassName(name);
    }
    else
      return super.getFullImplName();
  }

  /**
   * Sets the CMP version.
   */
  public void setCmpVersion(String version)
    throws ConfigException
  {
    _cmpVersion = version;

    if (! version.equals("1.x") && ! version.equals("2.x"))
      throw error(L.l("CMP version `{0}' is not currently supported.  Only CMP version 1.x and 2.x are supported.", version));
  }

  /**
   * Sets true if the bean is reentrant.
   */
  public void setReentrant(boolean reentrant)
    throws ConfigException
  {
    _isReentrant = reentrant;
  }

  /**
   * Returns the JNDI name for the data-source
   */
  public DataSource getDataSource()
  {
    return _dataSource;
  }

  /**
   * Sets the data-source for the bean.
   */
  public void setDataSource(DataSource dataSource)
  {
    _dataSource = dataSource;
  }

  /**
   * Returns the abstract-schema-name for the bean.
   */
  public String getAbstractSchemaName()
  {
    if (_abstractSchemaName != null)
      return _abstractSchemaName;
    else {
      String name = getEJBName();
      
      int p = name.lastIndexOf('/');
      if (p < 0)
	return name;
      else
	return name.substring(p + 1);
    }
  }

  /**
   * Sets the abstract-schema for the bean.
   */
  public void setAbstractSchemaName(String abstractSchema)
    throws ConfigException
  {
    _abstractSchemaName = abstractSchema;

    EjbEntityBean bean = getConfig().findEntityBySchema(abstractSchema);

    if (bean != null && this != bean)
      throw new ConfigException(L.l("Entity bean '{0}' already has abstract schema '{1}'.  abstract-schema-name values must be distinct.",
				    bean.getEJBName(), abstractSchema));
  }

  /**
   * Returns the sql-table for the bean.
   */
  public String getSQLTable()
  {
    return _sqlTable;
  }

  /**
   * Sets the sql-table for the bean.
   */
  public void setSQLTable(String sqlTable)
  {
    _sqlTable = sqlTable;
  }

  /**
   * Returns the number of items in the entity cache.
   */
  public int getCacheSize()
  {
    return _cacheSize;
  }

  /**
   * Sets the number of items in the entity cache.
   */
  public void setCacheSize(int cacheSize)
  {
    _cacheSize = cacheSize;
  }

  /**
   * Sets the timeout for items in the entity cache.
   */
  public void setCacheTimeout(Period cacheTimeout)
  {
    _cacheTimeout = cacheTimeout.getPeriod();
  }

  /**
   * Gets the timeout for items in the entity cache.
   */
  public long getCacheTimeout()
  {
    return _cacheTimeout;
  }

  /**
   * Returns true if the entity bean is read-only
   */
  public boolean isReadOnly()
  {
    return _isReadOnly;
  }

  /**
   * Sets true if the entity bean is read-only
   */
  public void setReadOnly(boolean isReadOnly)
  {
    _isReadOnly = isReadOnly;
  }

  /**
   * Adds a new cmp-field.
   */
  public CmpFieldProxy createCmpField()
  {
    return new CmpFieldProxy(this);
  }

  /**
   * Gets all the cmp-fields
   */
  public ArrayList<CmpField> getCmpFields()
  {
    return _fields;
  }
  
  /**
   * Gets the matching cmp-field.
   */
  public CmpField getCmpField(String fieldName)
  {
    for (int i = 0; i < _fields.size(); i++) {
      CmpField field = _fields.get(i);
      
      if (field.getName().equals(fieldName))
        return field;
    }

    return null;
  }
  
  /**
   * Add a cmp-field.
   */
  public CmpField addField(String fieldName)
    throws ConfigException
  {
    CmpField field = getCmpField(fieldName);

    if (field == null) {
      field = new CmpField(this);
      field.setFieldName(fieldName);

      field.init();

      _fields.add(field);
    }

    return field;
  }

  /**
   * Returns the field getter method.
   */
  public JMethod getFieldGetter(String fieldName)
  {
    if (fieldName == null)
      return null;

    String methodName = ("get" +
			 Character.toUpperCase(fieldName.charAt(0)) +
			 fieldName.substring(1));

    try {
      return getMethod(getEJBClassWrapper(), methodName, new JClass[0]);
    } catch (Throwable e) {
      return null;
    }
  }
  
  /**
   * Adds a relation role.
   */
  public void addRelation(CmrRelation relation)
    throws ConfigException
  {
    String fieldName = relation.getName();

    JMethod method = getMethodField(fieldName);

    if (method == null && ! (relation instanceof CmrMap))
      throw error(L.l("'{0}' is a missing method", fieldName));

    // relation.setJavaType(method.getReturnType());

    _relations.add(relation);
  }
  
  /**
   * Gets the matching cmp-relation.
   */
  public CmrRelation getRelation(String relationName)
  {
    for (int i = 0; i < _relations.size(); i++) {
      CmrRelation relation = _relations.get(i);

      if (relationName.equals(relation.getName()))
	return relation;
    }
    
    return null;
  }
  
  /**
   * Returns the cmp-relations
   */
  public ArrayList<CmrRelation> getRelations()
  {
    return _relations;
  }
  
  /**
   * Returns the stub methods
   */
  public ArrayList<JMethod> getStubMethods()
  {
    return _stubMethods;
  }
  
  /**
   * Adds a stub method
   */
  public void addStubMethod(JMethod method)
  {
    if (! _stubMethods.contains(method))
      _stubMethods.add(method);
  }
  
  /**
   * Gets the matching cmp-field.
   */
  public JClass getFieldType(String fieldName)
  {
    JMethod method = getMethodField(fieldName);

    if (method != null)
      return method.getReturnType();
    else
      return null;
  }

  /**
   * Creates a query object for addition.
   */
  public Query createQuery()
  {
    return new Query(this);
  }

  /**
   * Adds a query.
   */
  public void addQuery(Query query)
  {
    MethodSignature sig = query.getSignature();

    EjbMethodPattern method = getMethod(sig);

    if (method == null) {
      method = new EjbMethodPattern(this, sig);
      method.setLocation(query.getConfigLocation());
      _methodList.add(method);
    }

    method.setQueryLocation(query.getConfigLocation());
    method.setQuery(query.getEjbQl());
  }

  public EjbMethodPattern getMethod(MethodSignature sig)
  {
    for (int i = 0; i < _methodList.size(); i++) {
      EjbMethodPattern method = _methodList.get(i);

      if (method.getSignature().equals(sig))
        return method;
    }

    return null;
  }

  /**
   * Gets the best method.
   */
  public EjbMethodPattern getQuery(JMethod method, String intf)
  {
    return getMethodPattern(method, intf);
  }

  /**
   * returns the method list.
   */
  public ArrayList<EjbMethodPattern> getQueryList()
  {
    return _methodList;
  }

  /**
   * Configure initialization.
   */
  public void init()
    throws ConfigException
  {
    try {
      if (! isCMP() && getEJBClassWrapper().isAbstract())
	throw error(L.l("'{0}' must not be abstract. BMP entity beans may not be abstract.",
			getEJBClass().getName()));
      
      super.init();
      
      if (_primKeyClass == null && ! isAllowPOJO() &&
	  (getRemote() != null || getLocal() != null))
	throw new ConfigException(L.l("{0}: <entity> has no primary key class.  Entity beans must define a prim-key-class.",
				      getEJBClass().getName()));

      if (getRemoteHome() != null)
	validateHome(getRemoteHome());
      if (getLocalHome() != null)
	validateHome(getLocalHome());
      if (getRemote() != null)
	validateRemote(getRemote());
      if (getLocal() != null)
	validateRemote(getLocal());

      validateMethods();
    } catch (LineConfigException e) {
      throw e;
    } catch (ConfigException e) {
      throw new LineConfigException(getLocation() + e.getMessage(), e);
    }
  }

  /**
   * Configure initialization.
   */
  public void introspect()
    throws ConfigException
  {
    if (isCMP()) {
      introspectCMPFields();

      introspectCMPId();
      
      validateCMPFields();
    }
  }

  /**
   * Introspects the missing CMP fields.
   */
  protected void introspectCMPFields()
    throws ConfigException
  {
    JMethod []methods = getMethods(getEJBClassWrapper());

    for (JMethod method : methods) {
      if (! method.isAbstract())
	continue;

      String name = method.getName();
      
      if (name.startsWith("get") && name.length() > 3 &&
	  method.getParameterTypes().length == 0) {
	String fieldName = getterToFieldName(name);

	CmrRelation rel = getRelation(fieldName);

	if (rel != null) {
	  continue;
	}

	JClass type = method.getReturnType();

	if (type.isAssignableTo(Collection.class)) {
	  throw error(L.l("'{0}' needs to be a relation",
			  fieldName));
	}
	else if (type.isAssignableTo(Map.class)) {
	  throw error(L.l("'{0}' needs to be a relation",
			  fieldName));
	}
	else if (type.isAssignableTo(EJBLocalObject.class)) {
	  throw error(L.l("{0}: '{1}' needs to be defined in an ejb-relation for getter method {2}.",
			  getEJBClass().getName(),
			  fieldName,
			  method.getFullName()));
	}
	else if (type.isAssignableTo(EJBObject.class)) {
	  throw error(L.l("'{0}' needs to be a relation",
			  fieldName));
	}
	else {
	  CmpField cmpField = addField(fieldName);

	  cmpField.setJavaType(method.getReturnType());
	}
      }
    }
  }

  /**
   * Introspects the missing CMP fields.
   */
  protected void validateCMPFields()
    throws ConfigException
  {
    JMethod []methods = getMethods(getEJBClassWrapper());

    for (JMethod method : methods) {
      if (! method.isAbstract())
	continue;

      String name = method.getName();

      if (name.startsWith("ejb")) {
	continue;
      }
      else if (name.startsWith("get") && name.length() > 3 &&
	       method.getParameterTypes().length == 0) {
	String fieldName = getterToFieldName(name);

	if (getCmpField(fieldName) != null ||
	    getRelation(fieldName) != null)
	  continue;
      }
      else if (name.startsWith("get") && name.length() > 3 &&
	       method.getParameterTypes().length == 1) {
	String fieldName = getterToFieldName(name);

	CmrRelation rel = getRelation(fieldName);

	if (rel instanceof CmrMap) {
	  CmrMap map = (CmrMap) rel;

	  if (method.equals(map.getMapMethod()))
	    continue;
	}
      }
      else if (name.startsWith("set") && name.length() > 3 &&
	       method.getParameterTypes().length == 1) {
	String fieldName = getterToFieldName(name);

	CmpField field = getCmpField(fieldName);

	if (field == null) {
	}
	else if (isMatch(method, field.getSetter()))
	  continue;
	else
	  throw new ConfigException(L.l("{0}: '{1}' does not match the corresponding cmp-field getter '{2}'.",
					getEJBClass().getName(),
					method.getFullName(),
					field.getGetter().getFullName()));

	CmrRelation rel = getRelation(fieldName);

	if (rel == null) {
	}
	else if (method.equals(rel.getSetter()))
	  continue;
	else
	  throw new ConfigException(L.l("{0}: '{1}' does not match the corresponding cmp-field getter '{2}'.",
					getEJBClass().getName(),
					method.getFullName(),
					rel.getGetter().getFullName()));
      }
      
      throw error(L.l("{0}: '{1}' must not be abstract.  Business methods must be implemented.",
		      getEJBClass().getName(),
		      method.getFullName()));
    }
  }

  /**
   * Introspects the CMP id.
   */
  protected void introspectCMPId()
    throws ConfigException
  {

    if (_primKeyClass != null &&
	! _primKeyClass.isPrimitive() &&
	! _primKeyClass.getName().startsWith("java.lang.") &&
	! _primKeyClass.getName().equals("java.util.Date") &&
	! _primKeyClass.getName().equals("java.sql.Date") &&
	! _primKeyClass.getName().equals("java.sql.Time") &&
	! _primKeyClass.getName().equals("java.sql.Timestamp") &&
	! _primKeyClass.isAssignableTo(EJBLocalObject.class)) {

      if (_primKeyField != null)
	throw error(L.l("{0}: 'primkey-field' must not be defined for a composite primkey-class.",
			getEJBClass().getName()));
      
      _compositeKeyClass = _primKeyClass;
      introspectCMPCompositeId();
      return;
    }
      
    String id = _primKeyField;
    if (id == null)
      id = "id";
    
    CmpProperty property = getCmpField(id);

    if (property == null)
      property = getRelation(id);

    if (property == null)
      throw error(L.l("{0}: primary key field '{1}' is an unknown cmp-field",
		      getEJBClass().getName(), id));
    
    property.setId(true);
  }

  /**
   * Introspects the CMP id.
   */
  protected void introspectCMPCompositeId()
    throws ConfigException
  {
    try {
      JMethod equals = _primKeyClass.getMethod("equals", new JClass[] { JClass.OBJECT });

      if (equals.getDeclaringClass().equals(JClass.OBJECT))
	throw error(L.l("{0}: primary key class '{1}' must override the 'equals' method.",
			getEJBClass().getName(),
			_primKeyClass.getName()));
    } catch (ConfigException e) {
      throw e;
    } catch (Throwable e) {
    }
    
    try {
      JMethod hashCode = _primKeyClass.getMethod("hashCode", new JClass[] { });

      if (hashCode.getDeclaringClass().getName().equals(Object.class.getName()))
	throw error(L.l("{0}: primary key class '{1}' must override the 'hashCode' method.",
			getEJBClass().getName(),
			_primKeyClass.getName()));
    } catch (ConfigException e) {
      throw e;
    } catch (Throwable e) {
    }

    if (_primKeyClass.getFields().length == 0)
	throw error(L.l("{0}: compound key '{1}' has no public accessible fields.  Compound key fields must be public.",
			getEJBClass().getName(),
			_primKeyClass.getName()));

    for (JField field : _primKeyClass.getFields()) {
      CmpProperty cmpProperty = getCmpField(field.getName());

      if (cmpProperty == null)
	cmpProperty = getRelation(field.getName());

      if (cmpProperty == null)
	throw error(L.l("{0}: primary key field '{1}' is an unknown field.",
		      getEJBClass().getName(), field.getName()));

      cmpProperty.setId(true);
    }
  }

  private static String getterToFieldName(String name)
  {
    String fieldName = name.substring(3);
    char ch = fieldName.charAt(0);

    if (Character.isUpperCase(ch) &&
	(fieldName.length() == 1 ||
	 Character.isLowerCase(fieldName.charAt(1)))) {
      fieldName = Character.toLowerCase(ch) + fieldName.substring(1);
    }

    return fieldName;
  }

  /**
   * Configure for amber.
   */
  public void configureAmber(AmberConfig config)
    throws ConfigException
  {
    if (isCMP()) {
      try {
	config.addBean(this);
      } catch (LineConfigException e) {
	throw e;
      } catch (Exception e) {
	throw new LineConfigException(getLocation() + e.getMessage(), e);
      }
    }
  }

  /**
   * Creates the assembler for the bean.
   */
  protected BeanAssembler createAssembler(String fullClassName)
  {
    if (isCMP())
      return new AmberAssembler(this, fullClassName);
    else
      return new EntityAssembler(this, fullClassName);
  }

  /**
   * Adds the assemblers.
   */
  protected void addImports(BeanAssembler assembler)
  {
    super.addImports(assembler);

    assembler.addImport("com.caucho.ejb.FinderExceptionWrapper");
    
    assembler.addImport("com.caucho.ejb.entity.EntityServer");
    assembler.addImport("com.caucho.ejb.entity.QEntityContext");
    assembler.addImport("com.caucho.ejb.entity.EntityHome");
    assembler.addImport("com.caucho.ejb.entity.EntityLocalHome");
    assembler.addImport("com.caucho.ejb.entity.EntityRemoteHome");
    assembler.addImport("com.caucho.ejb.entity.EntityObject");
    assembler.addImport("com.caucho.ejb.entity.QEntity");
  }

  /**
   * Introspects an ejb method.
   */
  protected EjbBaseMethod introspectEJBMethod(JMethod method)
    throws ConfigException
  {
    String methodName = method.getName();
    JClass []paramTypes = method.getParameterTypes();

    if (methodName.startsWith("ejbSelect") && method.isAbstract()) {
      validateSelectMethod(method);
      
      EjbMethodPattern pattern = getMethodPattern(method, "Local");

      if (pattern == null)
	throw error(L.l("{0}: '{1}' expects an ejb-ql query. ejbSelect methods must have an ejb-ql query in the deployment descriptor.",
			getEJBClass().getName(),
			method.getFullName()));
	
      String query = pattern.getQuery();

      EjbAmberSelectMethod select;
      select = new EjbAmberSelectMethod(this, method, query,
					pattern.getQueryLocation());

      select.setQueryLoadsBean(pattern.getQueryLoadsBean());

      return select;
    }

    return null;
  }

  /**
   * Introspects an ejb method.
   */
  protected void validateImplMethod(JMethod method)
    throws ConfigException
  {
    String methodName = method.getName();
    JClass []paramTypes = method.getParameterTypes();
    
    if (method.isAbstract() &&
	methodName.startsWith("get") &&
	paramTypes.length == 0) {
    }
    else if (method.isAbstract() &&
	     methodName.startsWith("get") &&
	     paramTypes.length == 1) {
    }
    else if (method.isAbstract() &&
	     methodName.startsWith("set") &&
	     paramTypes.length == 1) {
    }
    else if (methodName.startsWith("ejb")) {
    }
    else if (method.isAbstract()) {
      throw error(L.l("{0}: '{1}' must not be abstract.  The bean must implement its business methods.",
		      getEJBClass().getName(),
		      method.getFullName()));
    }
  }

  /**
   * Check that a method is public.
   *
   * @return the matching method
   */
  private void validateSelectMethod(JMethod method)
    throws ConfigException
  {
    if (! method.isPublic()) {
      throw error(L.l("{0}: '{1}' must be public. ejbSelect methods must be public.",
                      getEJBClass().getName(),
                      method.getFullName()));
    }
    else if (method.isStatic()) {
      throw error(L.l("{0}: `{1}' must not be static. ejbSelect methods must not be static.",
                      getEJBClass().getName(),
                      method.getFullName()));
    }
    else if (! method.isAbstract()) {
      throw error(L.l("{0}: `{1}' must be abstract. ejbSelect methods must be abstract.",
                      getEJBClass().getName(),
                      method.getFullName()));
    }
  }

  /**
   * Creates the views.
   */
  protected EjbHomeView createHomeView(JClass homeClass, String prefix)
    throws ConfigException
  {
    return new EjbEntityHomeView(this, homeClass, prefix);
  }

  /**
   * Creates the views.
   */
  protected EjbObjectView createObjectView(JClass apiClass, String prefix)
    throws ConfigException
  {
    if (isCMP())
      return new EjbCmpView(this, apiClass, prefix);
    else
      return new EjbEntityView(this, apiClass, prefix);
  }

  /**
   * Deploys the bean.
   */
  public AbstractServer deployServer(EjbServerManager ejbManager,
				     JavaClassGenerator javaGen)
    throws ClassNotFoundException
  {
    EntityServer server = new EntityServer(ejbManager);

    server.setEJBName(getEJBName());
    server.setJndiName(getJndiName());

    server.setRemoteHomeClass(getRemoteHomeClass());
    server.setRemoteObjectClass(getRemoteClass());

    Class contextImplClass = javaGen.loadClass(getSkeletonName());
    
    server.setContextImplClass(contextImplClass);
    
    server.setCMP(isCMP());

    if (_primKeyClass != null)
      server.setPrimaryKeyClass(_primKeyClass.getWrappedClass());

    server.setLoadLazyOnTransaction(_loadLazyOnTransaction);
    
    server.setInitProgram(getInitProgram());

    if (isCMP()) {
      server.setAmberEntityHome(getEntityType().getHome());
    }

    return server;
  }

  private void validateMethods()
    throws ConfigException
  {
    JClass primKeyClass = getPrimKeyClass();

    JMethod []methods = getMethods(getEJBClassWrapper());
    
    for (JMethod method : methods) {
      String name = method.getName();

      try {
	// XXX: ???
	JMethod cleanMethod = getEJBClassWrapper().getMethod(name, method.getParameterTypes());
	if (cleanMethod != null)
	  method = cleanMethod;
      } catch (Exception e) {
      }

      if (method.getReturnType().isAssignableTo(EntityBean.class))
        throw error(L.l("{0}: `{1}' must not return entity bean `{2}'.  Entity bean methods must always return local or remote interfaces.",
                        getEJBClass().getName(),
                        method.getFullName(),
                        method.getReturnType().getShortName()));
      
      if (name.startsWith("ejbFind")) {
        if (! isCMP()) {
          validateNonFinalMethod(method.getName(),
                                 method.getParameterTypes());
        }
        else if (true) {
          // allow overriding
          validateNonFinalMethod(method.getName(), method.getParameterTypes());
        }
        else {
          throw error(L.l("{0}: `{1}' forbidden. CMP entity beans must not implement ejbFind methods.",
                          method.getDeclaringClass().getName(),
                          method.getFullName()));
        }
      }
      else if (name.startsWith("ejbSelect")) {
        if (! method.isAbstract())
          throw error(L.l("{0}: `{1}' must be abstract. ejbSelect methods must be abstract.",
                              method.getDeclaringClass().getName(),
                              method.getFullName()));
        if (! method.isPublic())
          throw error(L.l("{0}: `{1}' must be public.",
                          method.getDeclaringClass().getName(),
                          method.getFullName()));
        validateException(method, FinderException.class);
      }
      else if (name.startsWith("ejbCreate")) {
        validateNonFinalMethod(method.getName(), method.getParameterTypes());
        if (! isPrimaryKeyClass(method.getReturnType()))
          throw error(L.l("{0}: `{1}' must return `{2}'.  ejbCreate methods must return the primary key.",
                          method.getDeclaringClass().getName(),
                          method.getFullName(),
                          getClassName(primKeyClass)));
        if (isCMP())
          validateException(method, CreateException.class);
      }
      else if (name.startsWith("ejbPostCreate")) {
        validateNonFinalMethod(method.getName(), method.getParameterTypes());

        if (! method.getReturnType().getName().equals("void"))
          throw error(L.l("{0}: `{1}' must return void. ejbPostCreate methods must return void.",
                          method.getDeclaringClass().getName(),
                          method.getFullName()));
      }
      else if (name.startsWith("create") &&
               method.isAbstract()) {
        // validated in dependent phase.
      }
      else if (name.startsWith("ejbHome")) {
        validateNonFinalMethod(method.getName(), method.getParameterTypes());
      }
      else if (name.equals("ejbRemove")) {
        if (method.getParameterTypes().length != 0)
          throw error(L.l("{0}: `{1}' must have no arguments.",
                          method.getDeclaringClass().getName(),
                          method.getFullName()));
      }
      else if (name.equals("ejbTimeout")) {
	JClass []types = method.getParameterTypes();
	
        if (types.length != 1 || ! types[0].getName().equals("javax.ejb.Timer"))
          throw error(L.l("{0}: `{1}' must have one javax.ejb.Timer argument.",
                          method.getDeclaringClass().getName(),
                          method.getFullName()));
      }
      else if (classHasMethod(method, JClassLoader.systemForName(EntityBean.class.getName()))) {
      }
      else if (name.equals("finalize") &&
               method.getParameterTypes().length == 0 &&
               ! method.getDeclaringClass().getName().equals("java.lang.Object"))
        throw error(L.l("{0}: Entity beans must not define `finalize'.",
                        getEJBClass().getClass()));
      else if (name.startsWith("ejb")) {
        throw error(L.l("{0}: `{1}' must not start with `ejb'.  ejbXXX methods are reserved by the EJB spec.",
                        method.getDeclaringClass().getName(),
                        method.getFullName()));
      }
      else {
        boolean isAbstract = method.isAbstract();
        JMethod persist = null;
        
        if (! isAbstract || ! isCMP()) {
        }
        else if (method.getName().startsWith("get")) {
          //validateGetImpl(method); -- in relations
        }
        else if (method.getName().startsWith("set")) {
          //validateSetImpl(method); -- in relations
        }
        else if (method.getName().startsWith("ejbSelect")) {
        }
        else {
          throw error(L.l("{0}: `{1}' must not be abstract.  Only CMP methods may be abstract.",
                          method.getDeclaringClass().getName(),
                          method.getFullName()));
        }

        /* XXX: should look for matching cmp/cmr-field */
        /*
        if (! getBeanManagedPersistence()) {
          persist = validator.findPersistentMethod(method.getName(),
                                                   method.getParameterTypes());

          if (persist != null && ! isAbstract)
            throw error(L.l("`{0}' must be abstract in {1}",
                            method.getFullName(),
                            method.getDeclaringClass().getName()));
        }

        if (persist == null && isAbstract)
          throw error(L.l("`{0}' must not be abstract in {1}",
                          method.getFullName(),
                          method.getDeclaringClass().getName()));
        */
      }
    }
  }

  /**
   * Validates the home interface.
   */
  private void validateHome(JClass homeClass)
    throws ConfigException
  {
    JClass beanClass = getEJBClassWrapper();
    String beanName = beanClass.getName();
  
    String homeName = homeClass.getName();

    JClass objectClass;
    if (homeClass.isAssignableTo(EJBHome.class))
      objectClass = getRemote();
    else
      objectClass = getLocal();
    String objectName = objectClass != null ? objectClass.getName() : null;

    boolean hasFindByPrimaryKey = false;

    JClass primKeyClass = getPrimKeyClass();
  
    if (! homeClass.isPublic())
      throw error(L.l("`{0}' must be public. Entity beans must be public.", homeName));
  
    if (beanClass.isFinal())
      throw error(L.l("`{0}' must not be final.  Entity beans must not be final.", beanName));

    if (! isCMP() && beanClass.isAbstract())
      throw error(L.l("`{0}' must not be abstract. BMP entity beans must not be abstract.", beanName));

    if (! homeClass.isInterface())
      throw error(L.l("`{0}' must be an interface.", homeName));

    JMethod []methods = getMethods(homeClass);
    for (int i = 0; i < methods.length; i++) {
      JMethod method = methods[i];
      String name = method.getName();
      JClass []param = method.getParameterTypes();
      JClass retType = method.getReturnType();

      if (method.getDeclaringClass().isAssignableFrom(EJBHome.class))
        continue;
      
      if (method.getDeclaringClass().isAssignableFrom(EJBLocalHome.class))
        continue;

      if (homeClass.isAssignableTo(EJBHome.class))
        validateException(method, java.rmi.RemoteException.class);
      
      if (name.startsWith("create")) {
        validateException(method, CreateException.class);

        if (! retType.equals(objectClass))
          throw error(L.l("{0}: `{1}' must return {2}.  Create methods must return the local or remote interface.",
                          homeName,
                          method.getFullName(),
                          objectName));

        String createName = "ejbC" + name.substring(1);
        JMethod implMethod =
          validateNonFinalMethod(createName, param, method, homeClass);

        if (! isPrimaryKeyClass(implMethod.getReturnType()))
          throw error(L.l("{0}: `{1}' must return `{2}'.  ejbCreate methods must return the primary key.",
                          beanName,
                          getFullMethodName(createName, param),
                          getClassName(primKeyClass)));

        if (! hasException(implMethod, CreateException.class)) {
          throw error(L.l("{0}: `{1}' must throw {2}.  ejbCreate methods must throw CreateException.",
                          implMethod.getDeclaringClass().getName(),
                          implMethod.getFullName(),
                          "CreateException"));
          
        }

        validateExceptions(method, implMethod);

        createName = "ejbPostC" + name.substring(1);
        implMethod = validateNonFinalMethod(createName, param,
                                            method, homeClass);

        if (! implMethod.getReturnType().getName().equals("void"))
          throw error(L.l("{0}: `{1}' must return {2}. ejbPostCreate methods must return void.",
                          beanName,
                          getFullMethodName(createName, param),
                          "void"));
        
        
        validateExceptions(method, implMethod);
      }
      else if (name.startsWith("find")) {
        if (name.equals("findByPrimaryKey")) {
          hasFindByPrimaryKey = true;

	  /*
          if (param.length != 1 || ! param[0].equals(primKeyClass))
            throw error(L.l("`{0}' expected as only argument of {1} in {2}. findByPrimaryKey must take the primary key as its only argument.",
                            getClassName(primKeyClass),
                            name, homeName));
	  */
          
          if (! objectClass.equals(method.getReturnType()))
            throw error(L.l("{0}: `{1}' must return `{2}'.  Find methods must return the remote or local interface.",
                            homeName,
                            method.getFullName(),
                            objectName));
        }
        
        String findName = "ejbF" + name.substring(1);
        if (! isCMP() && ! isCMP1()
	    || getMethod(beanClass, findName, param) != null) {
          JMethod impl = validateNonFinalMethod(findName, param, isAllowPOJO());

	  if (impl != null)
	    validateExceptions(method, impl);
          
          if (objectClass.equals(method.getReturnType())) {
            if (impl != null && ! isPrimaryKeyClass(impl.getReturnType()))
              throw error(L.l("{0}: `{1}' must return primary key `{2}'.  ejbFind methods must return the primary key",
                              beanName,
                              impl.getFullName(),
                              getClassName(primKeyClass)));
          }
          else if (method.getReturnType().isAssignableTo(Collection.class)) {
            if (impl != null && ! impl.getReturnType().isAssignableTo(Collection.class))
              throw error(L.l("{0}: `{1}' must return collection.",
                              beanName,
                              impl.getFullName()));
          }
          else if (method.getReturnType().isAssignableTo(Enumeration.class)) {
            if (! impl.getReturnType().isAssignableTo(Enumeration.class))
              throw error(L.l("{0}: `{1}' must return enumeration.",
                              beanName,
                              impl.getFullName()));
          }
          else
            throw error(L.l("{0}: `{1}' must return `{2}' or a collection. ejbFind methods must return the primary key or a collection.",
                            homeName,
                            method.getFullName(),
                            objectName));
        }
        else if (isCMP() && ! name.equals("findByPrimaryKey")) {
          String query = findQuery(method);

          if (query == null) {
            throw error(L.l("{0}: `{1}' expects an ejb-ql query.  All find methods need queries defined in the EJB deployment descriptor.",
                                  homeName,
                                  method.getFullName()));
          }
        }

        validateException(method, FinderException.class);
        
        if (! retType.equals(objectClass) &&
            (! retType.isAssignableTo(Collection.class) &&
             ! Enumeration.class.getName().equals(retType.getName()) ||
             name.equals("findByPrimaryKey"))) {
          throw error(L.l("{0}: `{1}' must return {2} or a collection. ejbFind methods must return the primary key or a collection.",
                          homeName,
                          method.getFullName(),
                          objectName));
        }
      }
      else if (name.startsWith("ejbSelect")) {
        throw error(L.l("{0}: `{1}' forbidden.  ejbSelect methods may not be exposed in the remote or local interface.",
                        homeName,
                        method.getFullName()));
      }
      else if (name.startsWith("ejb")) {
        throw error(L.l("{0}: `{1}' forbidden.  Only ejbXXX methods defined by the spec are allowed.",
                        homeName,
                        method.getFullName()));
      }
      else if (name.startsWith("remove")) {
        throw error(L.l("{0}: `{1}' forbidden.  removeXXX methods are reserved by the spec.",
                        homeName,
                        method.getFullName()));
      }
      else {
        retType = method.getReturnType();
        
        if (homeClass.isAssignableTo(EJBHome.class) &&
            (retType.isAssignableTo(EJBLocalObject.class) ||
             retType.isAssignableTo(EJBLocalHome.class)))
          throw error(L.l("{1}: `{0}' must not return local interface.",
                          homeClass.getName(),
                          method.getFullName()));
        
        String homeMethodName = ("ejbHome" +
                                 Character.toUpperCase(name.charAt(0)) +
                                 name.substring(1));
        JMethod implMethod = validateMethod(homeMethodName, param,
                                           method, homeClass);

        if (! retType.equals(implMethod.getReturnType()))
          throw error(L.l("{0}: `{1}' must return {2}.",
                          beanName,
                          implMethod.getFullName(),
                          method.getReturnType().getName()));

        validateExceptions(method, implMethod.getExceptionTypes());
      }
    }

    // ejb/0588
    if (! hasFindByPrimaryKey && ! isAllowPOJO() && objectClass != null)
      throw error(L.l("{0}: expected `{1}'. All entity homes must define findByPrimaryKey.",
                      homeName,
                      getFullMethodName("findByPrimaryKey",
                                                  new JClass[] {
                                                    primKeyClass })));
  }

  protected void assembleHomeMethods(BeanAssembler assembler,
				     BaseClass baseClass,
				     String contextClassName,
				     JClass homeClass,
				     String prefix)
    throws NoSuchMethodException
  {
    JMethod []methods = getMethods(homeClass);

    for (int i = 0; i < methods.length; i++) {
      String className = methods[i].getDeclaringClass().getName();
      String methodName = methods[i].getName();

      if (className.startsWith("javax.ejb.")) {
      }
      else if (isOld(methods, methods[i], i)) {
      }
      else if (methodName.startsWith("create")) {
	assembleCreateMethod(methods[i], baseClass, contextClassName, prefix);
      }
      else if (methodName.startsWith("find")) {
        JMethod beanMethod = null;

        String name = ("ejb" + Character.toUpperCase(methodName.charAt(0))
                       + methodName.substring(1));

        try {
          beanMethod = getEJBClassWrapper().getMethod(name,
					       methods[i].getParameterTypes());
        } catch (Throwable e) {
        }

	if (beanMethod != null) {
	  EntityFindMethod findMethod;
	  findMethod = new EntityFindMethod(methods[i],
					    beanMethod,
					    contextClassName,
					    prefix);

	  CallChain call = findMethod.getCall();
	  call = new EntityHomePoolChain(call);
	  // call = getTransactionChain(call, methods[i], prefix);
	  findMethod.setCall(call);

	  baseClass.addMethod(findMethod);
	}
      }
      else {
        JMethod beanMethod = null;

        String name = ("ejbHome" + Character.toUpperCase(methodName.charAt(0))
                       + methodName.substring(1));

        try {
          beanMethod = getEJBClassWrapper().getMethod(name,
					       methods[i].getParameterTypes());
        } catch (Exception e) {
          throw new NoSuchMethodException("can't find method " + name);
        }

	CallChain call = new MethodCallChain(beanMethod);
	// call = assembler.createPoolChain(call);
	call = getTransactionChain(call, beanMethod, prefix);

	baseClass.addMethod(new BaseMethod(methods[i], call));
      }
    }
  }

  protected void assembleCreateMethod(JMethod method,
				      BaseClass baseClass,
				      String contextClassName,
				      String prefix)
  {
    String methodName = method.getName();
    
    JMethod beanCreateMethod = null;
    JMethod beanPostCreateMethod = null;

    String name = ("ejb" + Character.toUpperCase(methodName.charAt(0))
		   + methodName.substring(1));

    try {
      beanCreateMethod = getEJBClassWrapper().getMethod(name,
						 method.getParameterTypes());
    } catch (Throwable e) {
    }

    if (beanCreateMethod == null)
      throw new IllegalStateException(name);

    name = ("ejbPost" + Character.toUpperCase(methodName.charAt(0))
	    + methodName.substring(1));

    try {
      beanPostCreateMethod = getEJBClassWrapper().getMethod(name,
						     method.getParameterTypes());
    } catch (Throwable e) {
    }

    EntityCreateMethod createMethod;

    createMethod = new EntityCreateMethod(this, method,
					  beanCreateMethod,
					  beanPostCreateMethod,
					  contextClassName);

    createMethod.setCall(getTransactionChain(createMethod.getCall(),
					     method,
					     prefix));
						 
    baseClass.addMethod(createMethod);
  }

  /**
   * Return true if the type matches the primary key class.
   */
  private boolean isPrimaryKeyClass(JClass type)
  {
    return type.equals(getPrimKeyClass());
  }

  private JMethod getMethodField(String fieldName)
  {
    String getter = "get" + Character.toUpperCase(fieldName.charAt(0)) +
      fieldName.substring(1);
    
    JMethod []methods = getMethods(getEJBClassWrapper());
    
    for (int i = 0; i < methods.length; i++) {
      if (getter.equals(methods[i].getName()) &&
	  methods[i].getParameterTypes().length == 0)
	return methods[i];
    }

    return null;
  }

  /**
   * Finds the matching query.
   */
  private String findQuery(JMethod method)
    throws ConfigException
  {
    EjbMethodPattern ejbMethod = getMethodPattern(method, null);

    if (ejbMethod != null && ejbMethod.getQuery() != null)
      return ejbMethod.getQuery();
    
    EjbMethodPattern ejbQuery = getQuery(method, null);
    if (ejbQuery != null)
      return ejbQuery.getQuery();
    else
      return null;
  }

  /**
   * Compares dependencies for identifying relations.
   */
  public boolean dependsOn(EjbEntityBean target)
  {
    for (CmrRelation rel : _relations) {
      if (rel.isId()) {
	EjbEntityBean targetBean = rel.getTargetBean();

	if (target == targetBean ||
	    targetBean != null && targetBean.dependsOn(target))
	  return true;
      }
    }

    return false;
  }

  /**
   * Generates the bean prologue.
   */
  public void generateBeanPrologue(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates the after commit method.
   */
  public void generateAfterCommit(JavaWriter out)
    throws IOException
  {
    for (CmrRelation rel : _relations) {
      rel.generateAfterCommit(out);
    }
  }

  /**
   * Generates the destroy method.
   */
  public void generateDestroy(JavaWriter out)
    throws IOException
  {
    for (CmrRelation rel : _relations) {
      rel.generateDestroy(out);
    }
  }

  public String toString()
  {
    return "EjbEntityBean[" + getEJBName() + "]";
  }
}
