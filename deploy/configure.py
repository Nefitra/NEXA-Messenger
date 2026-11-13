#!/usr/bin/env python3
"""Generate private test-server configuration; never overwrite existing keys or data."""
import argparse
import ipaddress
import os
from pathlib import Path
import re
import secrets

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("domain", help="DNS name pointing directly to the VPS")
    parser.add_argument("ipv4", help="Public IPv4 of the VPS")
    parser.add_argument("--shared-proxy", action="store_true", help="Use an existing Caddy ingress; public IPv4 HTTPS is supported")
    args = parser.parse_args()
    domain = args.domain.lower()
    address = ipaddress.IPv4Address(args.ipv4)
    if not address.is_global:
        parser.error("A public IPv4 address is required")
    is_ip = domain == str(address)
    if is_ip and not args.shared_proxy:
        parser.error("Public IPv4 HTTPS requires --shared-proxy and its ACME profile configuration")
    if not is_ip and not re.fullmatch(r"(?=.{4,253}$)(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z]{2,63}", domain):
        parser.error("Enter a DNS name or the same public IPv4, without scheme or path")
    folder = Path(__file__).resolve().parent
    env_file = folder / ".env"
    turn_file = folder / "turnserver.conf"
    shared_file = folder / "Caddyfile.shared"
    if env_file.exists() or turn_file.exists() or (args.shared_proxy and shared_file.exists()):
        parser.error("Configuration already exists; keys were not changed")
    os.umask(0o077)
    folder.chmod(0o700)
    join_code, turn_secret = secrets.token_urlsafe(32), secrets.token_urlsafe(48)
    env_file.write_text(f"TONINO_DOMAIN={domain}\nJOIN_CODE={join_code}\nTURN_SECRET={turn_secret}\n")
    turn_file.write_text(f"""listening-port=3478
listening-ip=0.0.0.0
external-ip={address}
realm={domain}
fingerprint
use-auth-secret
static-auth-secret={turn_secret}
min-port=49160
max-port=49200
user-quota=4
total-quota=40
max-bps=128000
no-cli
no-tls
no-dtls
no-multicast-peers
no-tcp-relay
denied-peer-ip=0.0.0.0-0.255.255.255
denied-peer-ip=10.0.0.0-10.255.255.255
denied-peer-ip=100.64.0.0-100.127.255.255
denied-peer-ip=127.0.0.0-127.255.255.255
denied-peer-ip=169.254.0.0-169.254.255.255
denied-peer-ip=172.16.0.0-172.31.255.255
denied-peer-ip=192.168.0.0-192.168.255.255
denied-peer-ip=224.0.0.0-255.255.255.255
denied-peer-ip=::1
denied-peer-ip=fc00::-fdff:ffff:ffff:ffff:ffff:ffff:ffff:ffff
denied-peer-ip=fe80::-febf:ffff:ffff:ffff:ffff:ffff:ffff:ffff
log-file=stdout
simple-log
no-stdout-log
""")
    # Bind-mounted files are consumed by the unprivileged coturn image.
    # The directory itself remains private to its owner/root.
    turn_file.chmod(0o644)
    data = folder / "data"
    data.mkdir(mode=0o700, exist_ok=True)
    if os.geteuid() == 0:
        os.chown(data, 1000, 1000)
    if args.shared_proxy:
        tls = """  tls {
    issuer acme {
      dir https://acme-v02.api.letsencrypt.org/directory
      profile shortlived
    }
  }
""" if is_ip else ""
        shared_file.write_text(f"""https://{domain} {{
{tls}  encode zstd gzip
  request_body {{
    max_size 64KB
  }}
  header {{
    Strict-Transport-Security \"max-age=31536000\"
    X-Content-Type-Options nosniff
    Referrer-Policy no-referrer
  }}
  reverse_proxy tonino-relay:8080
}}
""")
    print("Created .env, turnserver.conf and data/. Keep .env private.")

if __name__ == "__main__":
    main()
