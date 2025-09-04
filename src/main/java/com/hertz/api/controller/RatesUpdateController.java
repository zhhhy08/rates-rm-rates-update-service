package com.hertz.api.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.hertz.api.drivers.UpdateDriver;
import com.hertz.api.models.RatesUpdateRequest;
import com.hertz.api.models.RatesUpdateResponse;
import com.hertz.rates.common.utils.logging.HertzLogger;
import com.hertz.api.metrics.RumMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.TimeUnit;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
public class RatesUpdateController implements RateOperationsApi {
    private static final HertzLogger logger = new HertzLogger(RatesUpdateController.class);
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @Override
    public ResponseEntity<RatesUpdateResponse> performRatesUpdate(@Valid @RequestBody RatesUpdateRequest ratesUpdateRequest) throws Exception {
        logger.info("Received bulk update request: " + ratesUpdateRequest.toString());
        
        // Get the current HTTP request for IP extraction
        HttpServletRequest httpRequest = getCurrentHttpRequest();
        String remoteIP = "NO_IP_FOUND";
        
        try {
            // Retrieve the IP address from the request
            remoteIP = (String) httpRequest.getAttribute("remoteIP");
            
            if(null != remoteIP && remoteIP.length() > 0) {
                String[] splitIPs = remoteIP.split(",\\s*");
                remoteIP = splitIPs[0].trim();
            }
            
            logger.info("Incoming request IP: " + remoteIP);
        } catch (Exception ex) {
            logger.error("IPAddress Capture Error : " + ex.getMessage());
        }

        try {
            meterRegistry.counter(RumMetrics.METRIC_RUM_RATES_UPDATE).increment();
        } catch (Exception e) {
            logger.error("Dynatrace incrementCounter bulk update counter exception: " + e.getMessage());
        }

        UpdateDriver updateDriver = new UpdateDriver(meterRegistry);

        String requestString = ratesUpdateRequest.getRequestString();
        String responseString;
        
        // Record timing for the update operation
        long startTime = System.currentTimeMillis();
        
        try {
            responseString = updateDriver.doWebServiceUpdate(requestString, remoteIP);
            logger.info("Update successful with response: " + responseString);
        } catch (Exception e) {
            logger.error("Error during update: " + e.getMessage());
            throw e;
        } finally {
            // Record latency metric for all operations (successful and failed)
            try {
                long latency = System.currentTimeMillis() - startTime;
                meterRegistry.timer(RumMetrics.METRIC_RUM_RATES_UPDATE_LATENCY)
                    .record(latency, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                logger.error("Dynatrace record latency metric exception: " + e.getMessage());
            }
        }

        RatesUpdateResponse response = new RatesUpdateResponse();
        response.setResponseMessage(responseString);
        return ResponseEntity.ok(response);
    }

    private HttpServletRequest getCurrentHttpRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    }
}
