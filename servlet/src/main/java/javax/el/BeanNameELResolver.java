package javax.el;

import java.util.*;
import java.beans.FeatureDescriptor;

public class BeanNameELResolver extends ELResolver
{
  private final BeanNameResolver _beanNameResolver;
  
  public BeanNameELResolver(BeanNameResolver beanNameResolver)
  {
    _beanNameResolver = beanNameResolver;
  }

  @Override
  public Class<?> getCommonPropertyType(ELContext context, Object base)
  {
    return String.class;
  }

  @Override
  public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context,
                                                           Object base)
  {
    return null;
  }

  @Override
  public Class<?> getType(ELContext context, Object base, Object property)
  {
    Objects.requireNonNull(context);

    Object bean = getValue(context, base, property);
    if (bean == null)
      return null;
    return bean.getClass();
  }

  @Override
  public Object getValue(ELContext context, Object base, Object property)
    throws PropertyNotFoundException, ELException
  {
    Objects.requireNonNull(context);
    
    if (base != null || property == null)
      return null;

    if (! (property instanceof String))
      return null;

    String beanName = (String) property;

    if (beanName.isEmpty())
      return null;
    
    if (! _beanNameResolver.isNameResolved(beanName))
      return null;
    
    context.setPropertyResolved(base, property);
    
    return _beanNameResolver.getBean(beanName);
  }

  @Override
  public boolean isReadOnly(ELContext context, Object base, Object property)
    throws PropertyNotFoundException, ELException
  {
    Objects.requireNonNull(context);

    if (base != null || property == null)
      return false;

    if (! (property instanceof String))
      return false;

    String beanName = (String) property;

    if (beanName.isEmpty())
      return false;
    
    if (! _beanNameResolver.isNameResolved(beanName))
      return false;
    
    context.setPropertyResolved(true);
    
    return _beanNameResolver.isReadOnly(beanName);
  }

  @Override
  public void setValue(ELContext context, 
                       Object base, 
                       Object property,
                       Object value) 
    throws PropertyNotFoundException,
    PropertyNotWritableException, ELException
  {
    Objects.requireNonNull(context);
    
    if (base != null || property == null)
      return;

    if (! (property instanceof String))
      return;

    String beanName = (String) property;

    if (beanName.isEmpty())
      return;
    
    if (_beanNameResolver.isNameResolved(beanName) || 
        _beanNameResolver.canCreateBean(beanName)) {
      _beanNameResolver.setBeanValue(beanName, value);
      context.setPropertyResolved(base, property);
    }
  }
  
 }
