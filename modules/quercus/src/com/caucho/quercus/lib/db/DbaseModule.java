/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
 * @author Rodrigo Westrupp
 */

package com.caucho.quercus.lib.db;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.xBaseJ.DBF;
import org.xBaseJ.fields.Field;

import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.DoubleValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.Log;

public class DbaseModule extends AbstractQuercusModule
{
  private static final Logger log = Log.open(DbaseModule.class);

  public static Value dbase_open(Env env, String path, int mode) {
    try {
      DBF aDB = null;
      if (mode == 0) {
        // read only
        aDB = new DBF(path, DBF.READ_ONLY);
      } else if (mode == 2) {
        // read/write
        aDB = new DBF(path);
      } else {
        log.fine("Wrong mode for opening dbase file: " + mode);
        return BooleanValue.FALSE;
      }
      return env.wrapJava(aDB);
    } catch (Exception e) {
      log.log(Level.SEVERE, "Could not open DBase file " + path, e);
      return BooleanValue.FALSE;
    }
  }

  public static Value dbase_get_record_with_names(Env env,
      Value dbaseIdentifier, int recordNumber) {
    try {
      DBF aDB = (DBF) dbaseIdentifier.toJavaObject(env, DBF.class);
      ArrayValue result = new ArrayValueImpl();
      aDB.gotoRecord(recordNumber);
      for (int i = 1; i <= aDB.getFieldCount(); i++) {
        Field field = aDB.getField(i);
        Value value;
        if (field.isNumField()) {
          value = LongValue.create(Long.parseLong(field.get().trim()));
        } else if (field.isFloatField()) {
          value = DoubleValue.create(Double.parseDouble(field.get().trim()));
        } else {
          value = env.createString(field.get());  
        }
        result.put(env.createString(field.getName()), value);
      }
      return result;
    } catch (Exception e) {
      log.log(Level.INFO, "Could not read DBase record", e);
      return BooleanValue.FALSE;
    }
  }

  public static Value dbase_close(Env env, Value dbaseIdentifier) {
    try {
      DBF aDB = (DBF) dbaseIdentifier.toJavaObject(env, DBF.class);
      aDB.close();
    } catch (Exception e) {
      return BooleanValue.FALSE;
    }
    return BooleanValue.TRUE;
  }
}
