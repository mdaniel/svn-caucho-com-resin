package com.caucho.admin.thread;

import java.util.*;

import com.caucho.admin.thread.filter.*;

public class DatabaseThreadActivityReport extends AbstractThreadActivityReport
{
  public static enum ThreadActivityCode
  {
    SELECT('S'),
    INSERT('I'),
    UPDATE('U'),
    DELETE('D'),
    IDLE('.');

    final char _char;
    
    private ThreadActivityCode(char c)
    {
      _char = c;
    }
    
    public char getChar()
    {
      return _char;
    }
  }  
  
  private Map<ThreadSnapshotFilter, ThreadActivityCode> _codes = new 
    LinkedHashMap<ThreadSnapshotFilter, ThreadActivityCode>();
  
  private Map<Character, String> _key = new HashMap<Character, String>();
  
  public DatabaseThreadActivityReport()
  {
    // add others
    _codes.put(new IdlePoolFilter(), ThreadActivityCode.IDLE);

    // create better descriptions?
    for(ThreadActivityCode code : ThreadActivityCode.values()) {
      _key.put(code.getChar(), code.toString());
    }
    
    _key = Collections.unmodifiableMap(_key);    
  }
  
  public Map<Character, String> getScoreboardKey()
  {
    return _key;
  }
  
  protected boolean assignActivityCode(ThreadSnapshot thread)
  {
    for(Map.Entry<ThreadSnapshotFilter, ThreadActivityCode> entry 
      : _codes.entrySet()) {
      if (entry.getKey().isMatch(thread)) {
        thread.setCode(entry.getValue().getChar());
        return true;
      }
    }
    
    return false;
  }
  
  protected ThreadActivityGroup[] createGroups()
  {
    List<ThreadActivityGroup> groups = new ArrayList<ThreadActivityGroup>();
    
    // create a group or each database connection pool
    
    ThreadActivityGroup []array = new ThreadActivityGroup[groups.size()];
    groups.toArray(array);
    
    return array;
  }
}