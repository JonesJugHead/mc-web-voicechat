import './style.css'
import {
  connectWebSocket,
  sendMessage
} from "./websocket";
import {
  store
} from "./store";

// @ts-ignore
window.store = store;

// DOM elements retrieval
const pseudoInput = document.getElementById("pseudo") as HTMLInputElement;
const connectBtn = document.getElementById("connectBtn") as HTMLButtonElement;
const startAudioBtn = document.getElementById("startAudioBtn") as HTMLButtonElement;
const disconnectBtn = document.getElementById("disconnectBtn") as HTMLButtonElement;
const applyMicrophoneBtn = document.getElementById("applyMicrophoneBtn") as HTMLButtonElement;
const micSelect = document.getElementById("microphoneSelect") as HTMLSelectElement;

/**
 * "Connect" button: initializes username, connects WebSocket
 */
connectBtn.onclick = () => {
  const pseudo = pseudoInput.value.trim();
  if (!pseudo) {
    alert("Please enter your username (same as Minecraft).");
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
    sendMessage({
      type: "join",
      from: store.localPseudo,
      to: null,
      payload: {}
    });
  }, 500);

  connectBtn.disabled = true;
  pseudoInput.disabled = true;
};

/**
 * "Enable my microphone" button: retrieves local stream
 */
startAudioBtn.onclick = async () => {
  try {
    store.localStream = await navigator.mediaDevices.getUserMedia({ audio: true, video: false });
    console.log("Microphone captured.");

    // Add this stream to each existing PeerConnection
    Object.keys(store.peers).forEach((peerId) => {
      const pc = store.peers[peerId];
      store.localStream!.getTracks().forEach(track => {
        pc.addTrack(track, store.localStream!);
      });
    });

    // Optional: restart an offer to send the stream
    Object.keys(store.peers).forEach(peerId => {
      const pc = store.peers[peerId];
      pc.createOffer()
        .then(offer => pc.setLocalDescription(offer))
        .then(() => {
          if (!pc.localDescription) return;
          sendMessage({
            type: "offer",
            from: store.localPseudo,
            to: peerId,
            payload: pc.localDescription
          });
        })
        .catch(console.error);
    });

    startAudioBtn.disabled = true;
  } catch (err) {
    console.error("getUserMedia error:", err);
    alert("Unable to access the microphone.");
  }
};

disconnectBtn.onclick = () => {
  Object.keys(store.peers).forEach(peerId => {
    store.peers[peerId].close();
    delete store.peers[peerId];
  });

  if (store.ws) {
    store.ws.close();
    store.ws = null;
  }

  // Reset the store
  store.localPseudo = "";
  store.localStream?.getTracks().forEach(track => track.stop());
  store.localStream = null;

  // Reset the UI
  pseudoInput.disabled = false;
  pseudoInput.value = "";
  connectBtn.disabled = false;
  startAudioBtn.disabled = true;
  disconnectBtn.disabled = true;
};

async function populateMicrophoneList() {

  try {
    const devices = await navigator.mediaDevices.enumerateDevices();
    const audioInputs = devices.filter(device => device.kind === "audioinput");

    micSelect.innerHTML = ""; // Reset the list
    audioInputs.forEach(device => {
      const option = document.createElement("option");
      option.value = device.deviceId;
      option.textContent = device.label || `Microphone ${micSelect.options.length + 1}`;
      micSelect.appendChild(option);
    });

    // Enable the button if microphones are available
    applyMicrophoneBtn.disabled = audioInputs.length === 0;
  } catch (err) {
    console.error("Error retrieving audio devices:", err);
    alert("Unable to retrieve the list of microphones.");
  }
}

populateMicrophoneList();

applyMicrophoneBtn.onclick = async () => {
  const deviceId = micSelect.value;

  if (!deviceId) {
    alert("Please select a microphone.");
    return;
  }

  try {
    const newStream = await navigator.mediaDevices.getUserMedia({
      audio: { deviceId: { exact: deviceId } },
      video: false
    });

    if (store.localStream) {
      store.localStream.getTracks().forEach(track => track.stop());
    }
    store.localStream = newStream;

    console.log("New microphone activated:", deviceId);

    Object.keys(store.peers).forEach(peerId => {
      const pc = store.peers[peerId];
      store.localStream!.getTracks().forEach(track => {
        pc.addTrack(track, store.localStream!);
      });
    });

    Object.keys(store.peers).forEach(peerId => {
      const pc = store.peers[peerId];
      pc.createOffer()
        .then(offer => pc.setLocalDescription(offer))
        .then(() => {
          if (!pc.localDescription) return;
          sendMessage({
            type: "offer",
            from: store.localPseudo,
            to: peerId,
            payload: pc.localDescription
          });
        })
        .catch(console.error);
    });

    console.log("New audio stream sent.");
  } catch (err) {
    console.error("Error selecting the microphone:", err);
  }
};
