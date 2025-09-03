package com.hertz.api.corebusiness;

/**
 * Bean to hold a RUM update's place
 * 
 *
 */
public class RumLocationInfo {

    private String areaLocation;
    private String placeIdCode;
    private String placeTypeCode;
    private Exception exception;

    /**
     * Constructor
     * @param areaLocation
     * @param placeIdCode
     * @param placeTypeCode
     * @param exception
     */
    public RumLocationInfo(String areaLocation, String placeIdCode, String placeTypeCode, Exception exception) {

        this.areaLocation = areaLocation;
        this.placeIdCode = placeIdCode;
        this.placeTypeCode = placeTypeCode;
        this.exception = exception;
    }

    public Exception getException() {

        return exception;
    }

    public void setException(Exception exception) {

        this.exception = exception;
    }

    public String getPlaceIdCode() {

        return placeIdCode;
    }

    public void setPlaceIdCode(String placeIdCode) {

        this.placeIdCode = placeIdCode;
    }

    public String getAreaLocation() {

        return areaLocation;
    }

    public void setAreaLocation(String areaLocation) {

        this.areaLocation = areaLocation;
    }

    /**
     * Compare - using Area/Locations only.
     * @param areaLocation
     * @return
     */
    public boolean equals(String areaLocation) {

        if (areaLocation != null) {
            return areaLocation.equals(this.areaLocation);
        }

        return false;
    }

    public String getPlaceTypeCode() {

        return placeTypeCode;
    }

    public void setPlaceTypeCode(String placeTypeCode) {

        this.placeTypeCode = placeTypeCode;
    }
}

/*
 *************************************************************
 * Change History:
 *
 * $Log$
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