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

package com.caucho.amber.cfg;

import com.caucho.amber.field.IdField;
import com.caucho.amber.table.ForeignColumn;
import com.caucho.amber.type.AbstractStatefulType;
import com.caucho.amber.type.RelatedType;
import com.caucho.bytecode.JAccessibleObject;
import com.caucho.bytecode.JAnnotation;
import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JField;
import com.caucho.bytecode.JMethod;
import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Logger;
import java.lang.reflect.*;


/**
 * Abstract introspector for orm.xml and annotations.
 */
abstract public class AbstractConfigIntrospector {
  private static final Logger log
    = Logger.getLogger(AbstractConfigIntrospector.class.getName());
  private static final L10N L = new L10N(AbstractConfigIntrospector.class);

  // annotations allowed for a property
  static HashSet<String> _propertyAnnotations
    = new HashSet<String>();

  // types allowed with a @Basic annotation
  static HashSet<String> _basicTypes = new HashSet<String>();

  // annotations allowed with a @Basic annotation
  static HashSet<String> _basicAnnotations = new HashSet<String>();

  // types allowed with an @Id annotation
  static HashSet<String> _idTypes = new HashSet<String>();

  // annotations allowed with an @Id annotation
  static HashSet<String> _idAnnotations = new HashSet<String>();

  // annotations allowed with a @ManyToOne annotation
  static HashSet<String> _manyToOneAnnotations = new HashSet<String>();

  // annotations allowed with a @OneToMany annotation
  static HashSet<String> _oneToManyAnnotations = new HashSet<String>();

  // types allowed with a @OneToMany annotation
  static HashSet<String> _oneToManyTypes = new HashSet<String>();

  // annotations allowed with a @ManyToMany annotation
  static HashSet<String> _manyToManyAnnotations = new HashSet<String>();

  // types allowed with a @ManyToMany annotation
  static HashSet<String> _manyToManyTypes = new HashSet<String>();

  // annotations allowed with a @OneToOne annotation
  static HashSet<String> _oneToOneAnnotations = new HashSet<String>();

  // annotations allowed with a @Embedded annotation
  static HashSet<String> _embeddedAnnotations = new HashSet<String>();

  // annotations allowed with a @EmbeddedId annotation
  static HashSet<String> _embeddedIdAnnotations = new HashSet<String>();

  // annotations allowed with a @Version annotation
  static HashSet<String> _versionAnnotations = new HashSet<String>();

  // types allowed with an @Version annotation
  static HashSet<String> _versionTypes = new HashSet<String>();

  AnnotationConfig _annotationCfg = new AnnotationConfig();


  /**
   * Validates a callback method
   */
  void validateCallback(String callbackName,
                        JMethod method,
                        boolean isListener)
    throws ConfigException
  {
    if (method.isFinal())
      throw error(method, L.l("'{0}' must not be final.  @{1} methods may not be final.",
                                    method.getFullName(),
                                    callbackName));

    if (method.isStatic())
      throw error(method, L.l("'{0}' must not be static.  @{1} methods may not be static.",
                                    method.getFullName(),
                                    callbackName));

    JClass params[] = method.getParameterTypes();

    if (isListener) {
      if (params.length != 1) {
        throw error(method, L.l("'{0}' must have the <METHOD>(Object) signature for entity listeners.",
                                      method.getFullName()));
      }
    }
    else if (params.length != 0) {
      throw error(method, L.l("'{0}' must not have any arguments.  @{1} methods have zero arguments for entities or mapped superclasses.",
                                    method.getFullName(),
                                    callbackName));
    }
  }

  /**
   * Validates the bean
   */
  public void validateType(JClass type, boolean isEntity)
    throws ConfigException
  {
    if (type.isFinal())
      throw new ConfigException(L.l("'{0}' must not be final.  Entity beans may not be final.",
                                    type.getName()));

    // NOTE: Both abstract and concrete classes can be entities.

    // MappedSuperclass does not need constructor validation.
    if (isEntity)
      validateConstructor(type);

    for (JMethod method : type.getMethods()) {
      if (method.getDeclaringClass().getName().equals("java.lang.Object")) {
      }
      else if (method.isFinal())
        throw error(method, L.l("'{0}' must not be final.  Entity beans methods may not be final.",
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

      if (param.length == 0 && (ctor.isPublic() || ctor.isProtected()))
        return;
    }

    // jpa/0gb2
    throw new ConfigException(L.l("'{0}' needs a public or protected no-arg constructor.  Entity beans must have a public or protected no-arg constructor.",
                                  type.getName()));
  }

  /**
   * Validates a non-getter method.
   */
  public void validateNonGetter(JMethod method)
    throws ConfigException
  {
    JAnnotation ann = isAnnotatedMethod(method);

    if ((ann != null) && (! ann.getType().equals("javax.persistence.Version")))  {
      throw error(method,
                  L.l("'{0}' is not a valid annotation for {1}.  Only public getters and fields may have property annotations.",
                      ann.getType(), method.getFullName()));
    }
  }

  /**
   * Validates a non-getter method.
   */
  JAnnotation isAnnotatedMethod(JMethod method)
    throws ConfigException
  {
    for (JAnnotation ann : method.getDeclaredAnnotations()) {
      if (_propertyAnnotations.contains(ann.getType())) {
        return ann;
      }
    }

    return null;
  }

  static boolean containsFieldOrCompletion(AbstractStatefulType type,
                                           String fieldName)
  {
    // jpa/0l03

    while (type != null) {

      if (type.getField(fieldName) != null)
        return true;

      if (type.containsCompletionField(fieldName))
        return true;

      if (type instanceof RelatedType)
        type = ((RelatedType) type).getParentType();
    }

    return false;
  }

  static void validateAnnotations(JAccessibleObject field,
				  String fieldType,
                                  HashSet<String> validAnnotations)
    throws ConfigException
  {
    for (JAnnotation ann : field.getDeclaredAnnotations()) {
      String name = ann.getType();

      if (! name.startsWith("javax.persistence"))
        continue;

      if (! validAnnotations.contains(name)) {
        throw error(field, L.l("{0} may not have a @{1} annotation.  {2} does not allow @{3}.",
                               field.getName(),
                               name,
			       fieldType,
			       name));
      }
    }
  }

  static ConfigException error(JAccessibleObject field,
                               String msg)
  {
    // XXX: the field is for line numbers in the source, theoretically

    String className = field.getDeclaringClass().getName();

    int line = field.getLine();

    if (line > 0)
      return new ConfigException(className + ":" + line + ": " + msg);
    else
      return new ConfigException(className + "." + field.getName() + ": " + msg);
  }

  static String toFieldName(String name)
  {
    if (Character.isLowerCase(name.charAt(0)))
      return name;
    else if (name.length() == 1
	     || Character.isLowerCase(name.charAt(1)))
      return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    else
      return name;
  }

  static ArrayList<ForeignColumn> calculateColumns(com.caucho.amber.table.Table mapTable,
                                                   RelatedType type,
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

  static ArrayList<ForeignColumn>
    calculateColumns(JAccessibleObject field,
                     com.caucho.amber.table.Table mapTable,
                     String prefix,
                     RelatedType type,
                     Object []joinColumnsAnn,
                     HashMap<String, JoinColumnConfig> joinColumnsConfig)
    throws ConfigException
  {
    if ((joinColumnsAnn == null || joinColumnsAnn.length == 0) &&
        (joinColumnsConfig == null || joinColumnsConfig.size() == 0))
      return calculateColumns(mapTable, prefix, type);

    ArrayList<ForeignColumn> columns = new ArrayList<ForeignColumn>();

    // #1448 not reproduced.
    if (type.getId() == null)
      throw error(field, L.l("Entity {0} has no primary key defined.",
                             type.getName()));

    ArrayList<IdField> idFields = type.getId().getKeys();

    int len;

    if (joinColumnsAnn != null)
      len = joinColumnsAnn.length;
    else
      len = joinColumnsConfig.size();

    if (len != idFields.size()) {
      throw error(field, L.l("@JoinColumns for {0} do not match number of the primary key columns in {1}.  The foreign key columns must match the primary key columns.",
                             field.getName(),
                             type.getName()));
    }

    Iterator it = null;

    if (joinColumnsConfig != null)
      it = joinColumnsConfig.values().iterator();

    for (int i = 0; i < len; i++) {
      ForeignColumn foreignColumn;

      String name;

      if (joinColumnsAnn != null) {
        JAnnotation joinColumnAnn = (JAnnotation) joinColumnsAnn[i];
        name = joinColumnAnn.getString("name");
      }
      else {
        JoinColumnConfig joinColumnConfig = (JoinColumnConfig) it.next();
        name = joinColumnConfig.getName();
      }

      foreignColumn =
        mapTable.createForeignColumn(name,
                                     idFields.get(i).getColumns().get(0));

      columns.add(foreignColumn);
    }

    return columns;
  }

  static ArrayList<ForeignColumn> calculateColumns(com.caucho.amber.table.Table mapTable,
                                                   RelatedType type)
  {
    ArrayList<ForeignColumn> columns = new ArrayList<ForeignColumn>();

    RelatedType parentType = type;

    ArrayList<com.caucho.amber.table.Column> targetIdColumns;

    targetIdColumns = type.getId().getColumns();

    while (targetIdColumns.size() == 0) {

      parentType = parentType.getParentType();

      if (parentType == null)
        break;

      targetIdColumns = parentType.getId().getColumns();
    }

    for (com.caucho.amber.table.Column key : targetIdColumns) {
      columns.add(mapTable.createForeignColumn(key.getName(), key));
    }

    return columns;
  }

  static ArrayList<ForeignColumn> calculateColumns(com.caucho.amber.table.Table mapTable,
                                                   String prefix,
                                                   RelatedType type)
  {
    ArrayList<ForeignColumn> columns = new ArrayList<ForeignColumn>();

    RelatedType parentType = type;

    ArrayList<com.caucho.amber.table.Column> targetIdColumns;

    targetIdColumns = type.getId().getColumns();

    while (targetIdColumns.size() == 0) {

      parentType = parentType.getParentType();

      if (parentType == null)
        break;

      targetIdColumns = parentType.getId().getColumns();
    }

    for (com.caucho.amber.table.Column key : targetIdColumns) {
      columns.add(mapTable.createForeignColumn(prefix + key.getName(), key));
    }

    return columns;
  }

  protected static String loc(JMethod method)
  {
    return method.getDeclaringClass().getName() + "." + method.getName() + ": ";
  }

  protected static String loc(Method method)
  {
    return method.getDeclaringClass().getSimpleName() + "." + method.getName() + ": ";
  }

  protected static String loc(JField field)
  {
    return field.getDeclaringClass().getName() + "." + field.getName() + ": ";
  }

  protected static String loc(Field field)
  {
    return field.getDeclaringClass().getSimpleName() + "." + field.getName() + ": ";
  }

  public static String toSqlName(String name)
  {
    return name.toUpperCase();
  }

  class AnnotationConfig {
    private JAnnotation _annotation;
    private Object _config;

    public JAnnotation getAnnotation()
    {
      return _annotation;
    }

    public Object getConfig()
    {
      return _config;
    }

    public void setAnnotation(JAnnotation annotation)
    {
      _annotation = annotation;
    }

    public void setConfig(Object config)
    {
      _config = config;
    }

    public boolean isNull()
    {
      return (_annotation == null) && (_config == null);
    }

    public void reset()
    {
      _annotation = null;
      _config = null;
    }

    public void reset(JClass type, Class cl)
    {
      _annotation = type.getAnnotation(cl);
      _config = null;
    }

    public void reset(JAccessibleObject field, Class cl)
    {
      _annotation = field.getAnnotation(cl);
      _config = null;
    }

    public EmbeddableConfig getEmbeddableConfig()
    {
      return (EmbeddableConfig) _config;
    }

    public EntityConfig getEntityConfig()
    {
      return (EntityConfig) _config;
    }

    public MappedSuperclassConfig getMappedSuperclassConfig()
    {
      return (MappedSuperclassConfig) _config;
    }

    public EntityListenersConfig getEntityListenersConfig()
    {
      return (EntityListenersConfig) _config;
    }

    public TableConfig getTableConfig() {
      return (TableConfig) _config;
    }

    public SecondaryTableConfig getSecondaryTableConfig() {
      return (SecondaryTableConfig) _config;
    }

    public IdClassConfig getIdClassConfig() {
      return (IdClassConfig) _config;
    }

    public PostLoadConfig getPostLoadConfig() {
      return (PostLoadConfig) _config;
    }

    public PrePersistConfig getPrePersistConfig() {
      return (PrePersistConfig) _config;
    }

    public PostPersistConfig getPostPersistConfig() {
      return (PostPersistConfig) _config;
    }

    public PreUpdateConfig getPreUpdateConfig() {
      return (PreUpdateConfig) _config;
    }

    public PostUpdateConfig getPostUpdateConfig() {
      return (PostUpdateConfig) _config;
    }

    public PreRemoveConfig getPreRemoveConfig() {
      return (PreRemoveConfig) _config;
    }

    public PostRemoveConfig getPostRemoveConfig() {
      return (PostRemoveConfig) _config;
    }

    public InheritanceConfig getInheritanceConfig() {
      return (InheritanceConfig) _config;
    }

    public NamedQueryConfig getNamedQueryConfig() {
      return (NamedQueryConfig) _config;
    }

    public NamedNativeQueryConfig getNamedNativeQueryConfig() {
      return (NamedNativeQueryConfig) _config;
    }

    public SqlResultSetMappingConfig getSqlResultSetMappingConfig() {
      return (SqlResultSetMappingConfig) _config;
    }

    public PrimaryKeyJoinColumnConfig getPrimaryKeyJoinColumnConfig() {
      return (PrimaryKeyJoinColumnConfig) _config;
    }

    public DiscriminatorColumnConfig getDiscriminatorColumnConfig() {
      return (DiscriminatorColumnConfig) _config;
    }

    public IdConfig getIdConfig() {
      return (IdConfig) _config;
    }

    public EmbeddedIdConfig getEmbeddedIdConfig() {
      return (EmbeddedIdConfig) _config;
    }

    public ColumnConfig getColumnConfig() {
      return (ColumnConfig) _config;
    }

    public GeneratedValueConfig getGeneratedValueConfig() {
      return (GeneratedValueConfig) _config;
    }

    public BasicConfig getBasicConfig() {
      return (BasicConfig) _config;
    }

    public VersionConfig getVersionConfig() {
      return (VersionConfig) _config;
    }

    public ManyToOneConfig getManyToOneConfig() {
      return (ManyToOneConfig) _config;
    }

    public OneToOneConfig getOneToOneConfig() {
      return (OneToOneConfig) _config;
    }

    public ManyToManyConfig getManyToManyConfig() {
      return (ManyToManyConfig) _config;
    }

    public OneToManyConfig getOneToManyConfig() {
      return (OneToManyConfig) _config;
    }

    public MapKeyConfig getMapKeyConfig() {
      return (MapKeyConfig) _config;
    }

    public JoinTableConfig getJoinTableConfig() {
      return (JoinTableConfig) _config;
    }

    public JoinColumnConfig getJoinColumnConfig() {
      return (JoinColumnConfig) _config;
    }

    public AttributeOverrideConfig getAttributeOverrideConfig() {
      return (AttributeOverrideConfig) _config;
    }

    // public AttributeOverridesConfig getAttributeOverridesConfig() {
    //   return (AttributeOverridesConfig) _config;
    // }

    public AssociationOverrideConfig getAssociationOverrideConfig() {
      return (AssociationOverrideConfig) _config;
    }

    // public AssociationOverridesConfig getAssociationOverridesConfig() {
    //   return (AssociationOverridesConfig) _config;
    // }
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
    _idAnnotations.add("javax.persistence.Temporal");

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

    // annotations allowed with a @OneToMany annotation
    _oneToManyAnnotations.add("javax.persistence.OneToMany");
    _oneToManyAnnotations.add("javax.persistence.JoinTable");
    _oneToManyAnnotations.add("javax.persistence.MapKey");
    _oneToManyAnnotations.add("javax.persistence.OrderBy");

    // types allowed with a @OneToMany annotation
    _oneToManyTypes.add("java.util.Collection");
    _oneToManyTypes.add("java.util.List");
    _oneToManyTypes.add("java.util.Set");
    _oneToManyTypes.add("java.util.Map");

    // annotations allowed with a @ManyToMany annotation
    _manyToManyAnnotations.add("javax.persistence.ManyToMany");
    _manyToManyAnnotations.add("javax.persistence.JoinTable");
    _manyToManyAnnotations.add("javax.persistence.MapKey");
    _manyToManyAnnotations.add("javax.persistence.OrderBy");

    // types allowed with a @ManyToMany annotation
    _manyToManyTypes.add("java.util.Collection");
    _manyToManyTypes.add("java.util.List");
    _manyToManyTypes.add("java.util.Set");
    _manyToManyTypes.add("java.util.Map");

    // annotations allowed with a @OneToOne annotation
    _oneToOneAnnotations.add("javax.persistence.OneToOne");
    _oneToOneAnnotations.add("javax.persistence.JoinColumn");
    _oneToOneAnnotations.add("javax.persistence.JoinColumns");

    // annotations allowed with a @Embedded annotation
    _embeddedAnnotations.add("javax.persistence.Embedded");
    _embeddedAnnotations.add("javax.persistence.AttributeOverride");
    _embeddedAnnotations.add("javax.persistence.AttributeOverrides");
    _embeddedAnnotations.add("javax.persistence.Column");

    // annotations allowed with a @EmbeddedId annotation
    _embeddedIdAnnotations.add("javax.persistence.EmbeddedId");
    _embeddedIdAnnotations.add("javax.persistence.AttributeOverride");
    _embeddedIdAnnotations.add("javax.persistence.AttributeOverrides");

    // annotations allowed for a property
    _propertyAnnotations.add("javax.persistence.Basic");
    _propertyAnnotations.add("javax.persistence.Column");
    _propertyAnnotations.add("javax.persistence.Id");
    _propertyAnnotations.add("javax.persistence.Transient");
    _propertyAnnotations.add("javax.persistence.OneToOne");
    _propertyAnnotations.add("javax.persistence.ManyToOne");
    _propertyAnnotations.add("javax.persistence.OneToMany");
    _propertyAnnotations.add("javax.persistence.ManyToMany");
    _propertyAnnotations.add("javax.persistence.JoinColumn");
    _propertyAnnotations.add("javax.persistence.Embedded");
    _propertyAnnotations.add("javax.persistence.EmbeddedId");
    _propertyAnnotations.add("javax.persistence.Version");

    // annotations allowed with a @Version annotation
    _versionAnnotations.add("javax.persistence.Version");
    _versionAnnotations.add("javax.persistence.Column");
    _versionAnnotations.add("javax.persistence.Temporal");

    // types allowed with a @Version annotation
    _versionTypes.add("short");
    _versionTypes.add("int");
    _versionTypes.add("long");
    _versionTypes.add("java.lang.Short");
    _versionTypes.add("java.lang.Integer");
    _versionTypes.add("java.lang.Long");
    _versionTypes.add("java.sql.Timestamp");
  }
}
