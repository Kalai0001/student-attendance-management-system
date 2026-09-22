package com.college.attendance.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static final int OTP_EXPIRY_MINUTES = 5;
    private final SecureRandom random = new SecureRandom();
    
    // Store OTP in memory: username -> OtpData
    private final Map<String, OtpData> otpStorage = new ConcurrentHashMap<>();

    public String generateOtp(String username) {
        int otp = 100000 + random.nextInt(900000);
        String otpString = String.valueOf(otp);
        otpStorage.put(username, new OtpData(otpString, LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES)));
        return otpString;
    }

    public boolean validateOtp(String username, String enteredOtp) {
        OtpData otpData = otpStorage.get(username);
        if (otpData == null) {
            return false;
        }
        
        if (LocalDateTime.now().isAfter(otpData.getExpiryTime())) {
            otpStorage.remove(username); // Expired
            return false;
        }

        if (otpData.getOtp().equals(enteredOtp)) {
            otpStorage.remove(username); // OTP used successfully
            return true;
        }
        
        return false;
    }

    public void clearOtp(String username) {
        if (username != null) {
            otpStorage.remove(username);
        }
    }

    private static class OtpData {
        private final String otp;
        private final LocalDateTime expiryTime;

        public OtpData(String otp, LocalDateTime expiryTime) {
            this.otp = otp;
            this.expiryTime = expiryTime;
        }

        public String getOtp() {
            return otp;
        }

        public LocalDateTime getExpiryTime() {
            return expiryTime;
        }
    }
}
