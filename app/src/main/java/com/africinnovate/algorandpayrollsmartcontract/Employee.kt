package com.africinnovate.algorandpayrollsmartcontract

data class Employee(
    val name: String,
    val role : String,
    val salary : Int,
    val profilePics : Int,
    val accountAddress : String,
    val isSelected : Boolean = false,
    )