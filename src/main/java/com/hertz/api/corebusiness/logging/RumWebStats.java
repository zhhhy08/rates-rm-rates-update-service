package com.hertz.api.corebusiness.logging;

import java.util.Iterator;

import org.apache.commons.collections.ArrayStack;

import com.hertz.rates.common.utils.logging.ElapsedTime;
import com.hertz.rates.common.utils.logging.EntryExit;
import com.hertz.rates.common.utils.logging.HertzLogger;
import com.hertz.rates.common.utils.logging.LoggingStats;

/**
 * Provides RUM Webservice call Statistics.
 * 
 *
 */
public class RumWebStats implements LoggingStats {

    //
    // Define constants used in logger.entry/exit and denyOrAcceptElapsedTimeEvent, addElapsedTime methods.
    //
    public static final String TRANS_ID = " TransId ";
    
    // Logging Events that we process:
    public static final String DO_WEBSERVICE_UPDATE = "doWebServiceUpdate ";
    public static final String RETRIEVE_LOCATION_INFORMATION_METHOD = "findPlaceTypeIdCode ";
    public static final String NUMBER_OF_UPDATES_IN_PLAN_PLACE = "Number Of Updates in Plan/Place : ";
    private static final String EXECUTE_STORED_PROCEDURE = "executeStoredProcedure";

    
    // Logging Events we ignore at this time:
    public static final String PERFORM_DB_UPDATE_METHOD = "performDBUpdate ";
    private static final String LOADING_CONFIG_DATA = "Loading_Config_Data";
    private static final String SORTING = "sortingWebserviceRecords";
    private static final String LOADING_AREA_NUMBER_CACHE = "Loading_Area_Number_Cache";

   
    private static final int MAX_NUMBER_OF_BEANS = 5000;
    
    private final static HertzLogger logger = new HertzLogger(RumWebStats.class);

    /** A singleton of this class. */ 
    private volatile static RumWebStats rumWebStatsSingleton;

    // Note: Our version of ArrayStack is not generic in the Apache library we are using so don't try to do that.
    /** Stack of History Beans. */
    private ArrayStack historyBeans;

    /**
     * Constructor
     */
    public RumWebStats() {

        // Create a stack of history beans.
        historyBeans = new ArrayStack(MAX_NUMBER_OF_BEANS);
    }

    /**
     * Return the singleton for this class.
     * @return
     */
    public synchronized static RumWebStats getRumWebStatsObject() {
    	
        if (rumWebStatsSingleton == null) {
        	
            synchronized (RumWebStats.class) {
                if (rumWebStatsSingleton == null) {
                    rumWebStatsSingleton = new RumWebStats();
                }
            }
        }

        return rumWebStatsSingleton;
    }

    @Override
    
    /**
     * Determine if the specified logger event is to be processed or ignored.
     * Since this is called within a logger event, we should use System.out.println to do any logging instead of a logger call which might be disruptive.
     */
    public int denyOrAcceptElapsedTimeEvent(EntryExit ee, String loggerName) {

        int response = org.apache.log4j.spi.Filter.DENY;
        String methodName = ee.getMethodName();
                
        //int transIdIndex = methodName.indexOf(TRANS_ID);

        //safeLog("denyOrAcceptElapsedTimeEvent :<+>: >" + methodName + "<");

        // Log information
        //StringBuffer s = new StringBuffer();
        //s.append("RumWebStats.denyOrAcceptElapsedTimeEvent : methodName >" + methodName + "<");
        //s.append(" transIdIndex >" + transIdIndex + "<");

        if (methodName.indexOf(DO_WEBSERVICE_UPDATE) >= 0) {
        	
            // Transaction Time
            response = org.apache.log4j.spi.Filter.ACCEPT;

            int transactionIdIndex = methodName.indexOf(TRANS_ID);
            String transactionId = methodName.substring(transactionIdIndex + TRANS_ID.length());

            //s.append(" transactionIdIndex: >" + transactionIdIndex + "<");
            //s.append(" transactionId: >" + transactionId + "<");
            
            synchronized (historyBeans) {
                if (ee.getEnd() <= 0) {
                    // entry
                    //s.append(" addTimeEventBufferItem: >" + ee.getEnd() + "<");
                    RumWebStatsBean newBean = new RumWebStatsBean(transactionId, ee.getStart());
                    this.addTimeEventBufferItem(this.historyBeans, newBean);
                }
                else {
                    // exit
                    //s.append(" setTransactionEndTime: >" + ee.getEnd() + "<");
                    RumWebStatsBean beanFromStack = getBeanFromStack(transactionId);
                    beanFromStack.setTransactionEndTime(ee.getEnd());
                }
            }
            // No more work to do.
        }
        else if (methodName.indexOf(RETRIEVE_LOCATION_INFORMATION_METHOD) >= 0) {
            // Find Location
            response = org.apache.log4j.spi.Filter.ACCEPT;
        }
        else if (methodName.indexOf(NUMBER_OF_UPDATES_IN_PLAN_PLACE) >= 0) {
            // Number of Updates
            response = org.apache.log4j.spi.Filter.ACCEPT;
        }
        else if (methodName.indexOf(EXECUTE_STORED_PROCEDURE) >= 0) {
            // Stored Procedure
            response = org.apache.log4j.spi.Filter.ACCEPT;
            
            // Since we need access to the begin/end times we do this processing here
            // instead of the addToStoredProcedureTime() method.
                        
            int transactionIdIndex = methodName.indexOf(TRANS_ID);
            if (transactionIdIndex >= 0) {
                // This is a Web Service Transaction.
                String transactionId = methodName.substring(transactionIdIndex + TRANS_ID.length());

                synchronized (historyBeans) {
                    RumWebStatsBean beanFromStack = getBeanFromStack(transactionId);
                    if (beanFromStack != null) {
                        // Found the statistics bean.
                        if (ee.getStart() > 0 && ee.getEnd() > 0) {
                            // This is an Exit call since both times are available
                            beanFromStack.addToStoredProcedureTime(ee.getStart(), ee.getEnd());
                        }
                        else {
                            // An Entry call - do nothing
                        }
                    }
                    else {
                        safeLog("Did not find statistics for this Transaction ID: " + transactionId + " for: " + loggerName);
                    }
                }
            }
        }
        
        //
        // IGNORE THESE EVENTS
        //
        
        else if (methodName.indexOf(LOADING_CONFIG_DATA) >= 0) {
            // Ignore
            response = org.apache.log4j.spi.Filter.DENY;
        }
        else if (methodName.indexOf(SORTING) >= 0) {
            // Ignore
            response = org.apache.log4j.spi.Filter.DENY;
        }
        else if (methodName.indexOf(LOADING_AREA_NUMBER_CACHE) >= 0) {
            // Ignore
            response = org.apache.log4j.spi.Filter.DENY;
        }
        else if (methodName.indexOf(PERFORM_DB_UPDATE_METHOD) >= 0) {
            // Ignore
            response = org.apache.log4j.spi.Filter.DENY;
        }
        
        else {
            //s.append(" methodName did not match");
        }

        if (response == org.apache.log4j.spi.Filter.ACCEPT) {
            //s.append(" --ACCEPT--");
        }
        else {
            //s.append(" --DENY--");
        }
        
        //safeLog(s.toString());

        return response;
    }
    
    /**
     * Gather statistics on other timed events.
     */
    @Override
    public int denyOrAcceptOtherEvent(EntryExit ee, String loggerName) {

        int response = org.apache.log4j.spi.Filter.DENY;
        return response;
    }

    /**
     * This just prints to System.out.println
     * Inside the logger methods, call this instead of a recursive logger call so to avoid problems.
     * @param s
     */
    private void safeLog(String s) {

        System.out.println(s);
    }
    
    public RumWebStatsBean getBeanFromStack(String transId) {

        synchronized (historyBeans) {
            Iterator<RumWebStatsBean> iter = historyBeans.iterator();
            while (iter.hasNext()) {
                RumWebStatsBean bean = iter.next();
                if (transId.equals(bean.getTransactionId())) {
                    return bean;
                }
            }
        }
        return null;
    }

    /**
     * Process designated logger events.
     * Since this is called within a logger event, we should use System.out.println to do any logging instead of a logger call which might be disruptive.
     */
    @Override
    public void addElapsedTime(ElapsedTime et) {

        String loggerName = et.getLoggerName();

        //        // @@JWH
        //        if (loggerName.indexOf("executeStoredProcedure") >= 0) {
        //            String s;
        //            s = "addElapsedTime";
        //        }

        //safeLog("addElapsedTime :<+>: >" + loggerName + "<");

        // Log information
        //StringBuffer s = new StringBuffer();
        //s.append("WWW "); // tag for grep
        //s.append("RumWebStats.addElapsedTime - loggerName: >" + loggerName + "<");
        //s.append(" transIdIndex: >" + transIdIndex + "<");

        int transactionIdIndex = loggerName.indexOf(TRANS_ID);
        //s.append(" transactionIdIndex: >" + transactionIdIndex + "<");

        boolean isTransaction = (transactionIdIndex >= 0);
        if (isTransaction) {
            String transactionId = loggerName.substring(transactionIdIndex + TRANS_ID.length());
            //s.append(" transactionId: >" + transactionId + "<");

            Long elapsedTime = et.getElapsedTime();
            RumWebStatsBean bean = getBeanFromStack(transactionId);

            if (bean != null) {

                synchronized (bean) {
                    if (loggerName.indexOf(RETRIEVE_LOCATION_INFORMATION_METHOD) >= 0) {

                        // Retrieve Location
                        //s.append(" setRetrieveLocationTime: >" + elapsedTime + "<" + "--MATCH--");
                        bean.setRetrieveLocationTime(elapsedTime.longValue());
                    }
                    else if (loggerName.indexOf(EXECUTE_STORED_PROCEDURE) >= 0) {

                        // Execute Stored Procedure
                        // Since we want access to the start and end times, we have to do this computation
                        // in the denyOrAcceptElapsedTimeEvent() method above.
                        // Nothing else happens here...                       
                    }
                    else if (loggerName.indexOf(NUMBER_OF_UPDATES_IN_PLAN_PLACE) >= 0) {

                        // Number of Updates in Place/Plan
                        int indexOfColon = loggerName.indexOf(":");
                        //s.append(" indexOfColon: >" + indexOfColon + "<");

                        String numberOfUpdatesStr = loggerName.substring(indexOfColon + 1, transactionIdIndex).trim();

                        //s.append(" setNumberOfUpdates: >" + numberOfUpdates + "<" + "--MATCH--");
                        Long numberOfUpdate = RumStats.convertStrToLongOrZero(numberOfUpdatesStr);
                        bean.addToNumberOfUpdates(numberOfUpdate);
                    }
                    else {
                        //s.append(" loggerName did not match" + " --SKIP--");
                    }
                }
            }
            else {
                //s.append(" bean from stack == null" + " --SKIP--");
            }
        }
        else {
            //s.append(" transactionIdIndex < 0" + " --SKIP--");
        }

        //safeLog(s.toString());
    }

    @Override
    public void addTimeEventBufferItem(ArrayStack buffer, Object item) {

        if (buffer.size() == MAX_NUMBER_OF_BEANS) {
            buffer.remove(0);
        }
        buffer.push(item);
    }

    /**
     * Return statistics information to the Monitoring Function.
     * @param maxNumberToReturn
     * @param startTime
     * @param endTime - (ignored)
     * @return
     */
    public synchronized RumWebStatsBean[] getCurrentStats(int maxNumberToReturn, long startTime, long endTime) {

        synchronized (historyBeans) {
            int numberOfBeans = historyBeans.size();

            if (maxNumberToReturn > numberOfBeans || maxNumberToReturn == 0) {
                maxNumberToReturn = numberOfBeans;
            }

            RumWebStatsBean[] beans = new RumWebStatsBean[maxNumberToReturn];
            Iterator<RumWebStatsBean> stackIter = historyBeans.iterator();
            int count = 0;
            
            while (stackIter.hasNext() && count < maxNumberToReturn) {
                RumWebStatsBean bean = stackIter.next();
                if (bean != null && bean.getTransactionStartTime() >= startTime && bean.getTransactionEndTime() > 0L) {

                    beans[count] = bean;
                    count++;
                }
            }

            return beans;
        }
    }

    /**
     * Describe this object
     * @return
     */
    public String toStringVerbose() {
        
        return toStringVerbose("");
    }
    
    /**
     * Describe this object
     * @return
     */
    public String toStringVerbose(String indent) {
        
        synchronized (historyBeans) {

            StringBuffer s = new StringBuffer();
            int i = 0;
            
            s.append(indent + "RumWebStats:" + "\n");
            
            int numberOfBeans = historyBeans.size();
            s.append(indent + "  numberOfBeans: " + numberOfBeans + "\n");
            
            Iterator<RumWebStatsBean> stackIter = historyBeans.iterator();
            while (stackIter.hasNext()) {
                RumWebStatsBean bean = stackIter.next();
                
                s.append(indent + "  [" + i + "]\n");
                s.append(indent + "  " + bean.toStringVerbose() + "\n");
                
                i++;
            }
            return s.toString();
        }
    }
}

/*
 *************************************************************
 * Change History:
 *
 * $Log$
 * Revision 1.11  2017/03/08 07:11:45  dtp4395
 * RATES-11849; Heflin; Fixed denyOrAcceptElapsedTimeEvent() to add guard for missing Transaction ID in text.
 *
 * Revision 1.10  2017/02/07 21:08:02  dtp4395
 * RATES-11849; Heflin; Refactored statistics collection.  Changed algorithm for measuring DB call time.
 *
 * Revision 1.9  2017/01/16 22:17:51  dtp4395
 * RATES-11849; Heflin; Corrected to handle threads.  Refactored methodName constants. Commented all debug output
 *
 * Revision 1.8  2017/01/11 23:00:45  dtp4395
 * RATES-11849; Fixed getRumWebStatsObject() synchronization.
 *
 * Revision 1.7  2017/01/10 21:30:35  dtp4395
 * RATES-11849; TESTING - Display all 'method names' in filters.
 *
 * Revision 1.6  2017/01/10 19:41:46  dtp4395
 * RATES-11849; Added synchronized statements.  Commented.
 *
 * Revision 1.5  2017/01/09 18:00:32  dtp4395
 * RATES-11849; Heflin; Added XXXXXX constant as place-holder for later fix.   Changed getRumWebStatsObject() to use RumWebStats.class as lock.
 *
 * Revision 1.4  2017/01/06 00:40:42  dtp4395
 * RATES-11849; Heflin; Added logging to denyOrAcceptElapsedTimeEvent and addElapsedTime methods.
 *
 * Revision 1.3  2016/11/10 16:22:56  dtp4395
 * RATES-11849; Heflin; Comment changes and formatting. No code changes.
 *
 * Revision 1.2  2014/12/08 16:13:43  dtc1090
 * java.util.ConcurrentModificationException were being thrown in production, added code to prevent this from happening
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
