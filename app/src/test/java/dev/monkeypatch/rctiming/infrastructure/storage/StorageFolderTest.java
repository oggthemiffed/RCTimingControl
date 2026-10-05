package dev.monkeypatch.rctiming.infrastructure.storage;

import dev.monkeypatch.rctiming.persistence.DatabaseProperties;
import dev.monkeypatch.rctiming.persistence.vendor.DatabaseVendor;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StorageFolderTest {

    private final DatabaseProperties database =
            new DatabaseProperties(DatabaseVendor.SQLITE, Path.of("/var/lib/rctimingcontrol"), List.of("db/migration"), 4);

    @Test
    void uploadsSitBesideTheDatabaseUnlessAPathIsSet() {
        assertThat(StorageFolder.resolve("", database)).isEqualTo(Path.of("/var/lib/rctimingcontrol/uploads"));
        assertThat(StorageFolder.resolve(null, database)).isEqualTo(Path.of("/var/lib/rctimingcontrol/uploads"));
        assertThat(StorageFolder.resolve("/app/data/uploads", database)).isEqualTo(Path.of("/app/data/uploads"));
    }
}
