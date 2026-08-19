package com.devops.studentmanagement.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StudentController {

    @GetMapping("/students")
    public String getStudents() {
        return "Student Management System - Automatic Deployment Version 20 Successfully!";
    }
}
