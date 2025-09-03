package com.hertz.api.corebusiness.logging;

import org.apache.log4j.bridge.LogEventAdapter;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.logging.log4j.message.ObjectMessage;

import com.hertz.rates.common.utils.logging.ElapsedTime;
import com.hertz.rates.common.utils.logging.EntryExit;
import com.hertz.rates.common.utils.logging.HertzLogger;

/**
 * RUM Performance Filter - Computes RUM Webservices performance statistics.
 * 
 *
 */
public class RumWebPerformanceFilter extends Filter {
	
    final static HertzLogger logger = new HertzLogger(RumWebPerformanceFilter.class);

    @Override
    public int decide(LoggingEvent loggingEvent) {
    	
    	LogEventAdapter logEventAdapter = (LogEventAdapter) loggingEvent;
        int decision = org.apache.log4j.spi.Filter.DENY;   // Default to denying everything.
        if (loggingEvent.getLevel().toInt() == org.apache.log4j.Priority.INFO.toInt()) {
            // We are logging INFO level items.
            RumWebStats stats = RumWebStats.getRumWebStatsObject();
            Object message = logEventAdapter.getEvent().getMessage();
            if (message != null) {
            	Object msgObj;
            	if(message instanceof ObjectMessage) {
            		msgObj = ((ObjectMessage) message).getParameter();
            	} else {
            		msgObj = message;
            	}
            	EntryExit ee;
            	if( msgObj.getClass().equals(com.hertz.rates.common.utils.logging.EntryExit.class)) {
            		ee = (EntryExit) msgObj;
            		// Should we process this event?
                    decision = stats.denyOrAcceptElapsedTimeEvent(ee, logEventAdapter.getEvent().getLoggerName());
                    if (decision == org.apache.log4j.spi.Filter.ACCEPT) {
                        if (ee.getEnd() > 0 && ee.getStart() > 0) {
                            // This is an EXIT call since both times are non-zero.
                            ElapsedTime et = new ElapsedTime(
                                                ee.getMethodName(), 
                                                new Long(ee.getEnd() - ee.getStart()), 
                                                new Long(ee.getEnd()), 
                                                ee.getMethodName());
                            stats.addElapsedTime(et);
                        }
                    }
                    else {
                        // Should we process this 'other' event? -- @@JWH This method denies everything so this code does nothing.
                        decision = stats.denyOrAcceptOtherEvent(ee, logEventAdapter.getEvent().getLoggerName());
                        if (decision == org.apache.log4j.spi.Filter.ACCEPT) {
                            // JWH: THIS CODE IS NEVER REACHED
                            if (ee.getEnd() > 0 && ee.getStart() > 0) {
                                // This is an EXIT call since both times are non-zero.
                                ElapsedTime et = new ElapsedTime(
                                                    ee.getMethodName(), 
                                                    new Long(ee.getEnd() - ee.getStart()), 
                                                    new Long(ee.getEnd()), 
                                                    ee.getMethodName());
                                stats.addElapsedTime(et);
                            }
                        }
                    }
            	}
            }
        }

        return decision;
    }
}

/*
 *************************************************************
 * Change History:
 *
 * $Log$
 * Revision 1.4  2017/01/10 05:16:33  dtp4395
 * RATES-11849; Added comments.
 *
 * Revision 1.3  2017/01/09 17:58:53  dtp4395
 * RATES-11849; Heflin; Added comment about why code section is duplicated.
 *
 * Revision 1.2  2016/11/01 16:21:10  dtp4395
 * RATES-11849; Heflin; Added comments.  Formatted.  No code changes.
 *
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
 * 
 *************************************************************
 */
