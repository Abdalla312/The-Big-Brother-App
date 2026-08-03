const fs = require('fs');
const path = require('path');

const configPath = path.join(__dirname, 'js', 'config.js');
const backendUrl = process.env.BACKEND_URL || 'http://localhost:8080';

let content = fs.readFileSync(configPath, 'utf8');
content = content.replace('%%BACKEND_URL%%', backendUrl);
fs.writeFileSync(configPath, content);

console.log(`config.js: API_URL set to ${backendUrl}`);
