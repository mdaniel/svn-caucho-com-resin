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

package com.caucho.amber.type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.IOException;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.caucho.bytecode.JMethod;
import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JClassDependency;
import com.caucho.bytecode.JField;

import com.caucho.util.L10N;
import com.caucho.util.CharBuffer;

import com.caucho.config.ConfigException;

import com.caucho.make.PersistentDependency;
import com.caucho.make.ClassDependency;

import com.caucho.java.JavaWriter;

import com.caucho.lifecycle.Lifecycle;

import com.caucho.amber.AmberRuntimeException;

import com.caucho.amber.entity.Entity;
import com.caucho.amber.entity.EntityItem;
import com.caucho.amber.entity.AmberEntityHome;
import com.caucho.amber.entity.AmberCompletion;

import com.caucho.amber.field.Id;
import com.caucho.amber.field.IdField;
import com.caucho.amber.field.AmberField;
import com.caucho.amber.field.StubMethod;
import com.caucho.amber.field.EntityManyToOneField;
import com.caucho.amber.field.AmberFieldCompare;

import com.caucho.amber.idgen.IdGenerator;

import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.manager.AmberConnection;

import com.caucho.amber.table.Table;
import com.caucho.amber.table.Column;

/**
 * Represents an application persistent bean type
 */
public class EntityType extends Type {
  private static final Logger log = Logger.getLogger(EntityType.class.getName());
  private static final L10N L = new L10N(EntityType.class);

  private AmberPersistenceUnit _amberPersistenceUnit;

  private String _name;
  
  private Table _table;
  
  private ArrayList<Table> _secondaryTables = new ArrayList<Table>();
  
  private Id _id;
  
  private Column _discriminator;
  private String _discriminatorValue;
  private boolean _isJoinedSubClass;
  
  private ArrayList<AmberField> _fields = new ArrayList<AmberField>();

  private HashMap<String,EntityType> _subEntities;
  
  private JClass _beanClass;

  private boolean _isFieldAccess;

  private Throwable _exception;
  
  private String _instanceClassName;
  private boolean _isEnhanced;
  private ClassLoader _instanceLoader;
  private Class _instanceClass;

  private JClass _proxyClass;
  
  private AmberEntityHome _home;

  private ArrayList<StubMethod> _methods = new ArrayList<StubMethod>();

  protected int _defaultLoadGroupIndex;
  protected int _loadGroupIndex;
  
  protected int _minDirtyIndex;
  protected int _dirtyIndex;

  protected boolean _hasLoadCallback;

  private HashMap<String,IdGenerator> _idGenMap
    = new HashMap<String,IdGenerator>();

  private ArrayList<PersistentDependency> _dependencies
    = new ArrayList<PersistentDependency>();

  private ArrayList<JMethod> _postLoadCallbacks
    = new ArrayList<JMethod>();

  private ArrayList<JMethod> _prePersistCallbacks
    = new ArrayList<JMethod>();

  private ArrayList<JMethod> _postPersistCallbacks
    = new ArrayList<JMethod>();

  private ArrayList<JMethod> _preUpdateCallbacks
    = new ArrayList<JMethod>();

  private ArrayList<JMethod> _postUpdateCallbacks
    = new ArrayList<JMethod>();

  private ArrayList<JMethod> _preRemoveCallbacks
    = new ArrayList<JMethod>();

  private ArrayList<JMethod> _postRemoveCallbacks
    = new ArrayList<JMethod>();

  private final Lifecycle _lifecycle = new Lifecycle();

  private volatile boolean _isConfigured;
  private volatile boolean _isGenerated;
  
  public EntityType(AmberPersistenceUnit amberPersistenceUnit)
  {
    _amberPersistenceUnit = amberPersistenceUnit;
  }

  /**
   * Returns the manager.
   */
  public AmberPersistenceUnit getPersistenceUnit()
  {
    return _amberPersistenceUnit;
  }

  /**
   * Returns the table.
   */
  public Table getTable()
  {
    if (_table == null)
      setTable(_amberPersistenceUnit.createTable(getName()));
    
    return _table;
  }

  /**
   * Sets the table.
   */
  public void setTable(Table table)
  {
    _table = table;

    table.setType(this);
  }

  /**
   * Adds a secondary table.
   */
  public void addSecondaryTable(Table table)
  {
    if (! _secondaryTables.contains(table)) {
      _secondaryTables.add(table);
    }

    table.setType(this);
  }

  /**
   * Adds a secondary table.
   */
  public ArrayList<Table> getSecondaryTables()
  {
    return _secondaryTables;
  }

  /**
   * Gets a secondary table.
   */
  public Table getSecondaryTable(String name)
  {
    for (Table table : _secondaryTables) {
      if (table.getName().equals(name))
	return table;
    }

    return null;
  }

  /**
   * Set true for field-access.
   */
  public void setFieldAccess(boolean isFieldAccess)
  {
    _isFieldAccess = isFieldAccess;
  }

  /**
   * Set true for field-access.
   */
  public boolean isFieldAccess()
  {
    return _isFieldAccess;
  }

  /**
   * Returns the java type.
   */
  public String getJavaTypeName()
  {
    return getInstanceClassName();
  }

  /**
   * Returns the java type.
   */
  public String getForeignTypeName()
  {
    return getId().getForeignTypeName();
  }

  /**
   * Sets the bean class.
   */
  public void setBeanClass(JClass beanClass)
  {
    _beanClass = beanClass;

    if (getName() == null) {
      String name = beanClass.getName();
      int p = name.lastIndexOf('.');

      if (p > 0)
	name = name.substring(p + 1);

      setName(name);
    }

    addDependency(_beanClass);
  }

  /**
   * Gets the bean class.
   */
  public JClass getBeanClass()
  {
    return _beanClass;
  }

  /**
   * Gets the proxy class.
   */
  public JClass getProxyClass()
  {
    if (_proxyClass != null)
      return _proxyClass;
    else
      return _beanClass;
  }

  /**
   * Gets the proxy class.
   */
  public void setProxyClass(JClass proxyClass)
  {
    _proxyClass = proxyClass;
  }

  public boolean isInit()
  {
    return _instanceClass != null;
  }

  /**
   * Sets the instance class name.
   */
  public void setInstanceClassName(String className)
  {
    _instanceClassName = className;
  }

  /**
   * Gets the instance class name.
   */
  public String getInstanceClassName()
  {
    return _instanceClassName;
  }

  /**
   * Sets true if the class is enhanced.
   */
  public void setEnhanced(boolean isEnhanced)
  {
    _isEnhanced = isEnhanced;
  }

  /**
   * Returns true if the class is enhanced.
   */
  public boolean isEnhanced()
  {
    return _isEnhanced;
  }

  /**
   * Sets the instance class loader
   */
  public void setInstanceClassLoader(ClassLoader loader)
  {
    _instanceLoader = loader;
  }

  public void setConfigException(Throwable e)
  {
    if (_exception == null)
      _exception = e;
  }
  
  /**
   * Gets the instance class.
   */
  public Class getInstanceClass()
  {
    if (_instanceClass == null) {
      if (getInstanceClassName() == null) {
	throw new RuntimeException("No instance class:" + this);
      }

      try {
	if (_isEnhanced) {
	  ClassLoader loader = getPersistenceUnit().getEnhancedLoader();

          if (log.isLoggable(Level.FINEST))
            log.finest(L.l("loading bean class `{0}' from `{1}'", getBeanClass().getName(), loader));

          _instanceClass = Class.forName(getBeanClass().getName(), false, loader);
	}
	else {
	  ClassLoader loader = _instanceLoader;

	  if (loader == null)
	    loader = getPersistenceUnit().getEnhancedLoader();

          if (log.isLoggable(Level.FINEST))
            log.finest(L.l("loading instance class `{0}' from `{1}'", getInstanceClassName(), loader));

          _instanceClass = Class.forName(getInstanceClassName(), false, loader);
	}
      } catch (ClassNotFoundException e) {
	throw new RuntimeException(e);
      }

      if (! Entity.class.isAssignableFrom(_instanceClass)) {
	if (_exception != null)
	  throw new AmberRuntimeException(_exception);
	else if (_amberPersistenceUnit.getConfigException() != null)
	  throw new AmberRuntimeException(_amberPersistenceUnit.getConfigException());
	
	throw new AmberRuntimeException(L.l("'{0}' with classloader {1} is an illegal instance class",
					    _instanceClass.getName(), _instanceClass.getClassLoader()));
      }
    }

    return _instanceClass;
  }

  /**
   * Returns true if generated.
   */
  public boolean isGenerated()
  {
    return _isGenerated;
  }

  /**
   * Set true if generated.
   */
  public void setGenerated(boolean isGenerated)
  {
    // XXX: ejb/0600 vs ejb/0l00
    if (isEnhanced())
      _isGenerated = isGenerated;
  }

  /**
   * Sets the id.
   */
  public void setId(Id id)
  {
    _id = id;
  }

  /**
   * Returns the id.
   */
  public Id getId()
  {
    return _id;
  }

  /**
   * Returns the discriminator.
   */
  public Column getDiscriminator()
  {
    return _discriminator;
  }

  /**
   * Sets the discriminator.
   */
  public void setDiscriminator(Column discriminator)
  {
    _discriminator = discriminator;
  }

  /**
   * Set true for joined-subclass
   */
  public void setJoinedSubClass(boolean isJoinedSubClass)
  {
    _isJoinedSubClass = isJoinedSubClass;
  }

  /**
   * Set true for joined-subclass
   */
  public boolean isJoinedSubClass()
  {
    if (getParentType() != null)
      return getParentType().isJoinedSubClass();
    else
      return _isJoinedSubClass;
  }
  
  /**
   * Sets the discriminator value.
   */
  public String getDiscriminatorValue()
  {
    if (_discriminatorValue != null)
      return _discriminatorValue;
    else {
      String name = getBeanClass().getName(); 

      int p = name.lastIndexOf('.');
      if (p > 0)
	return name.substring(0, p);
      else
	return name;
    }
  }

  /**
   * Sets the discriminator value.
   */
  public void setDiscriminatorValue(String value)
  {
    _discriminatorValue = value;
  }

  /**
   * Sets the abstract table name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Gets the abstract table name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns true if read-only
   */
  public boolean isReadOnly()
  {
    return getTable().isReadOnly();
  }

  /**
   * Sets true if read-only
   */
  public void setReadOnly(boolean isReadOnly)
  {
    getTable().setReadOnly(isReadOnly);
  }

  /**
   * Returns the cache timeout.
   */
  public long getCacheTimeout()
  {
    return getTable().getCacheTimeout();
  }

  /**
   * Sets the cache timeout.
   */
  public void setCacheTimeout(long timeout)
  {
    getTable().setCacheTimeout(timeout);
  }

  /**
   * Adds a new field.
   */
  public void addField(AmberField field)
  {
    _fields.add(field);
    Collections.sort(_fields, new AmberFieldCompare());
  }

  /**
   * Returns the fields.
   */
  public ArrayList<AmberField> getFields()
  {
    return _fields;
  }

  /**
   * Returns the field with a given name.
   */
  public AmberField getField(String name)
  {
    if (_id != null) {
      ArrayList<IdField> keys = _id.getKeys();

      for (int i = 0; i < keys.size(); i++) {
	IdField key = keys.get(i);

	if (key.getName().equals(name))
	  return key;
      }
    }
    
    for (int i = 0; i < _fields.size(); i++) {
      AmberField field = _fields.get(i);

      if (field.getName().equals(name))
	return field;
    }
    
    return null;
  }

  /**
   * Returns the columns.
   */
  public ArrayList<Column> getColumns()
  {
    return getTable().getColumns();
  }
  
  /**
   * True if the load lifecycle callback should be generated.
   */
  public void setHasLoadCallback(boolean hasCallback)
  {
    _hasLoadCallback = hasCallback;
  }
  
  /**
   * True if the load lifecycle callback should be generated.
   */
  public boolean getHasLoadCallback()
  {
    return _hasLoadCallback;
  }

  /**
   * Returns the root type.
   */
  public EntityType getRootType()
  {
    EntityType parent = getParentType();

    if (parent != null)
      return parent.getRootType();
    else
      return this;
  }
  
  /**
   * Returns the parent type.
   */
  public EntityType getParentType()
  {
    return null;
  }

  /**
   * Adds a sub-class.
   */
  public void addSubClass(SubEntityType type)
  {
    if (_subEntities == null)
      _subEntities = new HashMap<String,EntityType>();

    _subEntities.put(type.getDiscriminatorValue(), type);
  }

  /**
   * Gets a sub-class.
   */
  public EntityType getSubClass(String discriminator)
  {
    if (_subEntities == null)
      return this;

    EntityType subType = _subEntities.get(discriminator);

    if (subType != null)
      return subType;
    else
      return this;
  }

  /**
   * Adds a @PostLoad callback.
   */
  public void addPostLoadCallback(JMethod callback)
  {
    _postLoadCallbacks.add(callback);
  }

  /**
   * Gets the post-load callback.
   */
  public ArrayList<JMethod> getPostLoadCallbacks()
  {
    return _postLoadCallbacks;
  }

  /**
   * Adds a pre-persist callback.
   */
  public void addPrePersistCallback(JMethod callback)
  {
    _prePersistCallbacks.add(callback);
  }

  /**
   * Gets the pre-persist callback.
   */
  public ArrayList<JMethod> getPrePersistCallbacks()
  {
    return _prePersistCallbacks;
  }

  /**
   * Adds a post-persist callback.
   */
  public void addPostPersistCallback(JMethod callback)
  {
    _postPersistCallbacks.add(callback);
  }

  /**
   * Gets the post-persist callback.
   */
  public ArrayList<JMethod> getPostPersistCallbacks()
  {
    return _postPersistCallbacks;
  }

  /**
   * Adds a pre-update callback.
   */
  public void addPreUpdateCallback(JMethod callback)
  {
    _preUpdateCallbacks.add(callback);
  }

  /**
   * Gets the pre-update callback.
   */
  public ArrayList<JMethod> getPreUpdateCallbacks()
  {
    return _preUpdateCallbacks;
  }

  /**
   * Adds a post-update callback.
   */
  public void addPostUpdateCallback(JMethod callback)
  {
    _postUpdateCallbacks.add(callback);
  }

  /**
   * Gets the post-update callback.
   */
  public ArrayList<JMethod> getPostUpdateCallbacks()
  {
    return _postUpdateCallbacks;
  }

  /**
   * Adds a pre-remove callback.
   */
  public void addPreRemoveCallback(JMethod callback)
  {
    _preRemoveCallbacks.add(callback);
  }

  /**
   * Gets the pre-remove callback.
   */
  public ArrayList<JMethod> getPreRemoveCallbacks()
  {
    return _preRemoveCallbacks;
  }

  /**
   * Adds a post-remove callback.
   */
  public void addPostRemoveCallback(JMethod callback)
  {
    _postRemoveCallbacks.add(callback);
  }

  /**
   * Gets the post-remove callback.
   */
  public ArrayList<JMethod> getPostRemoveCallbacks()
  {
    return _postRemoveCallbacks;
  }

  /**
   * Creates a new entity for this specific instance type.
   */
  public Entity createBean()
  {
    try {
      return (Entity) getInstanceClass().newInstance();
    } catch (Exception e) {
      throw new AmberRuntimeException(e);
    }
  }

  /**
   * Adds a stub method
   */
  public void addStubMethod(StubMethod method)
  {
    _methods.add(method);
  }

  /**
   * Returns the methods
   */
  public ArrayList<StubMethod> getMethods()
  {
    return _methods;
  }

  /**
   * Adds a dependency.
   */
  public void addDependency(Class cl)
  {
    addDependency(new ClassDependency(cl));
  }

  /**
   * Adds a dependency.
   */
  public void addDependency(JClass cl)
  {
    addDependency(new JClassDependency(cl));
  }

  /**
   * Adds a dependency.
   */
  public void addDependency(PersistentDependency depend)
  {
    if (! _dependencies.contains(depend))
      _dependencies.add(depend);
  }

  /**
   * Gets the dependency.
   */
  public ArrayList<PersistentDependency> getDependencies()
  {
    return _dependencies;
  }

  /**
   * Returns the home.
   */
  public AmberEntityHome getHome()
  {
    if (_home == null) {
      _home = getPersistenceUnit().getEntityHome(getName());
    }

    return _home;
  }

  /**
   * Returns the next load group.
   */
  public int nextLoadGroupIndex()
  {
    int nextLoadGroupIndex = getLoadGroupIndex() + 1;

    _loadGroupIndex = nextLoadGroupIndex;
    
    return nextLoadGroupIndex;
  }

  /**
   * Returns the current load group.
   */
  public int getLoadGroupIndex()
  {
    return _loadGroupIndex;
  }

  /**
   * Sets the next default loadGroupIndex
   */
  public void nextDefaultLoadGroupIndex()
  {
    _defaultLoadGroupIndex = nextLoadGroupIndex();
  }

  /**
   * Returns the current load group.
   */
  public int getDefaultLoadGroupIndex()
  {
    return _defaultLoadGroupIndex;
  }

  /**
   * Returns true if the load group is owned by this type (not a subtype).
   */
  public boolean isLoadGroupOwnedByType(int i)
  {
    return getDefaultLoadGroupIndex() <= i && i <= getLoadGroupIndex();
  }

  /**
   * Returns the next dirty index
   */
  public int nextDirtyIndex()
  {
    int dirtyIndex = getDirtyIndex();

    _dirtyIndex = dirtyIndex + 1;
    
    return dirtyIndex;
  }

  /**
   * Returns the current dirty group.
   */
  public int getDirtyIndex()
  {
    return _dirtyIndex;
  }

  /**
   * Returns the min dirty group.
   */
  public int getMinDirtyIndex()
  {
    return _minDirtyIndex;
  }

  /**
   * Returns true if the load group is owned by this type (not a subtype).
   */
  public boolean isDirtyIndexOwnedByType(int i)
  {
    return getMinDirtyIndex() <= i && i < getDirtyIndex();
  }

  /**
   * Set true if configured.
   */
  public boolean startConfigure()
  {
    synchronized (this) {
      if (_isConfigured)
	return false;
      
      _isConfigured = true;

      return true;
    }
  }

  /**
   * Initialize the entity.
   */
  public void init()
    throws ConfigException
  {
    if (_exception != null)
      return;
    
    if (! _lifecycle.toInit())
      return;

    // forces table lazy load
    getTable();

    assert getId() != null : "null id for " + _name;

    getId().init();
      
    for (AmberField field : _fields) {
      if (field.isUpdateable())
	field.setIndex(nextDirtyIndex());

      field.init();
    }

    /*
    if (_amberPersistenceUnitenceUnitenceUnitenceUnitenceUnitenceUnitenceUnitenceUnitenceUnitenceUnitenceUnitenceUnit.getCreateDatabaseTables())
     getTable().createDatabaseTable(_amberPersistenceUnitenceUnitenceUnitenceUnitenceUnitenceUnitenceUnitenceUnitenceUnitenceUnitenceUnitenceUnit);

    if (_amberPersistenceUnitenceUnitenceUnitenceUnitenceUnitenceUnitenceUnitenceUnitenceUnitenceUnitenceUnitenceUnit.getValidateDatabaseTables())
      getTable().validateDatabaseTable(_amberPersistenceUnitenceUnitenceUnitenceUnitenceUnitenceUnitenceUnitenceUnitenceUnitenceUnitenceUnitenceUnit);
    */
  }

  /**
   * Start the entry.
   */
  public void start()
    throws ConfigException
  {
    init();
    
    if (! _lifecycle.toActive())
      return;
  }

  /**
   * Converts the value.
   */
  public String generateCastFromObject(String value)
  {
    return "((" + getInstanceClassName() + ") " + value + ")";
  }

  /**
   * Generates a string to load the field.
   */
  public int generateLoad(JavaWriter out, String rs,
			  String indexVar, int index)
    throws IOException
  {
    // ejb/0ag3
    // out.print("(" + getInstanceClassName() + ") ");
    
    out.print("aConn.loadProxy(\"" + getName() + "\", ");

    index = getId().generateLoadForeign(out, rs, indexVar, index);

    out.println(");");

    return index;
  }

  /**
   * Returns true if there's a field with the matching load group.
   */
  public boolean hasLoadGroup(int loadGroupIndex)
  {
    if (loadGroupIndex == 0)
      return true;

    for (AmberField field : getFields()) {
      if (field.hasLoadGroup(loadGroupIndex))
	return true;
    }

    return false;
  }
    
  /**
   * Generates a string to load the field.
   */
  public int generateLoad(JavaWriter out, String rs,
			  String indexVar, int index, int loadGroupIndex)
    throws IOException
  {
    if (loadGroupIndex == 0 && _discriminator != null)
      index++;

    ArrayList<AmberField> fields = getFields();
    for (int i = 0; i < fields.size(); i++) {
      AmberField field = fields.get(i);

      if (field.getLoadGroupIndex() == loadGroupIndex)
	index = field.generateLoad(out, rs, indexVar, index);
    }

    return index;
  }

  /**
   * Generates a string to set the field.
   */
  public void generateSet(JavaWriter out, String pstmt,
			  String index, String value)
    throws IOException
  {
    if (getId() != null)
      getId().generateSet(out, pstmt, index, value);
  }

  /**
   * Gets the value.
   */
  public Object getObject(AmberConnection aConn, ResultSet rs, int index)
    throws SQLException
  {
    return getHome().loadLazy(aConn, rs, index);
  }

  /**
   * Finds the object
   */
  public EntityItem findItem(AmberConnection aConn, ResultSet rs, int index)
    throws SQLException
  {
    return getHome().findItem(aConn, rs, index);
  }

  /**
   * Gets the value.
   */
  public Object getLoadObject(AmberConnection aConn,
			      ResultSet rs, int index)
    throws SQLException
  {
    return getHome().loadFull(aConn, rs, index);
  }

  /**
   * Sets the named generator.
   */
  public void setGenerator(String name, IdGenerator gen)
  {
    _idGenMap.put(name, gen);
  }

  /**
   * Sets the named generator.
   */
  public IdGenerator getGenerator(String name)
  {
    return _idGenMap.get(name);
  }

  /**
   * Gets the named generator.
   */
  public long nextGeneratorId(AmberConnection aConn, String name)
    throws SQLException
  {
    return _idGenMap.get(name).allocate(aConn);
  }

  /**
   * Loads from an object.
   */
  public void generateLoadFromObject(JavaWriter out, String obj)
    throws IOException
  {
    getId().generateLoadFromObject(out, obj);
    
    ArrayList<AmberField> fields = getFields();
    for (int i = 0; i < fields.size(); i++) {
      AmberField field = fields.get(i);

      field.generateLoadFromObject(out, obj);
    }
  }

  /**
   * Copy from an object.
   */
  public void generateCopyLoadObject(JavaWriter out,
				     String dst, String src,
				     int loadGroup)
    throws IOException
  {
    if (getParentType() != null)
      getParentType().generateCopyUpdateObject(out, dst, src, loadGroup);
    
    ArrayList<AmberField> fields = getFields();
    for (int i = 0; i < fields.size(); i++) {
      AmberField field = fields.get(i);

      // XXX: setter issue, too

      field.generateCopyLoadObject(out, dst, src, loadGroup);
    }
  }

  /**
   * Copy from an object.
   */
  public void generateCopyUpdateObject(JavaWriter out,
				       String dst, String src,
				       int updateIndex)
    throws IOException
  {
    if (getParentType() != null)
      getParentType().generateCopyUpdateObject(out, dst, src, updateIndex);
    
    ArrayList<AmberField> fields = getFields();
    for (int i = 0; i < fields.size(); i++) {
      AmberField field = fields.get(i);

      field.generateCopyUpdateObject(out, dst, src, updateIndex);
    }
  }

  /**
   * Generates the select clause for a load.
   */
  public String generateKeyLoadSelect(String id)
  {
    String select = getId().generateLoadSelect(id);

    if (getDiscriminator() != null) {
      if (select != null && ! select.equals(""))
	select = select + ", ";

      select = select + getDiscriminator().getName();
    }

    return select;
  }

  /**
   * Generates the select clause for a load.
   */
  public String generateFullLoadSelect(String id)
  {
    CharBuffer cb = CharBuffer.allocate();

    String idSelect = getId().generateSelect(id);

    if (idSelect != null)
      cb.append(idSelect);

    String loadSelect = generateLoadSelect(id);

    if (! idSelect.equals("") && ! loadSelect.equals(""))
      cb.append(",");
    
    cb.append(loadSelect);

    return cb.close();
  }

  /**
   * Generates the select clause for a load.
   */
  public String generateLoadSelect(String id)
  {
    return generateLoadSelect(getTable(), id, 0);
  }

  /**
   * Generates the select clause for a load.
   */
  public String generateLoadSelect(Table table, String id)
  {
    return generateLoadSelect(table, id, 0);
  }

  /**
   * Generates the select clause for a load.
   */
  public String generateLoadSelect(Table table, String id, int loadGroup)
  {
    CharBuffer cb = CharBuffer.allocate();

    boolean hasSelect = false;

    if (loadGroup == 0 && getParentType() != null) {
      String parentSelect =
	getParentType().generateLoadSelect(table, id, loadGroup);

      cb.append(parentSelect);
      if (! parentSelect.equals(""))
	hasSelect = true;
    }
    else if (loadGroup == 0 && getDiscriminator() != null) {
      if (id != null) {
	cb.append(id + ".");
      }
      cb.append(getDiscriminator().getName());
      hasSelect = true;
    }

    ArrayList<AmberField> fields = getFields();
    for (int i = 0; i < fields.size(); i++) {
      AmberField field = fields.get(i);

      if (field.getLoadGroupIndex() != loadGroup)
	continue;

      String propSelect = field.generateLoadSelect(table, id);
      if (propSelect == null)
	continue;
      
      if (hasSelect)
	cb.append(", ");
      hasSelect = true;

      cb.append(propSelect);
    }

    if (cb.length() == 0)
      return null;
    else
      return cb.close();
  }

  /**
   * Returns the load mask generated on create.
   */
  public long getCreateLoadMask(int group)
  {
    long mask = 0;

    for (int i = 0; i < _fields.size(); i++) {
      mask |= _fields.get(i).getCreateLoadMask(group);
    }

    return mask;
  }
    
  /**
   * Generates the update sql.
   */
  public String generateCreateSQL(Table table)
  {
    CharBuffer sql = new CharBuffer();

    sql.append("insert into " + table.getName() + " (");

    boolean isFirst = true;
    
    ArrayList<String> idColumns = new ArrayList<String>();
    for (IdField field : getId().getKeys()) {
      for (Column key : field.getColumns()) {
	String name;
	
	if (table == key.getTable())
	  name = key.getName();
	else
	  name = table.getDependentIdLink().getSourceColumn(key).getName();
	    
	idColumns.add(name);

	if (! isFirst)
	  sql.append(", ");
	isFirst = false;

	sql.append(name);
      }
    }

    if (table == getTable() && getDiscriminator() != null) {
      if (! isFirst)
	sql.append(", ");
      isFirst = false;

      sql.append(getDiscriminator().getName());
    }
    
    ArrayList<String> columns = new ArrayList<String>();
    generateInsertColumns(table, columns);

    for (String columnName : columns) {
      if (! isFirst)
	sql.append(", ");
      isFirst = false;

      sql.append(columnName);
    }

    sql.append(") values (");

    isFirst = true;
    for (int i = 0; i < idColumns.size(); i++) {
      if (! isFirst)
	sql.append(", ");
      isFirst = false;

      sql.append("?");
    }

    if (table == getTable() && getDiscriminator() != null) {
      if (! isFirst)
	sql.append(", ");
      isFirst = false;

      sql.append("'" + getDiscriminatorValue() + "'");
    }

    for (int i = 0; i < columns.size(); i++) {
      if (! isFirst)
	sql.append(", ");
      isFirst = false;

      sql.append("?");
    }
    
    sql.append(")");
    
    return sql.toString();
  }

  protected void generateInsertColumns(Table table, ArrayList<String> columns)
  {
    if (getParentType() != null)
      getParentType().generateInsertColumns(table, columns);
    
    for (AmberField field : getFields()) {
      if (field.getTable() == table)
	field.generateInsertColumns(columns);
    }
  }

  /**
   * Generates the update sql.
   */
  public void generateInsertSet(JavaWriter out,
				Table table, 
				String pstmt,
				String query,
				String obj)
    throws IOException
  {
    if (getParentType() != null)
      getParentType().generateInsertSet(out, table, pstmt, query, obj);

    for (AmberField field : getFields()) {
      if (field.getTable() == table)
	field.generateInsertSet(out, pstmt, query, obj);
    }
  }

  /**
   * Generates the select clause for a load.
   */
  public String generateIdSelect(String id)
  {
    CharBuffer cb = CharBuffer.allocate();

    cb.append(getId().generateSelect(id));

    if (_discriminator != null) {
      cb.append(", ");
      cb.append(_discriminator.getName());
    }

    return cb.close();
  }

  /**
   * Generates the foreign delete
   */
  public void generateInvalidateForeign(JavaWriter out)
    throws IOException
  {
    for (AmberField field : getFields()) {
      field.generateInvalidateForeign(out);
    }
  }

  /**
   * Generates any expiration code.
   */
  public void generateExpire(JavaWriter out)
    throws IOException
  {
    for (AmberField field : getFields()) {
      field.generateExpire(out);
    }
  }

  /**
   * Gets a matching getter.
   */
  public JMethod getGetter(String name)
  {
    return getGetter(_beanClass, name);
  }

  /**
   * Gets a matching getter.
   */
  public static JMethod getGetter(JClass cl, String name)
  {
    JMethod []methods = cl.getMethods();

    for (int i = 0; i < methods.length; i++) {
      JClass []param = methods[i].getParameterTypes();
      String methodName = methods[i].getName();

      if (name.equals(methodName) && param.length == 0)
	return methods[i];
    }

    cl = cl.getSuperClass();

    if (cl != null)
      return getGetter(cl, name);
    else
      return null;
  }

  /**
   * Gets a matching getter.
   */
  public static JField getField(JClass cl, String name)
  {
    JField []fields = cl.getDeclaredFields();

    for (int i = 0; i < fields.length; i++) {
      if (name.equals(fields[i].getName()))
	return fields[i];
    }

    cl = cl.getSuperClass();

    if (cl != null)
      return getField(cl, name);
    else
      return null;
  }

  /**
   * Gets a matching getter.
   */
  public static JMethod getSetter(JClass cl, String name)
  {
    JMethod []methods = cl.getMethods();

    for (int i = 0; i < methods.length; i++) {
      JClass []param = methods[i].getParameterTypes();
      String methodName = methods[i].getName();

      if (name.equals(methodName) && param.length == 1)
	return methods[i];
    }

    cl = cl.getSuperClass();

    if (cl != null)
      return getSetter(cl, name);
    else
      return null;
  }

  /**
   * Generates the update sql.
   */
  public void generateUpdateSQLPrefix(CharBuffer sql)
  {
    sql.append("update " + getTable().getName() + " set ");
  }

  /**
   * Generates the update sql.
   *
   * @param sql the partially built sql
   * @param group the dirty group
   * @param mask the group's mask
   * @param isFirst marks the first set group
   */
  public boolean generateUpdateSQLComponent(CharBuffer sql,
					    int group,
					    long mask,
					    boolean isFirst)
  {
    ArrayList<AmberField> fields = getFields();

    while (mask != 0) {
      int i = 0;
      for (i = 0; (mask & (1L << i)) == 0; i++) {
      }

      mask &= ~(1L << i);

      AmberField field = null;

      for (int j = 0; j < fields.size(); j++) {
	field = fields.get(j);
	
	if (field.getIndex() == i + group * 64)
	  break;
	else
	  field = null;
      }

      if (field != null) {
	if (! isFirst)
	  sql.append(", ");
	isFirst = false;
	
	field.generateUpdate(sql);
      }
    }

    return isFirst;
  }

  /**
   * Generates the update sql.
   */
  public void generateUpdateSQLSuffix(CharBuffer sql)
  {
    sql.append(" where ");

    sql.append(getId().generateMatchArgWhere(null));
  }

  /**
   * Generates the update sql.
   */
  public String generateUpdateSQL(long mask)
  {
    if (mask == 0)
      return null;
    
    CharBuffer sql = CharBuffer.allocate();

    sql.append("update " + getTable().getName() + " set ");

    boolean isFirst = true;

    ArrayList<AmberField> fields = getFields();

    while (mask != 0) {
      int i = 0;
      for (i = 0; (mask & (1L << i)) == 0; i++) {
      }

      mask &= ~(1L << i);

      AmberField field = null;

      for (int j = 0; j < fields.size(); j++) {
	field = fields.get(j);
	
	if (field.getIndex() == i)
	  break;
	else
	  field = null;
      }

      if (field != null) {
	if (! isFirst)
	  sql.append(", ");
	isFirst = false;
	
	field.generateUpdate(sql);
      }
    }

    if (isFirst)
      return null;

    sql.append(" where ");

    sql.append(getId().generateMatchArgWhere(null));
    
    return sql.toString();
  }

  /**
   * Generates code after the remove.
   */
  public void generatePreDelete(JavaWriter out)
    throws IOException
  {
    for (AmberField field : getFields()) {
      field.generatePreDelete(out);
    }
  }

  /**
   * Generates code after the remove.
   */
  public void generatePostDelete(JavaWriter out)
    throws IOException
  {
    for (AmberField field : getFields()) {
      field.generatePostDelete(out);
    }
  }
  
  /**
   * Deletes by the primary key.
   */
  public void delete(AmberConnection aConn, Object key)
    throws SQLException
  {
    getHome().delete(aConn, key);
  }
  
  /**
   * Deletes by the primary key.
   */
  public void update(Entity entity)
    throws SQLException
  {
    // aConn.addCompletion(_tableCompletion);
  }

  /**
   * Returns a completion for the given field.
   */
  public AmberCompletion createManyToOneCompletion(String name,
						   Entity source,
						   Object newTarget)
  {
    AmberField field = getField(name);

    if (field instanceof EntityManyToOneField) {
      EntityManyToOneField manyToOne = (EntityManyToOneField) field;

      return getTable().getInvalidateCompletion();
    }
    else
      throw new IllegalStateException();
  }

  /**
   * XXX: temp hack.
   */
  public boolean isEJBProxy(String typeName)
  {
    return (getBeanClass() != getProxyClass() &&
	    getProxyClass().getName().equals(typeName));
  }

  /**
   * Printable version of the entity.
   */
  public String toString()
  {
    return "EntityType[" + _beanClass.getName() + "]";
  }
}
