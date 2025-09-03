package com.hertz.api.transform;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Iterator;

import com.hertz.rates.common.utils.FastStringTokenizer;
import com.hertz.rates.common.utils.HertzErrorCode;
import com.hertz.rates.common.utils.HertzException;
import com.hertz.rates.common.utils.StringUtils;
import com.hertz.rates.common.utils.config.ConfigData;
import com.hertz.rates.common.utils.config.PropertyGroup;
import com.hertz.rates.common.utils.logging.HertzLogger;
import com.hertz.api.corebusiness.RumChangeDetails;
import com.hertz.api.corebusiness.RumUpdateGroup;
import com.hertz.api.corebusiness.UpdateList;
import com.hertz.api.corebusiness.errorcodes.RumErrorCodes;

/**
 * This Class handles the writing of response file to a specified directory.
 * 
 * @author Clint Hedrick
 *
 */
public class OutputFileWriter {

    private final static HertzLogger logger = new HertzLogger(OutputFileWriter.class);

    private final static String OUTPUT_DIRECTORY_CONFIG = "OutputDirectory";
    private final static String CSV_EXTENSION = ".csv";
    
    private ConfigData cfgData = null;
    
    private final static String INPUT_STR = "INPUT";
    private final static String OUTPUT_STR = "OUTPUT";
    private final static String ERROR_OUTPUT_STR = "OUTPUT_ERROR";
    private final static char ERROR_MESSAGE_DELIMITER = ':';

    /**
     * Constructor for OutputFileWriter.  Will create the FileOutputStream and PrintWriter via
     * inputs configured in the config properties.
     */
    public OutputFileWriter() {

        try {
            cfgData = ConfigData.getInstance();

        }
        catch (HertzException e1) {
            logger.info("OutputFile not written due to " + e1.getMessage());
        }
    }

    public void writeOutput(UpdateList updateList, String inputFileName, String fileError) {

        // Write the regular output file
        writeToOuputFile(updateList, inputFileName, OUTPUT_STR, false, fileError);
        // Write the errored output file
        writeToOuputFile(updateList, inputFileName, ERROR_OUTPUT_STR, true, fileError);
    }

    /**
     * This method will write the output file.
     * 
     * @param ArrayList responses
     */
    private void writeToOuputFile(UpdateList updateList, String inputFileName, String filePrefix, boolean errorsOnly, String fileError) {

        //logger.entry(HertzLogger.INFO, "writeToOuputFile");

        // RATES-12737 - A case was observed Mar 2017 where 'updateList' was null and a null pointer exception occurred.  Log this and just return.
        // Return if there we are directed to log errors but there is a null list of errors.
        if (!errorsOnly) {
            // 'updateList' will be used during reporting so can not be null.
            if (updateList == null) {
                logger.warn("OutputFile for " + inputFileName + " " + filePrefix + " not errors only - not written due to a null pointer to the Update List - case A");
                return;  // Return - no resources left open.
            }
        }
        else {
            // errorsOnly == true
            // 'updateList' is used in a test for errors and then also during reporting later and can not be null.
            if (updateList == null) {
                logger.warn("OutputFile for " + inputFileName + " " + filePrefix + " errors only - not written due to a null pointer to the Update List - case B");
                return;  // Return - no resources left open.
            }
        }
        
        // If not the errored output or the errored output and there are errors.
        if (!errorsOnly || (errorsOnly && updateList.hasErrors())) {

            PrintWriter printWriter = null;
            FileOutputStream fileOutputStream = null;
            try {
                PropertyGroup group = cfgData.getGroup(OUTPUT_DIRECTORY_CONFIG);
                String outputDirectory = group.getPropertyValue("dir");

                int inputStart = inputFileName.indexOf(INPUT_STR);
                String partOfFileNameToUse = inputFileName.substring(inputStart + 5);
                StringBuffer fileNameBuffer = new StringBuffer(outputDirectory);
                fileNameBuffer.append(filePrefix);
                fileNameBuffer.append(partOfFileNameToUse);

                if (fileNameBuffer.indexOf(CSV_EXTENSION) < 0) {
                    fileNameBuffer.append(CSV_EXTENSION);
                }

                try {
                    fileOutputStream = new FileOutputStream(fileNameBuffer.toString());
                }
                catch (FileNotFoundException e) {
                    logger.info("OutputFile for " + inputFileName + "not written due to " + e.getMessage());
                }

                if (fileOutputStream != null) {
                    printWriter = new PrintWriter(fileOutputStream, true);
                }

                if (printWriter != null) {
                    if (fileError != null) {
                        printWriter.println(fileError);
                    }
                    else {
                        for (int i = 0; i < updateList.getListOfUpdates().size(); i++) {
                            writeFromResponseList(printWriter, (RumUpdateGroup) updateList.getListOfUpdates().get(i), errorsOnly);
                        }
                    }
                }
            }
            finally {
                if (printWriter != null) {
                    printWriter.close();
                }

                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        //  logger.exit(HertzLogger.INFO, "writeToOuputFile");
    }

    private void writeFromResponseList(PrintWriter printWriter, RumUpdateGroup updateGroup, boolean errorsOnly) {

        if (updateGroup != null && updateGroup.getChangeDetails() != null) {
            Iterator<RumChangeDetails> iter = updateGroup.getChangeDetails().iterator();
            while (iter.hasNext()) {
                String lineToWrite = getLineToWrite(updateGroup, iter.next(), errorsOnly);
                if (lineToWrite != null) {
                    printWriter.println(lineToWrite);
                }
            }
        }
    }

    /**
     * This method will transform the RumUpdateResponse into a line that can be written to 
     * a file.
     * @param RumUpdateResponse response
     * @return
     */
    private String getLineToWrite(RumUpdateGroup group, RumChangeDetails details, boolean errorsOnly) {

        if (!errorsOnly || (errorsOnly && details.isLocked())) {
            StringBuffer lineBuffer = new StringBuffer();
            
            // Sequence Number
            lineBuffer.append(details.getSequenceNumber());
            lineBuffer.append(",");
            
            // Location
            lineBuffer.append(group.getLocation());
            lineBuffer.append(",");
            
            // Place Type
            lineBuffer.append(group.getPlaceTypeCode());
            lineBuffer.append(",");
            
            // Company ID
            lineBuffer.append(group.getCompanyId());        //PTR7024 added company id
            lineBuffer.append(",");
            
            // Region
            lineBuffer.append(details.getRegion());
            lineBuffer.append(",");
            
            // Vehicle
            lineBuffer.append(details.getVehicle());
            lineBuffer.append(",");
            
            // Plan ID
            lineBuffer.append(group.getPlanId());
            lineBuffer.append(",");
            
            // Plan Type
            lineBuffer.append(group.getPlanType());
            lineBuffer.append(",");
            
            // Classification
            lineBuffer.append(group.getClassTimeCode());
            lineBuffer.append(",");

            // Start Date
            if (details.getStartDate() != null) {
                lineBuffer.append(details.getStartDate().toStringDateOnly());
            }
            else {
                lineBuffer.append("null");
            }
            lineBuffer.append(",");

            // End Date
            if (details.getEndDate() != null) {
                lineBuffer.append(details.getEndDate().toStringDateOnly());
            }
            else {
                lineBuffer.append("null");
            }
            lineBuffer.append(",");

            // Rate Amount
            lineBuffer.append(details.getRate());
            lineBuffer.append(",");
            
            // Extra Day
            lineBuffer.append(details.getExtraDay());
            lineBuffer.append(",");
            
            // Extra Hour
            lineBuffer.append(details.getExtraHour());
            lineBuffer.append(",");
            
            // Response (Result) Message
            lineBuffer.append(details.getResponseMessage());
            lineBuffer.append(",");


            // Is there an exception ?
            Exception e = details.getException();
            if (e != null) {
                // Exception Details
                if (e instanceof HertzException) {
                    HertzException hrtzExecption = (HertzException) e;
                    HertzErrorCode errorCode = hrtzExecption.getErrorCode();
                    if (errorCode instanceof RumErrorCodes) {
                        lineBuffer.append(errorCode.getCodeID());
                    }
                    else {
                        doNonHertzExceptionProcessing(lineBuffer, e);
                    }
                }
                else {
                    doNonHertzExceptionProcessing(lineBuffer, e);
                }
            }
            
            return lineBuffer.toString();
        }

        return null;
    }

    public static void doNonHertzExceptionProcessing(StringBuffer lineBuffer, Exception e) {

        FastStringTokenizer stringTokenizer = new FastStringTokenizer(e.getMessage(), ERROR_MESSAGE_DELIMITER);
        
        int count = 1;
        while (stringTokenizer.hasMoreTokens()) {
            String errorMessage = (String) stringTokenizer.nextToken();
            try {
                errorMessage = StringUtils.removeLinefeeds(errorMessage);
            }
            catch (HertzException e1) {
                logger.info("Error.. Exception processing unsuccessful for " + e.getMessage());
            }

            if (e instanceof SQLException) {
                if (count == 1) {
                    lineBuffer.append((String) errorMessage);
                }
                if (count == 2) {
                    lineBuffer.append((String) errorMessage);
                }
            }
            else {
                if (count == 4) {
                    lineBuffer.append((String) errorMessage);
                }
                if (count == 6) {
                    lineBuffer.append((String) errorMessage);
                }

                //RATES-8073 - adding user id to pending error message.
                if (count == 7) {
                    lineBuffer.append((String) errorMessage);
                    lineBuffer.append(" : ");
                }

                if (count == 8) {
                    int indexOfOra = errorMessage.indexOf("ORA");
                    if (indexOfOra >= 0) {
                        lineBuffer.append((String) errorMessage.substring(0, indexOfOra));
                    }
                    else {
                        lineBuffer.append((String) errorMessage);
                    }

                }
            }
            count++;
        }
    }
}

/*
 *************************************************************
 * Change History:
 *
 * $Log$
 * Revision 1.12  2017/02/01 20:27:02  dtp4395
 * RATES-11849; Heflin; Added generic type in: writeFromResponseList().
 *
 * Revision 1.11  2016/11/01 19:31:54  dtp4395
 * RATES-11849; Heflin; Added comments. No code changes.
 *
 * Revision 1.10  2016/10/31 20:43:33  dtp4395
 * RATES-11849; Heflin; Added/fixed comments.  Formatted.  No code changes.
 *
 * Revision 1.9  2014/10/23 15:30:28  dtp7094
 * RATES-8073 - Added user id to pending error message.
 *
 * Revision 1.8  2014/08/05 14:54:50  dtc1090
 * SR 59911 - adding new WebService layer to RUM
 *
 * Revision 1.7  2014/06/04 19:48:46  dtc1090
 * RATES-7941 - Put a Limit on the Number of Rows allowed per file
 *
 * Revision 1.6  2013/09/04 14:44:06  dtc1090
 * Merged DTG branch into HEAD
 *
 * Revision 1.5  2013/06/25 15:08:46  dtp0540
 * DJA - I accidentally committed the company_id changes to the head too soon.  So I'm reverting back to the previous historical modification.
 *
 * Revision 1.3  2012/08/20 18:03:31  dtc1090
 * SR-54472 - RUM Processing Changes
 *
 * Revision 1.2  2012/01/09 19:20:21  dtc1090
 * RATES-5641 - SR52946 - Addendums 1 & 2
 *
 * Revision 1.1  2011/05/19 19:13:48  dtc1090
 * SR 500044.01 (RUM Addendum 1) added the abilty to handle users sending in CCSS in the location field.  Also to write a seperate output file for locked plan/places
 *
 * Revision 1.9  2011/03/08 19:26:28  dtc1090
 * fixed null pointer
 *
 * Revision 1.8  2011/02/28 23:00:38  dtc1090
 * refactored RUM
 *
 * Revision 1.7  2011/02/25 21:06:58  dtc1090
 * added code to handle invalid dates
 *
 * Revision 1.6  2011/02/17 20:24:49  dtc1090
 * added date changes
 *
 * Revision 1.5  2011/01/31 20:54:44  dtc1090
 * continued development
 *
 * Revision 1.4  2011/01/27 20:01:18  dtc1090
 * development add
 *
 * Revision 1.3  2011/01/27 17:00:38  dtc1090
 * Added handling of sequence number
 *
 * Revision 1.2  2011/01/27 16:22:47  dtc1090
 * made changes
 *
 *
 *************************************************************
 *
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