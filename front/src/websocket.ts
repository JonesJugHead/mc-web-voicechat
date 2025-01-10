import { store } from "./store";
import {
    handleJoin,
    handleOffer,
    handleAnswer,
    handleCandidate
} from "./webrtc";
import { WsErrorMessages as WsErrorMessage, WsMessage } from "./types";
import { handlePosition } from "./positions";
import { resetUI, setConectionControls, showToast } from "./dom";

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
        setConectionControls(true);

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

        const { type } = data;

        // Other WebRTC messages: ignore if not for us
        if ((
                type === "join" ||
                type === 'answer' ||
                type === 'candidate' ||
                type === 'offer'
            ) && data.to !== store.localPseudo
        ) return;
        

        switch (type) {
            case "join":
                if (data.from) handleJoin(data.from);
                break;
            case "offer":
                if (data.from && data.payload) handleOffer(data.from, data.payload);
                break;
            case "answer":
                if (data.from && data.payload) handleAnswer(data.from, data.payload);
                break;
            case "candidate":
                if (data.from && data.payload) handleCandidate(data.from, data.payload);
                break;
            case "ping":
                store.ws?.send(JSON.stringify({ type: "pong" }));
                break;
            case "pong":
                break;
            case 'error':
                handleError(data.message as WsErrorMessage);
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
        resetUI();
        clearInterval(interval);
    };

    store.ws.onerror = (err) => {
        showToast("WebSocket error. Please reload the page.");
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


function handleError(message?: WsErrorMessage): void {
    console.warn("Server error:", message);
    resetUI();

    switch (message) {
        case 'INVALID_CODE':
            showToast("Invalid code. Please try again.");
            break;
        case 'AUTH_REQUIRED':
            store.setAuthRequired(true);
            showToast("Authentication required.");
            break;
        case 'AUTH_NOT_REQUIRED':
            store.setAuthRequired(false);
            showToast("Authentication not required.");
            break;
        case 'PLAYER_NOT_FOUND':
            showToast("Player not found.");
            break;
        default:
            showToast("An error occurred. Please try again.");
            break;
    }
}