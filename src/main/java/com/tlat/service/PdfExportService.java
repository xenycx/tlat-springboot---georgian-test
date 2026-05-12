package com.tlat.service;

import com.tlat.dto.LectureDto;
import java.io.ByteArrayOutputStream;
import java.util.List;

public interface PdfExportService {
    /**
     * PDF-ის გენერაცია მხოლოდ ერთი ლექციისთვის
     */
    ByteArrayOutputStream generateLecturePdf(LectureDto lecture);
    
    /**
     * მრავალ ლექციაზე PDF-ის გენერაცია
     */
    ByteArrayOutputStream generateLecturesPdf(List<LectureDto> lectures, String title);
    
    /**
     * Excel ფაილის გენერაცია ლექციებისთვის
     */
    ByteArrayOutputStream generateLecturesExcel(List<LectureDto> lectures);
}
