package com.example.demo.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    private Path tempDir;
    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("filestorage-test");
        fileStorageService = new FileStorageService(tempDir.toString());
    }

    @Test
    void storeFiles_shouldStoreFileOnDisk_andReturnFilenames() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "pdfFiles",
                "lesson.pdf",
                "application/pdf",
                "Dummy content".getBytes()
        );

        List<String> storedNames = fileStorageService.storeFiles(List.of(file));

        assertEquals(1, storedNames.size());
        assertEquals("lesson.pdf", storedNames.get(0));

        Path storedPath = fileStorageService.load("lesson.pdf");
        assertTrue(Files.exists(storedPath));
    }

    @Test
    void storeFiles_whenCopyFails_shouldThrowRuntimeException() {
        MockMultipartFile badFile = new MockMultipartFile(
                "pdfFiles",
                "broken.pdf",
                "application/pdf",
                "x".getBytes()
        ) {
            @Override
            public InputStream getInputStream() throws IOException {
                throw new IOException("Simulated I O error");
            }
        };

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> fileStorageService.storeFiles(List.of(badFile))
        );

        assertTrue(ex.getMessage().toLowerCase().contains("failed"));
    }

    @Test
    void load_shouldResolvePathInsideRootDirectory() {
        Path path = fileStorageService.load("somefile.pdf");

        
        assertTrue(path.toString().endsWith("somefile.pdf"));
        assertTrue(path.toString().contains(tempDir.toString()));
    }
}
