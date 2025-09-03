package com.hertz.api.corebusiness;


/**
 * Holds statistics for RUM for a specific file from the Historical Data table.
 *
 */
public class RumStatsBean {

    private int createDate;
    private int createTime;

    private long numberOfDBUpdates;
    private long filesUpdated;
    private long totalRecordsReceived;
    private long recordsUpdated;
    
    private long totalDatabaseElapsedTime;
    private long totalFileElapsedTime;
    
    private float rumUpdatesUnderOneMinute;
    private float rumUpdatesUnderFiveMinutes;
    private float rumUpdatesUnderTenMintues;
    private float rumUpdatesUnderFifteenMintues;
    private float rumUpdatesUnderThirtyMintues;
    private float rumUpdatesUnderOneHour;
    private float rumUpdatesUnderTwoHours;
    private float rumUpdatesUnderFiveHours;
    
    private int numberOfThreads;
    
    private long capturedStartTime;
    private long capturedEndTime;
    private long retrieveLocationInfoTime;
    private long writeOutputFileTime;
    private long sortFileRecordsTime;
    
    private String fileName;
    private String fileStatus;

    
    public int getCreateTime() {

        return createTime;
    }

    public void setCreateTime(int createTime) {

        this.createTime = createTime;
    }

    public String getFileName() {

        return fileName;
    }

    public void setFileName(String fileName) {

        this.fileName = fileName;
    }

    public long getRetrieveLocationInfoTime() {

        return retrieveLocationInfoTime;
    }

    public void setRetrieveLocationInfoTime(long retrieveLocationInfoTime) {

        this.retrieveLocationInfoTime = retrieveLocationInfoTime;
    }

    public long getWriteOutputFileTime() {

        return writeOutputFileTime;
    }

    public void setWriteOutputFileTime(long writeOutputFileTime) {

        this.writeOutputFileTime = writeOutputFileTime;
    }

    public long getSortFileRecordsTime() {

        return sortFileRecordsTime;
    }

    public void setSortFileRecordsTime(long sortFileRecordsTime) {

        this.sortFileRecordsTime = sortFileRecordsTime;
    }

    public int getCreateDate() {

        return createDate;
    }

    public void setCreateDate(int createDate) {

        this.createDate = createDate;
    }

    public long getNumberOfDBUpdates() {

        return numberOfDBUpdates;
    }

    public float getRumUpdatesUnderFifteenMintues() {

        return rumUpdatesUnderFifteenMintues;
    }

    public void setRumUpdatesUnderFifteenMintues(float rumUpdatesUnderFifteenMintues) {

        this.rumUpdatesUnderFifteenMintues = rumUpdatesUnderFifteenMintues;
    }

    public void setNumberOfDBUpdates(long numberOfUpdates) {

        this.numberOfDBUpdates = numberOfUpdates;
    }

    public float getRumUpdatesUnderFiveHours() {

        return rumUpdatesUnderFiveHours;
    }

    public void setRumUpdatesUnderFiveHours(float rumUpdatesUnderFiveHours) {

        this.rumUpdatesUnderFiveHours = rumUpdatesUnderFiveHours;
    }

    public float getRumUpdatesUnderFiveMinutes() {

        return rumUpdatesUnderFiveMinutes;
    }

    public void setRumUpdatesUnderFiveMinutes(float rumUpdatesUnderFiveMinutes) {

        this.rumUpdatesUnderFiveMinutes = rumUpdatesUnderFiveMinutes;
    }

    public float getRumUpdatesUnderOneHour() {

        return rumUpdatesUnderOneHour;
    }

    public void setRumUpdatesUnderOneHour(float rumUpdatesUnderOneHour) {

        this.rumUpdatesUnderOneHour = rumUpdatesUnderOneHour;
    }

    public float getRumUpdatesUnderOneMinute() {

        return rumUpdatesUnderOneMinute;
    }

    public void setRumUpdatesUnderOneMinute(float rumUpdatesUnderOneMinute) {

        this.rumUpdatesUnderOneMinute = rumUpdatesUnderOneMinute;
    }

    public float getRumUpdatesUnderTenMintues() {

        return rumUpdatesUnderTenMintues;
    }

    public void setRumUpdatesUnderTenMintues(float rumUpdatesUnderTenMintues) {

        this.rumUpdatesUnderTenMintues = rumUpdatesUnderTenMintues;
    }

    public float getRumUpdatesUnderTwoHours() {

        return rumUpdatesUnderTwoHours;
    }

    public void setRumUpdatesUnderTwoHours(float rumUpdatesUnderTwoHours) {

        this.rumUpdatesUnderTwoHours = rumUpdatesUnderTwoHours;
    }

    public long getFilesUpdated() {

        return filesUpdated;
    }

    public void setFilesUpdated(long numberOfFileUpdates) {

        this.filesUpdated = numberOfFileUpdates;
    }

    public float getRumUpdatesUnderThirtyMintues() {

        return rumUpdatesUnderThirtyMintues;
    }

    public void setRumUpdatesUnderThirtyMintues(float rumUpdatesUnderThirtyMintues) {

        this.rumUpdatesUnderThirtyMintues = rumUpdatesUnderThirtyMintues;
    }

    public long getTotalRecordsReceived() {

        return totalRecordsReceived;
    }

    public void setTotalRecordsReceived(long totalRecordsReceived) {

        this.totalRecordsReceived = totalRecordsReceived;
    }

    /**
     * Merge (add) statistics from another bean into this bean.
     * @param bean
     * @return
     */
    public RumStatsBean mergeRumStatsBean(RumStatsBean bean) {

        if (this.getCapturedStartTime() == 0 || (bean.getCapturedStartTime() < this.getCapturedStartTime() && bean.getCapturedStartTime() != 0)) {
            this.capturedStartTime = bean.getCapturedStartTime();
        }

        if (this.getCapturedEndTime() == 0 || (bean.getCapturedEndTime() > this.getCapturedEndTime() && bean.getCapturedEndTime() != 0)) {
            this.capturedEndTime = bean.getCapturedEndTime();
        }

        this.numberOfDBUpdates = this.numberOfDBUpdates + bean.getNumberOfDBUpdates();
        this.filesUpdated = this.filesUpdated + bean.getFilesUpdated();
        this.recordsUpdated = this.recordsUpdated + bean.getRecordsUpdated();
        this.totalRecordsReceived = this.totalRecordsReceived + bean.getTotalRecordsReceived();
        this.totalDatabaseElapsedTime = this.totalDatabaseElapsedTime + bean.getTotalDatabaseElapsedTime();
        this.rumUpdatesUnderOneMinute = getCombinedValue(this.rumUpdatesUnderOneMinute, bean.getRumUpdatesUnderOneMinute(), false);
        this.rumUpdatesUnderFiveMinutes = getCombinedValue(this.rumUpdatesUnderFiveMinutes, bean.getRumUpdatesUnderFiveMinutes(), false);
        this.rumUpdatesUnderTenMintues = getCombinedValue(this.rumUpdatesUnderTenMintues, bean.getRumUpdatesUnderTenMintues(), false);
        this.rumUpdatesUnderFifteenMintues = getCombinedValue(this.rumUpdatesUnderFifteenMintues, bean.getRumUpdatesUnderFifteenMintues(), false);
        this.rumUpdatesUnderThirtyMintues = getCombinedValue(this.rumUpdatesUnderThirtyMintues, bean.getRumUpdatesUnderThirtyMintues(), false);
        this.rumUpdatesUnderOneHour = getCombinedValue(this.rumUpdatesUnderOneHour, bean.getRumUpdatesUnderOneHour(), false);
        this.rumUpdatesUnderTwoHours = getCombinedValue(this.rumUpdatesUnderTwoHours, bean.getRumUpdatesUnderTwoHours(), false);
        this.rumUpdatesUnderFiveHours = getCombinedValue(this.rumUpdatesUnderFiveHours, bean.getRumUpdatesUnderFiveHours(), false);
        this.numberOfThreads = getCombinedValue(this.numberOfThreads, bean.getNumberOfThreads(), true);
        this.totalFileElapsedTime = this.totalFileElapsedTime + bean.getTotalFileElapsedTime();
        this.retrieveLocationInfoTime = this.retrieveLocationInfoTime + bean.getRetrieveLocationInfoTime();
        this.sortFileRecordsTime = this.sortFileRecordsTime + bean.getSortFileRecordsTime();
        this.writeOutputFileTime = this.writeOutputFileTime + bean.getWriteOutputFileTime();

        return this;
    }

    private float getCombinedValue(float value1, float value2, boolean averages) {

        if (averages) {
            return (value1 + value2) / 2F;
        }
        else {
            return (value1 + value2);
        }
    }

    private int getCombinedValue(int value1, int value2, boolean averages) {

        if (averages) {
            return (value1 + value2) / 2;
        }
        else {
            return (value1 + value2);
        }
    }

    public long getFilesprocessed() {

        return filesUpdated;
    }

    public void setFilesprocessed(long filesprocessed) {

        this.filesUpdated = filesprocessed;
    }

    public int getNumberOfThreads() {

        return numberOfThreads;
    }

    public void setNumberOfThreads(int numberOfThreads) {

        this.numberOfThreads = numberOfThreads;
    }

    public long getTotalDatabaseElapsedTime() {

        return totalDatabaseElapsedTime;
    }

    public void setTotalDatabaseElapsedTime(long totalDatabaseElapsedTime) {

        this.totalDatabaseElapsedTime = totalDatabaseElapsedTime;
    }

    public long getTotalFileElapsedTime() {

        return totalFileElapsedTime;
    }

    public void setTotalFileElapsedTime(long totalFileElapsedTime) {

        this.totalFileElapsedTime = totalFileElapsedTime;
    }

    public long getCapturedEndTime() {

        return capturedEndTime;
    }

    public void setCapturedEndTime(long capturedEndTime) {

        this.capturedEndTime = capturedEndTime;
    }

    public long getCapturedStartTime() {

        return capturedStartTime;
    }

    public void setCapturedStartTime(long capturedStartTime) {

        this.capturedStartTime = capturedStartTime;
    }

    public long getRecordsUpdated() {

        return recordsUpdated;
    }

    public void setRecordsUpdated(long recordsUpdated) {

        this.recordsUpdated = recordsUpdated;
    }

    public String getFileStatus() {

        return fileStatus;
    }

    public void setFileStatus(String fileStatus) {

        this.fileStatus = fileStatus;
    }

    /**
     * Describe this object.
     * @param indent
     * @return
     */
    public String toStringVerbose() {

        return toStringVerbose("");
    }

    /**
     * Describe this object.
     * @return
     */
    public String toStringVerbose(String indent) {

        StringBuffer s = new StringBuffer();

        s.append(indent + "RumStatsBean" + "\n");

        s.append(indent + "  createDate                    : " + getCreateDate() + "\n");
        s.append(indent + "  createTime                    : " + getCreateTime() + "\n");
        
        s.append(indent + "  numberOfDBUpdates             : " + getNumberOfDBUpdates() + "\n");
        s.append(indent + "  filesUpdated                  : " + getFilesUpdated() + "\n");
        s.append(indent + "  totalRecordsReceived          : " + getTotalRecordsReceived() + "\n");
        s.append(indent + "  recordsUpdated                : " + getRecordsUpdated() + "\n");
        
        s.append(indent + "  totalDatabaseElapsedTime      : " + getTotalDatabaseElapsedTime() + "\n");
        s.append(indent + "  totalFileElapsedTime          : " + getTotalFileElapsedTime() + "\n");
        
        s.append(indent + "  rumUpdatesUnderOneMinute      : " + getRumUpdatesUnderOneMinute() + "\n");
        s.append(indent + "  rumUpdatesUnderFiveMinutes    : " + getRumUpdatesUnderFiveMinutes() + "\n");
        s.append(indent + "  rumUpdatesUnderTenMintues     : " + getRumUpdatesUnderTenMintues() + "\n");
        s.append(indent + "  rumUpdatesUnderFifteenMintues : " + getRumUpdatesUnderFifteenMintues() + "\n");
        s.append(indent + "  rumUpdatesUnderThirtyMintues  : " + getRumUpdatesUnderThirtyMintues() + "\n");
        s.append(indent + "  rumUpdatesUnderOneHour        : " + getRumUpdatesUnderOneHour() + "\n");
        s.append(indent + "  rumUpdatesUnderTwoHours       : " + getRumUpdatesUnderTwoHours() + "\n");
        s.append(indent + "  rumUpdatesUnderFiveHours      : " + getRumUpdatesUnderFiveHours() + "\n");
        
        s.append(indent + "  numberOfThreads               : " + getNumberOfThreads() + "\n");
        
        s.append(indent + "  capturedStartTime             : " + getCapturedStartTime() + "\n");
        s.append(indent + "  capturedEndTime               : " + getCapturedEndTime() + "\n");
        s.append(indent + "  retrieveLocationInfoTime      : " + getRetrieveLocationInfoTime() + "\n");
        s.append(indent + "  writeOutputFileTime           : " + getWriteOutputFileTime() + "\n");
        s.append(indent + "  sortFileRecordsTime           : " + getSortFileRecordsTime() + "\n");
        
        s.append(indent + "  fileName                      : " + getFileName() + "\n");
        s.append(indent + "  fileStatus                    : " + getFileStatus() + "\n");

        s.append(indent + "<<<<" + "\n");

        return s.toString();
    }
}

/*
 *************************************************************
 * Change History:
 *
 * $Log$
 * Revision 1.14  2017/02/21 15:53:29  dtp4395
 * RATES-11849; Heflin; Added toStringVerbose() and organized imports.
 *
 * Revision 1.13  2016/10/27 14:46:47  dtp4395
 * RATES-11849; Heflin; Added comments, added CVS history tag, added copyright.  Formatting only.
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
