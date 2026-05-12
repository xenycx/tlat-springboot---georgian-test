package com.tlat.dto.matrix;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class MatrixGradingDTO {
    private LectureDTO lecture;
    private GroupDTO group;
    private LocalDate semesterStartDate;
    private int semesterWeeks;
    private int maxWeeklyScore;
    private int maxMidtermScore;
    private int maxFinalScore;
    private List<StudentRowDTO> rows;
    private List<ColumnHeaderDTO> columns;
    private List<Integer> weeklyMaxScores; // Index matches week - 1

    @Data
    public static class LectureDTO {
        private Long id;
        private String subject;
    }

    @Data
    public static class GroupDTO {
        private Long id;
        private String code;
    }

    @Data
    public static class StudentRowDTO {
        private Long studentId;
        private String studentName;
        private String studentGroupName;
        private String avatarPath;
        private Map<Integer, CellDTO> cells; // Key is week number
        private double totalAttendanceScore;
        private Double midtermScore;
        private Double finalScore;
        private double totalScore;
        private String totalGrade;
        
        @Data
        public static class CellDTO {
            private Double score;
            private String letterGrade;
            private Long lectureScheduleId;
            private int week;
        }
    }

    @Data
    public static class ColumnHeaderDTO {
        private int week;
        private String label;
        private LocalDate weekStart;
        private LocalDate weekEnd;
    }
}