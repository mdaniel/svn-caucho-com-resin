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

package com.caucho.amber.cfg;

import java.sql.SQLException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.persistence.*;

import com.caucho.amber.AmberTableCache;

import com.caucho.amber.field.*;

import com.caucho.amber.idgen.IdGenerator;

import com.caucho.amber.manager.AmberContainer;
import com.caucho.amber.manager.AmberPersistenceUnit;

import com.caucho.amber.table.Column;
import com.caucho.amber.table.ForeignColumn;
import com.caucho.amber.table.LinkColumns;
import com.caucho.amber.table.Table;

import com.caucho.amber.type.*;

import com.caucho.bytecode.*;
import com.caucho.config.ConfigException;
import com.caucho.config.types.Period;
import com.caucho.ejb.EjbServerManager;
import com.caucho.jdbc.JdbcMetaData;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

/**
 * Configuration for an entity bean
 */
public class EntityIntrospector {
  private static final L10N L = new L10N(EntityIntrospector.class);
  private static final Logger log
    = Logger.getLogger(EntityIntrospector.class.getName());

  private static HashSet<String> _propertyAnnotations
    = new HashSet<String>();

  // types allowed with a @Basic annotation
  private static HashSet<String> _basicTypes = new HashSet<String>();

  // annotations allowed with a @Basic annotation
  private static HashSet<String> _basicAnnotations = new HashSet<String>();

  // types allowed with an @Id annotation
  private static HashSet<String> _idTypes = new HashSet<String>();

  // annotations allowed with an @Id annotation
  private static HashSet<String> _idAnnotations = new HashSet<String>();

  // annotations allowed with a @ManyToOne annotation
  private static HashSet<String> _manyToOneAnnotations = new HashSet<String>();

  private AmberPersistenceUnit _persistenceUnit;

  private HashMap<String,EntityType> _entityMap
    = new HashMap<String,EntityType>();

  private ArrayList<Completion> _linkCompletions = new ArrayList<Completion>();
  private ArrayList<Completion> _depCompletions = new ArrayList<Completion>();

  /**
   * Creates the introspector.
   */
  public EntityIntrospector(AmberPersistenceUnit persistenceUnit)
  {
    _persistenceUnit = persistenceUnit;
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

    try {
      ///entityType = _amberContainer.createEntity(entityName, type);
      entityType = _persistenceUnit.createEntity(entityName, type);
      _entityMap.put(entityName, entityType);

      boolean isField = isField(type);

      if (isField)
	entityType.setFieldAccess(true);

      entityType.setInstanceClassName(type.getName() + "__ResinExt");
      entityType.setEnhanced(true);

      Table table = null;
      JAnnotation tableAnn = type.getAnnotation(javax.persistence.Table.class);

      String tableName = null;
      if (tableAnn != null)
	tableName = (String) tableAnn.get("name");

      if (tableName == null || tableName.equals(""))
	tableName = entityName.toUpperCase();

      if (parentType == null)
	entityType.setTable(_persistenceUnit.createTable(tableName));
      else if (parentType.isJoinedSubClass())
	entityType.setTable(_persistenceUnit.createTable(tableName));
      else
	entityType.setTable(parentType.getTable());

      JAnnotation tableCache = type.getAnnotation(AmberTableCache.class);
      if (tableCache != null) {
	entityType.getTable().setReadOnly(tableCache.getBoolean("readOnly"));

	long cacheTimeout = Period.toPeriod(tableCache.getString("timeout"));
	entityType.getTable().setCacheTimeout(cacheTimeout);
      }
      
      JAnnotation secondaryTableAnn = type.getAnnotation(SecondaryTable.class);

      Table secondaryTable = null;

      if (inheritanceAnn != null)
	introspectInheritance(_persistenceUnit, entityType, type);

      if (secondaryTableAnn != null) {
	String secondaryName = (String) secondaryTableAnn.get("name");

	secondaryTable = _persistenceUnit.createTable(secondaryName);

	entityType.addSecondaryTable(secondaryTable);
      }

      JAnnotation idClassAnn = type.getAnnotation(IdClass.class);

      JClass idClass = null;
      if (idClassAnn != null)
	idClass = idClassAnn.getClass("value");


      if (entityType.getId() != null) {
      }
      else if (isField)
	introspectIdField(_persistenceUnit, entityType, parentType,
			  type, idClass);
      else
	introspectIdMethod(_persistenceUnit, entityType, parentType,
			   type, idClass);

      if (entityType.getId() == null)
	throw new ConfigException(L.l("{0} does not have any primary keys.  Entities must have at least one @Id field.",
				      entityType.getName()));

      if (isField)
	introspectFields(_persistenceUnit, entityType, parentType, type);
      else
	introspectMethods(_persistenceUnit, entityType, parentType, type);

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
    } catch (ConfigException e) {
      entityType.setConfigException(e);

      throw e;
    } catch (SQLException e) {
      entityType.setConfigException(e);

      throw e;
    } catch (RuntimeException e) {
      entityType.setConfigException(e);

      throw e;
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
  public void configure()
    throws ConfigException
  {
    ConfigException exn = null;
    
    while (_depCompletions.size() > 0 || _linkCompletions.size() > 0) {
      while (_linkCompletions.size() > 0) {
	Completion completion = _linkCompletions.remove(0);

	try {
	  completion.complete();
	} catch (Exception e) {
	  completion.getEntityType().setConfigException(e);

	  if (exn == null)
	    exn = new ConfigException(e);
	  else
	    log.log(Level.WARNING, e.toString(), e);
	}
      }

      if (_depCompletions.size() > 0) {
	Completion completion = _depCompletions.remove(0);


	try {
	  completion.complete();
	} catch (Exception e) {
	  completion.getEntityType().setConfigException(e);

	  log.log(Level.WARNING, e.toString(), e);

	  if (exn == null)
	    exn = new ConfigException(e);
	  else
	    log.log(Level.WARNING, e.toString(), e);
	}
      }
    }

    if (exn != null)
      throw exn;
  }

  /**
   * Introspects the Inheritance
   */
  private void introspectInheritance(AmberPersistenceUnit persistenceUnit,
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

      JAnnotation joinAnn = type.getAnnotation(JoinColumn.class);

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
    DiscriminatorType discType = DiscriminatorType.STRING;

    if (discriminatorAnn != null)
      discType = (DiscriminatorType) discriminatorAnn.get("discriminatorType");

    switch (discType) {
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
  private void introspectIdMethod(AmberPersistenceUnit persistenceUnit,
				  EntityType entityType,
				  EntityType parentType,
				  JClass type,
				  JClass idClass)
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

      JAnnotation id = method.getAnnotation(javax.persistence.Id.class);
      if (id == null)
	continue;

      IdField idField = introspectId(persistenceUnit,
				     entityType,
				     method,
				     fieldName,
				     method.getReturnType());

      if (idField != null)
	keys.add(idField);
    }

    if (keys.size() == 0) {
    }
    else if (keys.size() == 1)
      entityType.setId(new com.caucho.amber.field.Id(entityType, keys));
    else if (idClass == null) {
      throw new ConfigException(L.l("{0} has multiple @Id methods, but no @IdClass.  Compound primary keys require an @IdClass.",
				    entityType.getName()));
    }
    else {
      CompositeId id = new CompositeId(entityType, keys);
      id.setKeyClass(idClass);
      
      entityType.setId(id);
    }
  }

  /**
   * Introspects the fields.
   */
  private void introspectIdField(AmberPersistenceUnit persistenceUnit,
				 EntityType entityType,
				 EntityType parentType,
				 JClass type,
				 JClass idClass)
    throws ConfigException, SQLException
  {
    ArrayList<IdField> keys = new ArrayList<IdField>();

    for (JField field : type.getDeclaredFields()) {
      String fieldName = field.getName();

      if (parentType != null && parentType.getField(fieldName) != null)
	continue;

      JAnnotation id = field.getAnnotation(javax.persistence.Id.class);

      if (id == null)
	continue;

      IdField idField = introspectId(persistenceUnit,
				     entityType,
				     field,
				     fieldName,
				     field.getType());

      if (idField != null)
	keys.add(idField);
    }

    if (keys.size() == 0) {
    }
    else if (keys.size() == 1)
      entityType.setId(new com.caucho.amber.field.Id(entityType, keys));
    else if (idClass == null) {
      throw new ConfigException(L.l("{0} has multiple @Id fields, but no @IdClass.  Compound primary keys require an @IdClass.",
				    entityType.getName()));
    }
    else {
      CompositeId id = new CompositeId(entityType, keys);
      id.setKeyClass(idClass);
      
      entityType.setId(id);
    }
  }

  /**
   * Check if it's field
   */
  private boolean isField(JClass type)
    throws ConfigException
  {
    for (JField field : type.getDeclaredFields()) {
      JAnnotation id = field.getAnnotation(javax.persistence.Id.class);

      if (id != null)
	return true;
    }

    return false;
  }

  private IdField introspectId(AmberPersistenceUnit persistenceUnit,
			       EntityType entityType,
			       JAccessibleObject field,
			       String fieldName,
			       JClass fieldType)
    throws ConfigException, SQLException
  {
    JAnnotation id = field.getAnnotation(javax.persistence.Id.class);
    JAnnotation column = field.getAnnotation(javax.persistence.Column.class);
    JAnnotation gen = field.getAnnotation(javax.persistence.GeneratedValue.class);

    Type amberType = persistenceUnit.createType(fieldType);

    Column keyColumn = createColumn(entityType,
				    fieldName,
				    column,
				    amberType);

    KeyPropertyField idField;
    idField = new KeyPropertyField(entityType, fieldName, keyColumn);

    JdbcMetaData metaData = persistenceUnit.getMetaData();

    if (gen == null) {
    }
    else if (GenerationType.IDENTITY.equals(gen.get("strategy"))) {
      if (! metaData.supportsIdentity())
	throw new ConfigException(L.l("'{0}' does not support identity.",
				      metaData.getDatabaseName()));

      keyColumn.setGeneratorType("identity");
      idField.setGenerator("identity");
    }
    else if (GenerationType.SEQUENCE.equals(gen.get("strategy"))) {
      if (! metaData.supportsSequences())
	throw new ConfigException(L.l("'{0}' does not support sequence.",
				      metaData.getDatabaseName()));

      addSequenceIdGenerator(persistenceUnit, idField, id);
    }
    else if (GenerationType.TABLE.equals(gen.get("strategy"))) {
      addTableIdGenerator(persistenceUnit, idField, id);
    }
    else if (GenerationType.AUTO.equals(gen.get("strategy"))) {
      if (metaData.supportsIdentity()) {
	keyColumn.setGeneratorType("identity");
	idField.setGenerator("identity");
      }
      else if (metaData.supportsSequences()) {
	addSequenceIdGenerator(persistenceUnit, idField, id);
      }
      else
	addTableIdGenerator(persistenceUnit, idField, id);
    }

    return idField;
  }

  private void addSequenceIdGenerator(AmberPersistenceUnit persistenceUnit,
				      KeyPropertyField idField,
				      JAnnotation idAnn)
    throws ConfigException
  {
    idField.setGenerator("sequence");
    idField.getColumn().setGeneratorType("sequence");

    String name = idAnn.getString("generator");
    if (name == null || "".equals(name))
      name = idField.getSourceType().getTable().getName() + "_cseq";

    IdGenerator gen = persistenceUnit.createSequenceGenerator(name, 1);

    idField.getSourceType().setGenerator(idField.getName(), gen);
  }

  private void addTableIdGenerator(AmberPersistenceUnit persistenceUnit,
				   KeyPropertyField idField,
				   JAnnotation idAnn)
    throws ConfigException
  {
    idField.setGenerator("table");
    idField.getColumn().setGeneratorType("table");

    String name = idAnn.getString("generator");
    if (name == null || "".equals(name))
      name = "caucho";

    IdGenerator gen = persistenceUnit.getTableGenerator(name);

    if (gen == null) {
      String genName = "GEN_TABLE";

      GeneratorTableType genTable;
      genTable = persistenceUnit.createGeneratorTable(genName);

      gen = genTable.createGenerator(name);

      persistenceUnit.putTableGenerator(name, gen);
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
  private void introspectMethods(AmberPersistenceUnit persistenceUnit,
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

      introspectField(persistenceUnit, entityType, method, fieldName, fieldType);
    }
  }

  /**
   * Introspects the fields.
   */
  private void introspectFields(AmberPersistenceUnit persistenceUnit,
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

      introspectField(persistenceUnit, entityType, field, fieldName, fieldType);
    }
  }

  private void introspectField(AmberPersistenceUnit persistenceUnit,
			       EntityType sourceType,
			       JAccessibleObject field,
			       String fieldName,
			       JClass fieldType)
    throws ConfigException
  {
    if (field.isAnnotationPresent(javax.persistence.Id.class)) {
      validateAnnotations(field, _idAnnotations);

      if (! _idTypes.contains(fieldType.getName())) {
	throw error(field, L.l("{0} is an invalid @Id type for {1}.",
			       fieldType.getName(), field.getName()));
      }
    }
    else if (field.isAnnotationPresent(javax.persistence.Basic.class)) {
      validateAnnotations(field, _basicAnnotations);
      
      addBasic(sourceType, field, fieldName, fieldType);
    }
    else if (field.isAnnotationPresent(javax.persistence.ManyToOne.class)) {
      JAnnotation ann = field.getAnnotation(javax.persistence.ManyToOne.class);
      
      validateAnnotations(field, _manyToOneAnnotations);

      JClass targetEntity = ann.getClass("targetEntity");

      if (targetEntity == null ||
	  targetEntity.getName().equals("void")) {
	targetEntity = fieldType;
      }
      
      if (! targetEntity.isAnnotationPresent(javax.persistence.Entity.class)) {
	throw error(field, L.l("'{0}' is an illegal targetEntity for {1}.  @ManyToOne relations must target a valid @Entity.",
			       targetEntity.getName(), field.getName()));
      }

      if (! fieldType.isAssignableFrom(targetEntity)) {
	throw error(field, L.l("'{0}' is an illegal targetEntity for {1}.  @ManyToOne targetEntity must be assignable to the field type '{2}'.",
			       targetEntity.getName(),
			       field.getName(),
			       fieldType.getName()));
      }
      
      _linkCompletions.add(new ManyToOneCompletion(sourceType,
						   field,
						   fieldName,
						   fieldType));
    }
    else if (field.isAnnotationPresent(javax.persistence.OneToMany.class)) {
      _depCompletions.add(new OneToManyCompletion(sourceType,
						  field,
						  fieldName,
						  fieldType));
    }
    else if (field.isAnnotationPresent(javax.persistence.OneToOne.class)) {
      _depCompletions.add(new OneToOneCompletion(sourceType,
						  field,
						  fieldName,
						  fieldType));
    }
    else if (field.isAnnotationPresent(javax.persistence.ManyToMany.class)) {
      _depCompletions.add(new ManyToManyCompletion(sourceType,
						   field,
						   fieldName,
						   fieldType));
    }
    else if (field.isAnnotationPresent(javax.persistence.Transient.class)) {
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
    AmberPersistenceUnit persistenceUnit = sourceType.getPersistenceUnit();

    JAnnotation basicAnn = field.getAnnotation(javax.persistence.Basic.class);
    JAnnotation columnAnn = field.getAnnotation(javax.persistence.Column.class);

    if (_basicTypes.contains(fieldType.getName())) {
    }
    else if (fieldType.isAssignableTo(java.io.Serializable.class)) {
    }
    else
      throw error(field, L.l("{0} is an invalid @Basic type for {1}.",
			     fieldType.getName(), field.getName()));

    Type amberType = persistenceUnit.createType(fieldType);

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

  private Column createColumn(EntityType entityType, String fieldName,
			      JAnnotation columnAnn, Type amberType)
    throws ConfigException
  {
    String name;
    
    if (columnAnn != null && ! columnAnn.get("name").equals(""))
      name = (String) columnAnn.get("name");
    else
      name = toSqlName(fieldName);

    Column column;

    /*
    if (columnAnn != null && ! columnAnn.get("secondaryTable").equals("")) {
      String tableName = columnAnn.getString("secondaryTable");
      Table table;

      table = entityType.getSecondaryTable(tableName);

      if (table == null)
	throw new ConfigException(L.l("'{0}' is an unknown secondary table.",
				      tableName));

      column = table.createColumn(name, amberType);
    }
    */

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
      int precision = columnAnn.getInt("precision");
      if (precision < 0) {
        throw new ConfigException(L.l("precision cannot be less than 0."));
      }
      int scale = columnAnn.getInt("scale");
      if (scale < 0) {
        throw new ConfigException(L.l("scale cannot be less than 0."));
      }
      // this test implicitly works for case where
      // precision is not set explicitly (ie: set to 0 by default)
      // and scale is set
      if (scale > precision) {
        throw new ConfigException(L.l("Scale cannot be greater than precision. Must set precision to a non-zero value before setting scale."));
      }
      if (precision > 0){
        column.setPrecision(precision);
        column.setScale(scale);
      }
    }

    return column;
  }

  private void addManyToOne(EntityType sourceType,
			    JAccessibleObject field,
			    String fieldName,
			    JClass fieldType)
    throws ConfigException
  {
    AmberPersistenceUnit persistenceUnit = sourceType.getPersistenceUnit();

    JAnnotation manyToOneAnn = field.getAnnotation(ManyToOne.class);

    if (manyToOneAnn == null) {
      // XXX: ejb/0o03
      manyToOneAnn = field.getAnnotation(OneToOne.class);
    }

    JAnnotation joinColumns = field.getAnnotation(JoinColumns.class);
    Object []joinColumnsAnn = null;

    if (joinColumns != null)
      joinColumnsAnn = (Object []) joinColumns.get("value");
    JAnnotation joinColumnAnn = field.getAnnotation(JoinColumn.class);

    if (joinColumnsAnn != null && joinColumnAnn != null) {
      throw error(field, L.l("{0} may not have both @JoinColumn and @JoinColumns",
			     field.getName()));
    }

    if (joinColumnAnn != null)
      joinColumnsAnn = new Object[] { joinColumnAnn };

    JClass targetClass = manyToOneAnn.getClass("targetEntity");
    String targetName = "";
    if (targetClass != null)
      targetName = targetClass.getName();

    if (targetName.equals("") || targetName.equals("void"))
      targetName = fieldType.getName();

    EntityManyToOneField manyToOneField;
    manyToOneField = new EntityManyToOneField(sourceType, fieldName);

    EntityType targetType = persistenceUnit.createEntity(targetName, fieldType);

    manyToOneField.setType(targetType);

    manyToOneField.setLazy(manyToOneAnn.get("fetch") == FetchType.LAZY);

    sourceType.addField(manyToOneField);

    Table sourceTable = sourceType.getTable();

    validateJoinColumns(field, joinColumnsAnn, targetType);

    ArrayList<ForeignColumn> foreignColumns = new ArrayList<ForeignColumn>();
    for (Column keyColumn : targetType.getId().getColumns()) {
      JAnnotation joinAnn = getJoinColumn(joinColumnsAnn, keyColumn.getName());

      String columnName = fieldName.toUpperCase() + '_' + keyColumn.getName();
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

    return getJoinColumn((Object []) joinColumns.get("value"), keyName);
  }

  private void validateJoinColumns(JAccessibleObject field,
				   Object []columnsAnn,
				   EntityType targetType)
    throws ConfigException
  {
    if (columnsAnn == null || columnsAnn.length == 0)
      return;

    com.caucho.amber.field.Id id = targetType.getId();

    if (id.getColumns().size() != columnsAnn.length) {
      throw error(field, L.l("Number of @JoinColumns for '{1}' ({0}) does not match the number of primary key columns for '{3}' ({2}).",
			     "" + columnsAnn.length,
			     field.getName(),
			     id.getColumns().size(),
			     targetType.getName()));
			     
    }

    for (int i = 0; i < columnsAnn.length; i++) {
      JAnnotation ann = (JAnnotation) columnsAnn[i];
      
      String ref = ann.getString("referencedColumnName");
      
      if (ref.equals("") && columnsAnn.length > 1)
	throw error(field, L.l("referencedColumnName is required when more than one @JoinColumn is specified."));

      Column column = findColumn(id.getColumns(), ref);

      if (column == null)
	throw error(field, L.l("referencedColumnName '{0}' does not match any key column in '{1}'.",
			       ref, targetType.getName()));
    }
  }

  private Column findColumn(ArrayList<Column> columns, String ref)
  {
    if (ref.equals("") && columns.size() == 1)
      return columns.get(0);
    
    for (Column column : columns) {
      if (column.getName().equals(ref))
	return column;
    }

    return null;
  }

  private JAnnotation getJoinColumn(Object []columnsAnn, String keyName)
  {
    if (columnsAnn == null || columnsAnn.length == 0)
      return null;

    for (int i = 0; i < columnsAnn.length; i++) {
      JAnnotation ann = (JAnnotation) columnsAnn[i];
      
      String ref = ann.getString("referencedColumnName");

      if (ref.equals("") || ref.equals(keyName))
	return ann;
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

    AmberPersistenceUnit persistenceUnit = sourceType.getPersistenceUnit();

      JType retType;

      if (field instanceof JField)
	retType = ((JField) field).getGenericType();
      else
	retType = ((JMethod) field).getGenericReturnType();

      JType []typeArgs = retType.getActualTypeArguments();

      JClass targetEntity = manyToManyAnn.getClass("targetEntity");
      String targetName = "";

      if (targetEntity != null)
	targetName = targetEntity.getName();

      if (! targetName.equals("") && ! targetName.equals("void")) {
      }
      else if (typeArgs.length > 0)
	targetName = typeArgs[0].getName();
      else
	throw new ConfigException(L.l("can't determine target name for {0}",
				      fieldName));

    EntityType targetType = persistenceUnit.getEntity(targetName);

    EntityManyToManyField manyToManyField;
    manyToManyField = new EntityManyToManyField(sourceType, fieldName);
    manyToManyField.setType(targetType);

    JAnnotation associationTableAnn =
      field.getAnnotation(JoinTable.class);

    String sqlTable = sourceType.getTable().getName() + "_" + targetType.getTable().getName();

    Table mapTable = null;

    ArrayList<ForeignColumn> sourceColumns = null;
    ArrayList<ForeignColumn> targetColumns = null;

    if (associationTableAnn != null) {
      JAnnotation table = associationTableAnn.getAnnotation("table");

      if (! table.getString("name").equals(""))
	sqlTable = table.getString("name");

      mapTable = persistenceUnit.createTable(sqlTable);

      sourceColumns = calculateColumns(mapTable,
				       sourceType,
				       (Object []) associationTableAnn.get("joinColumns"));

      targetColumns = calculateColumns(mapTable,
				       targetType,
				       (Object []) associationTableAnn.get("inverseJoinColumns"));
    }
    else {
      mapTable = persistenceUnit.createTable(sqlTable);

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

  private void validateAnnotations(JAccessibleObject field,
				   HashSet<String> validAnnotations)
    throws ConfigException
  {
    for (JAnnotation ann : field.getDeclaredAnnotations()) {
      String name = ann.getType();
      
      if (! name.startsWith("javax.persistence"))
	continue;

      if (! validAnnotations.contains(name))
	throw error(field, L.l("{0} may not have a @{1} annotation.",
			       field.getName(),
			       name));
    }
  }

  private ConfigException error(JAccessibleObject field,
				String msg)
  {
    // XXX: the field is for line numbers in the source, theoretically

    String className = field.getDeclaringClass().getShortName();
    
    return new ConfigException(className + ": " + msg);
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
    return name.toUpperCase();
    /*
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
    */
  }

  /**
   * completes for dependent
   */
  class Completion {
    protected EntityType _entityType;

    protected Completion(EntityType entityType)
    {
      _entityType = entityType;
    }
    
    EntityType getEntityType()
    {
      return _entityType;
    }
    
    void complete()
      throws ConfigException
    {
    }
  }

  /**
   * completes for dependent
   */
  class OneToManyCompletion extends Completion {
    private JAccessibleObject _field;
    private String _fieldName;
    private JClass _fieldType;

    OneToManyCompletion(EntityType type,
			JAccessibleObject field,
			String fieldName,
			JClass fieldType)
    {
      super(type);
      
      _field = field;
      _fieldName = fieldName;
      _fieldType = fieldType;
    }

    void complete()
      throws ConfigException
    {
      JAnnotation oneToManyAnn = _field.getAnnotation(OneToMany.class);

      AmberPersistenceUnit persistenceUnit = _entityType.getPersistenceUnit();

      JType retType;

      if (_field instanceof JField)
	retType = ((JField) _field).getGenericType();
      else
	retType = ((JMethod) _field).getGenericReturnType();

      JType []typeArgs = retType.getActualTypeArguments();

      JClass targetEntity = oneToManyAnn.getClass("targetEntity");
      String targetName = "";

      if (targetEntity != null)
	targetName = targetEntity.getName();

      if (! targetName.equals("") && ! targetName.equals("void")) {
      }
      else if (typeArgs.length > 0)
	targetName = typeArgs[0].getName();
      else
	throw new ConfigException(L.l("can't determine target name for {0}",
				      _fieldName));

      EntityType targetType = persistenceUnit.getEntity(targetName);
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
    private JAccessibleObject _field;
    private String _fieldName;
    private JClass _fieldType;

    OneToOneCompletion(EntityType type,
		       JAccessibleObject field,
		       String fieldName,
		       JClass fieldType)
    {
      super(type);
      
      _field = field;
      _fieldName = fieldName;
      _fieldType = fieldType;
    }

    void complete()
      throws ConfigException
    {
      JAnnotation oneToOneAnn = _field.getAnnotation(OneToOne.class);

      AmberPersistenceUnit persistenceUnit = _entityType.getPersistenceUnit();

      JClass targetEntity = oneToOneAnn.getClass("targetEntity");
      String targetName = null;

      if (targetEntity != null)
	targetName = targetEntity.getName();
      
      String mappedBy =  oneToOneAnn.getString("mappedBy");

      EntityType targetType = null;

      if (targetName != null && ! targetName.equals("")) {
	targetType = persistenceUnit.getEntity(targetName);
	
	if (targetType == null)
	  throw new ConfigException(L.l("{0}: '{1}' is an unknown entity for '{2}'.",
					_field.getDeclaringClass().getName(),
					targetName,
					_field.getName()));
      }
      else {
	targetType = persistenceUnit.getEntity(_field.getReturnType().getName());

	if (targetType == null)
	  throw new ConfigException(L.l("{0} can't determine target name for '{1}'",
					_field.getDeclaringClass().getName(),
					_field.getName()));
      }

      if (mappedBy == null || mappedBy.equals("")) {
	// ejb/0o03
	addManyToOne(_entityType, _field, _fieldName, _field.getReturnType());
	
	// XXX: set unique
      }
      else {
	EntityManyToOneField sourceField
	  = getSourceField(targetType, mappedBy);

	if (sourceField == null)
	  throw new ConfigException(L.l("{0}: OneToOne target '{1}' does not have a matching ManyToOne relation.",
					_field.getDeclaringClass().getName(),
					targetType.getName()));

	DependentEntityOneToOneField oneToOne;

	oneToOne = new DependentEntityOneToOneField(_entityType, _fieldName);
	oneToOne.setTargetField(sourceField);

	_entityType.addField(oneToOne);
      }
    }

    private EntityManyToOneField getSourceField(EntityType targetType,
						String mappedBy)
    {
      for (AmberField field : targetType.getFields()) {
	if (field.getName().equals(mappedBy)) {
	  return (EntityManyToOneField) field;
	}
      }

      return null;
    }
  }

  /**
   * completes for dependent
   */
  class ManyToManyCompletion extends Completion {
    private JAccessibleObject _field;
    private String _fieldName;
    private JClass _fieldType;

    ManyToManyCompletion(EntityType type,
			 JAccessibleObject field,
			 String fieldName,
			 JClass fieldType)
    {
      super(type);
      
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
    private JAccessibleObject _field;
    private String _fieldName;
    private JClass _fieldType;

    ManyToOneCompletion(EntityType type,
			JAccessibleObject field,
			String fieldName,
			JClass fieldType)
    {
      super(type);

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
    // annotations allowed with a @Basic annotation
    _basicAnnotations.add("javax.persistence.Basic");
    _basicAnnotations.add("javax.persistence.Column");
    _basicAnnotations.add("javax.persistence.Enumerated");
    _basicAnnotations.add("javax.persistence.Lob");
    _basicAnnotations.add("javax.persistence.Temporal");
    
    // non-serializable types allowed with a @Basic annotation
    _basicTypes.add("boolean");
    _basicTypes.add("byte");
    _basicTypes.add("char");
    _basicTypes.add("short");
    _basicTypes.add("int");
    _basicTypes.add("long");
    _basicTypes.add("float");
    _basicTypes.add("double");
    _basicTypes.add("[byte");
    _basicTypes.add("[char");
    _basicTypes.add("[java.lang.Byte");
    _basicTypes.add("[java.lang.Character");

    // annotations allowed with an @Id annotation
    _idAnnotations.add("javax.persistence.Column");
    _idAnnotations.add("javax.persistence.GeneratedValue");
    _idAnnotations.add("javax.persistence.Id");
    _idAnnotations.add("javax.persistence.SequenceGenerator");
    _idAnnotations.add("javax.persistence.TableGenerator");
    
    // allowed with a @Id annotation
    _idTypes.add("boolean");
    _idTypes.add("byte");
    _idTypes.add("char");
    _idTypes.add("short");
    _idTypes.add("int");
    _idTypes.add("long");
    _idTypes.add("float");
    _idTypes.add("double");
    _idTypes.add("java.lang.Boolean");
    _idTypes.add("java.lang.Byte");
    _idTypes.add("java.lang.Character");
    _idTypes.add("java.lang.Short");
    _idTypes.add("java.lang.Integer");
    _idTypes.add("java.lang.Long");
    _idTypes.add("java.lang.Float");
    _idTypes.add("java.lang.Double");
    _idTypes.add("java.lang.String");
    _idTypes.add("java.util.Date");
    _idTypes.add("java.sql.Date");
    
    // annotations allowed with a @ManyToOne annotation
    _manyToOneAnnotations.add("javax.persistence.ManyToOne");
    _manyToOneAnnotations.add("javax.persistence.JoinColumn");
    _manyToOneAnnotations.add("javax.persistence.JoinColumns");
    
    // ??
    _propertyAnnotations.add("javax.persistence.Basic");
    _propertyAnnotations.add("javax.persistence.Column");
    _propertyAnnotations.add("javax.persistence.Id");
    _propertyAnnotations.add("javax.persistence.Transient");
    _propertyAnnotations.add("javax.persistence.OneToOne");
    _propertyAnnotations.add("javax.persistence.ManyToOne");
    _propertyAnnotations.add("javax.persistence.OneToMany");
    _propertyAnnotations.add("javax.persistence.ManyToMany");
    _propertyAnnotations.add("javax.persistence.JoinColumn");
  }
}
