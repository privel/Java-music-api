package org.example.musicapi.controllers;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/music")
public class MusicController {

    private final Path musicDir = Paths.get("src/main/resources/static/music");

    // Получение списка всех MP3 файлов, сгруппированных по папкам (жанрам)
    @GetMapping("/list")
    public ResponseEntity<Map<String, List<String>>> listFiles() throws IOException {
        if (!Files.exists(musicDir) || !Files.isDirectory(musicDir)) {
            return ResponseEntity.notFound().build();
        }

        Map<String, List<String>> categorizedFiles = new HashMap<>();

        try (Stream<Path> genreFolders = Files.list(musicDir)) {
            genreFolders
                    .filter(Files::isDirectory)
                    .forEach(folder -> {
                        try {
                            List<String> files = getFilesFromFolder(folder);
                            categorizedFiles.put(folder.getFileName().toString(), files);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }

        return ResponseEntity.ok(categorizedFiles);
    }


    @GetMapping("/play")
    public ResponseEntity<Resource> getFile(@RequestParam String path) throws IOException {
        return serveFile(path);
    }

    // Потоковая передача MP3-файла
    @GetMapping("/stream")
    public ResponseEntity<Resource> streamMusic(@RequestParam String path) throws IOException {
        return serveFile(path);
    }

    // Вспомогательный метод для обработки файлов
    private ResponseEntity<Resource> serveFile(String path) throws IOException {
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        Path filePath = musicDir.resolve(decodedPath).normalize();

        if (!filePath.startsWith(musicDir) || !Files.exists(filePath) || !Files.isReadable(filePath)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new UrlResource(filePath.toUri());

        // Кодирование имени файла в UTF-8
        String encodedFilename = URLEncoder.encode(filePath.getFileName().toString(), StandardCharsets.UTF_8)
                .replace("+", "%20"); // Фикс для пробелов

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + encodedFilename + "\"")
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(resource);
    }

    // Вспомогательный метод для получения списка файлов из конкретной папки
    private List<String> getFilesFromFolder(Path folderPath) throws IOException {
        try (Stream<Path> files = Files.list(folderPath)) {
            return files
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        }
    }
}
