#!/usr/bin/env bash
# Generates a local CA + server cert + client cert in the same layout NAIS mounts:
#   root-cert.pem   CA (postgres trusts this for client certs; client trusts this for server)
#   server.crt      server cert signed by CA (CN=localhost)
#   server.key      server private key (PEM)
#   cert.pem        client cert signed by CA
#   key.pem         client private key, PKCS#1 **RSA PEM** (matches NAIS sqlcertificate — SSLKEY,
#                   `-----BEGIN RSA PRIVATE KEY-----`; Netty's PemReader can't parse it as PKCS#8)
#   key.pk8         client private key, PKCS#8 **DER** (matches NAIS sqlcertificate — SSLKEY_PK8)
#
# Client username in pg_hba is enforced by password (scram-sha-256) and the cert chain;
# CN on the client cert is informational here.
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
dir="$here/certs"
mkdir -p "$dir"
cd "$dir"

# --- CA ---
openssl req -x509 -newkey rsa:2048 -nodes -days 365 \
  -keyout ca.key -out root-cert.pem \
  -subj "/CN=local-nais-ca" \
  -addext "basicConstraints=critical,CA:TRUE" >/dev/null 2>&1

# --- Server ---
openssl req -newkey rsa:2048 -nodes \
  -keyout server.key -out server.csr \
  -subj "/CN=localhost" >/dev/null 2>&1

openssl x509 -req -in server.csr -CA root-cert.pem -CAkey ca.key -CAcreateserial \
  -out server.crt -days 365 \
  -extfile <(printf "subjectAltName=DNS:localhost,IP:127.0.0.1\nextendedKeyUsage=serverAuth") \
  >/dev/null 2>&1

# --- Client ---
openssl req -newkey rsa:2048 -nodes \
  -keyout client.key -out client.csr \
  -subj "/CN=user" >/dev/null 2>&1

openssl x509 -req -in client.csr -CA root-cert.pem -CAkey ca.key -CAcreateserial \
  -out cert.pem -days 365 \
  -extfile <(printf "extendedKeyUsage=clientAuth") \
  >/dev/null 2>&1

# NAIS ships key.pem as PKCS#1 RSA PEM and key.pk8 as PKCS#8 DER generate with -traditional
openssl rsa   -in client.key -out key.pem -traditional 2>/dev/null
openssl pkcs8 -topk8 -inform PEM -outform DER -in client.key -out key.pk8 -nocrypt

rm -f server.csr client.csr client.key root-cert.srl ca.key

# --- pg_hba ---
cat >pg_hba.conf <<'EOF'
# TYPE  DATABASE   USER   ADDRESS       METHOD
local   all        all                  trust
hostssl all        all    0.0.0.0/0     scram-sha-256 clientcert=verify-ca
hostssl all        all    ::/0          scram-sha-256 clientcert=verify-ca
host    all        all    all           reject
EOF

echo "Generated in $dir:"
ls -la "$dir"
