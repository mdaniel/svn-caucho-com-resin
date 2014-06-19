package com.caucho.resin.admin.service;

import java.io.Serializable;
import java.util.Comparator;

public class User implements Serializable {
  private long _id;
  private String _email;
  private String _firstName;
  private String _lastName;

  public User()
  {
  }

  public User(long id, String email, String firstName, String lastName)
  {
    _id = id;
    _email = email;
    _firstName = firstName;
    _lastName = lastName;
  }

  public long getId() { return _id; }
  public String getEmail() { return _email; }
  public String getFirstName() { return _firstName; }
  public String getLastName() { return _lastName; }

  public static Comparator<User> EmployeeNameComparator = new Comparator<User>() {
    public int compare(User e1, User e2)
    {
      return 0;
    }
  };
}