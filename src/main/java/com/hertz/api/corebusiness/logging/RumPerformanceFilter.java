package com.hertz.api.corebusiness.logging;

import org.apache.log4j.bridge.LogEventAdapter;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.logging.log4j.message.ObjectMessage;

import com.hertz.rates.common.utils.logging.ElapsedTime;
import com.hertz.rates.common.utils.logging.EntryExit;


/**
 * RUM Performance Filter - Computes performance statistics.
 * 
 *
 */
public class RumPerformanceFilter extends Filter {

	 @Override
	    public int decide(LoggingEvent loggingEvent) {
	    	
	    	LogEventAdapter logEventAdapter = (LogEventAdapter) loggingEvent;
	        int decision = org.apache.log4j.spi.Filter.DENY;   // Default to denying everything.
	        if (loggingEvent.getLevel().toInt() == org.apache.log4j.Priority.INFO.toInt()) {
	            // We are logging INFO level items.
	        	RumStats stats = RumStats.getRumStatsObject();
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
 * Revision 1.3  2017/01/10 05:12:41  dtp4395
 * RATES-11849; Added comments.
 *
 * Revision 1.2  2016/11/01 16:19:45  dtp4395
 * RATES-11849; Heflin; Added comments, CVS history tag, added copyright.  Formatting only.
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

