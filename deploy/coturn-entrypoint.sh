#!/bin/sh
set -eu
umask 077
# Bridge addresses can change after recreation. Rebuild the public/private mapping
# inside this container, preserving the private peer deny rules and shared secret.
private_ip="$(hostname -i | awk '{print $1}')"
public_ip="$(sed -n 's/^external-ip=//p' /etc/coturn/turnserver.conf | head -n 1 | cut -d / -f 1)"
case "$public_ip/$private_ip" in
  *[!0-9./]*|*/*/*) echo 'Invalid TURN address mapping' >&2; exit 1 ;;
esac
test -n "$public_ip" && test -n "$private_ip"
sed '/^listening-ip=/d; /^relay-ip=/d; /^external-ip=/d' /etc/coturn/turnserver.conf > /tmp/tonino-turn.conf
printf '\nlistening-ip=%s\nrelay-ip=%s\nexternal-ip=%s/%s\n' "$private_ip" "$private_ip" "$public_ip" "$private_ip" >> /tmp/tonino-turn.conf
exec /usr/bin/turnserver -c /tmp/tonino-turn.conf
