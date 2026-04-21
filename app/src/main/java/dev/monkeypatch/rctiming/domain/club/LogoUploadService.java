package dev.monkeypatch.rctiming.domain.club;

import dev.monkeypatch.rctiming.infrastructure.storage.ObjectStorageService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Service
@Transactional
public class LogoUploadService {

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/png", "image/jpeg", "image/webp", "image/svg+xml");

    private static final long MAX_BYTES = 2 * 1024 * 1024L; // 2 MB

    private final ObjectStorageService storage;
    private final ClubProfileRepository clubProfileRepository;

    public LogoUploadService(ObjectStorageService storage,
                              ClubProfileRepository clubProfileRepository) {
        this.storage = storage;
        this.clubProfileRepository = clubProfileRepository;
    }

    public String uploadLogo(Long clubProfileId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file part is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Unsupported content type: " + contentType + ". Allowed: " + ALLOWED_CONTENT_TYPES);
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("File too large: max " + MAX_BYTES + " bytes");
        }
        ClubProfile profile = clubProfileRepository.findById(clubProfileId)
                .orElseThrow(() -> new EntityNotFoundException("Club profile not found: " + clubProfileId));

        String extension = switch (contentType.toLowerCase()) {
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/webp" -> "webp";
            case "image/svg+xml" -> "svg";
            default -> "bin";
        };
        String key = "club-logos/" + clubProfileId + "-" + System.currentTimeMillis() + "." + extension;

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read uploaded file: " + e.getMessage(), e);
        }

        String url = storage.upload(key, bytes, contentType);
        profile.setLogoUrl(url);
        clubProfileRepository.save(profile);
        return url;
    }
}
