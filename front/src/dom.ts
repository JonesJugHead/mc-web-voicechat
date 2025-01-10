import { store } from "./store";

export const pseudoInput = document.getElementById("pseudo") as HTMLInputElement;
export const pseudoLabel = document.getElementById("pseudoLabel") as HTMLLabelElement;
export const connectBtn = document.getElementById("connectBtn") as HTMLButtonElement;
export const startAudioBtn = document.getElementById("startAudioBtn") as HTMLButtonElement;
export const muteMicroBtn = document.getElementById("muteMicroBtn") as HTMLButtonElement;
export const disconnectBtn = document.getElementById("disconnectBtn") as HTMLButtonElement;
export const playerList = document.getElementById("connectedPlayers") as HTMLUListElement;

export const setPseudoDisabled = (disabled: boolean) => {
    pseudoInput.disabled = disabled;
    connectBtn.disabled = disabled;
};


store.onAuthRequiredChange((authRequired) => {
    if (authRequired) {
        pseudoLabel.textContent = "Auth Code :";
        pseudoInput.placeholder = "Enter the auth code";
        return;
    } else {
        pseudoLabel.textContent = "Username (same as your Minecraft username):";
        pseudoInput.placeholder = "MyMinecraftUsername";
    }
})


export const setMicrophoneControls = (enabled: boolean) => {
    startAudioBtn.disabled = !enabled;
    muteMicroBtn.disabled = !enabled;
    disconnectBtn.disabled = !enabled;
};


export const setConectionControls = (connected: boolean) => {
    connectBtn.disabled = connected;
    pseudoInput.disabled = connected;
    startAudioBtn.disabled = !connected;
    disconnectBtn.disabled = !connected;
}


export const resetUI = () => {
    pseudoInput.disabled = false;
    pseudoInput.value = "";
    connectBtn.disabled = false;
    startAudioBtn.disabled = true;
    disconnectBtn.disabled = true;
}




export const updateConnectedPlayersList = (players: string[]) => {
    playerList.innerHTML = ""; // Clear the list

    players.forEach(player => {
        const li = document.createElement("li");
        li.textContent = player;
        playerList.appendChild(li);
    });
};


export function showToast(message: string) {
    const toaster = document.getElementById('toaster');
    if (!toaster) return;
    toaster.textContent = message;
    toaster.style.display = 'block';

    // Hide the toaster after 3 seconds
    setTimeout(() => {
        toaster.style.display = 'none';
    }, 3000);
}