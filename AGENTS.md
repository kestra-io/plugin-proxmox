# Kestra Proxmox Plugin

## What

- Provides plugin components under `io.kestra.plugin.proxmox`.
- Covers the full lifecycle of QEMU VMs and LXC containers on Proxmox VE: create, start, stop, reboot, reset, update, clone, migrate, delete.
- Includes backup management (create, restore, list), snapshot management (create, delete, list, rollback), cluster introspection (node status, resource listing, pool listing), network and firewall inspection, template management, and async task monitoring.
- Exposes a polling trigger that fires when any VM reaches a target status (e.g. `stopped`).

## Why

- Teams running workloads on Proxmox VE need to orchestrate VM and container lifecycle from Kestra workflows without writing custom API wrappers.
- Enables use cases such as scheduled backups, automated provisioning, CI/CD environment spin-up/tear-down, and reactive scaling based on VM state changes.
- Centralises authentication (ticket-based or API token), error handling, and async task polling in a single reusable client so individual tasks stay focused on business logic.

## How

### Architecture

Single-module plugin. All source packages live under `io.kestra.plugin.proxmox`:

| Sub-package | Purpose |
|---|---|
| `vm` | QEMU VM lifecycle: `Create`, `Delete`, `Start`, `Stop`, `Reboot`, `Reset`, `Update`, `Clone`, `Migrate`, `List`, `Trigger` |
| `container` | LXC container lifecycle: `Create`, `Delete`, `Start`, `Stop`, `Reboot`, `Update`, `Clone`, `Migrate`, `List` |
| `backup` | Backup management: `Create`, `Restore`, `List` |
| `snapshot` | Snapshot management: `Create`, `Delete`, `List`, `Rollback` |
| `cluster` | Cluster introspection: `GetNodeStatus`, `ListResources`, `ListPools` |
| `task` | Async task monitoring: `GetTaskStatus`, `WaitForTask` |
| `template` | Template management: `Create`, `List` |
| `network` | Network and firewall: `ListNetworks`, `GetFirewallRules` |

Shared infrastructure:

- `AbstractTask` — base class for all tasks; holds connection properties (host, port, node, auth) and creates a `ProxmoxClient`.
- `ProxmoxConnection` — embeddable connection object used by `Trigger` (which cannot extend `AbstractTask`).
- `ProxmoxClient` — Kestra HTTP-client wrapper for the Proxmox API: GET/POST/PUT/DELETE, ticket and token auth, async task polling.
- `ClientFactory` — constructs `ProxmoxClient` from rendered credentials.
- `ResourceType` — enum (`vm`, `container`) used by snapshot, backup, and firewall tasks.

Infrastructure dependencies:

- No Docker Compose service required for unit tests; tests use WireMock to stub the Proxmox API.

### Key Plugin Classes

- `io.kestra.plugin.proxmox.AbstractTask`
- `io.kestra.plugin.proxmox.ProxmoxConnection`
- `io.kestra.plugin.proxmox.ProxmoxClient`
- `io.kestra.plugin.proxmox.ClientFactory`
- `io.kestra.plugin.proxmox.ResourceType`
- `io.kestra.plugin.proxmox.vm.Trigger`

### Project Structure

```
plugin-proxmox/
├── src/main/java/io/kestra/plugin/proxmox/
│   ├── AbstractTask.java
│   ├── ClientFactory.java
│   ├── ProxmoxClient.java
│   ├── ProxmoxConnection.java
│   ├── ResourceType.java
│   ├── backup/
│   ├── cluster/
│   ├── container/
│   ├── network/
│   ├── snapshot/
│   ├── task/
│   ├── template/
│   └── vm/
├── src/test/java/io/kestra/plugin/proxmox/
├── build.gradle
└── README.md
```

## References

- https://kestra.io/docs/plugin-developer-guide
- https://kestra.io/docs/plugin-developer-guide/contribution-guidelines
