package com.caucho.servlets;

public class EmployeeServiceImpl implements EmployeeService{

    @Override
    public void addEmployee(Employee emp, int salary, float rate, Integer rank,
            String description) {
        System.out.printf("%s %s  %s %s\n", emp, salary, rate, rank);
        System.out.println("Hello WORLD from Servlet !!!!!!!!!!!");
        
    }

}
