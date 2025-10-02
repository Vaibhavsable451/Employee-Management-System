package com.vaibhav.repository;

import org.springframework.data.jpa.repository.JpaRepository;


import com.vaibhav.model.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
// all crud database methods
}
