package com.tlat.service.Impl;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.tlat.dto.LectureDto;
import com.tlat.service.PdfExportService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfExportServiceImpl implements PdfExportService {

    private static final String COMPANY_NAME = "TLAT - ლექციების აღრიცხვის სისტემა";
    private static final String REPORT_TITLE = "ლექციების რეპორტი";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public ByteArrayOutputStream generateLecturePdf(LectureDto lecture) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            document.setMargins(50, 40, 50, 40);

            // ქართული ფონტის მხარდაჭერა - Sylfaen საუკეთესო მხარდაჭერას უზრუნველყოფს ქართულისთვის
            PdfFont georgianFont;
            try {
                // Sylfaen-ის დირექტორია C:\Windows\Fonts-ში
                georgianFont = PdfFontFactory.createFont("C:/Windows/Fonts/sylfaen.ttf", PdfEncodings.IDENTITY_H, 
                        PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);
            } catch (Exception e) {
                try {
                    // სხვა ფონტი თუ რატომღაც არ მუშაობს Sylfaen-ი
                    georgianFont = PdfFontFactory.createFont("C:/Windows/Fonts/georgia.ttf", PdfEncodings.IDENTITY_H, 
                            PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);
                } catch (Exception ex) {
                    // ბოლო შანსი თუ არცერთმა არ იმუშავა
                    georgianFont = PdfFontFactory.createFont("C:/Windows/Fonts/arial.ttf", PdfEncodings.IDENTITY_H, 
                            PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);
                }
            }
            document.setFont(georgianFont);

            // დეკორატიული ჰედერი
            Table headerBox = new Table(1).useAllAvailableWidth();
            Cell headerCell = new Cell()
                    .add(new Paragraph(COMPANY_NAME)
                            .setFontSize(16)
                            .setBold()
                            .setTextAlignment(TextAlignment.CENTER)
                            .setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(new DeviceRgb(41, 128, 185))
                    .setPadding(15)
                    .setBorder(null);
            headerBox.addCell(headerCell);
            document.add(headerBox);

            // სათაური
            Paragraph title = new Paragraph("ლექციის ინფორმაცია")
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(20)
                    .setMarginBottom(20);
            document.add(title);

            // მთავარი დეტალები
            Table detailsBox = new Table(1).useAllAvailableWidth()
                    .setMarginBottom(20);
            
            Cell boxCell = new Cell()
                    .setBackgroundColor(new DeviceRgb(236, 240, 241))
                    .setPadding(15)
                    .setBorder(null);
            
            // შიდა ცხრილი დეტალებისთვის
            Table detailsTable = new Table(UnitValue.createPercentArray(new float[]{35, 65}))
                    .useAllAvailableWidth();

            addDetailRow(detailsTable, "ლექციის ID:", String.valueOf(lecture.getId()));
            addDetailRow(detailsTable, "თარიღი:", lecture.getDate().format(DATE_FORMATTER));
            addDetailRow(detailsTable, "დრო:", lecture.getStartTime().format(TIME_FORMATTER) + " - " + 
                    lecture.getEndTime().format(TIME_FORMATTER));
            addDetailRow(detailsTable, "ხანგრძლივობა:", calculateDuration(lecture.getStartTime(), lecture.getEndTime()));
            addDetailRow(detailsTable, "ოთახის ნომერი:", lecture.getRoomNumber());
            addDetailRow(detailsTable, "სტატუსი:", getStatusInGeorgian(lecture.getStatus()));
            
            boxCell.add(detailsTable);
            detailsBox.addCell(boxCell);
            document.add(detailsBox);

            // ლექტორისა და საგნის სექცია
            Paragraph lecturerTitle = new Paragraph("ლექციის დეტალები")
                    .setFontSize(14)
                    .setBold()
                    .setMarginTop(10)
                    .setMarginBottom(10);
            document.add(lecturerTitle);

            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{35, 65}))
                    .useAllAvailableWidth()
                    .setMarginBottom(20);
            
            addDetailRow(infoTable, "ლექტორი:", lecture.getLecturer());
            addDetailRow(infoTable, "საგანი:", lecture.getSubject());
            
            document.add(infoTable);

            // დეკორატიული ფუტერი 
            Table footerBox = new Table(1).useAllAvailableWidth()
                    .setMarginTop(30);
            Cell footerCell = new Cell()
                    .add(new Paragraph("რეპორტი შექმნილია: " + java.time.LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                            .setFontSize(9)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(new DeviceRgb(236, 240, 241))
                    .setPadding(10)
                    .setBorder(null);
            footerBox.addCell(footerCell);
            document.add(footerBox);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF: " + e.getMessage(), e);
        }

        return baos;
    }
    
    private String calculateDuration(java.time.LocalTime start, java.time.LocalTime end) {
        long minutes = java.time.Duration.between(start, end).toMinutes();
        long hours = minutes / 60;
        long mins = minutes % 60;
        
        if (hours > 0) {
            return hours + " საათი " + mins + " წუთი";
        } else {
            return mins + " წუთი";
        }
    }
    
    // ლექციის სტატუსები
    private String getStatusInGeorgian(com.tlat.entity.LectureStatus status) {
        if (status == null) return "მითითებული არ არის";
        
        switch (status) {
            case SCHEDULED:
                return "დაგეგმილი";
            case IN_PROGRESS:
                return "მიმდინარე";
            case COMPLETED:
                return "დასრულებული";
            case MISSED:
                return "გამოტოვებული";
            default:
                return status.toString();
        }
    }

    @Override
    public ByteArrayOutputStream generateLecturesPdf(List<LectureDto> lectures, String title) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            document.setMargins(50, 40, 50, 40);

            // ქართული ფონტის მხარდაჭერა - Sylfaen საუკეთესო მხარდაჭერას უზრუნველყოფს ქართულისთვის
            PdfFont georgianFont;
            try {
                georgianFont = PdfFontFactory.createFont("C:/Windows/Fonts/sylfaen.ttf", PdfEncodings.IDENTITY_H, 
                        PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);
            } catch (Exception e) {
                try {
                    georgianFont = PdfFontFactory.createFont("C:/Windows/Fonts/georgia.ttf", PdfEncodings.IDENTITY_H, 
                            PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);
                } catch (Exception ex) {
                    georgianFont = PdfFontFactory.createFont("C:/Windows/Fonts/arial.ttf", PdfEncodings.IDENTITY_H, 
                            PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);
                }
            }
            document.setFont(georgianFont);

            Table headerBox = new Table(1).useAllAvailableWidth();
            Cell headerCell = new Cell()
                    .add(new Paragraph(COMPANY_NAME)
                            .setFontSize(16)
                            .setBold()
                            .setTextAlignment(TextAlignment.CENTER)
                            .setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(new DeviceRgb(41, 128, 185))
                    .setPadding(15)
                    .setBorder(null);
            headerBox.addCell(headerCell);
            document.add(headerBox);

            Paragraph titlePara = new Paragraph(title != null ? title : REPORT_TITLE)
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(20)
                    .setMarginBottom(10);
            document.add(titlePara);

            // სტატისტიკის სექცია
            Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                    .useAllAvailableWidth()
                    .setMarginBottom(20);
            
            // სულ ლექციები
            summaryTable.addCell(createSummaryCell("სულ ლექციები:", String.valueOf(lectures.size())));
            
            // რეპორტის თარიღი
            summaryTable.addCell(createSummaryCell("რეპორტის თარიღი:", 
                    java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
            
            // სტატუსების განაწილება თუ არის ლექციები
            if (!lectures.isEmpty()) {
                long scheduled = lectures.stream().filter(l -> l.getStatus().toString().equals("SCHEDULED")).count();
                long inProgress = lectures.stream().filter(l -> l.getStatus().toString().equals("IN_PROGRESS")).count();
                long completed = lectures.stream().filter(l -> l.getStatus().toString().equals("COMPLETED")).count();
                
                summaryTable.addCell(createSummaryCell("დაგეგმილი:", String.valueOf(scheduled)));
                summaryTable.addCell(createSummaryCell("მიმდინარე:", String.valueOf(inProgress)));
                summaryTable.addCell(createSummaryCell("დასრულებული:", String.valueOf(completed)));
                summaryTable.addCell(createSummaryCell("სხვა:", String.valueOf(lectures.size() - scheduled - inProgress - completed)));
            }
            
            document.add(summaryTable);

            Paragraph sectionTitle = new Paragraph("ლექციების დეტალები")
                    .setFontSize(14)
                    .setBold()
                    .setMarginTop(10)
                    .setMarginBottom(10);
            document.add(sectionTitle);

            // ლექციების ჯგუფირება თარიღის მიხედვით
            var lecturesByDate = lectures.stream()
                    .collect(java.util.stream.Collectors.groupingBy(LectureDto::getDate));
            
            // თარიღების სორტირება
            var sortedDates = lecturesByDate.keySet().stream()
                    .sorted()
                    .toList();

            for (java.time.LocalDate date : sortedDates) {
                // თარიღის სათაური
                Paragraph dateHeader = new Paragraph(date.format(DATE_FORMATTER))
                        .setFontSize(12)
                        .setBold()
                        .setBackgroundColor(new DeviceRgb(236, 240, 241))
                        .setPadding(8)
                        .setMarginTop(10)
                        .setMarginBottom(5);
                document.add(dateHeader);

                // ცხრილი ამ თარიღის ლექციებისთვის
                float[] columnWidths = {10, 12, 12, 12, 27, 27};
                Table table = new Table(UnitValue.createPercentArray(columnWidths))
                        .useAllAvailableWidth()
                        .setMarginBottom(15);

                // სათაურის რიგი
                addHeaderCell(table, "ID");
                addHeaderCell(table, "ოთახი");
                addHeaderCell(table, "დაწყება");
                addHeaderCell(table, "დასრულება");
                addHeaderCell(table, "ლექტორი");
                addHeaderCell(table, "საგანი");

                // მონაცემთა რიგები ამ თარიღისთვის
                List<LectureDto> dateLectures = lecturesByDate.get(date);
                dateLectures.sort((a, b) -> a.getStartTime().compareTo(b.getStartTime())); // დროის მიხედვით სორტირება
                
                for (LectureDto lecture : dateLectures) {
                    addDataCell(table, String.valueOf(lecture.getId()));
                    addDataCell(table, lecture.getRoomNumber());
                    addDataCell(table, lecture.getStartTime().format(TIME_FORMATTER));
                    addDataCell(table, lecture.getEndTime().format(TIME_FORMATTER));
                    addDataCell(table, lecture.getLecturer());
                    addDataCell(table, lecture.getSubject());
                }

                document.add(table);
            }

            // ფუტერი გვერდების ნომრებით
            int numberOfPages = pdfDoc.getNumberOfPages();
            for (int i = 1; i <= numberOfPages; i++) {
                document.showTextAligned(new Paragraph("გვერდი " + i + " / " + numberOfPages)
                        .setFontSize(9),
                        300, 30, i, TextAlignment.CENTER, null, 0);
            }

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF: " + e.getMessage(), e);
        }

        return baos;
    }

    private Cell createSummaryCell(String label, String value) {
        Paragraph content = new Paragraph()
                .add(new Paragraph(label).setBold().setFontSize(10))
                .add(" " + value);
        return new Cell()
                .add(content)
                .setBorder(null)
                .setPadding(5);
    }

    @Override
    public ByteArrayOutputStream generateLecturesExcel(List<LectureDto> lectures) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Lectures");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Room", "Date", "Start Time", "End Time", "Lecturer", "Subject", "Status"};
            
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (LectureDto lecture : lectures) {
                Row row = sheet.createRow(rowNum++);
                
                row.createCell(0).setCellValue(lecture.getId());
                row.createCell(1).setCellValue(lecture.getRoomNumber());
                row.createCell(2).setCellValue(lecture.getDate().format(DATE_FORMATTER));
                row.createCell(3).setCellValue(lecture.getStartTime().format(TIME_FORMATTER));
                row.createCell(4).setCellValue(lecture.getEndTime().format(TIME_FORMATTER));
                row.createCell(5).setCellValue(lecture.getLecturer());
                row.createCell(6).setCellValue(lecture.getSubject());
                row.createCell(7).setCellValue(lecture.getStatus() != null ? lecture.getStatus().toString() : "N/A");

                // მონაცემთა სტილის გამოყენება ყველა უჯრაზე
                for (int i = 0; i < headers.length; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }

            // სვეტების ავტომატური ზომის მორგება
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(baos);
        } catch (Exception e) {
            throw new RuntimeException("Error generating Excel: " + e.getMessage(), e);
        }

        return baos;
    }

    private void addDetailRow(Table table, String label, String value) {
        Cell labelCell = new Cell().add(new Paragraph(label).setBold())
                .setBackgroundColor(new DeviceRgb(240, 240, 240));
        Cell valueCell = new Cell().add(new Paragraph(value != null ? value : "N/A"));
        
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addHeaderCell(Table table, String text) {
        Cell cell = new Cell().add(new Paragraph(text).setBold().setFontSize(10))
                .setBackgroundColor(new DeviceRgb(52, 73, 94))
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER);
        table.addHeaderCell(cell);
    }

    private void addDataCell(Table table, String text) {
        Cell cell = new Cell().add(new Paragraph(text != null ? text : "N/A").setFontSize(9))
                .setTextAlignment(TextAlignment.LEFT);
        table.addCell(cell);
    }
}
