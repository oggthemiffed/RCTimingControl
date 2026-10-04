package dev.monkeypatch.rctiming.persistence;

import org.jooq.ConnectionProvider;
import org.jooq.exception.DataAccessException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Gives jOOQ the transaction's connection inside a Spring transaction, so a query sees that
 * transaction's own writes, and a read-pool connection otherwise, so read queries never wait
 * behind the single writer.
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
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            return DataSourceUtils.getConnection(writeDataSource);
        }
        try {
            return readDataSource.getConnection();
        } catch (SQLException e) {
            throw new DataAccessException("Can't get a read connection", e);
        }
    }

    @Override
    public void release(Connection connection) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            DataSourceUtils.releaseConnection(connection, writeDataSource);
            return;
        }
        try {
            connection.close();
        } catch (SQLException e) {
            throw new DataAccessException("Can't release a read connection", e);
        }
    }
}
