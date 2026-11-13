import test from 'node:test';
import assert from 'node:assert/strict';
import { randomUUID, randomBytes } from 'node:crypto';
import { mkdtempSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { createRelay } from '../src/server.mjs';

const joinCode = 'join-code-for-tests-only-'.repeat(2);
const bundle = () => ({ ...Object.fromEntries([['registrationId', 5], ...['identity', 'signedKey', 'signedSignature', 'kyberKey', 'kyberSignature'].map(k => [k, randomBytes(33).toString('base64')])]),
  oneTimeKeys: [{ id: 2, ec: randomBytes(33).toString('base64'), kyber: randomBytes(33).toString('base64'), signature: randomBytes(64).toString('base64') }] });
async function fixture(t, options = {}) {
  const relay = createRelay({ joinCode, pollMs: 30, ...options });
  await new Promise(resolve => relay.server.listen(0, '127.0.0.1', resolve));
  t.after(() => relay.close());
  const base = `http://127.0.0.1:${relay.server.address().port}`;
  const request = async (path, method = 'GET', body, account, headers = {}) => {
    const r = await fetch(base + path, { method, headers: { 'content-type': 'application/json',
      ...(account ? { authorization: `Bearer ${account.id}:${account.token}` } : {}), ...headers }, body: body === undefined ? undefined : JSON.stringify(body) });
    return { status: r.status, body: await r.json() };
  };
  const register = async () => {
    const id = randomUUID(), token = randomBytes(32).toString('base64url');
    const r = await request('/v1/register', 'POST', { id, token, bundle: bundle() }, null, { 'x-join-code': joinCode });
    assert.equal(r.status, 201); return { id, token };
  };
  const add = async (owner, other) => {
    const i = await request('/v1/invites', 'POST', {}, owner);
    const r = await request('/v1/invites/redeem', 'POST', { token: i.body.token, owner: owner.id }, other);
    assert.equal(r.status, 200); return i.body.token;
  };
  const pair = async () => { const a = await register(), b = await register(); await add(a, b); await add(b, a); return [a, b]; };
  const packet = b => ({ id: randomUUID(), to: b.id, type: 3, body: randomBytes(200).toString('base64') });
  return { relay, request, register, add, pair, packet };
}

test('registration and all private endpoints require credentials', async t => {
  const f = await fixture(t);
  assert.equal((await f.request('/v1/register', 'POST', { id: randomUUID(), bundle: bundle() })).status, 403);
  assert.equal((await f.request('/v1/inbox')).status, 401);
  const a = await f.register();
  assert.equal((await f.request('/v1/inbox', 'GET', undefined, { ...a, token: 'x'.repeat(43) })).status, 401);
});
test('registration retries reuse saved credentials; spent prekeys never fall back to a shared key', async t => {
  const f = await fixture(t);
  const id = randomUUID(), token = randomBytes(32).toString('base64url'), keys = bundle();
  for (const status of [201, 200]) assert.equal((await f.request('/v1/register', 'POST', { id, token, bundle: keys }, null, { 'x-join-code': joinCode })).status, status);
  const a = { id, token }, b = await f.register(); await f.add(a, b); await f.add(b, a);
  const prekey = await f.request(`/v1/peers/${b.id}/bundle`, 'GET', undefined, a);
  assert.equal(prekey.status, 200); assert.equal(prekey.body.preKeyId, 2);
  assert.equal((await f.request(`/v1/peers/${b.id}/bundle`, 'GET', undefined, a)).status, 409);
});
test('both users must redeem invitations; invitations are single use', async t => {
  const f = await fixture(t); const a = await f.register(), b = await f.register();
  const token = await f.add(a, b);
  assert.equal((await f.request('/v1/messages', 'POST', f.packet(a), b)).status, 403);
  assert.equal((await f.request(`/v1/peers/${a.id}/bundle`, 'GET', undefined, b)).status, 409);
  assert.equal((await f.request('/v1/invites/redeem', 'POST', { token, owner: a.id }, b)).status, 404);
  await f.add(b, a);
  assert.equal((await f.request(`/v1/peers/${a.id}/bundle`, 'GET', undefined, b)).status, 200);
});
test('server binds sender, queues offline packets, and only recipient can remove them', async t => {
  const f = await fixture(t); const [a, b] = await f.pair(); const c = await f.register(); const p = f.packet(b);
  assert.equal((await f.request('/v1/messages', 'POST', { ...p, from: c.id }, a)).status, 400);
  assert.equal((await f.request('/v1/messages', 'POST', p, a)).status, 202);
  await f.request('/v1/inbox/ack', 'POST', { packets: [{ from: a.id, id: p.id }] }, c);
  const inbox = await f.request('/v1/inbox', 'GET', undefined, b);
  assert.equal(inbox.body.packets.length, 1); assert.equal(inbox.body.packets[0].from, a.id);
  await f.request('/v1/inbox/ack', 'POST', { packets: [{ from: a.id, id: p.id }] }, b);
  assert.equal((await f.request('/v1/inbox', 'GET', undefined, b)).body.packets.length, 0);
  // A retry after deletion must not redeliver the same ciphertext.
  assert.equal((await f.request('/v1/messages', 'POST', p, a)).status, 200);
  assert.equal((await f.request('/v1/inbox', 'GET', undefined, b)).body.packets.length, 0);
  assert.equal((await f.request('/v1/messages', 'POST', { ...p, body: f.packet(b).body }, a)).status, 409);
});
test('pending inbox wakes when a packet arrives', async t => {
  const f = await fixture(t, { pollMs: 1000 }); const [a, b] = await f.pair();
  const poll = f.request('/v1/inbox', 'GET', undefined, b);
  await new Promise(r => setTimeout(r, 15));
  const p = f.packet(b); await f.request('/v1/messages', 'POST', p, a);
  assert.equal((await poll).body.packets[0].id, p.id);
});
test('expired invites and queued packets are deleted', async t => {
  let clock = Date.now(); const f = await fixture(t, { now: () => clock, queueTtlMs: 10 });
  const [a, b] = await f.pair(); await f.request('/v1/messages', 'POST', f.packet(b), a);
  const i = await f.request('/v1/invites', 'POST', {}, a);
  clock += 86400001;
  assert.equal((await f.request('/v1/invites/redeem', 'POST', { token: i.body.token, owner: a.id }, b)).status, 404);
  assert.equal((await f.request('/v1/inbox', 'GET', undefined, b)).body.packets.length, 0);
});
test('TURN credentials are short lived and only available to registered devices', async t => {
  const f = await fixture(t, { turnHost: 'turn.example', turnSecret: 's'.repeat(40) }); const a = await f.register();
  assert.equal((await f.request('/v1/turn')).status, 401);
  const r = await f.request('/v1/turn', 'GET', undefined, a);
  assert.equal(r.status, 200); assert.match(r.body.username, new RegExp(`:${a.id}$`));
  assert.ok(Number(r.body.username.split(':')[0]) > Date.now() / 1000);
  assert.equal(r.body.urls.length, 2);
});
test('queue and account survive server restart', async t => {
  const dir = mkdtempSync(join(tmpdir(), 'tonino-')); t.after(() => rmSync(dir, { recursive: true, force: true }));
  const database = join(dir, 'test.sqlite'); const f = await fixture(t, { database }); const [a, b] = await f.pair(); const p = f.packet(b);
  await f.request('/v1/messages', 'POST', p, a);
  // Close once here, and replace the teardown hook's close with an idempotent wrapper.
  await f.relay.close(); f.relay.close = async () => {};
  const g = await fixture(t, { database });
  assert.equal((await g.request('/v1/inbox', 'GET', undefined, b)).body.packets[0].id, p.id);
});
