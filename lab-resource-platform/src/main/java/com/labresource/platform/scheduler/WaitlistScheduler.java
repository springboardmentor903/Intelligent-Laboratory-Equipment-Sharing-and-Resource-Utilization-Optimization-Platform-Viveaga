
package com.labresource.platform.scheduler;

import com.labresource.platform.service.WaitlistService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WaitlistScheduler {

    private final WaitlistService waitlistService;

    public WaitlistScheduler(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    // Check for expired waitlist offers every 60 seconds.
    @Scheduled(fixedRate = 60000)
    public void expireWaitlistOffers() {

        waitlistService.processExpiredOffers();
    }
}
