package com.hertz.api.helpers;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import com.hertz.rates.common.utils.HertzException;
import com.hertz.rates.common.utils.config.PropertyGroup;
import com.hertz.rates.common.utils.jdbc.ConnectionMgr;
import com.hertz.rates.common.utils.jdbc.DbDataUtilities;
import com.hertz.rates.common.utils.logging.HertzLogger;
import com.hertz.api.corebusiness.logging.RumWebStatsBean;

import oracle.jdbc.OracleTypes;

/**
 * RUM Webservices data helper for historical data and results.
 * 
 *
 */
public class GetRumWebDataHelper {

    final static HertzLogger logger = new HertzLogger(GetRumWebDataHelper.class);
    
    // Strings used in Config Data
    private final static String RATES_DB_CONNECTION = "Oracle";
    private static final String GET_HISTORICAL_DATA_PROC_NAME = "getWebHistoricalData";
    private static final String TXN_NAME = "RumWebHistoricaData";

    private static final String DATE_FILLER = "000000";

    protected PropertyGroup configDataProperties = null;

    /**
     * Instantiate passing in ConfigData properties for the RatePlanDataService this helper will be
     * use to lookup per-transaction settings.
     * 
     * @param properties a config data property group for the RatePlanDataService (from the
     * DataServices portion of config data)
     */
    public GetRumWebDataHelper(PropertyGroup properties) {

        this.configDataProperties = properties;
    }

    /**
     * Subclasses must override this to return a stored proc name/params string for the helper.
     * This is used in looking up the stored proc to call for this transaction that this helper is working on.
     * 
     * @return A string specifying the proc name and param positions (with ?s) for this helper's transaction
     */
    protected String getStoredProcString() {

        return configDataProperties.getPropertyValue(GET_HISTORICAL_DATA_PROC_NAME);
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

    public final RumWebStatsBean[] getHistoricalWebData(int hertzDateTime) throws HertzException, SQLException {

        Connection conn = null;
        CallableStatement statement = null;
        RumWebStatsBean[] beans = null;
        ResultSet rs = null;
        
        // This uses a connection mgr which can be configured to
        // allow running disconnected from the db (using playback),
        // running within WebSphere and its pool of connections,
        // or running standalone outside of WebSphere.
        
        try {
            String txnName = this.getTransactionName(); // subclasses implement this

            conn = ConnectionMgr.getConnection(RATES_DB_CONNECTION, txnName);
            statement = conn.prepareCall(this.getStoredProcString());

            registerParams(statement, hertzDateTime); // subclasses might extend this

            DbDataUtilities.executeStoredProcedure(txnName, statement); // handles retry logic as needed

            ResultSetHelper rsHelper = new ResultSetHelper(statement);

            beans = processResultSet(rsHelper);
        }
        finally {
            ConnectionMgr.closeStatement(statement);
            ConnectionMgr.closeConnection(conn);
        }

        return beans;
    }

    protected RumWebStatsBean[] processResultSet(ResultSetHelper rsHelper) throws SQLException, HertzException {

        ArrayList<RumWebStatsBean> beansFound = new ArrayList<RumWebStatsBean>();

        ResultSet rs = rsHelper.getResultSet(3);
        
        try {
            while (rs.next()) {
                int col = 1;
                String transId = DbDataUtilities.getStringValue(rs, col++);
                Long startTime = DbDataUtilities.getLongValue(rs, col++);

                RumWebStatsBean bean = new RumWebStatsBean(transId, startTime.longValue());

                bean.setTransactionEndTime((DbDataUtilities.getLongValue(rs, col++)).longValue());
                bean.setRetrieveLocationTime((DbDataUtilities.getLongValue(rs, col++)).longValue());
                bean.setStoredProcedureTime((DbDataUtilities.getLongValue(rs, col++)).longValue());
                bean.setNumberOfUpdates((DbDataUtilities.getLongValue(rs, col++)).longValue());
                //don't do anything with this value yet
                String createDateTimeStamp = DbDataUtilities.getStringValue(rs, col++);

                beansFound.add(bean);
            }
        }
        finally {
            ConnectionMgr.closeResultSet(rs);
        }

        if (beansFound != null) {
            RumWebStatsBean[] beansTempArray = new RumWebStatsBean[beansFound.size() + 1];
            for (int i = 0; i < beansFound.size(); i++) {
                beansTempArray[i] = (RumWebStatsBean) beansFound.get(i);
            }

            return beansTempArray;
        }

        // JWH: WARNING: Dead code but not even safe code.  Is this here to catch something else??
        return new RumWebStatsBean[1];
    }

    /**
     * Registers the input parameters to the stored procedure's callable statement.
     * 
     * @param statement The callable statement to register parms into
     * @param hertzDateTime
     * 
     * @throws SQLException
     */
    protected void registerParams(CallableStatement statement, int hertzDateTime) throws SQLException, HertzException {

        int col = 1;
        StringBuffer dateBuffer = new StringBuffer();
        
        dateBuffer.append(hertzDateTime);
        dateBuffer.append(DATE_FILLER);

        DbDataUtilities.setStringParam(statement, col++, dateBuffer.toString());

        dateBuffer = new StringBuffer();
        dateBuffer.append(hertzDateTime + 1);
        dateBuffer.append(DATE_FILLER);

        DbDataUtilities.setStringParam(statement, col++, dateBuffer.toString());
        
        statement.registerOutParameter(col++, OracleTypes.CURSOR);
    }
}

/*
 *************************************************************
 * Change History:
 *
 * $Log$
 * Revision 1.1  2014/09/29 13:52:34  dtc1090
 * Checked in new code for DTAG Update Webservice
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
