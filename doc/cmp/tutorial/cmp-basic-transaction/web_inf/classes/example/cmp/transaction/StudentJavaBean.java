package example.cmp.transaction;

import java.util.*;

import java.security.*;

/**
 * Holds Student Information to be displayed on the presenation layer XTP page
 */
public class StudentJavaBean implements Principal {

  public StudentJavaBean(int id, String name, String gender)
  {
    this.setId(id);
    this.setName(name);
    this.setGender(gender);
  }

  public int getId()
  {
    return this.id;
  }

  public void setId(int id)
  {
    this.id = id;
  }

  public String getName()
  {
    return this.name;
  }

  public void setName(String name)
  {
    this.name = name;
  }

  public String getGender()
  {
    return this.gender;
  }

  public void setGender(String gender)
  {
    this.gender = gender;
  }

  public Collection getSelectedCourses()
  {
    return this.selectedCourses;
  }

  public void setSelectedCourses(Collection selectedCourses)
  {
    this.selectedCourses = selectedCourses;
  }

  public Collection getAvailableCourses()
  {
    return availableCourses;
  }

  public void setAvailableCourses(Collection availableCourses)
  {
    this.availableCourses = availableCourses;
  }

  public Collection getEnrolledCourses()
  {
    return enrolledCourses;
  }

  public void setEnrolledCourses(Collection enrolledCourses)
  {
    this.enrolledCourses = enrolledCourses;
  }

  private int id;
  private String name;
  private String gender;
  private Collection selectedCourses;
  private Collection enrolledCourses;
  private Collection availableCourses;
}
