
import { Client } from "@stomp/stompjs";

let stompClient = null;

export const connectWebSocket = (onEquipmentUpdate) => {

    const token = sessionStorage.getItem("token");

    if (!token) {
        console.error("JWT token not found. WebSocket connection cancelled.");
        return;
    }

    stompClient = new Client({

        brokerURL: "ws://localhost:8080/ws",

        connectHeaders: {
            Authorization: `Bearer ${token}`,
        },

        reconnectDelay: 5000,

        onConnect: () => {

            console.log("Authenticated WebSocket connected successfully!");

            stompClient.subscribe(
                "/topic/equipment-status",
                (message) => {

                    const equipmentData = JSON.parse(message.body);

                    onEquipmentUpdate(equipmentData);
                }
            );
        },

        onStompError: (frame) => {
            console.error("STOMP error:", frame.headers["message"]);
        },

        onWebSocketError: (error) => {
            console.error("WebSocket error:", error);
        }
    });

    stompClient.activate();
};

export const disconnectWebSocket = () => {

    if (stompClient) {
        stompClient.deactivate();
        stompClient = null;
        console.log("WebSocket disconnected");
    }
};
