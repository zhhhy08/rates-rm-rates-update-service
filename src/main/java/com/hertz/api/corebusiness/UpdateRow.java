package com.hertz.api.corebusiness;

import com.hertz.rates.common.utils.HertzDateTime;
import com.hertz.rates.common.utils.HertzException;
import com.hertz.api.corebusiness.errorcodes.RumErrorCodes;

/**
 * Holds RUM changes from an input 'file row' for a single Date Span/Vehicle for a Plan/Place/Company ID.
 *
 */
public class UpdateRow {

    private String sequenceNumber;
    
    private HertzDateTime captureDateTime;
    private String userId;
    private String planId;
    private String planIdTypeCode;
    private String location;
    private String placeIdCd;
    private String placeTypeCode;
    private String classTimeCode;
    private HertzDateTime startDate;
    private HertzDateTime endDate;
    private String rate;
    private String extraDay;
    private String extraHour;
    private String vehicle;
    
    private String region;
    //added Company ID PTR7024 SR56958
    private String companyId;
    private RumErrorCodes errorCode;

    /**
     * Constructor
     */
    public UpdateRow() {

    }

    /**
     * Constructor
     * @param captureDateTime
     * @param userId
     * @param planId
     * @param planIdTypeCode
     * @param location
     * @param placeTypeCode
     * @param classTimeCode
     * @param startDate
     * @param endDate
     * @param rate
     * @param extraDay
     * @param extraHour
     * @param vehicle
     */
    public UpdateRow(String captureDateTime, String userId, String planId, String planIdTypeCode, String location, String placeTypeCode, String classTimeCode,
            HertzDateTime startDate, HertzDateTime endDate, String rate, String extraDay, String extraHour, String vehicle) {

        try {
            this.captureDateTime = new HertzDateTime(captureDateTime, null);
        }
        catch (HertzException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        this.userId = userId;
        this.planId = planId;
        this.planIdTypeCode = planIdTypeCode;
        this.location = location;
        this.placeTypeCode = placeTypeCode;
        this.classTimeCode = classTimeCode;
        this.startDate = startDate;
        this.endDate = endDate;
        this.rate = rate;
        this.extraDay = extraDay;
        this.extraHour = extraHour;
        this.vehicle = vehicle;
    }

    public String getRegion() {

        return region;
    }

    public void setRegion(String region) {

        this.region = region;
    }

    public String getLocation() {

        return location;
    }

    public void setLocation(String location) {

        this.location = location;
    }

    public HertzDateTime getCaptureDateTime() {

        return captureDateTime;
    }

    public void setCaptureDateTime(HertzDateTime captureDateTime) {

        this.captureDateTime = captureDateTime;
    }

    public String getClassTimeCode() {

        return classTimeCode;
    }

    public void setClassTimeCode(String classTimeCode) {

        this.classTimeCode = classTimeCode;
    }

    public HertzDateTime getEndDate() {

        return endDate;
    }

    public void setEndDate(HertzDateTime endDate) {

        this.endDate = endDate;
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

    public String getPlaceTypeCode() {

        return placeTypeCode;
    }

    public void setPlaceTypeCode(String placeTypeCode) {

        this.placeTypeCode = placeTypeCode;
    }

    public String getPlanId() {

        return planId;
    }

    public void setPlanId(String planId) {

        this.planId = planId;
    }

    public String getPlanIdTypeCode() {

        return planIdTypeCode;
    }

    public void setPlanIdTypeCode(String planIdTypeCode) {

        this.planIdTypeCode = planIdTypeCode;
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

    public String getUserId() {

        return userId;
    }

    public void setUserId(String userId) {

        this.userId = userId;
    }

    public String getVehicle() {

        return vehicle;
    }

    public void setVehicle(String vehicle) {

        this.vehicle = vehicle;
    }

    public String getPlaceIdCd() {

        return placeIdCd;
    }

    public void setPlaceIdCd(String placeIdCd) {

        this.placeIdCd = placeIdCd;
    }

    //added get and set for Company ID PTR7024 SR56958
    public String getCompanyId() {

        return companyId;
    }

    public void setCompanyId(String companyId) {

        this.companyId = companyId;
    }

    public String getSequenceNumber() {

        return sequenceNumber;
    }

    public void setSequenceNumber(String sequenceNumber) {

        this.sequenceNumber = sequenceNumber;
    }

    public RumErrorCodes getErrorCode() {

        return errorCode;
    }

    public void setErrorCode(RumErrorCodes errorCode) {

        this.errorCode = errorCode;
    }

    public boolean hasError() {

        return errorCode != null;
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

        s.append(indent + "UpdateRow:" + "\n");
        
        s.append(indent + "  SequenceNumber  : " + getSequenceNumber() + "\n");
        s.append(indent + "  CaptureTime     : " + getCaptureDateTime() + "\n");
        s.append(indent + "  UserID          : " + getUserId() + "\n");
        s.append(indent + "  PlanID          : " + getPlanId() + "\n");
        s.append(indent + "  PlanIdTypeCode  : " + getPlanIdTypeCode() + "\n");
        s.append(indent + "  location        : " + getLocation() + "\n");
        s.append(indent + "  placeIdCd       : " + getPlaceIdCd() + "\n");
        s.append(indent + "  placeTypeCode   : " + getPlaceTypeCode() + "\n");
        s.append(indent + "  classTimeCode   : " + getClassTimeCode() + "\n");
        s.append(indent + "  startDate       : " + getStartDate() + "\n");
        s.append(indent + "  endDate         : " + getEndDate() + "\n");
        s.append(indent + "  rate            : " + getRate() + "\n");
        s.append(indent + "  extraDay        : " + getExtraDay() + "\n");
        s.append(indent + "  extraHour       : " + getExtraHour() + "\n");
        s.append(indent + "  vehicle         : " + getVehicle() + "\n");
        s.append(indent + "  region          : " + getRegion() + "\n");
        s.append(indent + "  companyId       : " + getCompanyId() + "\n");
        s.append(indent + "  errorCode       : " + getErrorCode() + "\n");
        
        s.append(indent + "<<<<" + "\n");
        
        return s.toString();
    }
}

/*
 *************************************************************
 * Change History:
 *
 * $Log$
 * Revision 1.2  2016/11/08 18:57:10  dtp4395
 * RATES-11849; Heflin; Comment changes and formatting. No code changes.
 *
 * Revision 1.1  2014/08/05 14:54:50  dtc1090
 * SR 59911 - adding new WebService layer to RUM
 *
 * Revision 1.13  2013/09/04 14:44:06  dtc1090
 * Merged DTG branch into HEAD
 *
 * Revision 1.12  2013/06/25 15:08:46  dtp0540
 * DJA - I accidentally committed the company_id changes to the head too soon.  So I'm reverting back to the previous historical modification.
 *
 * Revision 1.10  2012/01/09 19:20:21  dtc1090
 * RATES-5641 - SR52946 - Addendums 1 & 2
 *
 * Revision 1.9  2011/05/19 19:13:48  dtc1090
 * SR 500044.01 (RUM Addendum 1) added the abilty to handle users sending in CCSS in the location field.  Also to write a seperate output file for locked plan/places
 *
 * Revision 1.8  2011/02/28 23:19:00  dtc1090
 * organized imports
 *
 * Revision 1.7  2011/02/28 23:00:37  dtc1090
 * refactored RUM
 *
 * Revision 1.6  2011/02/17 20:24:49  dtc1090
 * added date changes
 *
 * Revision 1.5  2011/01/31 17:33:24  dtc1090
 * Added new code for development
 *
 * Revision 1.4  2011/01/27 20:01:18  dtc1090
 * development add
 *
 * Revision 1.3  2011/01/27 17:00:24  dtc1090
 * Added handling of sequence number
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

