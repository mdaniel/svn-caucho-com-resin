package java.sql;

public class SQLClientInfoException extends SQLException
{
  public SQLClientInfoException()
  {
  }
  
  public SQLClientInfoException(String msg, Throwable exn)
  {
    super(msg, exn);
  }
}
