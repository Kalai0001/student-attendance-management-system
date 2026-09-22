package com.college.attendance.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AttendanceSheetForm {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date = LocalDate.now();

    private Integer period = 1;
    private String subject = "Data Structures";
    private String department;
    private Integer year;
    private String section;

    private List<AttendanceRecordDto> records = new ArrayList<>();

    public AttendanceSheetForm() {
    }

    public AttendanceSheetForm(LocalDate date, Integer period, String subject, String department, Integer year, String section) {
        this.date = date != null ? date : LocalDate.now();
        this.period = period != null ? period : 1;
        this.subject = (subject != null && !subject.trim().isEmpty()) ? subject.trim() : "Data Structures";
        this.department = department;
        this.year = year;
        this.section = section;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getPeriod() {
        return period != null ? period : 1;
    }

    public void setPeriod(Integer period) {
        this.period = period != null ? period : 1;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = (subject != null && !subject.trim().isEmpty()) ? subject.trim() : "Data Structures";
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public List<AttendanceRecordDto> getRecords() {
        return records;
    }

    public void setRecords(List<AttendanceRecordDto> records) {
        this.records = records;
    }
}
