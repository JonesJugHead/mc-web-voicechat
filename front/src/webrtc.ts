// webrtc.ts
import { store } from "./store";
import { sendMessage } from "./websocket";

/**
 * Configuration RTC (serveurs STUN/TURN si besoin)
 */
const rtcConfig: RTCConfiguration = {
  iceServers: []
};

/**
 * Retourne le RTCPeerConnection existant pour un peerId
 * ou le crée s'il n'existe pas
 */
export function getOrCreatePeerConnection(peerId: string): RTCPeerConnection {
  if (store.peers[peerId]) {
    return store.peers[peerId];
  }

  const pc = new RTCPeerConnection(rtcConfig);

  pc.onicecandidate = (event) => {
    if (event.candidate) {
      sendMessage({
        type: "candidate",
        from: store.localPseudo,
        to: peerId,
        payload: event.candidate
      });
    }
  };

  pc.ontrack = (event) => {
    console.log("ontrack from", peerId);
    const remoteStream = event.streams[0];
    createSpatialAudioNode(peerId, remoteStream);
  };

  // Si on a déjà le flux local (micro), on l'ajoute
  if (store.localStream) {
    store.localStream.getTracks().forEach(track => {
      pc.addTrack(track, store.localStream!);
    });
  }

  store.peers[peerId] = pc;
  return pc;
}

/**
 * Crée un noeud audio spatial (source -> panner -> gain -> destination)
 */
export function createSpatialAudioNode(peerId: string, remoteStream: MediaStream): void {
  if (!store.audioContext) return;

  // (Optionnel) Création d'un <audio> caché pour debug
  let audioElem = document.getElementById("audio-" + peerId) as HTMLAudioElement;
  if (!audioElem) {
    audioElem = document.createElement("audio");
    audioElem.id = "audio-" + peerId;
    audioElem.autoplay = false; // on utilise l'AudioContext
    audioElem.style.display = "none";
    document.body.appendChild(audioElem);
  }
  audioElem.srcObject = remoteStream;

  // Création des noeuds Web Audio
  const sourceNode = store.audioContext.createMediaStreamSource(remoteStream);
  const pannerNode = store.audioContext.createStereoPanner();
  const gainNode = store.audioContext.createGain();

  const filterNode = store.audioContext.createBiquadFilter();
  filterNode.type = "lowpass";
  filterNode.frequency.value = 20000; // par défaut, très haut => inaudible

  // Chaînage : source -> panner -> gain -> filtre -> destination
  sourceNode
    .connect(pannerNode)
    .connect(gainNode)
    .connect(filterNode)
    .connect(store.audioContext.destination);

  // Stocke ces noeuds pour pouvoir ajuster volume/pan plus tard
  store.audioNodes[peerId] = {
    source: sourceNode,
    panner: pannerNode,
    gain: gainNode,
    filter: filterNode
  };

}

/**
 * Réception d'un message "join"
 */
export function handleJoin(peerId: string): void {
  console.log("handleJoin from", peerId);
  const pc = getOrCreatePeerConnection(peerId);

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
}

/**
 * Réception d'un message "offer"
 */
export function handleOffer(peerId: string, offer: RTCSessionDescriptionInit): void {
  console.log("handleOffer from", peerId);
  const pc = getOrCreatePeerConnection(peerId);

  pc.setRemoteDescription(offer)
    .then(() => pc.createAnswer())
    .then(answer => pc.setLocalDescription(answer))
    .then(() => {
      if (!pc.localDescription) return;
      sendMessage({
        type: "answer",
        from: store.localPseudo,
        to: peerId,
        payload: pc.localDescription
      });
    })
    .catch(console.error);
}

/**
 * Réception d'un message "answer"
 */
export function handleAnswer(peerId: string, answer: RTCSessionDescriptionInit): void {
  console.log("handleAnswer from", peerId);
  const pc = getOrCreatePeerConnection(peerId);
  pc.setRemoteDescription(answer).catch(console.error);
}

/**
 * Réception d'un message "candidate"
 */
export function handleCandidate(peerId: string, candidate: RTCIceCandidateInit): void {
  console.log("handleCandidate from", peerId);
  const pc = getOrCreatePeerConnection(peerId);
  pc.addIceCandidate(new RTCIceCandidate(candidate)).catch(console.error);
}
