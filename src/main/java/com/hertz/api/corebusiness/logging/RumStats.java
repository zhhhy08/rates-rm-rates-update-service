package com.hertz.api.corebusiness.logging;

import java.util.Iterator;

import org.apache.commons.collections.ArrayStack;

import com.hertz.rates.common.utils.HertzException;
import com.hertz.rates.common.utils.config.ConfigData;
import com.hertz.rates.common.utils.config.PropertyGroup;
import com.hertz.rates.common.utils.logging.ElapsedTime;
import com.hertz.rates.common.utils.logging.EntryExit;
import com.hertz.rates.common.utils.logging.LoggingStats;
import com.hertz.api.corebusiness.RumStatsBean;

/**
 * Statistics collection for RUM File process
 *
 */
public class RumStats implements LoggingStats {

    //
    // Define constants used in logger.entry/exit and denyOrAcceptElapsedTimeEvent, addElapsedTime methods.
    //
    public static final String FILE_NAME = " FileName>";
    public static final String RECORDS_UPDATED_METHOD = "Number of records to update: ";
    public static final String RETRIEVE_LOCATION_INFORMATION_METHOD = "findPlaceTypeIdCode ";
    public static final String PERFORM_DB_UPDATE_METHOD = "performDBUpdate ";

    
    private static final String RUM_DB_UPDATE_METHOD = "executeStoredProcedure RumUpdate";
    private static final String RECORD_COUNT_METHOD = "NUMBER OF RECORDS IN FILE:";
    private static final String RUM_FILE_UPDATE_METHOD = "doUpdate";
    private static final String FILE_BEING_UPDATED_METHOD = "Current File";
    private static final String SORTING_FILE_RECORDS_METHOD = "sortingFileRecords";
    private static final String WRITE_OUTPUT_FILE_METHOD = "writeOutputFile";
    
    public static final String CONCURRENT_UPDATES_NAME = "ConcurrentUpdates";
    public static final String RUM_UPDATES_ENTRY = "RumUpdates";
    public static final String NUMBER_OF_CONCURRENT_UPDATES = "numberOfConcurrentUpdates";

    // Create local constants for logger constants for cleaner code
    private static final int ACCEPT = org.apache.log4j.spi.Filter.ACCEPT;
    private static final int DENY = org.apache.log4j.spi.Filter.DENY;
    
    public static final int DEFAULT_TIME_BETWEEN_CAPTURES = 10000;  // In milliseconds - 10 seconds

    public static final int MAX_NUMBER_OF_HISTORICAL_BEANS = 8640;  // Allows for 24 hours of 10 second statistics updates.

    /** A singleton of this class. */
    private volatile static RumStats rumStatsSingleton;

    // Note: Our version of ArrayStack is not generic in the Apache library we are using so don't try to do that.
    
    /** Stack of History Beans. */
    private ArrayStack historyBeans;
    
    private ArrayStack fileHistoryBeans;

    
    private long lastTimeStatsCaptured = 0L;


    /**
     * Constructor
     */
    private RumStats() {

        this.historyBeans = new ArrayStack(MAX_NUMBER_OF_HISTORICAL_BEANS);
        this.fileHistoryBeans = new ArrayStack(MAX_NUMBER_OF_HISTORICAL_BEANS);
    }

    /**
     * Return the singleton statistics object - of this class type.
     * @return
     */
    public static RumStats getRumStatsObject() {

        if (rumStatsSingleton == null) {
            
            synchronized (RumStats.class) {
                if (rumStatsSingleton == null) {
                    rumStatsSingleton = new RumStats();
                }
            }
        }

        return rumStatsSingleton;
    }

    /**
     * Do a statistics capture if:
     *  - it is being forced, e.g. doCapture = true, or
     *  - we have never captured statistics, or
     *  - there has been sufficient time since the last capture
     *  
     * @param doCapture- true forces a capture
     * @param fileName
     */
    private synchronized void checkForCapture(boolean doCapture, String fileName) {

        long curTime = System.currentTimeMillis();

        if (doCapture || 
            (getLastTimeStatsCaptured() <= 0L || 
            ((curTime - getLastTimeStatsCaptured()) > DEFAULT_TIME_BETWEEN_CAPTURES))) {

            doStatsCapture(curTime, fileName);
        }
    }

    /**
     * Capture statistics for the specified file.
     * @param curTime
     * @param fileName
     */
    private synchronized void doStatsCapture(long curTime, String fileName) {

        if (getLastTimeStatsCaptured() <= 0L) {
            // We've never captured statistics so record time.
            setLastTimeStatsCaptured(curTime);
        }

        // Get the statistics bean for this file name.
        RumStatsBean historicalBean = getRumStatsBeanForFileName(fileName);

        // Add historical bean to history stack.
        this.addHistoryBeanToBuffer(getHistoryBeans(), historicalBean);

        // Reset values for next iteration.  (JWH: redundant with code above)
        setLastTimeStatsCaptured(curTime);
    }

    /**
     * Traverse the file history and history bean lists and remove items for the specified file name.
     * @param fileName
     */
    public void resetHistory(String fileName) {

        synchronized (fileHistoryBeans) {
            Iterator iterator = getFileHistoryBeans().iterator();
            while (iterator != null && iterator.hasNext()) {

                RumStatsPerFile stats = (RumStatsPerFile) iterator.next();
                if (stats != null && stats.getFileName().equals(fileName.trim())) {
                    iterator.remove();
                    break;
                }
            }
        }

        synchronized (historyBeans) {
            Iterator beanIterator = getHistoryBeans().iterator();
            while (beanIterator != null && beanIterator.hasNext()) {

                RumStatsBean beanStats = (RumStatsBean) beanIterator.next();
                if (beanStats != null && beanStats.getFileName().equals(fileName.trim())) {
                    beanIterator.remove();
                }
            }
        }
    }

    /**
     * This method adds a ramStatsBean to the array.  
     * If the current count equals the maximum (8640), the oldest record (bottom of the stack) is removed.
     * @param buffer
     * @param item
     */
    private void addHistoryBeanToBuffer(ArrayStack buffer, Object item) {

        if (buffer.size() == MAX_NUMBER_OF_HISTORICAL_BEANS) {
            buffer.remove(0);
        }
        
        buffer.push(item);
    }

    public int getNumberOfThreadsRunning() {

        int defaultThreads = 50;
        try {
            ConfigData configData = ConfigData.getInstance();
            String numberOfConcurrentUpdatesStr = System.getProperty("concurrentUpdateOverride");
            if (numberOfConcurrentUpdatesStr == null) {
                PropertyGroup group = configData.getGroup(CONCURRENT_UPDATES_NAME);
                numberOfConcurrentUpdatesStr = group.getChildGroup(RUM_UPDATES_ENTRY).getPropertyValue(NUMBER_OF_CONCURRENT_UPDATES);
            }

            if (numberOfConcurrentUpdatesStr != null) {
                defaultThreads = Integer.parseInt(numberOfConcurrentUpdatesStr);
            }
        }
        catch (HertzException e) {
            return defaultThreads;
        }

        return defaultThreads;
    }

    /**
     * Gather statistics on timed events.
     */
    public int denyOrAcceptElapsedTimeEvent(EntryExit ee, String loggerName) {

        int response = DENY;
        String methodName = ee.getMethodName();
        int fileNameIndex = methodName.indexOf(FILE_NAME);
        
        if (fileNameIndex >= 0) {
            String fileName = methodName.substring(fileNameIndex + FILE_NAME.length(), methodName.length());
            RumStatsPerFile statsForFile = getBeanFromFileStack(getFileHistoryBeans(), fileName);

            if (methodName.indexOf(RumStats.RUM_DB_UPDATE_METHOD) >= 0) {

                response = ACCEPT;
            }
            else if (methodName.indexOf(RumStats.RUM_FILE_UPDATE_METHOD) >= 0) {

                response = ACCEPT;

                if (ee.getStart() > 0 && ee.getEnd() > 0) {
                    // Both start/stop exist so we are finished.
                    statsForFile.setFileStatus("Finished");
                }
                else {
                    // We have started.
                    statsForFile.setStartTime(System.currentTimeMillis());
                    statsForFile.setFileStatus("Started");
                }
            }
            else if (methodName.indexOf(FILE_BEING_UPDATED_METHOD) >= 0) {
                
                response = ACCEPT;
            }
            else if (methodName.indexOf(RETRIEVE_LOCATION_INFORMATION_METHOD) >= 0) {
                
                response = ACCEPT;

                if (ee.getEnd() > 0 && ee.getStart() > 0) {
                    statsForFile.setFileStatus("Retrieving Location Info Completed");
                }
                else {
                    statsForFile.setFileStatus("Retrieving Location Info....");
                }
            }
            else if (methodName.indexOf(SORTING_FILE_RECORDS_METHOD) >= 0) {
                
                response = ACCEPT;

                if (ee.getEnd() > 0 && ee.getStart() > 0) {
                    statsForFile.setFileStatus("Sorting File Records by Plan/Place Completed");
                }
                else {
                    statsForFile.setFileStatus("Sorting File Records by Plan/Place....");
                }
            }
            else if (methodName.indexOf(PERFORM_DB_UPDATE_METHOD) >= 0) {

                response = ACCEPT;

                if (ee.getEnd() > 0 && ee.getStart() > 0) {
                    statsForFile.setFileStatus("Finished Database Update");
                }
                else {
                    statsForFile.setFileStatus("Performing Database Update....");
                }
            }
            else if (methodName.indexOf(WRITE_OUTPUT_FILE_METHOD) >= 0) {

                response = ACCEPT;

                if (ee.getEnd() > 0 && ee.getStart() > 0) {
                    statsForFile.setFileStatus("Writing Output File Completed");
                }
                else {
                    statsForFile.setFileStatus("Writing Output File....");
                }
            }
            else {
                //System.out.println("RumStats.denyOrAcceptElapsedTimeEvent -");
            }

            if (response == ACCEPT) {
                // If we accept this logging call, record the end time as now. (??)
                statsForFile.setEndTime(System.currentTimeMillis());
            }
            
            // @@JWH - REMOVE THIS FOR PRODUCTION !!! 
            //if (response == ACCEPT) {
                // RATES-13501 Removed following line that appears to cause an OutOfMemory exception.
                //System.out.println("<FB> Stats For File (denyOrAcceptElapsedTimeEvent):\n" + statsForFile.toStringVerbose());
            //}
        }
        else {
            //System.out.println("RumStats.denyOrAcceptElapsedTimeEvent - ");
        }

        return response;
    }

    /**
     * Gather statistics on other timed events.
     */
    public int denyOrAcceptOtherEvent(EntryExit ee, String loggerName) {

        int response = DENY;

        if (ee.getMethodName().indexOf(RECORD_COUNT_METHOD) >= 0) {
            response = ACCEPT;
        }
        else if (ee.getMethodName().indexOf(RECORDS_UPDATED_METHOD) >= 0) {
            response = ACCEPT;
        }
        else if (ee.getMethodName().indexOf(FILE_BEING_UPDATED_METHOD) >= 0) {
            response = ACCEPT;
        }

        return response;
    }

    /**
     * Parse the passed long value.  If there are any errors, return 0.
     * @param longStr
     * @return
     */
    public static long convertStrToLongOrZero(String longStr) {
        
        if (longStr == null) {
            return 0L;
        }
        else if (longStr.trim().length() == 0) {
            return 0L;
        }
        else {
            try {
                long value = Long.parseLong(longStr.trim());
                return value;
            }
            catch (NumberFormatException e) {
                return 0L;
            }
        }
    }

    /**
     * Compute the elapsed time and store in a statistics bean.
     */
    public void addElapsedTime(ElapsedTime et) {

        String loggerName = et.getLoggerName();
        int fileNameIndex = loggerName.indexOf(FILE_NAME);
        
        if (fileNameIndex >= 0) {
            String fileName = loggerName.substring(fileNameIndex + FILE_NAME.length(), loggerName.length());
            
//            synchronized (fileHistoryBeans) {

                RumStatsPerFile statsForFile = getBeanFromFileStack(getFileHistoryBeans(), fileName);

                synchronized (statsForFile) {
                    if (loggerName.indexOf(RUM_DB_UPDATE_METHOD) >= 0) {         
                        // Compute and store DB update elapsed time.    	     
                        statsForFile.addTotalDatabaseUpdateTime(et.getElapsedTime());
                        statsForFile.addDatabaseUpdates(1L);                       // JWH: DB Update calls = Place/Plan Thread count - is NOT CORRECT
                        checkForCapture(false, fileName);
                    }
                    else if (loggerName.indexOf(RECORD_COUNT_METHOD) >= 0) {
                        //  Record Count - parse from logger parameter. 
                        int startOfRecordsNumberIndex = loggerName.indexOf(':');
                        int endOfRecordsNumberIndex = loggerName.indexOf(FILE_NAME.trim());
                        String numberOfRecords = et.getLoggerName().substring(startOfRecordsNumberIndex + 2, endOfRecordsNumberIndex - 1);
                        statsForFile.addTotalRecordsReceived(convertStrToLongOrZero(numberOfRecords.trim()));
                        checkForCapture(false, fileName);
                    }
                    else if (loggerName.indexOf(RECORDS_UPDATED_METHOD) >= 0) {
                        // Records updated count - parse from logger parameter.
                        int startOfRecordsNumberIndex = loggerName.indexOf(':');
                        int endOfRecordsNumberIndex = loggerName.indexOf(FILE_NAME.trim());
                        String numberOfRecords = et.getLoggerName().substring(startOfRecordsNumberIndex + 2, endOfRecordsNumberIndex);
                        statsForFile.addRecordsUpdated(convertStrToLongOrZero(numberOfRecords.trim()));       // JWH: Total Records Updated - is CORRECT
                        checkForCapture(false, fileName);
                    }
                    else if (loggerName.indexOf(RUM_FILE_UPDATE_METHOD) >= 0) {
                        // Compute and store ETE elapsed time for all records in the file.
                        statsForFile.addFileUpdates(1L);
                        statsForFile.addTotalFileTime(et.getElapsedTime());
                        checkForCapture(true, fileName);
                    }
                    else if (loggerName.indexOf(FILE_BEING_UPDATED_METHOD) >= 0) {
                        // Current File
                        //	    	   int endOfMethodNameIndex = et.getLoggerName().indexOf(':');
                        //	    	   currentFileName = et.getLoggerName().substring(endOfMethodNameIndex + 1);    	    
                    }
                    else if (loggerName.indexOf(RETRIEVE_LOCATION_INFORMATION_METHOD) >= 0) {
                        // Find Place Code elapsed time.
                        statsForFile.addRetrieveLocationInfoTime(et.getElapsedTime().longValue());
                        checkForCapture(true, fileName);
                    }
                    else if (loggerName.indexOf(SORTING_FILE_RECORDS_METHOD) >= 0) {
                        // Sort file records elapsed time.
                        statsForFile.addFileSortingTime(et.getElapsedTime().longValue());
                        checkForCapture(true, fileName);
                    }
                    else if (et.getLoggerName().indexOf(WRITE_OUTPUT_FILE_METHOD) >= 0) {
                        // Write file output elapsed time.
                        statsForFile.addWritingOutputFileTime(et.getElapsedTime().longValue());
                        checkForCapture(true, fileName);
                    }
                }

                // @@JWH - REMOVE THIS FOR PRODUCTION !!! 
                //if (response == ACCEPT) {
                System.out.println("<FB> Stats For File (addElapsedTime):\n" + statsForFile.toStringVerbose());
                //}
//            }
        }
    }

    /**
     * This method returns the number of beans asked for or the max number in the list if more
     * are asked for than are available.  It returns them from the youngest (most recently added)
     * to the oldest.  Uses LIFO principle.
     * 
     * @param maxNumberToReturn
     * @param historyStartTime
     * @param historyEndTime
     * @return
     */
    public RumStatsBean[] getRumHistoricalBeans(int maxNumberToReturn, long historyStartTime, long historyEndTime) {
        
        // JWH: This code has no effect and is commented out.  I don't know if we really *did* want to limit returned items.
        //        int numOfAvailableBeans = getMaxNumberOfBeans();
        //        int beansToReturn = 0;
        //        if (maxNumberToReturn > 0 && (maxNumberToReturn <= numOfAvailableBeans)) {
        //            // A limit exists.
        //            beansToReturn = maxNumberToReturn;
        //        }
        //        else {
        //            beansToReturn = numOfAvailableBeans;
        //        }

        // Create return array one bigger (why?) than the size of the statistics array.
        RumStatsBean[] rumStatsBeans = new RumStatsBean[getFileHistoryBeans().size() + 1];

        int beanCount = 0;

        Iterator iter = getFileHistoryBeans().iterator();
        while (iter.hasNext() && iter.hasNext()) {
            RumStatsPerFile statsPerFile = (RumStatsPerFile) iter.next();
            String fileName = statsPerFile.getFileName();
            // Get statistics for this file name.
            RumStatsBean historicalBean = getRumStatsBeanForFileName(fileName);

            rumStatsBeans[beanCount] = historicalBean;

            beanCount++;
        }

        return rumStatsBeans;
    }

    /**
     * Find the RumStatsPerFile for the specified file in passed stack.  
     * If one doesn't exist, create and add it to the list.
     * @param fileHistoryBeans
     * @param fileName
     * @return
     */
    public RumStatsPerFile getBeanFromFileStack(ArrayStack fileHistoryBeans, String fileName) {

        Iterator iter = fileHistoryBeans.iterator();
        while (iter != null && iter.hasNext()) {
            RumStatsPerFile fileStats = (RumStatsPerFile) iter.next();
            if (fileStats != null && fileStats.getFileName().equals(fileName.trim())) {
                return fileStats;
            }
        }

        // Statistics bean not found so create one and add it to the stack.
        RumStatsPerFile newRumStatsPerFile = new RumStatsPerFile(fileName);
        fileHistoryBeans.add(newRumStatsPerFile);
        return newRumStatsPerFile;
    }

    /**
     * Create and return a RumStatsBean for the file name populated with statistics.
     * @param fileName
     * @return
     */
    private synchronized RumStatsBean getRumStatsBeanForFileName(String fileName) {

        // Find the file statistics for this file in the stack.
        RumStatsPerFile fileStats = this.getBeanFromFileStack(fileHistoryBeans, fileName);
        
        // Create a new statistics bean from the RumStatsPerFile object.
        RumStatsBean bean = new RumStatsBean();

        bean.setFilesUpdated(fileStats.getFileUpdates());
        bean.setRecordsUpdated(fileStats.getRecordsUpdated());
        bean.setNumberOfDBUpdates(fileStats.getDatabaseUpdates());
        
        bean.setRumUpdatesUnderOneMinute(fileStats.getUpdatesUnder1Min());
        bean.setRumUpdatesUnderFiveMinutes(fileStats.getUpdatesUnder5Min());
        bean.setRumUpdatesUnderTenMintues(fileStats.getUpdatesUnder10Min());
        bean.setRumUpdatesUnderFifteenMintues(fileStats.getUpdatesUnder15Min());
        bean.setRumUpdatesUnderThirtyMintues(fileStats.getUpdatesUnder30Min());
        bean.setRumUpdatesUnderOneHour(fileStats.getUpdatesUnder60Min());
        bean.setRumUpdatesUnderTwoHours(fileStats.getUpdatesUnder120Min());
        bean.setRumUpdatesUnderFiveHours(fileStats.getUpdatesUnder300Min());
        
        bean.setTotalRecordsReceived(fileStats.getTotalRecordsReceived());
        bean.setNumberOfThreads(this.getNumberOfThreadsRunning());
        
        bean.setTotalDatabaseElapsedTime(fileStats.getTotalDatabaseUpdateTime());
        bean.setTotalFileElapsedTime(fileStats.getTotalFileTime());
        bean.setRetrieveLocationInfoTime(fileStats.getRetrieveLocationInfoTime());
        bean.setWriteOutputFileTime(fileStats.getWritingOutputFileTime());
        bean.setSortFileRecordsTime(fileStats.getFileSortingTime());
        
        bean.setFileName(fileStats.getFileName());
        bean.setFileStatus(fileStats.getFileStatus());
        
        bean.setCapturedEndTime(fileStats.getEndTime());
        bean.setCapturedStartTime(fileStats.getStartTime());

        return bean;
    }

    public long getLastTimeStatsCaptured() {

        return lastTimeStatsCaptured;
    }

    public void setLastTimeStatsCaptured(long lastTimeStatsCaptured) {

        this.lastTimeStatsCaptured = lastTimeStatsCaptured;
    }

    public int getMaxNumberOfBeans() {

        return getHistoryBeans().size();
    }

    @Override
    public void addTimeEventBufferItem(ArrayStack buffer, Object item) {

        // TODO Auto-generated method stub
    }
    
    public ArrayStack getHistoryBeans() {
        
        return historyBeans;
    }
    
    public void setHistoryBeans(ArrayStack historyBeans) {
    
        this.historyBeans = historyBeans;
    }
    
    public ArrayStack getFileHistoryBeans() {
        
        return fileHistoryBeans;
    }

    
    public void setFileHistoryBeans(ArrayStack fileHistoryBeans) {
    
        this.fileHistoryBeans = fileHistoryBeans;
    }
}

/*
 *************************************************************
 * Change History:
 *
 * $Log$
 * Revision 1.29  2017/09/14 19:10:25  dtp4395
 * RATES-12061; Heflin; Fixed Web statistics collection.
 *
 * Revision 1.28  2017/02/21 17:06:29  dtp4395
 * RATES-11849; Heflin; Commented unused variable in getRumHistoricalBeans().  Formatted line.
 *
 * Revision 1.27  2017/02/07 21:04:01  dtp4395
 * RATES-11849; Heflin; Fixed statistics collection.  Renamed method.
 *
 * Revision 1.26  2017/02/02 22:00:27  dtp4395
 * RATES-11849; Heflin; Added convertStrToLongOrZero() and used to for safe Long conversion.
 *
 * Revision 1.25  2017/01/18 22:29:09  dtp4395
 * RATES-11849; Heflin; Fixed string parsing in denyOrAcceptElapsedTimeEvent() and addElapsedTime().  Trimmed strings before parsing with Long.parseLong().
 *
 * Revision 1.24  2017/01/16 22:24:27  dtp4395
 * RATES-11849; Heflin; Refactored methodName constants.
 *
 * Revision 1.23  2017/01/09 17:58:01  dtp4395
 * RATES-11849; Heflin; Added commented debug lines, added comments and formatted.
 *
 * Revision 1.22  2016/11/08 18:55:21  dtp4395
 * RATES-11849; Heflin; Comment changes and formatting. No code changes.
 *
 * Revision 1.21  2016/10/27 14:49:53  dtp4395
 * RATES-11849; Heflin; Added comments, added CVS history tag, added copyright.  Formatting only.
 *
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
