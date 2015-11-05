package org.springframework.instrument.classloading;

public interface LoadTimeWeaver
{
  void addTransformer(ClassFileTransformer transformer);

  ClassLoader getInstrumentableClassLoader();

  ClassLoader getThrowawayClassLoader();
}