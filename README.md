# Audiobook Auto

> **Credit:** Audiobook Auto is an independent fork of
> **[Chronicle Epilogue](https://github.com/fabiogermann/chronicle)** by
> [@fabiogermann](https://github.com/fabiogermann), which itself continues the original
> **[Chronicle](https://github.com/mattttvaughn/chronicle)** by
> [@mattttvaughn](https://github.com/mattttvaughn). Nearly all of the code here is their work,
> used under the GPLv3. Please support and star the upstream project:
> **https://github.com/fabiogermann/chronicle**
>
> This fork is not affiliated with or endorsed by the Chronicle project. It uses its own
> package name (`io.github.mjohnson7cd.audiobook`), name, and icons, and focuses on
> Android Auto.

An Android audiobook player for Plex Media Server, with a focus on Android Auto.
Stream audiobooks hosted on your own server, or download them for offline listening.

### Features

 - Sync audiobook progress on device
 - Support for file formats: mp3, m4a, m4b
 - Adjustable playback speed
 - Auto-rewind
 - Sleep timer
 - Skip silent audio
 - Download books for playing any time, even when offline
 - Android Auto support

### Reporting Bugs

Please report bugs for this fork in [GitHub Issues](https://github.com/mjohnson7cd/audiobook-auto/issues).
Issues that also affect upstream belong in the
[upstream tracker](https://github.com/fabiogermann/chronicle/issues).

### Building a release

Release builds are signed using `keystore.properties` in the project root (copy
`keystore.properties.example`), or the `KEYSTORE_FILE` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` /
`KEY_PASSWORD` environment variables in CI. Neither the keystore nor its properties file
belongs in git.

```sh
./gradlew assembleRelease
```

### Useful Links

 - [Plex Audiobook Guide](https://github.com/seanap/Plex-Audiobook-Guide)

### License

- **Source code:** [GNU General Public License v3.0](LICENSE), as inherited from upstream.
- **Icons and store graphics:** the upstream Chronicle branding (All Rights Reserved) has been
  removed. The placeholder assets in this fork are public domain (CC0). See [ASSETS-LICENSE](ASSETS-LICENSE).
