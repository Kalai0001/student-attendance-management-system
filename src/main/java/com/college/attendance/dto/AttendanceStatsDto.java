package com.college.attendance.dto;

public class AttendanceStatsDto {

    private Long studentId;
    private String registerNumber;
    private String studentName;
    private String email;
    private String phone;
    private String department;
    private Integer year;
    private String section;
    private long totalClasses;
    private long presentClasses;
    private long absentClasses;
    private long odClasses;
    private long leaveGrantedClasses;
    private long leaveDeniedClasses;
    private double percentage;
    private boolean lowAttendance;
    private boolean enabled = true;

    public AttendanceStatsDto() {
    }

    public AttendanceStatsDto(Long studentId, String registerNumber, String studentName, 
                              String department, Integer year, String section, 
                              long totalClasses, long presentClasses, long absentClasses) {
        this(studentId, registerNumber, studentName, null, null, department, year, section, 
             totalClasses, presentClasses, absentClasses, 0, 0, 0, true);
    }

    public AttendanceStatsDto(Long studentId, String registerNumber, String studentName,
                              String email, String phone,
                              String department, Integer year, String section, 
                              long totalClasses, long presentClasses, long absentClasses,
                              boolean enabled) {
        this(studentId, registerNumber, studentName, email, phone, department, year, section, 
             totalClasses, presentClasses, absentClasses, 0, 0, 0, enabled);
    }

    public AttendanceStatsDto(Long studentId, String registerNumber, String studentName,
                              String email, String phone,
                              String department, Integer year, String section, 
                              long totalClasses, long presentClasses, long absentClasses,
                              long odClasses, long leaveGrantedClasses, long leaveDeniedClasses,
                              boolean enabled) {
        this.studentId = studentId;
        this.registerNumber = registerNumber;
        this.studentName = studentName;
        this.email = email;
        this.phone = phone;
        this.department = department;
        this.year = year;
        this.section = section;
        this.totalClasses = totalClasses;
        this.presentClasses = presentClasses;
        this.absentClasses = absentClasses;
        this.odClasses = odClasses;
        this.leaveGrantedClasses = leaveGrantedClasses;
        this.leaveDeniedClasses = leaveDeniedClasses;
        this.enabled = enabled;

        // Formula: Attendance % = (Present + OD) / (Total Classes - Granted Leave) * 100
        long effectiveClasses = totalClasses - leaveGrantedClasses;
        long attendedClasses = presentClasses + odClasses;
        this.percentage = effectiveClasses > 0 
                ? Math.round(((double) attendedClasses / effectiveClasses * 100.0) * 10.0) / 10.0 
                : 0.0;
        this.lowAttendance = effectiveClasses > 0 && this.percentage < 75.0;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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

    public long getTotalClasses() {
        return totalClasses;
    }

    public void setTotalClasses(long totalClasses) {
        this.totalClasses = totalClasses;
    }

    public long getPresentClasses() {
        return presentClasses;
    }

    public void setPresentClasses(long presentClasses) {
        this.presentClasses = presentClasses;
    }

    public long getAbsentClasses() {
        return absentClasses;
    }

    public void setAbsentClasses(long absentClasses) {
        this.absentClasses = absentClasses;
    }

    public long getOdClasses() {
        return odClasses;
    }

    public void setOdClasses(long odClasses) {
        this.odClasses = odClasses;
    }

    public long getLeaveGrantedClasses() {
        return leaveGrantedClasses;
    }

    public void setLeaveGrantedClasses(long leaveGrantedClasses) {
        this.leaveGrantedClasses = leaveGrantedClasses;
    }

    public long getLeaveDeniedClasses() {
        return leaveDeniedClasses;
    }

    public void setLeaveDeniedClasses(long leaveDeniedClasses) {
        this.leaveDeniedClasses = leaveDeniedClasses;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public boolean isLowAttendance() {
        return lowAttendance;
    }

    public void setLowAttendance(boolean lowAttendance) {
        this.lowAttendance = lowAttendance;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isPendingApproval() {
        return !enabled;
    }

    public String getBadgeClass() {
        if ((totalClasses - leaveGrantedClasses) <= 0) return "bg-secondary";
        if (percentage >= 85.0) return "bg-success";
        if (percentage >= 75.0) return "bg-primary";
        if (percentage >= 60.0) return "bg-warning text-dark";
        return "bg-danger";
    }
}
