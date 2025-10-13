/* global BigInt */
const https = require('node:https');
const crypto = require('node:crypto');
const fs = require('fs');

const version = fs.readFileSync(`${__dirname}/../build.gradle.kts`, 'utf8')
	.split('\n')
	.filter(line => line.startsWith('version = "'))[0]
	.replace('version = "', '')
	.replace('"', '')
	.trim();

fs.copyFileSync(`${__dirname}/../build/libs/antinbt-${version}.jar`, `${__dirname}/AntiNbt.jar`);
console.log('File copied');


fs.watchFile(`${__dirname}/../build/libs/antinbt-${version}.jar`, () => { // copy file again whenever changes are made
	const version = fs.readFileSync(`${__dirname}/../build.gradle.kts`, 'utf8')
		.split('\n')
		.filter(line => line.startsWith('version = "'))[0]
		.replace('version = "', '')
		.replace('"', '')
		.trim();

	fs.copyFileSync(`${__dirname}/../build/libs/antinbt-${version}.jar`, `${__dirname}/AntiNbt.jar`);
	console.log('File re-copied');
});

let totpCode = '';
if (process.argv.includes('--unsecure-code')) {
	console.warn('Not using a randomly generated code, but a public code');
	totpCode = '50xOnTop';
}
else {
	const chars = 'abdecfghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
	for (let i = 0; i < Math.floor(Math.random() * 10) + 20; i++) {
		totpCode += chars.charAt(Math.floor(Math.random() * chars.length));
	}
}
console.log(`TOTP CODE: ${totpCode}`);

function generateCode() {
	const epoch = Math.floor(Date.now() / 1000); // current unix time in seconds
	const counter = Math.floor(epoch / 30); // time counter

	// Convert counter to an 8-byte buffer
	const buffer = Buffer.alloc(8);
	buffer.writeBigUInt64BE(BigInt(counter));

	// HMAC-SHA1 using the secret
	const hmac = crypto.createHmac('sha1', Buffer.from(totpCode));
	hmac.update(buffer);
	const hash = hmac.digest();

	// Dynamic truncation
	const offset = hash[hash.length - 1] & 0xf;
	const code =
    ((hash[offset] & 0x7f) << 24) |
    ((hash[offset + 1] & 0xff) << 16) |
    ((hash[offset + 2] & 0xff) << 8) |
    (hash[offset + 3] & 0xff);

	// Return last N digits
	return String(code % 10 ** 10).padStart(10, '0');
}


const app = require('express')();
app.get('/modules', (req) => req.socket.destroy());

app.get('/modules/:module', (req, res) => {
	const modules = fs.readdirSync('../modules/build/classes/java/main/me/mynameisbob1928/antinbt/modules');
	if (!modules.includes(req.params.module)) {
		console.log(`Ignoring incoming connection from ${req.ip || req.connection.remoteAddress} on url '${req.url}' (Invalid class)`);
		req.socket.destroy();
		return;
	}

	console.log(`Module download requested, IP: ${req.ip || req.connection.remoteAddress}, current code: ${generateCode()}, received code: ${req.query?.code}`);

	if (req.query?.code !== generateCode()) {
		req.socket.destroy();
		return;
	}
	res.download(`${__dirname}/../modules/build/classes/java/main/me/mynameisbob1928/antinbt/modules/${req.params.module}`);

});


app.use((req, res, next) => {
	// Trying to make it appear that there isn't anything here, but that isn't possible wihout modifying the os so this is the next best thing
	if (req.path !== '/antinbt.jar') {
		console.log(`Ignoring incoming connection from ${req.ip || req.connection.remoteAddress} on url '${req.url}'`);
		req.socket.destroy();
		return;
	}
	if (!req.query?.code) {
		req.socket.destroy();
		console.log(`Ignoring incoming connection from ${req.ip || req.connection.remoteAddress} on url '${req.url}'`);
		return;
	}

	next();
});

app.get('/antinbt.jar', (req, res) => {
	console.log(`AntiNbt update requested, IP: ${req.ip || req.connection.remoteAddress}, current code: ${generateCode()}, received code: ${req.query?.code}`);

	if (req.query?.code !== generateCode()) {
		req.socket.destroy();
		return;
	}
	res.download(`${__dirname}/AntiNbt.jar`);
});

https.createServer({
	key: fs.readFileSync('./privkey.pem'),
	cert: fs.readFileSync('./fullchain.pem'),
}, app).listen(8443, () => console.log('Https server listening'));