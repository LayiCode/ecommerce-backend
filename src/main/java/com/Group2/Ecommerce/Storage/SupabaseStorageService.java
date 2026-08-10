package com.Group2.Ecommerce.Storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
public class SupabaseStorageService {

    private final RestClient restClient;
    private final String url;
    private final String bucket;

    public SupabaseStorageService(RestClient.Builder builder,
                                  @Value("${supabase.url}") String url,
                                  @Value("${supabase.service-role-key}") String serviceRoleKey,
                                  @Value("${supabase.bucket:product-images}") String bucket) {
        this.url = url;
        this.bucket = bucket;
        this.restClient = builder
                .defaultHeader("Authorization", "Bearer " + serviceRoleKey)
                .defaultHeader("apikey", serviceRoleKey)
                .build();
    }

    public String upload(MultipartFile file) {
        String fileName = generateFileName(file.getOriginalFilename());
        String contentType = file.getContentType() != null ? file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Could not read the uploaded file", e);
        }

        try {
            restClient.post()
                    .uri("{base}/storage/v1/object/{bucket}/{name}", url, bucket, fileName)
                    .contentType(MediaType.valueOf(contentType))
                    .body(bytes)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.error("Supabase storage upload failed: {}", e.getStatusCode(), e);
            throw new IllegalStateException("Image upload to storage failed", e);
        }

        return url + "/storage/v1/object/public/" + bucket + "/" + fileName;
    }

    private String generateFileName(String originalName) {
        String extension = "";
        if (originalName != null) {
            int dot = originalName.lastIndexOf('.');
            if (dot >= 0 && dot < originalName.length() - 1) {
                extension = originalName.substring(dot).toLowerCase();
            }
        }
        return UUID.randomUUID() + extension;
    }
}
