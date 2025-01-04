const express = require("express");
const http = require("http");
const { Server } = require("socket.io");

const app = express();
const server = http.createServer(app);
const io = new Server(server);

const PORT = 3000;

// Servir les fichiers statiques (HTML, JS, CSS)
app.use(express.static("public"));

// Gestion des connexions WebSocket
io.on("connection", (socket) => {
    console.log(`Nouvelle connexion : ${socket.id}`);

    // Recevoir une offre et la transmettre
    socket.on("offer", ({ offer, target }) => {
        io.to(target).emit("offer", { offer, from: socket.id });
    });

    // Recevoir une réponse et la transmettre
    socket.on("answer", ({ answer, target }) => {
        io.to(target).emit("answer", { answer, from: socket.id });
    });

    // Recevoir des ICE candidates et les transmettre
    socket.on("ice-candidate", ({ candidate, target }) => {
        io.to(target).emit("ice-candidate", { candidate, from: socket.id });
    });

    // Recevoir une demande de liste des utilisateurs connectés
    socket.on("get-users", () => {
        const users = Array.from(io.sockets.sockets.keys()).filter((id) => id !== socket.id);
        socket.emit("users", users);
    });

    // Déconnexion
    socket.on("disconnect", () => {
        console.log(`Déconnexion : ${socket.id}`);
    });
});

// Démarrage du serveur
server.listen(PORT, () => {
    console.log(`Serveur WebRTC en écoute sur http://localhost:${PORT}`);
});
