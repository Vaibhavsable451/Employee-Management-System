package com.vaibhav.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vaibhav.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestParam;

import com.vaibhav.exception.ResourceNotFoundException;
import com.vaibhav.model.Employee;
import com.vaibhav.repository.EmployeeRepository;
@CrossOrigin(origins = "${frontend.url}")
@RestController
@RequestMapping("api/v1/employees")
public class EmployeeController {
  
	@Autowired
	private EmployeeRepository employeeRepository;
    @Autowired
    private GeminiService geminiService;
    @Value("${frontend.url}")
    private String frontendUrl;
	
	@GetMapping
	public List<Employee> getAllEmployees(){
		return employeeRepository.findAll();
		
	}
	
	 // build create employee REST API
    @PostMapping
    public Employee createEmployee(@RequestBody Employee employee) {
        return employeeRepository.save(employee);
    }
    // build get employee by id REST API
    @GetMapping("{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable  long id){
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not exist with id:" + id));
        return ResponseEntity.ok(employee);
    }

    // build update employee REST API
    @PutMapping("{id}")
    public ResponseEntity<Employee> updateEmployee(@PathVariable long id,@RequestBody Employee employeeDetails) {
        Employee updateEmployee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not exist with id: " + id));

        updateEmployee.setFirstName(employeeDetails.getFirstName());
        updateEmployee.setLastName(employeeDetails.getLastName());
        updateEmployee.setEmailId(employeeDetails.getEmailId());

        employeeRepository.save(updateEmployee);

        return ResponseEntity.ok(updateEmployee);
    }

    // build delete employee REST API
    @DeleteMapping("{id}")
    public ResponseEntity<HttpStatus> deleteEmployee(@PathVariable long id){

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not exist with id: " + id));

        employeeRepository.delete(employee);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);

    }
    @PostMapping("/agent/query")
    public ResponseEntity<?> chatWithGemini(@RequestBody Map<String, String> body) {
        String query = body.get("query");

        // Gemini will handle both SQL generation and execution
        Object result = geminiService.askGeminiAndExecute(query);

        return ResponseEntity.ok(Map.of("response", result));
    }

    // Search endpoint for AgentX or frontend widgets
    // Example: GET /api/v1/employees/search?q=vaibhav
    @GetMapping("/search")
    public ResponseEntity<?> searchEmployees(@RequestParam(name = "q", required = false) String q) {
        if (q == null || q.trim().isEmpty()) {
            return ResponseEntity.ok(employeeRepository.findAll());
        }

        String term = q.trim().toLowerCase();

        // 1️⃣ Total number of employees
        if (term.equals("count")) {
            long count = employeeRepository.count();
            return ResponseEntity.ok(count);
        }

        // 2️⃣ Count employees by first name: countByFirstName:John
        if (term.startsWith("countbyfirstname:")) {
            String firstName = term.split(":")[1];
            long count = employeeRepository.countByFirstNameIgnoreCase(firstName);
            return ResponseEntity.ok(count);
        }

        // 3️⃣ Employee by specific ID
        if (term.matches("\\d+")) {
            long id = Long.parseLong(term);
            Optional<Employee> employeeOpt = employeeRepository.findById(id);

            return employeeOpt.<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("response", "No employee found with ID: " + id)));
        }


        // 4️⃣ Employees whose first name starts with letter: firstNameStarts:V
        if (term.startsWith("firstnamestarts:")) {
            String prefix = term.split(":")[1];
            List<Employee> list = employeeRepository.findByFirstNameStartingWithIgnoreCase(prefix);
            return list.isEmpty()
                    ? ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("response", "No employees found starting with " + prefix))
                    : ResponseEntity.ok(list);
        }

        // 5️⃣ Employees with Gmail emails
        if (term.equals("emailcontains:gmail.com")) {
            List<Employee> list = employeeRepository.findByEmailIdContainingIgnoreCase("gmail.com");
            return list.isEmpty()
                    ? ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("response", "No Gmail employees found"))
                    : ResponseEntity.ok(list);
        }

        // 6️⃣ Employees where ID greater than X (idGreater:10)
        if (term.startsWith("idgreater:")) {
            long idVal = Long.parseLong(term.split(":")[1]);
            List<Employee> list = employeeRepository.findByIdGreaterThan(idVal);
            return list.isEmpty()
                    ? ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("response", "No employees with ID > " + idVal))
                    : ResponseEntity.ok(list);
        }

        // 7️⃣ Employee IDs and names only
        if (term.equals("display employee ids and names only")) {
            List<Employee> list = employeeRepository.findAll();
            List<Map<String, Object>> simplified = list.stream()
                    .map(emp -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", emp.getId());
                        map.put("firstName", emp.getFirstName());
                        map.put("lastName", emp.getLastName());
                        return map;
                    })
                    .collect(Collectors.toList());
            return simplified.isEmpty()
                    ? ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("response", "No employee data found"))
                    : ResponseEntity.ok(simplified);
        }

        // 8️⃣ Highest ID employee(s)
        if (term.equals("highestid")) {
            Optional<Long> maxIdOpt = employeeRepository.findAll().stream()
                    .map(Employee::getId)
                    .max(Long::compare);

            if (maxIdOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("response", "No employee data found"));
            }

            Long maxId = maxIdOpt.get();
            Optional<Employee> optionalEmp = employeeRepository.findById(maxId);
            List<Employee> list = optionalEmp.map(List::of).orElse(List.of());
            return ResponseEntity.ok(list);
        }
        // 9️⃣ Default: try normal search
        List<Employee> exactEmailMatch = employeeRepository.findByEmailIdIgnoreCase(term);
        if (!exactEmailMatch.isEmpty()) return ResponseEntity.ok(exactEmailMatch);

        List<Employee> partialMatches = employeeRepository.searchDistinct(term);
        if (partialMatches.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("response", "No matching employees found for: " + term));
        }

        return ResponseEntity.ok(partialMatches);
    }

    @GetMapping("/chat")
    public ResponseEntity<?> handleAiQuery(@RequestParam String query) {
    	String q = query == null ? "" : query.toLowerCase().trim();

    	// Normalize punctuation/whitespace
    	q = q
    		.replaceAll("[“”]", "\"")
    		.replaceAll("[’']", "'")
    		.replaceAll("[^a-z0-9@._:\\-\\s\"]", " ")
    		.replaceAll("\\s+", " ")
    		.trim();

    	// 1) Show all employees
    	if (q.matches("(?s).*(show|display).*(all ).*(employee|employees).*")
    		|| q.equals("select from employees")
    		|| q.equals("all employees")) {
    		return ResponseEntity.ok(employeeRepository.findAll());
    	}

    	// 2) Employee by specific ID
    	java.util.regex.Matcher idMatcher =
    		java.util.regex.Pattern.compile(".*\\bid\\b[^0-9]*([0-9]+).*").matcher(q);
    	if (idMatcher.matches()) {
    		long id = Long.parseLong(idMatcher.group(1));
    		return employeeRepository.findById(id)
    			.<ResponseEntity<?>>map(ResponseEntity::ok)
    			.orElse(ResponseEntity.ok("No employee found with ID " + id));
    	}

    	// 3) First name equals
    	java.util.regex.Matcher fnEq =
    		java.util.regex.Pattern.compile(".*first name\\s*\"?([a-z]+)\"?.*").matcher(q);
    	if (fnEq.matches()) {
    		String name = fnEq.group(1);
    		return ResponseEntity.ok(employeeRepository.findByFirstNameIgnoreCase(cap(name)));
    	}

    	// 4) Last name equals
    	java.util.regex.Matcher lnEq =
    		java.util.regex.Pattern.compile(".*last name\\s*\"?([a-z]+)\"?.*").matcher(q);
    	if (lnEq.matches()) {
    		String name = lnEq.group(1);
    		return ResponseEntity.ok(employeeRepository.findByLastNameIgnoreCase(cap(name)));
    	}

    	// 5) Unique/distinct emails
    	if (q.contains("unique email") || q.contains("distinct email")) {
    		return ResponseEntity.ok(employeeRepository.findDistinctEmailIds());
    	}

    	// 6) Count employees with first name “X”
    	java.util.regex.Matcher countFn =
    		java.util.regex.Pattern.compile(".*(count|how many).*first name\\s*\"?([a-z]+)\"?.*").matcher(q);
    	if (countFn.matches()) {
    		String name = countFn.group(2);
    		long count = employeeRepository.countByFirstNameIgnoreCase(cap(name));
    		return ResponseEntity.ok("Employees named " + cap(name) + ": " + count);
    	}

    	// 7) Email contains gmail.com
    	if (q.contains("email") && q.contains("contains") && q.contains("gmail.com")) {
    		return ResponseEntity.ok(employeeRepository.findByEmailIdContainingIgnoreCase("gmail.com"));
    	}

    	// 8) Display IDs and names only (return array of strings)
    	if (q.contains("ids and names") || q.contains("id and name")) {
    		java.util.List<String> idAndNames = employeeRepository.findAll().stream()
    			.map(emp -> emp.getId() + " | " + emp.getFirstName() + " " + emp.getLastName())
    			.toList();
    		return ResponseEntity.ok(idAndNames);
    	}

    	// 9) Sort by first name ascending
    	if (q.contains("sort") && q.contains("first name") && q.contains("ascending")) {
    		return ResponseEntity.ok(employeeRepository.findAllByOrderByFirstNameAsc());
    	}

    	// 10) ID greater than N
    	java.util.regex.Matcher gt =
    		java.util.regex.Pattern.compile(".*id.*(>|greater than)\\s*([0-9]+).*").matcher(q);
    	if (gt.matches()) {
    		long num = Long.parseLong(gt.group(2));
    		java.util.List<Employee> list = employeeRepository.findByIdGreaterThan(num);
    		return list.isEmpty()
    			? ResponseEntity.ok("No employees found with ID greater than " + num)
    			: ResponseEntity.ok(list);
    	}

    	// 11) Duplicate emails (flat list simplest for UI)
    	if (q.contains("duplicate email")) {
    		java.util.List<String> dupEmails = employeeRepository.findDuplicateEmailValues();
    		return dupEmails.isEmpty() ? ResponseEntity.ok("No duplicate emails found.") : ResponseEntity.ok(dupEmails);
    	}

    	// 12) Total number of employees
    	if (q.contains("total number of employees") || (q.contains("count") && q.contains("employees"))) {
    		long count = employeeRepository.count();
    		return ResponseEntity.ok("Total employees: " + count);
    	}

    	// 13) First name starts with “V”
    	java.util.regex.Matcher starts =
    		java.util.regex.Pattern.compile(".*first name starts with\\s*\"?([a-z])\"?.*").matcher(q);
    	if (starts.matches()) {
    		String letter = starts.group(1);
    		return ResponseEntity.ok(employeeRepository.findByFirstNameStartingWithIgnoreCase(letter));
    	}

    	// 14) Last name not “Sable”
    	java.util.regex.Matcher notLast =
    		java.util.regex.Pattern.compile(".*last name (?:is )?not\\s*\"?([a-z]+)\"?.*").matcher(q);
    	if (notLast.matches()) {
    		String name = notLast.group(1);
    		return ResponseEntity.ok(employeeRepository.findByLastNameNotIgnoreCase(cap(name)));
    	}

    	// 15) Highest id
    	if (q.contains("highest id") || q.contains("max id") || q.contains("maximum id")) {
    		java.util.List<Employee> top = employeeRepository.findEmployeesWithMaxId();
    		if (!top.isEmpty()) return ResponseEntity.ok(top);
    		Long maxId = employeeRepository.findMaxId();
    		return (maxId == null)
    			? ResponseEntity.ok("No employees found.")
    			: employeeRepository.findById(maxId).<ResponseEntity<?>>map(ResponseEntity::ok)
    				.orElse(ResponseEntity.ok("No employees found."));
    	}

    	// Fallback: fuzzy search
    	java.util.List<Employee> matches = employeeRepository.searchDistinct(q);
    	if (!matches.isEmpty()) return ResponseEntity.ok(matches);

    	return ResponseEntity.ok("Sorry, I didn’t understand your query: " + query);
    }

    private static String cap(String s) {
    	if (s == null || s.isEmpty()) return s;
    	return s.substring(0, 1).toUpperCase() + s.substring(1);
    }}