/**
 * Represents the general structure of WS messages exchanged
 */
export interface WsMessage {
  type: "spatial" | "join" | "offer" | "answer" | "candidate" | string;
  from?: string;
  to?: string | null;
  player?: string; // used for position
  payload?: any;   // keep "any" for flexibility
  targets?: Array<{ player: string, volume: number, pan: number, angle: number }>;
}

/**
* Audio nodes of a remote stream
* source -> panner -> gain -> destination
*/
export interface AudioNodeConfig {
  source: MediaStreamAudioSourceNode;
  panner: StereoPannerNode;
  gain: GainNode;
  filter?: BiquadFilterNode;
}

/**
* Set of audio nodes, indexed by peerId
*/
export interface AudioNodes {
  [peerId: string]: AudioNodeConfig;
}

/**
* Set of RTCPeerConnection objects, indexed by peerId
*/
export interface PeerConnections {
  [peerId: string]: RTCPeerConnection;
}
