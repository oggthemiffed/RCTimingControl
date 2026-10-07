package dev.monkeypatch.rctiming.domain.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an endpoint that changes data and whose change is recorded: who did it, when and what changed.
 * {@link #value()} names where, for example {@code "audit_log"} or {@code "official_audit_log"}.
 *
 * <p>It is a promise the endpoint's service keeps, not something that records anything by itself.
 * {@code AuditCoverageIT} fails when a POST, PUT, PATCH or DELETE endpoint is neither marked with this nor
 * listed, with a reason, in {@code audit-allowlist.txt}, so a new endpoint cannot ship unrecorded by accident.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {
    /** The table or tables the change is recorded in. */
    String value();
}
