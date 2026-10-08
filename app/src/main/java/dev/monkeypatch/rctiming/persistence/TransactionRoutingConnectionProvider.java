package dev.monkeypatch.rctiming.persistence;

import org.jooq.ConnectionProvider;
import org.jooq.exception.DataAccessException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Gives jOOQ the write transaction's connection inside one, so a query sees that transaction's own
 * writes. Otherwise it gives the read transaction's connection inside a {@link ReadTransaction}, so
 * its queries share one snapshot, or a fresh read-pool connection. Reads therefore never wait behind
 * the single writer unless they run in a write-pool transaction.
 */
class TransactionRoutingConnectionProvider implements ConnectionProvider {

    private final DataSource writeDataSource;
    private final DataSource readDataSource;

    TransactionRoutingConnectionProvider(DataSource writeDataSource, DataSource readDataSource) {
        this.writeDataSource = writeDataSource;
        this.readDataSource = readDataSource;
    }

    @Override
    public Connection acquire() {
        DataSource bound = boundDataSource();
        if (bound != null) {
            return DataSourceUtils.getConnection(bound);
        }
        try {
            return readDataSource.getConnection();
        } catch (SQLException e) {
            throw new DataAccessException("Can't get a read connection", e);
        }
    }

    @Override
    public void release(Connection connection) {
        DataSource bound = boundDataSource();
        if (bound != null) {
            DataSourceUtils.releaseConnection(connection, bound);
            return;
        }
        try {
            connection.close();
        } catch (SQLException e) {
            throw new DataAccessException("Can't release a read connection", e);
        }
    }

    /** The pool whose transaction is open on this thread, the write pool first, or null outside one. */
    private DataSource boundDataSource() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            return null;
        }
        if (TransactionSynchronizationManager.hasResource(writeDataSource)) {
            return writeDataSource;
        }
        if (TransactionSynchronizationManager.hasResource(readDataSource)) {
            return readDataSource;
        }
        return null;
    }
}
