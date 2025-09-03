package com.hertz.api.helpers;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Random;
import oracle.jdbc.OracleTypes;

import com.hertz.rates.common.utils.ErrorCategory;
import com.hertz.rates.common.utils.ErrorSystem;
import com.hertz.rates.common.utils.HertzDateTime;
import com.hertz.rates.common.utils.HertzException;
import com.hertz.rates.common.utils.config.PropertyGroup;
import com.hertz.rates.common.utils.jdbc.ConnectionMgr;
import com.hertz.rates.common.utils.jdbc.DbDataUtilities;
import com.hertz.rates.common.utils.logging.HertzLogger;
import com.hertz.rates.common.utils.logging.LogLevel;
import com.hertz.api.corebusiness.RumChangeDetails;
import com.hertz.api.corebusiness.RumUpdateGroup;
import com.hertz.api.corebusiness.WebServiceThreadManager;
import com.hertz.api.corebusiness.errorcodes.RumErrorCodes;
import com.hertz.api.corebusiness.logging.RumWebStats;

/**
 * Performs a RUM update on a single RumUpdateGroup.
 * Randomly calls the 20 stored procedures for the DB update.
 *
 */
public class RumUpdateHelper {

    final static HertzLogger logger = new HertzLogger(RumUpdateHelper.class);

    private static Random randomGenerator = null;
    private static Object loadTimeLock = new Object();

    private static final String RUM_UPDATE_PROC_NAME_1 = "doRumUpdateCall_1";
    private static final String RUM_UPDATE_PROC_NAME_2 = "doRumUpdateCall_2";
    private static final String RUM_UPDATE_PROC_NAME_3 = "doRumUpdateCall_3";
    private static final String RUM_UPDATE_PROC_NAME_4 = "doRumUpdateCall_4";
    private static final String RUM_UPDATE_PROC_NAME_5 = "doRumUpdateCall_5";
    private static final String RUM_UPDATE_PROC_NAME_6 = "doRumUpdateCall_6";
    private static final String RUM_UPDATE_PROC_NAME_7 = "doRumUpdateCall_7";
    private static final String RUM_UPDATE_PROC_NAME_8 = "doRumUpdateCall_8";
    private static final String RUM_UPDATE_PROC_NAME_9 = "doRumUpdateCall_9";
    private static final String RUM_UPDATE_PROC_NAME_10 = "doRumUpdateCall_10";
    private static final String RUM_UPDATE_PROC_NAME_11 = "doRumUpdateCall_11";
    private static final String RUM_UPDATE_PROC_NAME_12 = "doRumUpdateCall_12";
    private static final String RUM_UPDATE_PROC_NAME_13 = "doRumUpdateCall_13";
    private static final String RUM_UPDATE_PROC_NAME_14 = "doRumUpdateCall_14";
    private static final String RUM_UPDATE_PROC_NAME_15 = "doRumUpdateCall_15";
    private static final String RUM_UPDATE_PROC_NAME_16 = "doRumUpdateCall_16";
    private static final String RUM_UPDATE_PROC_NAME_17 = "doRumUpdateCall_17";
    private static final String RUM_UPDATE_PROC_NAME_18 = "doRumUpdateCall_18";
    private static final String RUM_UPDATE_PROC_NAME_19 = "doRumUpdateCall_19";
    private static final String RUM_UPDATE_PROC_NAME_20 = "doRumUpdateCall_20";

    private static final String RUM_UPDATE_TXN = "RumUpdate";
    private static final String RUM_UPDATE_TABLE = "RUM_UPDATE_TABLE";

    // Strings used in Config Data
    private final static String RATES_DB_CONNECTION = "Oracle";
    private final static char DELIMITER = '~';
    private final static char END_OF_RECORD = '^';
    
    // Commented unused constants.
    //private final static char END_OF_LIST = ':';
    //private final static String DEFAULT_TIME_STR = "00:00:00";

    /** Value for RAM_USER_ID in modified tables when source is a Web Service call. */
    private final static String MASSUPDATE_HEADER = "massupdate ";
    /** Value for RAM_USER_ID in modified tables when source is a file. */
    private final static String MASSFILEUPDATE_HEADER = "massfile ";
    
    
    private final static String FILE_NAME_LOG_MSG = " FileName>";
    //private final static String WEB_TRANS_ID_LOG_MSG = " TransId : ";

    private static int SEQUENCE_TO_ADD_TO_CAPTURE_DATE = 1;
    
    // Constants for stored procedure output parameters
    private static int RUM_STORED_PROC_DML_ROW_COUNT_INDEX = 11;
    private static int RUM_STORED_PROC_REF_CURSOR_INDEX = 12;

    protected PropertyGroup configDataProperties = null;

    public static HashMap<String, String> monthNumberToMonAlphaMap = new HashMap<String, String>();
    static {
        monthNumberToMonAlphaMap.put("1", "JAN");
        monthNumberToMonAlphaMap.put("2", "FEB");
        monthNumberToMonAlphaMap.put("3", "MAR");
        monthNumberToMonAlphaMap.put("4", "APR");
        monthNumberToMonAlphaMap.put("5", "MAY");
        monthNumberToMonAlphaMap.put("6", "JUN");
        monthNumberToMonAlphaMap.put("7", "JUL");
        monthNumberToMonAlphaMap.put("8", "AUG");
        monthNumberToMonAlphaMap.put("9", "SEP");
        monthNumberToMonAlphaMap.put("10", "OCT");
        monthNumberToMonAlphaMap.put("11", "NOV");
        monthNumberToMonAlphaMap.put("12", "DEC");
    }

    public static HashMap<String, String> numberToProcCallMap = new HashMap<String, String>();
    static {
        numberToProcCallMap.put("0", RUM_UPDATE_PROC_NAME_1);
        numberToProcCallMap.put("1", RUM_UPDATE_PROC_NAME_2);
        numberToProcCallMap.put("2", RUM_UPDATE_PROC_NAME_3);
        numberToProcCallMap.put("3", RUM_UPDATE_PROC_NAME_4);
        numberToProcCallMap.put("4", RUM_UPDATE_PROC_NAME_5);
        numberToProcCallMap.put("5", RUM_UPDATE_PROC_NAME_6);
        numberToProcCallMap.put("6", RUM_UPDATE_PROC_NAME_7);
        numberToProcCallMap.put("7", RUM_UPDATE_PROC_NAME_8);
        numberToProcCallMap.put("8", RUM_UPDATE_PROC_NAME_9);
        numberToProcCallMap.put("9", RUM_UPDATE_PROC_NAME_10);
        numberToProcCallMap.put("10", RUM_UPDATE_PROC_NAME_11);
        numberToProcCallMap.put("11", RUM_UPDATE_PROC_NAME_12);
        numberToProcCallMap.put("12", RUM_UPDATE_PROC_NAME_13);
        numberToProcCallMap.put("13", RUM_UPDATE_PROC_NAME_14);
        numberToProcCallMap.put("14", RUM_UPDATE_PROC_NAME_15);
        numberToProcCallMap.put("15", RUM_UPDATE_PROC_NAME_16);
        numberToProcCallMap.put("16", RUM_UPDATE_PROC_NAME_17);
        numberToProcCallMap.put("17", RUM_UPDATE_PROC_NAME_18);
        numberToProcCallMap.put("18", RUM_UPDATE_PROC_NAME_19);
        numberToProcCallMap.put("19", RUM_UPDATE_PROC_NAME_20);
    }

    /**
     * Instantiate passing in ConfigData properties for the RatePlanDataService this helper will be
     * use to lookup per-transaction settings.
     * 
     * @param properties a config data property group for the RatePlanDataService (from the
     * DataServices portion of config data)
     */
    public RumUpdateHelper(PropertyGroup properties) {

        this.configDataProperties = properties;
    }

    /**
     * Subclasses must override this to return a stored proc name/params string for the helper.
     * This is used in looking up the stored proc to call for this transaction that this helper is working on.
     * 
     * @return A string specifying the proc name and param positions (with ?s) for this helper's transaction
     */
    protected String getStoredProcString() {

        // Default number of instances of the Process_RUM_Update procedure.
        int nbrOfVersions = 20;
        
        // Determine if the default is overridden in config data.
        String versions = configDataProperties.getPropertyValue("storedProcVersions");
        if (versions != null) {
            nbrOfVersions = Integer.parseInt(versions);
        }
        
        int procNumber = getNumberGenerator().nextInt(nbrOfVersions);

        return configDataProperties.getPropertyValue(numberToProcCallMap.get(String.valueOf(procNumber)));
    }

    /**
     * Subclasses must override this to return a logical transaction name for the helper.
     * This is used in looking up config data stuff like connection properties, playback/record
     * properties, etc. for the transaction this helper is working on.
     * 
     * @return A logical transaction name for this txn that matches txn names in Config Data
     */
    protected String getTransactionName() {

        return RUM_UPDATE_TXN;
    }

    private Random getNumberGenerator() {

        // guard against multiple threads trying to load it at the same
        // time.  only one will actually win.  afterwards, there should not
        // be any threading issues since everything is immutable.
        //
        synchronized (loadTimeLock) {
            if (randomGenerator == null) { // if still unloaded by another thread
                logger.before(LogLevel.INFO, "Loading_Random_Number_Generator");
                try {
                    randomGenerator = new Random();
                }
                finally {
                    logger.after(LogLevel.INFO, "Loading_Random_Number_Generator");
                }
            }
        }

        return randomGenerator;
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
        protected RumUpdateGroup processUpdateResponse(ResultSetHelper rsHelper, RumUpdateGroup updateGroup) throws SQLException, HertzException {

        logger.entry(HertzLogger.INFO, "processUpdateResponse");

        ResultSet rs = rsHelper.getResultSet(RUM_STORED_PROC_REF_CURSOR_INDEX);  // Use constant for ref cursor parameter
        
        try {
            while (rs != null && rs.next()) {

                int col = 1;

                // Capture Date Time (ignore)
                DbDataUtilities.getStringValue(rs, col++);

                // Sequence Number
                String sequenceNumber = DbDataUtilities.getStringValue(rs, col++);
                
                // Area_Location (ignore)
                DbDataUtilities.getStringValue(rs, col++);

                // Vehicle
                String vehicle = DbDataUtilities.getStringValue(rs, col++);

                // Plan ID (ignore)
                DbDataUtilities.getStringValue(rs, col++);
                // Rate Type (ignore)
                DbDataUtilities.getStringValue(rs, col++);
                // Classification (ignore)
                DbDataUtilities.getStringValue(rs, col++);

                // Start Date
                int startDateInt = 0;
                Integer startDate = DbDataUtilities.getIntegerValue(rs, col++);
                if (startDate != null) {
                    startDateInt = startDate.intValue();
                }

                // End Date
                int endDateInt = 0;
                Integer endDate = DbDataUtilities.getIntegerValue(rs, col++);
                if (endDate != null) {
                    endDateInt = endDate.intValue();
                }

                // Rate Amount (ignore)
                DbDataUtilities.getStringValue(rs, col++);
                // Extra Day Amount (ignore)
                DbDataUtilities.getStringValue(rs, col++);
                // Extra Hour Factor (ignore)
                DbDataUtilities.getStringValue(rs, col++);

                // Response Message (Success/Failure)
                String responseMessage = DbDataUtilities.getStringValue(rs, col++);
                
                // Build update details array.
                RumChangeDetails details = updateGroup.getRumChangeDetailByLOKandVehicle(vehicle, startDateInt, endDateInt);
                if (details != null) {

                    if (responseMessage.indexOf("Successfull") > 0) {
                        details.setResponseMessage(responseMessage);
                    }
                    else {
                        details.setResponseMessage("FAILED");
                        RumErrorCodes codes = RumErrorCodes.findByDBSpecificCode(responseMessage);
                        if (codes == null) {
                            if (responseMessage.indexOf("locked") >= 0) {
                                details.setLocked(true);
                            }
                            codes = new RumErrorCodes(responseMessage, ErrorSystem.CLIENT_SYSTEM, ErrorCategory.DATA_NOT_FOUND);
                        }
                        details.setException(new HertzException(codes, responseMessage, false));
                    }
                    
                    // Save Sequence number
                    details.setSequenceNumber(sequenceNumber);
                    
                    // Failure Number
                    details.setFailureNumber(DbDataUtilities.getStringValue(rs, col++));
                }
                else {
                    logger.debug("DID NOT FIND A MATCH FOR RESPONSE DETAILS");
                }

                // Remaining fields are ignored or not even extracted from the Result Set: 
                //    Failure_Number (if successful),
                //    Create Date Time
                //    Company ID
                //    Transaction ID
                DbDataUtilities.getStringValue(rs, col++);
            }
        }
        finally {
            ConnectionMgr.closeResultSet(rs);
            logger.exit(HertzLogger.INFO, "processUpdateResponse");
        }

        return updateGroup;
    }

    //    /**
    //     * RATES-11849 FAKE
    //     * Performs a FAKE call to the database.
    //     * 
    //     * @param IRumUpdateRequest request
    //     * @return IRumUpdateResponse
    //     * @throws HertzException
    //     * @throws SQLException
    //     */
    //    public final RumUpdateGroup doFakeRumUpdate(RumUpdateGroup group) throws HertzException, SQLException {
    //
    //        String txnName = this.getTransactionName(); // subclasses implement this
    //
    //        // FAKE DB Call
    //        String fileName = group.getFileName();
    //        String txnNameExtended;
    //        if (fileName != null) {
    //            // MUST EXACTLY MATCH REAL method doRumUpdate() text here....
    //            txnNameExtended = txnName + FILE_NAME_LOG_MSG + group.getFileName();
    //        }
    //        else {
    //            // MUST EXACTLY MATCH REAL method doRumUpdate() text here....
    //            txnNameExtended = txnName + RumWebStats.TRANS_ID + group.getWebTransactionId();
    //        }
    //        DbDataUtilities.fakeExecuteStoredProcedure(txnNameExtended, null);
    //
    //        //-----------------------------------------------------------------------------------------
    //        // Alternate simplified version of actions that are in processUpdateResponse()
    //        //-----------------------------------------------------------------------------------------
    //
    //
    //        final String sequenceNumber = "1234567890";
    //        final String failureNumber = "0";
    //        final String responseMessage = "Successfull";
    //
    //        try {
    //
    //            ArrayList<RumChangeDetails> changeDetails = group.getChangeDetails();
    //            if (changeDetails != null) {
    //                Iterator<RumChangeDetails> iter = changeDetails.iterator();
    //                while (iter.hasNext()) {
    //                    RumChangeDetails details = iter.next();
    //
    //                    // Ignore matching vehicle, startDate, EndDate, just set all details the same.
    //                    // Assume all operations were successful.
    //
    //                    // Just for fun, collect these...
    //                    HertzDateTime detailStartDate = details.getStartDate();
    //                    HertzDateTime detailEndDate = details.getEndDate();
    //                    int startSystemDate = detailStartDate.getHertzSystemDate();
    //                    int endSystemDate = detailEndDate.getHertzSystemDate();
    //
    //                    details.setResponseMessage(responseMessage);
    //                    details.setSequenceNumber(sequenceNumber);
    //                    details.setFailureNumber(failureNumber);
    //                }
    //            }
    //        }
    //        finally {
    //            String capturedDateTime = getDateTime();
    //            group.setTimeToPurge(capturedDateTime);
    //
    //            if (capturedDateTime != null) {
    //                logDBCall(group, true);
    //            }
    //        }
    //
    //        return group;
    //    }

    /**
     * Actually performs the call to the database.
     * 
     * @param IRumUpdateRequest request
     * @return IRumUpdateResponse
     * @throws HertzException
     * @throws SQLException
     */
    public final RumUpdateGroup doRumUpdate(RumUpdateGroup group) throws HertzException, SQLException {

        Connection conn = null;
        CallableStatement statement = null;

        String capturedDateTime = null;

        // This uses a connection mgr which can be configured to
        // allow running disconnected from the db (using playback),
        // running within WebSphere and its pool of connections,
        // or running standalone outside of WebSphere.
        //
        try {
            String txnName = this.getTransactionName(); // subclasses implement this

            conn = ConnectionMgr.getConnection(RATES_DB_CONNECTION, txnName);
            statement = conn.prepareCall(this.getStoredProcString());

            capturedDateTime = registerParams(statement, group, conn, txnName); // subclasses might extend this

            if (capturedDateTime != null) {

                String fileName = group.getFileName();
                String txnNameExtended;
                if (fileName != null) {
                    txnNameExtended = txnName + FILE_NAME_LOG_MSG + group.getFileName();
                }
                else {
                    txnNameExtended = txnName + RumWebStats.TRANS_ID + group.getWebTransactionId();
                }
                DbDataUtilities.executeStoredProcedure(txnNameExtended, statement); // handles retry logic as needed

                // Extract DML row count from stored procedure output parameter
                Object rowCountObj = statement.getObject(RUM_STORED_PROC_DML_ROW_COUNT_INDEX);
                int dmlRowCount = (null != rowCountObj) ? (Integer)rowCountObj : 0;
                if (dmlRowCount > 0) {
                    Integer currentDMLRowCount = WebServiceThreadManager.txnDMLCountMap.get(group.getWebTransactionId());
                    if (null != currentDMLRowCount) {
                        currentDMLRowCount += dmlRowCount;
                        WebServiceThreadManager.txnDMLCountMap.put(group.getWebTransactionId(), currentDMLRowCount);
                    }
                }

                // Process the result set using the ref cursor output parameter
                ResultSetHelper rsHelper = new ResultSetHelper(statement);
                group = this.processUpdateResponse(rsHelper, group);
            }
        }
        finally {

            group.setTimeToPurge(capturedDateTime);

            if (capturedDateTime != null) {
                logDBCall(group, true);
            }

            // the result sets are closed by the process**ResultSet methods!
            ConnectionMgr.closeStatement(statement);
            ConnectionMgr.closeConnection(conn);
        }

        return group;
    }

    /**
     * Registers the input parameters to the stored procedure's callable statement.
     * 
     * @param statement The callable statement to register parms into
     * @param req The rates request to get the parameters out of
     * @throws SQLException
     */
    protected String registerParams(CallableStatement statement, RumUpdateGroup group, Connection conn, String txnName) throws SQLException, HertzException {

        String planId = null;
        String planType = null;
        String placeIdCode = null;
        String companyId = null;
        String placeTypeIdCode = null;
        String planClassCode = null;
        String transactionID = null;
        String ramUserID;
        
        String[][] updateArray = group.getDetailsArray();

        if (updateArray != null && updateArray.length > 0) {
            
            // Do this entry/exit to log the number of items being updated.
            //logger.entry(LogLevel.INFO, RumWebStats.NUMBER_OF_UPDATES_IN_PLAN_PLACE + updateArray.length + RumWebStats.TRANS_ID + group.getWebTransactionId());
            //logger.exit(LogLevel.INFO, RumWebStats.NUMBER_OF_UPDATES_IN_PLAN_PLACE + updateArray.length + RumWebStats.TRANS_ID + group.getWebTransactionId());

            int col = 1;

            // Plan ID
            planId = group.getPlanId();
            if (planId != null) {
                planId = planId.trim();
            }

            // Plan Type
            planType = group.getPlanType();
            if (planType != null) {
                planType = planType.trim();
            }

            // Place Type
            placeIdCode = group.getPlaceIdCdToUse();

            placeTypeIdCode = group.getPlaceTypeCode();
            if (placeTypeIdCode != null) {
                placeTypeIdCode = placeTypeIdCode.trim();
            }

            //PTR7024 SR56958 - DJA get Company ID
            companyId = group.getCompanyId();

            // Classification
            planClassCode = group.getClassTimeCode();
            if (planClassCode != null) {
                planClassCode = planClassCode.trim();
            }

            // Transaction ID
            transactionID = group.getWebTransactionId();
            if (transactionID != null) {
                transactionID = transactionID.trim();
            }

            // Captured time
            String capturedDateTime = getDateTime();

            // RAM User ID 
            // RATES-12505 - Distinguish between File and Web Service changes. 
            if (group.isAFileUpdate()) {
                // Set user as a File Update
                ramUserID = MASSFILEUPDATE_HEADER + capturedDateTime;
            }
            else {
                // Set user as a Web Service Update
                ramUserID = MASSUPDATE_HEADER + capturedDateTime;
            }
            
            // Captured time
            DbDataUtilities.setStringParam(statement, col++, capturedDateTime);
            // RAM User ID
            DbDataUtilities.setStringParam(statement, col++, ramUserID);
            // Plan ID
            DbDataUtilities.setStringParam(statement, col++, planId);
            // Plan Type
            DbDataUtilities.setStringParam(statement, col++, planType);
            // Place
            DbDataUtilities.setStringParam(statement, col++, placeIdCode);
            // Place Type
            DbDataUtilities.setStringParam(statement, col++, placeTypeIdCode);
            
            // Company ID - PTR7024 SR56958 - DJA add Company ID
            DbDataUtilities.setStringParam(statement, col++, companyId);
            
            // Classification
            DbDataUtilities.setStringParam(statement, col++, planClassCode);

            // Transaction ID
            DbDataUtilities.setStringParam(statement, col++, transactionID);
            
            // Modification Table
            DbDataUtilities.setStringStringArrayParam(statement, col++, conn, updateArray, RUM_UPDATE_TABLE);

            // Output parameters
            statement.registerOutParameter(col++, OracleTypes.INTEGER);  // DML row count
            statement.registerOutParameter(col++, OracleTypes.CURSOR);   // Result cursor
            // logDBCall(group, true);

            return capturedDateTime;
        }

        return null;
    }

    /**
     * This will log the call for debugging purposes
     * 
     *   PROCESS_RUM_UPDATE_CLINT(
            I_CAPTURE_DATE_TIME => '01-JAN-2011 14:24:56.12345',
            I_USER => 'bjmartin1',
            I_PLAN_ID => 'BIRDDY',
            I_PLAN_TYPE_ID_CD => 'PU',
            I_PLACE_ID_CD => '37502',
            I_PLACE_TYPE_CD => 1,
            I_CLASS_TIME_CD => 'DY',
            I_RUM_CHANGES_LIST => '0~10/24/2010~11/2/2010~A~567.79~0~283.895~7^1:',
            O_RUM_RESULT_SET => O_RUM_RESULT_SET);
     * @throws HertzException 
     *
     */
    public void logDBCall(RumUpdateGroup group, boolean debug) throws HertzException {

        StringBuffer logBuffer = new StringBuffer();

        if (group != null) {
            
            // Identify the system this data came from:
            if (group.getFileName() != null) {
                logBuffer.append("Filename: ");
                logBuffer.append(group.getFileName());
            }
            else if (group.getWebTransactionId() != null) {
                logBuffer.append("Trans ID: ");
                logBuffer.append(group.getWebTransactionId());                
            }
            else {
                logBuffer.append("Unknown Data Source: ");
            }

            logBuffer.append("  DECLARE ");

            String[][] updateArray = group.getDetailsArray();
            
            if (updateArray != null) {
                StringBuffer varBuffer = new StringBuffer();
                boolean firstvar = true;
                for (int i = 0; i < updateArray.length; i++) {

                    String sequenceNbr = updateArray[i][0];
                    String startDate = updateArray[i][1];
                    String endDate = updateArray[i][2];
                    String veh = updateArray[i][3];
                    String rate = updateArray[i][4];
                    String exDD = updateArray[i][5];
                    String exHH = updateArray[i][6];

                    String varName = null;
                    String varNameWithComma = null;
                    if (firstvar) {
                        varName = "rumtype";
                        varNameWithComma = "rumtype";
                        firstvar = false;
                    }
                    else {
                        varName = "rumtype" + i;
                        varNameWithComma = ", rumtype" + i;
                    }

                    varBuffer.append(varNameWithComma);

                    logBuffer.append(varName + " RUM_Update := RUM_Update_Record(");
                    logBuffer.append("'" + sequenceNbr + "'," + "'" + startDate + "'," + "'" + endDate + "'," + "'" + veh + "'," + "'" + rate + "'," + "'" + exDD + "'," + "'"
                            + exHH + "'); ");

                }

                logBuffer.append("rumtable RUM_UPDATE_TABLE := RUM_UPDATE_TABLE(");
                logBuffer.append(varBuffer);
                logBuffer.append(");");
                logBuffer.append("c_return CURSOR_TYPES.REF_CURSOR;");
                logBuffer.append("BEGIN");
                logBuffer.append("Process_RUM_Update('" + group.getTimeToPurge() + "' ,");
                logBuffer.append("'massupdate " + group.getTimeToPurge() + "', ");
                logBuffer.append("'" + group.getPlanId() + "',");
                logBuffer.append("'" + group.getPlanType() + "',");
                logBuffer.append("'" + group.getPlaceIdCdToUse() + "',");
                logBuffer.append("'" + group.getPlaceTypeCode() + "',");
                logBuffer.append("'" + group.getCompanyId() + "',");
                logBuffer.append("'" + group.getClassTimeCode() + "',");
                logBuffer.append("rumtable, c_return);");

                // These statements generates a very large amount of logging.  
                // However, they detail the changes made in the DB pretty well.  Much better than parsing the raw message.
                if (debug) {
                    logger.debug(logBuffer.toString());
                }
                else {
                    logger.info(logBuffer.toString());
                }

                //            logBuffer.append("PROCESS_RUM_UPDATE(I_CAPTURE_DATE_TIME => '");
                //            logBuffer.append(group.getTimeToPurge());
                //            logBuffer.append("', ");
                //            logBuffer.append("I_USER => '");
                //            logBuffer.append("massupdate " + group.getTimeToPurge() + "'" ); 
                //            logBuffer.append("', ");
                //            logBuffer.append("I_PLAN_ID => '");
                //            logBuffer.append(group.getPlanId());
                //            logBuffer.append("', ");
                //            logBuffer.append("I_PLAN_TYPE_ID_CD => '");
                //            logBuffer.append(group.getPlanType());
                //            logBuffer.append("', ");
                //            logBuffer.append("I_PLACE_ID_CD => '");
                //            logBuffer.append(group.getPlaceIdCdToUse());
                //            logBuffer.append("', ");
                //            logBuffer.append("I_PLACE_TYPE_CD => '");
                //            logBuffer.append(group.getPlaceTypeCode());
                //            logBuffer.append("', ");
                //            //PTR7024 SR56958 - DJA get Company ID
                //            logBuffer.append("I_COMPANY_ID => '");
                //            logBuffer.append(group.getCompanyId());
                //            logBuffer.append("', ");
                //            logBuffer.append("I_CLASS_TIME_CD => '");
                //            logBuffer.append(group.getClassTimeCode());
                //            logBuffer.append("', ");
                //            logBuffer.append("I_RUM_CHANGES_LIST => '");
                //        
                //            StringBuffer changesBuffer = new StringBuffer();
                //            ArrayList changes = group.getChangeDetails();
                //            int recordCount = 0;
                //            if (changes != null){
                //                Iterator changeIter = changes.iterator();
                //                while (changeIter.hasNext()){
                //                    createChangesDelimitedList(changesBuffer, (RumChangeDetails) changeIter.next()); 
                //                    recordCount++;
                //                }
                //            }
                //            
                //            changesBuffer.append(recordCount);
                //            changesBuffer.append(END_OF_LIST);         
                //            
                //            logBuffer.append(changesBuffer.toString());
                //            logBuffer.append("', ");
                //            logBuffer.append("O_RUM_RESULT_SET => O_RUM_RESULT_SET);");

            }
        }
    }

    /**
     * Convert to a date format the DB can use
     * 
     * @return String in format of "01-JAN-2011 14:24:56"
     * @throws HertzException
     */

    protected static synchronized String getDateTime() throws HertzException {

        HertzDateTime currentDateTime = HertzDateTime.getCurrentDateTime();
        StringBuffer dateBuffer = new StringBuffer();
        dateBuffer.append(currentDateTime.getDay());
        dateBuffer.append("-");
        dateBuffer.append(monthNumberToMonAlphaMap.get(String.valueOf(currentDateTime.getMonth())));
        dateBuffer.append("-");
        dateBuffer.append(currentDateTime.getYear());
        dateBuffer.append(" ");
        dateBuffer.append(currentDateTime.getTimeAsString(true));
        //  dateBuffer.append(SEQUENCE_TO_ADD_TO_CAPTURE_DATE);

        return dateBuffer.toString();
    }

    /**
     * We need a unique identifier to put on the capture date so we can purge the data
     * later from run rum 
     * @return
     */
    protected static int getSequenceToAddToCaptureDate() {

        if (SEQUENCE_TO_ADD_TO_CAPTURE_DATE >= 999999) {
            SEQUENCE_TO_ADD_TO_CAPTURE_DATE = 1;
        }
        else {
            SEQUENCE_TO_ADD_TO_CAPTURE_DATE++;
        }

        return SEQUENCE_TO_ADD_TO_CAPTURE_DATE;
    }

    /**
     * Create the delimited list String of updates to be made.
     * 
     * @param StringBuffer buffer
     * @param UpdateFileRow row
     * @param int sequenceNumber
     */
    private void createChangesDelimitedList(StringBuffer buffer, RumChangeDetails details) {

        buffer.append(details.getSequenceNumber());
        buffer.append(DELIMITER);
        if (details.getStartDate() != null) {
            buffer.append(details.getStartDate().toStringDateOnly());
        }
        else {
            buffer.append("INVALID START DATE CHECK DATA");
        }

        buffer.append(DELIMITER);
        if (details.getEndDate() != null) {
            buffer.append(details.getEndDate().toStringDateOnly());
        }
        else {
            buffer.append("INVALID END DATE CHECK DATA");
        }

        buffer.append(DELIMITER);
        buffer.append(details.getVehicle());
        buffer.append(DELIMITER);
        buffer.append(details.getRate());
        buffer.append(DELIMITER);
        buffer.append(details.getExtraDay());
        buffer.append(DELIMITER);
        buffer.append(details.getExtraHour());
        buffer.append(DELIMITER);
        buffer.append("7");
        buffer.append(END_OF_RECORD);
    }
}

/*
 *************************************************************
 * Change History:
 *
 * $Log$
 * Revision 1.41  2018/05/07 21:12:05  dtp4395
 * RATES-12505; Heflin; Corrected constant value for file-based changes.
 *
 * Revision 1.40  2018/05/02 18:25:31  dtp4395
 * RATES-12505; Heflin; Added processing to set RAM_USER_ID based on source of the update: file or web service.
 *
 * Revision 1.39  2017/10/27 19:18:21  dtp4395
 * RATES-12320; Heflin; Improved logging messages by adding Trans ID: to DB parm logging.  Added comments.
 *
 * Revision 1.38  2017/09/14 19:04:07  dtp4395
 * RATES-12061; Heflin; Commented unused constants, added Transaction ID to registerParams() to store in RUM_UPDATE_STATUS table, added comments, formatted.
 *
 * Revision 1.37  2017/03/08 07:08:03  dtp4395
 * RATES-11849; Heflin; Fixed text parameter in registerParams() in update count statistics call.
 *
 * Revision 1.36  2017/02/21 16:01:54  dtp4395
 * RATES-11849; Heflin; Commented test code out.
 *
 * Revision 1.35  2017/01/17 04:07:37  dtp4395
 * RATES-11849; Heflin; Simplified doFakeRumUpdate() to avoid external information.  Added comments.
 *
 * Revision 1.34  2017/01/16 22:30:18  dtp4395
 * RATES-11849; Heflin; Fixed doFakeRumUpdate(), refactored methodName for logging in doRumUpdate(), added comments.
 *
 * Revision 1.33  2017/01/10 21:31:42  dtp4395
 * RATES-11849; TESTING - Added correct entry/exit to fake DB call.
 *
 * Revision 1.32  2017/01/10 19:34:10  dtp4395
 * RATES-11849; TESTING - Added doFakeRumUpdate
 *
 * Revision 1.31  2016/11/08 18:51:03  dtp4395
 * RATES-11849; Heflin; Comment changes and formatting. No code changes.
 *
 * Revision 1.30  2016/10/27 14:55:35  dtp4395
 * RATES-11849; Heflin; Added/fixed comments.  Formatting only.
 *
 * Revision 1.29  2014/09/29 13:52:34  dtc1090
 * Checked in new code for DTAG Update Webservice
 *
 * Revision 1.28  2014/08/05 14:54:50  dtc1090
 * SR 59911 - adding new WebService layer to RUM
 *
 * Revision 1.27  2014/07/31 20:24:46  dtc1090
 * RATES-8035 - fixed deadlock on connection when using ARRAY
 *
 * Revision 1.26  2014/07/18 16:01:30  dtc1090
 * RATES-8035 - Start sending RUM updates as ARRAYS and not a delimited list .
 *
 * Revision 1.25  2014/04/29 16:12:05  dtc1090
 * RATES-7814 - Maklee Enhancements - Using more than 1 version of update proc and adding functionality to RUM to update more than 1 file at a time.
 *
 * Revision 1.24  2013/09/20 18:40:56  dtp0540
 * remove variable read of database result set.  I originally put the new variable for company id in the middle of the result set then later changed it to the end but forgot to change the update helper.
 *
 * Revision 1.23  2013/09/04 14:44:06  dtc1090
 * Merged DTG branch into HEAD
 *
 * Revision 1.22  2013/06/25 15:08:46  dtp0540
 * DJA - I accidentally committed the company_id changes to the head too soon.  So I'm reverting back to the previous historical modification.
 *
 * Revision 1.20  2012/08/20 18:03:30  dtc1090
 * SR-54472 - RUM Processing Changes
 *
 * Revision 1.19  2011/08/30 16:16:07  dtc1090
 * updated log statement
 *
 * Revision 1.18  2011/05/19 19:13:48  dtc1090
 * SR 500044.01 (RUM Addendum 1) added the abilty to handle users sending in CCSS in the location field.  Also to write a seperate output file for locked plan/places
 *
 * Revision 1.17  2011/04/11 13:13:50  dtc1090
 * checked in changes to date for purging
 *
 * Revision 1.16  2011/03/18 19:46:05  dtc1090
 * stored proc will not send back a sequence number
 *
 * Revision 1.15  2011/03/09 17:56:43  dtc1090
 * merged from qual branch
 *
 * Revision 1.10.2.6  2011/03/09 17:54:20  dtc1090
 * added new bug fixes
 *
 * Revision 1.10.2.5  2011/03/09 17:10:07  dtc1090
 * fixed nulls in debug
 *
 * Revision 1.10.2.4  2011/03/08 19:29:25  dtc1090
 * merged from head
 *
 * Revision 1.14  2011/03/08 19:26:28  dtc1090
 * fixed null pointer
 *
 * Revision 1.13  2011/03/08 18:32:33  dtc1090
 * merged changes from qual branch
 *
 * Revision 1.10.2.3  2011/03/08 17:15:02  dtc1090
 * fixed debug statement
 *
 * Revision 1.10.2.2  2011/03/08 02:09:52  dtc1090
 * merged needed changes
 *
 * Revision 1.12  2011/03/08 02:07:52  dtc1090
 * Added code for reporting errors from DB
 *
 * Revision 1.11  2011/03/07 15:08:40  dtc1090
 * Modified to code to not log as much
 *
 * Revision 1.10  2011/02/28 23:00:38  dtc1090
 * refactored RUM
 *
 * Revision 1.9  2011/02/23 17:24:12  dtc1090
 * added govenor to control number of concurrent updates
 *
 * Revision 1.8  2011/02/17 20:24:49  dtc1090
 * added date changes
 *
 * Revision 1.7  2011/02/17 15:42:50  dtc1090
 * added date / time to update user name
 *
 * Revision 1.6  2011/02/17 15:07:33  dtc1090
 * added date/time to user name
 *
 * Revision 1.5  2011/01/31 17:33:24  dtc1090
 * Added new code for development
 *
 * Revision 1.4  2011/01/27 20:23:55  dtc1090
 * fixed row count
 *
 * Revision 1.3  2011/01/27 17:00:38  dtc1090
 * Added handling of sequence number
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
 *************************************************************
 */