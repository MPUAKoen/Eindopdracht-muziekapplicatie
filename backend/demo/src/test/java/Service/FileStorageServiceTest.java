package Service;

import com.example.demo.Service.FileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * ✅ Unit tests for FileStorageService
 */
class FileStorageServiceTest {

    private FileStorageService fileStorageService;
    private Path testUploadDir;

    @BeforeEach
    void setUp() throws IOException {
        // Create a temporary directory for testing
        testUploadDir = Files.createTempDirectory("upload-test-dir");
        fileStorageService = new FileStorageService(testUploadDir.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        // Delete files & folder created during tests
        if (Files.exists(testUploadDir)) {
            Files.walk(testUploadDir)
                    .sorted((a, b) -> b.compareTo(a)) // delete files first
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {}
                    });
        }
    }

    // ✅ 1. storeFiles should save all files and return filenames
    @Test
    void storeFiles_ShouldSaveFiles_AndReturnFilenames() throws IOException {
        MockMultipartFile file1 = new MockMultipartFile("files", "file1.pdf", "application/pdf", "Hello".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "file2.pdf", "application/pdf", "World".getBytes());

        List<String> stored = fileStorageService.storeFiles(List.of(file1, file2));

        assertThat(stored).containsExactly("file1.pdf", "file2.pdf");
        assertThat(Files.exists(testUploadDir.resolve("file1.pdf"))).isTrue();
        assertThat(Files.exists(testUploadDir.resolve("file2.pdf"))).isTrue();
    }

    // ✅ 2. storeFiles should throw RuntimeException when IOException occurs
    @Test
    void storeFiles_WhenIOException_ShouldThrowRuntimeException() {
        assertThatThrownBy(() -> {
            // Use an invalid path to force IOException
            FileStorageService badService = new FileStorageService("Z:/definitely_invalid_folder_path_xyz");
            MockMultipartFile badFile = new MockMultipartFile("files", "bad.pdf", "application/pdf", "oops".getBytes());
            badService.storeFiles(List.of(badFile));
        }).isInstanceOf(RuntimeException.class);
    }

    // ✅ 3. load should return a normalized absolute path
    @Test
    void load_ShouldReturnNormalizedPath() {
        Path loaded = fileStorageService.load("test.pdf");

        assertThat(loaded.toString()).contains("test.pdf");
        assertThat(loaded.isAbsolute()).isTrue();
    }
}
