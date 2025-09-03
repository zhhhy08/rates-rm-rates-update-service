package com.hertz.api.helpers;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.hertz.rates.common.utils.HertzException;
import com.hertz.rates.common.utils.logging.HertzLogger;

import oracle.jdbc.OracleCallableStatement;

import com.zaxxer.hikari.pool.HikariProxyCallableStatement;


/**
 * Helps sequence thru result sets, especially since some tnxs skip some result
 * sets, but they must be sequenced thru via JDBC calls even if txns don't want to know about them.
 * 
 * @author Venkat
 */
public class ResultSetHelper {
	
    public final static HertzLogger logger = new HertzLogger( ResultSetHelper.class ); 
    
    private CallableStatement statement;

    /**
     * Creates an instance of this manager
     * 
     * @param statement
     */
    public ResultSetHelper( CallableStatement statement ) {
        this.statement = statement;
    }

    
    /**
     * Gets the specified result set.
     */
    public ResultSet getResultSet( int resultSetType ) throws SQLException, HertzException {
    	
    	ResultSet resultSet = null;
    			
    	// Assuming HikariProxyCallableStatement is the actual type
    	HikariProxyCallableStatement proxyStatement = (HikariProxyCallableStatement) statement;

    	// Unwrap to access the underlying statement (might be OracleCallableStatement)
    	OracleCallableStatement oracleStatement = (OracleCallableStatement) proxyStatement.unwrap(OracleCallableStatement.class);

    	// Use oracleStatement for Oracle-specific functionalities (if unwrapping is successful)
    	if (oracleStatement != null) {
    		resultSet =  oracleStatement.getCursor(resultSetType);
    	} else {
    		resultSet =  proxyStatement.getResultSet();
    	}       
        
        return resultSet;
    }
}

