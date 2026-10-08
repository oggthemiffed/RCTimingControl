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
    private final DataSource readTransactionDataSource;

    /**
     * @param readTransactionDataSource the read pool as the {@link ReadTransaction} manager binds it
     */
    TransactionRoutingConnectionProvider(DataSource writeDataSource, DataSource readDataSource,
                                         DataSource readTransactionDataSource) {
        this.writeDataSource = writeDataSource;
        this.readDataSource = readDataSource;
        this.readTransactionDataSource = readTransactionDataSource;
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
        if (TransactionSynchronizationManager.hasResource(readTransactionDataSource)) {
            return readTransactionDataSource;
        }
        // Only a transaction manager over neither pool gets here: refuse rather than guess which one it meant
        throw new IllegalStateException("A transaction is open on neither the write nor the read pool; bound: "
                + TransactionSynchronizationManager.getResourceMap().keySet());
    }
}
