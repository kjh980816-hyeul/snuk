package com.chzikon.global.upload;

import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** 이미지 파일 로컬 저장 — /uploads/** 로 서빙. 운영은 UPLOAD_DIR env 로 위치 지정(카이도쿠 교훈: nginx 서빙 경로와 일치 필수). */
@Slf4j
@Component
public class FileStorageService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");

    /** 무료소스 자료(항목 19) — 이미지 + 압축/오디오/영상/문서. 실행파일류는 차단. */
    private static final Set<String> ALLOWED_RESOURCE_EXT = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "svg",
            "zip", "7z", "rar",
            "mp3", "wav", "ogg",
            "mp4", "webm", "mov",
            "pdf", "txt", "psd");

    private final Path uploadDir;

    public FileStorageService(@Value("${app.upload.dir}") String dir) {
        this.uploadDir = Path.of(dir).toAbsolutePath().normalize();
    }

    /** 이미지 저장 후 공개 경로(/uploads/파일명) 반환. */
    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "파일이 비어 있습니다.");
        }
        String ext = extOf(file.getOriginalFilename());
        String contentType = file.getContentType();
        if (!ALLOWED_EXT.contains(ext) || contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미지 파일(jpg/png/gif/webp)만 업로드할 수 있습니다.");
        }
        try {
            Files.createDirectories(uploadDir);
            String name = UUID.randomUUID() + "." + ext;
            Files.copy(file.getInputStream(), uploadDir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + name;
        } catch (IOException e) {
            log.error("file store failed", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    /** 무료소스 파일 저장(zip/오디오/영상/문서 허용, 원본 파일명 유지 다운로드를 위해 확장자 보존). */
    public String storeResourceFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "파일이 비어 있습니다.");
        }
        String ext = extOf(file.getOriginalFilename());
        if (!ALLOWED_RESOURCE_EXT.contains(ext)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "허용되지 않는 파일 형식입니다. (이미지/zip/7z/rar/mp3/wav/ogg/mp4/webm/mov/pdf/txt/psd)");
        }
        try {
            Files.createDirectories(uploadDir);
            String name = UUID.randomUUID() + "." + ext;
            Files.copy(file.getInputStream(), uploadDir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + name;
        } catch (IOException e) {
            log.error("resource file store failed", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    /** 우리가 저장한 업로드 파일이면 삭제(외부 URL 은 무시). */
    public void deleteIfLocal(String url) {
        if (url == null || !url.startsWith("/uploads/")) {
            return;
        }
        String name = url.substring("/uploads/".length());
        if (name.contains("/") || name.contains("\\") || name.contains("..")) {
            return;
        }
        try {
            Files.deleteIfExists(uploadDir.resolve(name));
        } catch (IOException e) {
            log.warn("old upload delete failed: {}", e.getMessage());
        }
    }

    private static String extOf(String filename) {
        if (filename == null) {
            return "";
        }
        int i = filename.lastIndexOf('.');
        return i < 0 ? "" : filename.substring(i + 1).toLowerCase(Locale.ROOT);
    }
}
