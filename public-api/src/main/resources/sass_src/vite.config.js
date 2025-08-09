const path = require('path')

export default {
  root: path.resolve(__dirname, 'src'),
  resolve: {
    alias: {
      '~bootstrap': path.resolve(__dirname, 'node_modules/bootstrap'),
    }
  },
  build: {
    // outDir: './../../../../../webroot',
    rollupOptions: {
      input: '/js/main.js',
      output: {
        assetFileNames: (chunkInfo) => {
          return 'css/[name][extname]'
        },
        entryFileNames: 'js/[name].js'
      },
    },
  },
  server: {
    port: 8080,
    hot: true
  }
}
