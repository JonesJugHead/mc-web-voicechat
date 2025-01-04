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
