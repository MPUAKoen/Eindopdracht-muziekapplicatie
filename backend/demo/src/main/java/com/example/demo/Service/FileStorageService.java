package com.example.demo.Service;  
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileStorageService {

    private final Path uploadDir;

    public FileStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    public List<String> storeFiles(List<MultipartFile> files) {
        return files.stream().map(file -> {
            String filename = StringUtils.cleanPath(file.getOriginalFilename());
            try {
                Path target = this.uploadDir.resolve(filename);
                Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
                return filename;
            } catch (IOException ex) {
                throw new RuntimeException("Failed to store file " + filename, ex);
            }
        }).collect(Collectors.toList());
    }

    public Path load(String filename) {
        return uploadDir.resolve(filename).normalize();
    }
}
