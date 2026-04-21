package dev.monkeypatch.rctiming.infrastructure.storage;

public interface ObjectStorageService {
    /**
     * Uploads content under the given key and returns a retrievable URL.
     *
     * @param key         object key, e.g. "club-logos/42.png"
     * @param content     raw bytes
     * @param contentType MIME type, e.g. "image/png"
     * @return retrievable URL (public or signed)
     */
    String upload(String key, byte[] content, String contentType);
}
