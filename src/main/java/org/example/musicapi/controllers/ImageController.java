package org.example.musicapi.controllers;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.*;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/images")
public class ImageController {

    private final String BASE_PATH = "src/main/resources/static/cover";

    // Метод для получения изображения по жанру и имени файла
    @GetMapping("/{genre}/{filename:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String genre, @PathVariable String filename) {
        try {
            Path filePath = Paths.get(BASE_PATH, genre, filename).toAbsolutePath().normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            // Определяем MIME-тип файла
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream"; // Фallback для неизвестных типов
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }


    // Метод для получения списка всех изображений в указанной папке (жанре)
    @GetMapping("/{genre}")
    public ResponseEntity<List<String>> getAllImages(@PathVariable String genre) {
        return getImagesFromFolder(genre);
    }

    // Метод для получения списка всех изображений из всех жанров
    @GetMapping("/all")
    public ResponseEntity<Map<String, List<String>>> getAllImagesFromAllGenres() {
        try {
            Path baseFolder = Paths.get(BASE_PATH).toAbsolutePath().normalize();
            if (!Files.exists(baseFolder) || !Files.isDirectory(baseFolder)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            Map<String, List<String>> allImages = new HashMap<>();

            try (Stream<Path> genreFolders = Files.list(baseFolder)) {
                genreFolders
                        .filter(Files::isDirectory)
                        .forEach(folder -> allImages.put(folder.getFileName().toString(), getImagesFromFolder(folder.getFileName().toString()).getBody()));

                return ResponseEntity.ok(allImages);
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Вспомогательный метод для получения списка файлов из папки
    private ResponseEntity<List<String>> getImagesFromFolder(String genre) {
        try {
            Path folderPath = Paths.get(BASE_PATH, genre).toAbsolutePath().normalize();
            if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            try (Stream<Path> paths = Files.list(folderPath)) {
                List<String> fileNames = paths
                        .filter(Files::isRegularFile)
                        .map(Path::getFileName)
                        .map(Path::toString)
                        .collect(Collectors.toList());

                return ResponseEntity.ok(fileNames);
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
