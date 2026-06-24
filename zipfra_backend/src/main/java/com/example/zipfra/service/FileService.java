package com.example.zipfra.service;

import com.example.zipfra.exception.ApiException;
import com.example.zipfra.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class FileService {

    @Value("${file.upload-dir:uploads/profiles}")
    private String uploadDir;

    /**
     * 프로필 이미지를 서버 로컬 스토리지에 저장하고, 웹에서 접근 가능한 상대 URL을 반환한다.
     */
    public String storeProfileImage(MultipartFile file) {
        if (file.isEmpty()) {
            return null;
        }

        try {
            // 원본 파일명 (공백 제거 및 안전한 이름으로 변환)
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            if (originalFilename.contains("..")) {
                throw new ApiException(ErrorCode.INVALID_PARAM); // 간단히 예외 처리
            }

            // 확장자 추출
            String ext = "";
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex >= 0) {
                ext = originalFilename.substring(dotIndex);
            }

            // UUID를 사용한 고유 파일명 생성
            String storedFilename = UUID.randomUUID().toString() + ext;

            // 절대 경로 혹은 상대 경로 변환
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

            // 디렉토리가 없으면 생성
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path targetLocation = uploadPath.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // 클라이언트에서 접근할 수 있는 URL 경로 반환 (WebConfig에 매핑됨)
            return "/uploads/profiles/" + storedFilename;

        } catch (IOException ex) {
            log.error("Could not store file: " + file.getOriginalFilename(), ex);
            throw new RuntimeException("Could not store file. Please try again!", ex);
        }
    }
}
