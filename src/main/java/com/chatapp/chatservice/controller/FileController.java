package com.chatapp.chatservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "http://localhost:4200")
public class FileController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        Map<String, String> response = new HashMap<>();
        
        try {
            // 1. Directory check karein aur na ho toh banayein
            Path copyLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(copyLocation)) {
                Files.createDirectories(copyLocation);
            }

            // 2. Unique filename banayein (UUID + original name)
            // Isse kabhi bhi do files ka naam same nahi hoga
            String originalFileName = file.getOriginalFilename();
            String fileName = UUID.randomUUID().toString() + "_" + originalFileName;

            // 3. File ko physically folder mein save karein
            Path targetLocation = copyLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // 4. Relative URL return karein jo Database mein jayega
            // Hum '/uploads/...' bhej rahe hain kyunki WebConfig isse handle kar raha hai
            String fileUrl = "/uploads/" + fileName;
            
            response.put("url", fileUrl);
            response.put("status", "success");
            
            return ResponseEntity.ok(response);

        } catch (IOException ex) {
            response.put("error", "Could not store file: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}