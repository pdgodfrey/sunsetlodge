// Plugins
import vue from '@vitejs/plugin-vue'
import vuetify from 'vite-plugin-vuetify'

// Utilities
import { defineConfig } from 'vite'
import { fileURLToPath, URL } from 'node:url'

// https://vitejs.dev/config/
export default defineConfig({
  build: {
    outDir: '../webroot'
  },
  plugins: [
    vue(),
    // https://github.com/vuetifyjs/vuetify-loader/tree/next/packages/vite-plugin
    vuetify({
      autoImport: true,
    }),
  ],
  // css: {
  //   preprocessorOptions: {
  //     sass: {
  //       additionalData: [
  //         // Make the variables defined in these files available to all components, without requiring an explicit
  //         // @import of the files themselves
  //         '@import "./styles/variables"',
  //         // '@import "vuetify/src/styles/settings/_variables"',
  //         '', // end with newline
  //       ].join('\n'),
  //     },
  //   },
  // },
  define: { 'process.env': {} },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
    extensions: [
      '.js',
      '.json',
      '.jsx',
      '.mjs',
      '.ts',
      '.tsx',
      '.vue',
    ],
  },
  server: {
    port: 3000,
  },
})
