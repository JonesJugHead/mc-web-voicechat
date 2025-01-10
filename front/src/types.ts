/**
 * Represents the general structure of WS messages exchanged
 */
export type WsMessage = WsResponseAuthMessage | WsSendAuthMessage | WsRTCMessage | WsPositionMessage


export type WsPositionMessage = {
  type: 'spatial'
  targets?: Array<{ player: string, volume: number, pan: number, angle: number }>;
}

export type WsRTCMessage = {
  type: "join" | "offer" | "answer" | "candidate" | "ping" | "pong";
  from?: string;
  to?: string | null;
  payload?: any;   // keep "any" for flexibility
}

type WsSendAuthMessage = {
  type: "auth";
  code: string;
}

type WsResponseAuthMessage = {
  type: "auth" | "error";
  message?: 'success' | WsErrorMessages;
}

export type WsErrorMessages = 'AUTH_REQUIRED' | 'PLAYER_NOT_FOUND' | 'AUTH_NOT_REQUIRED' | 'INVALID_CODE';


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
