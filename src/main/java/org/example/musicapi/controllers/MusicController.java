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

    // Получение списка всех MP3 файлов
    @GetMapping("/list")
    public List<String> listFiles() throws IOException {
        return Files.list(musicDir)
                .filter(Files::isRegularFile)
                .map(Path::getFileName)
                .map(Path::toString)
                .collect(Collectors.toList());
    }

    // Воспроизведение MP3-файла (inline)
    @GetMapping("/play/{filename}")
    public ResponseEntity<Resource> getFile(@PathVariable String filename) throws IOException {
        String decodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);
        Path filePath = musicDir.resolve(decodedFilename);
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() && resource.isReadable()) {
            System.out.println("Serving file: " + decodedFilename);
            System.out.println("File size: " + Files.size(filePath));

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("audio/mpeg"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + decodedFilename + "\"")
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes") // Позволяет потоковое воспроизведение
                    .body(resource);
        } else {
            System.out.println("File not found: " + decodedFilename);
            return ResponseEntity.notFound().build();
        }
    }

    // Потоковая передача MP3
    @GetMapping("/stream/{filename}")
    public ResponseEntity<Resource> streamMusic(@PathVariable String filename) {
        try {
            String decodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);
            Path filePath = musicDir.resolve(decodedFilename);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                System.out.println("File not found: " + decodedFilename);
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("audio/mpeg"))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes") // Позволяет потоковое воспроизведение
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}