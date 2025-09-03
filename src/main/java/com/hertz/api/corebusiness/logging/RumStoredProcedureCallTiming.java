package com.hertz.api.corebusiness.logging;

/**
 * Holds the start and end time of a single stored procedure call within a single Web Services call.
 * @author jwheflin
 *
 */
public class RumStoredProcedureCallTiming {

    /** Start time of stored procedure call. */
    private long start;

    /** End time of stored procedure call. */
    private long end;

    /**
     * Constructor
     * 
     * @param start
     * @param end
     */
    public RumStoredProcedureCallTiming(long start, long end) {

        this.start = start;
        this.end = end;
    }

    public long getStart() {

        return start;
    }

    public long getEnd() {

        return end;
    }

    public void setStart(long start) {

        this.start = start;
    }

    public void setEnd(long end) {

        this.end = end;
    }
}

/*************************************************************
 * Change History:
 *
 * $Log$RumStoredProcedureCallTiming.java,v $
 *
 *************************************************************
 *
 * Copyright (C) 2017 The Hertz Corporation
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
