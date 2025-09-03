package com.hertz.api.helpers;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.hertz.rates.common.utils.HertzDateTime;
import com.hertz.rates.common.utils.HertzException;
import com.hertz.rates.common.utils.config.PropertyGroup;
import com.hertz.rates.common.utils.jdbc.ConnectionMgr;
import com.hertz.rates.common.utils.jdbc.DbDataUtilities;
import com.hertz.rates.common.utils.logging.HertzLogger;
import com.hertz.api.corebusiness.RumUpdateGroup;
import com.hertz.api.corebusiness.logging.RumWebStatsBean;

/**
 * RUM - Helper to insert Webservices call update data into DB.
 * 
 *
 */
public class InsertRumWebDataHelper {

    final static HertzLogger logger = new HertzLogger(InsertRumWebDataHelper.class);

    // Strings used in Config Data
    private final static String RATES_DB_CONNECTION = "Oracle";
    private static final String PROC_NAME = "insertWebHistoricalData";
    private static final String TXN_NAME = "RumWebHistoricaData";
    
    protected PropertyGroup configDataProperties = null;

    /**
     * Instantiate passing in ConfigData properties for the RatePlanDataService this helper will be
     * use to lookup per-transaction settings.
     * 
     * @param properties a config data property group for the RatePlanDataService (from the
     * DataServices portion of config data)
     */
    public InsertRumWebDataHelper(PropertyGroup properties) {

        this.configDataProperties = properties;
    }

    /**
     * Subclasses must override this to return a stored proc name/params string for the helper.
     * This is used in looking up the stored proc to call for this transaction that this helper is working on.
     * 
     * @return A string specifying the proc name and param positions (with ?s) for this helper's transaction
     */
    protected String getStoredProcString(boolean forGetFile) {

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
     * Register the following column data for RUM_WEB_TRANS_HISTORY table:
     * 
     *    TRANSACTION_ID
     *    START_TIME_IN_MS
     *    END_TIME_IN_MS
     *    LOC_RETRIEVE_TIME
     *    EXEC_STORED_PROC_TIME
     *    NBR_OF_UPDATES_IN_TRANS
     *    CREATE_DATE_TIME_STAMP
     *    
     * @param statement
     * @param bean
     * @param group
     * @throws SQLException
     * @throws HertzException
     */
    protected void registerParms(CallableStatement statement, RumWebStatsBean bean, RumUpdateGroup group) throws SQLException, HertzException {

        int col = 1;

        // TRANSACTION_ID
        DbDataUtilities.setStringParam(statement, col++, group.getWebTransactionId());
        // START_TIME_IN_MS
        DbDataUtilities.setStringParam(statement, col++, String.valueOf(bean.getTransactionStartTime()));
        // END_TIME_IN_MS
        DbDataUtilities.setStringParam(statement, col++, String.valueOf(bean.getTransactionEndTime()));
        // LOC_RETRIEVE_TIME
        DbDataUtilities.setStringParam(statement, col++, String.valueOf(bean.getRetrieveLocationTime()));
        // EXEC_STORED_PROC_TIME
        DbDataUtilities.setStringParam(statement, col++, String.valueOf(bean.getStoredProcedureTime()));
        
        // NBR_OF_UPDATES_IN_TRANS
        // RATES-11849 - RUM can now process more than one place/plan in a webservices call so we must accumulate changes across all place/plans.
        DbDataUtilities.setStringParam(statement, col++, String.valueOf(bean.getNumberOfUpdates()));
        //DbDataUtilities.setStringParam(statement, col++, String.valueOf(group.getChangeDetails().size()));

        // Set up the oracle time stamp
        HertzDateTime currentDateTime = HertzDateTime.getCurrentDateTime();
        StringBuffer createDateTimeStampBuffer = new StringBuffer();
        createDateTimeStampBuffer.append(currentDateTime.getHertzSystemDate());

        // CREATE_DATE_TIME_STAMP, format HH:MM:SS
        int hour = currentDateTime.getHour();
        if (hour < 10) {
            createDateTimeStampBuffer.append("0");
        }
        createDateTimeStampBuffer.append(hour);

        int minute = currentDateTime.getMinute();
        if (minute < 10) {
            createDateTimeStampBuffer.append("0");
        }
        createDateTimeStampBuffer.append(minute);

        int second = currentDateTime.getSecond();
        if (second < 10) {
            createDateTimeStampBuffer.append("0");
        }
        createDateTimeStampBuffer.append(second);

        DbDataUtilities.setStringParam(statement, col++, createDateTimeStampBuffer.toString());
    }
    
    //    /**
    //     * RATES-11849 FAKE
    //     * @param bean
    //     * @param group
    //     * @return
    //     * @throws HertzException
    //     * @throws SQLException
    //     */
    //    public final boolean insertFakeRumWebTransactionHistory(RumWebStatsBean bean, RumUpdateGroup group) throws HertzException, SQLException {
    //
    //        StringBuffer s = new StringBuffer();
    //
    //        s.append("History Record:" + "\n");
    //        
    //        s.append("  WebTransactionId     :" + group.getWebTransactionId() + "\n");
    //        s.append("  TransactionStartTime :" + String.valueOf(bean.getTransactionStartTime()) + "\n");
    //        s.append("  TransactionEndTime   :" + String.valueOf(bean.getTransactionEndTime()) + "\n");
    //        s.append("  RetrieveLocationTime :" + String.valueOf(bean.getRetrieveLocationTime()) + "\n");
    //        s.append("  StoredProcedureTime  :" + String.valueOf(bean.getStoredProcedureTime()) + "\n");
    //        s.append("  ChangeDetails        :" + String.valueOf(bean.getNumberOfUpdates()) + "\n");
    //
    //        //set up the oracle time stamp
    //        HertzDateTime currentDateTime = HertzDateTime.getCurrentDateTime();
    //        StringBuffer createDateTimeStampBuffer = new StringBuffer();
    //        createDateTimeStampBuffer.append(currentDateTime.getHertzSystemDate());
    //
    //        int hour = currentDateTime.getHour();
    //        if (hour < 10) {
    //            createDateTimeStampBuffer.append("0");
    //        }
    //        createDateTimeStampBuffer.append(hour);
    //
    //        int minute = currentDateTime.getMinute();
    //        if (minute < 10) {
    //            createDateTimeStampBuffer.append("0");
    //        }
    //        createDateTimeStampBuffer.append(minute);
    //
    //        int second = currentDateTime.getSecond();
    //        if (second < 10) {
    //            createDateTimeStampBuffer.append("0");
    //        }
    //        createDateTimeStampBuffer.append(second);
    //
    //        s.append("  CreateDateTimeStamp  :" + createDateTimeStampBuffer.toString() + "\n");
    //
    //        logger.debug(s.toString());
    //
    //        // Simulate the DB call.
    //        String txnName = this.getTransactionName();
    //        DbDataUtilities.fakeExecuteStoredProcedure(txnName, null); // handles retry logic as needed
    //        
    //        return true;
    //    }


    public final boolean insertRumWebTransactionHistory(RumWebStatsBean bean, RumUpdateGroup group) throws HertzException, SQLException {

        // RATES-11849 FAKE
        // Do Fake DB if selected.
        //if (UpdateDriver.isRunWithNoDb()) {
        //    return insertFakeRumWebTransactionHistory(bean, group);
        //}
        
        // Normal Code.
        Connection conn = null;
        CallableStatement statement = null;
        ResultSet rs = null;

        try {
            String txnName = this.getTransactionName(); // subclasses implement this

            conn = ConnectionMgr.getConnection(RATES_DB_CONNECTION, txnName);
            statement = conn.prepareCall(this.getStoredProcString(false));
            
            registerParms(statement, bean, group);

            DbDataUtilities.executeStoredProcedure(txnName, statement); // handles retry logic as needed	        	      		        	

            //rs = statement.getResultSet();   	        	
        }
        finally {
            if (rs != null) {
                rs.close();
            }

            ConnectionMgr.closeStatement(statement);
            ConnectionMgr.closeConnection(conn);
        }

        return true;
    }
}

/*
 *************************************************************
 * Change History:
 *
 * $Log$
 * Revision 1.7  2017/02/28 15:42:32  dtp4395
 * RATES-11849; Heflin; Organized imports.  No code changes.
 *
 * Revision 1.6  2017/02/21 16:02:16  dtp4395
 * RATES-11849; Heflin; Commented test code out.
 *
 * Revision 1.5  2017/01/16 22:19:36  dtp4395
 * RATES-11849; Heflin; Fixed number of updates calculation in registerParms(), insertFakeRumWebTransactionHistory(), added code to bypass DB during debug.
 *
 * Revision 1.4  2017/01/10 21:31:28  dtp4395
 * RATES-11849; TESTING - Added correct entry/exit to fake DB call.
 *
 * Revision 1.3  2017/01/10 19:35:02  dtp4395
 * RATES-11849; TESTING - Added insertFakeRumWebTransactionHistory
 *
 * Revision 1.2  2016/10/31 21:13:03  dtp4395
 * RATES-11849; Heflin; Added/fixed comments.  Formatted.  No code changes.
 *
 * 
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
