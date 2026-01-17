package com.vaibhav.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import com.vaibhav.model.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
	@Query("SELECT DISTINCT e FROM Employee e " +
		       "WHERE LOWER(e.firstName) LIKE LOWER(CONCAT('%', :term, '%')) " +
		       "OR LOWER(e.lastName) LIKE LOWER(CONCAT('%', :term, '%')) " +
		       "OR LOWER(e.emailId) LIKE LOWER(CONCAT('%', :term, '%'))")
		List<Employee> searchDistinct(@Param("term") String term);

		List<Employee> findByEmailIdIgnoreCase(String emailId);

		// New helpers
		List<Employee> findByFirstNameIgnoreCase(String firstName);

		List<Employee> findByLastNameIgnoreCase(String lastName);

		@Query("SELECT DISTINCT e.emailId FROM Employee e WHERE e.emailId IS NOT NULL")
		List<String> findDistinctEmailIds();

		long countByFirstNameIgnoreCase(String firstName);

		List<Employee> findByEmailIdContainingIgnoreCase(String part);

		List<Employee> findAllByOrderByFirstNameAsc();

		List<Employee> findByIdGreaterThan(long id);

		List<Employee> findByFirstNameStartingWithIgnoreCase(String prefix);

		List<Employee> findByLastNameNotIgnoreCase(String lastName);

		// Duplicate email helpers
		@Query("SELECT e.emailId FROM Employee e WHERE e.emailId IS NOT NULL GROUP BY e.emailId HAVING COUNT(e.emailId) > 1")
		List<String> findDuplicateEmailValues();

		List<Employee> findByEmailIdIn(List<String> emails);

		// Highest ID helpers
		@Query("SELECT MAX(e.id) FROM Employee e")
		Long findMaxId();

		@Query("SELECT e FROM Employee e WHERE e.id = (SELECT MAX(e2.id) FROM Employee e2)")
		List<Employee> findEmployeesWithMaxId();
		
	}