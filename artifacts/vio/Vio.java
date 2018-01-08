package com.groksoft.volmonger.vio;

import com.groksoft.volmonger.MongerException;
import com.groksoft.volmonger.repository.Item;
import com.groksoft.volmonger.repository.Libraries;
import com.groksoft.volmonger.repository.Library;
import com.groksoft.volmonger.repository.LibraryData;
import com.groksoft.volmonger.storage.Targets;

/**
 * Vio
 * <p>
 * The Virtual I/O controller
 */
public class Vio
{
    static Vio instance = null;

    private void Vio() {
        // singleton pattern
    }

    public static Vio getInstance() {
        if (instance == null) {
            instance = new Vio();
        }
        return instance;
    }


    // ------------------------------------------ Publisher Section ------------------------------------------

    public boolean publisherCopyToSubscriber(String from, String to) {
        // TODO Handle if from or to are remote in Vio class
        // Rule: It is ALWAYS from Publisher to Subscriber
        return false;
    }

    public long publisherItemSize() {
        // TODO Get the size of a publisher item
        return 0L;
    }

    public void publisherScan(Libraries libraries, String libraryName) {
        // TODO publisherScan the library for its items
        // library might be remote
    }


    // ------------------------------------------ Subscriber Section ------------------------------------------

    public long subscriberAvailableSpace(String location) {
        // TODO get the free space at the specified location
        // location might be remote
        return 0;
    }

    public void subscriberScan(Libraries libraries, String libraryName) {
        // TODO subscriberScan the library for its items
        // library might be remote
    }


    // ------------------------------------------ Targets Section ------------------------------------------

    public String targetFind(Item item, String library, long size) throws MongerException {
        return "";
    }

    public Targets targetsRead() {
        // TODO read targets
        // targets might be remote
        return new Targets();
    }

    public boolean targetMakeDirectories(String dirs) {
        // TODO make directories on the target
        // target might be remote
        return false;
    }


}
