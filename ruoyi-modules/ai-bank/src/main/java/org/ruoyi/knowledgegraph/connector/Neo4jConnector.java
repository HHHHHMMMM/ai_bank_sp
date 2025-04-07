package org.ruoyi.knowledgegraph.connector;

import java.util.List;

public interface Neo4jConnector extends DatabaseConnector {

    /**
     * Create a constraint in the graph database
     * @param label the node label
     * @param property the property name
     * @return true if the constraint was created successfully
     */
    boolean createConstraint(String label, String property);

    /**
     * Create a node key constraint in the graph database
     * @param label the node label
     * @param properties the property names
     * @return true if the constraint was created successfully
     */
    boolean createNodeKeyConstraint(String label, String... properties);

    /**
     * Clear all data from the database
     * @return true if the operation was successful
     */
    boolean clearDatabase();

    /**
     * Check if a constraint exists
     * @param constraintName the name of the constraint
     * @return true if the constraint exists
     */
    boolean constraintExists(String constraintName);

    /**
     * Drop a constraint
     * @param constraintName the name of the constraint
     * @return true if the constraint was dropped successfully
     */
    boolean dropConstraint(String constraintName);

    /**
     * List all constraints in the database
     * @return a list of constraint names
     */
    List<String> listConstraints();
}