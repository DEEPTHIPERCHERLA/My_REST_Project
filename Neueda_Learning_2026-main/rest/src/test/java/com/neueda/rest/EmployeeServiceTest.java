package com.neueda.rest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
public class EmployeeServiceTest {
    // for service layer testing we will use Mock data / not use the actual database;
    //using mockito we will use mock data to test the service layer methods
    @InjectMocks //instead of @Autowired we will use @InjectMocks to inject the mock data into the service layer
    EmpService service; // mock data injected in service layer
    @Mock // mock data for repository layer
    EmpRepository repo;
    @Test
    void shouldReturnEmployee(){
        //mock data
        List<Employee> list= List.of(
                new Employee("John","Doe",50000),
                new Employee("Jane","Doe",80000),
                new Employee("Jane","Doe",00000)
        );
        when(repo.getAllEmployees()).thenReturn(list);
        List<Employee> result= service.getAllEmployees();
        assertEquals(3,result.size());
        //verify(repo).getAllEmployees();




    }
}
