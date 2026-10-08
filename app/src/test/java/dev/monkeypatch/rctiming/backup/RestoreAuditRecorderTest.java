package dev.monkeypatch.rctiming.backup;

import dev.monkeypatch.rctiming.domain.audit.Actor;
import dev.monkeypatch.rctiming.domain.audit.AuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** A restore leaves a note; the next start records it once and removes it (#139). */
class RestoreAuditRecorderTest {

    @TempDir Path dataDirectory;

    @Test
    void aPendingRestoreIsRecordedAsTheCommandLineUserAndTheNoteRemoved() throws Exception {
        AuditService audit = Mockito.mock(AuditService.class, Mockito.RETURNS_DEEP_STUBS);
        RestoreNote.write(dataDirectory, Path.of("/backups/rctiming-day.db"));

        new RestoreAuditRecorder(dataDirectory, audit).recordPendingRestore();

        verify(audit).entry(eq(Actor.cli(System.getProperty("user.name", "unknown"))), eq("DATABASE_RESTORED"));
        assertThat(dataDirectory.resolve(RestoreNote.FILE_NAME)).doesNotExist();
    }

    @Test
    void withoutANoteNothingIsRecorded() {
        AuditService audit = Mockito.mock(AuditService.class, Mockito.RETURNS_DEEP_STUBS);

        new RestoreAuditRecorder(dataDirectory, audit).recordPendingRestore();

        verify(audit, never()).entry(any(), any());
    }

    @Test
    void aFailureToRecordLeavesTheNoteForTheNextStartAndDoesNotThrow() throws Exception {
        AuditService audit = Mockito.mock(AuditService.class);
        Mockito.when(audit.entry(any(), any())).thenThrow(new IllegalStateException("database busy"));
        RestoreNote.write(dataDirectory, Path.of("/backups/x.db"));

        new RestoreAuditRecorder(dataDirectory, audit).recordPendingRestore();

        assertThat(Files.exists(dataDirectory.resolve(RestoreNote.FILE_NAME))).isTrue();
    }
}
