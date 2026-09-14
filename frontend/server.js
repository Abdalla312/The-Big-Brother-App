#!/usr/bin/env node

const http = require('http');
const fs = require('fs');
const path = require('path');

const ROOT = __dirname;

const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8080';

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.gif': 'image/gif',
  '.webp': 'image/webp',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.map': 'application/json',
};

function getPort(argv) {
  const envPort = Number(process.env.PORT);
  if (Number.isInteger(envPort) && envPort > 0) return envPort;

  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i];
    if (arg === '-l' || arg === '-p' || arg === '--port') {
      const value = argv[i + 1];
      if (value && /^\d+$/.test(value)) return Number(value);
    }
    if (/^--port=\d+$/.test(arg)) return Number(arg.split('=')[1]);
  }

  return 3000;
}

const PORT = getPort(process.argv.slice(2));
const HOST = process.env.HOST || '127.0.0.1';

function sendFile(res, filePath, status = 200) {
  fs.readFile(filePath, (err, data) => {
    if (err) {
      send404(res);
      return;
    }
    const ext = path.extname(filePath).toLowerCase();
    res.writeHead(status, {
      'Content-Type': MIME[ext] || 'application/octet-stream',
      'Cache-Control': 'no-cache',
    });
    res.end(data);
  });
}

function sendConfig(res) {
  const configPath = path.join(ROOT, 'js', 'config.js');
  fs.readFile(configPath, 'utf8', (err, content) => {
    if (err) {
      send404(res);
      return;
    }
    const rendered = content.replace('%%BACKEND_URL%%', BACKEND_URL);
    res.writeHead(200, {
      'Content-Type': 'text/javascript; charset=utf-8',
      'Cache-Control': 'no-cache',
    });
    res.end(rendered);
  });
}

function send404(res) {
  res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
  res.end('404 Not Found');
}

function safeJoin(base, target) {
  const relative = target.replace(/^\/+/, '');
  const resolved = path.resolve(base, relative);
  if (resolved !== base && !resolved.startsWith(base + path.sep)) return null;
  return resolved;
}

const server = http.createServer((req, res) => {
  if (req.method !== 'GET' && req.method !== 'HEAD') {
    res.writeHead(405, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end('405 Method Not Allowed');
    return;
  }

  const urlPath = decodeURIComponent(req.url.split('?')[0]);

  if (urlPath === '/js/config.js') {
    sendConfig(res);
    return;
  }

  let filePath = urlPath === '/' ? path.join(ROOT, 'index.html') : safeJoin(ROOT, urlPath);

  if (!filePath) {
    send404(res);
    return;
  }

  fs.stat(filePath, (err, stats) => {
    if (err || !stats.isFile()) {
      if (urlPath === '/' || !path.extname(urlPath)) {
        sendFile(res, path.join(ROOT, 'index.html'));
        return;
      }
      send404(res);
      return;
    }
    sendFile(res, filePath);
  });
});

server.listen(PORT, HOST, () => {
  console.log(`Big Brother frontend running at http://${HOST}:${PORT}`);
});