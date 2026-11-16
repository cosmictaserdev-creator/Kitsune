# Theme collections and release boundary

The Themes tab separates Included, My themes and Collections. Included themes resolve their
preview palettes through the actual keyboard color resolver. Selecting one changes the same
preferences as the existing theme picker; the existing visual editor remains available.

The Collections catalog is intentionally empty: no paid themes, product IDs, prices, billing
credentials or private theme artwork were supplied. There is no fake checkout or purchase flag.

## Future paid collections

- Keep the keyboard, HIG settings, generic pack loader/renderer and any derived theme code under
  the applicable open-source licenses. Selling access does not make GPL-derived source proprietary.
- Original, independently licensed artwork may be distributed separately from application code.
  Keep its license and assets in a private content repository. Treat pack metadata/rendering
  instructions separately according to their actual copyright status; a JSON extension alone
  does not establish a proprietary licensing boundary.
- Sell future collections as Google Play one-time products. Query localized product details from
  Play; handle pending, cancellation, already-owned and restored purchases; acknowledge verified
  purchases. Never hardcode a successful entitlement in the keyboard.
- For controlled asset delivery, verify purchase tokens on a service you operate before returning
  short-lived asset download URLs. Keep that service and original artwork separate from this repo.
  Device-delivered assets cannot be guaranteed impossible to extract; this is access control,
  not a promise of unbreakable DRM.
- No executable plugins, downloaded Kotlin/DEX or remote scripts. Theme packs contain validated
  visual data/assets consumed by the public engine. Define size limits, hashes and schema versions
  when the first collection is added.
- Test purchases using Play internal testing and license testers before production. Whispry's
  existing Premium purchase is not a purchase of keyboard collections.

References: https://www.gnu.org/licenses/gpl-faq.html and
https://developer.android.com/distribute/play-billing

## UI dependencies

Compose HIG is Apache-2.0; its license, NOTICE and Android adaptation provenance are retained in
`hig/`. Android 7–11, software-rendered views and reduced-transparency mode use opaque surfaces
without evaluating backdrop effect callbacks. Glass is an optional rendering enhancement.

The requested Jetpack_SF_Symbols library is referenced at 1.0.4. Its MIT code license and glyph
rights disclaimer are preserved in `docs/SF_SYMBOLS_LICENSE.txt`; the wrapper's license does not
establish third-party glyph rights for Play distribution. Review the underlying symbol terms or
substitute original cleared vectors through SettingsIcons before publishing.

No app, paid theme or purchase configuration has been published by this change.
