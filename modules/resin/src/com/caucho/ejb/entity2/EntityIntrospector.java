/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.ejb.entity2;

import com.caucho.amber.AmberManager;
import com.caucho.amber.field.*;
import com.caucho.amber.field.Id;
import com.caucho.amber.idgen.IdGenerator;
import com.caucho.amber.table.Column;
import com.caucho.amber.table.ForeignColumn;
import com.caucho.amber.table.LinkColumns;
import com.caucho.amber.table.Table;
import com.caucho.amber.type.EntityType;
import com.caucho.amber.type.*;
import com.caucho.bytecode.*;
import com.caucho.config.ConfigException;
import com.caucho.ejb.EjbServerManager;
import com.caucho.jdbc.JdbcMetaData;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import javax.ejb.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Configuration for an entity bean
 */
public class EntityIntrospector {
  private static final L10N L = new L10N(EntityIntrospector.class);

  private static HashSet<String> _propertyAnnotations
    = new HashSet<String>();

  private EjbServerManager _ejbManager;

  private HashMap<String,EntityType> _entityMap =
    new HashMap<String,EntityType>();

  private ArrayList<Completion> _linkCompletions = new ArrayList<Completion>();
  private ArrayList<Completion> _depCompletions = new ArrayList<Completion>();

  /**
   * Creates the introspector.
   */
  public EntityIntrospector(EjbServerManager manager)
  {
    _ejbManager = manager;
  }

  /**
   * Introspects.
   */
  public EntityType introspect(JClass type)
    throws ConfigException, SQLException
  {
    JAnnotation entityAnn = type.getAnnotation(Entity.class);

    if (entityAnn == null)
      throw new ConfigException(L.l("'{0}' is not an @Entity class.",
				    type));

    validateType(type);

    boolean isField = entityAnn.get("access") == AccessType.FIELD;

    EjbServerManager serverManager = _ejbManager;
    AmberManager amberManager = serverManager.getAmberManager();

    JAnnotation inheritanceAnn = type.getAnnotation(Inheritance.class);

    EntityType parentType = null;

    if (inheritanceAnn != null) {
      for (JClass parentClass = type.getSuperClass();
	   parentClass != null;
	   parentClass = parentClass.getSuperClass()) {
	JAnnotation parentEntity = parentClass.getAnnotation(Entity.class);

	if (parentEntity != null) {
	  parentType = introspect(parentClass);
	  break;
	}
      }
    }

    String entityName = type.getName();
    int p = entityName.lastIndexOf('.');
    if (p > 0)
      entityName = entityName.substring(p + 1);

    EntityType entityType = _entityMap.get(entityName);

    if (entityType != null)
      return entityType;

    entityType = amberManager.createEntity(entityName, type);
    _entityMap.put(entityName, entityType);

    if (isField)
      entityType.setFieldAccess(true);

    entityType.setInstanceClassName(type.getName() + "__ResinExt");
    entityType.setEnhanced(true);

    Table table = null;
    JAnnotation tableAnn = type.getAnnotation(javax.ejb.Table.class);

    String tableName = null;
    if (tableAnn != null)
      tableName = (String) tableAnn.get("name");

    if (tableName == null || tableName.equals(""))
      tableName = entityName;

    if (parentType == null)
      entityType.setTable(amberManager.createTable(tableName));
    else if (parentType.isJoinedSubClass())
      entityType.setTable(amberManager.createTable(tableName));
    else
      entityType.setTable(parentType.getTable());

    JAnnotation secondaryTableAnn = type.getAnnotation(SecondaryTable.class);

    Table secondaryTable = null;

    if (inheritanceAnn != null)
      introspectInheritance(amberManager, entityType, type);

    if (secondaryTableAnn != null) {
      String secondaryName = (String) secondaryTableAnn.get("name");

      secondaryTable = amberManager.createTable(secondaryName);

      entityType.addSecondaryTable(secondaryTable);
    }

    if (entityType.getId() != null) {
    }
    else if (isField)
      introspectIdField(amberManager, entityType, parentType, type);
    else
      introspectIdMethod(amberManager, entityType, parentType, type);

    if (isField)
      introspectFields(amberManager, entityType, parentType, type);
    else
      introspectMethods(amberManager, entityType, parentType, type);

    for (JMethod method : type.getMethods()) {
      introspectCallbacks(entityType, method);
    }

    if (secondaryTableAnn != null) {
      Object []join = (Object []) secondaryTableAnn.get("join");
      JAnnotation []joinAnn = new JAnnotation[join.length];
      System.arraycopy(join, 0, joinAnn, 0, join.length);

      linkSecondaryTable(entityType.getTable(),
			 secondaryTable,
			 joinAnn);
    }

    return entityType;
  }

  /**
   * Introspects the callbacks.
   */
  public void introspectCallbacks(EntityType type, JMethod method)
    throws ConfigException
  {
    JClass []param = method.getParameterTypes();

    if (method.getAnnotation(PrePersist.class) != null) {
      type.addPrePersistCallback(method);
    }

    if (method.getAnnotation(PostPersist.class) != null) {
      type.addPostPersistCallback(method);
    }
  }

  /**
   * Validates the bean
   */
  public void validateType(JClass type)
    throws ConfigException
  {
    if (type.isFinal())
      throw new ConfigException(L.l("'{0}' must not be final.  Entity beans may not be final.",
				    type.getName()));

    if (type.isAbstract())
      throw new ConfigException(L.l("'{0}' must not be abstract.  Entity beans may not be abstract.",
				    type.getName()));

    validateConstructor(type);

    for (JMethod method : type.getMethods()) {
      if (method.isFinal())
	throw new ConfigException(L.l("'{0}' must not be final.  Entity beans methods may not be final.",
				    method.getFullName()));
    }
  }

  /**
   * Checks for a valid constructor.
   */
  public void validateConstructor(JClass type)
    throws ConfigException
  {
    for (JMethod ctor : type.getConstructors()) {
      JClass []param = ctor.getParameterTypes();

      if (param.length == 0 && ctor.isPublic())
	return;
    }

    throw new ConfigException(L.l("'{0}' needs a public, no-arg constructor.",
				  type.getName()));
  }

  /**
   * Validates a non-getter method.
   */
  public void validateNonGetter(JMethod method)
    throws ConfigException
  {
    JAnnotation ann = isAnnotatedMethod(method);
    
    if (ann != null) {
      throw new ConfigException(L.l("'{0}' is not a valid annotation for {1}.  Only public getters and fields may have property annotations.",
				    ann.getType(), method.getFullName()));
    }
  }

  /**
   * Validates a non-getter method.
   */
  private JAnnotation isAnnotatedMethod(JMethod method)
    throws ConfigException
  {
    for (JAnnotation ann : method.getDeclaredAnnotations()) {
      if (_propertyAnnotations.contains(ann.getType())) {
	return ann;
      }
    }

    return null;
  }

  /**
   * Completes all partial bean introspection.
   */
  public void init()
    throws ConfigException
  {
    while (_depCompletions.size() > 0 || _linkCompletions.size() > 0) {
      while (_linkCompletions.size() > 0) {
	Completion completion = _linkCompletions.remove(0);

	completion.complete();
      }

      if (_depCompletions.size() > 0) {
	Completion completion = _depCompletions.remove(0);

	completion.complete();
      }
    }
  }

  /**
   * Introspects the Inheritance
   */
  private void introspectInheritance(AmberManager manager,
				     EntityType entityType,
				     JClass type)
    throws ConfigException, SQLException
  {
    JAnnotation inheritanceAnn = type.getAnnotation(Inheritance.class);

    String discriminatorValue = inheritanceAnn.getString("discriminatorValue");

    if (discriminatorValue == null || discriminatorValue.equals("")) {
      String name = entityType.getBeanClass().getName();
      int p = name.lastIndexOf('.');
      if (p > 0)
	name = name.substring(p + 1);

      discriminatorValue = name;
    }

    entityType.setDiscriminatorValue(discriminatorValue);

    if (entityType instanceof SubEntityType) {
      SubEntityType subType = (SubEntityType) entityType;

      subType.getParentType().addSubClass(subType);

      JAnnotation joinAnn = type.getAnnotation(InheritanceJoinColumn.class);

      if (subType.isJoinedSubClass()) {
	linkInheritanceTable(subType.getRootType().getTable(),
			     subType.getTable(),
			     joinAnn);

	subType.setId(new SubId(subType, subType.getRootType()));
      }

      return;
    }

    switch ((InheritanceType) inheritanceAnn.get("strategy")) {
    case JOINED:
      entityType.setJoinedSubClass(true);
      break;
    }

    JAnnotation discriminatorAnn =
      type.getAnnotation(DiscriminatorColumn.class);

    String columnName = null;

    if (discriminatorAnn != null)
      columnName = discriminatorAnn.getString("name");

    if (columnName == null || columnName.equals(""))
      columnName = "TYPE";

    Type columnType = null;

    switch ((DiscriminatorType) inheritanceAnn.get("discriminatorType")) {
    case STRING:
      columnType = StringType.create();
      break;
    case CHAR:
      columnType = PrimitiveCharType.create();
      break;
    case INTEGER:
      columnType = PrimitiveIntType.create();
      break;
    default:
      throw new IllegalStateException();
    }

    Column column = entityType.getTable().createColumn(columnName,
						       columnType);

    if (discriminatorAnn != null) {
      column.setNotNull(! discriminatorAnn.getBoolean("nullable"));

      column.setLength(discriminatorAnn.getInt("length"));

      if (! "".equals(discriminatorAnn.get("columnDefinition")))
	column.setSQLType(discriminatorAnn.getString("columnDefinition"));
    }
    else {
      column.setNotNull(true);
      column.setLength(10);
    }

    entityType.setDiscriminator(column);
  }

  /**
   * Introspects the fields.
   */
  private void introspectIdMethod(AmberManager manager,
				  EntityType entityType,
				  EntityType parentType,
				  JClass type)
    throws ConfigException, SQLException
  {
    ArrayList<IdField> keys = new ArrayList<IdField>();

    for (JMethod method : type.getMethods()) {
      String methodName = method.getName();
      JClass []paramTypes = method.getParameterTypes();

      if (! methodName.startsWith("get") || paramTypes.length != 0) {
	continue;
      }

      String fieldName = toFieldName(methodName.substring(3));

      if (parentType != null && parentType.getField(fieldName) != null)
	continue;

      JAnnotation id = method.getAnnotation(javax.ejb.Id.class);
      if (id == null)
	continue;

      IdField idField = introspectId(manager,
				     entityType,
				     method,
				     fieldName,
				     method.getReturnType());

      if (idField != null)
	keys.add(idField);
    }

    if (keys.size() == 1)
      entityType.setId(new Id(entityType, keys));
    else
      entityType.setId(new CompositeId(entityType, keys));
  }

  /**
   * Introspects the fields.
   */
  private void introspectIdField(AmberManager manager,
				 EntityType entityType,
				 EntityType parentType,
				 JClass type)
    throws ConfigException, SQLException
  {
    ArrayList<IdField> keys = new ArrayList<IdField>();

    for (JField field : type.getDeclaredFields()) {
      String fieldName = field.getName();

      if (parentType != null && parentType.getField(fieldName) != null)
	continue;

      JAnnotation id = field.getAnnotation(javax.ejb.Id.class);

      if (id == null)
	continue;

      IdField idField = introspectId(manager,
				     entityType,
				     field,
				     fieldName,
				     field.getType());

      if (idField != null)
	keys.add(idField);
    }

    if (keys.size() == 1)
      entityType.setId(new Id(entityType, keys));
    else
      entityType.setId(new CompositeId(entityType, keys));
  }

  private IdField introspectId(AmberManager manager,
			       EntityType entityType,
			       JAccessibleObject field,
			       String fieldName,
			       JClass fieldType)
    throws ConfigException, SQLException
  {
    JAnnotation id = field.getAnnotation(javax.ejb.Id.class);
    JAnnotation column = field.getAnnotation(javax.ejb.Column.class);

    Type amberType = manager.createType(fieldType);

    Column keyColumn = createColumn(entityType, "ID", column, amberType);

    KeyPropertyField idField;
    idField = new KeyPropertyField(entityType, fieldName, keyColumn);

    JdbcMetaData metaData = manager.getMetaData();

    if (GeneratorType.IDENTITY.equals(id.get("generate"))) {
      if (! metaData.supportsIdentity())
	throw new ConfigException(L.l("'{0}' does not support identity.",
				      metaData.getDatabaseName()));

      keyColumn.setGeneratorType("identity");
      idField.setGenerator("identity");
    }
    else if (GeneratorType.SEQUENCE.equals(id.get("generate"))) {
      if (! metaData.supportsSequences())
	throw new ConfigException(L.l("'{0}' does not support sequence.",
				      metaData.getDatabaseName()));

      addSequenceIdGenerator(manager, idField, id);
    }
    else if (GeneratorType.TABLE.equals(id.get("generate"))) {
      addTableIdGenerator(manager, idField, id);
    }
    else if (GeneratorType.AUTO.equals(id.get("generate"))) {
      if (metaData.supportsIdentity()) {
	keyColumn.setGeneratorType("identity");
	idField.setGenerator("identity");
      }
      else if (metaData.supportsSequences()) {
	addSequenceIdGenerator(manager, idField, id);
      }
      else
	addTableIdGenerator(manager, idField, id);
    }

    return idField;
  }

  private void addSequenceIdGenerator(AmberManager manager,
				      KeyPropertyField idField,
				      JAnnotation idAnn)
    throws ConfigException
  {
    idField.setGenerator("sequence");
    idField.getColumn().setGeneratorType("sequence");

    String name = idAnn.getString("generator");
    if (name == null || "".equals(name))
      name = idField.getSourceType().getTable().getName() + "_cseq";

    IdGenerator gen = manager.createSequenceGenerator(name, 1);

    idField.getSourceType().setGenerator(idField.getName(), gen);
  }

  private void addTableIdGenerator(AmberManager manager,
				   KeyPropertyField idField,
				   JAnnotation idAnn)
    throws ConfigException
  {
    idField.setGenerator("table");
    idField.getColumn().setGeneratorType("table");

    String name = idAnn.getString("generator");
    if (name == null || "".equals(name))
      name = "caucho";

    IdGenerator gen = manager.getTableGenerator(name);

    if (gen == null) {
      String genName = "GEN_TABLE";

      GeneratorTableType genTable = manager.createGeneratorTable(genName);

      gen = genTable.createGenerator(name);

      manager.putTableGenerator(name, gen);
    }

    idField.getSourceType().setGenerator(idField.getName(), gen);
  }

  /**
   * Links a secondary table.
   */
  private void linkSecondaryTable(Table primaryTable,
				  Table secondaryTable,
				  JAnnotation []joinColumnsAnn)
    throws ConfigException
  {
    ArrayList<ForeignColumn> linkColumns = new ArrayList<ForeignColumn>();
    for (Column column : primaryTable.getIdColumns()) {
      ForeignColumn linkColumn;

      JAnnotation joinAnn = getJoinColumn(joinColumnsAnn, column.getName());
      String name;

      if (joinAnn == null)
	name = column.getName();
      else
	name = joinAnn.getString("name");

      linkColumn = secondaryTable.createForeignColumn(name, column);
      linkColumn.setPrimaryKey(true);

      secondaryTable.addIdColumn(linkColumn);

      linkColumns.add(linkColumn);
    }

    LinkColumns link = new LinkColumns(secondaryTable,
				       primaryTable,
				       linkColumns);

    link.setSourceCascadeDelete(true);

    secondaryTable.setDependentIdLink(link);
  }

  /**
   * Links a secondary table.
   */
  private void linkInheritanceTable(Table primaryTable,
				    Table secondaryTable,
				    JAnnotation joinAnn)
    throws ConfigException
  {
    if (joinAnn != null)
      linkInheritanceTable(primaryTable, secondaryTable,
			   new JAnnotation[] { joinAnn });
    else
      linkInheritanceTable(primaryTable, secondaryTable,
			   (JAnnotation []) null);
  }

  /**
   * Links a secondary table.
   */
  private void linkInheritanceTable(Table primaryTable,
				    Table secondaryTable,
				    JAnnotation []joinColumnsAnn)
    throws ConfigException
  {
    ArrayList<ForeignColumn> linkColumns = new ArrayList<ForeignColumn>();
    for (Column column : primaryTable.getIdColumns()) {
      ForeignColumn linkColumn;

      JAnnotation join;
      join = getJoinColumn(joinColumnsAnn, column.getName());
      String name;

      if (join == null)
	name = column.getName();
      else
	name = join.getString("name");

      linkColumn = secondaryTable.createForeignColumn(name, column);
      linkColumn.setPrimaryKey(true);

      secondaryTable.addIdColumn(linkColumn);

      linkColumns.add(linkColumn);
    }

    LinkColumns link = new LinkColumns(secondaryTable,
				       primaryTable,
				       linkColumns);

    link.setSourceCascadeDelete(true);

    secondaryTable.setDependentIdLink(link);
  }

  /**
   * Introspects the methods.
   */
  private void introspectMethods(AmberManager manager,
				 EntityType entityType,
				 EntityType parentType,
				 JClass type)
    throws ConfigException
  {
    if (entityType.getId() == null)
      throw new IllegalStateException(L.l("{0} has no key", entityType));

    for (JMethod method : type.getMethods()) {
      String methodName = method.getName();
      JClass []paramTypes = method.getParameterTypes();

      introspectCallbacks(entityType, method);

      String propName;

      if (paramTypes.length != 0) {
	validateNonGetter(method);
	continue;
      }
      else if (methodName.startsWith("get")) {
	propName = methodName.substring(3);
      }
      else if (methodName.startsWith("is") &&
	       (method.getReturnType().getName().equals("boolean") ||
		method.getReturnType().getName().equals("java.lang.Boolean"))) {
	propName = methodName.substring(2);
      }
      else {
	validateNonGetter(method);
	continue;
      }

      if (type.getMethod("set" + propName,
			 new JClass[] { method.getReturnType() }) == null) {
	JAnnotation ann = isAnnotatedMethod(method);

	/*
	if (ann != null) {
	  throw new ConfigException(L.l("'{0}' is not a valid annotation for {1}.  Only public persistent property getters with matching setters may have property annotations.",
					ann.getType(), method.getFullName()));
	}

	continue;
	    */
      }

      if (method.isStatic() || ! method.isPublic()) {
	validateNonGetter(method);
	continue;
      }

      String fieldName = toFieldName(propName);

      if (parentType != null && parentType.getField(fieldName) != null)
	continue;

      JClass fieldType = method.getReturnType();

      introspectField(manager, entityType, method, fieldName, fieldType);
    }
  }

  /**
   * Introspects the fields.
   */
  private void introspectFields(AmberManager manager,
				EntityType entityType,
				EntityType parentType,
				JClass type)
    throws ConfigException
  {
    if (entityType.getId() == null)
      throw new IllegalStateException(L.l("{0} has no key", entityType));

    for (JField field : type.getDeclaredFields()) {
      String fieldName = field.getName();

      if (parentType != null && parentType.getField(fieldName) != null)
	continue;

      if (field.isStatic() || field.isTransient())
	continue;

      JClass fieldType = field.getType();

      introspectField(manager, entityType, field, fieldName, fieldType);
    }
  }

  private void introspectField(AmberManager manager,
			       EntityType sourceType,
			       JAccessibleObject field,
			       String fieldName,
			       JClass fieldType)
    throws ConfigException
  {
    if (field.isAnnotationPresent(javax.ejb.Id.class)) {
    }
    else if (field.isAnnotationPresent(javax.ejb.Basic.class)) {
      addBasic(sourceType, field, fieldName, fieldType);
    }
    else if (field.isAnnotationPresent(javax.ejb.ManyToOne.class)) {
      _linkCompletions.add(new ManyToOneCompletion(sourceType,
						   field,
						   fieldName,
						   fieldType));
    }
    else if (field.isAnnotationPresent(javax.ejb.OneToMany.class)) {
      _depCompletions.add(new OneToManyCompletion(sourceType,
						  field,
						  fieldName,
						  fieldType));
    }
    else if (field.isAnnotationPresent(javax.ejb.OneToOne.class)) {
      _depCompletions.add(new OneToOneCompletion(sourceType,
						  field,
						  fieldName,
						  fieldType));
    }
    else if (field.isAnnotationPresent(javax.ejb.ManyToMany.class)) {
      _depCompletions.add(new ManyToManyCompletion(sourceType,
						   field,
						   fieldName,
						   fieldType));
    }
    else if (field.isAnnotationPresent(javax.ejb.Transient.class)) {
    }
    else {
      addBasic(sourceType, field, fieldName, fieldType);
    }
  }

  private void addBasic(EntityType sourceType,
			JAccessibleObject field,
			String fieldName,
			JClass fieldType)
    throws ConfigException
  {
    AmberManager manager = sourceType.getAmberManager();

    JAnnotation basicAnn = field.getAnnotation(javax.ejb.Basic.class);
    JAnnotation columnAnn = field.getAnnotation(javax.ejb.Column.class);

    Type amberType = manager.createType(fieldType);

    Column fieldColumn = createColumn(sourceType, fieldName,
				      columnAnn, amberType);

    PropertyField property = new PropertyField(sourceType, fieldName);
    property.setColumn(fieldColumn);

    if (basicAnn != null)
      property.setLazy(basicAnn.get("fetch") == FetchType.LAZY);

    /*
      field.setInsertable(insertable);
      field.setUpdateable(updateable);
    */

    sourceType.addField(property);
  }

  private Column createColumn(EntityType entityType, String name,
			      JAnnotation columnAnn, Type amberType)
    throws ConfigException
  {
    if (columnAnn == null) {
    }
    else if (! columnAnn.get("name").equals(""))
      name = (String) columnAnn.get("name");

    Column column;

    if (columnAnn != null && ! columnAnn.get("secondaryTable").equals("")) {
      String tableName = columnAnn.getString("secondaryTable");
      Table table;

      table = entityType.getSecondaryTable(tableName);

      if (table == null)
	throw new ConfigException(L.l("'{0}' is an unknown secondary table.",
				      tableName));

      column = table.createColumn(name, amberType);
    }
    else
      column = entityType.getTable().createColumn(name, amberType);

    if (columnAnn != null) {
      // primaryKey = column.primaryKey();
      column.setUnique(columnAnn.getBoolean("unique"));
      column.setNotNull(! columnAnn.getBoolean("nullable"));
      //insertable = column.insertable();
      //updateable = column.updatable();
      if (! "".equals(columnAnn.getString("columnDefinition")))
	column.setSQLType(columnAnn.getString("columnDefinition"));
      column.setLength(columnAnn.getInt("length"));
    }

    return column;
  }

  private void addManyToOne(EntityType sourceType,
			    JAccessibleObject field,
			    String fieldName,
			    JClass fieldType)
    throws ConfigException
  {
    AmberManager manager = sourceType.getAmberManager();

    JAnnotation manyToOneAnn = field.getAnnotation(ManyToOne.class);

    JAnnotation joinColumns = field.getAnnotation(JoinColumns.class);
    JAnnotation []joinColumnsAnn = null;

    if (joinColumns != null)
      joinColumnsAnn = (JAnnotation []) joinColumns.get("value");
    JAnnotation joinColumnAnn = field.getAnnotation(JoinColumn.class);

    String targetName = manyToOneAnn.getString("targetEntity");

    if (targetName == null || targetName.equals(""))
      targetName = fieldType.getName();

    EntityManyToOneField manyToOneField;
    manyToOneField = new EntityManyToOneField(sourceType, fieldName);

    EntityType targetType = manager.createEntity(targetName, fieldType);

    manyToOneField.setType(targetType);

    manyToOneField.setLazy(manyToOneAnn.get("fetch") == FetchType.LAZY);

    sourceType.addField(manyToOneField);

    Table sourceTable = sourceType.getTable();

    ArrayList<ForeignColumn> foreignColumns = new ArrayList<ForeignColumn>();
    for (Column keyColumn : targetType.getId().getColumns()) {
      JAnnotation joinAnn = getJoinColumn(joinColumnsAnn, keyColumn.getName());
      if (joinAnn == null)
	joinAnn = joinColumnAnn;

      String columnName = keyColumn.getName();
      if (joinAnn != null)
	columnName = joinAnn.getString("name");

      ForeignColumn foreignColumn;

      foreignColumn = sourceTable.createForeignColumn(columnName, keyColumn);

      if (joinAnn != null) {
	foreignColumn.setNotNull(! joinAnn.getBoolean("nullable"));
	foreignColumn.setUnique(joinAnn.getBoolean("unique"));
      }

      foreignColumns.add(foreignColumn);
    }

    LinkColumns linkColumns = new LinkColumns(sourceType.getTable(),
					      targetType.getTable(),
					      foreignColumns);

    manyToOneField.setLinkColumns(linkColumns);

    manyToOneField.init();
  }

  private JAnnotation getJoinColumn(JAnnotation joinColumns, String keyName)
  {
    if (joinColumns == null)
      return null;

    return getJoinColumn((JAnnotation []) joinColumns.get("value"), keyName);
  }

  private JAnnotation getJoinColumn(JAnnotation []columnsAnn, String keyName)
  {
    if (columnsAnn == null || columnsAnn.length == 0)
      return null;

    for (int i = 0; i < columnsAnn.length; i++) {
      String ref = columnsAnn[i].getString("referencedColumnName");

      if (ref.equals("") || ref.equals(keyName))
	return columnsAnn[i];
    }

    return null;
  }

  /* XXX: inheritance
  private JAnnotation getJoinColumn(JAnnotation []columnsAnn, String keyName)
  {
    if (columns == null || columns.length == 0)
      return null;

    for (int i = 0; i < columns.length; i++) {
      String ref = columns[i].getString("referencedColumnName");

      if (ref.equals("") || ref.equals(keyName))
	return columns[i];
    }

    return null;
  }
  */

  private void addManyToMany(EntityType sourceType,
			     JAccessibleObject field,
			     String fieldName,
			     JClass fieldType)
    throws ConfigException
  {
    JAnnotation manyToManyAnn = field.getAnnotation(ManyToMany.class);

    AmberManager manager = sourceType.getAmberManager();

      JType retType;

      if (field instanceof JField)
	retType = ((JField) field).getGenericType();
      else
	retType = ((JMethod) field).getGenericReturnType();

      JType []typeArgs = retType.getActualTypeArguments();

      String targetName;

      targetName = manyToManyAnn.getString("targetEntity");

      if (targetName != null && ! targetName.equals("")) {
      }
      else if (typeArgs.length > 0)
	targetName = typeArgs[0].getName();
      else
	throw new ConfigException(L.l("can't determine target name for {0}",
				      fieldName));

    EntityType targetType = manager.getEntity(targetName);

    EntityManyToManyField manyToManyField;
    manyToManyField = new EntityManyToManyField(sourceType, fieldName);
    manyToManyField.setType(targetType);

    JAnnotation associationTableAnn =
      field.getAnnotation(AssociationTable.class);

    String sqlTable = sourceType.getTable().getName() + "_" + targetType.getTable().getName();

    Table mapTable = null;

    ArrayList<ForeignColumn> sourceColumns = null;
    ArrayList<ForeignColumn> targetColumns = null;

    if (associationTableAnn != null) {
      JAnnotation table = associationTableAnn.getAnnotation("table");

      if (! table.getString("name").equals(""))
	sqlTable = table.getString("name");

      mapTable = manager.createTable(sqlTable);

      sourceColumns = calculateColumns(mapTable,
				       sourceType,
				       (Object []) associationTableAnn.get("joinColumns"));

      targetColumns = calculateColumns(mapTable,
				       targetType,
				       (Object []) associationTableAnn.get("inverseJoinColumns"));
    }
    else {
      mapTable = manager.createTable(sqlTable);

      sourceColumns = calculateColumns(mapTable, sourceType);

      targetColumns = calculateColumns(mapTable, targetType);
    }

    manyToManyField.setAssociationTable(mapTable);
    manyToManyField.setTable(sqlTable);

    manyToManyField.setSourceLink(new LinkColumns(mapTable,
						  sourceType.getTable(),
						  sourceColumns));

    manyToManyField.setTargetLink(new LinkColumns(mapTable,
						  targetType.getTable(),
						  targetColumns));

    sourceType.addField(manyToManyField);
  }

  static String toFieldName(String name)
  {
    if (Character.isLowerCase(name.charAt(0)))
      return name;
    else if (name.length() == 1 ||
	     Character.isLowerCase(name.charAt(1)))
      return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    else
      return name;
  }

  static ArrayList<ForeignColumn>
    calculateColumns(Table mapTable,
		     EntityType type,
		     Object []joinColumnsAnn)
  {
    if (joinColumnsAnn == null || joinColumnsAnn.length == 0)
      return calculateColumns(mapTable, type);

    ArrayList<ForeignColumn> columns = new ArrayList<ForeignColumn>();

    for (int i = 0; i < joinColumnsAnn.length; i++) {
      ForeignColumn foreignColumn;
      JAnnotation joinColumnAnn = (JAnnotation) joinColumnsAnn[i];

      foreignColumn =
	mapTable.createForeignColumn(joinColumnAnn.getString("name"),
				     type.getId().getKey().getColumns().get(0));

      columns.add(foreignColumn);
    }

    return columns;
  }

  static ArrayList<ForeignColumn> calculateColumns(Table mapTable,
						   EntityType type)
  {
    ArrayList<ForeignColumn> columns = new ArrayList<ForeignColumn>();

    for (Column key : type.getId().getColumns()) {
      columns.add(mapTable.createForeignColumn(key.getName(), key));
    }

    return columns;
  }

  static String toSqlName(String name)
  {
    CharBuffer cb = new CharBuffer();

    for (int i = 0; i < name.length(); i++) {
      char ch = name.charAt(i);

      if (! Character.isUpperCase(ch))
	cb.append(ch);
      else if (i > 0 && ! Character.isUpperCase(name.charAt(i - 1))) {
	cb.append("_");
	cb.append(Character.toLowerCase(ch));
      }
      else if (i > 0 &&
	       i + 1 < name.length() &&
	       ! Character.isUpperCase(name.charAt(i + 1))) {
	cb.append("_");
	cb.append(Character.toLowerCase(ch));
      }
      else
	cb.append(Character.toLowerCase(ch));
    }

    return cb.toString();
  }

  /**
   * completes for dependent
   */
  class Completion {
    void complete()
      throws ConfigException
    {
    }
  }

  /**
   * completes for dependent
   */
  class OneToManyCompletion extends Completion {
    private EntityType _entityType;
    private JAccessibleObject _field;
    private String _fieldName;
    private JClass _fieldType;

    OneToManyCompletion(EntityType type,
			JAccessibleObject field,
			String fieldName,
			JClass fieldType)
    {
      _entityType = type;
      _field = field;
      _fieldName = fieldName;
      _fieldType = fieldType;
    }

    void complete()
      throws ConfigException
    {
      JAnnotation oneToManyAnn = _field.getAnnotation(OneToMany.class);

      AmberManager manager = _entityType.getAmberManager();

      JType retType;

      if (_field instanceof JField)
	retType = ((JField) _field).getGenericType();
      else
	retType = ((JMethod) _field).getGenericReturnType();

      JType []typeArgs = retType.getActualTypeArguments();

      String targetName;

      targetName = oneToManyAnn.getString("targetEntity");

      if (targetName != null && ! targetName.equals("")) {
      }
      else if (typeArgs.length > 0)
	targetName = typeArgs[0].getName();
      else
	throw new ConfigException(L.l("can't determine target name for {0}",
				      _fieldName));

      EntityType targetType = manager.getEntity(targetName);
      if (targetType == null)
	throw new ConfigException(L.l("'{0}' is an unknown entity.",
				      targetName));

      EntityManyToOneField sourceField = getSourceField(targetType,
							_entityType);

      if (sourceField == null)
	throw new ConfigException(L.l("'{0}' does not have a matching ManyToOne relation.",
				      targetType.getName()));

      EntityOneToManyField oneToMany;

      oneToMany = new EntityOneToManyField(_entityType, _fieldName);
      oneToMany.setSourceField(sourceField);

      _entityType.addField(oneToMany);
    }

    private EntityManyToOneField getSourceField(EntityType targetType,
						EntityType entity)
    {
      for (AmberField field : targetType.getFields()) {
	if (field instanceof EntityManyToOneField) {
	  EntityManyToOneField manyToOne = (EntityManyToOneField) field;

	  if (manyToOne.getEntityType().equals(entity))
	    return manyToOne;
	}
      }

      return null;
    }
  }

  /**
   * completes for dependent
   */
  class OneToOneCompletion extends Completion {
    private EntityType _entityType;
    private JAccessibleObject _field;
    private String _fieldName;
    private JClass _fieldType;

    OneToOneCompletion(EntityType type,
		       JAccessibleObject field,
		       String fieldName,
		       JClass fieldType)
    {
      _entityType = type;
      _field = field;
      _fieldName = fieldName;
      _fieldType = fieldType;
    }

    void complete()
      throws ConfigException
    {
      JAnnotation oneToOneAnn = _field.getAnnotation(OneToOne.class);

      AmberManager manager = _entityType.getAmberManager();

      String targetName = oneToOneAnn.getString("targetEntity");

      if (targetName == null || targetName.equals(""))
	throw new ConfigException(L.l("can't determine target name"));

      EntityType targetType = manager.getEntity(targetName);
      if (targetType == null)
	throw new ConfigException(L.l("'{0}' is an unknown entity.",
				      targetName));

      EntityManyToOneField sourceField = getSourceField(targetType,
							_entityType);

      if (sourceField == null)
	throw new ConfigException(L.l("'{0}' does not have a matching ManyToOne relation.",
				      targetType.getName()));

      DependentEntityOneToOneField oneToOne;

      oneToOne = new DependentEntityOneToOneField(_entityType, _fieldName);
      oneToOne.setTargetField(sourceField);

      _entityType.addField(oneToOne);
    }

    private EntityManyToOneField getSourceField(EntityType targetType,
						EntityType entity)
    {
      for (AmberField field : targetType.getFields()) {
	if (field instanceof EntityManyToOneField) {
	  EntityManyToOneField manyToOne = (EntityManyToOneField) field;

	  if (manyToOne.getEntityType().equals(entity))
	    return manyToOne;
	}
      }

      return null;
    }
  }

  /**
   * completes for dependent
   */
  class ManyToManyCompletion extends Completion {
    private EntityType _entityType;
    private JAccessibleObject _field;
    private String _fieldName;
    private JClass _fieldType;

    ManyToManyCompletion(EntityType type,
			 JAccessibleObject field,
			 String fieldName,
			 JClass fieldType)
    {
      _entityType = type;
      _field = field;
      _fieldName = fieldName;
      _fieldType = fieldType;
    }

    void complete()
      throws ConfigException
    {
      addManyToMany(_entityType, _field, _fieldName, _fieldType);
    }
  }

  /**
   * completes for link
   */
  class ManyToOneCompletion extends Completion {
    private EntityType _entityType;
    private JAccessibleObject _field;
    private String _fieldName;
    private JClass _fieldType;

    ManyToOneCompletion(EntityType type,
			JAccessibleObject field,
			String fieldName,
			JClass fieldType)
    {
      _entityType = type;
      _field = field;
      _fieldName = fieldName;
      _fieldType = fieldType;
    }

    void complete()
      throws ConfigException
    {
      addManyToOne(_entityType, _field, _fieldName, _fieldType);
    }
  }

  static {
    _propertyAnnotations.add("javax.ejb.Basic");
    _propertyAnnotations.add("javax.ejb.Column");
    _propertyAnnotations.add("javax.ejb.Id");
    _propertyAnnotations.add("javax.ejb.Transient");
    _propertyAnnotations.add("javax.ejb.OneToOne");
    _propertyAnnotations.add("javax.ejb.ManyToOne");
    _propertyAnnotations.add("javax.ejb.OneToMany");
    _propertyAnnotations.add("javax.ejb.ManyToMany");
    _propertyAnnotations.add("javax.ejb.JoinColumn");
  }
}

