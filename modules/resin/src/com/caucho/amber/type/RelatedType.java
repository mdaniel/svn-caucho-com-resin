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
 * @author Rodrigo Westrupp
 */

package com.caucho.amber.type;

import com.caucho.amber.AmberRuntimeException;
import com.caucho.amber.entity.AmberCompletion;
import com.caucho.amber.entity.AmberEntityHome;
import com.caucho.amber.entity.Entity;
import com.caucho.amber.entity.EntityItem;
import com.caucho.amber.field.AmberField;
import com.caucho.amber.field.EntityManyToOneField;
import com.caucho.amber.field.Id;
import com.caucho.amber.field.IdField;
import com.caucho.amber.field.VersionField;
import com.caucho.amber.idgen.AmberTableGenerator;
import com.caucho.amber.idgen.IdGenerator;
import com.caucho.amber.idgen.SequenceIdGenerator;
import com.caucho.amber.manager.AmberConnection;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.table.Column;
import com.caucho.amber.table.Table;
import com.caucho.bytecode.JClass;
import com.caucho.config.ConfigException;
import com.caucho.java.JavaWriter;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base for entity or mapped-superclass types.
 */
abstract public class RelatedType extends AbstractStatefulType {
  private static final Logger log = Logger.getLogger(RelatedType.class.getName());
  private static final L10N L = new L10N(RelatedType.class);

  Table _table;

  private String _rootTableName;

  private ArrayList<Table> _secondaryTables
    = new ArrayList<Table>();

  private ArrayList<ListenerType> _listeners
    = new ArrayList<ListenerType>();

  private Id _id;

  private String _discriminatorValue;
  private boolean _isJoinedSubClass;

  private HashSet<String> _eagerFieldNames;

  private HashMap<String,EntityType> _subEntities;

  private boolean _hasDependent;

  private JClass _proxyClass;

  private AmberEntityHome _home;

  protected int _defaultLoadGroupIndex;
  protected int _loadGroupIndex;

  protected int _minDirtyIndex;
  protected int _dirtyIndex;

  private boolean _excludeDefaultListeners;
  private boolean _excludeSuperclassListeners;

  protected boolean _hasLoadCallback;

  private HashMap<String,IdGenerator> _idGenMap
    = new HashMap<String,IdGenerator>();

  private final Lifecycle _lifecycle = new Lifecycle();

  private VersionField _versionField;


  public RelatedType(AmberPersistenceUnit amberPersistenceUnit)
  {
    super(amberPersistenceUnit);
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

    if (_rootTableName == null)
      _rootTableName = table.getName();
  }

  /**
   * Returns the root table name.
   */
  public String getRootTableName()
  {
    return _rootTableName;
  }

  /**
   * Sets the root table name.
   */
  public void setRootTableName(String rootTableName)
  {
    _rootTableName = rootTableName;
  }

  /**
   * Returns the version field.
   */
  public VersionField getVersionField()
  {
    return _versionField;
  }

  /**
   * Sets the version field.
   */
  public void setVersionField(VersionField versionField)
  {
    addField(versionField);

    _versionField = versionField;
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
   * Gets the secondary tables.
   */
  public ArrayList<Table> getSecondaryTables()
  {
    return _secondaryTables;
  }

  /**
   * Adds an entity listener.
   */
  public void addListener(ListenerType listener)
  {
    if (_listeners.contains(listener))
      return;

    _listeners.add(listener);
  }

  /**
   * Gets the entity listeners.
   */
  public ArrayList<ListenerType> getListeners()
  {
    return _listeners;
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
   * Returns true if and only if it has a
   * many-to-one, one-to-one or embedded field/property.
   */
  public boolean hasDependent()
  {
    return _hasDependent;
  }

  /**
   * Sets true if and only if it has a
   * many-to-one, one-to-one or embedded field/property.
   */
  public void setHasDependent(boolean hasDependent)
  {
    _hasDependent = hasDependent;
  }

  /**
   * Returns the java type.
   */
  public String getForeignTypeName()
  {
    return getId().getForeignTypeName();
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

  /**
   * Gets the instance class.
   */
  public Class getInstanceClass()
  {
    return getInstanceClass(Entity.class);
  }

  /**
   * Returns true if the corresponding class is abstract.
   */
  public boolean isAbstractClass()
  {
    // ejb/0600 - EJB 2.1 are not abstract in this sense
    return getBeanClass().isAbstract() && _proxyClass == null;
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
    super.addField(field);

    if (! field.isLazy()) {

      if (_eagerFieldNames == null)
        _eagerFieldNames = new HashSet<String>();

      _eagerFieldNames.add(field.getName());
    }
  }

  /**
   * Gets the EAGER field names.
   */
  public HashSet<String> getEagerFieldNames()
  {
    return _eagerFieldNames;
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

    return super.getField(name);
  }

  /**
   * Returns the columns.
   */
  public ArrayList<Column> getColumns()
  {
    return getTable().getColumns();
  }

  /**
   * Gets the exclude default listeners flag.
   */
  public boolean getExcludeDefaultListeners()
  {
    return _excludeDefaultListeners;
  }

  /**
   * Sets the exclude default listeners flag.
   */
  public void setExcludeDefaultListeners(boolean b)
  {
    _excludeDefaultListeners = b;
  }

  /**
   * Gets the exclude superclass listeners flag.
   */
  public boolean getExcludeSuperclassListeners()
  {
    return _excludeSuperclassListeners;
  }

  /**
   * Sets the exclude superclass listeners flag.
   */
  public void setExcludeSuperclassListeners(boolean b)
  {
    _excludeSuperclassListeners = b;
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
  public RelatedType getRootType()
  {
    RelatedType parent = getParentType();

    if (parent != null)
      return parent.getRootType();
    else
      return this;
  }

  /**
   * Returns the parent type.
   */
  public RelatedType getParentType()
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
  public RelatedType getSubClass(String discriminator)
  {
    if (_subEntities == null)
      return this;

    RelatedType subType = _subEntities.get(discriminator);

    if (subType != null)
      return subType;
    else {
      // jpa/0l15
      for (EntityType subEntity : _subEntities.values()) {
        subType = subEntity.getSubClass(discriminator);

        if (subType != subEntity)
          return subType;
      }

      return this;
    }
  }

  /**
   * Creates a new entity for this specific instance type.
   */
  public Entity createBean()
  {
    try {
      Entity entity = (Entity) getInstanceClass().newInstance();

      return entity;
    } catch (Exception e) {
      throw new AmberRuntimeException(e);
    }
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
   * Initialize the entity.
   */
  public void init()
    throws ConfigException
  {
    if (getConfigException() != null)
      return;

    if (! _lifecycle.toInit())
      return;

    // forces table lazy load
    getTable();

    log.log(Level.FINE, "RelatedType.init() has id? " + (getId() != null));

    if (this instanceof EntityType) {
      assert getId() != null : "null id for " + getName();

      getId().init();
    }

    for (AmberField field : getFields()) {
      if (field.isUpdateable())
        field.setIndex(nextDirtyIndex());

      field.init();
    }

    if (getMappedSuperclassFields() == null)
      return;

    for (AmberField field : getMappedSuperclassFields()) {
      if (field.isUpdateable())
        field.setIndex(nextDirtyIndex());

      field.init();
    }
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
   * Generates loading code after the basic fields.
   */
  public int generatePostLoadSelect(JavaWriter out, int index, int loadGroupIndex)
    throws IOException
  {
    if (loadGroupIndex == 0 && getDiscriminator() != null)
      index++;

    RelatedType parentType = this;

    // jpa/0l40
    do {
      ArrayList<AmberField> fields = parentType.getMappedSuperclassFields();

      for (int i = 0; i < 2; i++) {
        if (fields != null) {
          for (int j = 0; j < fields.size(); j++) {
            AmberField field = fields.get(j);

            // jpa/0l40 if (field.getLoadGroupIndex() == loadGroupIndex)
            index = field.generatePostLoadSelect(out, index);
          }
        }

        fields = parentType.getFields();
      }
    } while ((parentType = parentType.getParentType()) != null);

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
    IdGenerator idGen = _idGenMap.get(name);

    if (idGen instanceof SequenceIdGenerator) {
      ((SequenceIdGenerator) idGen).init(_amberPersistenceUnit);
    }
    else if (idGen instanceof AmberTableGenerator) {
      // jpa/0g60
      ((AmberTableGenerator) idGen).init(_amberPersistenceUnit);
    }

    return idGen.allocate(aConn);
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
    if (getParentType() != null) // jpa/0ge3
      getParentType().generateCopyLoadObject(out, dst, src, loadGroup);

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
  public void generateCopyMergeObject(JavaWriter out,
                                      String dst, String src,
                                      int loadGroup)
    throws IOException
  {
    if (getParentType() != null)
      getParentType().generateCopyMergeObject(out, dst, src, loadGroup);

    ArrayList<AmberField> fields = getFields();

    for (int i = 0; i < fields.size(); i++) {
      AmberField field = fields.get(i);

      field.generateCopyMergeObject(out, dst, src, loadGroup);
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
   * Checks entity-relationships from an object.
   */
  public void generateDumpRelationships(JavaWriter out,
                                        int updateIndex)
    throws IOException
  {
    if (getParentType() != null) // jpa/0ge3
      getParentType().generateDumpRelationships(out, updateIndex);

    ArrayList<AmberField> fields = getFields();

    for (int i = 0; i < fields.size(); i++) {
      AmberField field = fields.get(i);

      field.generateDumpRelationships(out, updateIndex);
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

    /*
    if (loadGroup == 0 && getParentType() != null) {
      // jpa/0ge3
      if (getParentType() instanceof EntityType) {
        String parentSelect =
          getParentType().generateLoadSelect(table, id,
                                             loadGroup, getMappedSuperclassFields());

        // jpa/0ge2
        if (parentSelect != null) {
          cb.append(parentSelect);
          if (! parentSelect.equals(""))
            hasSelect = true;
        }
      }

    }
    else
    */

    if ((getTable() == table) && // jpa/0l11
        (loadGroup == 0 && getDiscriminator() != null)) {

      if (id != null) {
        if (getDiscriminator().getTable() == getTable()) {
          cb.append(id + ".");
          cb.append(getDiscriminator().getName());
        }
        else {
          // jpa/0l4b
          cb.append("'" + getDiscriminatorValue() + "'");
        }
      }

      hasSelect = true;
    }

    String propSelect = super.generateLoadSelect(table,
                                                 id,
                                                 loadGroup,
                                                 hasSelect,
                                                 null);
    // jpa/0s26
    if (propSelect != null)
      cb.append(propSelect);

    if (cb.length() == 0)
      return null;
    else
      return cb.close();
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

    if (getDiscriminator() != null) {
      cb.append(", ");
      cb.append(getDiscriminator().getName());
    }

    return cb.close();
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

        // jpa/0x00
        if (field instanceof VersionField)
          continue;

        if (! isFirst)
          sql.append(", ");
        isFirst = false;

        field.generateUpdate(sql);
      }
    }

    // jpa/0x00
    for (int j = 0; j < fields.size(); j++) {
      AmberField field = fields.get(j);

      if (field instanceof VersionField) {
        if (! isFirst)
          sql.append(", ");
        isFirst = false;

        field.generateUpdate(sql);
        break;
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

    // optimistic locking
    if (_versionField != null) {
      sql.append(" and ");
      sql.append(_versionField.generateMatchArgWhere(null));
    }
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
    return "RelatedType[" + _beanClass.getName() + "]";
  }
}
