package com.hertz.api.corebusiness.logging;

/**
 * This bean contains statistics and status information for a single RUM file. 
 *
 */
public class RumStatsPerFile {

    /** File Name */
    private String fileName;
    
    // File Status:
    // - "Started", "Finished", etc. etc.
    private String fileStatus;
    
    // Counts
    private long totalRecordsReceived;
    private long recordsUpdated;
    private long databaseUpdates;
    private long fileUpdates;
    
    // Elapsed Times
    private long fileSortingTime;
    private long writingOutputFileTime;
    private long retrieveLocationInfoTime;
    private long totalDatabaseUpdateTime;
    private long totalFileTime;
    
    // Time buckets
    private long updatesUnder1Min;
    private long updatesUnder5Min;
    private long updatesUnder10Min;
    private long updatesUnder15Min;
    private long updatesUnder30Min;
    private long updatesUnder60Min;
    private long updatesUnder120Min;
    private long updatesUnder300Min;
    
    // ETE times
    private long startTime;
    private long endTime;

    /**
     * Constructor
     * @param fileName
     */
    public RumStatsPerFile(String fileName) {

        super();
        this.fileName = fileName.trim();
    }

    public void addTotalDatabaseUpdateTime(long time) {

        if (time < 18000000L) {
            updatesUnder300Min++;
        }
        if (time < 7200000L) {
            updatesUnder120Min++;
        }
        if (time < 3600000L) {
            updatesUnder60Min++;
        }
        if (time < 1800000L) {
            updatesUnder30Min++;
        }
        if (time < 900000L) {
            updatesUnder15Min++;
        }
        if (time < 600000L) {
            updatesUnder10Min++;
        }
        if (time < 300000L) {
            updatesUnder5Min++;
        }
        if (time < 60000L) {
            updatesUnder1Min++;
        }

        totalDatabaseUpdateTime = totalDatabaseUpdateTime + time;
    }

    public void addTotalFileTime(long time) {

        totalFileTime = totalFileTime + time;
    }

    public long getTotalFileTime() {

        return totalFileTime;
    }

    public long getTotalDatabaseUpdateTime() {

        return totalDatabaseUpdateTime;
    }

    public void addTotalRecordsReceived(long recordsReceived) {

        totalRecordsReceived = totalRecordsReceived + recordsReceived;
    }

    public long getTotalRecordsReceived() {

        return totalRecordsReceived;
    }

    public void addRecordsUpdated(long updated) {

        recordsUpdated = recordsUpdated + updated;
    }

    public long getRecordsUpdated() {

        return recordsUpdated;
    }

    public void addDatabaseUpdates(long updated) {

        databaseUpdates = databaseUpdates + updated;
    }

    public long getDatabaseUpdates() {

        return databaseUpdates;
    }

    public void addFileUpdates(long updated) {

        fileUpdates = fileUpdates + updated;
    }

    public long getFileUpdates() {

        return fileUpdates;
    }

    public void addFileSortingTime(long time) {

        fileSortingTime = fileSortingTime + time;
    }

    public long getFileSortingTime() {

        return fileSortingTime;
    }

    public void addWritingOutputFileTime(long time) {

        writingOutputFileTime = writingOutputFileTime + time;
    }

    public long getWritingOutputFileTime() {

        return writingOutputFileTime;
    }

    public void addRetrieveLocationInfoTime(long time) {

        retrieveLocationInfoTime = retrieveLocationInfoTime + time;
    }

    public long getRetrieveLocationInfoTime() {

        return retrieveLocationInfoTime;
    }

    public String getFileName() {

        return fileName;
    }

    public void setFileName(String fileName) {

        this.fileName = fileName;
    }

    public long getUpdatesUnder1Min() {

        return updatesUnder1Min;
    }

    public long getUpdatesUnder5Min() {

        return updatesUnder5Min;
    }

    public long getUpdatesUnder10Min() {

        return updatesUnder10Min;
    }

    public long getUpdatesUnder15Min() {

        return updatesUnder15Min;
    }

    public long getUpdatesUnder30Min() {

        return updatesUnder30Min;
    }

    public long getUpdatesUnder60Min() {

        return updatesUnder60Min;
    }

    public long getUpdatesUnder120Min() {

        return updatesUnder120Min;
    }

    public long getUpdatesUnder300Min() {

        return updatesUnder300Min;
    }

    public String getFileStatus() {

        return fileStatus;
    }

    public void setFileStatus(String fileStatus) {

        this.fileStatus = fileStatus;
    }

    public long getStartTime() {

        return startTime;
    }

    public void setStartTime(long startTime) {

        this.startTime = startTime;
    }

    public long getEndTime() {

        return endTime;
    }

    public void setEndTime(long endTime) {

        this.endTime = endTime;
    }

    /**
     * Describe Object
     * @return
     */
    public String toStringVerbose() {

        return toStringVerbose("");
    }

    /**
     * Describe Object
     * @param indent
     * @return
     */
    public String toStringVerbose(String indent) {

        StringBuffer s = new StringBuffer();

        s.append(indent + "RumStatsPerFile:" + "\n");

        s.append(indent + "  fileName                  : " + getFileName() + "\n");
        
        s.append(indent + "  fileStatus                : " + getFileStatus() + "\n");
        
        s.append(indent + "  totalRecordsReceived      : " + getTotalRecordsReceived() + "\n");
        s.append(indent + "  recordsUpdated            : " + getRecordsUpdated() + "\n");
        s.append(indent + "  databaseUpdates           : " + getDatabaseUpdates() + "\n");
        s.append(indent + "  fileUpdates               : " + getFileUpdates() + "\n");
        
        s.append(indent + "  fileSortingTime           : " + getFileSortingTime() + "\n");
        s.append(indent + "  writingOutputFileTime     : " + getWritingOutputFileTime() + "\n");
        s.append(indent + "  retrieveLocationInfoTime  : " + getRetrieveLocationInfoTime() + "\n");
        s.append(indent + "  totalDatabaseUpdateTime   : " + getTotalDatabaseUpdateTime() + "\n");
        s.append(indent + "  totalFileTime             : " + getTotalFileTime() + "\n");
        
        s.append(indent + "  updatesUnder1Min          : " + getUpdatesUnder1Min() + "\n");
        s.append(indent + "  updatesUnder5Min          : " + getUpdatesUnder5Min() + "\n");
        s.append(indent + "  updatesUnder10Min         : " + getUpdatesUnder10Min() + "\n");
        s.append(indent + "  updatesUnder15Min         : " + getUpdatesUnder15Min() + "\n");
        s.append(indent + "  updatesUnder30Min         : " + getUpdatesUnder30Min() + "\n");
        s.append(indent + "  updatesUnder60Min         : " + getUpdatesUnder60Min() + "\n");
        s.append(indent + "  updatesUnder120Min        : " + getUpdatesUnder120Min() + "\n");
        s.append(indent + "  updatesUnder300Min        : " + getUpdatesUnder300Min() + "\n");
        
        s.append(indent + "  startTime                 : " + getStartTime() + "\n");
        s.append(indent + "  endTime                   : " + getEndTime() + "\n");

        s.append(indent + "<<<<" + "\n");

        return s.toString();
    }
}

/*
 *************************************************************
 * Change History:
 *
 * $Log$
 * Revision 1.4  2017/08/18 19:39:47  dtp4395
 * RATES-12061; Heflin; Comment and Formatting changes only.
 *
 * Revision 1.3  2017/02/21 17:05:50  dtp4395
 * RATES-11849; Heflin; Made small changes to toStringVerbose().
 *
 * Revision 1.2  2017/02/07 21:06:36  dtp4395
 * RATES-11849; Heflin; Renamed method.  Added toStringVerbose().  Added comments.
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