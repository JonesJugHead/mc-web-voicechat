
let localStream;
const peerConnections = {};
const configuration = {
    iceServers: [{ urls: 'stun:stun.l.google.com:19302' }]
};

let userList;

const playerName = prompt("Enter your username");
const socket = new WebSocket("ws://192.168.1.27:25566");

socket.onopen = () => {
    console.log("WebSocket connected.");
    socket.send(JSON.stringify({ type: "set-username", username: playerName }));
};

socket.onmessage = async (event) => {
    const data = JSON.parse(event.data);
    console.log("Received:", data);

    switch (data.type) {
        case "user-list":
            updatePlayerList(data.payload);
            break;

        case "offer":
            await handleOffer(data);
            break;

        case "answer":
            await handleAnswer(data);
            break;

        case "ice-candidate":
            await handleIceCandidate(data);
            break;

        default:
            console.warn("Unknown type:", data.type);
    }
};

async function handleOffer(data) {
    const from = data.from;

    if (!peerConnections[from]) {
        createPeerConnection(from);
    }

    await peerConnections[from].setRemoteDescription(new RTCSessionDescription(data.payload));
    const answer = await peerConnections[from].createAnswer();
    await peerConnections[from].setLocalDescription(answer);

    socket.send(JSON.stringify({ type: "answer", from: playerName, target: from, payload: answer }));
}

async function handleAnswer(data) {
    const from = data.from;
    if (peerConnections[from]) {
        await peerConnections[from].setRemoteDescription(new RTCSessionDescription(data.payload));
    }
}

async function handleIceCandidate(data) {
    const from = data.from;
    if (peerConnections[from] && data.payload) {
        await peerConnections[from].addIceCandidate(new RTCIceCandidate(data.payload));
    }
}

function createPeerConnection(username) {
    const peerConnection = new RTCPeerConnection(configuration);

    peerConnection.onicecandidate = (event) => {
        if (event.candidate) {
            socket.send(JSON.stringify({ type: "ice-candidate", from: playerName, target: username, payload: event.candidate }));
        }
    };

    peerConnection.ontrack = (event) => {
        const audio = document.createElement("audio");
        audio.srcObject = event.streams[0];
        audio.autoplay = true;
        document.body.appendChild(audio);
    };

    if (localStream) {
        localStream.getTracks().forEach((track) => peerConnection.addTrack(track, localStream));
    }

    peerConnections[username] = peerConnection;
}

function updatePlayerList(users) {
    userList = users
    const playerList = document.getElementById("playerList");
    playerList.innerHTML = users.map(user => `<li>${user}</li>`).join("");
}

document.getElementById("startMic").addEventListener("click", async () => {
    localStream = await navigator.mediaDevices.getUserMedia({ audio: true });
    document.getElementById("localAudio").srcObject = localStream;

    Object.keys(peerConnections).forEach((username) => {
        localStream.getTracks().forEach((track) => peerConnections[username].addTrack(track, localStream));
    });

    userList.forEach((username) => {
        if (!peerConnections[username]) {
            createPeerConnection(username);
        }
    });

    document.getElementById("startMic").disabled = true;
    document.getElementById("stopMic").disabled = false;
});

document.getElementById("stopMic").addEventListener("click", () => {
    if (localStream) {
        localStream.getTracks().forEach((track) => track.stop());
        localStream = null;
    }

    document.getElementById("startMic").disabled = false;
    document.getElementById("stopMic").disabled = true;
});