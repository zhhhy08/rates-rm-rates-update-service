package com.hertz.api.service.data.update;

import java.sql.SQLException;

import com.hertz.rates.common.utils.HertzException;
import com.hertz.rates.common.utils.config.PropertyGroup;
import com.hertz.rates.common.utils.logging.HertzLogger;
import com.hertz.api.helpers.RumPurgeMessageHelper;

public class RumPurgeMessagesDataService implements IRumPurgeMessagesDataService {

    final static HertzLogger logger = new HertzLogger(RumPurgeMessagesDataService.class);

    // The config data properties for this data service (passed in by the DataServiceLocator)
    // This isn't really state, just read-only config data
    private PropertyGroup configDataProperties = null;

    public void setProperties(PropertyGroup properties) {

        this.configDataProperties = properties;
    }

    public void doPurgeOfRumMessagesTable(String capturedDateTime) {

        RumPurgeMessageHelper helper = new RumPurgeMessageHelper(configDataProperties);
        try {
            helper.doDBCall(capturedDateTime);
        }
        catch (HertzException e) {
            logger.info("Purge Failed" + e.getMessage());
        }
        catch (SQLException e) {
            logger.info("Purge Failed" + e.getMessage());
        }
    }

}

/*
 *************************************************************
 * Change History:
 *
 * $Log$
 *
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