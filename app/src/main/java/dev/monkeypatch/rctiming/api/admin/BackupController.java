package dev.monkeypatch.rctiming.api.admin;

import dev.monkeypatch.rctiming.backup.BackupFile;
import dev.monkeypatch.rctiming.backup.BackupService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Database backups for the admin page (#22): list them, and take one now. */
@RestController
@RequestMapping("/api/v1/admin/backups")
@PreAuthorize("hasRole('ADMIN')")
public class BackupController {

    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    public record BackupsDto(String directory, List<BackupFile> backups) {
    }

    @GetMapping
    public BackupsDto list() {
        return new BackupsDto(backupService.directory().toAbsolutePath().toString(), backupService.list());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BackupFile backupNow() {
        return backupService.backup("manual");
    }
}
