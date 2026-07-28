# Module boundaries

## Dependency rules

- `app` is the composition root. It wires navigation and application-wide services only.
- Features must not depend on other features. Cross-feature actions use `core:navigation` contracts.
- `core:database`, `core:network`, and future transport modules must not depend on UI or feature modules.
- `core:designsystem` may depend on models needed to render shared UI, but data and transport modules must not depend on it.
- Repository interfaces live with the domain that owns the business capability; their implementations and Hilt bindings live in the same feature or infrastructure module.

## Planned module roles

| Module | Responsibility | Must not depend on |
| --- | --- | --- |
| `core:model` | Serializable domain values and identifiers | Android UI, features |
| `core:navigation` | Type-safe destinations and navigation contracts | features |
| `core:database` | Room entities, DAO, schema history, migrations | UI, features |
| `core:network` | Network clients and protocol transport | UI, repositories |
| `core:connectivity` | P2P, Bluetooth, Wi-Fi Direct, NFC and WebRTC capability contracts | UI, features |
| `core:designsystem` | Compose components, resources and themes | feature implementations |
| `core:testing` | Fakes, fixtures and coroutine test utilities | production features |
| `feature:*` | UI, feature state, domain contracts and data implementation | other features |

## Social and group extensibility

Direct and group conversations share a conversation contract; group-specific membership, roles and notices remain in the group capability. Moments are an independent social-feed capability, not a chat extension. Future destinations are added to `core:navigation`, while their entries are installed by the app host. This prevents Chat, Contacts and Discovery from gaining direct dependencies on one another.

`core:location` is currently a transitional exception because it contains map and picker Compose UI. It must be migrated to a location feature before the same no-UI core rule can be enforced for it.
