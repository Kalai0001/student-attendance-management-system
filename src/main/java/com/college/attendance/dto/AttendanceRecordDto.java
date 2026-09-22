package com.college.attendance.dto;

import com.college.attendance.model.AttendanceStatus;

public class AttendanceRecordDto {

    private Long studentId;
    private String registerNumber;
    private String studentName;
    private AttendanceStatus status = AttendanceStatus.ABSENT;

    public AttendanceRecordDto() {
    }

    public AttendanceRecordDto(Long studentId, String registerNumber, String studentName, AttendanceStatus status) {
        this.studentId = studentId;
        this.registerNumber = registerNumber;
        this.studentName = studentName;
        this.status = status != null ? status : AttendanceStatus.ABSENT;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getRegisterNumber() {
        return registerNumber;
    }

    public void setRegisterNumber(String registerNumber) {
        this.registerNumber = registerNumber;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }
}
