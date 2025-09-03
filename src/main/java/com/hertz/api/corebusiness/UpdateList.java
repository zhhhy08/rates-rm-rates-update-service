package com.hertz.api.corebusiness;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Holds a list of RumUpdateGroup's, each for a single Place/Plan/Company ID.
 * 
 *
 */
public class UpdateList {

    /** List of updates, each for a single place/plan */
    private ArrayList<RumUpdateGroup> listOfUpdates;

    /**
     * Adds group of changes for a Place/Plan/Company ID and its date span/vehicles to the list of updates. 
     * @param group
     */
    public void addRumUpdateGroup(RumUpdateGroup group) {

        if (listOfUpdates == null) {
            listOfUpdates = new ArrayList<RumUpdateGroup>();
        }

        listOfUpdates.add(group);
    }

    public ArrayList<RumUpdateGroup> getListOfUpdates() {

        return listOfUpdates;
    }

    /**
     * 
     * @param e
     */
    public void addErrorMessageToRumGroup(Exception e) {

        if (listOfUpdates != null) {
            Iterator<RumUpdateGroup> iter = listOfUpdates.iterator();
            while (iter.hasNext()) {
                RumUpdateGroup updateGroup = iter.next();
                updateGroup.addErrorMessageToDetails(e);
            }
        }
    }

    /**
     * Return true if all Groups have been processed.
     * @return
     */
    public boolean allGroupsProcessed() {

        Iterator<RumUpdateGroup> iter = listOfUpdates.iterator();
        boolean allGroupsProcessed = true;
        while (iter.hasNext()) {
            RumUpdateGroup group = iter.next();
            if (group != null && !group.isProcessed()) {
                allGroupsProcessed = false;
                break;
            }
        }

        return allGroupsProcessed;
    }

    /**
     * This will check all the change records and search for errors.
     * Is only looking for Locked plan/places for now, add to if needed. 
     *
     * @return
     */
    public boolean hasErrors() {

        Iterator<RumUpdateGroup> iter = listOfUpdates.iterator();
        boolean errorFound = false;
        while (iter.hasNext() && !errorFound) {
            RumUpdateGroup group = iter.next();
            if (group != null) {
                Iterator<RumChangeDetails> groupIter = group.getChangeDetails().iterator();
                while (groupIter.hasNext() && !errorFound) {
                    RumChangeDetails details = groupIter.next();
                    if (details != null && details.isLocked()) {
                        errorFound = true;
                        // @@JWH - should just return true here.
                    }
                }
            }
        }

        return errorFound;
    }
    
    
    /**
     * Describe Object
     * @return
     */
    public String toStringVerbose() {
        
        return toStringVerbose("");
    }
    
    /**
     * Describe Object
     * @param indent
     * @return
     */
    public String toStringVerbose(String indent) {
        
        StringBuffer s = new StringBuffer();

        s.append(indent + "UpdateList:" + "\n");
        
        Iterator<RumUpdateGroup> i = getListOfUpdates().iterator();
        while (i.hasNext()) {
            RumUpdateGroup rumUpdateGroup = i.next();
            s.append(rumUpdateGroup.toStringVerbose("    ") + "\n");
        }
        
        s.append(indent + "<<<<" + "\n");
        
        return s.toString();
    }
}

/*
 *************************************************************
 * Change History:
 *
 * $Log$
 * Revision 1.7  2016/11/08 19:01:12  dtp4395
 * RATES-11849; Heflin; Changed comments. No code changes.
 *
 * Revision 1.6  2016/10/27 14:47:54  dtp4395
 * RATES-11849; Heflin; Added comments, added CVS history tag, added copyright.  Formatting only.
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