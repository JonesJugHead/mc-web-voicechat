import { AudioNodes, PeerConnections } from "./types";

export const store = {
  authRequired: false,
  ws: null as WebSocket | null,
  localStream: null as MediaStream | null,
  peers: {} as PeerConnections,
  audioNodes: {} as AudioNodes,
  localPseudo: "",
  connected: false,
  audioContext: null as AudioContext | null,

  callbacks: [] as ((value: boolean) => void)[],
  setAuthRequired(value: boolean) {
    if (this.authRequired !== value) {
      this.authRequired = value;
      this.callbacks.forEach((callback) => callback(value));
    }
  },
  
  onAuthRequiredChange(callback: (value: boolean) => void) {
    this.callbacks.push(callback);
  }
};