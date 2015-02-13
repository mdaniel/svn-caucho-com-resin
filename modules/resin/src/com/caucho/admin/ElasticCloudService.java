/*
 * Copyright (c) 1998-2014 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.admin;

import io.baratine.core.Startup;

import javax.inject.Singleton;

/**
 * <p>Enables dynamic servers. When dynamic servers are enabled a new Resin
 * server can be started from the command line. The new server will join
 * an existing cluster.
 * 
 * <p>The new dynamic server will join the cluster defined by --cluster on
 * the command line, or the &lt;home-cluster> in the resin.xml
 * 
 * <code><pre>
 * unix> resinctl start --cluster app --elastic
 * </pre></code>
 */
@Startup
@Singleton
public class ElasticCloudService
  implements ElasticCloudServiceMarker
{
}