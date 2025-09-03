package com.hertz.api.corebusiness;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import com.hertz.rates.common.utils.HertzException;
import com.hertz.rates.common.utils.config.ConfigData;
import com.hertz.rates.common.utils.config.PropertyGroup;
import com.hertz.rates.common.utils.logging.HertzLogger;
import com.hertz.rates.common.utils.logging.LogLevel;
import com.hertz.api.drivers.UpdateDriver;

/**
 * Implement using Java 6 technology as this is what is in the WAS Production environment.
 * 
 * This Singleton class manages active threads which are used by the Webservice calls.
 * It imposes a maximum thread count across all Webservices calls.
 * All the sub-threads for a particular WS thread will be allocated *before* threads
 *   will be allocated to another WS thread.  This imposes FIFO order on thread allocation
 * and prevents blocking or starvation.
 * 
 * WS #1 Call -> requests sub-thread 1 of 4   WS #1 is added to FIFO - thread count increments, sub-thread start allowed
 * WS #1 Call -> requests sub-thread 2 of 4                          - thread count increments, sub-thread start allowed
 * WS #1 Call -> requests sub-thread 3 of 4                          - thread count increments, sub-thread start allowed
 * WS #2 Call -> requests sub-thread 1 of 3   WS #2 is added to FIFO - waits
 * WS #1 Call -> requests sub-thread 4 of 4                          - thread count increments, last sub-thread start allowed, WS #1 removed from FIFO
 * WS #2 Call ->                                                     - thread count increments, sub-thread start is now allowed     
 * 
 * A thread completes                                                - thread count decrements, 
 *                                                                   - any waiting WS thread gets a sub-thread and count is incremented 
 * 
 * 
 * @author jwheflin
 *
 */
public class WebServiceThreadManager {

    final static HertzLogger logger = new HertzLogger(WebServiceThreadManager.class);

    /** Singleton */
    private static final WebServiceThreadManager webThreadManagerSingleton = new WebServiceThreadManager();

    /** Maximum Threads that can be concurrently running across all Webservice calls. */
    private static int MAX_THREADS = 300;

    // Smaller limit for testing
    //private static int MAX_THREADS = 10;

    /** If true then queue accesses for threads blocked on insertion or removal, are processed in FIFO order; if false the access order is unspecified. */
    private static boolean IS_FAIR = true;

    /** Holds running threads for all Webservice call with FIFO fairness. */
    private static ArrayBlockingQueue<UpdateThread> threadPool = null;

    /** Maximum Threads. */
    private static int maximumThreads = -1;  // Uninitialized

    /** This is the logging level of Thread Pool messages. */
    public final static LogLevel TIMING_LOG_LEVEL = LogLevel.INFO;

    /** This thread monitors the Thread Pool for problems. */
    private static Thread monitorThread = null;
    
    /** This collection keeps track of database DML count of all rate updates. */
    public static Map<String,Integer> txnDMLCountMap = new ConcurrentHashMap<String,Integer>();

    /**
     * Constructor
     */
    private WebServiceThreadManager() {

        // Do nothing.
    }

    /**
     * Get the Singleton object. Thread-safe because singleton was created at class load time.
     * @return
     */
    public static WebServiceThreadManager getWebThreadManager() {

        if (WebServiceThreadManager.monitorThread == null) {

            synchronized (WebServiceThreadManager.class) {

                if (WebServiceThreadManager.monitorThread == null) {
                    // Initialize the monitor pool thread.
                    WebServiceThreadManager.monitorThread = new WebServiceThreadManagerMonitor();
                    WebServiceThreadManager.monitorThread.start();
                }
            }
        }

        return webThreadManagerSingleton;
    }

    /**
     * Add all threads in the list to the thread pool.  If full, this will block until there is room.
     * Once a thread has been queued, it can be started.
     * Thread-safe - only one call at a time can be made.
     * We *WANT* this call to be single-filed so that all threads for a particular WS call are queued
     * before another WS call can do any queuing.
     * 
     * @param transactionID
     * @param updateThreadList
     */
    public synchronized void queueNewWebserviceCallThreads(String transactionID, ArrayList<UpdateThread> updateThreadList) {

        final String methodName = "<WS> TransID: " + transactionID + " WebServiceThreadManager queuing threadlist";
        try {
            logger.entry(HertzLogger.INFO, methodName);

            // Initialize DML count for this transaction
            txnDMLCountMap.put(transactionID, 0);

            // Queue and Start each thread
            Iterator<UpdateThread> threadListIter = updateThreadList.iterator();
            while (threadListIter.hasNext()) {

                UpdateThread updateThreadToStart = threadListIter.next();
                boolean wasAdded = addThreadToPool(transactionID, updateThreadToStart);
                if (!wasAdded) {
                    logger.error("<WS> TransID: " + transactionID + " FAILED to add thread" + "\n" + updateThreadToStart.toStringVerbose() + "\n");
                }
            }
        }
        finally {
            logger.exit(HertzLogger.INFO, methodName);
        }
    }

    /**
     * Call ONLY from synchronized code.
     * Adds a thread and blocks if there is no room.
     * @param transactionID
     * @param updateThreadToStart
     * @return
     */
    private static boolean addThreadToPool(String transactionID, UpdateThread updateThreadToStart) {

        boolean wasAdded = false;

        if (updateThreadToStart != null) {
            long index = updateThreadToStart.getIndex();

            try {

                ArrayBlockingQueue<UpdateThread> threadPool = getThreadPool();
                if (threadPool != null) {
                    int remainingCapacity = getThreadPool().remainingCapacity();

                    logger.info("<WS> Before adding TransID: " + transactionID + " index: " + index + " - capacity: " + remainingCapacity + " max: " + getMaxThreads());
                    //logger.debug("<WS> Before adding TransID: " + transactionID + " thread: " + count + " - capacity: " + remainingCapacity);
                    
                    // Warn if we are running low on threads.
                    if (remainingCapacity < (maximumThreads / 10) ) {
                        logger.warn("<WS> Thread Capacity is below 10%: " + remainingCapacity);
                    }
                    
                    boolean willLikelyBlock = (remainingCapacity == 0);
                    final String methodText;

                    if (willLikelyBlock) {
                        methodText = "<WS> Likely blocking while adding TransID: " + transactionID + " index: " + index + " to Thread Pool with capacity: " + remainingCapacity;
                        logger.before(TIMING_LOG_LEVEL, methodText);
                    }
                    else {
                        methodText = ""; // Not Used: Must be initialized since it is 'final' to avoid compiler warning.
                    }

                    // Record when this thread was put into the Thread Pool.
                    long now = System.currentTimeMillis();
                    updateThreadToStart.setTimePutIntoThreadPool(now);

                    // Put this thread into the Thread Pool - possibly waiting until there is room.
                    threadPool.put(updateThreadToStart);

                    if (willLikelyBlock) {
                        logger.after(TIMING_LOG_LEVEL, methodText);
                    }

                    logger.info("<WS> After adding TransID: " + transactionID + " index; " + index + " - capacity: " + getThreadPool().remainingCapacity());
                    //logger.debug("<WS> After  adding TransID: " + transactionID + " count; " + count + " - capacity: " + getThreadPool().remainingCapacity());

                    // Start thread processing.
                    UpdateDriver.displayMemory("<WS>");
                    logger.info("<WS> TransID: " + transactionID + " Starting Thread: index: " + index);
                    updateThreadToStart.start();
                    wasAdded = true;
                }
                else {
                    // Thread Pool should *NEVER* be null.
                    logger.error("<WS> Skipping null Thread Pool: TransID: " + transactionID + " index: " + index);
                }
            }
            catch (InterruptedException e) {
                // Something went wrong.  Log and try to remove the thread.
                e.printStackTrace();
                // Remove the item
                boolean wasRemoved = getThreadPool().remove(updateThreadToStart);
                logger.error("<WS> Attempt to remove INTERRUPTED WS Thread for TransID: " + transactionID + " index: " + index + " was " + (wasRemoved ? "successful" : "failed"));
            }
        }
        else {
            // Why is the thread null ?
            logger.error("<WS> Skipping null thread: TransID: " + transactionID);
        }

        return wasAdded;
    }

    /**
     * Remove a completed thread.  This may unblock a put().
     * @param updateThread
     */
    public void removeCompletedThread(UpdateThread updateThread) {

        final String transactionID = updateThread.getTransactionId();
        final long index = updateThread.getIndex();
        final String methodName = "<WS> TransID: " + transactionID + " WebServiceThreadManager removing completed thread index: " + index;

        try {
            logger.entry(TIMING_LOG_LEVEL, methodName);

            int capacityBefore = getThreadPool().remainingCapacity();
            //logger.debug("<WS> Before removing completed thread for Transaction: " + transactionID + " thread: " + count + " - capacity: " + capacityBefore);

            boolean wasRemoved = getThreadPool().remove(updateThread);
            if (!wasRemoved) {
                logger.warn("<WS> Completed thread was not removed from Thread Pool!");
            }

            int capacityAfter = getThreadPool().remainingCapacity();
            
            if (capacityAfter == getMaxThreads()) {
                // The pool is full again.
                logger.info("<WS> TransID: " + transactionID +" Removing thread index: " + index + " which returned all threads to pool");
            }
            
            logger.debug("<WS> TransID: " + transactionID + " After removing completed thread thread index: " + index + " - capacity before: " + capacityBefore
                    + " capacity after: " + capacityAfter + " result: " + (wasRemoved ? "successful" : "failed"));
        }
        finally {
            logger.exit(TIMING_LOG_LEVEL, methodName);
        }
    }

    /**
     * Call ONLY from synchronized code.
     * Return a value to be used for Max Threads.
     * If there is an override from Config data, use it.
     * This might include an override from a XML config file.
     * @return
     */
    private static int getMaxThreads() {

        if (maximumThreads > 0) {
            // Value has been initialized.
            return maximumThreads;
        }
        else {
            // Value not initialized so do so.
            final String GROUP = "WebServicesControl";
            final String PROPERTY = "WebServicesMaxThreadCount";
            int maxThreads = MAX_THREADS;

            try {
                ConfigData configData = ConfigData.getInstance();
                if (configData == null) {
                    // Could throw an exception here.  Or just use the default value.
                    logger.debug("<WS> getMaxThreads - invalid ConfigData");
                }
                else {
                    PropertyGroup propertyGroup = configData.getGroup(GROUP);
                    if (propertyGroup == null) {
                        logger.debug("<WS> getMaxThreads - propertyGroup is null");
                    }
                    else {
                        String maxThreadsStr = propertyGroup.getPropertyValue(PROPERTY);
                        if (maxThreadsStr == null) {
                            // Could throw an exception here.  Or just use the default value.
                            logger.debug("<WS> getMaxThreads - property value is null");
                        }
                        else {
                            // We found the config data.
                            try {
                                int value = Integer.parseInt(maxThreadsStr.trim());
                                maxThreads = value;

                                // When initializing, report the maximum number of threads allowed.
                                logger.info("<WS> getMaxThreads - maximum threads from Config: " + maxThreads);
                            }
                            catch (NumberFormatException e) {
                                // Could throw an exception here.  Or just use the default value.
                                logger.error("<WS> getMaxThreads - invalid value for maxThreads: >" + maxThreadsStr + "<");
                            }
                        }
                    }
                }
            }
            catch (HertzException e) {
                // Do nothing, just return the default value.
                logger.error("<WS> getMaxThreads - ConfigData issue");
            }

            maximumThreads = maxThreads;

            logger.debug("<WS> Returning maximumThreads: " + maximumThreads);

            return maximumThreads;
        }
    }

    /**
     * Return an array of all UpdateThreads currently in the Thread Pool.
     * Documentation says:
     * Returns an array containing all of the elements in this queue, in proper sequence. 
     * The returned array will be "safe" in that no references to it are maintained by this queue. 
     * (In other words, this method must allocate a new array). The caller is thus free to modify the returned array. 
     * @return
     */
    public static Object[] getThreadPoolAsArray() {

        if (threadPool == null) {
            return null;
        }
        else {
            return threadPool.toArray();
        }
    }

    /**
     * Remove the specified thread from the Thread Pool.  
     * It has been there so long we think it actually died and didn't remove itself.
     * @param deadUpdateThread
     * @return
     */
    public static boolean removeDeadThread(UpdateThread deadUpdateThread) {

        logger.error("<WSTMM> WebServiceThreadManager - removeDeadThread: " + "\n" + deadUpdateThread.toStringVerbose());

        boolean wasRemoved = getThreadPool().remove(deadUpdateThread);

        return wasRemoved;
    }

    /**
     * Return the Thread Pool.
     * @return
     */
    private static ArrayBlockingQueue<UpdateThread> getThreadPool() {

        if (threadPool == null) {
            logger.info("Initializing Thread Pool for: " + getMaxThreads());
            
            synchronized (WebServiceThreadManager.class) {
                if (threadPool == null) {
                    int maxThreads = getMaxThreads();
                    threadPool = new ArrayBlockingQueue<UpdateThread>(maxThreads, IS_FAIR);
                }
            }
        }

        return threadPool;
    }

    /**
     * Describe this object.
     * @return
     */
    public synchronized String toStringVerbose() {

        return toStringVerbose("");
    }

    /**
     * Describe this object.
     * @return
     */
    public synchronized String toStringVerbose(String indent) {

        StringBuffer s = new StringBuffer();

        s.append(indent + "WebServiceThreadManager" + "\n");

        Iterator<UpdateThread> i = getThreadPool().iterator();

        while (i.hasNext()) {
            UpdateThread updateThread = i.next();
            s.append(updateThread.toStringVerbose("  ") + "\n");
        }

        s.append("\n");
        s.append(indent + "<<<<" + "\n");

        return s.toString();
    }
}

/*************************************************************
 * Change History:
 *
 * $Log$
 * Revision 1.9  2017/11/16 23:31:23  dtp4395
 * RATES-12329; Heflin; Improved logging and thread identification.
 *
 * Revision 1.8  2017/11/16 22:47:19  dtp4395
 * RATES-12329; Heflin; Removed extraneous new line in logging for completed thread.
 *
 * Revision 1.7  2017/11/14 22:31:55  dtp4395
 * RATES-12329; Heflin; Removed extraneous new line in logging for completed thread.
 *
 * Revision 1.6  2017/10/27 19:16:23  dtp4395
 * RATES-12320; Heflin; Improved logging messages.
 *
 * Revision 1.5  2017/09/14 19:13:10  dtp4395
 * RATES-12061; Heflin; Improved logging messages.
 *
 * Revision 1.4  2017/03/02 22:48:42  dtp4395
 * RATES-11849; Heflin; Fixed null resulting from initialization race.  Added memory logging.
 *
 * Revision 1.3  2017/03/01 05:16:59  dtp4395
 * RATES-11849; Heflin; Added Thread Pool Monitoring functionality.  Fixed synchronization issues.
 *
 * Revision 1.2  2017/02/22 21:57:52  dtp4395
 * RATES-11849; Heflin; Refactoring to rename variables, improve logging and monitoring.
 *
 * Revision 1.1  2017/02/22 01:08:46  dtp4395
 * RATES-11849; Heflin; Created class to impose limits on Webservice threads and maintain service fairness.
 *
 *************************************************************
 *
 * Copyright (C) 2017 The Hertz Corporation
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
