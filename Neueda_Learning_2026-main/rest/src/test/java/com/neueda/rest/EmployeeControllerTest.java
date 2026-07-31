package com.neueda.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmpController.class)
public class EmployeeControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    EmpService empService;

    @Test
    void shouldReturnAllEmployee() throws Exception {
        List<Employee> list = List.of(new Employee("John", "Doe", 30));
        when(empService.getAllEmployees()).thenReturn(list);

        mockMvc.perform(get("/api/v1/employees/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("John"));
    }
}
