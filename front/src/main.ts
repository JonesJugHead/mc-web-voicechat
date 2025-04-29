import "./style.css";
import { connectWebSocket, sendMessage } from "./websocket";
import { store } from "./store";
import {
  connectBtn,
  disconnectBtn,
  pseudoInput,
  resetUI,
  setPseudoDisabled,
  showToast,
  startAudioBtn,
} from "./dom";
import { fetchAuthRequiredStatus } from "./auth";

// @ts-ignore
window.store = store;

// DOM elements retrieval
const applyMicrophoneBtn = document.getElementById(
  "applyMicrophoneBtn"
) as HTMLButtonElement;
const micSelect = document.getElementById(
  "microphoneSelect"
) as HTMLSelectElement;

document.addEventListener("DOMContentLoaded", async () => {
  try {
    const { authRequired } = await fetchAuthRequiredStatus();
    store.setAuthRequired(authRequired);
  } catch (error) {
    console.error("Error fetching auth status:", error);
    showToast("Unable to retrieve the auth status.");
  }
});

/**
 * "Connect" button: initializes username, connects WebSocket
 */
connectBtn.onclick = () => {
  const pseudo = pseudoInput.value.trim();
  if (!pseudo) {
    showToast("Please fill the input field.");
    return;
  }

  // Store the username in the store
  store.localPseudo = pseudo;

  // Create (or reuse) an audio context
  const ctx = new AudioContext();
  store.audioContext?.valueOf; // just to ensure it exists
  store.audioContext = ctx;

  // Start WebSocket connection (adapt the URL to your server)
  connectWebSocket("wss://" + window.location.host + "/ws");

  // After a short delay, send a "join" message
  setTimeout(() => {
    if (store.authRequired) {
      // Send the auth code to the server
      sendMessage({
        type: "auth",
        code: store.localPseudo,
      });
    } else {
      sendMessage({
        type: "join",
        from: store.localPseudo,
      });
    }
  }, 500);

  setPseudoDisabled(true);
};

/**
 * "Enable my microphone" button: retrieves local stream
 */
startAudioBtn.onclick = async () => {
  try {
    store.localStream = await navigator.mediaDevices.getUserMedia({
      audio: true,
      video: false,
    });
    console.log("Microphone captured.");

    // Add this stream to each existing PeerConnection
    Object.keys(store.peers).forEach((peerId) => {
      const pc = store.peers[peerId];
      store.localStream!.getTracks().forEach((track) => {
        pc.addTrack(track, store.localStream!);
      });
    });

    // Optional: restart an offer to send the stream
    Object.keys(store.peers).forEach((peerId) => {
      const pc = store.peers[peerId];
      pc.createOffer()
        .then((offer) => pc.setLocalDescription(offer))
        .then(() => {
          if (!pc.localDescription) return;
          sendMessage({
            type: "offer",
            from: store.localPseudo,
            to: peerId,
            payload: pc.localDescription,
          });
        })
        .catch(console.error);
    });

    startAudioBtn.disabled = true;
  } catch (err) {
    console.error("getUserMedia error:", err);
    showToast("Unable to access the microphone.");
  }
};

disconnectBtn.onclick = () => {
  Object.keys(store.peers).forEach((peerId) => {
    store.peers[peerId].close();
    delete store.peers[peerId];
  });

  if (store.ws) {
    store.ws.close();
    store.ws = null;
  }

  // Reset the store
  store.localPseudo = "";
  store.localStream?.getTracks().forEach((track) => track.stop());
  store.localStream = null;

  // Reset the UI
  resetUI();
};

async function populateMicrophoneList() {
  try {
    const devices = await navigator.mediaDevices.enumerateDevices();
    const audioInputs = devices.filter(
      (device) => device.kind === "audioinput"
    );

    micSelect.innerHTML = ""; // Reset the list
    audioInputs.forEach((device) => {
      const option = document.createElement("option");
      option.value = device.deviceId;
      option.textContent =
        device.label || `Microphone ${micSelect.options.length + 1}`;
      micSelect.appendChild(option);
    });

    // Enable the button if microphones are available
    applyMicrophoneBtn.disabled = audioInputs.length === 0;
  } catch (err) {
    console.error("Error retrieving audio devices:", err);
    showToast("Unable to retrieve the list of microphones.");
  }
}

populateMicrophoneList();

applyMicrophoneBtn.onclick = async () => {
  const deviceId = micSelect.value;

  if (!deviceId) {
    showToast("Please select a microphone.");
    return;
  }

  try {
    const newStream = await navigator.mediaDevices.getUserMedia({
      audio: { deviceId: { exact: deviceId } },
      video: false,
    });

    if (store.localStream) {
      store.localStream.getTracks().forEach((track) => track.stop());
    }
    store.localStream = newStream;

    console.log("New microphone activated:", deviceId);

    Object.keys(store.peers).forEach((peerId) => {
      const pc = store.peers[peerId];
      store.localStream!.getTracks().forEach((track) => {
        pc.addTrack(track, store.localStream!);
      });
    });

    Object.keys(store.peers).forEach((peerId) => {
      const pc = store.peers[peerId];
      pc.createOffer()
        .then((offer) => pc.setLocalDescription(offer))
        .then(() => {
          if (!pc.localDescription) return;
          sendMessage({
            type: "offer",
            from: store.localPseudo,
            to: peerId,
            payload: pc.localDescription,
          });
        })
        .catch(console.error);
    });

    console.log("New audio stream sent.");
  } catch (err) {
    showToast("Unable to select the microphone.");
    console.error("Error selecting the microphone:", err);
  }
};
