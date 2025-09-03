package com.hertz.api.corebusiness;

import com.hertz.rates.common.utils.HertzException;
import com.hertz.rates.common.utils.config.ConfigData;
import com.hertz.rates.common.utils.config.PropertyGroup;
import com.hertz.rates.common.utils.logging.HertzLogger;
import com.hertz.rates.common.utils.logging.LogLevel;
import com.hertz.api.corebusiness.errorcodes.RumErrorCodes;

/**
 * Monitor the health of the thread pool.  
 * 
 * Detect really old threads that appear to have never finished and remove them.
 * @author jwheflin
 *
 */
public class WebServiceThreadManagerMonitor extends Thread {

    final static HertzLogger logger = new HertzLogger(WebServiceThreadManagerMonitor.class);

    /** Sleep time between checking the Thread Pool for dead threads (in milliseconds): (10 min * 60 sec/min * 1000 millis/sec) */
    //private final static long DURATION_BETWEEN_CHECKS_IN_MILLIS = (10L * 60L * 1000L);
    // FOR TESTING: 3 minutes
    private final static long DURATION_BETWEEN_CHECKS_IN_MILLIS = (3L * 60L * 1000L);
    
    /** Default Time in Thread Manager before a thread is considered to be dead: (5 min * 60 sec/min * 1000 millis/sec) */
    private final static long DEAD_THREAD_AGE_IN_MILLIS_DEFAULT = (5L * 60L * 1000L);
    /** This is the current value for age of a dead thread.  This value is configurable.*/
    private static long deadThreadAgeInMillis = -1;

    /**
     * Constructor
     */
    public WebServiceThreadManagerMonitor() {
        // Do nothing.
    }

    /**
     * Monitor the health of the thread pool.  Report and remove any dead threads.
     * This thread uses non-blocking calls to WebServiceThreadManager. 
     */
    public void run() {

        boolean FOREVER = true;
        
        logger.info("<WSTMM> WebServiceThreadManagerMonitor - Starting...");
        
        while (FOREVER) {

            doDeadThreadCheck();

            // Sleep until time to check again.
            try {
                Thread.sleep(DURATION_BETWEEN_CHECKS_IN_MILLIS);
                logger.debug("<WSTMM> WebServiceThreadManagerMonitor - waking and doing dead thread check");
            }
            catch (InterruptedException e) {
                HertzException.eatAndLogNonCriticalException(LogLevel.ERROR, RumErrorCodes.GENERIC_ERROR, e, "<WSTMM> Error when waiting to do dead thread check.");
            }
        }
    }

    /**
     * Check for Dead Threads.  Report and remove them.
     */
    private void doDeadThreadCheck() {

        try {
            final long now = System.currentTimeMillis();
            final long ageWhenDead = getDeadThreadAgeInMillis();

            Object[] elements = WebServiceThreadManager.getThreadPoolAsArray();
            if (elements != null) {
                for (int i = 0; i < elements.length; i++) {

                    Object o = elements[i];
                    if (o != null) {
                        if (o instanceof UpdateThread) {
                            UpdateThread updateThread = (UpdateThread) o;

                            long start = updateThread.getTimePutIntoThreadPool();
                            long age = (now - start);

                            if (age > ageWhenDead) {
                                // This thread is so old that it is considered to be 'dead'.  
                                // It should have completed and removed itself from the Thread Pool by now.
                                // Remove it manually.
                                logger.error("<WSTMM> WebServiceThreadManagerMonitor - DEAD THREAD DETECTED - start: " + start + " now: " + now + " ageWhenDead: " + ageWhenDead
                                        + " age: " + age + " ms." + " Trans ID: " + updateThread.getThreadId() + " thread index: " + updateThread.getIndex());

                                // Mark thread finished and remove from Thread Pool.
                                updateThread.setFinishedWithUpdate(true);
                                WebServiceThreadManager.removeDeadThread(updateThread);
                            }
                            else {
                                // This thread is fine and is not dead yet.  Keep looking.
                            }
                        }
                        else {
                            logger.error("<WSTMM> WebServiceThreadManagerMonitor - wrong object type: " + o.getClass().getName());
                        }
                    }
                    else {
                        logger.warn("<WSTMM> WebServiceThreadManagerMonitor - null element in Thread Pool index: " + i);
                    }
                }
            }
            else {
                logger.warn("<WSTMM> WebServiceThreadManagerMonitor - Thread Pool as array is null - not initialized yet.");
            }
        }
        catch (Exception e1) {
            // TODO Auto-generated catch block
            logger.error("Error During doDeadThreadCheck");
            e1.printStackTrace();
        }
    }
    
    /**
     * Return the configurable parameter for the number of milliseconds that
     * a thread can run before it is considered a 'Long Running Thread'.
     * Such a thread will be removed from the Thread Pool.
     * @return
     */
    private long getDeadThreadAgeInMillis() {

        if (deadThreadAgeInMillis < 0) {
            // Initialize using default if there isn't a setting on config.
            deadThreadAgeInMillis = DEAD_THREAD_AGE_IN_MILLIS_DEFAULT;

            final String GROUP = "WebServicesControl";
            final String PROPERTY = "DeadThreadAgeInMillis";

            try {
                ConfigData configData = ConfigData.getInstance();
                if (configData == null) {
                    // Could throw an exception here.  Or just use the default value.
                    logger.debug("<WSTMM> getDeadThreadAgeInMillis - invalid ConfigData");
                }
                else {
                    PropertyGroup propertyGroup = configData.getGroup(GROUP);
                    if (propertyGroup == null) {
                        logger.debug("<WSTMM> getDeadThreadAgeInMillis - propertyGroup is null");
                    }
                    else {
                        String processingPathStr = propertyGroup.getPropertyValue(PROPERTY);
                        if (processingPathStr == null) {
                            // Could throw an exception here.  Or just use the default value.
                            logger.debug("<WSTMM> getDeadThreadAgeInMillis - property value is null");
                        }
                        else {
                            try {
                                int value = Integer.parseInt(processingPathStr.trim());
                                deadThreadAgeInMillis = value;
                            }
                            catch (NumberFormatException e) {
                                // Could throw an exception here.  Or just use the default value.
                                logger.error("<WSTMM> getDeadThreadAgeInMillis - invalid value for deadThreadAgeInMillis: >" + processingPathStr + "<");
                            }
                        }
                    }
                }
            }
            catch (HertzException e) {
                // Do nothing, just return the default value.
                logger.debug("<WSTMM> getDeadThreadAgeInMillis - ConfigData issue");
            }

            logger.debug("<WSTMM> Using getDeadThreadAgeInMillis: " + deadThreadAgeInMillis);
        }

        return deadThreadAgeInMillis;
    }
}

/*************************************************************
 * Change History:
 *
 * $Log$
 * Revision 1.5  2017/10/27 19:17:43  dtp4395
 * RATES-12320; Heflin; Improved logging messages by adding Trans ID: to dead thread reporting.
 *
 * Revision 1.4  2017/03/06 01:48:52  dtp4395
 * RATES-11849; Heflin; Added error condition logging.
 *
 * Revision 1.3  2017/03/02 22:49:13  dtp4395
 * RATES-11849; Heflin; Test for null resulting from initialization race.
 *
 * Revision 1.2  2017/03/01 05:16:17  dtp4395
 * RATES-11849; Heflin; Fixed config title and increased logging.
 *
 * Revision 1.1  2017/03/01 03:56:56  dtp4395
 * RATES-11849; Heflin; Created to monitor Thread Pool.
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
