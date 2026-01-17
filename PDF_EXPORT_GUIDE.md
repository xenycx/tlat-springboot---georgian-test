# PDF Export Feature - Implementation Guide

## Overview
This document describes the PDF and Excel export functionality added to the TLAT (Technical Lecture Attendance Tracker) Spring Boot application.

## Features Implemented

### 1. **Single Lecture PDF Export**
- Export individual lecture details to a professional PDF document
- Accessible from each lecture row with a blue PDF icon button
- Endpoint: `GET /lectures/export/pdf/{id}`

### 2. **Bulk PDF Export**
- Export all lectures to a single PDF document
- Available from the "Export" dropdown menu
- Respects user permissions (regular users see only their lectures, admins see all)
- Endpoint: `GET /lectures/export/pdf/all`

### 3. **Excel Export**
- Export all lectures to Excel format (.xlsx)
- Professional formatting with headers and borders
- Available from the "Export" dropdown menu
- Endpoint: `GET /lectures/export/excel/all`

### 4. **Filtered PDF Export**
- Export lectures based on custom filters:
  - Date range (start date - end date)
  - Status (Scheduled, In Progress, Completed, Cancelled)
- Modal dialog for filter selection
- Endpoint: `GET /lectures/export/pdf/filtered`

## Technical Implementation

### Dependencies Added
```xml
<!-- PDF Generation -->
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>itext7-core</artifactId>
    <version>7.2.5</version>
    <type>pom</type>
</dependency>

<!-- Excel Export -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.3</version>
</dependency>
```

### New Files Created

1. **PdfExportService.java** - Service interface
   - Location: `src/main/java/com/tlat/service/`
   - Defines methods for PDF and Excel generation

2. **PdfExportServiceImpl.java** - Service implementation
   - Location: `src/main/java/com/tlat/service/Impl/`
   - Implements PDF generation using iText 7
   - Implements Excel generation using Apache POI

### Modified Files

1. **LectureController.java**
   - Added 4 new export endpoints
   - Integrated PdfExportService
   - Security: Respects user roles (admin vs regular user)

2. **lecture/list.html**
   - Added export dropdown menu in header
   - Added PDF icon button for each lecture row
   - Added filter modal for custom exports
   - Added JavaScript function for filtered export

3. **application.properties**
   - Added configuration for company name
   - Added configuration for report title

## User Interface

### Export Dropdown Menu
Located in the lecture list header, provides:
- **PDF ფორმატში** - Export all to PDF
- **Excel ფორმატში** - Export all to Excel
- **ფილტრით ექსპორტი** - Export with filters

### Individual Export
- Blue PDF icon button in each lecture row
- Direct download of single lecture details

### Filter Export Modal
Form fields:
- Start Date (optional)
- End Date (optional)
- Status dropdown (All, Scheduled, In Progress, Completed, Cancelled)

## Configuration

### application.properties
```properties
# PDF Export Configuration
export.pdf.company-name=TLAT - Technical Lecture Attendance Tracker
export.pdf.report-title=Lecture Report
```

You can customize:
- `export.pdf.company-name` - Organization name shown in PDFs
- `export.pdf.report-title` - Default title for bulk reports

## Security Features

1. **Role-Based Access**
   - Admins: Can export all lectures
   - Regular Users: Can export only their own lectures

2. **Authentication Required**
   - All export endpoints require authentication
   - Uses Spring Security Principal for user identification

## PDF Document Features

### Single Lecture PDF
- Title: "Lecture Details"
- Company name header
- Detailed information table with:
  - ID, Room Number, Date
  - Start Time, End Time
  - Lecturer, Subject, Status
- Generation timestamp footer

### Bulk Lectures PDF
- Custom title (e.g., "All Lectures Report")
- Company name header
- Summary (total lecture count)
- Professional table with all lectures
- Generation timestamp footer

### Styling
- Professional color scheme
- Dark blue headers with white text
- Striped rows for readability
- Auto-sized columns

## Excel Document Features

- Professional formatting
- Color-coded headers (dark blue background)
- Bordered cells
- Auto-sized columns
- All lecture data fields included

## Testing the Feature

### 1. Start the Application
```bash
./mvnw spring-boot:run
```

### 2. Login and Navigate
- Login with your credentials
- Navigate to "ლექციების ჩამონათვალი" (Lectures List)

### 3. Test Export Options
- Click individual PDF icon to export single lecture
- Use "ექსპორტი" dropdown to export all lectures
- Try filtered export with date range and status

### 4. Verify Downloads
- Check downloaded PDF files open correctly
- Verify Excel files open in Microsoft Excel or LibreOffice
- Confirm Georgian text displays properly (if applicable)

## Browser Compatibility

Tested and working on:
- Chrome
- Firefox
- Edge
- Safari

## File Naming Convention

Generated files use this pattern:
- Single lecture: `lecture_{id}_{date}.pdf`
- All lectures PDF: `all_lectures_{date}.pdf`
- All lectures Excel: `all_lectures_{date}.xlsx`
- Filtered lectures: `filtered_lectures_{date}.pdf`

Date format: `yyyyMMdd` (e.g., 20251119)

## Performance Considerations

1. **Large Datasets**: For hundreds of lectures, exports may take a few seconds
2. **Memory Usage**: Excel exports use more memory than PDF for large datasets
3. **Recommended**: Use filtered exports for large date ranges

## Future Enhancements (Optional)

Possible improvements:
1. Add company logo to PDF header
2. Support for custom PDF templates
3. Export to CSV format
4. Scheduled automated reports via email
5. Custom column selection for exports
6. Charts and graphs in PDF reports
7. Multi-language support for export labels

## Troubleshooting

### Issue: PDF Not Downloading
- Check browser's download settings
- Verify endpoint is accessible
- Check console for JavaScript errors

### Issue: Georgian Text Not Displaying
- Ensure proper font support in PDF library
- Consider adding custom font configuration

### Issue: Empty PDF Generated
- Verify lectures exist for the selected filters
- Check user permissions
- Verify database connection

## API Endpoints Reference

| Endpoint | Method | Description | Parameters |
|----------|--------|-------------|------------|
| `/lectures/export/pdf/{id}` | GET | Export single lecture | `id` - Lecture ID |
| `/lectures/export/pdf/all` | GET | Export all lectures | None |
| `/lectures/export/excel/all` | GET | Export all to Excel | None |
| `/lectures/export/pdf/filtered` | GET | Export with filters | `startDate`, `endDate`, `status` |

## Conclusion

The PDF export feature is now fully integrated into your TLAT application. Users can easily export lecture data in multiple formats with professional formatting and flexible filtering options.

For questions or issues, please refer to the troubleshooting section or contact the development team.
