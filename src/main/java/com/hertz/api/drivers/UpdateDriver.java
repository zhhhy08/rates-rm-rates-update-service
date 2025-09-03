package com.hertz.api.drivers;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import com.hertz.rates.common.errorcodes.CommonErrorCodes;
import com.hertz.rates.common.mq.GUIDGenerator;
import com.hertz.rates.common.service.data.DataServiceLocator;
import com.hertz.rates.common.utils.FastStringTokenizer;
import com.hertz.rates.common.utils.HertzErrorCode;
import com.hertz.rates.common.utils.HertzException;
import com.hertz.rates.common.utils.StringUtils;
import com.hertz.rates.common.utils.config.ConfigData;
import com.hertz.rates.common.utils.config.PropertyGroup;
import com.hertz.rates.common.utils.logging.HertzLogger;
import com.hertz.rates.common.utils.logging.LogLevel;
import com.hertz.api.corebusiness.AreaLocationsToPlaceIdCodeList;
import com.hertz.api.corebusiness.RumChangeDetails;
import com.hertz.api.corebusiness.RumLocationInfo;
import com.hertz.api.corebusiness.RumUpdateGroup;
import com.hertz.api.corebusiness.UpdateList;
import com.hertz.api.corebusiness.UpdateRow;
import com.hertz.api.corebusiness.UpdateThread;
import com.hertz.api.corebusiness.WebServiceThreadManager;
import com.hertz.api.corebusiness.errorcodes.RumErrorCodes;
import com.hertz.api.corebusiness.logging.RumStats;
import com.hertz.api.corebusiness.logging.RumWebStats;
import com.hertz.api.corebusiness.logging.RumWebStatsBean;
import com.hertz.api.service.data.historical.IRumWebHistoricalDataService;
import com.hertz.api.service.data.update.IRumGetPlaceTypeIdCodeDataService;
import com.hertz.api.service.data.update.IRumPurgeMessagesDataService;
import com.hertz.api.transform.OutputFileWriter;
import com.hertz.api.transform.UpdateRowMapper;
import com.hertz.api.metrics.RumMetrics;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * This is the main driver class that will drive all the MASS RATE UPDATES (RUM)
 * 
 * @author Clint Hedrick
 *
 */
public class UpdateDriver {

    private final static HertzLogger logger = new HertzLogger(UpdateDriver.class);

    /** Processing path for updates obtained from files. */
    private final static int PATH_NORMAL = 1;
    private final static int PATH_FORCE_TO_WS = 2;
    
    /**
     * RATES-11849 FAKE
     * This boolean should be FALSE in Production and can be set to TRUE to test RUM without actual DB calls.
     * 
     *     *** MAKE THIS FALSE IN PRODUCTION ***
     */
    //private final static boolean RUN_WITH_NO_DB = false;
    
    //protected static HashMap inputFileToThreadResponseMap = new HashMap();    
    
    private static final String UPDATE_DS_NAME = "IRumGetPlaceTypeIdCodeDataService";
    private static final String PURGE_DS_NAME = "IRumPurgeMessagesDataService";
    private static final String WEB_HISTORICAL_DS_NAME = "IRumWebHistoricalDataService";

    // This is used in UpdateThread:
    //private static final String RUM_UPDATE_DS = "IRumUpdateDataService";
    
    private ConfigData configData = null;
    
    private static final String UPDATE_SUCCESS_MSG = " Processed Successfully Update Successful";

    private static AreaLocationsToPlaceIdCodeList areaLocationsAlreadyFound = null;
    private static Object loadTimeLock = new Object();

    /** Total number of Webservice calls that this server has processed. */
    private static long totalWSCalls = 0;
    /** Maximum number of simultaneous Webservice calls that have been actually observed. */
    private static Object wsCallLock = new Object();
    private static long currentWSCalls = 0;
    private static long maxSimultaneousWSCalls = 0;

    /** Time span that defines a "long running DB call": (30 sec * 1000 millis/sec) */
    private final static long LONG_RUNNING_IN_MILLIS_DEFAULT = (30L * 1000L);
    private long longRunningInMillis = -1;
    
    /** Minimum memory encountered. Initialize this as the largest possible value. */
    private static long definitelyFreeMemoryLowest = Long.MAX_VALUE;
    private static long presumablyFreeMemoryLowest = Long.MAX_VALUE;
    
    private MeterRegistry meterRegistry;

    // Constants for client tracking in metrics
    private static final String NO_COUNTRY = "NO_COUNTRY";
    private static final String CLIENT_COUNTRY = "Client_Country";
    private static final String CLIENT_IP = "Client_IP";
    private static final String CLIENT_COUNTRY_API_CALL = "Client_Country_API_Call";
    
    /**
     * Constructor with dependency injection - for manual creation with metrics
     */
    public UpdateDriver(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
    	logger.info("Config Data called.....");
        try {
            configData = ConfigData.getInstance();
        }
        catch (HertzException e) {
            logger.info(e.getMessage());
        }
    }
    
    /**
     * Default constructor for Spring injection or backward compatibility
     */
    public UpdateDriver() {
        // meterRegistry will be injected by Spring if this is a Spring-managed bean
        // or remain null if created manually
        
    	logger.info("Config Data called.....");
        try {
            configData = ConfigData.getInstance();
        }
        catch (HertzException e) {
            logger.info(e.getMessage());
        }
    }

    private static AreaLocationsToPlaceIdCodeList getAreaLocation() {

        // guard against multiple threads trying to load it at the same
        // time.  only one will actually win.  afterwards, there should not
        // be any threading issues since everything is immutable.
        //
        synchronized (loadTimeLock) {
            if (areaLocationsAlreadyFound == null) { // if still unloaded by another thread
                logger.before(HertzLogger.INFO, "Loading_Area_Number_Cache");
                try {
                    areaLocationsAlreadyFound = new AreaLocationsToPlaceIdCodeList();
                }
                finally {
                    logger.after(HertzLogger.INFO, "Loading_Area_Number_Cache");
                }
            }
        }

        return areaLocationsAlreadyFound;
    }

    /**
     * File-Based RUM : This is the beginning of the File-based update process.  Will create a 
     * Thread to update DB for each plan/place and write the appropriate 
     * response file to specified directory for EAI pickup.
     * @throws SQLException 
     * @throws HertzException 
     * @throws SQLException 
     * @throws HertzException 
     * @throws InterruptedException 
     *
     */
    public void doUpdate(String fileName, ArrayList<UpdateRow> listOfUpdateRecords, int maxThreadsRunning) throws HertzException {

        final String methodName = "doUpdate FileName>" + fileName;
        logger.entry(HertzLogger.INFO, methodName);
        
        UpdateList updateList = null;
        
        try {
            // RATES-12747 - Log start of processing file.
            logger.info("<FB> Starting File processing: " + fileName);
            
            if (listOfUpdateRecords != null) {

                updateList = sortUpdateFileRowsToRumUpdateGroups(listOfUpdateRecords, fileName, null);

                // We don't need the records list anymore, clear it to preserve space.
                listOfUpdateRecords.clear();

                findPlaceTypeIdCode(updateList, fileName, null);

                if (updateList != null) {

                    while (!updateList.allGroupsProcessed()) {

                        int path = getProcessingPathForFileBasedUpdates();
                        
                        switch (path) {
                            case PATH_NORMAL:
                                // Original code - begin
                                doRUMUpdateForFileBased(updateList, fileName, maxThreadsRunning);
                                // Original code - end
                            break;
                            
                            case PATH_FORCE_TO_WS:
                                // Send updates to Webservices processing *ONLY* for testing purposes.
                                String hashString = fileName;
                                String transactionId = GUIDGenerator.generateGUID(hashString, 24);
                                switchFileForTransId(updateList, transactionId);
                                // Force a Webservice logging call to initiate statistics correctly.
                                final String methodNameFake = RumWebStats.DO_WEBSERVICE_UPDATE + RumWebStats.TRANS_ID + transactionId;
                                try {
                                    logger.entry(LogLevel.INFO, methodNameFake);
                                    // Do the update as if the data came from a Web Service call.
                                    doRUMUpdateForWebservice(updateList, transactionId);
                                }
                                finally {
                                    logger.exit(LogLevel.INFO, methodNameFake);
                                    saveWebServiceStatistics(transactionId, updateList);
                                }
                            break;
                        }
                    }

                    //now write the output file 
                    writeOutputFile(updateList, fileName, null);

                    //  ArrayList listOfResponses = (ArrayList) inputFileToThreadResponseMap.get(fileName);
                    // After we write the output file go ahead and purge the input status table
                    String doPurge = configData.getConfigSettings().get("doPurge");
                    if ((doPurge != null && doPurge.equals("true")) && updateList != null) {
                        Iterator<RumUpdateGroup> responseIter = updateList.getListOfUpdates().iterator();
                        while (responseIter.hasNext()) {
                            RumUpdateGroup updateGroup = responseIter.next();
                            doPurgeOfRumMessages(updateGroup.getTimeToPurge());
                        }
                    }
                    else {
                        logger.info("<FB> Purge Skipped. ");
                    }
                }
            }  
        }
        finally {
            // RATES-12747 - Log completion of processing file.
            logger.info("<FB> Completed File: " + fileName);
            
            if (updateList != null) {
                updateList.getListOfUpdates().clear();
            }
            logger.exit(HertzLogger.INFO, methodName);
        }
    }

    /**
     * TESTING SUPPORT:
     * 
     * For the "Files to Web Service" testing, we need to remove the File Name and add the Transaction ID 
     * in each of the UpdateGroups in the UpdateList.
     * @param updateList
     * @param transactionId
     */
    private static void switchFileForTransId(UpdateList updateList, String transactionId) {
        
        if (updateList != null) {

            Iterator<RumUpdateGroup> iter = updateList.getListOfUpdates().iterator();
            while (iter.hasNext()) {
                RumUpdateGroup rumUpdateGroup = iter.next();
                // Clear the File Name.
                rumUpdateGroup.setFileName(null);
                // Set the Transaction ID.
                rumUpdateGroup.setWebTransactionId(transactionId);
            }
        }
    }
    
    /**
     * File-based RUM function uses this method.
     * Perform updates to DB which are defined in the UpdateList.
     *   
     * @param updateList
     * @param fileName
     * @param maxThreadsRunning
     * @return
     */
    private String doRUMUpdateForFileBased(UpdateList updateList, String fileName, int maxThreadsRunning) {

        StringBuffer responseBuffer = new StringBuffer("");

        final String methodName = RumStats.PERFORM_DB_UPDATE_METHOD + RumStats.FILE_NAME + fileName;
        logger.entry(HertzLogger.INFO, methodName);

        try {
            if (updateList != null) {

                Iterator<RumUpdateGroup> iter = updateList.getListOfUpdates().iterator();
                ArrayList<UpdateThread> threadList = new ArrayList<UpdateThread>();

                // Create a thread for each Place/Plan to update the database.
                long index = 0L;
                long threadId = System.currentTimeMillis();  // Base Thread ID

                while (iter.hasNext()) {
                    RumUpdateGroup rumUpdateGroup = iter.next();

                    // Create a thread to run to update for each sorted list
                    
                    // JWH: not sure why this test is here...
                    if (!rumUpdateGroup.isProcessed()) {

                        // Create a unique thread ID for this thread.
                        threadId = System.currentTimeMillis() + index;

                        UpdateThread updateThread = new UpdateThread(threadId, rumUpdateGroup, fileName, null, index, getLongRunningInMillis());
                        threadList.add(updateThread);

                        index++;
                    }
                }

                logger.info("<FB> Created " + index + " threads");

                // Start each thread
                Iterator<UpdateThread> threadListIter = threadList.iterator();

                while (threadListIter.hasNext()) {

                    UpdateThread updateThreadToStart = threadListIter.next();

                    if (getNumberOfThreadsRunning(threadList) < maxThreadsRunning) {
                        //logger.info("Starting Thread " + updateThreadToStart.getThreadId());
                        displayMemory("<FB1>");
                        updateThreadToStart.start();
                    }
                    else {
                        logger.info("<FB> Max number of " + maxThreadsRunning + 
                                " concurrent threads reached, Thread " + updateThreadToStart.getThreadId()
                                + " waiting for other threads to finish");
                        
                        int sleeps = 0;
                        while (getNumberOfThreadsRunning(threadList) >= maxThreadsRunning) {
                            // Do nothing, wait, must wait until thread becomes available
                            // logger.info("Waiting for spot in Thread Pool " + updateThreadToStart.getThreadId());
                            try {
                                Thread.sleep(10);
                                sleeps++;
                            }
                            catch (InterruptedException e) {
                                HertzException.eatAndLogNonCriticalException(LogLevel.ERROR, RumErrorCodes.GENERIC_ERROR, e, "Error when waiting for thread count to be reduced");
                            }
                        }

                        // Thread is the ID of the Place/Plan thread.
                        logger.info("<FB> Wait Over after " + sleeps + " .... Starting Thread " + updateThreadToStart.getThreadId());
                        displayMemory("<FB2>");
                        updateThreadToStart.start();
                    }
                }

                // Wait for each thread to be done.
                threadListIter = threadList.iterator();
                while (threadListIter.hasNext()) {
                    
                    UpdateThread threadToWaitFor = threadListIter.next();
                    while (!threadToWaitFor.isFinishedWithUpdate()) {
                        try {
                            // Wait one half second before checking again - report the first thread we found that wasn't done.
                            // Thread is the ID of the Place/Plan thread.
                            logger.info("<FB> Waiting for Thread " + threadToWaitFor.getThreadId() + " index: " + threadToWaitFor.getIndex() + " to finish");
                            Thread.sleep(500); // Sleep 0.5 seconds = 500 ms.
                        }
                        catch (InterruptedException e) {
                            HertzException.eatAndLogNonCriticalException(LogLevel.ERROR, RumErrorCodes.GENERIC_ERROR, e, "Error when waiting for threads to finish");
                        }
                    }

                    // Don't use the join method anymore, as ARRAY has the connection living inside it 
                    // and join can get a deadlock.
                    //                try {
                    //                    threadToWaitFor.join(900000); //wait for 10 minutes.
                    //                } catch (InterruptedException e) {
                    //                    logger.error(e.getMessage());
                    //                }              

                    // Mark all the groups as processed.
                    threadToWaitFor.getUpdateGroup().setProcessed(true);
                }

                logger.info("<FB> Started " + index + " threads");
                // Clear the list to assure that the threads get picked up by GC
                threadList.clear();
            }
        }
        finally {
            logger.exit(HertzLogger.INFO, methodName);
        }
        return responseBuffer.toString();
    }

    
    /**
     * Webservice-based RUM functions use this common method.
     * Perform updates to DB which are defined in the UpdateList.
     * Modified for PTR 11849 for web services call
     *   
     * @param updateList
     * @param transactionId
     * @param maxThreadsRunning
     * @return
     */
    private String doRUMUpdateForWebservice(UpdateList updateList, String transactionId) {

        StringBuffer responseBuffer = new StringBuffer("");

        final String methodName = RumWebStats.PERFORM_DB_UPDATE_METHOD + RumWebStats.TRANS_ID + transactionId;
        logger.entry(HertzLogger.INFO, methodName);

        try {
            incrementWSCalls();
            
            if (updateList != null) {

                Iterator<RumUpdateGroup> iter = updateList.getListOfUpdates().iterator();
                ArrayList<UpdateThread> threadList = new ArrayList<UpdateThread>();

                // Create a thread for each Place/Plan to update the database
                long index = 0L;
                long threadId = System.currentTimeMillis();  // Base Thread ID

                while (iter.hasNext()) {
                    RumUpdateGroup rumUpdateGroup = iter.next();

                    // Create a thread to run to update for each sorted list
                    
                    // @@JWH - not sure what this test is for... it should always be true I think but maybe this handles an empty call.
                    if (!rumUpdateGroup.isProcessed()) {

                        // Create a unique thread ID for this thread.
                        threadId = System.currentTimeMillis() + index;

                        UpdateThread updateThread = new UpdateThread(threadId, rumUpdateGroup, null, transactionId, index, getLongRunningInMillis());
                        threadList.add(updateThread);

                        index++;
                    }
                }

                logger.info("<WS> Trans ID: " + transactionId + " Server Call: " + totalWSCalls + " created " + index + " place/plan threads");

                // Send thread list to Thread Manager to have them started.
                WebServiceThreadManager.getWebThreadManager().queueNewWebserviceCallThreads(transactionId, threadList);

                // Wait for each thread to be done.
                Iterator<UpdateThread> threadListIter = threadList.iterator();
                while (threadListIter.hasNext()) {
                    
                    int checkCount = 0;
                    UpdateThread threadToWaitFor = threadListIter.next();
                    while (!threadToWaitFor.isFinishedWithUpdate()) {
                        try {
                            // Wait 100 ms. before checking again.
                            if (checkCount % 10 == 0) {
                                // Log only every 10 loops = 1 second.
                                logger.info("<WS> Trans ID: " + transactionId + " waiting for Thread " + threadToWaitFor.getThreadId() + " index: " + threadToWaitFor.getIndex() + " To Finish " + "after " + (checkCount / 10) + " seconds.");
                            }
                            checkCount++;
                            
                            Thread.sleep(100);  // PTR 11849 reduced sleep time to 100 ms.
                        }
                        catch (InterruptedException e) {
                            HertzException.eatAndLogNonCriticalException(LogLevel.ERROR, RumErrorCodes.GENERIC_ERROR, e, "Error when waiting for threads to finish");
                        }
                    }

                    // Don't use the join method anymore, as ARRAY has the connection living inside it 
                    // and join can get a deadlock.
                    //                try {
                    //                    threadToWaitFor.join(900000); //wait for 10 minutes.
                    //                } catch (InterruptedException e) {
                    //                    logger.error(e.getMessage());
                    //                }              

                    // Mark all the groups as processed.
                    threadToWaitFor.getUpdateGroup().setProcessed(true);
                    
                    // For web services call only PTR 11849

                    // Now check to see if there were errors during the update process
                    ArrayList<RumChangeDetails> details = threadToWaitFor.getUpdateGroup().getChangeDetails();

                    Iterator<RumChangeDetails> detailIter = details.iterator();
                    while (detailIter.hasNext()) {
                        RumChangeDetails changeDetails = detailIter.next();
                        responseBuffer.append(org.apache.commons.lang.StringUtils.isBlank(changeDetails.getResponseMessage()) ? "" : changeDetails.getResponseMessage());
                        
                        Exception e = changeDetails.getException();
                        if (e != null) {
                            // Update had an error: add it to the response
                            
                            String errorCode = ""; // Added this error code for each thread 
                            StringBuffer errorDesc = new StringBuffer(""); // Added this errorDesc
                            
                            if (e instanceof HertzException) {
                                HertzException hrtzExecption = (HertzException) e;
                                HertzErrorCode htzErrorCode = hrtzExecption.getErrorCode();
                                // HertzException.eatAndLogNonCriticalException(LogLevel.ERROR, htzErrorCode, e, "");
                                if (htzErrorCode instanceof RumErrorCodes) {
                                    errorCode = htzErrorCode.getCodeID();
                                    responseBuffer.append(errorCode);
                                    errorDesc.append(errorCode);
                                }
                                else {
                                    // errorCode = "Unknown HertzErrorCode: " + ((htzErrorCode != null) ? htzErrorCode.getClass().getName() : "null");
                                    OutputFileWriter.doNonHertzExceptionProcessing(errorDesc, e);
                                    // errorCode = "Unknown HertzErrorCode: " + ((htzErrorCode != null) ? htzErrorCode.getClass().getName() : "null");
                                    // OutputFileWriter.doNonHertzExceptionProcessing(responseBuffer, e);
                                }
                            }
                            else {
                                // errorCode = "Unknown HertzException: " + e.getClass().getName();
                                HertzException.eatAndLogNonCriticalException(LogLevel.ERROR, RumErrorCodes.GENERIC_ERROR, e, "");
                                OutputFileWriter.doNonHertzExceptionProcessing(errorDesc, e);
                                // responseBuffer.append(errorCode);
                            }
                            
                            responseBuffer.append(errorDesc);
                            logger.error("<WSD> Trans ID: " + transactionId + " Update was unsuccessful due to : " + errorDesc);
                        }
                        else {
                            // Update had no error
                            logger.info("<WSD> Trans ID: " + transactionId + " Update Was Successful");
                            responseBuffer.append(UPDATE_SUCCESS_MSG);
                        }

                        responseBuffer.append(" | ");
                    }
                }

                logger.info("<WS> Trans ID: " + transactionId + " Completed " + index + " threads");
                // Clear the list to ensure that the threads get picked up by GC.
                threadList.clear();
            }
        }
        finally {
            decrementWSCalls();
            Map<String, Integer> txnDMLCountMap = WebServiceThreadManager.txnDMLCountMap;
            if (null != txnDMLCountMap && !txnDMLCountMap.isEmpty()) {
                int dmlCount = (null != txnDMLCountMap.get(transactionId)) ? txnDMLCountMap.get(transactionId) : 0;
                logger.info("<WS> Trans ID: " + transactionId + " Total DML Rows Affected Count: " + dmlCount);
                try {
                    if (meterRegistry != null) {
                        meterRegistry.counter(RumMetrics.METRIC_RUM_RATES_DML_COUNT).increment(dmlCount);
                        meterRegistry.summary(RumMetrics.METRIC_RUM_RATES_DML_DISTRIBUTION).record(dmlCount);
                    }
                } catch (Exception e) {
                    logger.error("Metrics DML count exception: " + e.getMessage());
                }
            }
            logger.exit(HertzLogger.INFO, methodName);
        }
        return responseBuffer.toString();
    }
    
    /**
     * This will call the purging of the Rum Messages table. Fire and forget.
     *
     */
    public void doPurgeOfRumMessages(String capturedTime) {

        if (capturedTime != null) {
            IRumPurgeMessagesDataService reqTrnDataSvc = null;
            DataServiceLocator svcLookup = new DataServiceLocator();
            
            try {
                reqTrnDataSvc = (IRumPurgeMessagesDataService) svcLookup.getService(PURGE_DS_NAME);
            }
            catch (HertzException e) {
                logger.info("Error calling DB Purge routine.");
            }
            
            reqTrnDataSvc.doPurgeOfRumMessagesTable(capturedTime);
        }
    }

    /**
     * This method will sort the array list that holds all the update records from the 
     * input file and put them into a HashSet sorted by place, plan.
     * 
     * @param listOfUpdates - list of updates: all 'file rows' 
     * @param fileName - file name of input file
     * @return UpdateList - list of updates to a single Place/Plan/Company ID
     */
    private UpdateList sortUpdateFileRowsToRumUpdateGroups(ArrayList<UpdateRow> listOfUpdates, String fileName, String transactionId) throws HertzException {

        final String methodName;
        if (fileName != null) {
            methodName = "sortingFileRecords FileName>" + fileName;
        }
        else {
            // @@JWH - will this match ? Should RumWebStats.TRANS_ID be used?
            methodName = "sortingWebserviceRecords Transaction ID " + transactionId; //call PTR RATES-11542
        }
        logger.entry(HertzLogger.INFO, methodName);

        try {
            
            String comparePlanId = null;
            String comparePlace = null;
            String compareCompanyId = null;

            RumUpdateGroup rumGroup = null;
            // Returned list of updates to a single Place/Plan/Company ID.
            UpdateList updateList = new UpdateList();

            // Keep the order in which we received the records

            Iterator<UpdateRow> iter = listOfUpdates.iterator();
            while (iter.hasNext()) {
                // Changes for a single Date Span/Vehicle for a Plan/Place/Company ID
                UpdateRow row = iter.next();

                // If we have no data, then skip the row
                if (row.getLocation() == null || row.getPlanId() == null) {
                    continue;
                }

                if (rumGroup == null) {
                    // Initialize the update group for the file.
                    rumGroup = new RumUpdateGroup(fileName);
                    
                    rumGroup.setLocation(row.getLocation());
                    rumGroup.setPlaceTypeCode(row.getPlaceTypeCode());
                    rumGroup.setCompanyId(row.getCompanyId());
                    rumGroup.setPlanId(row.getPlanId());
                    rumGroup.setPlanType(row.getPlanIdTypeCode());
                    rumGroup.setClassTimeCode(row.getClassTimeCode());
                    rumGroup.setWebTransactionId(transactionId); // Added Transaction Id
                }

                if (comparePlace == null) {
                    comparePlace = row.getLocation();
                }

                if (compareCompanyId == null) {
                    compareCompanyId = row.getCompanyId();
                }

                if (comparePlanId == null) {
                    comparePlanId = row.getPlanId();
                }

                // If we match on plan id and place then we are still on the same Place/Plan so add to the list
                if (row.getLocation().equals(comparePlace) && row.getPlanId().equals(comparePlanId)) {
                    rumGroup.addChangeDetail(populateDetailsFromRow(row));
                }
                else {
                    // If we don't match then we have moved to a different Place and/or Plan.       

                    updateList.addRumUpdateGroup(rumGroup);

                    rumGroup = new RumUpdateGroup(fileName);

                    rumGroup.setLocation(row.getLocation());
                    rumGroup.setPlaceTypeCode(row.getPlaceTypeCode());

                    rumGroup.setCompanyId(row.getCompanyId());
                    rumGroup.setPlanId(row.getPlanId());
                    rumGroup.setPlanType(row.getPlanIdTypeCode());
                    rumGroup.setClassTimeCode(row.getClassTimeCode());
                    rumGroup.addChangeDetail(populateDetailsFromRow(row));
                    rumGroup.setWebTransactionId(transactionId); // Added Transition Id for Web services call 

                    // Update the compare variables for the new Place/Plan/Company ID.
                    comparePlanId = row.getPlanId();
                    comparePlace = row.getLocation();
                    compareCompanyId = row.getCompanyId();
                }
            }

            // Add the last one
            updateList.addRumUpdateGroup(rumGroup);

            return updateList;
        }
        finally {
            logger.exit(HertzLogger.INFO, methodName);
        }
    }

    /**
     * Convert an UpdateRow into a RumChangeDetails.
     * @@JWH - the Company ID gets lost from the UpdateRow.
     * @param row
     * @return
     */
    private static RumChangeDetails populateDetailsFromRow(UpdateRow row) {

        RumChangeDetails details = new RumChangeDetails();
        details.setSequenceNumber(row.getSequenceNumber());
        details.setEndDate(row.getEndDate());

        RumErrorCodes errorCode = row.getErrorCode();
        details.setErrorCode(errorCode);

        if (errorCode != null) {
            details.setResponseMessage("FAILED");
            details.setException(new HertzException(errorCode, false));
        }

        details.setExtraDay(row.getExtraDay());
        details.setExtraHour(row.getExtraHour());
        details.setRate(row.getRate());
        details.setStartDate(row.getStartDate());
        details.setVehicle(row.getVehicle());
        details.setRegion(row.getRegion());

        return details;
    }

    //    /**
    //     * This method will map each thread's response by file name.
    //     * 
    //     * @param String fileName
    //     * @param IRumUpdateResponse response
    //     */
    //
    //  public synchronized static void mapThreadResponse(String fileName, RumUpdateGroup group){
    //      
    //      ArrayList listOfResponses = (ArrayList) inputFileToThreadResponseMap.get(fileName);
    //      if (listOfResponses == null){
    //          listOfResponses = new ArrayList();
    //      }
    //      listOfResponses.add(group);
    //      
    //      inputFileToThreadResponseMap.put(fileName , listOfResponses);       
    //  }

    /**
     * This method will write the output file by gathering all the responses
     * added to the HashMap for a particular file name.
     * 
     * @param String fileName
     */
    public synchronized static void writeOutputFile(UpdateList updateList, String fileName, String fileError) {

        final String methodName = "writeOutputFile FileName>" + fileName;
        logger.entry(LogLevel.INFO, methodName);

        OutputFileWriter fileWriter = new OutputFileWriter();
        fileWriter.writeOutput(updateList, fileName, fileError);

        logger.exit(LogLevel.INFO, methodName);
    }

    /**
     * Both File-based and Webservice functions use this method.
     * This will get the place id code for the area location from the database and populate
     * the row objects with the place id code
     * 
     * @param mapOfUpdates
     * @return
     * @throws HertzException
     * @throws SQLException
     */
    private static UpdateList findPlaceTypeIdCode(UpdateList updateList, String fileName, String transId) {

        final String methodName;
        if (fileName != null) {
            methodName = RumStats.RETRIEVE_LOCATION_INFORMATION_METHOD + RumStats.FILE_NAME + fileName;
        }
        else {
            methodName = RumWebStats.RETRIEVE_LOCATION_INFORMATION_METHOD + RumWebStats.TRANS_ID + transId;
        }
        logger.entry(HertzLogger.INFO, methodName);
        
        IRumGetPlaceTypeIdCodeDataService reqTrnDataSvc = null;

        try {
            DataServiceLocator svcLookup = new DataServiceLocator();
            try {
                reqTrnDataSvc = (IRumGetPlaceTypeIdCodeDataService) svcLookup.getService(UPDATE_DS_NAME);
            }
            catch (HertzException e) {
                logger.info("Fatal Exception caught ... Performing error routine");
                updateList.addErrorMessageToRumGroup(e);
            }

            AreaLocationsToPlaceIdCodeList areaLocationsAlreadyFound = getAreaLocation();

            Iterator<RumUpdateGroup> iter = updateList.getListOfUpdates().iterator();
            while (iter.hasNext()) {

                RumUpdateGroup updateGroup = iter.next();
                if (updateGroup != null) {
                    //If we have an area location number, then go get the place type code
                    String groupLocation = updateGroup.getLocation();
                    if (groupLocation != null && (StringUtils.isNumeric(groupLocation) ||
                            groupLocation.length() == 9)) {
                        
                        // JWH: Messy - is either an AreaLocation or a CountryStateCityLoc (implied with length = 9)
                        String updateGroupLocation = updateGroup.getLocation();
                        if (areaLocationsAlreadyFound.isLocationInList(updateGroupLocation)) {
                            // Location is cached
                            logger.info("Found Area Number : " + updateGroupLocation + " in Cache!");
                            RumLocationInfo rumLocationInfo = areaLocationsAlreadyFound.getRumLocationInfo(updateGroupLocation);
                            if (rumLocationInfo.getException() != null) {
                                updateGroup.addErrorMessageToDetails(rumLocationInfo.getException());
                            }
                            else {
                                updateGroup.setPlaceIdCd(rumLocationInfo.getPlaceIdCode());
                                updateGroup.setPlaceTypeCode(rumLocationInfo.getPlaceTypeCode());
                            }
                        }
                        else {
                            // Location is NOT cached
                            logger.info("Area Number : " + updateGroupLocation + " Not in Cache, getting from DB");
                            try {
                                updateGroup = reqTrnDataSvc.getPlaceIdTypeCode(updateGroup);
                                areaLocationsAlreadyFound.addLocation(new RumLocationInfo(updateGroup.getLocation(), updateGroup.getPlaceIdCd(), updateGroup.getPlaceTypeCode(), null));
                            }
                            catch (HertzException e) {
                                logger.info("Exception caught for Area Location " + updateGroup.getLocation() + " ... Performing error routine");
                                updateGroup.addErrorMessageToDetails(e);
                                //RATES-8111 DJA - I commented this line out so that when an exception is thrown on the AREA/LOC lookup we won't cache the error.
                                //                 That way we'll always go to the database for that location's info until the data is fixed and the exception goes away.
                                //      areaLocationsAlreadyFound.addLocation(new RumLocationInfo(updateGroup.getLocation(), null, null, e));
                                continue;
                            }
                            catch (SQLException e) {
                                logger.info("Exception caught for Area Location " + updateGroup.getLocation() + " ... Performing error routine");
                                updateGroup.addErrorMessageToDetails(e);
                                //RATES-8111 DJA - I commented this line out so that when an exception is thrown on the AREA/LOC lookup we won't cache the error.
                                //                 That way we'll always go to the database for that location's info until the data is fixed and the exception goes away.
                                //      areaLocationsAlreadyFound.addLocation(new RumLocationInfo(updateGroup.getLocation(), null, null, e));
                                continue;
                            }
                        }
                    }
                }
            }
        }
        finally {
            logger.exit(HertzLogger.INFO, methodName);
        }

        return updateList;
    }

    /**
     * Iterate through the thread list and count how many are still 'alive', e.g. have not completed or errored.
     * @param threadList
     * @return
     */
    private static int getNumberOfThreadsRunning(ArrayList<UpdateThread> threadList) {

        Iterator<UpdateThread> iter = threadList.iterator();
        int count = 0;
        while (iter.hasNext()) {
            UpdateThread thread = iter.next();
            if (thread.isAlive()) {
                count++;
            }
        }

        return count;
    }
    
    /**
     * Webservices RUM : Webservice call is processed starting here.
     * @param updateString
     * @param clientIP
     * @return
     */
    public String doWebServiceUpdate(String updateString, String clientIP) {

        String clientCountryCode = NO_COUNTRY;
        String transactionId = GUIDGenerator.generateGUID(updateString, 24);
        
        final String methodName = RumWebStats.DO_WEBSERVICE_UPDATE + RumWebStats.TRANS_ID + transactionId;
        logger.entry(LogLevel.INFO, methodName);

        logger.info("<WSD> Trans ID: " + transactionId + " Message Received : " + updateString); // Added logging RATES-11876/RATES-11849

        StringBuffer responseBuffer = new StringBuffer();
        responseBuffer.append("Transaction Id : ");
        responseBuffer.append(transactionId);
        responseBuffer.append(" - ");
        UpdateList updateList = null;
        ArrayList<UpdateRow> listUpdateRow = new ArrayList<UpdateRow>();

        try {
            FastStringTokenizer inputTokenizer = new FastStringTokenizer(updateString, '|');
            updateList = new UpdateList();

            UpdateRowMapper mapper = new UpdateRowMapper();
            while (inputTokenizer.hasMoreTokens()) {
                String updateFromInput = inputTokenizer.nextToken();
                // System.out.println("String value ==> " + updateFromInput);

                // Map the input from the WebService.

                UpdateRow row = mapper.convertLineToObject(updateFromInput, 1);
                listUpdateRow.add(row);
            }
            
            // Extract country code from location if available
            if (listUpdateRow != null && listUpdateRow.size() > 0) {
                String location = listUpdateRow.get(0).getLocation();
                if (StringUtils.isNotEmpty(location) && location.length() > 2) {
                    clientCountryCode = location.substring(0, 2);
                }
            }
            
            final int listSize = listUpdateRow.size();
            
            // Report the number of changes for this message.
            logger.info("<WSD> Trans ID: " + transactionId + " Changes in Message: > " + listSize + " <");
            
            try {
                if (meterRegistry != null) {
                    meterRegistry.counter(RumMetrics.METRIC_RUM_RATES_RMS_RECORD_COUNT).increment(listSize);
                    meterRegistry.summary(RumMetrics.METRIC_RUM_RATES_RMS_RECORD_DISTRIBUTION_COUNT).record(listSize);
                }
            } catch (Exception e) {
            	logger.error("Dynatrace incremenCounter rows exception: " + e.getMessage());
            }

            /* Adding from doUpdate method () -PTR RATES-11876 */
            updateList = sortUpdateFileRowsToRumUpdateGroups(listUpdateRow, null, transactionId);

            // We don't need the records list anymore, clear it to preserve space.
            listUpdateRow.clear();

            findPlaceTypeIdCode(updateList, null, transactionId);
            
            if (updateList != null) {

                while (!updateList.allGroupsProcessed()) {
                    String response = doRUMUpdateForWebservice(updateList, transactionId);
                    responseBuffer.append(response);
                }
            }
            logger.info("<WS> Trans ID for web before catch: " + transactionId + " Update Was Successful");
        }
        catch (HertzException e) {
            logger.info("<WS> Trans ID: " + transactionId + " Error processing Webservices Request.");
        }
        finally {
           
            logger.exit(LogLevel.INFO, methodName);
            saveWebServiceStatistics(transactionId, updateList);

        }
        
        // Record rate update metrics
        recordRateUpdateMetrics(transactionId, responseBuffer.toString(), clientIP, clientCountryCode);
        
        return responseBuffer.toString();
    }

    /**
     * Gather and write the final statistics for a Web Services call.
     * @param transactionId
     * @param updateList
     */
    private static void saveWebServiceStatistics(String transactionId, UpdateList updateList) {
        
        RumWebStats stats = RumWebStats.getRumWebStatsObject();
        RumWebStatsBean bean = stats.getBeanFromStack(transactionId);

        if (bean != null && updateList != null) {

            DataServiceLocator svcLookup = new DataServiceLocator();

            IRumWebHistoricalDataService reqTrnDataSvc = null;

            try {
                reqTrnDataSvc = (IRumWebHistoricalDataService) svcLookup.getService(WEB_HISTORICAL_DS_NAME);
                Iterator<RumUpdateGroup> iter = updateList.getListOfUpdates().iterator();
                
                while (iter.hasNext()) {
                    RumUpdateGroup rumUpdateGroup = iter.next();
                    reqTrnDataSvc.insertRumWebTransactionHistory(bean, rumUpdateGroup);
                }
            }
            catch (HertzException e) {
                HertzException.eatAndLogNonCriticalException(LogLevel.ERROR, CommonErrorCodes.UNEXPECTED_ERR, e, "Error writing Web Transaction History");
            }
        }
    }
    
    
    //    /**
    //     * RATES-11849 FAKE
    //     * This returns whether RUM should be run 'without' DB calls.  
    //     * @return true if should run without DB.
    //     */
    //    public static boolean isRunWithNoDb() {
    //    
    //        return RUN_WITH_NO_DB;
    //    }
    

    /**
     * Increment the number of simultaneous Webservices calls and report if this is a new maximum.
     * 
     * Since the webservice calls build up quickly to their maximum after a server is restarted and 
     * begins running, we don't really see this message in the logs after running a few hours.
     */
    public static void incrementWSCalls() {
        
        synchronized (wsCallLock) {

            totalWSCalls++;
            
            currentWSCalls++;
            if (currentWSCalls > maxSimultaneousWSCalls) {
                maxSimultaneousWSCalls = currentWSCalls;
                logger.info("<WS> New Max Simultaneous WS Calls: " + maxSimultaneousWSCalls);
            }
        }
    }

    /**
     * Decrement the number of simultaneous Webservices calls.
     */
    public static void decrementWSCalls() {

        synchronized (wsCallLock) {
            currentWSCalls--;
        }
    }
    
    /*
     * *************** Configuration Data *************************************************************************
     */
    
    /**
     * Return a value to be used for the Processing Path for File-Based Updates.
     * If there is an override from Config data, use it.
     * This might include an override from a XML config file.
     * @return
     */
    private static int getProcessingPathForFileBasedUpdates() {

        int processingPath = PATH_NORMAL;
        
        final String GROUP = "WebServicesControl";
        final String PROPERTY = "ProcessingPathForFileBasedUpdates";

        try {
            ConfigData configData = ConfigData.getInstance();
            if (configData == null) {
                // Could throw an exception here.  Or just use the default value.
                logger.debug("getProcessingPathForFileBasedUpdates - invalid ConfigData");
            }
            else {
                PropertyGroup propertyGroup = configData.getGroup(GROUP);
                if (propertyGroup == null) {
                    logger.debug("getProcessingPathForFileBasedUpdates - propertyGroup is null");
                }
                else {
                    String processingPathStr = propertyGroup.getPropertyValue(PROPERTY);
                    if (processingPathStr == null) {
                        // Could throw an exception here.  Or just use the default value.
                        logger.debug("getProcessingPathForFileBasedUpdates - property value is null");
                    }
                    else {
                        try {
                            int value = Integer.parseInt(processingPathStr.trim());
                            processingPath = value;
                        }
                        catch (NumberFormatException e) {
                            // Could throw an exception here.  Or just use the default value.
                            logger.error("getProcessingPathForFileBasedUpdates - invalid value for maxThreads: >" + processingPathStr + "<");
                        }
                    }
                }
            }
        }
        catch (HertzException e) {
            // Do nothing, just return the default value.
            logger.debug("getProcessingPathForFileBasedUpdates - ConfigData issue");
        }

        logger.debug("Using getProcessingPathForFileBasedUpdates: " + processingPath);
        return processingPath;
    }

    /**
     * Return the configurable parameter for the number of milliseconds that
     * a thread can run before it is considered a 'Long Running Thread'.
     * @return
     */
    public long getLongRunningInMillis() {

        if (this.longRunningInMillis < 0) {
            // Initialize using default if there isn't a setting on config.
            this.longRunningInMillis = LONG_RUNNING_IN_MILLIS_DEFAULT;

            final String GROUP = "WebServicesControl";
            final String PROPERTY = "LongRunningCallInMillis";

            try {
                ConfigData configData = ConfigData.getInstance();
                if (configData == null) {
                    // Could throw an exception here.  Or just use the default value.
                    logger.debug("getLongRunningInMillis - invalid ConfigData");
                }
                else {
                    PropertyGroup propertyGroup = configData.getGroup(GROUP);
                    if (propertyGroup == null) {
                        logger.debug("getLongRunningInMillis - propertyGroup is null");
                    }
                    else {
                        String processingPathStr = propertyGroup.getPropertyValue(PROPERTY);
                        if (processingPathStr == null) {
                            // Could throw an exception here.  Or just use the default value.
                            logger.debug("getLongRunningInMillis - property value is null");
                        }
                        else {
                            try {
                                int value = Integer.parseInt(processingPathStr.trim());
                                this.longRunningInMillis = value;
                            }
                            catch (NumberFormatException e) {
                                // Could throw an exception here.  Or just use the default value.
                                logger.error("getLongRunningInMillis - invalid value for LongRunningInMillis: >" + processingPathStr + "<");
                            }
                        }
                    }
                }
            }
            catch (HertzException e) {
                // Do nothing, just return the default value.
                logger.debug("getLongRunningInMillis - ConfigData issue");
            }

            logger.debug("Using getLongRunningInMillis: " + this.longRunningInMillis);
        }

        return this.longRunningInMillis;
    }
    
    /**
     * Updating Micrometer Metrics for a given RUM Transaction.
     * 
     * @param transactionId
     * @param responseBuffer
     * @param clientIP
     * @param clientCountry
     */
    public void recordRateUpdateMetrics(String transactionId, String responseBuffer, String clientIP, String clientCountry) {

        int noOfRateUpdatesSuccessful = 0;
        int noOfRateUpdatesFail = 0;
        
        // Ensure non-null values for metrics tags
        String safeClientIP = (clientIP != null) ? clientIP : "NO_IP_FOUND";
        String safeClientCountry = (clientCountry != null) ? clientCountry : "NO_COUNTRY";

        if (StringUtils.isNotEmpty(transactionId) && StringUtils.isNotEmpty(responseBuffer)) {
            String[] rateUpdateResponses = responseBuffer.toString().split("\\|");
            for (String rateUpdateResponse : rateUpdateResponses) {
                if (StringUtils.isNotEmpty(rateUpdateResponse)) {
                    if (rateUpdateResponse.toLowerCase().contains("success")) {
                        noOfRateUpdatesSuccessful++;
                    } else if (rateUpdateResponse.toLowerCase().contains("fail")) {
                        noOfRateUpdatesFail++;
                    }
                }
            }
            
            logger.info("<WS> Trans ID: " + transactionId + " , RATE_UPDATES_SUCCESS_COUNT : " + noOfRateUpdatesSuccessful + " , RATE_UPDATES_FAIL_COUNT : " + noOfRateUpdatesFail);
            
            if (meterRegistry != null) {
                meterRegistry.counter(RumMetrics.METRIC_RUM_RATE_UPDATE_SUCCESS_COUNT).increment(noOfRateUpdatesSuccessful);
                meterRegistry.counter(RumMetrics.METRIC_RUM_RATE_UPDATE_FAIL_COUNT).increment(noOfRateUpdatesFail);
                meterRegistry.counter(RumMetrics.METRIC_RUM_CLIENT_IP_RUP_SUCCESS_COUNT, CLIENT_IP, safeClientIP).increment(noOfRateUpdatesSuccessful);
                meterRegistry.counter(RumMetrics.METRIC_RUM_CLIENT_IP_RUP_FAILURE_COUNT, CLIENT_IP, safeClientIP).increment(noOfRateUpdatesFail);
                meterRegistry.counter(RumMetrics.METRIC_RUM_CLIENT_COUNTRY_RUP_SUCCESS_COUNT, CLIENT_COUNTRY, safeClientCountry).increment(noOfRateUpdatesSuccessful);
                meterRegistry.counter(RumMetrics.METRIC_RUM_CLIENT_COUNTRY_RUP_FAILURE_COUNT, CLIENT_COUNTRY, safeClientCountry).increment(noOfRateUpdatesFail);
                meterRegistry.counter(RumMetrics.METRIC_RUM_CLIENT_COUNTRY_API_CALL_COUNT, CLIENT_COUNTRY_API_CALL, safeClientCountry).increment(1);
            }
        }
    }
    
    /**
     * Display JVM Memory Statistics.
     */
    public static void displayMemory(String title) {

        // Set true to turn off this logging
        boolean activated = true;
        
        if (!activated || !logger.isDebugEnabled()) {
            // Debug logging is not enabled so just return.
            return;
        }
        
        StringBuffer s = new StringBuffer();

        long freeMemory = Runtime.getRuntime().freeMemory();
        long maxMemory = Runtime.getRuntime().maxMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();

        long allocatedMemory = totalMemory - freeMemory;
        long presumablyFreeMemory = maxMemory - allocatedMemory;
        long definitelyFreeMemory = freeMemory;

        if (presumablyFreeMemory < presumablyFreeMemoryLowest) {
            presumablyFreeMemoryLowest = presumablyFreeMemory;
            logger.debug(title + " MEMORY - new presumablyFreeMemoryLowest: " + presumablyFreeMemoryLowest);
        }

        if (definitelyFreeMemory < definitelyFreeMemoryLowest) {
            definitelyFreeMemoryLowest = definitelyFreeMemory;
            logger.debug(title + " MEMORY - new definitelyFreeMemoryLowest: " + definitelyFreeMemoryLowest);
        }

        s.append(title);
        s.append(" MEMORY:");

        s.append(" Free Memory: ");
        s.append(freeMemory);

        s.append(" Max Memory: ");
        s.append(maxMemory);

        s.append(" Total Memory: ");
        s.append(totalMemory);

        s.append(" Allocated Memory: ");
        s.append(allocatedMemory);

        s.append(" Presumabley Free Memory: ");
        s.append(presumablyFreeMemory);

        s.append(" Definitely Free Memory: ");
        s.append(definitelyFreeMemory);

        logger.debug(s.toString());
    }
}

/*
 *************************************************************
 * Change History:
 *
 * $Log$
 * Revision 1.66  2018/08/10 20:38:29  dtp4395
 * RATES-12578; Heflin; Formatted one line, removed 2 unneeded typecasts, fixed spelling.
 *
 * Revision 1.65  2017/11/19 02:33:51  dtp4395
 * RATES-12329; Heflin; Modified logging.
 *
 * Revision 1.64  2017/11/16 23:32:08  dtp4395
 * RATES-12329; Heflin; Improvded logging and thread identification.
 *
 * Revision 1.63  2017/11/14 22:34:35  dtp4395
 * RATES-12329; Heflin; Fixed spelling in comment.
 *
 * Revision 1.62  2017/11/01 16:07:26  dtp4395
 * RATES-12061; Heflin; Improved logging in doRUMUpdateForWebservice() to designate threads are completed.
 *
 * Revision 1.61  2017/10/27 19:19:21  dtp4395
 * RATES-12320; Heflin; Improved logging messages by adding Trans ID.
 *
 * Revision 1.60  2017/09/14 19:18:27  dtp4395
 * RATES-12061; Heflin; Fixed Web statistics collection.
 *
 * Revision 1.59  2017/03/08 07:09:40  dtp4395
 * RATES-11849; Heflin; Corrected issues with the processing for testing Web Services from files.
 *
 * Revision 1.58  2017/03/06 01:52:47  dtp4395
 * RATES-11849; Heflin; Added quick exit on memory logging if debug isn't enabled.
 *
 * Revision 1.57  2017/03/06 01:49:22  dtp4395
 * RATES-11849; Heflin; Change to displayMemory().
 *
 * Revision 1.56  2017/03/02 22:47:49  dtp4395
 * RATES-11849; Heflin; Report memory useage.
 *
 * Revision 1.55  2017/03/01 05:19:01  dtp4395
 * RATES-11849; Heflin; Added functionality to detect and report long-running threads and count the maximum simultaneous Webservice calls encountered.
 *
 * Revision 1.54  2017/02/22 21:58:58  dtp4395
 * RATES-11849; Heflin; Refactoring to improve logging and monitoring.
 *
 * Revision 1.53  2017/02/22 01:51:58  dtp4395
 * RATES-11849; Heflin; Merged changes from 17A branch.
 *
 * Revision 1.38.2.9  2017/02/22 01:30:06  dtp4395
 * RATES-11849; Heflin; Split processing path so there are separate ones for File-based versus WebServices. Added test path for sending files updates through Webservices Processing.
 * Added interface to Thread Manager.
 *
 * Revision 1.38.2.8  2017/01/16 23:14:15  dtp4395
 * RATES-11849; Heflin; Merged changes from HEAD.
 *
 * Revision 1.52  2017/02/01 20:53:16  dtp4395
 * RATES-11849; Heflin; Reduced max WS thread count from 10 to 3, added logging, added comments.
 *
 * Revision 1.51  2017/01/16 22:45:33  dtp4395
 * RATES-11849; Heflin; Deactivated run without DB setting.
 *
 * Revision 1.50  2017/01/16 22:35:21  dtp4395
 * RATES-11849; Heflin; Added debug boolean and getter to bypass DB calls, refactored methodName in all entry/exit logging, added comments.
 *
 * Revision 1.49  2017/01/11 23:06:21  dtp4395
 * RATES-11849; TESTING - Added transaction ID to class.  Add transaction ID to entry/exit methodNames for Webservice calls.  Put entry/exit calls into final blocks.
 *
 * Revision 1.48  2017/01/10 19:37:36  dtp4395
 * RATES-11849; TESTING - Added testing switch.
 *
 * Revision 1.47  2017/01/10 05:12:09  dtp4395
 * RATES-11849; Fixed method name in logger.exit in sortUpdateFileRowsToRumUpdateGroups; Corrected comment.
 *
 * Revision 1.46  2017/01/09 18:04:14  dtp4395
 * RATES-11849; Added warning comments about lack of delay in doRUMUpdate().  Changed logger.entry text in sortUpdateFileRowsToRumUpdateGroups (probably WRONG) and fixed logger.exit at end.
 *
 * Revision 1.45  2016/12/22 19:40:43  dtp4395
 * RATES-11849; Heflin; Formatted only.
 *
 * Revision 1.44  2016/12/22 17:55:59  dtp4395
 * RATES-11849; Heflin; Formatted only.
 *
 * Revision 1.43  2016/12/20 15:07:46  dtc4323
 * Merged PTR 11849 Restore Exception Handling to original
 *
 * Revision 1.42  2016/12/15 16:09:20  dtc4323
 * Added Logging- PTR 11849
 *
 * Revision 1.41  2016/12/02 16:19:16  dtc4323
 * RATES-11849 -Modification in doRUMUpdate() logging .
 *
 * Revision 1.40  2016/11/29 18:14:23  dtp4395
 * RATES-11849; Heflin; Ran format only.
 *
 * Revision 1.39  2016/11/29 15:52:31  dtc4323
 * Modified  doRUMUpdate()  PTR 11849
 *
 * Revision 1.38  2016/11/15 18:15:30  dtc4323
 * PTR RATES-11876- Modified doWebServiceUpdate()method
 *
 * Revision 1.37  2016/11/08 19:04:43  dtp4395
 * RATES-11849; Heflin; Added comments. No code changes.
 *
 * Revision 1.36  2016/10/31 20:57:36  dtp4395
 * RATES-11849; Heflin; Added/fixed comments.  Formatted.  No code changes.
 *
 * Revision 1.35  2016/10/27 15:08:48  dtp4395
 * RATES-11849; Heflin; Added/fixed comments.  Organized imports. Formatting only.
 *
 * Revision 1.34  2015/12/10 22:07:53  dtc1090
 * RATES-11372 - don't error the whole request when just 1 update is bad
 *
 * Revision 1.33  2015/01/15 15:44:16  dtc1090
 * RATES-8888 - must send back multiple messages when we get multiple updates
 *
 * Revision 1.32  2014/10/30 14:42:17  dtc1090
 * RATES-8502 - adding the logging of the message received
 *
 * Revision 1.31  2014/09/29 13:52:34  dtc1090
 * Checked in new code for DTAG Update Webservice
 *
 * Revision 1.30  2014/09/04 15:13:49  dtp0540
 * RATES-8111 DJA - don't write an area/loc to cache when it doesn't exist.
 *
 * Revision 1.29  2014/08/05 14:54:50  dtc1090
 * SR 59911 - adding new WebService layer to RUM
 *
 * Revision 1.28  2014/07/31 20:24:46  dtc1090
 * RATES-8035 - fixed deadlock on connection when using ARRAY
 *
 * Revision 1.27  2014/06/04 19:48:46  dtc1090
 * RATES-7941 - Put a Limit on the Number of Rows allowed per file
 *
 * Revision 1.26  2014/04/29 16:12:05  dtc1090
 * RATES-7814 - Maklee Enhancements - Using more than 1 version of update proc and adding functionality to RUM to update more than 1 file at a time.
 *
 * Revision 1.25  2013/10/10 21:37:19  dtc1090
 * RATES-7388 - skip rows that have no data
 *
 * Revision 1.24  2013/09/04 14:44:06  dtc1090
 * Merged DTG branch into HEAD
 *
 * Revision 1.23  2013/08/09 15:45:50  dtc1090
 * RATES-7143 - Need to update database when an exception occurs during the processing so RUM will go on to the next file
 *
 * Revision 1.22  2013/06/25 15:08:46  dtp0540
 * DJA - I accidentally committed the company_id changes to the head too soon.  So I'm reverting back to the previous historical modification.
 *
 * Revision 1.20  2012/08/20 18:03:31  dtc1090
 * SR-54472 - RUM Processing Changes
 *
 * Revision 1.19  2012/01/09 19:20:21  dtc1090
 * RATES-5641 - SR52946 - Addendums 1 & 2
 *
 * Revision 1.18  2011/11/16 17:16:32  dtc1090
 * RATES-5733 - split up creating threads into 'batches' to help conserve space
 *
 * Revision 1.17  2011/09/20 17:40:06  dtc1090
 * changed to write rum stats to db after each file processes
 *
 * Revision 1.16  2011/06/06 14:56:53  dtc1090
 * Changed the way we record the stats.  Trying to fix concurrent modification problem.
 *
 * Revision 1.15  2011/05/19 19:13:48  dtc1090
 * SR 500044.01 (RUM Addendum 1) added the abilty to handle users sending in CCSS in the location field.  Also to write a seperate output file for locked plan/places
 *
 * Revision 1.14  2011/03/18 16:50:45  dtc1090
 * Added more Stat gathering
 *
 * Revision 1.13  2011/03/15 16:03:20  dtc1090
 * added new functionality
 *
 * Revision 1.12  2011/03/08 18:32:33  dtc1090
 * merged changes from qual branch
 *
 * Revision 1.10.2.3  2011/03/08 17:15:38  dtc1090
 * added code to only get the place code once per area location number
 *
 * Revision 1.10.2.2  2011/03/08 02:09:52  dtc1090
 * merged needed changes
 *
 * Revision 1.11  2011/03/07 15:08:40  dtc1090
 * Modified to code to not log as much
 *
 * Revision 1.10  2011/03/01 14:43:09  dtc1090
 * fixed memory leak
 *
 * Revision 1.9  2011/02/28 23:19:14  dtc1090
 * organized imports
 *
 * Revision 1.8  2011/02/28 23:00:38  dtc1090
 * refactored RUM
 *
 * Revision 1.7  2011/02/25 16:15:59  dtc1090
 * fixed log statements and purging
 *
 * Revision 1.6  2011/02/24 16:15:14  dtc1090
 * changed the way we control number of threads
 *
 * Revision 1.5  2011/02/24 14:28:20  dtc1090
 * modified how we create thread id
 *
 * Revision 1.4  2011/02/23 17:24:11  dtc1090
 * added governor to control number of concurrent updates
 *
 * Revision 1.3  2011/01/31 20:54:44  dtc1090
 * continued development
 *
 * Revision 1.2  2011/01/31 17:33:24  dtc1090
 * Added new code for development
 *
 * Revision 1.1  2011/01/27 20:04:47  dtc1090
 * rearranged packages
 *
 * Revision 1.3  2011/01/27 20:01:18  dtc1090
 * development add
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