// store.ts

import { AudioNodes, PeerConnections, Positions } from "./types";

export const store = {
  ws: null as WebSocket | null,
  localStream: null as MediaStream | null,
  peers: {} as PeerConnections,
  audioNodes: {} as AudioNodes,
  positions: {} as Positions,
  localPseudo: "",
  connected: false,
  audioContext: null as AudioContext | null
};
