# Security Policy

## Never commit secrets

This repository must never contain signing material or private data, including:

- upload or release keystores (`.jks`, `.keystore`, `.p12`) and their passwords;
- `keystore.properties`, `local.properties`;
- Google Play service-account JSON, `google-services.json`, API keys;
- GitHub personal access tokens or other credentials;
- exported scan results, logs or screenshots of real networks you do not own.

Use `keystore.properties.example` only as a template. Keep the real keystore outside the project folder.

## Before every push

Run:

```bash
python scripts/security_scan.py .
```

The GitHub Actions workflow runs the same scanner on push and pull request.

## If a secret is exposed

1. Revoke or rotate it immediately. For a leaked upload key, request an upload-key reset in Play Console.
2. Remove it from the current tree.
3. Rewrite Git history if it was committed; deleting the latest file is not enough.
4. Re-run the scanner before pushing again.

## App privacy

The app has no network access and collects no data. Scan results stay in memory on the phone. See [docs/PRIVACY.md](docs/PRIVACY.md).

## Reporting

Please report security issues privately to the repository owner rather than publishing active credentials in an issue.

繁體中文版本：[docs/SECURITY.zh-TW.md](docs/SECURITY.zh-TW.md)
