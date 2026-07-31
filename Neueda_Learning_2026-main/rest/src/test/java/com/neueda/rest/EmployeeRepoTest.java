package com.neueda.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJdbcTest
@Import(EmpRepository.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class EmployeeRepoTest {
    @Autowired
    EmpRepository repo;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearEmployeeTable() {
        jdbcTemplate.update("DELETE FROM employee");
    }

    @Test
    void shouldReturnEmptyListWhenNoEmployeesExist() {
        List<Employee> employees = repo.getAllEmployees();
        assertNotNull(employees);
        assertTrue(employees.isEmpty());
    }

    @Test
    void shouldReturnAllEmployeesAfterSavingEmployees() {
        Employee first = new Employee("John", "Engineering", 50000);
        Employee second = new Employee("Jane", "Finance", 70000);

        repo.saveEmployee(first);
        repo.saveEmployee(second);

        List<Employee> employees = repo.getAllEmployees();
        assertEquals(2, employees.size());
        assertTrue(employees.stream().anyMatch(e -> "John".equals(e.getName())));
        assertTrue(employees.stream().anyMatch(e -> "Jane".equals(e.getName())));
    }

    @Test
    void shouldReturnEmployeeByIdWhenEmployeeExists() {
        String uniqueName = "emp-" + UUID.randomUUID();
        int id = insertEmployeeAndGetId(uniqueName, "IT", 65000);

        Employee employee = repo.getEmployeeById(id);

        assertEquals(uniqueName, employee.getName());
        assertEquals("IT", employee.getDepartment());
        assertEquals(65000, employee.getSalary());
    }

    @Test
    void shouldThrowWhenEmployeeByIdDoesNotExist() {
        assertThrows(EmployeeNotFoundException.class, () -> repo.getEmployeeById(999999));
    }

    @Test
    void shouldUpdateEmployeeWhenEmployeeExists() {
        String uniqueName = "emp-" + UUID.randomUUID();
        int id = insertEmployeeAndGetId(uniqueName, "Ops", 45000);

        Employee updated = new Employee("Updated Name", "HR", 90000);
        Employee result = repo.updateEmployee(id, updated);

        assertNotNull(result);
        Employee persisted = repo.getEmployeeById(id);
        assertEquals("Updated Name", persisted.getName());
        assertEquals("HR", persisted.getDepartment());
        assertEquals(90000, persisted.getSalary());
    }

    @Test
    void shouldReturnNullWhenUpdatingMissingEmployee() {
        Employee result = repo.updateEmployee(999999, new Employee("Ghost", "None", 1000));
        assertNull(result);
    }

    @Test
    void shouldDeleteEmployeeWhenEmployeeExists() {
        String uniqueName = "emp-" + UUID.randomUUID();
        int id = insertEmployeeAndGetId(uniqueName, "Admin", 35000);

        int result = repo.deleteEmployee(id);

        assertEquals(1, result);
        assertThrows(EmployeeNotFoundException.class, () -> repo.getEmployeeById(id));
    }

    @Test
    void shouldReturnZeroWhenDeletingMissingEmployee() {
        int result = repo.deleteEmployee(999999);
        assertEquals(0, result);
    }

    private int insertEmployeeAndGetId(String name, String department, double salary) {
        jdbcTemplate.update(
                "INSERT INTO employee (name, department, salary) VALUES (?, ?, ?)",
                name,
                department,
                salary
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM employee WHERE name = ? ORDER BY id DESC LIMIT 1",
                Integer.class,
                name
        );
    }
}
