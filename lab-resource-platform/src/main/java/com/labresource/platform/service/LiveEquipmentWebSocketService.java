package com.labresource.platform.service;

import com.labresource.platform.dto.LiveEquipmentStatusDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
public class LiveEquipmentWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final LiveEquipmentStatusService liveEquipmentStatusService;

    public LiveEquipmentWebSocketService(
            SimpMessagingTemplate messagingTemplate,
            LiveEquipmentStatusService liveEquipmentStatusService) {

        this.messagingTemplate = messagingTemplate;
        this.liveEquipmentStatusService = liveEquipmentStatusService;
    }

    public void broadcastEquipmentStatus() {

        Runnable sendUpdate = () -> {

            List<LiveEquipmentStatusDTO> equipmentStatus =
                    liveEquipmentStatusService.getLiveEquipmentStatus(
                            null, 0, 1000
                    );

            messagingTemplate.convertAndSend(
                    "/topic/equipment-status",
                    equipmentStatus
            );
        };

        if (TransactionSynchronizationManager.isActualTransactionActive()) {

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {

                        @Override
                        public void afterCommit() {
                            sendUpdate.run();
                        }
                    }
            );

        } else {
            sendUpdate.run();
        }
    }
}