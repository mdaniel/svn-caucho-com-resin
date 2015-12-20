/*
 * @author Scott Ferguson
 */

package com.caucho.v5.http.cache;

import javax.servlet.FilterChain;

import com.caucho.v5.http.dispatch.InvocationServlet;

/**
 * Creates a cache filter based on the invocation.
 */
public class FilterChainHttpCacheBuilder {
  /**
   * Builds the next item in the chain.
   *
   * @param next the following filter chain
   * @param invocation the invocation for the chain builder.
   */
  public FilterChain build(FilterChain next, InvocationServlet invocation)
  {
    return new FilterChainHttpCache(next, invocation.getWebApp());
  }
}
