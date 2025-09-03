package com.hertz.api.corebusiness;

import com.hertz.rates.common.service.data.DataServiceLocator;
import com.hertz.rates.common.utils.HertzException;
import com.hertz.rates.common.utils.logging.HertzLogger;
import com.hertz.rates.common.utils.logging.LogLevel;
import com.hertz.api.corebusiness.logging.RumStats;
import com.hertz.api.corebusiness.logging.RumWebStats;
import com.hertz.api.service.data.update.IRumUpdateDataService;

/**
 * This thread performs RUM updates for a single Plan/Place.
 * These changes are held in the RumUpdateGroup
 *
 */
public class UpdateThread extends Thread {

    private static final  HertzLogger logger = new HertzLogger(UpdateThread.class);

    private static final  String RUM_UPDATE_DS = "IRumUpdateDataService";

    /** Thread variables */
    private RumUpdateGroup updateGroup;
    
    /** Thread ID that is passed when thread is created. */
    private long threadId;

    /** Populated if this thread is for a File-based call. */
    private String fileName;

    /** Populated if this thread is for a Webservice call. */
    private String transactionId;

    /** This is the thread index from the thread creator: UpdateDriver */
    private long index;
    
    /** Is this thread a file-based thread? */
    private boolean isFileBasedThread = false;
    /** Is this thread a Web Service thread? */
    private boolean isWebserviceThread = false;
    
    /** Data service for DB call. */
    private IRumUpdateDataService reqTrnDataSvc = null;
    
    /** Is this thread finished with its work ? */
    private boolean finishedWithUpdate = false;

    
    /** This is the length of time a Thread can be in the DB call before it is considered to be a 'Long Running' thread. */
    private long longRunningInMillis = 0;
    
    /** This is the time that this thread was put into the thread pool. */
    private long timePutIntoThreadPool = 0;
    
    
    /**
     * Constructor
     * @param threadId
     * @param group
     * @param fileName
     * @param transactionId
     * @param index
     * @param longRunningInMillis
     */
    public UpdateThread(long threadId, RumUpdateGroup group, String fileName, String transactionId, long index, long longRunningInMillis) {

        this.updateGroup = group;
        this.threadId = threadId;
        
        // Only one of the following will be non-null.
        this.fileName = fileName;
        this.transactionId = transactionId;
        
        this.index = index;
        this.longRunningInMillis = longRunningInMillis;
        
        // One of the following will be set to true.
        if (fileName != null) {
            isFileBasedThread = true;
        }
        
        if (transactionId != null) {
            isWebserviceThread = true;
        }
    }

    /**
     * This is a call the data service layer and call the DB to do the update.
     */
    public void run() {

        final String methodName;
        final String threadLogStart;
        if (fileName != null) {
            methodName = RumStats.RECORDS_UPDATED_METHOD + updateGroup.getChangeDetails().size() + RumStats.FILE_NAME + this.fileName;
            threadLogStart = "<FB> " + " Thread Start " + " Filename: " + this.getFileName();
        }
        else {
            methodName = RumWebStats.NUMBER_OF_UPDATES_IN_PLAN_PLACE + updateGroup.getChangeDetails().size() + RumWebStats.TRANS_ID + this.transactionId;
            threadLogStart = "<WS> " + " Thread Start " + RumWebStats.TRANS_ID + this.getTransactionId();
        }
        logger.entry(LogLevel.INFO, methodName);

        logger.info(threadLogStart + " Thread " + getThreadId() + " index " + getIndex() + " for: " +  updateGroup.getLocation() + " " + updateGroup.getPlaceIdCd() + " " + updateGroup.getCompanyId() + "  " + updateGroup.getPlanId() + " " + updateGroup.getPlanType() + " updates: " + updateGroup.getChangeDetails().size());
        
        try {
            DataServiceLocator svcLookup = new DataServiceLocator();
            reqTrnDataSvc = (IRumUpdateDataService) svcLookup.getService(RUM_UPDATE_DS);

            long start = markThreadBegin();
            
            updateGroup = reqTrnDataSvc.doRumUpdate(updateGroup);
            
            markThreadEnd(start);
        }
        catch (HertzException e) {
            if (isFileBasedThread) {
                logger.warn("<FB> HertzException in Update Thread threadID: " + getThreadId());
            }
            else if (isWebserviceThread) {
                logger.warn("<WS> HertzException in " + RumWebStats.TRANS_ID + this.getTransactionId() + " Update Thread threadID: " + getThreadId());
            }
            updateGroup.addErrorMessageToDetails(e);
        }
        catch (Exception e1) {
            if (isFileBasedThread) {
                logger.warn("<FB> Other Exception in Update Thread threadID: " + getThreadId());
            }
            else if (isWebserviceThread) {
                logger.warn("<WS> Other Exception in " + RumWebStats.TRANS_ID + this.getTransactionId() + " Update Thread threadID: " + getThreadId());
            }
            updateGroup.addErrorMessageToDetails(e1);
        }
        finally {
            //  UpdateDriver.mapThreadResponse(fileName, updateGroup);

            // This *must* happen or the Thread Pool will lose a slot.
            if (isWebserviceThread) {
                // Completed thread is for a Webservice call. Remove it from the Webservice Thread Manager.
                WebServiceThreadManager.getWebThreadManager().removeCompletedThread(this);
            }

            logger.exit(LogLevel.INFO, methodName);

            finishedWithUpdate = true;
        }
    }

    public long getThreadId() {

        return threadId;
    }

    public RumUpdateGroup getUpdateGroup() {

        return updateGroup;
    }

    /**
     * Called by UpdateDriver to determine if this thread is finished with its work.
     * @return
     */
    public boolean isFinishedWithUpdate() {

        return finishedWithUpdate;
    }
    
    
    public String getFileName() {
    
        return fileName;
    }

    
    public String getTransactionId() {
    
        return transactionId;
    }
    
    public long getIndex() {
        
        return index;
    }

    public long getTimePutIntoThreadPool() {
        
        return timePutIntoThreadPool;
    }

    
    public void setTimePutIntoThreadPool(long timePutIntoThreadPool) {
    
        this.timePutIntoThreadPool = timePutIntoThreadPool;
    }

    public void setFinishedWithUpdate(boolean finishedWithUpdate) {
        
        this.finishedWithUpdate = finishedWithUpdate;
    }

    /**
     * Mark when a Thread call to DB begins.
     * @return
     */
    private long markThreadBegin() {
        
        //        // TESTING: Add a huge delay here to FORCE this thread to look dead.
        //        // Sleep until time to check again.
        //        try {
        //            Thread.sleep(1000 * 60 * 1000); // 1000 minutes
        //            logger.debug("<WS> UpdateThread - killing time until dead");
        //        }
        //        catch (InterruptedException e) {
        //            HertzException.eatAndLogNonCriticalException(LogLevel.ERROR, RumErrorCodes.GENERIC_ERROR, e, "<WSTMM> Error when waiting to do dead thread check.");
        //        }

        long now = System.currentTimeMillis();
        return now;
    }

    /**
     * Mark when a thread call to DB ends.
     * Report if this was a long call.
     * @param begin
     */
    private void markThreadEnd(long begin) {

        long now = System.currentTimeMillis();
        long duration = (now - begin);

        if (isFileBasedThread) {
            // File Based Thread
            logger.debug("<FB> RUM Update call duration: " + duration + " file: " + this.fileName + " threadID: " + this.threadId);
            
            // For now, we're not logging details of long-running File-Based DB calls... 
            // We can get that information from the files themselves.
        }
        else if (isWebserviceThread) {
            // Web Service Thread
            long nowTime = System.currentTimeMillis();
            logger.debug("<WS> markThreadEnd - marking thread done now: " + nowTime + " with start: " + begin + " and end: " + now + " for duration: " + duration);
            
            logger.info("<WS> " + RumWebStats.TRANS_ID + getTransactionId() + " threadID: " + this.threadId + " index: " + getIndex() + " RUM Update call duration: " + duration);

            if (duration > this.longRunningInMillis) {
                // Log parameters for this Long-Running call.
                logger.warn(this.toStringVerbose("<WS> LONG RUNNING THREAD " + RumWebStats.TRANS_ID + getTransactionId() + " threadID: " + this.threadId + " index: " + getIndex()));
            }
        }
    }

    /**
     * Describe this object.
     * @return
     */
    public String toStringVerbose() {

        return toStringVerbose("", "");
    }
    
    /**
     * Describe this object.
     * @param title
     * @return
     */
    public synchronized String toStringVerbose(String title) {

        return toStringVerbose("", title);
    }

    /**
     * Describe this object.
     * @return
     */
    public synchronized String toStringVerbose(String indent, String title) {
        
        StringBuffer s = new StringBuffer();

        s.append(indent + "UpdateThread" + "\n");
        
        s.append(indent + "  title                 : " + title + "\n");
        
        s.append(indent + "  isFileBasedThread     : " + isFileBasedThread + "\n");
        s.append(indent + "  isWebServiceThread    : " + isWebserviceThread + "\n");
        
        s.append(indent + "  updateGroup           : " + getUpdateGroup().toStringVerbose("    ") + "\n");
        
        s.append(indent + "  threadId              : " + getThreadId() + "\n");

        s.append(indent + "  fileName              : " + getFileName() + "\n");
        s.append(indent + "  transactionId         : " + getTransactionId() + "\n");
        
        s.append(indent + "  index                 : " + getIndex() + "\n");
        s.append(indent + "  timePutIntoThreadPool : " + getTimePutIntoThreadPool() + "\n");
        
      //s.append(indent + "  reqTrnDataSvc         : " + getReqTrnDataSvc() + "\n");
        
        s.append(indent + "  finishedWithUpdate    : " + isFinishedWithUpdate() + "\n");
        
        // Display short description of Updates.
        s.append(indent + "  updateGroup location  : " + getUpdateGroup().getPlaceIdCd() + "\n");
        s.append(indent + "  updateGroup plan      : " + getUpdateGroup().getPlanId() + "\n");
        s.append(indent + "  updateGroup location  : " + getUpdateGroup().getPlanType() + "\n");

        s.append(indent + "<<<<" + "\n");
        
        return s.toString();
    }
}

/*
 *************************************************************
 * Change History:
 *
 * $Log$
 * Revision 1.24  2017/11/19 02:32:26  dtp4395
 * RATES-12329; Heflin; Modified markThreadBegin() logging.
 *
 * Revision 1.23  2017/11/16 23:30:48  dtp4395
 * RATES-12329; Heflin; Improvded logging and thread identification.
 *
 * Revision 1.22  2017/11/01 16:06:37  dtp4395
 * RATES-12061; Heflin; Improved logging of thread start in rum() to identify File-based vs. WS.
 *
 * Revision 1.21  2017/09/14 19:14:15  dtp4395
 * RATES-12061; Heflin; Improved logging messages, corrected timing statistics, formatted, and added comments.
 *
 * Revision 1.20  2017/03/08 07:06:00  dtp4395
 * RATES-11849; Heflin; Enhanced toStringVerbose().
 *
 * Revision 1.19  2017/03/06 01:48:23  dtp4395
 * RATES-11849; Heflin; Small format change only.
 *
 * Revision 1.18  2017/03/01 05:18:08  dtp4395
 * RATES-11849; Heflin; Added functionality to detect and report long-running threads.
 *
 * Revision 1.17  2017/02/23 19:00:25  dtp4395
 * RATES-11849; Heflin; Moved the thread pool return action into the finally block to insure it always happens.
 *
 * Revision 1.16  2017/02/22 21:58:25  dtp4395
 * RATES-11849; Heflin; Refactoring to improve logging and monitoring.
 *
 * Revision 1.15  2017/02/22 01:49:08  dtp4395
 * RATES-11849; Heflin; Merged changes from 17A branch.
 *
 * Revision 1.12.2.2  2017/02/22 01:11:34  dtp4395
 * RATES-11849; Heflin; Added code for WebService thread to remove itself from ThreadManager when complete, commented, and added toStringVerbose().
 *
 * Revision 1.12.2.1  2017/01/16 23:14:15  dtp4395
 * RATES-11849; Heflin; Merged changes from HEAD.
 *
 * Revision 1.14  2017/01/16 22:23:40  dtp4395
 * RATES-11849; Heflin; Refactored methodName for logging.
 *
 * Revision 1.13  2017/01/11 23:00:14  dtp4395
 * RATES-11849; TESTING - Added transaction ID to class.  Add transaction ID to entry/exit methodNames for Webservice calls.
 *
 * Revision 1.12  2016/10/27 15:23:47  dtp4395
 * RATES-11849; Heflin; Added/fixed comments.  Synchronized variable modifier order.  Grouped variables and commented.  Formatted.  No code changes.
 *
 * Revision 1.11  2014/07/31 20:24:46  dtc1090
 * RATES-8035 - fixed deadlock on connection when using ARRAY
 *
 * Revision 1.10  2014/04/29 16:12:05  dtc1090
 * RATES-7814 - Maklee Enhancements - Using more than 1 version of update proc and adding functionality to RUM to update more than 1 file at a time.
 *
 * Revision 1.9  2011/03/18 16:50:45  dtc1090
 * Added more Stat gathering
 *
 * Revision 1.8  2011/02/28 23:19:00  dtc1090
 * organized imports
 *
 * Revision 1.7  2011/02/28 23:00:38  dtc1090
 * refactored RUM
 *
 * Revision 1.6  2011/02/24 16:15:14  dtc1090
 * changed the way we control number of threads
 *
 * Revision 1.5  2011/02/24 14:28:20  dtc1090
 * modified how we create thread id
 *
 * Revision 1.4  2011/02/23 17:24:11  dtc1090
 * added govenor to control number of concurrent updates
 *
 * Revision 1.3  2011/01/27 20:04:47  dtc1090
 * rearranged packages
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