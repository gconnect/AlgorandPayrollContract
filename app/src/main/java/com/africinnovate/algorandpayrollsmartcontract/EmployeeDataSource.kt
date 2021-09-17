package com.africinnovate.algorandpayrollsmartcontract

import java.util.ArrayList

object EmployeeDataSource {
    fun createEmployeeDate(): ArrayList<Employee> {
        val employeeList: ArrayList<Employee> = ArrayList()

        employeeList.add(
            Employee(
                "John Doe",
                "Blockchain Engineer",
                2,
                R.drawable.pix1,
                Constants.ACCOUNT1_ADDRESS
            )
        )
        employeeList.add(
            Employee(
                "Mary Doe",
                "Frontend Engineer",
                1,
                R.drawable.pix2,
                Constants.ACCOUNT2_ADDRESS,
            )
        )
        employeeList.add(
            Employee(
                "Franklin Gold",
                "Backend Engineer",
                2,
                R.drawable.pix3,
                Constants.ACCOUNT3_ADDRESS,
            )
        )
        return employeeList
    }
}