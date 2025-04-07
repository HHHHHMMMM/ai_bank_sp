package org.ruoyi.knowledgegraph.connector;

import java.util.List;
import java.util.Map;

/**
 * Generic interface for database connectors
 */
public interface DatabaseConnector {

    /**
     * Connect to the database
     * @return true if the connection was successful
     */
    boolean connect();

    /**
     * Execute a query that returns multiple rows
     * @param query the SQL query to execute
     * @param params the parameters for the query
     * @return a list of maps with the query results
     */
    List<Map<String, Object>> executeQuery(String query, Map<String, Object> params);

    /**
     * Execute a query that returns a single row
     * @param query the SQL query to execute
     * @param params the parameters for the query
     * @return a map with the query results
     */
    Map<String, Object> executeSingleQuery(String query, Map<String, Object> params);

    /**
     * Execute a statement that doesn't return a result
     * @param statement the SQL statement to execute
     * @param params the parameters for the statement
     * @return the number of affected rows
     */
    int executeUpdate(String statement, Map<String, Object> params);

    /**
     * Close the database connection
     */
    void close();

    /**
     * Get the name of the connector
     * @return the connector name
     */
    String getName();
}
