package com.caucho.amp;

public interface SkeletonServiceInvoker {

    public abstract Message invokeMessage(Object payload) throws Exception;

}