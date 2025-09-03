package com.hertz.api.corebusiness.logging;

/**
 * Holds a time and whether it is a Begin or End time stamp.
 *
 */
public class RumStoredProcTimingPoint implements Comparable<RumStoredProcTimingPoint> {

    /** Time point is a Begin event. */
    public static final char BEGIN = 'B';
    /** Time point is an End event. */
    public static final char END = 'E';

    /** Start time of stored procedure call event. */
    long time;

    /** Type of this event: Begin or End */
    char pointType;

    /**
     * Constructor
     * 
     * @param time
     * @param pointType
     */
    public RumStoredProcTimingPoint(long time, char pointType) {

        this.time = time;
        this.pointType = pointType;
    }

    /**
     * Perform a compare between two objects.
     * To make the processing consistent, choose Begin events over End events while sorting.
     * If a Begin causes an increment and End causes a decrement, this will cause processing to avoid negative 'depths'.
     */
    public int compareTo(RumStoredProcTimingPoint o) {

        int result = 0;

        if (this.time < o.time) {
            result = -1;
        }
        else if (this.time == o.time) {
            // Times are equal.
            switch (pointType) {
                case BEGIN:
                    switch (o.pointType) {
                        case BEGIN:
                            result = 0; // Begin / Begin: don't care.
                            break;
                        case END:
                            result = -1; // Begin is before End
                            break;
                    }
                    break;
                    
                case END:
                    switch (o.pointType) {
                        case BEGIN:
                            result = 1; // End / Begin: Begin is before End
                            break;
                        case END:
                            result = 0; // End / End: don't care.
                            break;
                    }
                    break;
            }
        }
        else if (this.time > o.time) {
            result = 1;
        }

        return result;
    }

    public long getTime() {

        return time;
    }

    public char getPointType() {

        return pointType;
    }

    public void setTime(long time) {

        this.time = time;
    }

    public void setPointType(char pointType) {

        this.pointType = pointType;
    }
    
    public String toStringVerbose() {
        
        StringBuffer s = new StringBuffer();
        
        s.append("RumStoredProcTimingPoint: ");
        s.append(" time: " + getTime() + " type: " + getPointType() + "\n");
        
        return s.toString();
    }
}

/*************************************************************
 * Change History:
 *
 * $Log$RumStoredProcTimingPoint.java,v $
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
 * 
 *************************************************************
 */
