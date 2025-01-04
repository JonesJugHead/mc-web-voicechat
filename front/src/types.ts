// types.ts

/**
 * Représente la forme générale des messages WS qu'on s'échange
 */
export interface WsMessage {
    type: "pos" | "join" | "offer" | "answer" | "candidate" | string;
    from?: string;
    to?: string | null;
    player?: string; // utilisé pour la position
    payload?: any;   // on garde un "any" pour rester flexible
  }
  
  /**
   * Position Minecraft
   */
  export interface Position {
    x: number;
    y: number;
    z: number;
    yaw: number;
    pitch: number;
  }
  
  /**
   * Stockage des positions par pseudo de joueur
   */
  export interface Positions {
    [player: string]: Position;
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
  