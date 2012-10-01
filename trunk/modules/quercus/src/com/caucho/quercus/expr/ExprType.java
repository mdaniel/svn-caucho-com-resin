/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

/**
 * Analyzed types of expressions
 */
public enum ExprType
{
  INIT {
    @Override
    public ExprType withLong()
    {
      return LONG;
    }
    
    @Override
    public ExprType withDouble()
    {
      return DOUBLE;
    }
    
    @Override
    public ExprType withType(ExprType type)
    {
      if (type == INIT)
	return VALUE;
      else
        return type;
    }
    
    public String toString()
    {
      return "ExprType::INIT";
    }
  },
    
  LONG {
    @Override
    public boolean isLong()
    {
      return true;
    }
    
    @Override
    public boolean isDouble()
    {
      return true;
    }
    
    @Override
    public ExprType withLong()
    {
      return LONG;
    }
    
    @Override
    public ExprType withDouble()
    {
      return DOUBLE;
    }
    
    @Override
    public ExprType withType(ExprType type)
    {
      if (type == LONG)
        return LONG;
      else if (type == DOUBLE)
        return DOUBLE;
      else
        return VALUE;
    }
    
    public String toString()
    {
      return "ExprType::LONG";
    }
  },
    
  DOUBLE {
    @Override
    public boolean isDouble()
    {
      return true;
    }
    
    @Override
    public ExprType withLong()
    {
      return DOUBLE;
    }
    
    @Override
    public ExprType withDouble()
    {
      return DOUBLE;
    }
    
    @Override
    public ExprType withType(ExprType type)
    {
      if (type == LONG || type == DOUBLE)
	return DOUBLE;
      else
	return VALUE;
    }
    
    public String toString()
    {
      return "ExprType::DOUBLE";
    }
  },
  
  BOOLEAN {
    @Override
    public boolean isBoolean()
    {
      return true;
    }
    
    @Override
    public ExprType withBoolean()
    {
      return BOOLEAN;
    }
    
    @Override
    public ExprType withType(ExprType type)
    {
      if (type == BOOLEAN)
        return BOOLEAN;
      else
        return VALUE;
    }
    
    public String toString()
    {
      return "ExprType::BOOLEAN";
    }
  },
    
  STRING {
    @Override
    public boolean isString()
    {
      return true;
    }
    
    @Override
    public ExprType withString()
    {
      return STRING;
    }
    
    @Override
    public ExprType withType(ExprType type)
    {
      if (type == STRING)
        return STRING;
      else
        return VALUE;
    }
    
    public String toString()
    {
      return "ExprType::STRING";
    }
  },
  
  VALUE;

  public boolean isBoolean()
  {
    return false;
  }

  public boolean isLong()
  {
    return false;
  }

  public boolean isDouble()
  {
    return false;
  }
  
  public boolean isString()
  {
    return false;
  }

  public ExprType withBoolean()
  {
    return VALUE;
  }
  
  public ExprType withLong()
  {
    return VALUE;
  }

  public ExprType withDouble()
  {
    return VALUE;
  }
  
  public ExprType withString()
  {
    return VALUE;
  }

  public ExprType withType(ExprType type)
  {
    return VALUE;
  }
}

