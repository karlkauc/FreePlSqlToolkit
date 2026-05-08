CREATE OR REPLACE PACKAGE BODY pkg_dirty AS

    PROCEDURE find_employee(p_name IN VARCHAR2) IS
        emp_data employees%ROWTYPE;
    BEGIN
        SELECT * INTO emp_data
          FROM employees
         WHERE name = p_name;
    EXCEPTION
        WHEN OTHERS THEN
            NULL;
    END find_employee;

END pkg_dirty;
/
