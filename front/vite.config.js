import { defineConfig } from 'vite'

export default defineConfig({
  build: {
    outDir: '../plugin/src/main/resources/web/', // Dossier de sortie
    rollupOptions: {
      input: 'index.html', // Fichier HTML d'entrée
      output: {
        // Force la sortie dans un seul fichier JS nommé "index.js"
        manualChunks: () => 'index',
        entryFileNames: 'index.js',
        chunkFileNames: 'index.js',
        assetFileNames: 'index.[ext]'
      }
    }
  }
})
