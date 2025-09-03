package com.hertz.api.corebusiness;

import java.util.ArrayList;
import java.util.Iterator;

import com.hertz.rates.common.utils.logging.HertzLogger;

/**
 * Thread-safe list of RUM locations.  This implements a cache of places identified by key of their Area Location.
 * 
 *
 */
public class AreaLocationsToPlaceIdCodeList {

    private final static HertzLogger logger = new HertzLogger(AreaLocationsToPlaceIdCodeList.class);

    /** List of RUM Location information objects */
    private ArrayList<RumLocationInfo> locations;

    
    public ArrayList<RumLocationInfo> getLocations() {

        return locations;
    }

    /**
     * Add RUM Location information to the list.
     * @param info
     */
    public synchronized void addLocation(RumLocationInfo info) {

        try {
            if (locations == null) {
                locations = new ArrayList<RumLocationInfo>();
            }

            if (!isLocationInList(info.getAreaLocation())) {
                locations.add(info);
            }
        }
        catch (Exception e) {
            logger.debug("Error loading cache", e);
        }
    }

    /**
     * Return true if this Area Location is cached.
     * @param areaLocation
     * @return
     */
    public synchronized boolean isLocationInList(String areaLocation) {

        try {
            if (locations != null) {
                Iterator<RumLocationInfo> iter = locations.iterator();
                while (iter.hasNext()) {
                    RumLocationInfo infoFromList = iter.next();
                    if (infoFromList.equals(areaLocation)) {
                        return true;
                    }
                }
            }
        }
        catch (Exception e) {
            logger.debug("Error loading cache", e);
            return false;
        }

        return false;
    }

    /**
     * Return the RUM Location information for the specified Area Location.
     * @param areaLocationNumber
     * @return
     */
    public synchronized RumLocationInfo getRumLocationInfo(String areaLocationNumber) {

        try {
            if (locations != null) {
                Iterator<RumLocationInfo> iter = locations.iterator();
                while (iter.hasNext()) {
                    RumLocationInfo infoFromList = iter.next();
                    if (infoFromList.equals(areaLocationNumber)) {
                        return infoFromList;
                    }
                }
            }
        }
        catch (Exception e) {
            logger.debug("Error loading cache", e);
            return null;

        }
        return null;
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
