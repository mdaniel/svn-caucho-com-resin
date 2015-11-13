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

package com.caucho.v5.config.custom;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.NormalScope;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Qualifier;
import javax.inject.Scope;
import javax.interceptor.InterceptorBinding;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.ConfiguredLiteral;
import com.caucho.v5.config.InlineConfig;
import com.caucho.v5.config.candi.BeanManagerBase;
import com.caucho.v5.config.candi.BeansConfig;
import com.caucho.v5.config.candi.CandiManager;
import com.caucho.v5.config.candi.ScanRootInject;
import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.config.inject.InjectContext;
import com.caucho.v5.config.program.Arg;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.program.ContainerProgram;
import com.caucho.v5.config.reflect.AnnotatedElementImpl;
import com.caucho.v5.config.reflect.AnnotatedMethodImpl;
import com.caucho.v5.config.reflect.AnnotatedTypeImpl;
import com.caucho.v5.config.reflect.BaseType;
import com.caucho.v5.config.reflect.ReflectionAnnotatedFactory;
import com.caucho.v5.config.type.ConfigType;
import com.caucho.v5.config.type.TypeFactoryConfig;
import com.caucho.v5.util.L10N;

/**
 * Custom bean configured by namespace
 */
public class ConfigCustomBean<T>
{
  private static final L10N L = new L10N(ConfigCustomBean.class);

  private CandiManager _cdiManager;

  private Class<T> _class;
  private AnnotatedTypeImpl<T> _annotatedType;
  private Bean<T> _bean;
  private ConfigType<T> _configType;

  private ArrayList<ConfigProgram> _args;

  private NameCfg _name;

  private String _filename;
  private int _line;

  private ContainerProgram _init;
  private boolean _hasBindings;
  private boolean _hasInterceptorBindings;
  private boolean _isInlineBean;
  private boolean _isBeansXml;

  public ConfigCustomBean(NameCfg name,
                          Class<T> cl,
                          Object parent)
  {
    Objects.requireNonNull(name);
    Objects.requireNonNull(cl);
    Objects.requireNonNull(parent);
    
    _name = name;

    _class = cl;

    _cdiManager = CandiManager.create();
    
    ScanRootInject scanRoot = ScanRootInject.getCurrent();
    
    BeanManagerBase beanManager = _cdiManager.getBeanManager();
    
    if (scanRoot != null) {
      beanManager = scanRoot.getBeanManager();
    }
    
    // XXX:
    // _component = new SimpleBean(cl);
    // _component.setScopeClass(Dependent.class);
    // ioc/2601
    BaseType baseType = _cdiManager.createSourceBaseType(cl);
    AnnotatedType<T> annType = ReflectionAnnotatedFactory.introspectType(baseType);
    
    _annotatedType = new AnnotatedTypeImpl<T>(annType, beanManager);
      
    _cdiManager.addConfiguredBean(cl.getName());
    
    _configType = TypeFactoryConfig.getCustomBeanType(cl);

    // defaults to @Configured
    // clearAnnotations(_annotatedType, DeploymentType.class);
    _annotatedType.addAnnotation(ConfiguredLiteral.create());

    for (AnnotatedMethod<?> method : _annotatedType.getMethods()) {
      // ioc/0614

      AnnotatedMethodImpl<?> methodImpl = (AnnotatedMethodImpl<?>) method;

      if (methodImpl.isAnnotationPresent(Produces.class)) {
        methodImpl.addAnnotation(ConfiguredLiteral.create());
      }
    }
    
    if (parent instanceof BeansConfig) {
      _isBeansXml = true;
    }
  }

  public ConfigType<T> getConfigType()
  {
    return _configType;
  }

  public Class<T> getClassType()
  {
    return _class;
  }
  
  protected AnnotatedType<?> getAnnotatedType()
  {
    return _annotatedType;
  }
  
  protected Bean<?> getBean()
  {
    return _bean;
  }

  public void setConfigLocation(String filename, int line)
  {
    _filename = filename;
    _line = line;
  }

  public String getFilename()
  {
    return _filename;
  }

  public int getLine()
  {
    return _line;
  }

  public void setInlineBean(boolean isInline)
  {
    _isInlineBean = isInline;
  }
  
  public void setBeansXml(boolean isBeansXml)
  {
    _isBeansXml = isBeansXml;
  }
  
  public ConfigCustomNew createNew()
  {
    throw new UnsupportedOperationException();
    //return new ConfigCustomNew(this);
  }
  
  public void addArg(ConfigProgram arg)
  {
    if (addAnnotation(arg)) {
      return;
    }
    
    if (_args == null) {
      _args = new ArrayList<ConfigProgram>();
    }

    _args.add(arg);
  }

  public void addArgs(ArrayList<ConfigProgram> args)
  {
    _args = new ArrayList<ConfigProgram>(args);
  }

  /*
  public void setNew(ConfigProgram []args)
  {
    if (_args == null)
      _args = new ArrayList<ConfigProgram>();

    for (ConfigProgram arg : args) {
      _args.add(arg);
    }
  }
  */

  public void addAdd(ConfigProgram add)
  {
    addInitProgram(add);
  }

  public void addInitProgram(ConfigProgram program)
  {
    if (_init == null) {
      _init = new ContainerProgram();

      /*
      if (_component != null)
        _component.setInit(_init);
      */
    }

    _init.addProgram(program);
  }

  public void addBuilderProgram(ConfigProgram program)
  {
    if (addAnnotation(program)) {
      return;
      
    }
    
    NameCfg name = program.getQName();

    if (name == null) {
      addInitProgram(program);

      return;
    }

    if (name.getNamespaceURI().equals(_name.getNamespaceURI())) {
      if (_configType.getAttribute(name) != null)
        addInitProgram(program);
      else {
        throw new ConfigException(L.l("'{0}' is an unknown field for '{1}'",
                                      name.getLocalName(), _class.getName()));
      }
    }

    else
      throw new ConfigException(L.l("'{0}' is an unknown field name.  Fields must belong to the same namespace as the class",
                                    name.getCanonicalName()));
  }

  /*
  private Node getProgramNode(ConfigProgram program)
  {
    if (program instanceof NodeBuilderChildProgram) {
      return ((NodeBuilderChildProgram) program).getNode();
    }
    
    return null;
  }
  */
  
  private boolean addAnnotation(ConfigProgram program)
  {
    NameCfg name = program.getQName();

    if (name == null) {
      return false;
    }

    Class<?> cl = createClass(name);

    if (cl != null && Annotation.class.isAssignableFrom(cl)) {
      ConfigType<?> type = TypeFactoryConfig.getType(cl);

      Object bean = type.create(null, name);

      program.configure(bean);
      /*
      Node node = getProgramNode(program);

      if (node != null)
        XmlConfigContext.getCurrent().configureNode(node, bean, type);
      */        

      Annotation ann = (Annotation) type.replaceObject(bean);

      addAnnotation(ann);

      return false;
    }
    
    return true;
  }

  public void addAnnotation(Annotation ann)
  {
    // XXX: some annotations also remove other annotations
    if (ann.annotationType().isAnnotationPresent(Qualifier.class)
        && ! _hasBindings) {
      _hasBindings = true;
      clearBindings(_annotatedType);
    }

    if (ann.annotationType().isAnnotationPresent(InterceptorBinding.class)
        && ! _hasInterceptorBindings) {
      _hasInterceptorBindings = true;
      clearAnnotations(_annotatedType, InterceptorBinding.class);
    }

    if (ann.annotationType().isAnnotationPresent(Scope.class)
        || ann.annotationType().isAnnotationPresent(NormalScope.class)) {
      clearAnnotations(_annotatedType, Scope.class);
      clearAnnotations(_annotatedType, NormalScope.class);
    }

    _annotatedType.addAnnotation(ann);
  }

  public void addMethod(ConfigCustomMethod methodConfig)
  {
    Method method = methodConfig.getMethod();
    Annotation []annList = methodConfig.getAnnotations();

    AnnotatedMethod<?> annMethod = _annotatedType.createMethod(method);

    if (annMethod instanceof AnnotatedMethodImpl<?>) {
      AnnotatedMethodImpl<?> methodImpl = (AnnotatedMethodImpl<?>) annMethod;

      // ioc/0c64
      methodImpl.clearAnnotations();

      addAnnotations(methodImpl, annList);
    }

    //_component.addMethod(new SimpleBeanMethod(method, annList));
  }

  private void addAnnotations(AnnotatedElementImpl annotated,
                              Annotation []annList)
  {
    for (Annotation ann : annList) {
      annotated.addAnnotation(ann);
    }
  }

  public void addField(ConfigCustomField fieldConfig)
  {
    // Field field = fieldConfig.getField();
    // Annotation []annList = fieldConfig.getAnnotations();

    //_component.addField(new SimpleBeanField(field, annList));
  }

  private void clearBindings(AnnotatedTypeImpl<?> beanType)
  {
    HashSet<Annotation> annSet
      = new HashSet<Annotation>(beanType.getAnnotations());

    for (Annotation ann : annSet) {
      if (ann.annotationType().isAnnotationPresent(Qualifier.class))
        beanType.removeAnnotation(ann);
    }
  }

  private void clearAnnotations(AnnotatedTypeImpl<?> beanType,
                                Class<? extends Annotation> annType)
  {
    HashSet<Annotation> annSet
      = new HashSet<Annotation>(beanType.getAnnotations());

    for (Annotation ann : annSet) {
      if (ann.annotationType().isAnnotationPresent(annType))
        beanType.removeAnnotation(ann);
    }
  }

  private Class<?> createClass(NameCfg name)
  {
    String uri = name.getNamespaceURI();
    String localName = name.getLocalName();

    if (! uri.startsWith("urn:java:"))
      return null;

    String pkg = uri.substring("urn:java:".length());

    return TypeFactoryConfig.loadClass(pkg, name.getLocalName());
  }

  @PostConstruct
  public void init()
  {
    if (_annotatedType != null) {
      initBean();
    }
  }

  public void initBean()
  {
    /* XXX: constructor

    if (_args != null)
      _component.setNewArgs(_args);
    */

    Arg<?> []newProgram = null;
    Constructor<?> javaCtor = null;

    if (_args != null) {
      AnnotatedConstructor<T> ctor = null;

      for (AnnotatedConstructor<T> testCtor
             : _annotatedType.getConstructors()) {
        if (testCtor.getParameters().size() == _args.size())
          ctor = testCtor;
      }

      if (ctor == null) {
        throw new ConfigException(L.l("No matching constructor found for '{0}' with {1} arguments.",
                                      _annotatedType, _args.size()));
      }

      javaCtor = ctor.getJavaMember();
      ArrayList<ConfigProgram> newList = _args;

      newProgram = new Arg[newList.size()];

      Type []genericParam = javaCtor.getGenericParameterTypes();
      List<AnnotatedParameter<T>> parameters
        = (List<AnnotatedParameter<T>>) ctor.getParameters();
      String loc = null;

      for (int i = 0; i < _args.size(); i++) {
        ConfigProgram argProgram = _args.get(i);
        ConfigType<?> type = TypeFactoryConfig.getType(genericParam[i]);

        if (argProgram != null)
          newProgram[i] = new ProgramArg(type, argProgram);
        else
          newProgram[i] = new BeanArg(loc, genericParam[i], parameters.get(i).getAnnotations());
      }
    }
    else {
      newProgram = new Arg[0];
    }

    ConfigProgram []injectProgram;

    if (_init != null) {
      ArrayList<ConfigProgram> programList = _init.getProgramList();

      injectProgram = new ConfigProgram[programList.size()];
      programList.toArray(injectProgram);
    }
    else
      injectProgram = new ConfigProgram[0];

    CookieCustomBean customCookie = _cdiManager.generateCustomBeanCookie();
    
    _annotatedType.addAnnotation(customCookie);
    
    // XXX: should be local to the archive containing the config file
    BeanManagerBase beanManager = _annotatedType.getBeanManager();
    
    ManagedBeanCustom<T> managedBean
      = new ManagedBeanCustom<T>(_cdiManager, beanManager,
                                 _annotatedType, false,
                                 _name, _filename, _line);

    managedBean.introspect();
    
    InjectionTargetCustomBean<T> injectionTarget
      = new InjectionTargetCustomBean(managedBean, javaCtor, newProgram, injectProgram);
    
    _bean = new BeanCustom<T>(managedBean, injectionTarget);

    _cdiManager.addInjectionTargetCustom(customCookie.value(), injectionTarget);
    
    if (! _isInlineBean) {
      _cdiManager.discoverBean(_annotatedType);
      
      postBind();
      
      // ioc/23n3, ioc/0603
      if (! _isBeansXml) {
        _cdiManager.update();
      }
    }
    
    //beanManager.addBean(_bean);

    //managedBean.introspectProduces();
    /*
    for (Bean producesBean : managedBean.getProducerBeans()) {
      beanManager.addBean(producesBean);
    }
    */
    
    // cache/1500
    if (_isInlineBean
        && _annotatedType.isAnnotationPresent(InlineConfig.class)) {
      toObject();
    }
  }
  
  protected void postBind()
  {
  }

  protected Bean bindParameter(String loc,
                               Type type,
                               Set<Annotation> bindingSet)
  {
    Annotation []bindings = new Annotation[bindingSet.size()];
    bindingSet.toArray(bindings);

    Set<Bean<?>> set = _cdiManager.getBeans(type, bindings);

    if (set == null || set.size() == 0)
      return null;

    return _cdiManager.resolve(set);
  }

  public Object toObject()
  {
    if (_bean == null) {
      init();
    }
    
    CandiManager beanManager = CandiManager.create();

    CreationalContext<?> env = beanManager.createCreationalContext(_bean);
    Class<?> type = _bean.getBeanClass();

    return CandiManager.create().getReference(_bean, type, env);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _class.getSimpleName() + "]";
  }

  class BeanArg extends Arg<T> {
    private String _loc;
    private Constructor<T> _ctor;
    private Type _type;
    private Set<Annotation> _bindings;
    private Bean<T> _bean;

    BeanArg(String loc, Type type, Set<Annotation> bindings)
    {
      _loc = loc;
      _type = type;
      _bindings = bindings;
      bind();
    }

    public void bind()
    {
      if (_bean == null) {
        _bean = bindParameter(_loc, _type, _bindings);

        if (_bean == null)
          throw new ConfigException(L.l("{0}: {1} does not have valid arguments",
                                        _loc, _ctor));
      }
    }

    public Object eval(CreationalContext<T> env)
    {
      if (_bean == null)
        bind();

      // XXX: getInstance for injection?
      Type type = null;
      return _cdiManager.getReference(_bean, type, env);
    }
  }

  static class ProgramArg<T> extends Arg<T> {
    private ConfigType<T> _type;
    private ConfigProgram _program;

    ProgramArg(ConfigType<T> type, ConfigProgram program)
    {
      _type = type;
      _program = program;
    }

    public Object eval(CreationalContext<T> creationalContext)
    {
      // ConfigContext env = ConfigContext.create();
      
      // env.setCreationalContext(creationalContext);
      InjectContext env = null;
      return _program.create(_type, env);
    }
  }
}
