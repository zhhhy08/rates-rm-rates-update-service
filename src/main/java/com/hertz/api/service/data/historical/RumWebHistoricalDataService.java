package com.hertz.api.service.data.historical;

import java.sql.SQLException;

import com.hertz.rates.common.errorcodes.CommonErrorCodes;
import com.hertz.rates.common.utils.HertzException;
import com.hertz.rates.common.utils.config.PropertyGroup;
import com.hertz.rates.common.utils.logging.HertzLogger;
import com.hertz.rates.common.utils.logging.LogLevel;
import com.hertz.api.corebusiness.RumUpdateGroup;
import com.hertz.api.corebusiness.logging.RumWebStatsBean;
import com.hertz.api.helpers.GetRumWebDataHelper;
import com.hertz.api.helpers.InsertRumWebDataHelper;

/**
 * History Logging Service for File-based and Web Services.
 * 
 *
 */
public class RumWebHistoricalDataService implements IRumWebHistoricalDataService {

    final static HertzLogger logger = new HertzLogger(RumWebHistoricalDataService.class);

    // The config data properties for this data service (passed in by the DataServiceLocator)
    // This isn't really state, just read-only config data
    private PropertyGroup configDataProperties = null;

    @Override
    public void insertRumWebTransactionHistory(RumWebStatsBean bean, RumUpdateGroup group) {

        InsertRumWebDataHelper helper = new InsertRumWebDataHelper(configDataProperties);
        try {
        	logger.info("insertRumWebTransactionHistory");
            helper.insertRumWebTransactionHistory(bean, group);
        }
        catch (HertzException e) {
            HertzException.eatAndLogNonCriticalException(LogLevel.ERROR, CommonErrorCodes.UNEXPECTED_ERR, e, "error writing trans history");
        }
        catch (SQLException e) {
            HertzException.eatAndLogNonCriticalException(LogLevel.ERROR, CommonErrorCodes.UNEXPECTED_ERR, e, "error writing trans history");
        }
    }

    public void setProperties(PropertyGroup properties) {

        this.configDataProperties = properties;
    }

    @Override
    public RumWebStatsBean[] getWebHistoricalData(int hertzSystemDate) {

        GetRumWebDataHelper helper = new GetRumWebDataHelper(configDataProperties);
        RumWebStatsBean[] beans = null;

        try {
            beans = helper.getHistoricalWebData(hertzSystemDate);
        }
        catch (HertzException e) {
            HertzException.eatAndLogNonCriticalException(LogLevel.ERROR, CommonErrorCodes.UNEXPECTED_ERR, e, "error writing trans history");
        }
        catch (SQLException e) {
            HertzException.eatAndLogNonCriticalException(LogLevel.ERROR, CommonErrorCodes.UNEXPECTED_ERR, e, "error writing trans history");
        }
        return beans;
    }
}

/*
 *************************************************************
 * Change History:
 *
 * $Log$
 * Revision 1.1  2014/09/29 13:52:34  dtc1090
 * Checked in new code for DTAG Update Webservice
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
 *************************************************************
 */
