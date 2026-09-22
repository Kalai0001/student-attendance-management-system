package com.college.attendance.service;

import com.college.attendance.dto.AttendanceRecordDto;
import com.college.attendance.dto.AttendanceSheetForm;
import com.college.attendance.dto.AttendanceStatsDto;
import com.college.attendance.model.Attendance;
import com.college.attendance.model.AttendanceStatus;
import com.college.attendance.model.Student;
import com.college.attendance.repository.AttendanceRepository;
import com.college.attendance.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;

    public AttendanceService(AttendanceRepository attendanceRepository, StudentRepository studentRepository) {
        this.attendanceRepository = attendanceRepository;
        this.studentRepository = studentRepository;
    }

    public Attendance markOrUpdateAttendance(Long studentId, LocalDate date, Integer period, String subject, AttendanceStatus status) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + studentId));

        Integer safePeriod = (period != null && period >= 1 && period <= 8) ? period : 1;
        String safeSubject = (subject != null && !subject.trim().isEmpty()) ? subject.trim() : "General";
        AttendanceStatus safeStatus = status != null ? status : AttendanceStatus.ABSENT;

        Optional<Attendance> existing = attendanceRepository.findByStudentAndDateAndPeriodAndSubject(student, date, safePeriod, safeSubject);
        if (existing.isPresent()) {
            Attendance attendance = existing.get();
            attendance.setStatus(safeStatus);
            attendance.setSubject(safeSubject);
            attendance.setPeriod(safePeriod);
            return attendanceRepository.save(attendance);
        } else {
            Optional<Attendance> existingPeriod = attendanceRepository.findByStudentAndDateAndPeriod(student, date, safePeriod);
            if (existingPeriod.isPresent()) {
                Attendance attendance = existingPeriod.get();
                attendance.setStatus(safeStatus);
                attendance.setSubject(safeSubject);
                return attendanceRepository.save(attendance);
            } else {
                Attendance attendance = new Attendance(student, date, safePeriod, safeSubject, safeStatus);
                return attendanceRepository.save(attendance);
            }
        }
    }

    public Attendance markOrUpdateAttendance(Long studentId, LocalDate date, AttendanceStatus status) {
        return markOrUpdateAttendance(studentId, date, 1, "General", status);
    }

    public int saveAttendanceSheet(AttendanceSheetForm form) {
        if (form == null || form.getRecords() == null) {
            return 0;
        }

        LocalDate date = form.getDate() != null ? form.getDate() : LocalDate.now();
        Integer period = (form.getPeriod() != null && form.getPeriod() >= 1 && form.getPeriod() <= 8) ? form.getPeriod() : 1;
        String subject = (form.getSubject() != null && !form.getSubject().trim().isEmpty()) ? form.getSubject().trim() : "General";
        int count = 0;

        for (AttendanceRecordDto record : form.getRecords()) {
            if (record.getStudentId() != null) {
                Student student = studentRepository.findById(record.getStudentId()).orElse(null);
                if (student != null) {
                    AttendanceStatus status = record.getStatus() != null ? record.getStatus() : AttendanceStatus.ABSENT;

                    // 1. Check if record matches student + date + period + subject
                    Optional<Attendance> existing = attendanceRepository.findByStudentAndDateAndPeriodAndSubject(student, date, period, subject);
                    if (existing.isPresent()) {
                        Attendance att = existing.get();
                        att.setStatus(status);
                        att.setSubject(subject);
                        att.setPeriod(period);
                        attendanceRepository.save(att);
                    } else {
                        // 2. If subject changed for this period, update existing period record without duplicate conflict
                        Optional<Attendance> existingPeriod = attendanceRepository.findByStudentAndDateAndPeriod(student, date, period);
                        if (existingPeriod.isPresent()) {
                            Attendance att = existingPeriod.get();
                            att.setStatus(status);
                            att.setSubject(subject);
                            attendanceRepository.save(att);
                        } else {
                            // 3. Otherwise create new attendance record
                            Attendance att = new Attendance(student, date, period, subject, status);
                            attendanceRepository.save(att);
                        }
                    }
                    count++;
                }
            }
        }
        return count;
    }

    @Transactional(readOnly = true)
    public AttendanceSheetForm prepareSheetForClass(LocalDate date, Integer period, String subject, String department, Integer year, String section) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        Integer targetPeriod = (period != null && period >= 1 && period <= 8) ? period : 1;
        String targetSubject = (subject != null && !subject.trim().isEmpty()) ? subject.trim() : "Data Structures";

        AttendanceSheetForm form = new AttendanceSheetForm(targetDate, targetPeriod, targetSubject, department, year, section);

        List<Student> students;
        if (department != null && year != null && section != null &&
            !department.isEmpty() && !section.isEmpty()) {
            students = studentRepository.findByDepartmentAndYearAndSectionOrderByRegisterNumberAsc(department, year, section);
        } else {
            students = studentRepository.findAll();
        }

        // Fetch existing attendance for this session (Date + Period + Subject)
        Map<Long, AttendanceStatus> existingStatusMap = new HashMap<>();
        if (!students.isEmpty()) {
            List<Attendance> existingList = attendanceRepository.findByDateAndPeriodAndSubject(targetDate, targetPeriod, targetSubject);
            for (Attendance att : existingList) {
                existingStatusMap.put(att.getStudent().getId(), att.getStatus());
            }
        }

        List<AttendanceRecordDto> records = new ArrayList<>();
        for (Student s : students) {
            // Default status when session is created is ABSENT
            AttendanceStatus status = existingStatusMap.getOrDefault(s.getId(), AttendanceStatus.ABSENT);
            records.add(new AttendanceRecordDto(s.getId(), s.getRegisterNumber(), s.getName(), status));
        }

        form.setRecords(records);
        return form;
    }

    @Transactional(readOnly = true)
    public AttendanceSheetForm prepareSheetForClass(LocalDate date, String department, Integer year, String section) {
        return prepareSheetForClass(date, 1, "Data Structures", department, year, section);
    }

    @Transactional(readOnly = true)
    public AttendanceStatsDto getStudentStats(Student student) {
        long total = attendanceRepository.countByStudent(student);
        long present = attendanceRepository.countByStudentAndStatus(student, AttendanceStatus.PRESENT);
        long absent = attendanceRepository.countByStudentAndStatus(student, AttendanceStatus.ABSENT);
        long od = attendanceRepository.countByStudentAndStatus(student, AttendanceStatus.OD);
        long leaveGranted = attendanceRepository.countByStudentAndStatus(student, AttendanceStatus.LEAVE_GRANTED);
        long leaveDenied = attendanceRepository.countByStudentAndStatus(student, AttendanceStatus.LEAVE_DENIED);

        boolean enabled = student.getUser() != null && student.getUser().isEnabled();

        return new AttendanceStatsDto(
                student.getId(),
                student.getRegisterNumber(),
                student.getName(),
                student.getEmail(),
                student.getPhone(),
                student.getDepartment(),
                student.getYear(),
                student.getSection(),
                total,
                present,
                absent,
                od,
                leaveGranted,
                leaveDenied,
                enabled
        );
    }

    @Transactional(readOnly = true)
    public List<AttendanceStatsDto> getAllStudentsStats(String department, Integer year, String section) {
        List<Student> students;
        if ((department != null && !department.isEmpty()) || year != null || (section != null && !section.isEmpty())) {
            students = studentRepository.searchStudents(null, department, year, section);
        } else {
            students = studentRepository.findAll();
        }

        return students.stream()
                .map(this::getStudentStats)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AttendanceStatsDto> getLowAttendanceStudents(double threshold) {
        return studentRepository.findAll().stream()
                .map(this::getStudentStats)
                .filter(stats -> (stats.getTotalClasses() - stats.getLeaveGrantedClasses()) > 0 && stats.getPercentage() < threshold)
                .sorted(Comparator.comparingDouble(AttendanceStatsDto::getPercentage))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Attendance> getStudentHistory(Student student) {
        return attendanceRepository.findByStudentOrderByDateDescPeriodAsc(student);
    }

    @Transactional(readOnly = true)
    public List<Attendance> searchAttendanceHistory(LocalDate date, Integer period, String subject, String dept, AttendanceStatus status, String keyword) {
        String safeSubject = (subject != null && !subject.trim().isEmpty()) ? subject.trim() : null;
        String safeDept = (dept != null && !dept.trim().isEmpty()) ? dept.trim() : null;
        String safeKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        return attendanceRepository.searchAttendanceHistory(date, period, safeSubject, safeDept, status, safeKeyword);
    }

    @Transactional(readOnly = true)
    public List<Attendance> searchAttendanceHistory(LocalDate date, String dept, AttendanceStatus status, String keyword) {
        return searchAttendanceHistory(date, null, null, dept, status, keyword);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new HashMap<>();
        long totalStudents = studentRepository.count();
        LocalDate today = LocalDate.now();

        long todayMarked = attendanceRepository.countByDate(today);
        long todayPresent = attendanceRepository.countByDateAndStatus(today, AttendanceStatus.PRESENT);
        long todayOD = attendanceRepository.countByDateAndStatus(today, AttendanceStatus.OD);
        long todayLeaveGranted = attendanceRepository.countByDateAndStatus(today, AttendanceStatus.LEAVE_GRANTED);
        long todayLeaveDenied = attendanceRepository.countByDateAndStatus(today, AttendanceStatus.LEAVE_DENIED);
        long todayAbsent = attendanceRepository.countByDateAndStatus(today, AttendanceStatus.ABSENT);

        long effectiveToday = todayMarked - todayLeaveGranted;
        long attendedToday = todayPresent + todayOD;

        double todayPercentage = effectiveToday > 0 
                ? Math.round(((double) attendedToday / effectiveToday * 100.0) * 10.0) / 10.0 
                : 0.0;

        long distinctSessions = 0;
        try {
            distinctSessions = attendanceRepository.countDistinctSessions();
        } catch (Exception ignored) {
            distinctSessions = attendanceRepository.count();
        }

        List<AttendanceStatsDto> lowAttendanceStudents = getLowAttendanceStudents(75.0);

        summary.put("totalStudents", totalStudents);
        summary.put("totalClasses", distinctSessions);
        summary.put("todayMarked", todayMarked);
        summary.put("todayPresent", todayPresent);
        summary.put("todayOD", todayOD);
        summary.put("todayLeaveGranted", todayLeaveGranted);
        summary.put("todayLeaveDenied", todayLeaveDenied);
        summary.put("todayAbsent", todayAbsent);
        summary.put("todayPercentage", todayPercentage);
        summary.put("lowAttendanceCount", lowAttendanceStudents.size());
        summary.put("lowAttendanceList", lowAttendanceStudents.stream().limit(5).collect(Collectors.toList()));

        return summary;
    }
}
