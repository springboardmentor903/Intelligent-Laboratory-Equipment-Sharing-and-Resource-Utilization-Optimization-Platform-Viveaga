
package com.labresource.platform.service;

import com.labresource.platform.entity.UsageLog;
import com.labresource.platform.repository.UsageLogRepository;
import org.springframework.stereotype.Service;

@Service
public class UsageLogService {

    private final UsageLogRepository usageLogRepository;

    public UsageLogService(UsageLogRepository usageLogRepository) {
        this.usageLogRepository = usageLogRepository;
    }

    public UsageLog saveUsageLog(UsageLog usageLog) {
        return usageLogRepository.save(usageLog);
    }

    public UsageLog getOpenUsageLog(Long bookingId) {
        return usageLogRepository
                .findTopByBookingIdAndActualEndTimeIsNullOrderByActualStartTimeDesc(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Active usage log not found"));
    }
}
