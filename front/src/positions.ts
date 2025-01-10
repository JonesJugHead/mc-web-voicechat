import { store } from "./store";
import { WsPositionMessage } from "./types";

export function handlePosition({ targets }:  WsPositionMessage): void {
    if (!targets) return;

    targets.forEach(({ player, volume, pan, angle }) => {
        const audioNodeConfig = store.audioNodes[player];

        if (audioNodeConfig) {
            audioNodeConfig.gain.gain.value = volume;
            audioNodeConfig.panner.pan.value = pan;

            if (audioNodeConfig.filter) {
                const absAngle = Math.abs(angle);       // 0..π
                const ratio = absAngle / Math.PI;       // 0..1
    
                const freqFront = 20000; // front : no filter
                const freqBack = 2000;   // back : low-pass filter
    
                // Linear interpolation between freqFront and freqBack
                const freq = freqFront + (freqBack - freqFront) * ratio;
                audioNodeConfig.filter.frequency.value = freq;
            }
        }
    });
}
