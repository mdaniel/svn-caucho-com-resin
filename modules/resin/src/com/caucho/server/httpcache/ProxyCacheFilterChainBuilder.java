/*
 * @author Scott Ferguson
 */

package com.caucho.server.httpcache;

import javax.servlet.FilterChain;

import com.caucho.server.dispatch.Invocation;

/**
 * Creates a cache filter based on the invocation.
 */
public class ProxyCacheFilterChainBuilder {
  /**
   * Builds the next item in the chain.
   *
   * @param next the following filter chain
   * @param invocation the invocation for the chain builder.
   */
  public FilterChain build(FilterChain next, Invocation invocation)
  {
    return new ProxyCacheFilterChain(next, invocation.getWebApp());
  }
}
