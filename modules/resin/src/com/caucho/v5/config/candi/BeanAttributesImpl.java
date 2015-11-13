/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.config.candi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.NormalScope;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Specializes;
import javax.enterprise.inject.Typed;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.DefinitionException;
import javax.inject.Named;
import javax.inject.Qualifier;
import javax.inject.Scope;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Names;
import com.caucho.v5.config.bytecode.ScopeAdapter;
import com.caucho.v5.config.reflect.BaseType;
import com.caucho.v5.util.L10N;

public class BeanAttributesImpl<T> implements BeanAttributes<T>
{
  private static final L10N L = new L10N(BeanAttributesImpl.class);
  private static final Logger log
    = Logger.getLogger(BeanAttributesImpl.class.getName());

  protected String _name;

  protected Set<Annotation> _qualifiers = new LinkedHashSet<>();
  protected Class<? extends Annotation> _scope;
  protected Set<Class<? extends Annotation>> _stereotypes
    = new LinkedHashSet<>();
  protected Set<Type> _types = new LinkedHashSet<>();
  protected boolean _isAlternative;

  private Annotated _annotated;
  private BeanManager _manager;
  private BaseType _baseType;

  public BeanAttributesImpl(Annotated annotated, CandiManager manager)
  {
    _manager = manager;
    _annotated = annotated;

    _baseType = manager.createSourceBaseType(annotated.getBaseType());

    Set<Type> baseTypes = annotated.getTypeClosure();

    Typed typed = annotated.getAnnotation(Typed.class);

    if (typed != null) {
      _types= fillTyped(baseTypes, typed.value());
    }
    else {
      _types = baseTypes;
    }

    introspect(annotated);
  }

  private LinkedHashSet<Type> fillTyped(Set<Type> closure,
                                        Class<?> []values)
  {
    LinkedHashSet<Type> typeClasses = new LinkedHashSet<Type>();

    for (Class<?> cl : values) {
      fillType(typeClasses, closure, cl);
    }

    typeClasses.add(Object.class);

    return typeClasses;
  }

  private void fillType(LinkedHashSet<Type> types,
                        Set<Type> closure,
                        Class<?> cl)
  {
    for (Type type : closure) {
      if (type.equals(cl)) {
        types.add(type);
      }
      else if (type instanceof BaseType) {
        BaseType baseType = (BaseType) type;

        if (baseType.getRawClass().equals(cl))
          types.add(type);
      }
    }
  }

  protected void introspect(Annotated annotated)
  {
    introspectScope(annotated);
    introspectQualifiers(annotated);
    introspectName(annotated);

    if (annotated.isAnnotationPresent(Alternative.class))
      _isAlternative = true;

    introspectStereotypes(annotated);
    introspectSpecializes(annotated);

    introspectDefault();

    if (getScope().isAnnotationPresent(NormalScope.class)) {
      ScopeAdapter.validateType(annotated.getBaseType());
    }
  }

  protected void introspectScope(Annotated annotated)
  {
    if (_scope != null)
      return;

    for (Annotation ann : annotated.getAnnotations()) {
      if (_manager.isScope(ann.annotationType())) {
        if (_scope != null && _scope != ann.annotationType())
          throw new DefinitionException(L.l("{0}: @Scope annotation @{1} conflicts with @{2}.  Java Injection components may only have a single @Scope.",
                                            getTargetName(),
                                            _scope.getName(),
                                            ann.annotationType().getName()));

        _scope = ann.annotationType();
      }
    }
  }

  /**
   * Introspects the qualifier annotations
   */
  protected void introspectQualifiers(Annotated annotated)
  {
    if (_qualifiers.size() > 0)
      return;

    BeanManager inject = _manager;

    for (Annotation ann : annotated.getAnnotations()) {
      if (inject.isQualifier(ann.annotationType())) {
        if (ann.annotationType().equals(Named.class)) {
          String namedValue = getNamedValue(ann);

          if ("".equals(namedValue)) {
            ann = Names.create(getDefaultName());
          }
        }

        _qualifiers.add(ann);
      }
    }
  }

  protected void introspectName(Annotated annotated)
  {
    if (_name != null)
      return;

    Annotation ann = annotated.getAnnotation(Named.class);

    if (ann != null) {
      String value = getNamedValue(ann);

      if (value == null)
        value = "";

      _name = value;
    }
  }

  protected String getNamedValue(Annotation ann)
  {
    try {
      if (ann instanceof Named) {
        return ((Named) ann).value();
      }

      Method method = ann.getClass().getMethod("value");
      method.setAccessible(true);

      return (String) method.invoke(ann);
    } catch (NoSuchMethodException e) {
      // ioc/0m04
      log.log(Level.FINE, e.toString(), e);

      return "";
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Adds the stereotypes from the bean's annotations
   */
  protected void introspectStereotypes(Annotated annotated)
  {
    Class<? extends Annotation> scope = null;

    for (Annotation stereotype : annotated.getAnnotations()) {
      Class<? extends Annotation> stereotypeType = stereotype.annotationType();

      Set<Annotation> stereotypeSet =
        _manager.getStereotypeDefinition(stereotypeType);

      if (stereotypeSet == null)
        continue;

      _stereotypes.add(stereotype.annotationType());

      for (Annotation ann : stereotypeSet) {
        Class<? extends Annotation> annType = ann.annotationType();

        if (annType.isAnnotationPresent(Scope.class)
            || annType.isAnnotationPresent(NormalScope.class)) {
          if (_scope == null && scope != null && !scope.equals(annType)) {
            throw new DefinitionException(L.l("{0}: '{1}' is an invalid @Scope because a scope '{2}' has already been defined.  Only one @Scope or @NormalScope is allowed on a bean.",
                                              getTargetName(),
                                              scope.getName(),
                                              annType.getName()));
          }

          scope = annType;
        }

        if (annType.equals(Named.class) && _name == null) {
          String namedValue = getNamedValue(ann);
          _name = "";

          if (!"".equals(namedValue))
            throw new DefinitionException(L.l("{0}: @Named must not have a value in a @Stereotype definition, because @Stereotypes are used with multiple beans.",
                                              getTargetName()));
        }

        if (annType.isAnnotationPresent(Qualifier.class)
            && !annType.equals(Named.class)) {
          throw new ConfigException(L.l(
            "{0}: '{1}' is not allowed on @Stereotype '{2}' because stereotypes may not have @Qualifier annotations",
            getTargetName(),
            ann,
            stereotype));
        }

        if (annType.equals(Alternative.class))
          _isAlternative = true;
      }
    }

    if (_scope == null)
      _scope = scope;
  }

  protected void introspectSpecializes(Annotated annotated)
  {
    if (! annotated.isAnnotationPresent(Specializes.class))
      return;

    /*
    if (annotated.isAnnotationPresent(Named.class)) {
      throw new ConfigException(L.l("{0}: invalid @Specializes bean because it also implements @Named.",
                                    getTargetName()));
    }
    */
    
    // XXX: is this validation right for @Produces beans?
    if (annotated.isAnnotationPresent(Produces.class)) {
      return;
    }

    Type baseType = annotated.getBaseType();

    if (!(baseType instanceof Class<?>)) {
      throw new ConfigException(L.l(
        "{0}: invalid @Specializes bean because '{1}' is not a class.",
        getTargetName(),
        baseType));
    }

    Class<?> baseClass = (Class<?>) baseType;
    Class<?> parentClass = baseClass.getSuperclass();

    if (baseClass.getSuperclass() == null ||
        baseClass.getSuperclass().equals(Object.class)) {
      throw new DefinitionException(L.l(
        "{0}: invalid @Specializes bean because the superclass '{1}' is not a managed bean.",
        getTargetName(),
        baseClass.getSuperclass()));
    }

    validateSpecializes(annotated, parentClass);
  }
  
  protected void validateSpecializes(Annotated annotated,
                                     Class<?> parentClass)
  {
  }

  protected void introspectDefault()
  {
    boolean isQualifier = false;
    for (Annotation ann : _qualifiers) {
      if (!Named.class.equals(ann.annotationType())
          && !Any.class.equals(ann.annotationType())) {
        isQualifier = true;
      }
    }

    if (!isQualifier)
      _qualifiers.add(DefaultLiteral.DEFAULT);

    _qualifiers.add(AnyLiteral.ANY);

    if (_scope == null)
      _scope = Dependent.class;

    if ("".equals(_name))
      _name = getDefaultName();
  }

  protected String getDefaultName()
  {
    String name;

    if (_annotated instanceof AnnotatedMethod) {
      name = getDefaultName((AnnotatedMethod<?>) _annotated);
    }
    else if (_annotated instanceof AnnotatedField) {
      name = ((AnnotatedField) _annotated).getJavaMember().getName();
    }
    else {
      Class<?> targetClass = getTargetClass();

      if (targetClass.isAnnotationPresent(Specializes.class))
        name = targetClass.getSuperclass().getSimpleName();
      else
        name = targetClass.getSimpleName();

      name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    return name;
  }

  private String getDefaultName(AnnotatedMethod<?> method)
  {
    final String methodName = method.getJavaMember().getName();
    final String name;

    if (methodName.length() < 3
        || ! methodName.startsWith("get"))
      name = methodName;
    else if (! Character.isUpperCase(methodName.charAt(3)))
      name = methodName;
    else
      name = Character.toLowerCase(methodName.charAt(3))
             + methodName.substring(4, methodName.length());

    return name;
  }

  public Annotated getAnnotated()
  {
    return _annotated;
  }

  public String getTargetName()
  {
    return _baseType.toString();
  }

  public Class<?> getTargetClass()
  {
    return _baseType.getRawClass();
  }

  @Override
  public String getName()
  {
    return _name;
  }

  @Override
  public Set<Annotation> getQualifiers()
  {
    return _qualifiers;
  }

  @Override
  public Class<? extends Annotation> getScope()
  {
    return _scope;
  }

  @Override
  public Set<Class<? extends Annotation>> getStereotypes()
  {
    return _stereotypes;
  }

  @Override
  public Set<Type> getTypes()
  {
    return _types;
  }

  @Override
  public boolean isAlternative()
  {
    return _isAlternative;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");

    sb.append(getTargetName());

    sb.append(", {");

    ArrayList<Annotation> bindings
      = new ArrayList<Annotation>(getQualifiers());

    for (int i = 0; i < bindings.size(); i++) {
      Annotation ann = bindings.get(i);

      if (i != 0)
        sb.append(", ");

      sb.append(ann);
    }

    sb.append("}");

    if (getName() != null) {
      sb.append(", ");
      sb.append("name=");
      sb.append(getName());
    }

    if (getScope() != null && getScope() != Dependent.class) {
      sb.append(", @");
      sb.append(getScope().getSimpleName());
    }

    sb.append("]");

    return sb.toString();
  }
}
