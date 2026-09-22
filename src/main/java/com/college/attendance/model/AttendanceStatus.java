package com.college.attendance.model;

public enum AttendanceStatus {
    PRESENT("Present"),
    ABSENT("Absent"),
    OD("OD (On Duty)"),
    LEAVE_GRANTED("Leave Granted"),
    LEAVE_DENIED("Leave Denied");

    private final String displayName;

    AttendanceStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAttended() {
        return this == PRESENT || this == OD;
    }

    public boolean isLeaveGranted() {
        return this == LEAVE_GRANTED;
    }
}
