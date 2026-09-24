const fs = require('fs');
const path = require('path');

const configPath = path.join(__dirname, 'js', 'config.js');
const backendUrl = process.env.BACKEND_URL || 'http://localhost:8080';

const content = `const API_URL = '${backendUrl}';\n\nexport { API_URL };\n`;
fs.writeFileSync(configPath, content);

console.log(`config.js: API_URL set to ${backendUrl}`);