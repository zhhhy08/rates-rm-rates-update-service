package com.hertz.api.service.data.update;

import com.hertz.rates.common.service.data.IDataServiceWithProperties;
import com.hertz.api.corebusiness.RumUpdateGroup;

/**
 * RUM Data Services Interface
 *
 */
public interface IRumUpdateDataService extends IDataServiceWithProperties {
	
	public RumUpdateGroup doRumUpdate(RumUpdateGroup updateGroup);

}

/*
 *************************************************************
 * Change History:
 *
 * $Log$
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
 *
 *************************************************************
 */