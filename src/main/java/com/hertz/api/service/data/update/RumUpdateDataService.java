package com.hertz.api.service.data.update;

import java.sql.SQLException;

import com.hertz.rates.common.utils.HertzException;
import com.hertz.rates.common.utils.config.PropertyGroup;
import com.hertz.rates.common.utils.logging.HertzLogger;
import com.hertz.api.corebusiness.RumUpdateGroup;
import com.hertz.api.helpers.RumUpdateHelper;

/**
 * RUM Web Service Update service
 * 
 *
 */
public class RumUpdateDataService implements IRumUpdateDataService {

    final static HertzLogger logger = new HertzLogger(RumUpdateDataService.class);

    // The config data properties for this data service (passed in by the DataServiceLocator)
    // This isn't really state, just read-only config data
    private PropertyGroup configDataProperties = null;

    /**
     * This is called by the DateServiceLocator with my config data property group.
     * 
     * @see com.hertz.rates.common.service.data.IDataServiceWithProperties#setProperties(com.hertz.rates.common.utils.config.PropertyGroup)
     */
    public void setProperties(PropertyGroup properties) {

        this.configDataProperties = properties;
    }

    /**
     * Update for a Place/Plan/Company ID and return the results.
     * 
     * @param updateGroup
     */
    public RumUpdateGroup doRumUpdate(RumUpdateGroup updateGroup) {

        RumUpdateHelper helper = new RumUpdateHelper(configDataProperties);

        try {
            //            if (UpdateDriver.isRunWithNoDb()) {
            //                // RATES-11849 FAKE
            //                updateGroup = helper.doFakeRumUpdate(updateGroup);
            //            }
            //            else {
            updateGroup = helper.doRumUpdate(updateGroup);
            //            }
        }
        catch (HertzException e) {
            // Log the error.
            try {
                helper.logDBCall(updateGroup, false);
            }
            catch (HertzException e1) {
                logger.error(e1.getMessage());
            }
            
            updateGroup.addErrorMessageToDetails(e);
        }
        catch (SQLException e) {
            try {
                helper.logDBCall(updateGroup, false);
            }
            catch (HertzException e1) {
                logger.error(e1.getMessage());
            }
            
            updateGroup.addErrorMessageToDetails(e);
        }

        return updateGroup;
    }
}

/*
 *************************************************************
 * Change History:
 *
 * $Log$
 * Revision 1.10  2017/01/16 22:31:42  dtp4395
 * RATES-11849; Heflin; Added comment for debugging fake path.
 *
 * Revision 1.9  2017/01/10 19:31:51  dtp4395
 * RATES-11849; TESTING - Added RUM Update DB call bypass.
 *
 * Revision 1.8  2016/11/08 18:54:13  dtp4395
 * RATES-11849; Heflin; Comment changes and formatting. No code changes.
 *
 * Revision 1.7  2016/10/27 14:56:53  dtp4395
 * RATES-11849; Heflin; Added comments.  Formatting only.
 *
 * Revision 1.6  2012/08/20 18:03:31  dtc1090
 * SR-54472 - RUM Processing Changes
 *
 * Revision 1.5  2011/03/08 02:07:52  dtc1090
 * Added code for reporting errors from DB
 *
 * Revision 1.4  2011/03/07 15:08:39  dtc1090
 * Modified to code to not log as much
 *
 * Revision 1.3  2011/02/28 23:00:38  dtc1090
 * refactored RUM
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