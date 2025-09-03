package com.hertz.api.corebusiness.errorcodes;

import com.hertz.rates.common.utils.EnumTable;
import com.hertz.rates.common.utils.ErrorCategory;
import com.hertz.rates.common.utils.ErrorSystem;
import com.hertz.rates.common.utils.HertzErrorCode;

/**
 * RUM Error Codes
 */
public class RumErrorCodes extends HertzErrorCode {
    
    // enumTable MUST be the 1st static in your enum class!
    private static EnumTable enumTable = new EnumTable(true); // reverse lookups enabled
    private static final long serialVersionUID = 1L;

    public static final RumErrorCodes NO_LOCATION_PROVIDED =
        new RumErrorCodes( "NO_LOCATION_PROVIDED", ErrorSystem.CLIENT_SYSTEM, ErrorCategory.DATA_NOT_FOUND );
    
    public static final RumErrorCodes RUM_UPDATE_FAILED =
        new RumErrorCodes( "RUM_UPDATE_FAILED", ErrorSystem.CLIENT_SYSTEM, ErrorCategory.DATA_NOT_FOUND );
    
    public static final RumErrorCodes NO_VEHICLE_PROVIDED =
        new RumErrorCodes( "NO_VEHICLE_PROVIDED", ErrorSystem.CLIENT_SYSTEM, ErrorCategory.BAD_REQUEST_DATA );
    
    public static final RumErrorCodes NO_REGION_PROVIDED =
        new RumErrorCodes( "NO_NO_REGION_PROVIDED_PROVIDED", ErrorSystem.CLIENT_SYSTEM, ErrorCategory.BAD_REQUEST_DATA );
    
    public static final RumErrorCodes NO_START_DATE_PROVIDED =
        new RumErrorCodes( "NO_START_DATE_PROVIDED", ErrorSystem.CLIENT_SYSTEM, ErrorCategory.BAD_REQUEST_DATA );
    
    public static final RumErrorCodes NO_END_DATE_PROVIDED =
        new RumErrorCodes( "NO_END_DATE_PROVIDED", ErrorSystem.CLIENT_SYSTEM, ErrorCategory.BAD_REQUEST_DATA );
    
    public static final RumErrorCodes END_DATE_INVALID =
        new RumErrorCodes( "END_DATE_INVALID", ErrorSystem.CLIENT_SYSTEM, ErrorCategory.BAD_REQUEST_DATA );
    
    public static final RumErrorCodes START_DATE_INVALID =
        new RumErrorCodes( "START_DATE_INVALID", ErrorSystem.CLIENT_SYSTEM, ErrorCategory.BAD_REQUEST_DATA );
    
    public static final RumErrorCodes NO_RATE_AMOUNT_PROVIDED =
        new RumErrorCodes( "NO_RATE_AMOUNT_PROVIDED", ErrorSystem.CLIENT_SYSTEM, ErrorCategory.BAD_REQUEST_DATA );

    //add company_id PTR7024 SR56958
    public static final RumErrorCodes NO_COMPANY_ID_PROVIDED =
        new RumErrorCodes( "NO_COMPANY_ID_PROVIDED", ErrorSystem.CLIENT_SYSTEM, ErrorCategory.BAD_REQUEST_DATA );
    
    public static final RumErrorCodes INVALID_RATE_AMOUNT =
        new RumErrorCodes( "INVALID_RATE_AMOUNT", ErrorSystem.CLIENT_SYSTEM, ErrorCategory.BAD_REQUEST_DATA );
    
    public static final RumErrorCodes INVALID_EXDD_AMOUNT =
        new RumErrorCodes( "INVALID_EXDD_AMOUNT", ErrorSystem.CLIENT_SYSTEM, ErrorCategory.BAD_REQUEST_DATA );
    
    public static final RumErrorCodes INVALID_EXHH_AMOUNT =
        new RumErrorCodes( "INVALID_EXHH_AMOUNT", ErrorSystem.CLIENT_SYSTEM, ErrorCategory.BAD_REQUEST_DATA );
    
    public static final RumErrorCodes RATE_DOES_NOT_MEET_MINIMUM_VALUE =
        new RumErrorCodes( "RATE_DOES_NOT_MEET_MINIMUM_VALUE", ErrorSystem.CLIENT_SYSTEM, ErrorCategory.BAD_REQUEST_DATA );
    
    public static final RumErrorCodes RATE_EXCEEDS_MAXIMUM_VALUE =
        new RumErrorCodes( "RATE_EXCEEDS_MAXIMUM_VALUE", ErrorSystem.CLIENT_SYSTEM, ErrorCategory.BAD_REQUEST_DATA );
    
    public static final RumErrorCodes CRITICAL_ERROR_DOING_PRICE_CHECKING =
        new RumErrorCodes( "CRITICAL_ERROR_DOING_PRICE_CHECKING", ErrorSystem.CLIENT_SYSTEM, ErrorCategory.BAD_REQUEST_DATA );
    
    public static final RumErrorCodes NO_EXTRA_DAY_PROVIDED =
        new RumErrorCodes( "NO_EXTRA_DAY_PROVIDED", ErrorSystem.CLIENT_SYSTEM, ErrorCategory.DATA_NOT_FOUND );
    
    public static final RumErrorCodes NO_EXTRA_HOUR_PROVIDED =
        new RumErrorCodes( "NO_EXTRA_HOUR_PROVIDED", ErrorSystem.CLIENT_SYSTEM, ErrorCategory.DATA_NOT_FOUND );
    
    public static final RumErrorCodes NO_PLAN_ID_PROVIDED =
        new RumErrorCodes( "NO_PLAN_ID_PROVIDED", ErrorSystem.CLIENT_SYSTEM, ErrorCategory.DATA_NOT_FOUND );
    
    public static final RumErrorCodes NO_PLAN_TYPE_PROVIDED =
        new RumErrorCodes( "NO_PLAN_TYPE_PROVIDED", ErrorSystem.CLIENT_SYSTEM, ErrorCategory.DATA_NOT_FOUND );
    
    public static final RumErrorCodes NO_CLASS_TIME_CODE_PROVIDED =
        new RumErrorCodes( "NO_CLASS_TIME_CODE_PROVIDED", ErrorSystem.CLIENT_SYSTEM, ErrorCategory.DATA_NOT_FOUND );  
    
    public static final RumErrorCodes TOO_MANY_COLUMNS_IN_ROW =
        new RumErrorCodes( "TOO_MANY_COLUMNS_IN_ROW", ErrorSystem.CLIENT_SYSTEM, ErrorCategory.BAD_REQUEST_DATA); 
    
    public static final RumErrorCodes TOO_FEW_COLUMNS_IN_ROW =
        new RumErrorCodes( "TOO_FEW_COLUMNS_IN_ROW", ErrorSystem.CLIENT_SYSTEM, ErrorCategory.BAD_REQUEST_DATA);  
    
    public static final RumErrorCodes NUMBER_OF_FILE_ROWS_EXCEEDED =
        new RumErrorCodes( "NUMBER_OF_FILE_ROWS_EXCEEDED", ErrorSystem.CLIENT_SYSTEM, ErrorCategory.BAD_REQUEST_DATA);   
    
    public static final RumErrorCodes STORED_PROC_TIMING_IS_INVALID =
            new RumErrorCodes( "STORED_PROC_TIMING_IS_INVALID", ErrorSystem.CLIENT_SYSTEM, ErrorCategory.CODING_BUG);   

    public static final RumErrorCodes GENERIC_ERROR =
        new RumErrorCodes( "GENERIC_ERROR", ErrorSystem.CLIENT_SYSTEM, ErrorCategory.UNEXPECTED_PROBLEM);   
 
    //    public static final RumErrorCodes MULTIPLE_PLACES_IN_WEB_SERVICE_CALL =
    //        new RumErrorCodes( "MULTIPLE_PLACES_IN_WEB_SERVICE_CALL", ErrorSystem.CLIENT_SYSTEM, ErrorCategory.DOES_NOT_QUALIFY);  
        
    //    public static final RumErrorCodes MULTIPLE_PLANS_IN_WEB_SERVICE_CALL =
    //        new RumErrorCodes( "MULTIPLE_PLANS_IN_WEB_SERVICE_CALL", ErrorSystem.CLIENT_SYSTEM, ErrorCategory.DOES_NOT_QUALIFY);
    
    public static final RumErrorCodes MULTIPLE_COMPANY_IDS_IN_WEB_SERVICE_CALL =
        new RumErrorCodes( "MULTIPLE_COMPANY_IDS_IN_WEB_SERVICE_CALL", ErrorSystem.CLIENT_SYSTEM, ErrorCategory.DOES_NOT_QUALIFY);  
    
    /**
     * Constructor
     * @param codeID
     * @param sys
     * @param cat
     */
    public RumErrorCodes(String codeID, ErrorSystem sys, ErrorCategory cat) {

        super(codeID, sys, cat);
        enumTable.addReverseLookupEnum(codeID, this);
    }

    protected EnumTable getEnumTable() {

        return enumTable;
    }

    public static RumErrorCodes findByDBSpecificCode(String dbSpecificCode) {

        return (RumErrorCodes) enumTable.findReverseLookupEnum(dbSpecificCode);
    }
}

/*
 *************************************************************
 * Change History:
 *
 * $Log$
 * Revision 1.18  2016/11/22 21:52:43  dtp4395
 * RATES-11849; Heflin; Commented Webservices errors that will now be obsolete about multiple places and plans.
 *
 * Revision 1.17  2016/11/02 21:14:37  dtp4395
 * RATES-11849; Heflin; Moved comment, added comment. No code changes.
 *
 * Revision 1.16  2016/10/31 21:36:47  dtp4395
 * RATES-11849; Heflin; Added comments, CVS history tag, added copyright.  Formatting only.
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
