# Algorand Payroll Smart Contract

This solution will guide you in developing and deploying android application using the 
Algorand blockchain atomic transfer and smart contract that addresses the following use case:

* Account creation

* Funding accounts

* Atomic transfer signed by the sender

* Atomic transfer signed by a smart contract

# Demo

![payroll](https://user-images.githubusercontent.com/23031920/133864955-df2e4102-7e4f-4095-9a12-063b7f0cf252.png)

# Requirements

* Android studio setup

* Familiarity with the Java and Kotlin programming language and its use within the Android Environment.

* Basic understanding of some blockchain terminologies.

* Basic understanding of teal and stateless smart contract

# Tools/ Libraries used
  Below are some of the important libraries used:
  - Algorand SDK
  - Coroutines
  - Glide
  - Databinding

# Setup Development Environment
To get started, your android studio should be up and running. To get the code on your android studio, simply click the clone button to clone the project or download the the project. Then from Android studio click on file and  select import to import the project from your local machine.

# Installation Guide

  To install the app, here is the link to the apk
  
# File Structure
- `Adapter` handles the recyclerview for the list of employees
- `constants` handles constant variables used in the MainActivity and DetailActivity
- `MainActivity` handles the main logic of the application
- `DetailActivity` handles the detail page for each employees
  
# How the app works
  After installation..
  
  - It takes you to the `MainActivity`. The `MainActivity` contains a list of employees, two fabs and two buttons
  - The `green fab` enables you to fund the contract/senders account
  - The `purple fab` enables you to copy the contract/senders address
  - The explore button takes you to the algoexplorer page
  - The `pay employees` button calls the atomic transfer method to send algo to the employees either using the smart contract logic sig option or the sender option
   
# License
  Distributed under the MIT License. See  for more information. [LICENSE](https://github.com/gconnect/AlgorandPayrollContract/blob/master/LICENSE)
  
# Blog and Video Tutorial
For more details you can checkout the blog post [here] .


# Disclaimer
 This project is not audited and should not be used in a production environment.
 


