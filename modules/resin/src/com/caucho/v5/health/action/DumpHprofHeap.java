/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.action;

import io.baratine.config.Configurable;
import io.baratine.service.Startup;

import javax.inject.Singleton;

/**
 * Health action to create a HPROF format heap dump.  The heap 
 * dump will be written to the Resin log directory as heap.hprof.  The dump 
 * file location and name can be changed using hprof-path or hprof-path-format. 
 * 
 * <p>
 * <pre>{@code
 * <health:HttpStatusHealthCheck ee:Named="httpStatusCheck">
 *   <url>http://localhost:8080/test-ping.jsp</url>
 * </health:HttpStatusHealthCheck>
 * 
 * <health:DumpHprofHeap>
 *   <health:IfHealthCritical healthCheck="${httpStatusCheck}"/>
 *   <health:IfRechecked/>
 * </health:DumpHprofHeap>
 * }</pre>
 */
@Startup
@Singleton
@Configurable
public class DumpHprofHeap extends DumpHeap
{
  public DumpHprofHeap()
  {
    setHprof(true);
  }
}
