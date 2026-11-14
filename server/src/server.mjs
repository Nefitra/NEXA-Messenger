import http from 'node:http';
import { DatabaseSync } from 'node:sqlite';
import { randomBytes, createHash, createHmac, timingSafeEqual } from 'node:crypto';
import { pathToFileURL } from 'node:url';

const hash = s => createHash('sha256').update(s).digest('hex');
const secret = () => randomBytes(32).toString('base64url');
const uuid = s => typeof s === 'string' && /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(s);
function fail(status, message) { throw Object.assign(new Error(message), { status }); }
function check(condition, status, message) { if (!condition) fail(status, message); }
function validBundle(b) {
  if (!b || !Number.isInteger(b.registrationId) || b.registrationId < 1 || b.registrationId > 16380) return false;
  const encoded = x => typeof x === 'string' && x.length > 20 && x.length < 5000 && /^[A-Za-z0-9+/]+=*$/.test(x);
  return ['identity', 'signedKey', 'signedSignature', 'kyberKey', 'kyberSignature'].every(k => encoded(b[k])) &&
    Array.isArray(b.oneTimeKeys) && b.oneTimeKeys.length >= 1 && b.oneTimeKeys.length <= 16 &&
    new Set(b.oneTimeKeys.map(k => k.id)).size === b.oneTimeKeys.length &&
    b.oneTimeKeys.every(k => Number.isInteger(k.id) && k.id > 1 && k.id < 0x7fffffff && ['ec','kyber','signature'].every(x => encoded(k[x])));
}

export function createRelay({ database = ':memory:', joinCode, turnHost = '', turnSecret = '', now = Date.now,
  queueTtlMs = 7 * 86400000, pollMs = 20000 } = {}) {
  check(typeof joinCode === 'string' && joinCode.length >= 32, 500, 'JOIN_CODE must contain at least 32 characters');
  const db = new DatabaseSync(database);
  db.exec(`PRAGMA journal_mode=WAL; PRAGMA foreign_keys=ON;
    CREATE TABLE IF NOT EXISTS accounts(id TEXT PRIMARY KEY, token TEXT NOT NULL, bundle TEXT NOT NULL);
    CREATE TABLE IF NOT EXISTS invites(token TEXT PRIMARY KEY, owner TEXT NOT NULL REFERENCES accounts(id), expires INTEGER NOT NULL);
    CREATE TABLE IF NOT EXISTS peers(owner TEXT NOT NULL REFERENCES accounts(id), peer TEXT NOT NULL REFERENCES accounts(id), PRIMARY KEY(owner,peer));
    CREATE TABLE IF NOT EXISTS packets(sender TEXT NOT NULL, id TEXT NOT NULL, recipient TEXT NOT NULL,
      digest TEXT NOT NULL, packet TEXT, expires INTEGER NOT NULL, PRIMARY KEY(sender,id));
    CREATE INDEX IF NOT EXISTS packet_recipient ON packets(recipient,expires);`);
  const polls = new Map();
  const limits = new Map();
  const stmt = sql => db.prepare(sql);
  const send = (res, status, body) => {
    if (res.destroyed || res.writableEnded) return;
    res.writeHead(status, { 'content-type': 'application/json; charset=utf-8', 'cache-control': 'no-store',
      'x-content-type-options': 'nosniff', 'referrer-policy': 'no-referrer' });
    res.end(JSON.stringify(body));
  };
  const clean = () => {
    stmt('DELETE FROM invites WHERE expires<=?').run(now());
    stmt('DELETE FROM packets WHERE expires<=?').run(now());
    for (const [key, v] of limits) if (v.reset <= now()) limits.delete(key);
  };
  const rate = (key, max, period = 60000) => {
    let v = limits.get(key);
    if (!v || v.reset <= now()) { v = { count: 0, reset: now() + period }; limits.set(key, v); }
    check(++v.count <= max, 429, 'Too many requests');
  };
  const inbox = id => stmt('SELECT packet FROM packets WHERE recipient=? AND packet IS NOT NULL AND expires>? ORDER BY rowid LIMIT 100')
    .all(id, now()).map(r => JSON.parse(r.packet));
  const wake = id => {
    const p = polls.get(id);
    if (p) { polls.delete(id); clearTimeout(p.timer); send(p.res, 200, { packets: inbox(id) }); }
  };
  async function body(req) {
    check(req.headers['content-type']?.split(';')[0] === 'application/json', 415, 'JSON required');
    let length = 0; const chunks = [];
    for await (const b of req) { length += b.length; check(length <= 65536, 413, 'Payload too large'); chunks.push(b); }
    try { return JSON.parse(Buffer.concat(chunks).toString('utf8')); } catch { fail(400, 'Invalid JSON'); }
  }
  const server = http.createServer(async (req, res) => {
    try {
      const url = new URL(req.url, 'http://relay');
      check(!url.search && !req.headers.origin, 400, 'Unexpected request parameters');
      if (req.method === 'GET' && url.pathname === '/healthz') return send(res, 200, { status: 'ok', protocol: 1 });
      clean();
      if (req.method === 'POST' && url.pathname === '/v1/register') {
        rate(`register:${req.socket.remoteAddress}`, 10, 3600000);
        const supplied = hash(String(req.headers['x-join-code'] || ''));
        check(timingSafeEqual(Buffer.from(supplied), Buffer.from(hash(joinCode))), 403, 'Registration code rejected');
        const b = await body(req);
        check(uuid(b.id) && validBundle(b.bundle) && typeof b.token === 'string' && /^[A-Za-z0-9_-]{43}$/.test(b.token), 400, 'Invalid registration');
        const previous = stmt('SELECT token,bundle FROM accounts WHERE id=?').get(b.id);
        if (previous) {
          check(previous.token === hash(b.token) && JSON.parse(previous.bundle).identity === b.bundle.identity, 409, 'Device already exists');
          return send(res, 200, { registered: true });
        }
        check(stmt('SELECT COUNT(*) AS n FROM accounts').get().n < 200, 409, 'Test server is full');
        stmt('INSERT INTO accounts(id,token,bundle) VALUES(?,?,?)').run(b.id, hash(b.token), JSON.stringify(b.bundle));
        return send(res, 201, { registered: true });
      }
      const auth = /^Bearer ([0-9a-f-]+):([A-Za-z0-9_-]{43})$/i.exec(req.headers.authorization || '');
      check(auth && uuid(auth[1]), 401, 'Authentication required');
      const me = auth[1];
      check(!!stmt('SELECT id FROM accounts WHERE id=? AND token=?').get(me, hash(auth[2])), 401, 'Authentication rejected');
      rate(`account:${me}`, 1200);
      if (req.method === 'GET' && url.pathname === '/v1/status') return send(res, 200, { ok: true });
      if (req.method === 'POST' && url.pathname === '/v1/invites') {
        rate(`invites:${me}`, 30, 3600000);
        const token = secret(), expires = now() + 86400000;
        stmt('DELETE FROM invites WHERE owner=?').run(me);
        stmt('INSERT INTO invites VALUES(?,?,?)').run(hash(token), me, expires);
        return send(res, 201, { token, expires });
      }
      if (req.method === 'POST' && url.pathname === '/v1/invites/redeem') {
        rate(`redeem:${me}`, 30, 3600000);
        const b = await body(req);
        check(typeof b.token === 'string' && /^[A-Za-z0-9_-]{43}$/.test(b.token), 400, 'Invalid invitation');
        const invite = stmt('SELECT owner FROM invites WHERE token=? AND expires>?').get(hash(b.token), now());
        check(invite && invite.owner !== me, 404, 'Invitation expired or already used');
        check(b.owner === invite.owner, 400, 'Invitation owner mismatch');
        check(stmt('SELECT COUNT(*) AS n FROM peers WHERE owner=?').get(me).n < 50, 409, 'Contact limit reached');
        db.exec('BEGIN IMMEDIATE');
        try {
          stmt('INSERT OR IGNORE INTO peers VALUES(?,?)').run(me, invite.owner);
          stmt('DELETE FROM invites WHERE token=?').run(hash(b.token));
          db.exec('COMMIT');
        } catch (e) { db.exec('ROLLBACK'); throw e; }
        return send(res, 200, { id: invite.owner, identity: JSON.parse(stmt('SELECT bundle FROM accounts WHERE id=?').get(invite.owner).bundle).identity });
      }
      if (req.method === 'GET' && /^\/v1\/peers\/[0-9a-f-]+\/bundle$/i.test(url.pathname)) {
        const peer = url.pathname.split('/')[3];
        check(!!stmt('SELECT peer FROM peers WHERE owner=? AND peer=?').get(me, peer) &&
          !!stmt('SELECT peer FROM peers WHERE owner=? AND peer=?').get(peer, me), 409, 'Both people must add each other');
        rate(`bundle:${me}:${peer}`, 5);
        const bundle = JSON.parse(stmt('SELECT bundle FROM accounts WHERE id=?').get(peer).bundle);
        check(bundle.oneTimeKeys?.length, 409, 'Recipient one-time keys exhausted; test server needs maintenance');
        const key = bundle.oneTimeKeys.shift();
        stmt('UPDATE accounts SET bundle=? WHERE id=?').run(JSON.stringify(bundle), peer);
        const { oneTimeKeys, ...publicBundle } = bundle;
        return send(res, 200, { ...publicBundle, preKeyId: key.id, preKey: key.ec,
          kyberId: key.id, kyberKey: key.kyber, kyberSignature: key.signature });
      }
      if (req.method === 'POST' && url.pathname === '/v1/messages') {
        const b = await body(req);
        check(uuid(b.id) && uuid(b.to) && [2, 3].includes(b.type) && typeof b.body === 'string' && b.body.length <= 48000 &&
          b.body.length > 10 && /^[A-Za-z0-9+/]+=*$/.test(b.body), 400, 'Invalid encrypted packet');
        check(Object.keys(b).sort().join(',') === 'body,id,to,type', 400, 'Unexpected packet fields');
        check(!!stmt('SELECT peer FROM peers WHERE owner=? AND peer=?').get(me, b.to) &&
          !!stmt('SELECT peer FROM peers WHERE owner=? AND peer=?').get(b.to, me), 403, 'Contact is not mutually approved');
        const packet = { ...b, from: me };
        const digest = hash(JSON.stringify([b.id, b.to, b.type, b.body]));
        const previous = stmt('SELECT digest FROM packets WHERE sender=? AND id=?').get(me, b.id);
        if (previous) {
          check(previous.digest === digest, 409, 'Packet ID already used for different content');
          return send(res, 200, { accepted: b.id });
        }
        check(stmt('SELECT COUNT(*) AS n FROM packets WHERE recipient=? AND packet IS NOT NULL').get(b.to).n < 1000, 429, 'Recipient queue full');
        stmt('INSERT INTO packets VALUES(?,?,?,?,?,?)').run(me, b.id, b.to, digest, JSON.stringify(packet), now() + queueTtlMs);
        wake(b.to);
        return send(res, 202, { accepted: b.id });
      }
      if (req.method === 'GET' && url.pathname === '/v1/inbox') {
        const packets = inbox(me);
        if (packets.length) return send(res, 200, { packets });
        check(!polls.has(me), 409, 'A device may only have one inbox connection');
        const timer = setTimeout(() => { if (polls.get(me)?.res === res) { polls.delete(me); send(res, 200, { packets: [] }); } }, pollMs);
        polls.set(me, { res, timer });
        res.on('close', () => { if (polls.get(me)?.res === res) { clearTimeout(timer); polls.delete(me); } });
        return;
      }
      if (req.method === 'POST' && url.pathname === '/v1/inbox/ack') {
        const b = await body(req);
        check(Array.isArray(b.packets) && b.packets.length <= 100 && b.packets.every(p => uuid(p.from) && uuid(p.id)), 400, 'Invalid acknowledgement');
        const del = stmt('UPDATE packets SET packet=NULL WHERE recipient=? AND sender=? AND id=?');
        db.exec('BEGIN IMMEDIATE');
        try { for (const p of b.packets) del.run(me, p.from, p.id); db.exec('COMMIT'); }
        catch (e) { db.exec('ROLLBACK'); throw e; }
        return send(res, 200, { ok: true });
      }
      if (req.method === 'GET' && url.pathname === '/v1/turn') {
        check(turnHost && turnSecret.length >= 32, 503, 'TURN is not configured');
        const username = `${Math.floor(now() / 1000) + 3600}:${me}`;
        const credential = createHmac('sha1', turnSecret).update(username).digest('base64');
        return send(res, 200, { username, credential, urls: [`turn:${turnHost}:3478?transport=udp`, `turn:${turnHost}:3478?transport=tcp`] });
      }
      fail(404, 'Not found');
    } catch (e) { send(res, e.status || 500, { error: e.status ? e.message : 'Server error' }); }
  });
  server.requestTimeout = 30000;
  server.headersTimeout = 10000;
  server.maxHeadersCount = 30;
  const close = async () => {
    for (const [id, p] of polls) { clearTimeout(p.timer); send(p.res, 503, { error: 'Server restarting' }); polls.delete(id); }
    server.closeAllConnections();
    if (server.listening) await new Promise(resolve => server.close(resolve));
    db.close();
  };
  return { server, close, db };
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const relay = createRelay({ database: process.env.DATABASE_PATH || '/data/tonino.sqlite', joinCode: process.env.JOIN_CODE,
    turnHost: process.env.TURN_HOST, turnSecret: process.env.TURN_SECRET });
  relay.server.listen(Number(process.env.PORT || 8080), process.env.BIND_ADDRESS || '127.0.0.1', () => console.log('Tonino relay listening'));
  for (const signal of ['SIGINT', 'SIGTERM']) process.on(signal, () => { relay.close().then(() => process.exit(0)); });
}
