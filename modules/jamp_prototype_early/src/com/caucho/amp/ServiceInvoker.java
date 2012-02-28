package com.caucho.amp;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.caucho.encoder.Decoder;
import com.caucho.encoder.JSONDecoder;
import com.caucho.encoder.JampMessageDecoder;

/** Used to invoke services on the side that acts as the server side. */
public class ServiceInvoker {
    private Class<?> clazz;
    private Object instance;
    private static Decoder messageDecoder = new JampMessageDecoder();
    private static Decoder jsonDecoder = new JSONDecoder();
    
	public ServiceInvoker (Class<?> clazz) {
		this.clazz = clazz;
	}
	public ServiceInvoker (Object instance) {
	    this.instance = instance;  
	}
	
	private final Object thisObject() throws Exception {
	    if (instance==null) {
	        instance = clazz.newInstance();
	    }
	    return instance;
	}
	public void invokeMessage(Object payload) throws Exception {
	    Message message = (Message) messageDecoder.decodeObject(payload);
	    
	    jsonDecoder = new JSONDecoder();
	    List<Object> args = (List<Object>) jsonDecoder.decodeObject(message.getArgs());
	    
	    
	    Object thisObject = thisObject();
	    Method method = findMethod(message, args, thisObject);
	    Object [] parameters = coerceArgumentList(args, method.getParameterTypes());
	    
	    method.invoke(instance, parameters);
	    
	    System.out.println("THIS FAR " + method.getName());
	    
	}
    private Object[] coerceArgumentList(List<Object> args,
            Class<?>[] parameterTypes)  throws Exception  {
        
        List<Object> parameters = new ArrayList<Object>(parameterTypes.length);
        
        for (int index=0; index<parameterTypes.length; index++) {
            Object inputArgument = args.get(index);
            Class<?> paraType = parameterTypes[index];
            parameters.add(coerceArgument(inputArgument, paraType));
        }
        
        return parameters.toArray(new Object[parameterTypes.length]);
    }
    
    private Object coerceArgument(Object inputArgument, Class<?> paraType)  throws Exception {
        if (inputArgument instanceof Map && paraType.isAssignableFrom(Map.class)) {
            return new HashMap((Map)inputArgument);
        } else if (inputArgument instanceof Map) {
            return coerceObject((Map<String, Object>) inputArgument, paraType);
        } else if (paraType.isPrimitive()){
            if (paraType == boolean.class){
                return inputArgument;
            } else {
                Number number = (Number) inputArgument;
                if (paraType==int.class) {
                    return number.intValue();
                } else if (paraType==double.class){
                    return number.doubleValue();
                } else if (paraType==float.class){
                    return number.floatValue();
                } else if (paraType==short.class) {
                    return number.shortValue();
                } else if (paraType==byte.class) {
                    return number.byteValue();
                }
            }
        } else {
            return inputArgument;
        }
        return null;
    }
    
    private Object coerceObject(Map<String, Object> inputArgument, Class<?> paraType) throws Exception {
        Object instance = null;
        if (paraType.isInterface()) {
            String jclass = (String) inputArgument.get("java_class");
            instance = Class.forName(jclass).newInstance();
        } else {
            instance = paraType.newInstance();
        }
        
        Method[] setterMethods = getSetterMethods(paraType);
        for (Method m : setterMethods) {
            String propName = m.getName().substring(3);
            propName = propName.substring(0,1).toLowerCase() + propName.substring(1);
            Object value = inputArgument.get(propName);
            Class<?> type = m.getParameterTypes()[0];
            
            invokeSetterMethod(paraType, instance, m, value);
        }
        
        return instance;
    }
    private void invokeSetterMethod(Class<?> paraType, Object instance, Method m,
            Object value) throws IllegalAccessException,
            InvocationTargetException, Exception {
   
        Object coercedValue = null;
        try {
            coercedValue = coerceArgument(value, paraType);
            m.invoke(instance, new Object[]{coercedValue});
        }catch (Exception ex) {
            System.out.printf("Method name = %s, valueType=%s, corercedValueType=%s, value=%s, cvalue=%s \n", 
                    m.getName(), value == null ? "null": value.getClass(), coercedValue==null? "null" : coercedValue.getClass(), value, coercedValue);
            
            throw ex;
        }
    }
    
    private Method[] getSetterMethods(Class<?> paraType) {
        List<Method> setters = new ArrayList<Method>(12); 
        Method[] methods = paraType.getMethods();
        for (int index=0; index < methods.length; index++){
            Method m = methods[index];
            String name = m.getName() + "safe no null";
            if (m.getReturnType()==void.class && Modifier.isPublic(m.getModifiers()) && m.getParameterTypes().length==1 && name.startsWith("set")){
                setters.add(m);
            }
        }
        return setters.toArray(new Method[setters.size()]);
    }
    private Method findMethod(Message message, List<Object> args,
            Object thisObject) {
        Method method = null;
	    
        Method[] methods = thisObject.getClass().getMethods();
        
        System.out.println(message);

	    for (Method m : methods) {
	        if (m.getName().equals(message.getMethodName())){
	            if (m.getParameterTypes().length==args.size()) {
	                method = m;
	                break;
	            }
	        }
	    }
	    if (method == null) {
	        throw new IllegalStateException("Method for message not found");
	    }
	    return method;
    }
}
