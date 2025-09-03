package com.hertz.api.metrics;

/**
 * Centralized constants for all RUM (Rate Update Mass) Dynatrace metrics.
 * 
 * This class provides a single source of truth for metric names used across
 * the RUM webservice application for Dynatrace monitoring.
 * 
 * @author Generated for RUM Metrics Migration
 */
public final class RumMetrics {
    
    // Prevent instantiation
    private RumMetrics() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String METRIC_RUM_RATES_UPDATE = "rates-update-txn-count";

    public static final String METRIC_RUM_RATES_UPDATE_LATENCY = "rates-update-txn-latency";

    public static final String METRIC_RUM_RATES_RMS_RECORD_COUNT = "rates-rms-record-count";

    public static final String METRIC_RUM_RATES_RMS_RECORD_DISTRIBUTION_COUNT = "rates-rms-record-distribution-count";

    public static final String METRIC_RUM_RATES_DML_COUNT = "rates-rum-dml-count";
    
    public static final String METRIC_RUM_RATES_DML_DISTRIBUTION = "rates-rum-dml-distribution";
    
    // Rate update success/failure metrics
    public static final String METRIC_RUM_RATE_UPDATE_SUCCESS_COUNT = "rates-rum-rate-update-success-count";
    
    public static final String METRIC_RUM_RATE_UPDATE_FAIL_COUNT = "rates-rum-rate-update-fail-count";
    
    // Client IP metrics
    public static final String METRIC_RUM_CLIENT_IP_RUP_SUCCESS_COUNT = "rates-rum-client-ip-success-count";
    
    public static final String METRIC_RUM_CLIENT_IP_RUP_FAILURE_COUNT = "rates-rum-client-ip-failure-count";
    
    // Client country metrics
    public static final String METRIC_RUM_CLIENT_COUNTRY_RUP_SUCCESS_COUNT = "rates-rum-client-country-success-count";
    
    public static final String METRIC_RUM_CLIENT_COUNTRY_RUP_FAILURE_COUNT = "rates-rum-client-country-failure-count";
    
    public static final String METRIC_RUM_CLIENT_COUNTRY_API_CALL_COUNT = "rates-rum-client-country-api-call-count";
}
