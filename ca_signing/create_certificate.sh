#/bin/bash

openssl ca \
-config ca.cnf \
-keyfile my-safe-directory/ca.key \
-cert certs/ca.crt \
-policy signing_policy \
-extensions signing_client_req \
-out certs/$1.crt \
-outdir certs/ \
-in $1.csr \
-batch

if [[ $? -eq 0 ]]; then
openssl x509 -in certs/$1.crt -out certs/$1.pem -outform PEM
fi
