# Releasing SnapCrop

Every release is signed with the SnapCrop release key and published as a GitHub Release when you push a version tag.

The NextColor Apps F-Droid repo at [apps.nextcolor.org](https://apps.nextcolor.org/) checks for new releases once a day, so F-Droid users get the update without anything more to do. That repo is run from [ganyuke/nextcolor-apps](https://github.com/ganyuke/nextcolor-apps).

## One-time setup

You need the release key file (`snapcrop-release.p12`) and its password. Keep both somewhere safe and private, like a password manager. **Never commit them.** If the key is lost, nobody can install updates over their existing copy. If it leaks, anyone can publish fake updates.

In **Settings → Secrets and variables → Actions**, add two secrets:

* `SNAPCROP_KEYSTORE_BASE64`: the key file encoded as base64
  * Linux: `base64 -w0 snapcrop-release.p12`
  * macOS: `base64 -i snapcrop-release.p12`
  * Windows PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("snapcrop-release.p12"))`
* `SNAPCROP_KEYSTORE_PASSWORD`: the key password

## Publishing a release

1. In `app/build.gradle.kts`, raise `versionCode` by one and set `versionName` to the new version. `versionName` is three numbers, like `1.0.2`.
2. Write what changed in `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`. This text appears in the GitHub Release and in F-Droid.
3. Commit, then tag and push:
   ```bash
   git tag v1.0.2
   git push origin main v1.0.2
   ```

The **Release** workflow in the Actions tab builds the app and publishes the GitHub Release. It takes a few minutes. If the tag doesn't match `versionName`, the workflow stops before publishing anything.

## Building a signed APK on your own computer

Create `keystore.properties` in the project root (it is ignored by git):

```properties
storeFile=/path/to/snapcrop-release.p12
storePassword=your-password
keyAlias=snapcrop
```

Then run `./gradlew assembleRelease`. Without this file, you get an unsigned APK instead.

## Store listing

The app's name, descriptions and icon for F-Droid live in `fastlane/metadata/android/en-US/`. To add screenshots, put PNG files in `images/phoneScreenshots/` there. Changes show up in F-Droid within a day of reaching `main`.
