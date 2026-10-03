# Releasing SnapCrop

Every release is signed with the SnapCrop release key and published in two places:

* **GitHub Releases**, for people who download the APK directly
* **The SnapCrop F-Droid repo** at [apps.nextcolor.org](https://apps.nextcolor.org/), so F-Droid users get updates automatically

Both happen on their own when you push a version tag.

## One-time setup

You need the release key file (`snapcrop-release.p12`) and its password. Keep both somewhere safe and private, like a password manager. **Never commit them.** If the key is lost, nobody can install updates over their existing copy. If it leaks, anyone can publish fake updates.

### GitHub repo

1. In **Settings → Secrets and variables → Actions**, add two secrets:
   * `SNAPCROP_KEYSTORE_BASE64`: the key file encoded as base64
     * Linux: `base64 -w0 snapcrop-release.p12`
     * macOS: `base64 -i snapcrop-release.p12`
     * Windows PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("snapcrop-release.p12"))`
   * `SNAPCROP_KEYSTORE_PASSWORD`: the key password
2. In **Settings → Pages**, set **Source** to **GitHub Actions**.
3. In your account's **Settings → Pages**, add `apps.nextcolor.org` as a verified domain. GitHub shows a TXT record. Send it to the domain owner.

### Domain (whoever owns apps.nextcolor.org)

1. Add a `CNAME` record for `apps` pointing to `vibecoder-jsx.github.io`.
2. Add the TXT record from step 3 above.

### Once DNS is working

In the repo's **Settings → Pages**, enter `apps.nextcolor.org` as the custom domain and turn on **Enforce HTTPS** once it becomes available.

## Publishing a release

1. In `app/build.gradle.kts`, raise `versionCode` by one and set `versionName` to the new version.
2. Write what changed in `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`. This text appears in the GitHub Release and in F-Droid.
3. Commit, then tag and push:
   ```bash
   git tag v1.0.2
   git push origin main v1.0.2
   ```

The **Release** workflow in the Actions tab builds the app, publishes the GitHub Release and updates the F-Droid repo. It takes a few minutes. If the tag doesn't match `versionName`, the workflow stops before publishing anything.

To refresh the F-Droid repo without a new release (for example after editing the store description), run the **Release** workflow by hand from the Actions tab.

## Building a signed APK on your own computer

Create `keystore.properties` in the project root (it is ignored by git):

```properties
storeFile=/path/to/snapcrop-release.p12
storePassword=your-password
keyAlias=snapcrop
```

Then run `./gradlew assembleRelease`. Without this file, you get an unsigned APK instead.

## Store listing

The app's name, descriptions and icon for F-Droid live in `fastlane/metadata/android/en-US/`. To add screenshots, put PNG files in `images/phoneScreenshots/` there.
