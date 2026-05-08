CREATE OR REPLACE PACKAGE pkg_employees AS
    PROCEDURE hire_employee(p_name IN VARCHAR2, p_salary IN NUMBER);
    FUNCTION get_salary(p_emp_id IN NUMBER) RETURN NUMBER;
END pkg_employees;
/

CREATE OR REPLACE PACKAGE BODY pkg_employees AS

    PROCEDURE hire_employee(p_name IN VARCHAR2, p_salary IN NUMBER) IS
        l_emp_id NUMBER;
    BEGIN
        INSERT INTO employees (name, salary)
        VALUES (p_name, p_salary)
        RETURNING emp_id INTO l_emp_id;
    END hire_employee;

    FUNCTION get_salary(p_emp_id IN NUMBER) RETURN NUMBER IS
        l_salary NUMBER;
    BEGIN
        SELECT salary INTO l_salary
          FROM employees
         WHERE emp_id = p_emp_id;
        RETURN l_salary;
    END get_salary;

END pkg_employees;
/
