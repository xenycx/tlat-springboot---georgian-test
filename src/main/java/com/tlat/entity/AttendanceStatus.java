package com.tlat.entity;

public enum AttendanceStatus {
    PRESENT("დაესწრო"),
    ABSENT("გააცდინა"),
    EXCUSED("საპატიო");

    private final String displayName;

    AttendanceStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
