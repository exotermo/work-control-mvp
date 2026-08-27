#!/usr/bin/env python3
"""Smoke local do Authorization Code + PKCE sem imprimir tokens.

Este utilitário automatiza somente o formulário do usuário público criado pelo
realm local. Ele não deve ser apontado para homologação ou produção.
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import html
import http.cookiejar
import json
import secrets
import sys
import urllib.error
import urllib.parse
import urllib.request
from html.parser import HTMLParser


class LoginFormParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__()
        self.action: str | None = None

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        if tag != "form" or self.action is not None:
            return
        values = dict(attrs)
        action = values.get("action")
        if values.get("id") == "kc-form-login" and action:
            self.action = html.unescape(action)


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):  # noqa: ANN001
        return None


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--issuer",
        default="http://localhost:8180/realms/work-control-local",
    )
    parser.add_argument("--api-url", default="http://localhost:8081")
    parser.add_argument("--client-id", default="work-control-android-local")
    parser.add_argument(
        "--redirect-uri",
        default="com.workcontrol.app.debug:/oauth2redirect",
    )
    parser.add_argument("--username", default="local-developer")
    parser.add_argument("--password", default="local-only-password")
    parser.add_argument("--debug", action="store_true")
    return parser.parse_args()


def load_json(opener: urllib.request.OpenerDirector, request: str | urllib.request.Request) -> dict:
    with opener.open(request, timeout=10) as response:
        return json.load(response)


def main() -> int:
    args = parse_args()
    for label, value in (("issuer", args.issuer), ("API", args.api_url)):
        parsed = urllib.parse.urlparse(value)
        if parsed.scheme != "http" or parsed.hostname not in {"localhost", "127.0.0.1"}:
            raise RuntimeError(f"este smoke aceita somente {label} HTTP de loopback")

    cookies = http.cookiejar.CookieJar()
    opener = urllib.request.build_opener(
        urllib.request.HTTPCookieProcessor(cookies), NoRedirect()
    )
    discovery = load_json(opener, args.issuer + "/.well-known/openid-configuration")
    if discovery.get("issuer") != args.issuer:
        raise RuntimeError("issuer do discovery difere do issuer configurado")

    verifier = secrets.token_urlsafe(64)
    challenge = base64.urlsafe_b64encode(hashlib.sha256(verifier.encode()).digest()).rstrip(b"=").decode()
    state = secrets.token_urlsafe(24)
    nonce = secrets.token_urlsafe(24)
    auth_query = urllib.parse.urlencode(
        {
            "client_id": args.client_id,
            "redirect_uri": args.redirect_uri,
            "response_type": "code",
            "scope": "openid profile email work-control.api",
            "state": state,
            "nonce": nonce,
            "code_challenge": challenge,
            "code_challenge_method": "S256",
        }
    )
    with opener.open(discovery["authorization_endpoint"] + "?" + auth_query, timeout=10) as response:
        login_page = response.read().decode()

    # Navegadores tratam localhost como contexto potencialmente confiável e enviam os
    # cookies Secure usados pelo Keycloak mesmo nesta exceção HTTP local. O CookieJar da
    # stdlib não implementa essa exceção; ela é reproduzida aqui somente após o gate de
    # loopback acima. Nenhum ambiente remoto é aceito por este utilitário.
    for cookie in cookies:
        cookie.secure = False

    form = LoginFormParser()
    form.feed(login_page)
    if form.action is None:
        raise RuntimeError("formulário de login do Keycloak não encontrado")
    if args.debug:
        form_url = urllib.parse.urlparse(form.action)
        cookie_metadata = [
            f"{cookie.name}@{cookie.domain}{cookie.path};secure={cookie.secure}"
            for cookie in cookies
        ]
        print(f"login_form={form_url.scheme}://{form_url.netloc}{form_url.path}")
        print("cookies=" + ",".join(cookie_metadata))

    login_data = urllib.parse.urlencode(
        {"username": args.username, "password": args.password, "credentialId": ""}
    ).encode()
    login_request = urllib.request.Request(form.action, data=login_data)
    try:
        opener.open(login_request, timeout=10)
        raise RuntimeError("Keycloak não redirecionou após o login")
    except urllib.error.HTTPError as error:
        if error.code not in {302, 303}:
            raise RuntimeError(f"formulário de login retornou HTTP {error.code}") from error
        location = error.headers.get("Location", "")

    callback = urllib.parse.urlparse(location)
    callback_query = urllib.parse.parse_qs(callback.query)
    if callback_query.get("state") != [state] or "code" not in callback_query:
        raise RuntimeError("callback OIDC inválido ou sem authorization code")

    token_data = urllib.parse.urlencode(
        {
            "grant_type": "authorization_code",
            "client_id": args.client_id,
            "redirect_uri": args.redirect_uri,
            "code": callback_query["code"][0],
            "code_verifier": verifier,
        }
    ).encode()
    try:
        token_response = load_json(
            opener,
            urllib.request.Request(discovery["token_endpoint"], data=token_data),
        )
    except urllib.error.HTTPError as error:
        raise RuntimeError(f"token endpoint retornou HTTP {error.code}") from error
    access_token = token_response.get("access_token")
    if not access_token:
        raise RuntimeError("token endpoint não retornou access_token")
    if args.debug:
        encoded_claims = access_token.split(".")[1]
        encoded_claims += "=" * (-len(encoded_claims) % 4)
        token_claims = json.loads(base64.urlsafe_b64decode(encoded_claims))
        safe_claims = {
            key: token_claims.get(key)
            for key in ("iss", "sub", "aud", "exp", "scope", "typ", "azp")
        }
        print("access_token_claims=" + json.dumps(safe_claims, sort_keys=True))

    unauthenticated = urllib.request.Request(args.api_url + "/v1/me")
    try:
        opener.open(unauthenticated, timeout=10)
        raise RuntimeError("API OIDC aceitou /v1/me sem Bearer token")
    except urllib.error.HTTPError as error:
        if error.code != 401:
            raise

    me_request = urllib.request.Request(
        args.api_url + "/v1/me",
        headers={"Authorization": "Bearer " + access_token},
    )
    me = load_json(opener, me_request)
    user_id = me.get("user", {}).get("id")
    memberships = me.get("workspaces", [])
    if not user_id or not memberships:
        raise RuntimeError("GET /v1/me não retornou usuário e workspace vinculados")

    print(f"OIDC local smoke: OK (user={user_id}, workspaces={len(memberships)})")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as error:  # noqa: BLE001
        print(f"OIDC local smoke: FALHOU: {error}", file=sys.stderr)
        raise SystemExit(1)
