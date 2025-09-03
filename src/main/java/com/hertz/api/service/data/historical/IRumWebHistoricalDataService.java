package com.hertz.api.service.data.historical;

import com.hertz.rates.common.service.data.IDataServiceWithProperties;
import com.hertz.api.corebusiness.RumUpdateGroup;
import com.hertz.api.corebusiness.logging.RumWebStatsBean;

public interface IRumWebHistoricalDataService extends IDataServiceWithProperties {

	public void insertRumWebTransactionHistory(RumWebStatsBean bean, RumUpdateGroup group);
	
	public RumWebStatsBean[] getWebHistoricalData(int hertzSystemDate);

}


/*
 *************************************************************
 * Change History:
 *
 * $Log$
 *
 *************************************************************
 *
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
