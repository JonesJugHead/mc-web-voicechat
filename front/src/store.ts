import { AudioNodes, PeerConnections } from "./types";

export const store = {
  ws: null as WebSocket | null,
  localStream: null as MediaStream | null,
  peers: {} as PeerConnections,
  audioNodes: {} as AudioNodes,
  localPseudo: "",
  connected: false,
  audioContext: null as AudioContext | null
};
