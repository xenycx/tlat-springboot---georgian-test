package com.tlat.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class AvatarService {
    
    @Value("${avatar.upload.path:./uploads/avatars}")
    private String uploadPath;
    
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    
    /**
     * Save avatar file and return the filename
     */
    public String saveAvatar(MultipartFile file, Long userId) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        
        // Validate file
        validateFile(file);
        
        // Create upload directory if it doesn't exist
        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String newFilename = "avatar_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + extension;
        
        // Save file
        Path filePath = uploadDir.resolve(newFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        return newFilename;
    }
    
    /**
     * Delete an avatar file
     */
    public void deleteAvatar(String filename) {
        if (filename == null || filename.isEmpty()) {
            return;
        }
        
        try {
            Path filePath = Paths.get(uploadPath).resolve(filename);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log error but don't throw - avatar deletion failure shouldn't break other operations
            System.err.println("Failed to delete avatar: " + filename);
        }
    }
    
    /**
     * Get the full path to an avatar file
     */
    public Path getAvatarPath(String filename) {
        return Paths.get(uploadPath).resolve(filename);
    }
    
    /**
     * Validate file type and size
     */
    private void validateFile(MultipartFile file) {
        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed (5MB)");
        }
        
        // Check file extension
        String extension = getFileExtension(file.getOriginalFilename());
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file type. Allowed types: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
        
        // Check content type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }
    }
    
    /**
     * Extract file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return null;
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
    
    /**
     * Get the upload path
     */
    public String getUploadPath() {
        return uploadPath;
    }
}
