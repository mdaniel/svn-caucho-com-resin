package org.springframework.instrument.classloading;

import java.lang.instrument.ClassFileTransformer;

public interface LoadTimeWeaver
{
  void addTransformer(ClassFileTransformer transformer);

  ClassLoader getInstrumentableClassLoader();

  ClassLoader getThrowawayClassLoader();
}