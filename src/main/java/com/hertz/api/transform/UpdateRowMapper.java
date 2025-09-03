package com.hertz.api.transform;

import java.util.ArrayList;

import com.hertz.rates.common.utils.Decimal;
import com.hertz.rates.common.utils.FastStringTokenizer;
import com.hertz.rates.common.utils.HertzDateTime;
import com.hertz.rates.common.utils.HertzException;
import com.hertz.rates.common.utils.logging.HertzLogger;
import com.hertz.api.corebusiness.UpdateRow;
import com.hertz.api.corebusiness.errorcodes.RumErrorCodes;

/**
 * RUM - Parse input file of rate updates.
 * 
 *
 */
public class UpdateRowMapper {

    private static char DELIMITER = ',';
    private static int DEFAULT_SEQ_NUMBER = 999999;
    private static String ERROR_AREA_LOCATION = "9999999";
    private final static String STATE_IN_TYPE = "State";
    private final static String STATE_DB_PLACE_TYPE = "6";
    
    //PTR7024 SR56958 new column for Company ID
    private final static int NUMBER_OF_COLUMNS_TO_EXPECT = 14;
    
    private final static String DAY_CLASS = "DY";
    private final static String WEEKEND_CLASS = "WE";
    private final static String WEEK_CLASS = "WK";
    private final static String MONTH_CLASS = "MO";
    
    private static Decimal NA_DAY_AND_WEEKEND_MIN_RATE = null;
    private static Decimal NA_DAY_AND_WEEKEND_MAX_RATE = null;
    private static Decimal NA_WEEK_MIN_RATE = null;
    private static Decimal NA_WEEK_MAX_RATE = null;
    private static Decimal NA_MONTH_MIN_RATE = null;
    private static Decimal NA_MONTH_MAX_RATE = null;
    private static Decimal DEFAULT_DAY_AND_WEEKEND_MIN_RATE = null;
    private static Decimal DEFAULT_DAY_AND_WEEKEND_MAX_RATE = null;
    private static Decimal DEFAULT_WEEK_MIN_RATE = null;
    private static Decimal DEFAULT_WEEK_MAX_RATE = null;
    private static Decimal DEFAULT_MONTH_MIN_RATE = null;
    private static Decimal DEFAULT_MONTH_MAX_RATE = null;
    private static Decimal EUROPE_DAY_AND_WEEKEND_MIN_RATE = null;
    private static Decimal EUROPE_DAY_AND_WEEKEND_MAX_RATE = null;
    private static Decimal EUROPE_WEEK_MIN_RATE = null;
    private static Decimal EUROPE_WEEK_MAX_RATE = null;
    private static Decimal EUROPE_MONTH_MIN_RATE = null;
    private static Decimal EUROPE_MONTH_MAX_RATE = null;

    private static ArrayList<String> europeanRegions = new ArrayList<String>(4);
    private static ArrayList<String> northAmericanRegions = new ArrayList<String>(1);

    private final static String EUROPE_REGION = "EU";
    private final static String MIDDLE_EAST_REGION = "ME";
    private final static String AFRICA_REGION = "AA";
    private final static String ASIA_PACIFIC = "AP";
    
    // private final static String CARIBBEAN = "CC";
    //private final static String LATIN_AMERICA = "LA";
    private final static String NORTH_AMERICA = "NA";

    private final static HertzLogger logger = new HertzLogger(UpdateRowMapper.class);

    static {
        try {
        	
        	//These are the min/max values for all other regions NA 
            // ----------------------------
            // North America Region limits
            // ----------------------------
            NA_DAY_AND_WEEKEND_MIN_RATE = new Decimal("6.00");
            NA_DAY_AND_WEEKEND_MAX_RATE = new Decimal("10000.00");
            NA_WEEK_MIN_RATE = new Decimal("50.00");
            NA_WEEK_MAX_RATE = new Decimal("100000.00");
            NA_MONTH_MIN_RATE = new Decimal("200.00");
            NA_MONTH_MAX_RATE = new Decimal("100000.00");
            
        	//These are the min/max values for all other regions LA,CC(Caucasus)
            // ----------------------------
            // Non-'European' Region limits
            // ----------------------------
            DEFAULT_DAY_AND_WEEKEND_MIN_RATE = new Decimal("6.00");
            DEFAULT_DAY_AND_WEEKEND_MAX_RATE = new Decimal("10000.00");
            DEFAULT_WEEK_MIN_RATE = new Decimal("50.00");
            DEFAULT_WEEK_MAX_RATE = new Decimal("10000.00");
            DEFAULT_MONTH_MIN_RATE = new Decimal("200.00");
            DEFAULT_MONTH_MAX_RATE = new Decimal("10000.00");
            
            // --------------------------------------------------
            // 'European' Region(s) (Europe + Middle East + Africa + Asia)
            // --------------------------------------------------
           
            EUROPE_DAY_AND_WEEKEND_MIN_RATE = new Decimal("0.01");
            
            // RATES-12638 - increased 'Europe' Max Day and Weekend Rate from 10,000 to 999,999. 
            //EUROPE_DAY_AND_WEEKEND_MAX_RATE = new Decimal("10000.00");
            EUROPE_DAY_AND_WEEKEND_MAX_RATE = new Decimal("999999.00");
            

            EUROPE_WEEK_MIN_RATE = new Decimal("0.01");
            
            // RATES-12638 - increased 'Europe' Max Weekly Rate from 10,000 to 999,999.
            //EUROPE_WEEK_MAX_RATE = new Decimal("10000.00");
            EUROPE_WEEK_MAX_RATE = new Decimal("999999.00");
            
            
            EUROPE_MONTH_MIN_RATE = new Decimal("0.01");
            
            // Rates-12578 - increased 'Europe' Max Monthly Rate from 10,000 to 999,999.
            //EUROPE_MONTH_MAX_RATE = new Decimal("10000.00");
            EUROPE_MONTH_MAX_RATE = new Decimal("999999.00");
        }
        catch (HertzException e) {
            String stackTrace = HertzException.buildStackTrace(e);
            logger.error(stackTrace);
        }

        europeanRegions.add(EUROPE_REGION);
        europeanRegions.add(MIDDLE_EAST_REGION);
        europeanRegions.add(AFRICA_REGION);
        europeanRegions.add(ASIA_PACIFIC);
        
        northAmericanRegions.add(NORTH_AMERICA);
    }

    /**
     * Constructor
     */
    public UpdateRowMapper() {

    }

    /**
     * Parse an update line into an UpdateRow object.
     * @param line
     * @param count
     * @return UpdateRow
     */
    public UpdateRow convertLineToObject(String line, int count) {

        FastStringTokenizer stringTokenizer = new FastStringTokenizer(line, DELIMITER);

        int tokenCount = 0;
        int tokenTotal = 0;

        UpdateRow row = new UpdateRow();
        
        // Check that we received exactly the correct number of fields.
        tokenTotal = stringTokenizer.countTokens();
        if (tokenTotal > NUMBER_OF_COLUMNS_TO_EXPECT) {
            row.setErrorCode(RumErrorCodes.TOO_MANY_COLUMNS_IN_ROW);
            row.setLocation(ERROR_AREA_LOCATION);
        }
        else if (tokenTotal < NUMBER_OF_COLUMNS_TO_EXPECT) {
            row.setErrorCode(RumErrorCodes.TOO_FEW_COLUMNS_IN_ROW);
            row.setLocation(ERROR_AREA_LOCATION);
        }

        // Process, validate, and convert the fields in the update.
        while (stringTokenizer.hasMoreTokens()) {
            String elementValue = stringTokenizer.nextToken();
            
            switch (tokenCount) {
                case 0:
                    // Sequence Number
                    //we will set the sequence number
                    //they should still send though.
                    int time = 0;
                    try {
                        time = HertzDateTime.getCurrentDateTime().getTimeAsInt(true);
                    }
                    catch (HertzException e) {
                        time = DEFAULT_SEQ_NUMBER;
                    }
                    time = time + count;
                    row.setSequenceNumber(String.valueOf(time));
                    break;
                
                case 1:
                    // Company ID
                    //new case for Company ID PTR7024 SR56958
                    row.setCompanyId(elementValue);
                    if (elementValue == null || elementValue.length() <= 0) {
                        row.setErrorCode(RumErrorCodes.NO_COMPANY_ID_PROVIDED);
                    }
                    break;
                
                case 2:
                    //  Location
                    if (elementValue.length() <= 0) {
                        row.setErrorCode(RumErrorCodes.NO_LOCATION_PROVIDED);
                    }
                    row.setLocation(elementValue);
                    break;
                
                case 3:
                    // Place Type Code
                    if (elementValue != null) {
                        if (STATE_IN_TYPE.equals(elementValue.trim())) {
                            elementValue = STATE_DB_PLACE_TYPE;
                        }
                    }
                    row.setPlaceTypeCode(elementValue);
                    break;
                
                case 4:
                    // Region
                    row.setRegion(elementValue);
                    //                  if (elementValue == null || elementValue.length() <= 0){
                    //                      row.setErrorCode(RumErrorCodes.NO_REGION_PROVIDED);
                    //                  }
                    break;
                
                case 5:
                    // Vehicle
                    row.setVehicle(elementValue);
                    if (elementValue == null || elementValue.length() <= 0) {
                        row.setErrorCode(RumErrorCodes.NO_VEHICLE_PROVIDED);
                    }
                    break;
                
                case 6:
                    // Plan ID
                    row.setPlanId(elementValue);
                    if (elementValue == null || elementValue.length() <= 0) {
                        row.setErrorCode(RumErrorCodes.NO_PLAN_ID_PROVIDED);
                    }
                    break;
                
                case 7:
                    // Plan ID Type Code
                    row.setPlanIdTypeCode(elementValue);
                    if (elementValue == null || elementValue.length() <= 0) {
                        row.setErrorCode(RumErrorCodes.NO_PLAN_TYPE_PROVIDED);
                    }
                    break;
                
                case 8:
                    //  Class Time Code = Classification
                    row.setClassTimeCode(elementValue);
                    if (elementValue == null || elementValue.length() <= 0) {
                        row.setErrorCode(RumErrorCodes.NO_CLASS_TIME_CODE_PROVIDED);
                    }
                    break;
                
                case 9:
                    //  Start Date
                    if (elementValue == null || elementValue.length() <= 0) {
                        row.setErrorCode(RumErrorCodes.NO_START_DATE_PROVIDED);
                    }
                    else {
                        try {
                            HertzDateTime startDateTime = new HertzDateTime(elementValue, HertzDateTime.DATE_FORMAT);

                            if (!doDateCheck(startDateTime)) {
                                row.setStartDate(startDateTime);
                                throw new HertzException(RumErrorCodes.START_DATE_INVALID);
                            }
                            else {
                                row.setStartDate(startDateTime);
                            }
                        }
                        catch (Exception e) {
                            row.setErrorCode(RumErrorCodes.START_DATE_INVALID);
                        }
                    }
                    break;
                
                case 10:
                    // End Date
                    if (elementValue == null || elementValue.length() <= 0) {
                        row.setErrorCode(RumErrorCodes.NO_END_DATE_PROVIDED);
                    }
                    else {
                        try {
                            HertzDateTime endDateTime = new HertzDateTime(elementValue, HertzDateTime.DATE_FORMAT);

                            if (!doDateCheck(endDateTime)) {
                                row.setEndDate(endDateTime);
                                throw new HertzException(RumErrorCodes.END_DATE_INVALID);
                            }
                            else if (row.getStartDate() != null && row.getStartDate().getHertzSystemDate() <= endDateTime.getHertzSystemDate()) {
                                row.setEndDate(endDateTime);
                            }
                            else {

                                row.setEndDate(endDateTime);
                                if (!row.hasError()) {
                                    row.setErrorCode(RumErrorCodes.END_DATE_INVALID);
                                }
                            }
                        }
                        catch (Exception e) {
                            row.setErrorCode(RumErrorCodes.END_DATE_INVALID);
                        }
                    }
                    break;
                
                case 11:
                    // Rate Amount
                    row.setRate(elementValue);
                    if (elementValue == null || elementValue.length() <= 0) {
                        row.setErrorCode(RumErrorCodes.NO_RATE_AMOUNT_PROVIDED);
                    }

                    try {
                        Decimal rateAmount = new Decimal(elementValue.trim());
                        if (rateAmount.lessThanOrEqualZero()) {
                            row.setErrorCode(RumErrorCodes.NO_RATE_AMOUNT_PROVIDED);
                        }
                    }
                    catch (Exception e) {
                        row.setErrorCode(RumErrorCodes.INVALID_RATE_AMOUNT);
                    }
                    break;

                case 12:
                    // Extra Day
                    row.setExtraDay(elementValue);
                    if (elementValue == null || elementValue.length() <= 0) {
                        row.setErrorCode(RumErrorCodes.NO_EXTRA_DAY_PROVIDED);
                    }
                    else {
                        try {
                            Decimal exdd = new Decimal(elementValue.trim());
                            //PTR10804 - we must stop an update from being sent to the database when the extra day rate is zero and the rate class is not
                            //daily or weekend. The reason is because in that scenario, the number is used as a divisor and causes an exception which is 
                            //inconveniently written to the error log and all of the input file's updates are aborted for the location.
                            if (exdd.lessThanOrEqualZero() && !row.getClassTimeCode().equals(DAY_CLASS) && !row.getClassTimeCode().equals(WEEKEND_CLASS)) {
                                row.setErrorCode(RumErrorCodes.NO_EXTRA_DAY_PROVIDED);
                            }
                        }
                        catch (Exception e) {
                            row.setErrorCode(RumErrorCodes.INVALID_EXDD_AMOUNT);
                        }
                    }
                    break;

                case 13:
                    //  Extra Hour
                    row.setExtraHour(elementValue);
                    if (elementValue == null || elementValue.length() <= 0) {
                        row.setErrorCode(RumErrorCodes.NO_EXTRA_HOUR_PROVIDED);
                    }
                    else {
                        try {
                            Decimal exhh = new Decimal(elementValue.trim());
                            //PTR10804 - we must stop an update from being sent to the database when the extra hour rate is zero. The reason is because
                            //the number is always used as a divisor and causes an exception which is inconveniently written to the error log and all of
                            //the input file's updates are aborted for the location.
                            if (exhh.lessThanOrEqualZero()) {
                                row.setErrorCode(RumErrorCodes.NO_EXTRA_HOUR_PROVIDED);
                            }
                        }
                        catch (Exception e) {
                            row.setErrorCode(RumErrorCodes.INVALID_EXHH_AMOUNT);
                        }
                    }
                    break;
            }
            
            // Move to the next field.
            tokenCount++;
        }

        try {
            // Do price checking validation based on region.
            String region = row.getRegion();
            if (region != null && northAmericanRegions.contains(region.trim())) {
            	// NA region 
                row = doPriceChecking(row, 
                        NA_DAY_AND_WEEKEND_MIN_RATE, NA_DAY_AND_WEEKEND_MAX_RATE, 
                        NA_WEEK_MIN_RATE, NA_WEEK_MAX_RATE,
                        NA_MONTH_MIN_RATE, NA_MONTH_MAX_RATE);
            }else if (region == null || !europeanRegions.contains(region.trim())) {
                // No region or Not European  or NA region
                row = doPriceChecking(row, 
                        DEFAULT_DAY_AND_WEEKEND_MIN_RATE, DEFAULT_DAY_AND_WEEKEND_MAX_RATE, 
                        DEFAULT_WEEK_MIN_RATE, DEFAULT_WEEK_MAX_RATE,
                        DEFAULT_MONTH_MIN_RATE, DEFAULT_MONTH_MAX_RATE);
            }
            else {
                // European region
                row = doPriceChecking(row, 
                        EUROPE_DAY_AND_WEEKEND_MIN_RATE, EUROPE_DAY_AND_WEEKEND_MAX_RATE, 
                        EUROPE_WEEK_MIN_RATE, EUROPE_WEEK_MAX_RATE, 
                        EUROPE_MONTH_MIN_RATE, EUROPE_MONTH_MAX_RATE);
            }
        }
        catch (HertzException e) {
            String stackTrace = HertzException.buildStackTrace(e);
            logger.error(stackTrace);
            if (row != null) {
                row.setErrorCode(RumErrorCodes.CRITICAL_ERROR_DOING_PRICE_CHECKING);
            }
        }

        return row;
    }

    /**
     * SR 52946 - Addendum 1 requires that a rate being update must meet minimum and maximum 
     * standards.  This method is doing the checking for rate classification (ie. DY, WE, WK or MO) 
     * with a predetermined min/max value.
     * 
     * @param row
     * @param minDaily
     * @param maxDaily
     * @param minWeek
     * @param maxWeek
     * @param minMonth
     * @param maxMonth
     * @return UpdateFileRow
     * @throws HertzException
     */
    private static UpdateRow doPriceChecking(UpdateRow row, Decimal minDaily, Decimal maxDaily, Decimal minWeek, Decimal maxWeek, Decimal minMonth, Decimal maxMonth)
            throws HertzException {

        if (row != null) {
            if (row.getErrorCode() == null) {
                String classification = row.getClassTimeCode();
                if (row.getRate() != null) {
                    Decimal rate = new Decimal(row.getRate().trim());
                    if (DAY_CLASS.equals(classification) || WEEKEND_CLASS.equals(classification)) {
                        if (rate.lessThan(minDaily)) {
                            row.setErrorCode(RumErrorCodes.RATE_DOES_NOT_MEET_MINIMUM_VALUE);
                        }
                        else if (rate.greaterThan(maxDaily)) {
                            row.setErrorCode(RumErrorCodes.RATE_EXCEEDS_MAXIMUM_VALUE);
                        }
                    }
                    else if (WEEK_CLASS.equals(classification)) {
                        if (rate.lessThan(minWeek)) {
                            row.setErrorCode(RumErrorCodes.RATE_DOES_NOT_MEET_MINIMUM_VALUE);
                        }
                        else if (rate.greaterThan(maxWeek)) {
                            row.setErrorCode(RumErrorCodes.RATE_EXCEEDS_MAXIMUM_VALUE);
                        }
                    }
                    else if (MONTH_CLASS.equals(classification)) {
                        if (rate.lessThan(minMonth)) {
                            row.setErrorCode(RumErrorCodes.RATE_DOES_NOT_MEET_MINIMUM_VALUE);
                        }
                        else if (rate.greaterThan(maxMonth)) {
                            row.setErrorCode(RumErrorCodes.RATE_EXCEEDS_MAXIMUM_VALUE);
                        }
                    }
                }
            }
        }

        return row;
    }

    /**
     * Perform sanity checks on a HertzDateTime.
     * @param dateTime
     * @return
     */
    private static boolean doDateCheck(HertzDateTime dateTime) {

        if (dateTime != null) {
            if (dateTime.getYear() < 1000) {
                return false;
            }

            if (dateTime.getDay() > 31) {
                return false;
            }

            if (dateTime.getMonth() > 12) {
                return false;
            }
        }

        return true;
    }
}

/*
 *************************************************************
 * Change History:
 *
 * $Log$
 * Revision 1.8  2018/08/10 20:40:07  dtp4395
 * RATES-12578; Heflin; Changed EUROPE_MONTH_MAX_RATE from 10000 to 999999, added comments, make two methods static.
 *
 * Revision 1.7  2017/01/09 17:55:03  dtp4395
 * RATES-11849; Heflin; Added comments and formatted only.
 *
 * Revision 1.6  2016/11/08 18:49:52  dtp4395
 * RATES-11849; Heflin; Added comments. No code changes.
 *
 * Revision 1.5  2016/11/01 16:34:17  dtp4395
 * RATES-11849; Heflin; Added comments. No code changes.
 *
 * Revision 1.4  2016/10/27 15:00:44  dtp4395
 * RATES-11849; Heflin; Added comments.  Added generic type to array.  Formatting.
 *
 * Revision 1.3  2015/09/03 15:05:49  dtp0540
 * RATES-10804 log an error in the output file for rows that have a zero in the extra hour divisor (any rate) and rows that have a zero in the extra day amount for WK and MO rates.  This will prevent our logging an error in the error_log table when we try to divide by zero in PROCESS_RUM_UPDATE.  I also removed the 'default' logic that was put in this class for when we added company id.  It isn't needed any more.
 *
 * Revision 1.2  2014/09/29 13:52:34  dtc1090
 * Checked in new code for DTAG Update Webservice
 *
 * Revision 1.1  2014/08/05 14:54:50  dtc1090
 * SR 59911 - adding new WebService layer to RUM
 *
 * Revision 1.20  2014/06/25 15:50:15  dtc1090
 * Made error code for exceeding max rate have a better description
 *
 * Revision 1.19  2013/09/04 14:44:06  dtc1090
 * Merged DTG branch into HEAD
 *
 * Revision 1.18  2013/06/25 15:08:46  dtp0540
 * DJA - I accidentally committed the company_id changes to the head too soon.  So I'm reverting back to the previous historical modification.
 *
 * Revision 1.16  2012/01/09 19:20:21  dtc1090
 * RATES-5641 - SR52946 - Addendums 1 & 2
 *
 * Revision 1.15  2011/08/30 15:54:32  dtc1090
 * RATES-5385 - fixed no output file being created when invalid data is sent in
 *
 * Revision 1.14  2011/08/30 14:16:37  dtc1090
 * RATES-5491 - need to send back error when start/end date is invalid
 *
 * Revision 1.13  2011/05/19 19:13:48  dtc1090
 * SR 500044.01 (RUM Addendum 1) added the abilty to handle users sending in CCSS in the location field.  Also to write a seperate output file for locked plan/places
 *
 * Revision 1.12  2011/03/18 17:22:41  dtc1090
 * merged changes from qual branch
 *
 * Revision 1.11.2.1  2011/03/18 17:11:51  dtc1090
 * Added code to make sure there is a rate amount greater than zero
 *
 * Revision 1.11  2011/03/09 17:56:43  dtc1090
 * merged from qual branch
 *
 * Revision 1.8.4.1  2011/03/08 17:14:49  dtc1090
 * added error checking
 *
 * Revision 1.8  2011/02/25 21:06:57  dtc1090
 * added code to handle invalid dates
 *
 * Revision 1.7  2011/02/17 20:24:49  dtc1090
 * added date changes
 *
 * Revision 1.6  2011/01/31 20:54:44  dtc1090
 * continued development
 *
 * Revision 1.5  2011/01/31 17:33:24  dtc1090
 * Added new code for development
 *
 * Revision 1.4  2011/01/27 20:01:18  dtc1090
 * development add
 *
 * Revision 1.3  2011/01/27 16:59:41  dtc1090
 * added sequence number processing
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