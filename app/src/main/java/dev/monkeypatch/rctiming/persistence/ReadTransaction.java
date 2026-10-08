package dev.monkeypatch.rctiming.persistence;

import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A read-only transaction on the read pool: every query in it sees the same committed snapshot, and it
 * never waits for the single write connection. Use it for read-side queries that run several statements.
 *
 * <p>Called inside a write transaction, the queries still use that transaction's connection and see its
 * uncommitted writes ({@link TransactionRoutingConnectionProvider}); no read connection is taken. A read
 * that must wait for a write in progress on another thread needs plain {@code @Transactional(readOnly = true)}
 * instead, which runs on the write connection.
 *
 * <p>The read pool's connections refuse writes with {@code PRAGMA query_only}; sqlite-jdbc ignores the JDBC
 * read-only flag the transaction sets, which is only a hint.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Transactional(transactionManager = DatabaseConfig.READ_TRANSACTION_MANAGER, readOnly = true)
public @interface ReadTransaction {
}
