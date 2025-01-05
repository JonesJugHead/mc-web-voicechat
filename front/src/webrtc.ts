import { store } from "./store";
import { sendMessage } from "./websocket";

/**
 * RTC Configuration (STUN/TURN servers if needed)
 */
const rtcConfig: RTCConfiguration = {
  iceServers: []
};

/**
 * Returns the existing RTCPeerConnection for a peerId
 * or creates one if it doesn't exist
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

  // If we already have the local stream (microphone), add it
  if (store.localStream) {
    store.localStream.getTracks().forEach(track => {
      pc.addTrack(track, store.localStream!);
    });
  }

  store.peers[peerId] = pc;
  return pc;
}

/**
 * Creates a spatial audio node (source -> panner -> gain -> destination)
 */
export function createSpatialAudioNode(peerId: string, remoteStream: MediaStream): void {
  if (!store.audioContext) return;

  // (Optional) Create a hidden <audio> element for debugging
  let audioElem = document.getElementById("audio-" + peerId) as HTMLAudioElement;
  if (!audioElem) {
    audioElem = document.createElement("audio");
    audioElem.id = "audio-" + peerId;
    audioElem.autoplay = false; // we use the AudioContext
    audioElem.style.display = "none";
    document.body.appendChild(audioElem);
  }
  audioElem.srcObject = remoteStream;

  // Create Web Audio nodes
  const sourceNode = store.audioContext.createMediaStreamSource(remoteStream);
  const pannerNode = store.audioContext.createStereoPanner();
  const gainNode = store.audioContext.createGain();

  const filterNode = store.audioContext.createBiquadFilter();
  filterNode.type = "lowpass";
  filterNode.frequency.value = 20000; // default to very high => inaudible

  // Chain: source -> panner -> gain -> filter -> destination
  sourceNode
    .connect(pannerNode)
    .connect(gainNode)
    .connect(filterNode)
    .connect(store.audioContext.destination);

  // Store these nodes to adjust volume/pan later
  store.audioNodes[peerId] = {
    source: sourceNode,
    panner: pannerNode,
    gain: gainNode,
    filter: filterNode
  };
}

/**
 * Handle "join" message
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
 * Handle "offer" message
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
 * Handle "answer" message
 */
export function handleAnswer(peerId: string, answer: RTCSessionDescriptionInit): void {
  console.log("handleAnswer from", peerId);
  const pc = getOrCreatePeerConnection(peerId);
  pc.setRemoteDescription(answer).catch(console.error);
}

/**
 * Handle "candidate" message
 */
export function handleCandidate(peerId: string, candidate: RTCIceCandidateInit): void {
  console.log("handleCandidate from", peerId);
  const pc = getOrCreatePeerConnection(peerId);
  pc.addIceCandidate(new RTCIceCandidate(candidate)).catch(console.error);
}
