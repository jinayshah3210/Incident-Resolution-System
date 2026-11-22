import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
export default defineConfig({
  plugins:[react()],
  server:{port:5173},
  build:{
    outDir:'dist',
    rollupOptions:{
      output:{manualChunks(id){
        if(!id.includes('node_modules')) return;
        if(id.includes('recharts')||id.includes('/d3-')) return 'charts';
        if(id.includes('lucide-react')) return 'icons';
        if(id.includes('/react/')||id.includes('react-dom')||id.includes('scheduler')) return 'react-core';
        if(id.includes('@tanstack')) return 'query';
        if(id.includes('axios')) return 'network';
        return 'vendor';
      }}
    }
  }
});
