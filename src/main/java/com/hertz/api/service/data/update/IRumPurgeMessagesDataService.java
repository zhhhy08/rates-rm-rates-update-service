package com.hertz.api.service.data.update;

import com.hertz.rates.common.service.data.IDataServiceWithProperties;

public interface IRumPurgeMessagesDataService extends IDataServiceWithProperties {
	
	public void doPurgeOfRumMessagesTable(String caputuredDateTime);

}
