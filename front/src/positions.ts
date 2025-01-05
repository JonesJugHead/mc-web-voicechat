import { store } from "./store";
import { WsMessage } from "./types";

export function handlePosition({ targets }: WsMessage): void {
    if (!targets) return;

    targets.forEach(({ player, volume, pan, angle }) => {
        const audioNodeConfig = store.audioNodes[player];

        if (audioNodeConfig) {
            audioNodeConfig.gain.gain.value = volume;
            audioNodeConfig.panner.pan.value = pan;

            if (audioNodeConfig.filter) {
                const absAngle = Math.abs(angle);       // 0..π
                const ratio = absAngle / Math.PI;       // 0..1
    
                const freqFront = 20000; // devant : aucune coupure
                const freqBack = 2000;   // derrière : coupe aigus
    
                // Interpolation linéaire
                const freq = freqFront + (freqBack - freqFront) * ratio;
                audioNodeConfig.filter.frequency.value = freq;
            }
        }
    });
}
