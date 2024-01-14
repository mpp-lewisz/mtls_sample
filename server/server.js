const https = require('node:https');
const fs = require('fs');

const options = {
  // server privte key
  key: fs.readFileSync('../ca_signing/certs/node.key'),

  // server certificate
  cert: fs.readFileSync('../ca_signing/certs/node.crt'),

  // client certificates CA
  ca: fs.readFileSync('../ca_signing/certs/ca.crt'),

  requestCert: true,
};

https.createServer(options, (req, res) => {
  res.writeHead(200, {});
  res.end('Hello World');
}).listen(3000, () => {
  console.log('Server is running on port 3000');
});
