package com.hertz.api.service.data.update;

import java.sql.SQLException;

import com.hertz.rates.common.utils.HertzException;
import com.hertz.rates.common.utils.config.PropertyGroup;
import com.hertz.api.corebusiness.RumUpdateGroup;
import com.hertz.api.helpers.GetPlaceIdCodeHelper;

public class RumGetPlaceTypeIdCodeDataService implements IRumGetPlaceTypeIdCodeDataService {

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

    public RumUpdateGroup getPlaceIdTypeCode(RumUpdateGroup updateGroup) throws HertzException, SQLException {

        GetPlaceIdCodeHelper helper = new GetPlaceIdCodeHelper(configDataProperties);

        return helper.doDBCall(updateGroup);
    }
}

/*
 *************************************************************
 * Change History:
 *
 * $Log$
 * Revision 1.6  2017/02/21 16:03:44  dtp4395
 * RATES-11849; Heflin; Commented test code out.
 *
 * Revision 1.5  2017/01/16 22:31:03  dtp4395
 * RATES-11849; Heflin; Added comment for debugging fake path.
 *
 * Revision 1.4  2017/01/10 19:32:51  dtp4395
 * RATES-11849; TESTING - Added Place lookup DB call bypass.
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