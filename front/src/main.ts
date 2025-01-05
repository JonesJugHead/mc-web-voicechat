import {
  connectWebSocket,
  sendMessage
} from "./websocket";
import {
  store
} from "./store";

// @ts-ignore
window.store = store;

// Récupération des éléments du DOM
const pseudoInput = document.getElementById("pseudo") as HTMLInputElement;
const connectBtn = document.getElementById("connectBtn") as HTMLButtonElement;
const startAudioBtn = document.getElementById("startAudioBtn") as HTMLButtonElement;
const disconnectBtn = document.getElementById("disconnectBtn") as HTMLButtonElement;
const applyMicrophoneBtn = document.getElementById("applyMicrophoneBtn") as HTMLButtonElement;
const micSelect = document.getElementById("microphoneSelect") as HTMLSelectElement;

/**
 * Bouton "Se connecter" : initialise pseudo, connecte WebSocket
 */
connectBtn.onclick = () => {
  const pseudo = pseudoInput.value.trim();
  if (!pseudo) {
    alert("Veuillez saisir votre pseudo (le même que Minecraft).");
    return;
  }

  // Stocke le pseudo dans le store
  store.localPseudo = pseudo;

  // Crée (ou reprend) un contexte audio
  const ctx = new AudioContext();
  store.audioContext?.valueOf; // juste pour s'assurer qu'il existe
  store.audioContext = ctx;

  // Lance la connexion WS (adapte l'URL à ton serveur)
  connectWebSocket("wss://" + window.location.host + "/ws");

  // Après un petit délai, envoie un message "join"
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
 * Bouton "Activer mon micro" : récupère le flux local
 */
startAudioBtn.onclick = async () => {
  try {
    store.localStream = await navigator.mediaDevices.getUserMedia({ audio: true, video: false });
    console.log("Micro capturé.");

    // Ajoute ce flux à chaque PeerConnection existante
    Object.keys(store.peers).forEach((peerId) => {
      const pc = store.peers[peerId];
      store.localStream!.getTracks().forEach(track => {
        pc.addTrack(track, store.localStream!);
      });
    });

    // Optionnel : relancer une offre pour envoyer le flux
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
    console.error("Erreur getUserMedia :", err);
    alert("Impossible d'accéder au micro.");
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

  // 3. Réinitialiser le store
  store.localPseudo = "";
  store.localStream?.getTracks().forEach(track => track.stop());
  store.localStream = null;

  // 4. Réinitialiser l'interface
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

    micSelect.innerHTML = ""; // Réinitialise la liste
    audioInputs.forEach(device => {
      const option = document.createElement("option");
      option.value = device.deviceId;
      option.textContent = device.label || `Microphone ${micSelect.options.length + 1}`;
      micSelect.appendChild(option);
    });

    // Activer le bouton si des microphones sont disponibles
    applyMicrophoneBtn.disabled = audioInputs.length === 0;
  } catch (err) {
    console.error("Erreur lors de la récupération des périphériques audio :", err);
    alert("Impossible de récupérer la liste des microphones.");
  }
}

populateMicrophoneList();



applyMicrophoneBtn.onclick = async () => {
  const deviceId = micSelect.value;

  if (!deviceId) {
    alert("Veuillez sélectionner un microphone.");
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

    console.log("Nouveau microphone activé :", deviceId);

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

    console.log("Nouveau flux audio envoyé.");
  } catch (err) {
    console.error("Erreur lors de la sélection du microphone :", err);
  }
};
