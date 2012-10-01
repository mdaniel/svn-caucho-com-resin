package com.caucho.servlets;

public interface EmployeeService {
	void addEmployee(Employee emp, int salary, float rate, Integer rank, String description);
}
