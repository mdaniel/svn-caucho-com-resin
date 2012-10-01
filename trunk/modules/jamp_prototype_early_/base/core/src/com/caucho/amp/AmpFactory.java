package com.caucho.amp;

import com.caucho.encoder.Decoder;

public class AmpFactory {
    
    static AmpFactory factory = new AmpFactory();
    
    
    public static AmpFactory factory() {
        //Look up in service locator as soon as there are two. 
        return factory;
    }
    
    public SkeletonServiceInvoker createJampServerSkeleton(Class<?> clazz) {
        //Look this up with a service locator design pattern, just as soon as there are two.
        SkeletonServiceInvokerImpl invoker = new SkeletonServiceInvokerImpl();
        invoker.clazz = clazz;
        return invoker;
    }
    
    public SkeletonServiceInvoker createJampServerSkeleton(Object instance) {
        SkeletonServiceInvokerImpl invoker = new SkeletonServiceInvokerImpl();
        invoker.instance = instance;
        return invoker;
    }


    public SkeletonServiceInvoker createCustomServerSkeleton(Object instance, @SuppressWarnings("rawtypes") Decoder message, Decoder<Object,Object> arguments) {
        SkeletonServiceInvokerImpl invoker = new SkeletonServiceInvokerImpl();
        invoker.instance = instance;
        invoker.messageDecoder = message;
        invoker.argumentDecoder = arguments;
        return invoker;
    }

    public SkeletonServiceInvoker createCustomServerSkeleton(Class<?> clazz, @SuppressWarnings("rawtypes") Decoder message, Decoder<Object,Object> arguments) {
        SkeletonServiceInvokerImpl invoker = new SkeletonServiceInvokerImpl();
        invoker.clazz = clazz;
        invoker.messageDecoder = message;
        invoker.argumentDecoder = arguments;
        return invoker;
    }
    
}
