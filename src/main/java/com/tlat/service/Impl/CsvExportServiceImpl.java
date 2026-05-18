package com.tlat.service.Impl;

import com.tlat.dto.LectureDto;
import com.tlat.service.CsvExportService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class CsvExportServiceImpl implements CsvExportService {

    private static final String[] HEADERS = {"Room", "Date", "StartTime", "EndTime", "Lecturers", "Subject", "Groups"};

    @Override
    public ByteArrayOutputStream generateLecturesCsv(List<LectureDto> lectures) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Write UTF-8 BOM to help Excel detect UTF-8 encoding
        out.write(0xEF);
        out.write(0xBB);
        out.write(0xBF);

           try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
               CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

              // write header row
              csvPrinter.printRecord((Object[]) HEADERS);

              for (LectureDto l : lectures) {
                List<String> lecturerNames = l.getLecturerNames();
                List<String> groupCodes = l.getGroupCodes();

                String lecturers = lecturerNames != null ? String.join(";", lecturerNames) : "";
                String groups = groupCodes != null ? String.join(";", groupCodes) : "";

                csvPrinter.printRecord(
                        l.getRoomNumber() == null ? "" : l.getRoomNumber(),
                        l.getDate() == null ? "" : l.getDate().toString(),
                        l.getStartTime() == null ? "" : l.getStartTime().toString(),
                        l.getEndTime() == null ? "" : l.getEndTime().toString(),
                        lecturers,
                        l.getSubject() == null ? "" : l.getSubject(),
                        groups
                );
            }
            csvPrinter.flush();
        }

        return out;
    }
}
