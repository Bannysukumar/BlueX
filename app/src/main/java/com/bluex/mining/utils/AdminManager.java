package com.bluex.mining.utils;

import java.util.Arrays;
import java.util.List;

public class AdminManager {
    private static final List<String> ADMIN_EMAILS = Arrays.asList(
        "admin@bluex.com",
        "bannysukumar@gmail.com"  // Add your admin email here
    );

    public static boolean isAdmin(String email) {
        return email != null && ADMIN_EMAILS.contains(email.toLowerCase());
    }
} 