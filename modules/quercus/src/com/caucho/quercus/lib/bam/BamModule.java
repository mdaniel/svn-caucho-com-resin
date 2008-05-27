/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Emil Ong
 */

package com.caucho.quercus.lib.bam;

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.annotation.ClassImplementation;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.module.IniDefinitions;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.IniDefinition;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.program.AbstractFunction;
import com.caucho.util.L10N;

import java.util.logging.Logger;

/**
 * BAM functions
 */
@ClassImplementation
public class BamModule extends AbstractQuercusModule
{
  private static final Logger log
    = Logger.getLogger(BamModule.class.getName());

  private static final L10N L = new L10N(BamModule.class);

  public static Value bam_dispatch(Env env)
  {
    Value eventTypeValue = env.getValue("_quercus_bam_event_type");
    BamEventType eventType = (BamEventType) eventTypeValue.toJavaObject();

    switch (eventType) {
      case MESSAGE: 
      {
        Value to = env.getValue("_quercus_bam_message_to");
        Value from = env.getValue("_quercus_bam_message_from");
        Value value = env.getValue("_quercus_bam_message_value");

        AbstractFunction function = null;
        
        if (value != null && value.toJavaObject() != null) {
          String functionName = 
            "bam_message_" + value.toJavaObject().getClass().getSimpleName();

          function = env.getFunction(functionName);

          if (function == null)
            function = env.getFunction("bam_message"); // generic handler
        }

        if (function == null) {
          log.fine("bam message handler function not found");

          return BooleanValue.FALSE;
        }

        return function.call(env, to, from, value);
      }

      case MESSAGE_ERROR:
        break;

      case QUERY_GET:
        break;

      case QUERY_SET:
        break;

      case QUERY_RESULT:
        break;

      case QUERY_ERROR:
        break;
    }

    return BooleanValue.FALSE;
  }
}

