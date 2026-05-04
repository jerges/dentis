package com.dentis.common.constant;

public final class AppConstants {

    private AppConstants() {}

    public static final String API_V1 = "/api/v1";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    public static final class Appointment {
        private Appointment() {}
        public static final int REMINDER_HOURS_BEFORE = 24;
        public static final int MIN_DURATION_MINUTES = 15;
        public static final int MAX_DURATION_MINUTES = 240;
    }

    public static final class Budget {
        private Budget() {}
        public static final int EXPIRY_DAYS = 30;
    }
}
