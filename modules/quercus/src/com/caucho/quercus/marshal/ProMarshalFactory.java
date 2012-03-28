/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.marshal;

import com.caucho.bytecode.CodeWriterAttribute;
import com.caucho.quercus.env.*;
import com.caucho.quercus.module.ModuleContext;
import com.caucho.quercus.lib.file.BinaryInput;
import com.caucho.quercus.lib.file.BinaryOutput;
import com.caucho.quercus.lib.regexp.Ereg;
import com.caucho.quercus.lib.regexp.Eregi;
import com.caucho.quercus.lib.regexp.Regexp;
import com.caucho.quercus.lib.regexp.UnicodeEreg;
import com.caucho.quercus.lib.regexp.UnicodeEregi;
import com.caucho.quercus.program.JavaClassDef;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Code for marshaling (PHP to Java) and unmarshaling (Java to PHP) arguments.
 */
public class ProMarshalFactory extends MarshalFactory {
  private static final L10N L = new L10N(ProMarshalFactory.class);
  
  private static final HashMap<Class<?>,Marshal> _marshalMap
    = new HashMap<Class<?>,Marshal>();

  public ProMarshalFactory(ModuleContext moduleContext)
  {
    super(moduleContext);
  }

  public Marshal create(Class argType,
                        boolean isNotNull,
                        boolean isNullAsFalse)
  {
    Marshal marshal = _marshalMap.get(argType);

    // optimized cases, new types should be added to JavaMarshall

    if (marshal != null) {
      
    }
    else if (String.class.equals(argType)) {
      marshal = ProStringMarshal.MARSHAL;
    }
    else if (boolean.class.equals(argType)) {
      marshal = ProBooleanMarshal.MARSHAL;
    }
    else if (Boolean.class.equals(argType)) {
      marshal = ProBooleanObjectMarshal.MARSHAL;
    }
    else if (byte.class.equals(argType)) {
      marshal = ProByteMarshal.MARSHAL;
    }
    else if (Byte.class.equals(argType)) {
      marshal = ProByteObjectMarshal.MARSHAL;
    }
    else if (short.class.equals(argType)) {
      marshal = ProShortMarshal.MARSHAL;
    }
    else if (Short.class.equals(argType)) {
      marshal = ProShortObjectMarshal.MARSHAL;
    }
    else if (int.class.equals(argType)) {
      marshal = ProIntegerMarshal.MARSHAL;
    }
    else if (Integer.class.equals(argType)) {
      marshal = ProIntegerObjectMarshal.MARSHAL;
    }
    else if (long.class.equals(argType)) {
      marshal = ProLongMarshal.MARSHAL;
    }
    else if (Long.class.equals(argType)) {
      marshal = ProLongObjectMarshal.MARSHAL;
    }
    else if (LongValue.class.equals(argType)) {
      marshal = ProLongValueMarshal.MARSHAL;
    }
    else if (float.class.equals(argType)) {
      marshal = ProFloatMarshal.MARSHAL;
    }
    else if (Float.class.equals(argType)) {
      marshal = ProFloatObjectMarshal.MARSHAL;
    }
    else if (double.class.equals(argType)) {
      marshal = ProDoubleMarshal.MARSHAL;
    }
    else if (Double.class.equals(argType)) {
      marshal = ProDoubleObjectMarshal.MARSHAL;
    }
    else if (DoubleValue.class.equals(argType)) {
      marshal = ProDoubleValueMarshal.MARSHAL;
    }
    else if (BigDecimal.class.equals(argType)) {
      marshal = ProBigDecimalMarshal.MARSHAL;
    }
    else if (BigInteger.class.equals(argType)) {
      marshal = ProBigIntegerMarshal.MARSHAL;
    }
    else if (char.class.equals(argType)) {
      marshal = ProCharacterMarshal.MARSHAL;
    }
    else if (Character.class.equals(argType)) {
      marshal = ProCharacterObjectMarshal.MARSHAL;
    }
    else if (Path.class.equals(argType)) {
      marshal = ProPathMarshal.MARSHAL;
    }
    else if (Regexp.class.equals(argType)) {
      marshal = ProRegexpMarshal.MARSHAL;
    }
    else if (Regexp[].class.equals(argType)) {
      marshal = ProRegexpArrayMarshal.MARSHAL;
    }
    else if (Ereg.class.equals(argType)) {
      marshal = ProEregMarshal.MARSHAL;
    }
    else if (Eregi.class.equals(argType)) {
      marshal = ProEregiMarshal.MARSHAL;
    }
    else if (UnicodeEreg.class.equals(argType)) {
      marshal = ProUnicodeEregMarshal.MARSHAL;
    }
    else if (UnicodeEregi.class.equals(argType)) {
      marshal = ProUnicodeEregiMarshal.MARSHAL;
    }
    else if (Callable.class.equals(argType)) {
      marshal = ProCallableMarshal.MARSHAL;
    }
    else if (StringValue.class.equals(argType)) {
      marshal = ProStringValueMarshal.MARSHAL;
    }
    else if (UnicodeValue.class.equals(argType)) {
      marshal = ProUnicodeValueMarshal.MARSHAL;
    }
    else if (BinaryValue.class.equals(argType)) {
      marshal = ProBinaryValueMarshal.MARSHAL;
    }
    else if (BinaryBuilderValue.class.equals(argType)) {
      marshal = ProBinaryValueMarshal.MARSHAL;
    }
    else if (InputStream.class.equals(argType)) {
      marshal = ProInputStreamMarshal.MARSHAL;
    }
    else if (BinaryInput.class.equals(argType)) {
      marshal = ProBinaryInputMarshal.MARSHAL;
    }
    else if (BinaryOutput.class.equals(argType)) {
      marshal = ProBinaryOutputMarshal.MARSHAL;
    }
    else if (ArrayValue.class.equals(argType)) {
      marshal = ProArrayValueMarshal.MARSHAL;
    }
    else if (Value.class.equals(argType)) {
      marshal = ProValueMarshal.MARSHAL;
    }
    else if (Value.class.isAssignableFrom(argType)) {
      marshal = new ProExtValueMarshal(argType);
    }
    else if (void.class.equals(argType)) {
      marshal = ProVoidMarshal.MARSHAL;
    }
    else if (Calendar.class.equals(argType)){
      marshal = ProCalendarMarshal.MARSHAL;
    }
    else if (Date.class.equals(argType)) {
      marshal = ProDateMarshal.MARSHAL;
    }
    else if (URL.class.equals(argType)) {
      marshal = ProURLMarshal.MARSHAL;
    }
    else if (byte[].class.equals(argType)) {
      marshal = ProJavaByteArrayMarshal.MARSHAL;
    }
    else if (char[].class.equals(argType)) {
      marshal = ProJavaCharacterArrayMarshal.MARSHAL;
    }
    else if (argType.isArray()) {
      marshal = ProJavaArrayMarshal.MARSHAL;
    }
    else if (Map.class.isAssignableFrom(argType)) {
      JavaClassDef javaDef
        = _moduleContext.getJavaClassDefinition(argType, argType.getName());

      marshal = new ProJavaMapMarshal(javaDef, isNotNull, isNullAsFalse);
    }
    else if (List.class.isAssignableFrom(argType)) {
      JavaClassDef javaDef
        = _moduleContext.getJavaClassDefinition(argType, argType.getName());

      marshal = new ProJavaListMarshal(javaDef, isNotNull, isNullAsFalse);
    }
    else if (Collection.class.isAssignableFrom(argType)) {
      JavaClassDef javaDef
        = _moduleContext.getJavaClassDefinition(argType, argType.getName());

      marshal = new ProJavaCollectionMarshal(javaDef, isNotNull, isNullAsFalse);
    }
    else if (Enum.class.isAssignableFrom(argType)) {
      marshal = new ProEnumMarshal(argType, isNotNull, isNullAsFalse);
    }
    else {
      JavaClassDef javaDef
        = _moduleContext.getJavaClassDefinition(argType, argType.getName());

      marshal = new ProJavaMarshal(javaDef, isNotNull, isNullAsFalse);
    }

    if (!isNullAsFalse)
      return marshal;
    else {
      if (Value.class.equals(argType)
          || Boolean.class.equals(argType)
          || Byte.class.equals(argType)
          || Short.class.equals(argType)
          || Integer.class.equals(argType)
          || Long.class.equals(argType)
          || Float.class.equals(argType)
          || Double.class.equals(argType)
          || Character.class.equals(argType)) {

        String shortName = argType.getSimpleName();
        throw new UnsupportedOperationException("@ReturnNullAsFalse cannot be used with return type `"+shortName+"'");
      }

      return new ProNullAsFalseMarshal(marshal);
    }
  }

  public Marshal createReference()
  {
    return ProReferenceMarshal.MARSHAL;
  }

  public Marshal createValuePassThru()
  {
    return ProValueMarshal.MARSHAL_PASS_THRU;
  }
  
  public Marshal createExpectString()
  {
    return ProExpectMarshal.MARSHAL_EXPECT_STRING;
  }
  
  public Marshal createExpectNumeric()
  {
    return ProExpectMarshal.MARSHAL_EXPECT_NUMERIC;
  }
  
  public Marshal createExpectBoolean()
  {
    return ProExpectMarshal.MARSHAL_EXPECT_BOOLEAN;
  }

  public boolean isByteCodeGenerator()
  {
    return false;
  }

  public void generateMarshal(CodeWriterAttribute code, int argIndex)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void generateUnmarshal(CodeWriterAttribute code)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  static {
    _marshalMap.put(Class.class, ProClassMarshal.MARSHAL);
    _marshalMap.put(Callable.class, ProCallableMarshal.MARSHAL);
  }
}

