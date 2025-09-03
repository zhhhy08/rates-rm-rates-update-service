package com.hertz.api.corebusiness;

import com.hertz.rates.common.utils.HertzDateTime;
import com.hertz.api.corebusiness.errorcodes.RumErrorCodes;

/**
 * Bean that holds RUM changes for a single Date Span/Vehicle of a Plan/Place.
 */
public class RumChangeDetails {

    private String sequenceNumber;
    
    private HertzDateTime startDate;
    private HertzDateTime endDate;
    
    private String rate;
    private String extraDay;
    private String extraHour;
    private String vehicle;
    
    // Results of the change
    private RumErrorCodes errorCode;
    private Exception exception;
    private String responseMessage;
    private String failureNumber;
    private boolean locked;
    
    private String region;

    /**
     * Constructor
     */
    public String getRegion() {

        return region;
    }

    public void setRegion(String region) {

        this.region = region;
    }

    public HertzDateTime getEndDate() {

        return endDate;
    }

    public void setEndDate(HertzDateTime endDate) {

        this.endDate = endDate;
    }

    public RumErrorCodes getErrorCode() {

        return errorCode;
    }

    public void setErrorCode(RumErrorCodes errorCode) {

        this.errorCode = errorCode;
    }

    public String getExtraDay() {

        return extraDay;
    }

    public void setExtraDay(String extraDay) {

        this.extraDay = extraDay;
    }

    public String getExtraHour() {

        return extraHour;
    }

    public void setExtraHour(String extraHour) {

        this.extraHour = extraHour;
    }

    public String getRate() {

        return rate;
    }

    public void setRate(String rate) {

        this.rate = rate;
    }

    public HertzDateTime getStartDate() {

        return startDate;
    }

    public void setStartDate(HertzDateTime startDate) {

        this.startDate = startDate;
    }

    public String getVehicle() {

        return vehicle;
    }

    public void setVehicle(String vehicle) {

        this.vehicle = vehicle;
    }

    public Exception getException() {

        return exception;
    }

    public void setException(Exception exception) {

        this.exception = exception;
    }

    public String getResponseMessage() {

        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {

        this.responseMessage = responseMessage;
    }

    public String getSequenceNumber() {

        return sequenceNumber;
    }

    public void setSequenceNumber(String sequenceNumber) {

        this.sequenceNumber = sequenceNumber;
    }

    public String getFailureNumber() {

        return failureNumber;
    }

    public void setFailureNumber(String failureNumber) {

        this.failureNumber = failureNumber;
    }

    public boolean isLocked() {

        return locked;
    }

    public void setLocked(boolean locked) {

        this.locked = locked;
    }

    /**
     * Describe this object.
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

        s.append(indent + "RumChangeDetails:" + "\n");
        
        s.append(indent + "  SequenceNumber  : " + getSequenceNumber() + "\n");
        s.append(indent + "  StartDate       : " + getStartDate() + "\n");
        s.append(indent + "  EndDate         : " + getEndDate() + "\n");
        s.append(indent + "  Rate            : " + getRate() + "\n");
        s.append(indent + "  ExtraDay        : " + getExtraDay() + "\n");
        s.append(indent + "  ExtraHour       : " + getExtraHour() + "\n");
        s.append(indent + "  Vehicle         : " + getVehicle() + "\n");
        s.append(indent + "  ErrorCode       : " + getErrorCode() + "\n");
        s.append(indent + "  Exception       : " + getException() + "\n");
        s.append(indent + "  ResponseMessage : " + getResponseMessage() + "\n");
        s.append(indent + "  FailureNumber   : " + getFailureNumber() + "\n");
        s.append(indent + "  Locked          : " + isLocked() + "\n");
        s.append(indent + "  Region          : " + getRegion() + "\n");
        
        s.append(indent + "<<<<" + "\n");
        
        return s.toString();
    }
}

/*
 *************************************************************
 * Change History:
 *
 * $Log$
 * Revision 1.9  2017/01/10 21:30:08  dtp4395
 * RATES-11849; Organized imports.
 *
 * Revision 1.8  2017/01/10 19:40:08  dtp4395
 * RATES-11849; TESTING - Added testing code.
 *
 * Revision 1.7  2016/11/28 19:50:25  dtp4395
 * RATES-11849; Heflin; Added comment only.
 *
 * Revision 1.6  2016/11/08 18:57:51  dtp4395
 * RATES-11849; Heflin; Comment changes and formatting. No code changes.
 *
 * Revision 1.5  2016/10/27 15:14:44  dtp4395
 * RATES-11849; Heflin; Added/fixed comments.  Moved CVS, copyright comments. Formatting only.
 *
 * Revision 1.4  2016/10/27 14:45:27  dtp4395
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
