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

package com.caucho.v5.config.extension;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.InjectionException;
import javax.enterprise.inject.spi.AfterTypeDiscovery;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.EventMetadata;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.inject.spi.ProcessProducer;
import javax.enterprise.inject.spi.Producer;
import javax.enterprise.inject.spi.WithAnnotations;

import com.caucho.v5.config.CauchoBean;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.candi.BeanBuilder;
import com.caucho.v5.config.candi.CandiManager;
import com.caucho.v5.config.candi.DefaultLiteral;
import com.caucho.v5.config.candi.EventMetadataImpl;
import com.caucho.v5.config.candi.ManagedBeanImpl;
import com.caucho.v5.config.candi.ProducesFieldBean;
import com.caucho.v5.config.candi.ProducesMethodBean;
import com.caucho.v5.config.event.EventManager;
import com.caucho.v5.config.event.ObserverMethodBase;
import com.caucho.v5.config.program.BeanArg;
import com.caucho.v5.config.reflect.BaseType;
import com.caucho.v5.inject.LazyExtension;
import com.caucho.v5.inject.Module;
import com.caucho.v5.loader.DynamicClassLoader;
import com.caucho.v5.util.ConcurrentArrayList;
import com.caucho.v5.util.IoUtil;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.Path;
import com.caucho.v5.vfs.ReadStream;
import com.caucho.v5.vfs.Vfs;

/**
 * Manages custom extensions for the inject manager.
 */
@Module
public class ExtensionManager
{
  private static final L10N L = new L10N(ExtensionManager.class);
  private static final Logger log
    = Logger.getLogger(ExtensionManager.class.getName());
  
  private final CandiManager _cdiManager;

  private String _classLoaderHash = "";
  
  private HashSet<URL> _extensionSet = new HashSet<URL>();
  
  private HashMap<Class<?>,ExtensionItem> _extensionMap
    = new HashMap<Class<?>,ExtensionItem>();
  
  private ConcurrentArrayList<PendingEvent> _pendingEventList
    = new ConcurrentArrayList<PendingEvent>(PendingEvent.class);
  
  private boolean _isCustomExtension;

  //for BeforeBeanDiscovery and AfterTypeDiscovery event sourcing
  private Extension _currentExtension;

  public ExtensionManager(CandiManager cdiManager)
  {
    _cdiManager = cdiManager;
  }
  
  boolean isCustomExtension()
  {
    return _isCustomExtension;
  }

  public void updateExtensions()
  {
    try {
      ClassLoader loader = _cdiManager.getClassLoader();

      if (loader == null)
        return;
      
      String hash = DynamicClassLoader.getHash(loader);
      
      if (_classLoaderHash.equals(hash))
        return;
      
      _classLoaderHash = hash;

      Enumeration<URL> e = loader.getResources("META-INF/services/" + Extension.class.getName());

      while (e.hasMoreElements()) {
        URL url = e.nextElement();
        
        if (_extensionSet.contains(url))
          continue;

        _extensionSet.add(url);
        
        try (InputStream is = url.openStream()) {
          parseExtensionServices(is);
        } catch (IOException e1) {
          log.log(Level.WARNING, e1.toString(), e1);
        }
      }
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
  
  public void parseExtensionServices(InputStream is)
    throws IOException
  {
    try (ReadStream in = Vfs.openRead(is)) {
      String line;

      while ((line = in.readLine()) != null) {
        int p = line.indexOf('#');
        if (p >= 0)
          line = line.substring(0, p);
        line = line.trim();

        if (line.length() > 0) {
          loadExtension(line);
        }
      }

      in.close();
    }
  }

  public void createExtension(String className)
  {
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      Class<?> cl = Class.forName(className, false, loader);
      Constructor<?> ctor = cl.getConstructor(new Class[] { CandiManager.class });

      Extension extension = (Extension) ctor.newInstance(_cdiManager);

      addExtension(extension);
    } catch (Exception e) {
      log.log(Level.FINEST, e.toString(), e);
    }
  }

  void loadExtension(String className)
  {
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      Class<?> cl = Class.forName(className, false, loader);

      if (! Extension.class.isAssignableFrom(cl))
        throw new InjectionException(L.l("'{0}' is not a valid extension because it does not implement {1}",
                                         cl, Extension.class.getName()));
      
      Extension extension = null;
      
      for (Constructor<?> ctor : cl.getDeclaredConstructors()) {
        if (ctor.getParameterTypes().length == 0) {
          ctor.setAccessible(true);
          
          extension = (Extension) ctor.newInstance();
        }
      }

      if (extension == null)
        extension = (Extension) cl.newInstance();

      addExtension(extension);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  public void addExtension(Extension ext)
  {
    if (log.isLoggable(Level.FINER)) {
      if (isCauchoExtension(ext)) {
        log.finest("add extension " + ext);
      }
      else {
        log.finer("add extension " + ext);
      }
    }

    // Register extension instance with CDI.
    // TODO Is this correct or should a managed bean be created?
    BeanBuilder<? extends Extension> beanBuilder = _cdiManager
      .createBeanBuilder(ext.getClass());

    // TODO Is singleton equivalent to ApplicationScoped? That is what the
    // extension is supposed to be as per CDI specification.
    beanBuilder.scope(ApplicationScoped.class);
    Bean<?> bean = beanBuilder.singleton(ext);

    if (! isCauchoExtension(ext)) {
      _cdiManager.addPendingExtension(beanBuilder.getAnnotatedType());
      processBean(bean, beanBuilder.getAnnotatedType());
    }

    _cdiManager.addBean(bean);

    ExtensionItem item = introspect(ext.getClass());

    for (ExtensionMethod method : item.getExtensionMethods()) {
      Method javaMethod = method.getMethod();
      Class<?> rawType = method.getBaseType().getRawClass();

      ExtensionObserver observer;
      observer = new ExtensionObserver(ext,
                                       method.getMethod(),
                                       method.getArgs(),
                                       method.getWithAnnotations());

      BaseType baseType = method.getBaseType();
      
      // #5531, convert Process<Foo> to Process<? extends Foo>
      //baseType = baseType.extendGenericType(); see comment in ioc/0p68
      _cdiManager.getEventManager().addExtensionObserver(observer,
                                                         baseType,
                                                         method.getWithAnnotations(),
                                                         method.getQualifiers());

      if ((ProcessAnnotatedType.class.isAssignableFrom(rawType))
          && ! javaMethod.isAnnotationPresent(LazyExtension.class)) {
        _cdiManager.setIsCustomExtension(true);
      }

      if ((ProcessBean.class.isAssignableFrom(rawType))
          && ! javaMethod.isAnnotationPresent(LazyExtension.class)) {
        _cdiManager.setIsCustomExtension(true);
      }

      if ((ProcessInjectionTarget.class.isAssignableFrom(rawType))
          && ! javaMethod.isAnnotationPresent(LazyExtension.class)) {
        _cdiManager.setIsCustomExtension(true);
      }

      if ((ProcessProducer.class.isAssignableFrom(rawType))
          && ! javaMethod.isAnnotationPresent(LazyExtension.class)) {
        _cdiManager.setIsCustomExtension(true);
      }
    }
  }
  
  private boolean isCauchoExtension(Extension ext)
  {
    return ext.getClass().isAnnotationPresent(CauchoBean.class);
  }

  private ExtensionItem introspect(Class<?> cl)
  {
    ExtensionItem item = _extensionMap.get(cl);

    if (item == null) {
      item = new ExtensionItem(cl);
      _extensionMap.put(cl, item);
    }

    return item;
  }

  public boolean isExtension(AnnotatedType<?> type)
  {
    return isExtension(type.getJavaClass());
  }

  boolean isExtension(Class<?> type)
  {
    // ioc/0062
    //return Extension.class.isAssignableFrom(type);
    return _extensionMap.containsKey(type);
  }

  public <T> Bean<T> processBean(Bean<T> bean, ProcessBean<T> processBean)
  {
    CandiManager cdi = _cdiManager;
    
    BaseType baseType = cdi.createTargetBaseType(processBean.getClass());
    baseType = baseType.fill(cdi.createTargetBaseType(bean.getBeanClass()));
    
    _pendingEventList.add(new PendingEvent(processBean, baseType));
    
    return processBean.getBean();
  }

  @Module
  public <T> Bean<T> processBean(Bean<T> bean, Annotated ann)
  {
    CandiManager cdi = _cdiManager;

    if (ann == null)
      ann = cdi.createAnnotatedType(bean.getBeanClass());
    
    ProcessBeanImpl<T> event = new ProcessBeanImpl<T>(_cdiManager, bean, ann);
    
    BaseType baseType = cdi.createTargetBaseType(event.getClass());
    baseType = baseType.fill(cdi.createTargetBaseType(bean.getBeanClass()));

    _pendingEventList.add(new PendingEvent(event, baseType));
    
    return event.getBean();
  }

  @Module
  public <T> BeanAttributes<T> processBeanAttributes(BeanAttributes<T> attributes,
                                                     Annotated ann)
  {
    ProcessBeanAttributesImpl event
      = new ProcessBeanAttributesImpl(_cdiManager, attributes, ann);

    BaseType baseType
      = _cdiManager.createTargetBaseType(ProcessBeanAttributesImpl.class);

    baseType
      = baseType.fill(_cdiManager.createTargetBaseType(ann.getBaseType()));

    getEventManager().fireExtensionEvent(event, baseType);

    if (event.isVeto()) {
      return null;
    }

    return event.getBeanAttributes();
  }

  @Module
  public <T> Bean<T> processManagedBean(ManagedBeanImpl<T> bean, Annotated ann)
  {
    CandiManager cdi = _cdiManager;

    ProcessManagedBeanImpl<T> event
      = new ProcessManagedBeanImpl<T>(_cdiManager, bean, ann);
    
    BaseType baseType = cdi.createTargetBaseType(event.getClass());
    baseType = baseType.fill(cdi.createTargetBaseType(bean.getBeanClass()));

    _pendingEventList.add(new PendingEvent(event, baseType));

    /*
    getEventManager().fireExtensionEvent(event, baseType);

    if (event.isVeto())
      return null;
    else
      return event.getBean();
      */
    
    return event.getBean();
  }

  @Module
  public <T,X> Bean<X> processProducerMethod(ProducesMethodBean<T,X> bean)
  {
    CandiManager cdi = _cdiManager;
    
    ProcessProducerMethodImpl<T,X> event
      = new ProcessProducerMethodImpl<T,X>(_cdiManager, bean);
    
    AnnotatedMethod<? super T> method = bean.getProducesMethod();
    Bean<?> producerBean = bean.getProducerBean();
    
    BaseType baseType = cdi.createTargetBaseType(event.getClass());
    baseType = baseType.fill(cdi.createTargetBaseType(method.getBaseType()),
                             cdi.createTargetBaseType(producerBean.getBeanClass()));
    
    _pendingEventList.add(new PendingEvent(event, baseType));
    
    /*
    getEventManager().fireExtensionEvent(event, baseType);

    if (event.isVeto())
      return null;
    else
      return event.getBean();
      */
    
    return event.getBean();
  }

  @Module
  public <T,X> Bean<X> processProducerField(ProducesFieldBean<T,X> bean)
  {
    CandiManager cdi = _cdiManager;
    
    ProcessProducerFieldImpl<T,X> event
      = new ProcessProducerFieldImpl<T,X>(_cdiManager, bean);
    
    AnnotatedField<? super T> field = bean.getField();
    
    BaseType baseType = cdi.createTargetBaseType(event.getClass());
    baseType = baseType.fill(cdi.createTargetBaseType(field.getBaseType()),
                             cdi.createTargetBaseType(bean.getProducerBean().getBeanClass()));
    
    _pendingEventList.add(new PendingEvent(event, baseType));
    
    /*
    getEventManager().fireExtensionEvent(event, baseType);

    if (event.isVeto())
      return null;
    else
      return event.getBean();
      */
    
    return event.getBean();
  }
  
  public void processPendingEvents()
  {
    while (_pendingEventList.size() > 0) {
      PendingEvent event = _pendingEventList.remove(0);
      
      getEventManager().fireExtensionEvent(event.getEvent(), event.getType());
    }
  }

  /**
   * Processes the discovered InjectionTarget
   */
  public <T> InjectionTarget<T> 
  processInjectionTarget(InjectionTarget<T> target,
                         AnnotatedType<T> annotatedType)
  {
    CandiManager cdi = _cdiManager;
    
    ProcessInjectionTargetImpl<T> processTarget
      = new ProcessInjectionTargetImpl<T>(_cdiManager, target, annotatedType);
    
    BaseType eventType = cdi.createTargetBaseType(ProcessInjectionTargetImpl.class);
    eventType = eventType.fill(cdi.createTargetBaseType(annotatedType.getBaseType()));

    getEventManager().fireExtensionEvent(processTarget, eventType);

    return (InjectionTarget<T>) processTarget.getInjectionTarget();
  }

  /**
   * Processes the discovered method producer
   */
  public <X,T> Producer<T>
  processProducer(AnnotatedMethod<X> producesMethod,
                  Producer<T> producer)
  {
    CandiManager cdi = _cdiManager;
    
    ProcessProducerImpl<T,X> event
      = new ProcessProducerImpl<T,X>(producesMethod, producer);
    
    AnnotatedType<?> declaringType = producesMethod.getDeclaringType();
    
    Type declaringClass;
    
    if (declaringType != null)
      declaringClass = declaringType.getBaseType();
    else
      declaringClass = producesMethod.getJavaMember().getDeclaringClass(); 
    
    BaseType eventType = cdi.createTargetBaseType(ProcessProducerImpl.class);
    eventType = eventType.fill(cdi.createTargetBaseType(producesMethod.getBaseType()),
                               cdi.createTargetBaseType(declaringClass));

    getEventManager().fireExtensionEvent(event, eventType);
    
    return event.getProducer();
  }

  /**
   * Processes the discovered method producer
   */
  public <X,T> Producer<T>
  processProducer(AnnotatedField<X> producesField,
                  Producer<T> producer)
  {
    CandiManager cdi = _cdiManager;
    
    ProcessProducerImpl<T,X> event
      = new ProcessProducerImpl<T,X>(producesField, producer);
    
    AnnotatedType<X> declaringType = producesField.getDeclaringType();
    
    BaseType eventType = cdi.createTargetBaseType(ProcessProducerImpl.class);
    eventType = eventType.fill(cdi.createTargetBaseType(producesField.getBaseType()),
                               cdi.createTargetBaseType(declaringType.getBaseType()));

    getEventManager().fireExtensionEvent(event, eventType);
    
    return event.getProducer();
  }
  
  /**
   * Processes the observer.
   */
  public <T,X> void processObserver(ObserverMethod<T> observer,
                                    AnnotatedMethod<X> method)
  {
    ProcessObserverImpl<T,X> event
      = new ProcessObserverImpl<T,X>(_cdiManager, observer, method);
  
    AnnotatedMethod<X> annotatedMethod = event.getAnnotatedMethod();
    AnnotatedType<X> declaringType = annotatedMethod.getDeclaringType();
    ObserverMethod<T> observerMethod = event.getObserverMethod();
    Type observedType = observerMethod.getObservedType();
    
    BaseType eventType = _cdiManager.createTargetBaseType(ProcessObserverImpl.class);
    eventType = eventType.fill(_cdiManager.createTargetBaseType(observedType),
                               _cdiManager.createTargetBaseType(declaringType.getBaseType()));
    
    getEventManager().fireExtensionEvent(event, eventType);
  }

  public void fireBeforeBeanDiscovery()
  {
    getEventManager().fireExtensionEvent(new BeforeBeanDiscoveryImpl(_cdiManager));
  }

  public AfterTypeDiscovery fireAfterTypeDiscovery()
  {
    AfterTypeDiscoveryImpl event = new AfterTypeDiscoveryImpl(_cdiManager);

    getEventManager().fireExtensionEvent(event);

    return event;
  }

  public void fireAfterBeanDiscovery()
  {
    getEventManager().fireExtensionEvent(new AfterBeanDiscoveryImpl(_cdiManager));
  }

  public void fireAfterDeploymentValidation()
  {
    AfterDeploymentValidationImpl event
      = new AfterDeploymentValidationImpl(_cdiManager);
  
    getEventManager().fireExtensionEvent(event);

    /*
    if (event.getDeploymentProblem() != null)
      throw ConfigException.create(event.getDeploymentProblem());
      */
  }
  
  /**
   * Creates a discovered annotated type.
   */
  public <T> AnnotatedType<T> processAnnotatedType(AnnotatedType<T> type)
  {
    CandiManager cdi = _cdiManager;
    
    ProcessAnnotatedTypeImpl<T> processType
      = new ProcessAnnotatedTypeImpl<T>(type);

    BaseType baseType = cdi.createTargetBaseType(ProcessAnnotatedTypeImpl.class);
    baseType = baseType.fill(cdi.createTargetBaseType(type.getBaseType()));
    getEventManager().fireExtensionEvent(processType, baseType);

    if (processType.isVeto()) {
      return null;
    }
    
    type = processType.getAnnotatedType();

    return type;
  }

  public <T,X> InjectionPoint processInjectionPoint(InjectionPoint injectionPoint)
  {
    ProcessInjectionPointImpl<T,X> event
      = new ProcessInjectionPointImpl<T,X>(injectionPoint);

    CandiManager cdi = _cdiManager;

    BaseType baseType
      = cdi.createTargetBaseType(ProcessInjectionPointImpl.class);
    baseType = baseType.fill(cdi.createTargetBaseType(injectionPoint.getMember()
                                                                    .getDeclaringClass()),
                             cdi.createTargetBaseType(injectionPoint.getType()));

    getEventManager().fireExtensionEvent(event, baseType);

    return event.getInjectionPoint();
  }

  void beforeInvokeExtension(Extension extension, Object event) {
    if (event instanceof BeforeBeanDiscovery
        || event instanceof AfterTypeDiscovery)
      _currentExtension = extension;
  }

  void afterInvokeExtension(Extension extension) {
    if (_currentExtension == extension)
      _currentExtension = null;
  }

  public Extension getCurrentExtension()
  {
    return _currentExtension;
  }

  public AnnotatedType<?> processSyntheticAnnotatedType(Extension extention,
                                                        AnnotatedType<?> type)
  {
    ProcessSyntheticAnnotatedTypeImpl<?> event
      = new ProcessSyntheticAnnotatedTypeImpl<>(type,
                                                extention);

    BaseType baseType
      = _cdiManager.createTargetBaseType(ProcessSyntheticAnnotatedTypeImpl.class);

    baseType
      = baseType.fill(_cdiManager.createTargetBaseType(type.getBaseType()));

    getEventManager().fireExtensionEvent(event, baseType);

    if (event.isVeto())
      type = null;
    else
      type = event.getAnnotatedType();

    return type;
  }

  private EventManager getEventManager()
  {
    return _cdiManager.getEventManager();
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _cdiManager + "]";
  }

  private static final class MethodItem {
    String _name;
    Class []_parameters;

    private MethodItem(Method method)
    {
      _name = method.getName();
      _parameters = method.getParameterTypes();
    }

    @Override
    public boolean equals(Object o)
    {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      MethodItem method = (MethodItem) o;

      if (!_name.equals(method._name)) return false;
      if (!Arrays.equals(_parameters, method._parameters)) return false;

      return true;
    }

    @Override
    public int hashCode()
    {
      int result = _name.hashCode();
      result = 31 * result + Arrays.hashCode(_parameters);
      return result;
    }
  }

  class ExtensionItem {
    private ArrayList<ExtensionMethod> _observers
      = new ArrayList<ExtensionMethod>();

    ExtensionItem(Class<?> cl)
    {
      Set<MethodItem> items = new HashSet<>();

      while (Extension.class.isAssignableFrom(cl)) {
        for (Method method : cl.getDeclaredMethods()) {
          MethodItem methodItem = new MethodItem(method);

          if (items.contains(methodItem))
            continue;
          else
            items.add(methodItem);

          ExtensionMethod extMethod = bindObserver(cl, method);

          if (extMethod != null)
            _observers.add(extMethod);
        }
        cl = cl.getSuperclass();
      }
    }

    private ArrayList<ExtensionMethod> getExtensionMethods()
    {
      return _observers;
    }

    @SuppressWarnings("unchecked")
    private ExtensionMethod bindObserver(Class<?> cl, Method method)
    {
      Type []param = method.getGenericParameterTypes();

      if (param.length < 1)
        return null;

      Annotation [][]paramAnn = method.getParameterAnnotations();

      final int observedIndex = hasObserver(paramAnn);

      if (observedIndex == -1)
        return null;

/*
      if (! hasObserver(paramAnn))
        return null;
*/

      CandiManager inject = _cdiManager;

      BeanArg<?> []args = new BeanArg[param.length];

      WithAnnotations withAnnotations
        = getWithAnnotations(paramAnn[observedIndex]);

      for (int i = 1; i < param.length; i++) {
        Annotation []bindings = inject.getQualifiers(paramAnn[i]);

        if (bindings.length == 0)
          bindings = new Annotation[] { DefaultLiteral.DEFAULT };
        
        InjectionPoint ip = null;

        args[i] = new BeanArg(inject, param[i], bindings, ip);
      }

      BaseType baseType = inject.createTargetBaseType(param[0]);

      return new ExtensionMethod(method, baseType,
                                 inject.getQualifiers(paramAnn[0]),
                                 args,
                                 withAnnotations);
    }

    private int hasObserver(Annotation [][]paramAnn)
    {
      for (int i = 0; i < paramAnn.length; i++) {
        for (int j = 0; j < paramAnn[i].length; j++) {
          if (paramAnn[i][j].annotationType().equals(Observes.class))
            return i;
        }
      }

      return -1;
    }

    private WithAnnotations getWithAnnotations(Annotation[] annotations)
    {
      for (Annotation annotation : annotations) {
        if (WithAnnotations.class.isAssignableFrom(annotation.annotationType()))
          return (WithAnnotations) annotation;
      }

      return null;
    }
  }

  static class ExtensionMethod {
    private final Method _method;
    private final BaseType _type;
    private final Annotation []_qualifiers;
    private final BeanArg<?> []_args;
    private final WithAnnotations _withAnnotations;

    ExtensionMethod(Method method,
                    BaseType type,
                    Annotation []qualifiers,
                    BeanArg<?> []args,
                    WithAnnotations withAnnotations)
    {
      _method = method;
      method.setAccessible(true);
      
      _type = type;
      _qualifiers = qualifiers;
      _args = args;
      _withAnnotations = withAnnotations;
    }

    public Method getMethod()
    {
      return _method;
    }

    public BeanArg<?> []getArgs()
    {
      return _args;
    }

    public BaseType getBaseType()
    {
      return _type;
    }

    public Annotation []getQualifiers()
    {
      return _qualifiers;
    }
    
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _method + "]";
    }

    public WithAnnotations getWithAnnotations()
    {
      return _withAnnotations;
    }
  }

  public class ExtensionObserver extends ObserverMethodBase<Object> {
    private Extension _extension;
    private Method _method;
    private BeanArg<?> []_args;
    private WithAnnotations _withAnnotations;

    ExtensionObserver(Extension extension,
                      Method method,
                      BeanArg<?> []args,
                      WithAnnotations withAnnotation)
    {
      _extension = extension;
      _method = method;
      _args = args;
      _withAnnotations = withAnnotation;
    }

    public void notify(Object event, InjectionPoint injectionPoint)
    {
      try {
        Object []args = new Object[_args.length];
        args[0] = event;

        for (int i = 1; i < args.length; i++) {
          if (EventMetadata.class.equals(_args[i].getType())) {
            args[i] = new EventMetadataImpl(new HashSet<Annotation>(),
                                            null,
                                            event.getClass());
          }
          else {
            args[i] = _args[i].eval(null);
          }
        }

        beforeInvokeExtension(_extension, event);

        _method.invoke(_extension, args);
      } catch (RuntimeException e) {
        throw e;
      } catch (InvocationTargetException e) {
        String loc = (_extension + "." + _method.getName() + ": ");
        
        Throwable cause = e.getCause();

        if (cause instanceof ConfigException)
          throw (ConfigException) cause;
        
        throw new InjectionException(loc + cause.getMessage(), cause);
      } catch (Exception e) {
        String loc = (_extension + "." + _method.getName() + ": ");

        throw new InjectionException(loc + e.getMessage(), e);
      } finally {
        afterInvokeExtension(_extension);
      }
    }

    public String toString()
    {
      return getClass().getSimpleName() + "[" + _extension + "," + _method.getName() + "]";
    }
  }
  
  static class PendingEvent {
    private Object _event;
    private BaseType _type;
    
    PendingEvent(Object event, BaseType type)
    {
      _event = event;
      _type = type;
    }
    
    public Object getEvent()
    {
      return _event;
    }
    
    public BaseType getType()
    {
      return _type;
    }
  }
}

