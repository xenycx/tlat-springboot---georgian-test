package com.tlat.entity;

public enum LectureStatus {
    SCHEDULED("დაგეგმილია"),
    IN_PROGRESS("მიმდინარეობს"), 
    COMPLETED("დასრულებულია"),
    MISSED("გაცდენილია");

    private final String displayName;

    LectureStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}