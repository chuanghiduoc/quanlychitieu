// ecosystem.config.js
module.exports = {
  apps: [
    {
      name: "quanly-admin",
      script: "node_modules/next/dist/bin/next",
      args: "start",
      env: {
        PORT: 8765,
        NODE_ENV: "production"
      }
    }
  ]
};
