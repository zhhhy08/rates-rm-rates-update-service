package com.hertz.api.helpers;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.hertz.rates.common.utils.HertzException;
import com.hertz.rates.common.utils.config.PropertyGroup;
import com.hertz.rates.common.utils.jdbc.ConnectionMgr;
import com.hertz.rates.common.utils.jdbc.DbDataUtilities;
import com.hertz.rates.common.utils.logging.HertzLogger;

/**
 * RUM - Support to write Purge messages to DB.
 * 
 *
 */
public class RumPurgeMessageHelper {

    final static HertzLogger logger = new HertzLogger(RumPurgeMessageHelper.class);

    // Strings used in Config Data
    private final static String RATES_DB_CONNECTION = "Oracle";
    private static final String PROC_NAME = "PurgeRumMessagesProc";
    private static final String TXN_NAME = "PurgeRumMessages";

    protected PropertyGroup configDataProperties = null;

    /**
     * Instantiate passing in ConfigData properties for the RatePlanDataService this helper will be
     * use to lookup per-transaction settings.
     * 
     * @param properties a config data property group for the RatePlanDataService (from the
     * DataServices portion of config data)
     */
    public RumPurgeMessageHelper(PropertyGroup properties) {

        this.configDataProperties = properties;
    }

    /**
     * Subclasses must override this to return a stored proc name/params string for the helper.
     * This is used in looking up the stored proc to call for this transaction that this helper is working on.
     * 
     * @return A string specifying the proc name and param positions (with ?s) for this helper's transaction
     */
    protected String getStoredProcString() {

        return configDataProperties.getPropertyValue(PROC_NAME);
    }

    /**
     * Subclasses must override this to return a logical transaction name for the helper.
     * This is used in looking up config data stuff like connection properties, playback/record
     * properties, etc. for the transaction this helper is working on.
     * 
     * @return A logical transaction name for this txn that matches txn names in Config Data
     */
    protected String getTransactionName() {

        return TXN_NAME;
    }

    public final void doDBCall(String capturedDateTime) throws HertzException, SQLException {

        Connection conn = null;
        CallableStatement statement = null;

        ResultSet rs = null;
        
        // This uses a connection mgr which can be configured to
        // allow running disconnected from the db (using playback),
        // running within WebSphere and its pool of connections,
        // or running standalone outside of WebSphere.

        try {
            String txnName = this.getTransactionName(); // subclasses implement this

            conn = ConnectionMgr.getConnection(RATES_DB_CONNECTION, txnName);
            statement = conn.prepareCall(this.getStoredProcString());

            registerParams(statement, capturedDateTime); // subclasses might extend this

            DbDataUtilities.executeStoredProcedure(txnName, statement); // handles retry logic as needed

            //rs = statement.getResultSet();       	
        }
        finally {

            //There is no result set associated with this procedure.
            if (rs != null) {
                rs.close();
            }

            ConnectionMgr.closeStatement(statement);
            ConnectionMgr.closeConnection(conn);
        }
    }

    /**
     * Registers the input parameters to the stored procedure's callable statement.
     * 
     * @param statement The callable statement to register parms into
     * @param req The rates request to get the parameters out of
     * @throws SQLException
     */
    protected void registerParams(CallableStatement statement, String capturedDateTime) throws SQLException, HertzException {

        int col = 1;

        DbDataUtilities.setStringParam(statement, col++, capturedDateTime);
    }
}

/*
 *************************************************************
 * Change History:
 *
 * $Log$
 *
 *************************************************************
 *
 * Copyright (C) 2014 The Hertz Corporation
 *
 * All Rights Reserved. (Unpublished.)
 * 
 * The information contained herein is confidential and
 * proprietary to The Hertz Corporation and may not be
 * duplicated, disclosed to third parties, or used for any
 * purpose not expressly authorized by it.  Any unauthorized
 * use, duplication or disclosure is prohibited by law.
 * 
 *************************************************************
 */
