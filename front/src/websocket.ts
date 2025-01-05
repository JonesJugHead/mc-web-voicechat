import { store } from "./store";
import {
    handleJoin,
    handleOffer,
    handleAnswer,
    handleCandidate
} from "./webrtc";
import { WsMessage } from "./types";
import { handlePosition } from "./positions";

/**
 * Connect to the WebSocket server
 */
export function connectWebSocket(url: string): void {
    // Establish the connection
    store.ws = new WebSocket(url);

    let interval: ReturnType<typeof setTimeout>;

    store.ws.onopen = () => {
        console.log("Connected to the WebSocket server.");
        store.connected = true;

        // Enable the "Enable my microphone" button
        const startAudioBtn = document.getElementById("startAudioBtn") as HTMLButtonElement;
        if (startAudioBtn) {
            startAudioBtn.disabled = false;
        }

        interval = setInterval(() => {
            if (store.ws?.readyState === WebSocket.OPEN) {
                store.ws.send(JSON.stringify({ type: "ping" }));
            }
        }, 20000);
    };

    store.ws.onmessage = async (event: MessageEvent<string>) => {
        let data: WsMessage;
        try {
            data = JSON.parse(event.data);
        } catch (e) {
            console.warn("Non-JSON message received:", event.data);
            return;
        }

        const { type, from, to } = data;

        // Other WebRTC messages: ignore if not for us
        if (to && to !== store.localPseudo) return;

        switch (type) {
            case "join":
                if (from) handleJoin(from);
                break;
            case "offer":
                if (from && data.payload) handleOffer(from, data.payload);
                break;
            case "answer":
                if (from && data.payload) handleAnswer(from, data.payload);
                break;
            case "candidate":
                if (from && data.payload) handleCandidate(from, data.payload);
                break;
            case "ping":
                store.ws?.send(JSON.stringify({ type: "pong" }));
                break;
            case "pong":
                break;
            case "spatial":
                handlePosition(data);
                break;
            default:
                console.warn("Unknown message type:", type);
        }
    };

    store.ws.onclose = () => {
        console.log("Disconnected from the WebSocket server.");
        store.connected = false;
        clearInterval(interval);
    };

    store.ws.onerror = (err) => {
        console.error("WebSocket error:", err);
    };
}

/**
 * Send a JSON message via WebSocket
 */
export function sendMessage(msg: WsMessage): void {
    if (store.ws && store.connected) {
        store.ws.send(JSON.stringify(msg));
    }
}
