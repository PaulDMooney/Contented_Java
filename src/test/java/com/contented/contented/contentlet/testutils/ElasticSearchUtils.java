package com.contented.contented.contentlet.testutils;

public class ElasticSearchUtils {

    /***
     * Wait for whatever changes were made to be reflected in the ES index.
     *
     * It seems like ES changes do not take affect immediately, so we need to wait a bit.
     * TODO: Figure out if there is a better way to do this rather than wait for a time period.
     */
    public static void waitForESToAffectChanges() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
