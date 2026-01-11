# SDK Publishing Guide

This document describes the complete setup and process for publishing all 9 renamed.to SDKs to their respective package registries.

## Overview

| SDK | Registry | Package Name | Publish Method |
|-----|----------|--------------|----------------|
| TypeScript | npm | `@renamed/sdk` | Automated via CI |
| Python | PyPI | `renamed` | Automated via CI |
| Go | GitHub | `github.com/renamed-to/renamed-sdk/sdks/go` | Git tags |
| Java | Maven Central | `to.renamed:renamed-sdk` | Automated via CI |
| C# | NuGet | `Renamed.Sdk` | Automated via CI |
| Ruby | RubyGems | `renamed` | Automated via CI |
| Rust | crates.io | `renamed` | Automated via CI |
| Swift | Swift Package Index | `Renamed` | Git tags |
| PHP | Packagist | `renamed-to/renamed-php` | Webhook |

---

## Prerequisites

### 1. Registry Accounts

Create accounts on each registry where you'll publish:

| Registry | URL | Account Type |
|----------|-----|--------------|
| npm | https://www.npmjs.com/signup | Personal or Organization |
| PyPI | https://pypi.org/account/register/ | Personal |
| Maven Central (Sonatype) | https://central.sonatype.org/register/central-portal/ | Organization |
| NuGet | https://www.nuget.org/users/account/LogOn | Microsoft Account |
| RubyGems | https://rubygems.org/sign_up | Personal |
| crates.io | https://crates.io/ | GitHub OAuth |
| Packagist | https://packagist.org/register/ | Personal |
| Swift Package Index | https://swiftpackageindex.com | Automatic (no account needed) |

### 2. Package Name Registration

Before first publish, ensure package names are available:

```bash
# npm - check availability
npm view @renamed/sdk

# PyPI - check availability
pip index versions renamed

# crates.io - check availability
cargo search renamed

# RubyGems - check availability
gem search renamed

# Packagist - check availability
composer show renamed-to/renamed-php
```

---

## Registry-Specific Setup

### npm (TypeScript)

#### Initial Setup (Trusted Publishing - Recommended)

npm supports **Trusted Publishing** via OIDC, which is more secure than API tokens.

1. **Publish First Version Manually**
   ```bash
   cd sdks/typescript
   npm login
   npm run build
   npm publish --access public
   ```

2. **Configure Trusted Publishing**
   - Go to https://www.npmjs.com/package/@renamed-to/sdk/access
   - Under "Trusted Publisher", click "Set up connection"
   - Fill in:
     - **Organization or user:** `renamed-to`
     - **Repository:** `renamed-sdk`
     - **Workflow filename:** `release.yml`
     - **Environment name:** (optional)
   - Click "Set up connection"

3. **No GitHub Secret Needed**
   - Trusted Publishing uses OIDC tokens automatically
   - GitHub Actions authenticates directly with npm

#### Alternative: API Token Setup

If you prefer API tokens instead of Trusted Publishing:

1. Go to https://www.npmjs.com/settings/tokens
2. Generate a "Granular Access Token" with publish permissions
3. Add GitHub secret: `NPM_TOKEN`

#### Manual Publishing (if needed)
```bash
cd sdks/typescript
npm ci
npm run build
npm publish --access public
```

---

### PyPI (Python)

#### Initial Setup

1. **Enable 2FA on PyPI**
   - Go to https://pypi.org/manage/account/
   - Enable two-factor authentication (required for new projects)

2. **Create API Token**
   - Go to https://pypi.org/manage/account/token/
   - Click "Add API token"
   - Scope: "Entire account" (or project-specific after first publish)
   - Copy the token (starts with `pypi-`)

3. **Add GitHub Secret**
   - Add secret: `PYPI_TOKEN` = your token

#### Manual Publishing (if needed)
```bash
cd sdks/python
pip install build twine
python -m build
twine upload dist/*
```

---

### Maven Central (Java)

#### Initial Setup

This is the most complex setup due to GPG signing requirements.

1. **Register Namespace with Sonatype**
   - Go to https://central.sonatype.org/register/central-portal/
   - Register namespace: `to.renamed`
   - Verify domain ownership via DNS TXT record or GitHub repo

2. **Generate GPG Key**
   ```bash
   # Generate a new GPG key
   gpg --gen-key
   # Choose: RSA and RSA, 4096 bits, no expiration
   # Enter name and email

   # List keys to get the key ID
   gpg --list-secret-keys --keyid-format LONG
   # Output example: sec rsa4096/ABCD1234EFGH5678
   # The key ID is: ABCD1234EFGH5678

   # Export public key to keyserver
   gpg --keyserver keyserver.ubuntu.com --send-keys ABCD1234EFGH5678

   # Export private key for CI
   gpg --armor --export-secret-keys ABCD1234EFGH5678 > private-key.asc
   ```

3. **Get Sonatype Credentials**
   - Go to https://s01.oss.sonatype.org/
   - Log in with your Sonatype account
   - Go to Profile → User Token → Access User Token
   - Note your username and token

4. **Add GitHub Secrets**
   - `OSSRH_USERNAME` = your Sonatype username token
   - `OSSRH_TOKEN` = your Sonatype password token
   - `MAVEN_GPG_PRIVATE_KEY` = contents of private-key.asc
   - `MAVEN_GPG_PASSPHRASE` = your GPG key passphrase

5. **Delete Local Key File**
   ```bash
   rm private-key.asc
   ```

#### Manual Publishing (if needed)
```bash
cd sdks/java
mvn clean deploy -P release
```

---

### NuGet (C#)

#### Initial Setup (Trusted Publishing - Recommended)

NuGet supports **Trusted Publishing** via OIDC, which is more secure than API keys.

1. **Sign in to NuGet**
   - Go to https://www.nuget.org/users/account/LogOn
   - Sign in with Microsoft account

2. **Configure Trusted Publishing**
   - Go to https://www.nuget.org/account/trustedpublishing
   - Click "Create" and fill in:
     - **Policy Name:** GitHub Actions Release
     - **Package Owner:** your NuGet username
     - **Repository Owner:** `renamed-to`
     - **Repository:** `renamed-sdk`
     - **Workflow File:** `release.yml`
     - **Environment:** (leave blank or set to `production`)
   - Click "Create"

3. **No GitHub Secret Needed**
   - Trusted Publishing uses OIDC tokens automatically
   - GitHub Actions authenticates directly with NuGet

#### Alternative: API Key Setup

If you prefer API keys instead of Trusted Publishing:

1. Go to https://www.nuget.org/account/apikeys
2. Create key with "Push new packages and package versions" scope
3. Add GitHub secret: `NUGET_API_KEY`

#### Manual Publishing (if needed)
```bash
cd sdks/csharp
dotnet pack -c Release
dotnet nuget push bin/Release/*.nupkg --source https://api.nuget.org/v3/index.json
```

---

### RubyGems (Ruby)

#### Initial Setup

1. **Enable MFA**
   - Go to https://rubygems.org/settings/edit
   - Enable MFA for "UI and gem signin" or "UI and API"

2. **Get API Key**
   - Go to https://rubygems.org/settings/edit
   - Find "API keys" section
   - Create new key with "Push rubygem" scope
   - Copy the key

3. **Add GitHub Secret**
   - Add secret: `RUBYGEMS_API_KEY` = your API key

#### Manual Publishing (if needed)
```bash
cd sdks/ruby
gem build renamed.gemspec
gem push renamed-*.gem
```

---

### crates.io (Rust)

#### Initial Setup

1. **Log in via GitHub**
   - Go to https://crates.io/
   - Click "Log in with GitHub"

2. **Create API Token**
   - Go to https://crates.io/settings/tokens
   - Click "New Token"
   - Name: "GitHub Actions"
   - Scopes: "publish-new" and "publish-update"
   - Copy the token

3. **Add GitHub Secret**
   - Add secret: `CRATES_IO_TOKEN` = your token

#### Manual Publishing (if needed)
```bash
cd sdks/rust
cargo publish
```

---

### Swift Package Index (Swift)

#### Initial Setup

No manual registration needed. The Swift Package Index automatically discovers packages.

1. **Ensure Package.swift is valid**
   ```bash
   cd sdks/swift
   swift build
   swift test
   ```

2. **Add to Swift Package Index** (optional, for discoverability)
   - Go to https://swiftpackageindex.com/add-a-package
   - Submit your repository URL

#### Publishing

Swift packages are distributed via git tags. When you push a version tag, users can immediately use it:

```swift
// Package.swift
dependencies: [
    .package(url: "https://github.com/renamed-to/renamed-sdk", from: "1.0.0")
]
```

---

### Packagist (PHP)

#### Initial Setup

1. **Register Package**
   - Go to https://packagist.org/packages/submit
   - Enter your GitHub repository URL
   - Click "Check" then "Submit"

2. **Set Up Auto-Update Webhook**
   - In your GitHub repo → Settings → Webhooks → Add webhook
   - Payload URL: `https://packagist.org/api/github?username=YOUR_PACKAGIST_USERNAME`
   - Content type: `application/json`
   - Secret: Your Packagist API token (from https://packagist.org/profile/)
   - Events: "Just the push event"

3. **Alternative: GitHub Integration**
   - Go to https://packagist.org/profile/
   - Connect your GitHub account
   - Enable auto-sync for the repository

#### Manual Publishing

Packagist auto-updates from git tags. No manual publish needed after initial setup.

---

### Go Modules

#### Initial Setup

No registry account needed. Go modules are fetched directly from git repositories.

1. **Verify Module Path**

   Ensure `go.mod` has the correct module path:
   ```go
   module github.com/renamed-to/renamed-sdk/sdks/go
   ```

2. **Tag Format**

   For subdirectory modules, tags must include the path:
   ```bash
   # For root module: v1.0.0
   # For subdirectory: sdks/go/v1.0.0
   ```

#### Publishing

Go modules are published by pushing git tags:
```bash
git tag sdks/go/v1.0.0
git push origin sdks/go/v1.0.0
```

Users install with:
```bash
go get github.com/renamed-to/renamed-sdk/sdks/go@v1.0.0
```

---

## GitHub Secrets Summary

Add these secrets to your repository (Settings → Secrets and variables → Actions):

| Secret Name | Registry | How to Get |
|-------------|----------|------------|
| ~~`NPM_TOKEN`~~ | npm | Not needed - uses Trusted Publishing |
| ~~`PYPI_TOKEN`~~ | PyPI | Not needed - uses Trusted Publishing |
| `OSSRH_USERNAME` | Maven Central | https://central.sonatype.com/account |
| `OSSRH_TOKEN` | Maven Central | https://central.sonatype.com/account |
| `MAVEN_GPG_PRIVATE_KEY` | Maven Central | GPG key export |
| `MAVEN_GPG_PASSPHRASE` | Maven Central | GPG passphrase |
| `RUBYGEMS_API_KEY` | RubyGems | https://rubygems.org/settings/edit |
| `CARGO_REGISTRY_TOKEN` | crates.io | https://crates.io/settings/tokens |

**No secret needed:**
- **npm** - Uses Trusted Publishing (OIDC)
- **PyPI** - Uses Trusted Publishing (OIDC)
- **NuGet** - Uses Trusted Publishing (OIDC)
- **Go** - Distributed via git tags
- **Swift** - Distributed via git tags
- **PHP/Packagist** - Auto-updates via webhook

---

## Release Process

### Step 1: Update Version Numbers

Update the version in all SDK package files:

```bash
# TypeScript
# sdks/typescript/package.json → "version": "1.0.0"

# Python
# sdks/python/pyproject.toml → version = "1.0.0"

# Java
# sdks/java/pom.xml → <version>1.0.0</version>

# C#
# sdks/csharp/Renamed.csproj → <Version>1.0.0</Version>

# Ruby
# sdks/ruby/lib/renamed/version.rb → VERSION = "1.0.0"

# Rust
# sdks/rust/Cargo.toml → version = "1.0.0"

# Swift
# sdks/swift/Package.swift (no version field, uses git tags)

# PHP
# No version field needed - Packagist uses git tags
```

### Step 2: Commit Version Changes

```bash
git add -A
git commit -m "chore: bump version to 1.0.0"
git push origin main
```

### Step 3: Create and Push Tag

```bash
git tag v1.0.0
git push origin v1.0.0
```

### Step 4: Monitor CI/CD

1. Go to Actions tab in GitHub
2. Watch the "Release" workflow
3. Verify all publish jobs succeed
4. Check each registry to confirm packages are available

---

## Verification Checklist

After publishing, verify each package is available:

```bash
# npm
npm view @renamed/sdk@1.0.0

# PyPI
pip index versions renamed

# Maven Central (may take up to 2 hours to sync)
# Check: https://search.maven.org/artifact/to.renamed/renamed-sdk

# NuGet
dotnet package search Renamed.Sdk

# RubyGems
gem info renamed -r

# crates.io
cargo search renamed

# Go
go list -m github.com/renamed-to/renamed-sdk/sdks/go@v1.0.0

# Swift - check Swift Package Index
# https://swiftpackageindex.com/renamed-to/renamed-sdk

# Packagist
composer show renamed-to/renamed-php
```

---

## Troubleshooting

### npm: 403 Forbidden
- Ensure the `@renamed` scope exists and you have publish rights
- Check that `NPM_TOKEN` has automation privileges

### PyPI: Invalid token
- Regenerate token at https://pypi.org/manage/account/token/
- Ensure 2FA is enabled on your account

### Maven Central: Signature verification failed
- Ensure GPG key is uploaded to keyserver: `gpg --keyserver keyserver.ubuntu.com --send-keys KEY_ID`
- Wait 10-15 minutes for key propagation

### Maven Central: Staging rules failed
- Check that all required files are present (sources, javadoc, signatures)
- Verify POM metadata is complete

### NuGet: Package already exists
- NuGet doesn't allow re-publishing the same version
- Increment version number for fixes

### RubyGems: MFA required
- Enable MFA at https://rubygems.org/settings/edit
- Use API key created after MFA was enabled

### crates.io: Package already exists
- crates.io doesn't allow re-publishing
- Increment version number for fixes

### Packagist: Not updating
- Check webhook is configured correctly
- Manually trigger update at https://packagist.org/packages/renamed-to/renamed-php

---

## Security Best Practices

1. **Rotate tokens regularly** - Set calendar reminders to rotate tokens annually
2. **Use minimum scopes** - Only grant necessary permissions to each token
3. **Audit access** - Review who has publish access to each registry
4. **Monitor for unauthorized publishes** - Set up alerts for new version notifications
5. **Use 2FA everywhere** - Enable on all registry accounts
6. **Keep GPG keys secure** - Never commit private keys to the repository

---

## Support

If you encounter issues:

1. Check the GitHub Actions logs for detailed error messages
2. Verify secrets are correctly set (Settings → Secrets → check names match exactly)
3. Test manual publishing locally to isolate CI vs registry issues
4. Check registry status pages for outages
