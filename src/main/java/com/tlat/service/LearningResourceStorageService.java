package com.tlat.service;

import com.tlat.Entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class LearningResourceStorageService {

    @Value("${resource.upload.path:./uploads/resources}")
    private String uploadPath;

    private final SettingsService settingsService;

    @Autowired
    public LearningResourceStorageService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    private static final List<String> ALLOWED_EXTENSIONS = List.of("pdf", "doc", "docx", "ppt", "pptx", "zip");

    public StoredFile save(MultipartFile file, User uploader, boolean isAdmin) throws IOException {
        validate(file, isAdmin);

        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        String originalFilename = sanitizeFileName(file.getOriginalFilename());
        String extension = getFileExtension(originalFilename);
        String storedFilename = "resource_" + uploader.getId() + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + extension;

        Path destination = uploadDir.resolve(storedFilename);
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        return new StoredFile(storedFilename, originalFilename, file.getSize(), contentType);
    }

    public void delete(String storedFilename) {
        if (storedFilename == null || storedFilename.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(getPath(storedFilename));
        } catch (IOException ignored) {
            // File deletion failures should not break business transactions.
        }
    }

    public Path getPath(String storedFilename) {
        return Paths.get(uploadPath).resolve(storedFilename);
    }

    public String probeContentType(Path filePath) {
        try {
            String contentType = Files.probeContentType(filePath);
            return contentType == null ? "application/octet-stream" : contentType;
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    private void validate(MultipartFile file, boolean isAdmin) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("ფაილის ატვირთვა აუცილებელია");
        }

        long maxBytes = settingsService.getLecturerMaxFileSizeBytes();
        if (!isAdmin && file.getSize() > maxBytes) {
            long maxMb = maxBytes / (1024 * 1024);
            throw new IllegalArgumentException("ლექტორისთვის დასაშვები ფაილის მაქსიმალური ზომა არის " + maxMb + "MB");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("დაშვებულია მხოლოდ PDF, DOC, DOCX, PPT, PPTX და ZIP ფაილები");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return null;
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private String sanitizeFileName(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }
        return filename.replace("..", "").replace("/", "_").replace("\\", "_").trim();
    }

    public record StoredFile(String storedFilename, String originalFilename, long fileSize, String contentType) {
    }
}
