package com.corionis.els;

import com.corionis.els.repository.Repository;

/**
 * Values that persist between layered threads when Context is cloned and/or a new Main is running, e.g. in a Job
 */
public class Persistent
{
    public static boolean couldNotConnect = false; // ClientStty command pipe could not connect

    public static boolean faultEmailSent = false; // do not send fault emails more than once

    public static Repository lastPublisherRepo = null;

    public static Repository lastSubscriberRepo = null;
}
