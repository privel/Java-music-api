package org.example.musicapi.controllers;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/music")
public class MusicController {

    private final Path musicDir = Paths.get("src/main/resources/static/music");

    // Получение списка всех MP3 файлов с учетом подпапок
    @GetMapping("/list")
    public List<String> listFiles() throws IOException {
        try (var stream = Files.walk(musicDir)) {
            return stream.filter(Files::isRegularFile)
                    .map(musicDir::relativize)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        }
    }

    // Воспроизведение MP3-файла с учетом вложенных папок
    @GetMapping("/play")
    public ResponseEntity<Resource> getFile(@RequestParam String path) throws IOException {
        return serveFile(path);
    }

    // Потоковая передача MP3-файла
    @GetMapping("/stream")
    public ResponseEntity<Resource> streamMusic(@RequestParam String path) throws IOException {
        return serveFile(path);
    }

    private ResponseEntity<Resource> serveFile(String path) throws IOException {
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        Path filePath = musicDir.resolve(decodedPath).normalize();

        if (!filePath.startsWith(musicDir) || !Files.exists(filePath) || !Files.isReadable(filePath)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new UrlResource(filePath.toUri());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filePath.getFileName() + "\"")
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(resource);
    }
}
