package com.hertz.api.corebusiness;

import java.util.ArrayList;
import java.util.Iterator;

import com.hertz.rates.common.utils.HertzDateTime;
import com.hertz.api.corebusiness.errorcodes.RumErrorCodes;

/**
 * Holds RUM Updates in a file for one or more date spans of a single Place/Plan/Company ID.
 * Contains all changes to its Date Span/Vehicles.
 *
 */
public class RumUpdateGroup {

    private String fileName;
    
    private String webTransactionId;
    
    private String location;
    private String planId;
    private String planType;
    private String placeIdCd;
    private String placeTypeCode;
    
    //added Company ID PTR7024 SR56958
    private String companyId;
    private String classTimeCode;
    
    /** List of changes: each for a single Date Span/Vehicle */
    private ArrayList<RumChangeDetails> changeDetails;
    
    private String timeToPurge;
    private boolean processed = false;

    /**
     * Constructor
     * @param fileName
     */
    public RumUpdateGroup(String fileName) {

        this.fileName = fileName;
    }

    public String getFileName() {

        return fileName;
    }

    public void setFileName(String fileName) {
        
        this.fileName = fileName;
    }

    public boolean isProcessed() {

        return processed;
    }

    public void setProcessed(boolean threadCreated) {

        this.processed = threadCreated;
    }

    public String getLocation() {

        return location;
    }

    public void setLocation(String location) {

        this.location = location;
    }

    public ArrayList<RumChangeDetails> getChangeDetails() {

        return changeDetails;
    }

    public void addChangeDetail(RumChangeDetails details) {

        if (changeDetails == null) {
            changeDetails = new ArrayList<RumChangeDetails>();
        }

        changeDetails.add(details);
    }

    public String getPlaceIdCdToUse() {

        if (placeIdCd != null) {
            return placeIdCd.trim();
        }
        else {
            return location;
        }
    }

    public String getPlaceIdCd() {

        return placeIdCd;
    }

    public void setPlaceIdCd(String placeIdCd) {

        this.placeIdCd = placeIdCd;
    }

    public String getPlaceTypeCode() {

        return placeTypeCode;
    }

    public void setPlaceTypeCode(String placeTypeCode) {

        this.placeTypeCode = placeTypeCode;
    }

    //added get and set for Company ID PTR7024 SR56958
    public String getCompanyId() {

        return companyId;
    }

    public void setCompanyId(String companyId) {

        this.companyId = companyId;
    }

    public String getPlanId() {

        return planId;
    }

    public void setPlanId(String planId) {

        this.planId = planId;
    }

    public void addErrorCodeToDetails(RumErrorCodes code) {

        if (changeDetails != null) {
            Iterator<RumChangeDetails> changeIter = changeDetails.iterator();
            while (changeIter.hasNext()) {
                RumChangeDetails details = changeIter.next();
                if (details.getErrorCode() == null) {
                    details.setErrorCode(code);
                    details.setResponseMessage("FAILED");
                }
            }
        }
    }

    public void addErrorMessageToDetails(Exception exception) {

        if (changeDetails != null) {
            Iterator<RumChangeDetails> changeIter = changeDetails.iterator();
            while (changeIter.hasNext()) {
                RumChangeDetails details = changeIter.next();
                String errorMessage = exception.getMessage();
                if (errorMessage.indexOf("locked") >= 0) {
                    details.setLocked(true);
                }
                details.setException(exception);
                details.setResponseMessage("FAILED");
            }
        }
    }

    public String getPlanType() {

        return planType;
    }

    public void setPlanType(String planType) {

        this.planType = planType;
    }

    public String getClassTimeCode() {

        return classTimeCode;
    }

    public void setClassTimeCode(String classTimeCode) {

        this.classTimeCode = classTimeCode;
    }

    public RumChangeDetails getRumChangeDetailByVehicleCode(String vehicleCode) {

        if (this.changeDetails != null) {
            Iterator<RumChangeDetails> iter = changeDetails.iterator();
            while (iter.hasNext()) {
                RumChangeDetails details = iter.next();
                if (details.getVehicle().equals(vehicleCode) && details.getResponseMessage() == null) {
                    return details;
                }
            }
        }

        return null;
    }

    public RumChangeDetails getRumChangeDetailByLOKandVehicle(String vehicleCode, int startDate, int endDate) {

        if (this.changeDetails != null) {
            Iterator<RumChangeDetails> iter = changeDetails.iterator();
            while (iter.hasNext()) {
                RumChangeDetails details = iter.next();
                if (details.getResponseMessage() == null && details.getVehicle().equals(vehicleCode)) {

                    HertzDateTime detailStartDate = details.getStartDate();
                    HertzDateTime detailEndDate = details.getEndDate();

                    if (detailStartDate != null && detailEndDate != null) {
                        int startSystemDate = detailStartDate.getHertzSystemDate();
                        int endSystemDate = detailEndDate.getHertzSystemDate();
                        if (startSystemDate == startDate && endSystemDate == endDate) {
                            return details;
                        }
                    }
                }
            }
        }

        return null;
    }

    public String getTimeToPurge() {

        return timeToPurge;
    }

    public void setTimeToPurge(String timeToPurge) {

        this.timeToPurge = timeToPurge;
    }

    /**
     * Convert the changes into an array of String arrays.
     * @return
     */
    public String[][] getDetailsArray() {

        Iterator<RumChangeDetails> iterator = changeDetails.iterator();

        int recordCount = 0;
        //RATES-11372 - noticed that a update was being done with null as parameters.
        int sizeOfArray = getNumberOfNonErroredDetails();

        String[][] updateArray = null;

        while (iterator.hasNext()) {

            RumChangeDetails details = iterator.next();
            if (details.getException() == null && details.getErrorCode() == null) {

                if (updateArray == null) {
                    updateArray = new String[sizeOfArray][7];
                }

                updateArray[recordCount][0] = details.getSequenceNumber();
                updateArray[recordCount][1] = details.getStartDate().toStringDateOnly();
                updateArray[recordCount][2] = details.getEndDate().toStringDateOnly();
                updateArray[recordCount][3] = details.getVehicle();
                updateArray[recordCount][4] = details.getRate();
                updateArray[recordCount][5] = details.getExtraDay();
                updateArray[recordCount][6] = details.getExtraHour();

                recordCount++;
                //createChangesDelimitedList(changesBuffer, details);  
            }
        }

        return updateArray;
    }

    private int getNumberOfNonErroredDetails() {

        int numberOfValid = 0;
        if (changeDetails != null) {

            Iterator<RumChangeDetails> iterator = changeDetails.iterator();
            while (iterator.hasNext()) {
                RumChangeDetails details = iterator.next();
                if (details.getException() == null && details.getErrorCode() == null) {
                    numberOfValid++;
                }
            }
        }

        return numberOfValid;
    }

    public String getWebTransactionId() {

        return webTransactionId;
    }

    public void setWebTransactionId(String webTransactionId) {

        this.webTransactionId = webTransactionId;
    }
    
    /**
     * Describe this object.
     * @return
     */
    public String toStringVerbose() {

        return toStringVerbose("");
    }
    
    /**
     * Return true if this is a File Update.
     * @return
     */
    public boolean isAFileUpdate() {
        
        boolean isFileChange = (getFileName() != null);
        return isFileChange;
    }
    
    /**
     * Describe this object.
     * @return
     */
    public String toStringVerbose(String indent) {
        
        StringBuffer s = new StringBuffer();

        s.append(indent + "RumUpdateGroup" + "\n");
        
        s.append(indent + "  FileName          : " + getFileName() + "\n");
        s.append(indent + "  WebTransactionId  : " + getWebTransactionId() + "\n");
        s.append(indent + "  Location          : " + getLocation() + "\n");
        s.append(indent + "  PlanId            : " + getPlanId() + "\n");
        s.append(indent + "  PlanType          : " + getPlanType() + "\n");
        s.append(indent + "  PlaceIdCd         : " + getPlaceIdCd() + "\n");
        s.append(indent + "  PlaceTypeCode     : " + getPlaceTypeCode() + "\n");
        
        s.append(indent + "  CompanyId         : " + getCompanyId() + "\n");
        s.append(indent + "  TimeCode          : " + getClassTimeCode() + "\n");
        
        /** List of changes: each for a single Date Span/Vehicle */
        s.append("  ChangeDetails     : ");
        if (changeDetails == null) {
            s.append("<null>" + "\n");
        }
        else {
            Iterator<RumChangeDetails> i = changeDetails.iterator();
            while (i.hasNext()) {
                RumChangeDetails rumChangeDetails = i.next();
                s.append(rumChangeDetails.toStringVerbose(indent + indent) + "\n");
            }
        }
        s.append("\n");
        
        s.append(indent + "  TimeToPurge       : " + getTimeToPurge() + "\n");
        s.append(indent + "  isProcessed       : " + isProcessed() + "\n");

        s.append(indent + "<<<<" + "\n");
        
        return s.toString();
    }
}

/*
 *************************************************************
 * Change History:
 *
 * $Log$
 * Revision 1.20  2017/08/18 18:56:32  dtp4395
 * RATES-12061; Heflin; Comment and Formatting changes only.
 *
 * Revision 1.19  2017/03/24 18:13:07  dtp4395
 * RATES-12061; Heflin; Fixed logging spelling.
 *
 * Revision 1.18  2017/03/08 07:05:00  dtp4395
 * RATES-11849; Heflin; Added setFileName() to support Web Service testing from Files.
 *
 * Revision 1.17  2017/02/21 17:05:30  dtp4395
 * RATES-11849; Heflin; Made small changes to toStringVerbose().
 *
 * Revision 1.16  2017/01/16 22:22:36  dtp4395
 * RATES-11849; Heflin; Fixed toStringVerbose().
 *
 * Revision 1.15  2017/01/10 19:40:47  dtp4395
 * RATES-11849; Added toStringVerbose();
 *
 * Revision 1.14  2016/11/08 18:59:53  dtp4395
 * RATES-11849; Heflin; Changed comments. No code changes.
 *
 * Revision 1.13  2016/10/27 15:24:27  dtp4395
 * RATES-11849; Heflin; Added/fixed comments.  Formatted.  No code changes.
 *
 * Revision 1.12  2016/10/27 14:47:19  dtp4395
 * RATES-11849; Heflin; Added comments, added CVS history tag, added copyright.  Formatting only.
 *
 *************************************************************
 *
 * Copyright (C) 2004 The Hertz Corporation
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
