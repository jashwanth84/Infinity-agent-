# GitHub Actions Release APK Workflow Guide

This repository includes an automated GitHub Actions workflow (`.github/workflows/release-apk.yml`) to build, sign, and publish your Android release APK directly to **GitHub Releases**.

---

## 🚀 How to Trigger a Release

### Option 1: Trigger Automatically via Git Tag (Recommended)

Whenever you push a version tag starting with `v` (e.g. `v1.0.0`), the workflow automatically runs, builds the release APK, and creates a GitHub Release:

```bash
git tag v1.0.0
git push origin v1.0.0
```

### Option 2: Trigger Manually from the GitHub Web UI

1. Go to your repository on **GitHub**.
2. Click on the **Actions** tab.
3. Select **Release APK** from the left workflow list.
4. Click the **Run workflow** dropdown button:
   - **Release Tag**: e.g., `v1.0.0` (or leave blank to auto-generate from the run number).
   - **Release Title**: e.g., `Infinity Agent v1.0.0`.
   - **Draft**: Check if you want to review the release before publishing.
   - **Prerelease**: Check if this is a beta or release candidate.
5. Click **Run workflow**.

---

## 📦 What the Workflow Produces

1. **GitHub Release Assets**:
   - `InfinityAgent-<tag>.apk` — The production-ready, signed Android APK ready to download and install.
   - `InfinityAgent-<tag>.apk.sha256` — SHA256 checksum file to verify APK integrity.
2. **GitHub Actions Artifacts**:
   - Downloadable from the summary page of any workflow run (retained for 30 days).

---

## 🔐 Optional: Custom Signing Keystore & Secrets

By default, if no custom secrets are provided, the workflow generates a self-signed upload keystore on the fly so the build **never fails**.

To sign with your own production release keystore:

1. In your GitHub repository, go to **Settings** > **Secrets and variables** > **Actions**.
2. Add the following repository secrets:

| Secret Name | Description | Example / Instructions |
|---|---|---|
| `RELEASE_KEYSTORE_BASE64` | Base64-encoded `.jks` file | Run `base64 -w 0 my-upload-key.jks` and paste the string |
| `STORE_PASSWORD` | Password for your keystore | Your keystore password |
| `KEY_PASSWORD` | Password for the key alias | Your alias key password |
| `GEMINI_API_KEY` | Optional Gemini API key | App API key injected into `.env` |
| `ENV_FILE` | Full `.env` file contents (optional) | Multi-line key/values to inject |

---

## 🛠 Testing the Build Locally

You can test building the release APK locally at any time:

```bash
# Set dummy signing variables or your own keystore:
KEYSTORE_PATH="$(pwd)/debug.keystore" STORE_PASSWORD="android" KEY_PASSWORD="android" ./gradlew assembleRelease
```
