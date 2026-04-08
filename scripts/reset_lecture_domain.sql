-- Reset lecture domain data only.
-- Run against the TLAT MySQL database when you want a clean lecture state.

SET FOREIGN_KEY_CHECKS = 0;

-- Remove dependent data first.
TRUNCATE TABLE learning_resources;
TRUNCATE TABLE lecture_schedules;
TRUNCATE TABLE lecture_groups;
TRUNCATE TABLE lecture_lecturers;
TRUNCATE TABLE lectures;

SET FOREIGN_KEY_CHECKS = 1;
