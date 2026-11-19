package com.tcs.VendorPulse.common.storage;

import com.tcs.VendorPulse.common.error.ApiException;
import com.tcs.VendorPulse.common.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path baseStoragePath;
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    public FileStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.baseStoragePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.baseStoragePath.resolve("documents"));
            Files.createDirectories(this.baseStoragePath.resolve("agreements"));
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory", e);
        }
    }

    public String storeDocument(MultipartFile file) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String storedFilename = UUID.randomUUID() + extension;
        Path targetDir = baseStoragePath.resolve("documents");
        Path targetLocation = targetDir.resolve(storedFilename);

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return "documents/" + storedFilename;
        } catch (IOException e) {
            throw new ApiException(ErrorCode.INVALID_FILE, "Failed to store file: " + e.getMessage());
        }
    }

    public String storeAgreementPdf(String poNumber, byte[] pdfBytes) {
        String filename = "agreement-" + poNumber + ".pdf";
        Path targetDir = baseStoragePath.resolve("agreements");
        Path targetLocation = targetDir.resolve(filename);
        try {
            Files.write(targetLocation, pdfBytes);
            return "agreements/" + filename;
        } catch (IOException e) {
            throw new ApiException(ErrorCode.AGREEMENT_GENERATION_FAILED, "Failed to save agreement PDF");
        }
    }

    public Resource loadFileAsResource(String storageKey) {
        try {
            Path filePath = baseStoragePath.resolve(storageKey).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ApiException(ErrorCode.NOT_FOUND, "File not found or not readable: " + storageKey);
            }
        } catch (MalformedURLException e) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Invalid file path: " + storageKey);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_FILE, "File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ApiException(ErrorCode.INVALID_FILE, "File size exceeds maximum allowed 5 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new ApiException(ErrorCode.INVALID_FILE, "Invalid file type. Allowed: PDF, JPEG, PNG");
        }
    }
}

