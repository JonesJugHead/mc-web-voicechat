// positions.ts
import { store } from "./store";
import { Position, WsMessage } from "./types";

export function handlePosition(data: WsMessage): void {
    const { player, payload } = data;
    if (!player || !payload) return;

    store.positions[player] = {
        x: payload.x,
        y: payload.y,
        z: payload.z,
        yaw: payload.yaw,
        pitch: payload.pitch
    };

    console.log("Position reçue pour", player, store.positions[player]);

    updateAllSpatialization();
}

/**
 * Met à jour le volume (gain) et le pan stéréo pour chaque flux,
 * en tenant compte de la distance ET de l'orientation du joueur local.
 */
export function updateAllSpatialization(): void {
    if (!store.audioContext) return;

    const localPos = store.positions[store.localPseudo];
    if (!localPos) return;


    for (const peerId in store.audioNodes) {
        const node = store.audioNodes[peerId];
        if (!store.positions[peerId]) continue;

        const otherPos = store.positions[peerId];

        const { volume, pan, angle } = computeVolumePan(localPos, otherPos);

        node.gain.gain.value = volume;
        node.panner.pan.value = pan;

        if (node.filter) {
            const absAngle = Math.abs(angle);       // 0..π
            const ratio = absAngle / Math.PI;       // 0..1

            const freqFront = 20000; // devant : aucune coupure
            const freqBack = 2000;   // derrière : coupe aigus

            // Interpolation linéaire
            const freq = freqFront + (freqBack - freqFront) * ratio;
            node.filter.frequency.value = freq;

            // (Optionnel) On peut lisser la transition avec setTargetAtTime :
            // node.filter.frequency.setTargetAtTime(freq, store.audioContext.currentTime, 0.05);
        }
    }
}


/**
 * Calcule un volume (0 à 1) et un pan (-1 à 1),
 * en transformant la position de l'autre joueur
 * dans le repère local du joueur (myPos).
 */
export function computeVolumePan(
    myPos: Position,
    otherPos: Position,
    maxDistance = 20
): { volume: number; pan: number, angle: number } {
    const dx = otherPos.x - myPos.x;
    const dy = otherPos.y - myPos.y;
    const dz = otherPos.z - myPos.z;
    const dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

    // Atténuation linéaire sur 20 mètres
    let volume = 1 - dist / maxDistance;
    volume = Math.max(0, Math.min(1, volume));

    // ----------------------
    // 2) Transformation en coordonnées locales
    // ----------------------
    // On veut "tourner" le repère de -yaw pour que
    // dans ce repère local, l'axe Z local représente "devant moi".
    // yaw en radians
    const yawRad = (myPos.yaw * Math.PI) / 180;

    // cos(-yaw) =  cos(yaw)
    // sin(-yaw) = -sin(yaw)
    const cosA = Math.cos(-yawRad); // = cos(yawRad)
    const sinA = Math.sin(-yawRad); // = -sin(yawRad)

    // Dans un plan 2D (x, z), la rotation d'un point (X, Z) par un angle a
    // s'écrit :
    //   X' = X*cos(a) - Z*sin(a)
    //   Z' = X*sin(a) + Z*cos(a)
    // Ici, a = -yawRad
    const localX = dx * cosA - dz * sinA;
    const localZ = dx * sinA + dz * cosA;

    // ----------------------
    // 3) Calcul de l’angle "local"
    // ----------------------
    // Dans ce repère local :
    //   - angle=0 => l'autre est pile devant
    //   - angle=+π/2 => l'autre est strictement à droite
    //   - angle=-π/2 => l'autre est strictement à gauche
    //   - angle= π ou -π => derrière
    const angle = Math.atan2(-localX, localZ);

    // atan2(y, x) attend (localY, localX) en 2D classique,
    // mais nous, on "joue" sur (localX, localZ) => 
    // le 1er argument => "axe horizontal" (gauche/droite),
    // le 2nd argument => "axe avant/arrière".

    // ----------------------
    // 4) Conversion en pan
    // ----------------------
    // On veut -π/2 => pan=-1, +π/2 => pan=+1
    let pan = angle / (Math.PI / 2);
    // bornage
    pan = Math.max(-1, Math.min(1, pan));

    return { volume, pan, angle };
}
