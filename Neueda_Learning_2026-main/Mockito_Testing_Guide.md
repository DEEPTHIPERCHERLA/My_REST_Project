# Spring Boot Testing Guide
## (JdbcTemplate + JUnit + Mockito)

> uses **JdbcTemplate**.

------------------------------------------------------------------------

# Application Flow

``` text
Client (Browser/Postman)
        │
        ▼
EmployeeController
        │
        ▼
EmployeeService
        │
        ▼
EmployeeRepository
        │
        ▼
JdbcTemplate
        │
        ▼
MySQL Database
```

Each layer has a different responsibility, so we test each layer
differently.

  Layer        Purpose                     Tool
  ------------ --------------------------- -------------------------
  Repository   Test SQL and JdbcTemplate   `@DataJdbcTest`
  Service      Test business logic         Mockito
  Controller   Test REST APIs              `@WebMvcTest` + MockMvc

------------------------------------------------------------------------

# 1. Repository Testing (`@DataJdbcTest`)

## Goal

Test:

-   SQL Queries
-   JdbcTemplate
-   RowMapper
-   CRUD Operations

Do **NOT** use Mockito here because we want to execute the real SQL.

## Example Repository

``` java
@Repository
public class EmployeeRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Employee> getAllEmployees() {
        return jdbcTemplate.query(
                "SELECT * FROM employee",
                new EmployeeRowMapper());
    }
}
```

## Repository Test

``` java
@DataJdbcTest
@Import(EmployeeRepository.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EmployeeRepositoryTest {

    @Autowired
    EmployeeRepository repository;

    @Test
    void shouldReturnEmployees() {

        List<Employee> employees = repository.getAllEmployees();

        assertFalse(employees.isEmpty());

    }
}
```

## Flow

``` text
Repository Test
      │
      ▼
Repository
      │
      ▼
JdbcTemplate
      │
      ▼
MySQL Test Database
```

## Annotations Used in Repository Testing

### `@DataJdbcTest`

**Purpose:** Loads only the JDBC layer required for repository testing.

**Loads** - DataSource - JdbcTemplate - Transaction Manager - Repository
related configuration

**Does NOT Load** - Controller - Service - Security - Entire Spring Boot
application

**Why do we use it?**

Repository methods contain SQL queries. We want to test the actual SQL
using a real database, so `@DataJdbcTest` loads only the JDBC
components, making the test fast.

``` java
@DataJdbcTest
class EmployeeRepositoryTest {}
```

### `@Import`

**Purpose:** Adds beans to the Spring Test Context.

**Why?**

If Spring doesn't automatically discover your repository,
`@Import(EmployeeRepository.class)` tells Spring to create that bean.

``` java
@Import(EmployeeRepository.class)
```

### `@AutoConfigureTestDatabase`

**Purpose:** Controls whether Spring replaces your configured
datasource.

``` java
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
```

`Replace.NONE` means:

> Use my configured MySQL database. Don't replace it with H2.

### `@Autowired`

**Purpose:** Injects the real Repository bean created by Spring.

``` java
@Autowired
EmployeeRepository repository;
```

------------------------------------------------------------------------

# 2. Service Testing (Mockito)

## Goal

Test only the business logic.

Do **NOT** connect to the database.

Repository becomes a Fake Object.

## Mockito Annotations

### `@Mock`

Creates a fake Repository.

``` java
@Mock
EmployeeRepository repository;
```

### `@InjectMocks`

Creates the Service and injects the fake Repository.

``` java
@InjectMocks
EmployeeService service;
```

## Service Test

``` java
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    EmployeeRepository repository;

    @InjectMocks
    EmployeeService service;

    @Test
    void shouldReturnEmployees() {

        List<Employee> list = List.of(
                new Employee(1, "John", "IT", 50000),
                new Employee(2, "David", "HR", 60000)
        );

        when(repository.getAllEmployees())
                .thenReturn(list);

        List<Employee> result = service.getAllEmployees();

        assertEquals(2, result.size());

        verify(repository).getAllEmployees();
    }
}
```

## Mockito Methods

### when()

``` java
when(repository.getAllEmployees())
        .thenReturn(list);
```

Meaning:

> Whenever the service calls `repository.getAllEmployees()`, don't
> access the database. Return this fake list.

### verify()

``` java
verify(repository).getAllEmployees();
```

Meaning:

> Verify that the Service actually called the Repository.

## Flow

``` text
Service Test
      │
      ▼
Service
      │
      ▼
Fake Repository
      │
      ▼
Fake Data
```

## Annotations & Methods Used in Service Testing

### `@ExtendWith(MockitoExtension.class)`

Enables Mockito support in JUnit 5. Without it, `@Mock` and
`@InjectMocks` won't work.

``` java
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {}
```

### `@Mock`

Creates a fake dependency.

``` java
@Mock
EmployeeRepository repository;
```

Instead of calling the real Repository, Mockito creates a fake one.

### `@InjectMocks`

Creates the class under test and injects all mocked dependencies.

``` java
@InjectMocks
EmployeeService service;
```

### `when()`

Defines the fake behaviour.

``` java
when(repository.getAllEmployees()).thenReturn(list);
```

Meaning: Whenever the service calls the repository, return the fake list
instead of going to the database.

### `thenReturn()`

Specifies what value Mockito should return.

### `verify()`

Checks whether the mocked method was called.

``` java
verify(repository).getAllEmployees();
```

Useful for verifying interactions between Service and Repository.

------------------------------------------------------------------------

# 3. Controller Testing (`@WebMvcTest`)

## Goal

Test:

-   Request Mapping
-   HTTP Status
-   JSON Response
-   Path Variables
-   Request Body

Do **NOT** call the real Service.

## Controller Example

``` java
@RestController
@RequestMapping("/employees")
public class EmployeeController {

    @Autowired
    EmployeeService service;

    @GetMapping
    public List<Employee> getEmployees() {
        return service.getAllEmployees();
    }
}
```

## Controller Test

``` java
@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    EmployeeService service;

    @Test
    void shouldReturnEmployees() throws Exception {

        List<Employee> list = List.of(
                new Employee(1, "John", "IT", 50000)
        );

        when(service.getAllEmployees())
                .thenReturn(list);

        mockMvc.perform(get("/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("John"));
    }
}
```

## Required Static Imports

``` java
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
```

## Flow

``` text
MockMvc
    │
    ▼
Controller
    │
    ▼
Fake Service
    │
    ▼
Fake Response
```

## Annotations & Methods Used in Controller Testing

### `@WebMvcTest`

Loads only the Spring MVC layer.

**Loads** - Controller - MockMvc - Jackson - Validation

**Does NOT Load** - Repository - Database - Full application

``` java
@WebMvcTest(EmployeeController.class)
```

### `@MockitoBean`

Creates a Mockito mock and registers it inside the Spring context.

``` java
@MockBean
EmployeeService service;
```

The Controller receives this fake Service.

### `@Autowired`

Injects the real `MockMvc` object.

``` java
@Autowired
MockMvc mockMvc;
```

### `MockMvc`

Simulates HTTP requests without starting a server.

``` java
mockMvc.perform(get("/employees"));
```

### `perform()`

Sends a fake HTTP request to the controller.

### `andExpect()`

Verifies the response.

``` java
.andExpect(status().isOk())
```

### `status()`

Checks the HTTP status code.

### `jsonPath()`

Checks JSON values in the response.

``` java
.andExpect(jsonPath("$[0].name").value("John"))
```

### `get()`

Creates a GET request.

``` java
mockMvc.perform(get("/employees"));
```

------------------------------------------------------------------------

# MockMvc

Instead of using Postman:

``` text
Browser
   │
Controller
```

We use

``` java
mockMvc.perform(get("/employees"));
```

MockMvc sends a fake HTTP request to the controller.

------------------------------------------------------------------------

# jsonPath()

Suppose the response is

``` json
[
  {
    "id":1,
    "name":"John",
    "department":"IT",
    "salary":50000
  }
]
```

Then

``` java
jsonPath("$[0].name").value("John");
```

checks that the first employee's name is **John**.

------------------------------------------------------------------------

# Summary

  -----------------------------------------------------------------------------------------
  Layer        Annotation                              Database   Mockito   What is Tested
  ------------ --------------------------------------- ---------- --------- ---------------
  Repository   `@DataJdbcTest`                         ✅         ❌        SQL,
                                                                            JdbcTemplate,
                                                                            RowMapper

  Service      `@ExtendWith(MockitoExtension.class)`   ❌         ✅        Business Logic

  Controller   `@WebMvcTest`                           ❌         ✅        REST API
  -----------------------------------------------------------------------------------------

------------------------------------------------------------------------

# Learning Order

1.  JUnit Assertions
2.  `@Test`
3.  `@BeforeEach`
4.  `@DataJdbcTest`
5.  Mockito (`@Mock`, `@InjectMocks`)
6.  `when()`
7.  `verify()`
8.  `@WebMvcTest`
9.  `MockMvc`
10. Integration Testing (`@SpringBootTest`)

Following this order makes each concept build naturally on the previous
one.
