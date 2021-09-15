package com.africinnovate.algorandpayrollsmartcontract

import java.util.ArrayList

object EmployeeDataSource {
    private val employeeList  : ArrayList<Employee> = ArrayList()
     fun createEmployeeDate(): ArrayList<Employee> {
        employeeList.add(Employee("John Doe", "Blockchain Engineer", 3, R.drawable.pix1, Constants.EMPLOYEE1_ADDRESS))
        employeeList.add(Employee("Mary Doe", "Frontend Engineer", 1, R.drawable.pix2, Constants.EMPLOYEE2_ADDRESS,))
        employeeList.add(Employee("Franklin Gold", "Backend Engineer", 2, R.drawable.pix3,Constants.EMPLOYER_ADDRESS,))
        return  employeeList
    }
}