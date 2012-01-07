/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */
package com.caucho.env.health;

/**
 * <pre>
 * UNKNOWN:     Health check has not yet executed or failed to execute properly; status is inconclusive.
 * OK:          Health check reported healthy status.  This does not imply recovery.
 * WARNING:     Health check reported warning threshold reached or critical is possible.
 * CRITICAL:    Health check reported critical status; action should be taken.
 * FATAL:       Health check reported fatal; restart expected.
 * </pre>
 */
public enum HealthStatus
{
  UNKNOWN,
  OK,
  WARNING,
  CRITICAL,
  FATAL,
}
