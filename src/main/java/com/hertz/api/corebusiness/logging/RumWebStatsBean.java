package com.hertz.api.corebusiness.logging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.hertz.rates.common.utils.HertzException;
import com.hertz.rates.common.utils.logging.HertzLogger;
import com.hertz.rates.common.utils.logging.LogLevel;
import com.hertz.api.corebusiness.errorcodes.RumErrorCodes;

/**
 * Holds Webservices Call statistics for the Monitoring function.
 *
 */
public class RumWebStatsBean {

    /** Identifies this Webservice call */
    private String transactionId;

    /** ETE Transaction Start */
    private long transactionStartTime;
    /** ETE Transaction End */
    private long transactionEndTime;

    /** Duration of Location lookup */
    private long retrieveLocationTime;

    // Stored Procedure timing data

    // An array holding the begin / end times of all stored procedure calls for this transaction.
    private List<RumStoredProcedureCallTiming> rumStoredProcedureCalls = new ArrayList<RumStoredProcedureCallTiming>();

    /** Stored procedure time */
    private long storedProcedureTime;

    // Update Count data

    /** Number of individual changes: Place/Plan/Date Span/Vehicle items. */
    private long numberOfUpdates;

    private final static HertzLogger logger = new HertzLogger(RumWebStatsBean.class);

    /**
     * Constructor
     */
    public RumWebStatsBean() {

    }

    /**
     * Constructor
     * @param transId
     * @param startTime
     */
    public RumWebStatsBean(String transId, long startTime) {

        transactionId = transId;
        transactionStartTime = startTime;
    }

    //
    // Getters
    //

    public String getTransactionId() {

        return transactionId;
    }

    public long getTransactionStartTime() {

        return transactionStartTime;
    }

    public long getTransactionEndTime() {

        return transactionEndTime;
    }

    public long getRetrieveLocationTime() {

        return retrieveLocationTime;
    }

    /**
     * Called to return procedure time to Monitoring function.
     * @return
     */
    public long getStoredProcedureTime() {

        return this.storedProcedureTime;
    }

    public long getNumberOfUpdates() {

        return numberOfUpdates;
    }

    //
    // Setters / Modifiers
    //

    public void setTransactionId(String transactionId) {

        this.transactionId = transactionId;
    }

    public void setTransactionStartTime(long transactionStartTime) {

        this.transactionStartTime = transactionStartTime;
    }

    public void setTransactionEndTime(long transactionEndTime) {

        this.transactionEndTime = transactionEndTime;
    }

    public void setRetrieveLocationTime(long retrieveLocationTime) {

        this.retrieveLocationTime = retrieveLocationTime;
    }

    /**
     * Add the current procedure time to the accruing total.  Called by the LOG4J method.
     * @param procStart
     * @param procEnd
     */
    public void addToStoredProcedureTime(long procStart, long procEnd) {

        rumStoredProcedureCalls.add(new RumStoredProcedureCallTiming(procStart, procEnd));
        evaluateStoredProcedureTime();
    }

    /**
     * Process the current list of Stored Procedure call times and store the revised value. 
     */
    private void evaluateStoredProcedureTime() {

        final String methodName = "evaluateStoredProcedureTime";
        try {
            logger.entry(HertzLogger.DEBUG, methodName);

            // Process and update with new value.
            this.storedProcedureTime = processTimingPoints(buildSortedTimingEventList());
        }
        catch (HertzException e) {
            HertzException.eatAndLogNonCriticalException(LogLevel.ERROR, RumErrorCodes.STORED_PROC_TIMING_IS_INVALID, e, "Stored Procedure timing is invalid.");
        }
        finally {
            logger.exit(HertzLogger.DEBUG, methodName);
        }
    }

    /**
     * Build a sorted list of Stored Procedure timing events.
     * @return
     */
    private List<RumStoredProcTimingPoint> buildSortedTimingEventList() {

        List<RumStoredProcTimingPoint> rumStoredProcTimingPoints = new ArrayList<RumStoredProcTimingPoint>();

        Iterator<RumStoredProcedureCallTiming> i = rumStoredProcedureCalls.iterator();
        while (i.hasNext()) {
            RumStoredProcedureCallTiming rumStoredProcedureCallTiming = i.next();

            // Create timing point for Begin event.
            rumStoredProcTimingPoints.add(new RumStoredProcTimingPoint(rumStoredProcedureCallTiming.getStart(), RumStoredProcTimingPoint.BEGIN));

            // Create timing point for End event.
            rumStoredProcTimingPoints.add(new RumStoredProcTimingPoint(rumStoredProcedureCallTiming.getEnd(), RumStoredProcTimingPoint.END));
        }

        // Sort the Begin/End times.
        Collections.sort(rumStoredProcTimingPoints);

        // Display the Timing Points
        if (logger.isDebugEnabled()) {
            displayRumStoredProcedureCalls(rumStoredProcTimingPoints, "rumStoredProcTimingPoints sorted: \n");
        }

        return rumStoredProcTimingPoints;
    }

    /**
     * Process the sorted Stored Procedure Timing Points for Begin/End events.  Total up the times spent in DB calls.
     * 
     * @param rumStoredProcTimingPoints
     * @return
     * @throws HertzException
     */
    private long processTimingPoints(List<RumStoredProcTimingPoint> rumStoredProcTimingPoints) throws HertzException {

        // Process the timing points grouping overlapping call intervals and totaling.
        long newStoredProcedureTime = 0;
        int depth = 0;

        long timingGroupBeginTime = 0;

        // Iterate through list of sorted Timing Points.
        Iterator<RumStoredProcTimingPoint> i = rumStoredProcTimingPoints.iterator();
        while (i.hasNext()) {
            RumStoredProcTimingPoint rumStoredProcTimingPoint = i.next();

            // If depth = 0, then we are about to enter a timing group.  Get the Begin time.
            if (depth == 0) {

                switch (rumStoredProcTimingPoint.pointType) {
                    
                    case RumStoredProcTimingPoint.BEGIN:
                        timingGroupBeginTime = rumStoredProcTimingPoint.getTime(); // Valid: get the Begin time.
                        break;
                        
                    case RumStoredProcTimingPoint.END:
                        // We should never encounter an END event when depth is 0.
                        throw new HertzException(RumErrorCodes.STORED_PROC_TIMING_IS_INVALID, "Stored Procedure timing is invalid - found END at depth 0."); // Not valid, throw an exception
                }
            }

            // Process this timing event.
            switch (rumStoredProcTimingPoint.pointType) {
                
                case RumStoredProcTimingPoint.BEGIN:
                    depth++; // Increment depth
                    break;
                    
                case RumStoredProcTimingPoint.END:
                    depth--; // Decrement depth
                    break;
            }

            // If depth = 0 now, then we just finished a timing group.  Determine its length.
            if (depth == 0) {
                if (rumStoredProcTimingPoint.pointType == RumStoredProcTimingPoint.BEGIN) {
                    // We should never have a depth of 0 after a BEGIN event.
                    throw new HertzException(RumErrorCodes.STORED_PROC_TIMING_IS_INVALID, "Stored Procedure timing is invalid - zero depth not at END."); // Not valid, throw an exception
                }

                long timingGroupDuration = (rumStoredProcTimingPoint.getTime() - timingGroupBeginTime);
                // Add this groups's duration to the total.
                newStoredProcedureTime += timingGroupDuration;
            }
            else if (depth < 0) {
                // We should never have a depth < 0.
                throw new HertzException(RumErrorCodes.STORED_PROC_TIMING_IS_INVALID, "Stored Procedure timing is invalid - depth < 0."); // Not valid, throw an exception
            }
        }

        if (depth != 0) {
            // We should never encounter an END event when depth is 0.
            throw new HertzException(RumErrorCodes.STORED_PROC_TIMING_IS_INVALID, "Stored Procedure timing is invalid - final depth is not zero."); // Not valid, throw an exception
        }

        return newStoredProcedureTime;
    }

    /**
     * Display the Stored Procedure Timing Points.
     * @param rumStoredProcTimingPoints
     * @return
     */
    private void displayRumStoredProcedureCalls(List<RumStoredProcTimingPoint> rumStoredProcTimingPoints, String title) {

        StringBuffer s = new StringBuffer();

        Iterator<RumStoredProcTimingPoint> i = rumStoredProcTimingPoints.iterator();
        while (i.hasNext()) {
            RumStoredProcTimingPoint rumStoredProcTimingPoint = i.next();
            s.append(rumStoredProcTimingPoint.toStringVerbose());
        }

        logger.debug(title + " TransID: " + transactionId + " " + s.toString());
    }

    /**
     * Sets the procedure time - this is called when we retrieve historical values from the DB table: RUM_WEB_TRANS_HISTORY
     * 
     * @param storedProcTime
     */
    public void setStoredProcedureTime(long storedProcTime) {

        this.storedProcedureTime = storedProcTime;
    }

    public void addToNumberOfUpdates(long numberOfUpdates) {

        this.numberOfUpdates = this.numberOfUpdates + numberOfUpdates;
    }

    public void setNumberOfUpdates(long numberOfUpdates) {

        this.numberOfUpdates = numberOfUpdates;
    }

    /**
     * Describe this object.
     * @return
     */
    public String toStringVerbose() {

        return toStringVerbose("");
    }

    public String toStringVerbose(String indent) {

        StringBuffer s = new StringBuffer();

        s.append(indent + "RumWebStatsBean: " + "\n");

        s.append(indent + "  transactionId             : " + getTransactionId() + "\n");
        s.append(indent + "  transactionStartTime      : " + getTransactionStartTime() + "\n");
        s.append(indent + "  transactionEndTime        : " + getTransactionEndTime() + "\n");
        s.append(indent + "  retrieveLocationTime      : " + getRetrieveLocationTime() + "\n");

        s.append(indent + "  storedProcedureTime       : " + getStoredProcedureTime() + "\n");

        s.append(indent + "  numberOfUpdates           : " + getNumberOfUpdates() + "\n");

        s.append(indent + "<<<<" + "\n");

        return s.toString();
    }
}

/*
 *************************************************************
 * Change History:
 *
 * $Log$
 * Revision 1.6  2017/03/08 17:06:39  dtp4395
 * RATES-11849; Heflin; Created and called updateStoredProcedureTime() to update the elapsed Stored Procedure time when possible.
 *
 * Revision 1.5  2017/02/07 21:02:52  dtp4395
 * RATES-11849; Heflin; Formatted, added comments.  Improved toStringVerbose().
 *
 * Revision 1.4  2017/01/16 22:25:26  dtp4395
 * RATES-11849; Heflin; Added addToNumberOfUpdates()
 *
 * Revision 1.3  2017/01/10 21:30:52  dtp4395
 * RATES-11849; TESTING - Added comments.
 *
 * Revision 1.2  2017/01/10 19:39:21  dtp4395
 * RATES-11849; Added toStringVerbose();
 *
 * Revision 1.1  2014/09/29 13:52:34  dtc1090
 * Checked in new code for DTAG Update Webservice
 *
 *
 *************************************************************
 *
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
 *************************************************************
 */
