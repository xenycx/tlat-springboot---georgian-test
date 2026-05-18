package com.tlat.service;

import com.tlat.dto.LectureDto;

import java.io.ByteArrayOutputStream;
import java.util.List;

public interface CsvExportService {
    ByteArrayOutputStream generateLecturesCsv(List<LectureDto> lectures) throws Exception;
}
