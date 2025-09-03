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
import com.hertz.api.corebusiness.RumUpdateGroup;

import oracle.jdbc.OracleTypes;

/**
 * Lookup Place for RUM
 */
public class GetPlaceIdCodeHelper {

    final static HertzLogger logger = new HertzLogger(GetPlaceIdCodeHelper.class);

    // Strings used in Config Data
    private final static String RATES_DB_CONNECTION = "Oracle";
    private static final String PROC_NAME = "getPlaceIdCodeCall";
    private static final String TXN_NAME = "GetPlaceIdCode";

    protected PropertyGroup configDataProperties = null;

    /**
     * Instantiate passing in ConfigData properties for the RatePlanDataService this helper will be
     * use to lookup per-transaction settings.
     * 
     * @param properties a config data property group for the RatePlanDataService (from the
     * DataServices portion of config data)
     */
    public GetPlaceIdCodeHelper(PropertyGroup properties) {

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

    /**
     * Subclasses must override this to do the actual processing of the result sets for a stored proc
     * call.  The statement has already been executed and is ready
     * to have its result sets processed.  The request is passed in, in case you need to copy anything
     * from the request.
     * 
     * @param rsMgr Lets you gain access to specific result sets.
     * @param request The request that fed into the statement's params.
     * 
     */
    protected RumUpdateGroup processResponse(ResultSetHelper rsHelper, RumUpdateGroup updateGroup) throws SQLException, HertzException {

        logger.entry(HertzLogger.INFO, "processResponse");
        
        ResultSet rs = rsHelper.getResultSet(2);
        
        try {
            while (rs.next()) {
                int col = 1;

                String placeIdCode = rs.getString(col++);
                String placeTypeCode = rs.getString(col++);

                updateGroup.setPlaceIdCd(placeIdCode);
                updateGroup.setPlaceTypeCode(placeTypeCode);

            }
        }
        finally {
            ConnectionMgr.closeResultSet(rs);
            logger.exit(HertzLogger.INFO, "processResponse");

        }
        return updateGroup;
    }

    //    /**
    //     * RATES-11849 FAKE
    //     * This is a fake call to the DB.
    //     * @param updateGroup
    //     * @return
    //     * @throws HertzException
    //     * @throws SQLException
    //     */
    //    public final RumUpdateGroup doFakeDBCall(RumUpdateGroup updateGroup) throws HertzException, SQLException {
    //
    //        // Stupid by-pass - just copy Location into Place field.  Sometimes that would even be accurate.
    //
    //        String txnName = this.getTransactionName(); // subclasses implement this
    //
    //        // Simulate DB call
    //        DbDataUtilities.fakeExecuteStoredProcedure(txnName, null);
    //
    //        // Just copy the location to 'looked-up' location.
    //        updateGroup.setPlaceIdCd(updateGroup.getLocation());
    //        updateGroup.setPlaceTypeCode("1");
    //
    //        return updateGroup;
    //    }

    public final RumUpdateGroup doDBCall(RumUpdateGroup updateGroup) throws HertzException, SQLException {

        Connection conn = null;
        CallableStatement statement = null;

        // This uses a connection mgr which can be configured to
        // allow running disconnected from the db (using playback),
        // running within WebSphere and its pool of connections,
        // or running standalone outside of WebSphere.
        
        try {
            String txnName = this.getTransactionName(); // subclasses implement this

            conn = ConnectionMgr.getConnection(RATES_DB_CONNECTION, txnName);
            statement = conn.prepareCall(this.getStoredProcString());

            registerParams(statement, updateGroup.getLocation()); // subclasses might extend this

            DbDataUtilities.executeStoredProcedure(txnName, statement); // handles retry logic as needed
            
            ResultSetHelper rsHelper = new ResultSetHelper(statement);
			           
            updateGroup = this.processResponse(rsHelper, updateGroup);
        }
        finally {
            // the result sets are closed by the process**ResultSet methods!
            ConnectionMgr.closeStatement(statement);
            ConnectionMgr.closeConnection(conn);
        }

        return updateGroup;
    }

    /**
     * Registers the input parameters to the stored procedure's callable statement.
     * 
     * @param statement The callable statement to register parms into
     * @param req The rates request to get the parameters out of
     * @throws SQLException
     */
    protected void registerParams(CallableStatement statement, String areaLocation) throws SQLException, HertzException {

        int col = 1;

        if (areaLocation.length() < 7) {
            StringBuffer buffer = new StringBuffer();
            buffer.append("0");
            buffer.append(areaLocation);
            statement.setString(col++, buffer.toString());
        }
        else {
            statement.setString(col++, areaLocation);
        }
        statement.registerOutParameter(col++, OracleTypes.CURSOR);
    }
}

/*
 *************************************************************
 * Change History:
 *
 * $Log$
 * Revision 1.8  2017/01/16 22:27:56  dtp4395
 * RATES-11849; Heflin; Fixed doFakeDBCall.
 *
 * Revision 1.7  2017/01/10 21:31:16  dtp4395
 * RATES-11849; TESTING - Added correct entry/exit to fake DB call.
 *
 * Revision 1.6  2017/01/10 19:36:52  dtp4395
 * RATES-11849; TESTING - Added doFakeDBCall; commented and formatted.
 *
 * Revision 1.5  2011/05/19 19:13:48  dtc1090
 * SR 500044.01 (RUM Addendum 1) added the ability to handle users sending in CCSS in the location field.  Also to write a seperate output file for locked plan/places
 *
 * Revision 1.4  2011/02/28 23:00:38  dtc1090
 * refactored RUM
 *
 * Revision 1.3  2011/02/07 21:52:23  dtc1090
 * committed new code for development
 *
 * Revision 1.2  2011/01/27 16:22:47  dtc1090
 * made changes
 *
 *************************************************************
 *
 * Copyright (C) 2003 The Hertz Corporation
 *
 * All Rights Reserved. (Unpublished.)
 *
 * The information contained herein is confidential and
 *
 * proprietary to The Hertz Corporation and may not be
 *
 * duplicated, disclosed to third parties, or used for any
 *
 * purpose not expressly authorized by it.  Any unauthorized
 *
 * use, duplication or disclosure is prohibited by law.
 *
 *
 *************************************************************
 */