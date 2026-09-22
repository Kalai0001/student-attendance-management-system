/**
 * Student Attendance System Client Script
 */

document.addEventListener("DOMContentLoaded", function () {
    // Auto-dismiss alerts after 6 seconds
    const alerts = document.querySelectorAll(".alert-dismissible");
    alerts.forEach(function (alert) {
        setTimeout(function () {
            const bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
            if (bsAlert) {
                bsAlert.close();
            }
        }, 6000);
    });

    // Helper: Mark all students Present in Attendance Sheet
    const btnAllPresent = document.getElementById("btnMarkAllPresent");
    if (btnAllPresent) {
        btnAllPresent.addEventListener("click", function () {
            const presentRadios = document.querySelectorAll("input[type='radio'][value='PRESENT']");
            presentRadios.forEach(radio => radio.checked = true);
            updateSummaryCounters();
        });
    }

    // Helper: Mark all students Absent in Attendance Sheet
    const btnAllAbsent = document.getElementById("btnMarkAllAbsent");
    if (btnAllAbsent) {
        btnAllAbsent.addEventListener("click", function () {
            const absentRadios = document.querySelectorAll("input[type='radio'][value='ABSENT']");
            absentRadios.forEach(radio => radio.checked = true);
            updateSummaryCounters();
        });
    }

    // Dynamic counter update on attendance toggle change
    const attendanceRadios = document.querySelectorAll(".attendance-toggle-group input[type='radio']");
    if (attendanceRadios.length > 0) {
        attendanceRadios.forEach(radio => {
            radio.addEventListener("change", updateSummaryCounters);
        });
        updateSummaryCounters();
    }
});

function updateSummaryCounters() {
    const presentRadios = document.querySelectorAll("input[type='radio'][value='PRESENT']:checked");
    const absentRadios = document.querySelectorAll("input[type='radio'][value='ABSENT']:checked");

    const presentBadge = document.getElementById("countLivePresent");
    const absentBadge = document.getElementById("countLiveAbsent");

    if (presentBadge) presentBadge.textContent = presentRadios.length;
    if (absentBadge) absentBadge.textContent = absentRadios.length;
}

// Function to autofill demo credentials on login screen
function fillCredentials(username, password) {
    const userInput = document.getElementById("username");
    const passInput = document.getElementById("password");
    if (userInput && passInput) {
        userInput.value = username;
        passInput.value = password;
        userInput.classList.add("is-valid");
        passInput.classList.add("is-valid");
    }
}
