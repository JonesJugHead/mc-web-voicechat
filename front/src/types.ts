/**
 * Représente la forme générale des messages WS qu'on s'échange
 */
export interface WsMessage {
    type: "spatial" | "join" | "offer" | "answer" | "candidate" | string;
    from?: string;
    to?: string | null;
    player?: string; // utilisé pour la position
    payload?: any;   // on garde un "any" pour rester flexible
    targets?: Array<{ player: string, volume: number, pan: number, angle: number }>;
  }
  
  /**
   * Les noeuds audio d'un flux distant
   * source -> panner -> gain -> destination
   */
  export interface AudioNodeConfig {
    source: MediaStreamAudioSourceNode;
    panner: StereoPannerNode;
    gain: GainNode;
    filter?: BiquadFilterNode;
  }
  
  /**
   * Ensemble des noeuds audio, indexés par peerId
   */
  export interface AudioNodes {
    [peerId: string]: AudioNodeConfig;
  }
  
  /**
   * Ensemble des RTCPeerConnection, indexées par peerId
   */
  export interface PeerConnections {
    [peerId: string]: RTCPeerConnection;
  }
  