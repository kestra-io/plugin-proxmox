# How to use the Proxmox VE plugin

Manage virtual machines, containers, backups, snapshots, and cluster resources on Proxmox VE from Kestra flows.

## Authentication

All tasks require `host` (the Proxmox VE node hostname or IP, required) and `node` (the cluster node name, e.g. `pve`, required). Optionally set `port` (default `8006`). Authenticate with either:

**Ticket (username/password):** set `username` (in the form `user@realm`, e.g. `root@pam`) and `password`.

**API token:** set `tokenId` (in the form `user@realm!tokenname`, e.g. `root@pam!mytoken`) and `tokenSecret`.

Optionally set `verifySsl` (default `true`; set to `false` only for trusted networks with self-signed Proxmox certificates). Store secrets in [secrets](https://kestra.io/docs/concepts/secret) and apply connection properties globally with [plugin defaults](https://kestra.io/docs/workflow-components/plugin-defaults).

## Tasks

`vm.Create` creates a new QEMU VM — set `vmId` (integer VMID, required) and `vmName` (required). Optionally set `cores` (default `1`), `memory` (MiB, default `1024`), `disk` (default `local-lvm:8`), `net` (default `virtio,bridge=vmbr0`), `osTemplate`, and `powerOn` (default `false`). Outputs `vmId`, `vmName`, and `upid`.

`vm.Clone` clones an existing QEMU VM — set `vmName` (source VM name or ID, required) and `newId` (new VMID, required). Optionally set `newName`, `targetNode`, and `full` (default `false` for a linked clone).

`vm.Start`, `vm.Stop`, `vm.Reboot`, `vm.Reset`, and `vm.Delete` manage VM power state and deletion — set `vmName` (VM name or VMID, required) on each.

`vm.Update` updates VM configuration — set `vmName` (required). Optionally set `cores`, `memory`, and `config` (a map of additional Proxmox parameters).

`vm.Migrate` migrates a VM to another node — set `vmName` and `targetNode` (both required).

`vm.List` lists all QEMU VMs on the node.

`container.Create` creates a new LXC container — set `vmId` (integer CTID, required) and `osTemplate` (e.g. `local:vztmpl/ubuntu-22.04-standard_22.04-1_amd64.tar.zst`, required). Optionally set `hostname`, `cores` (default `1`), `memory` (MiB, default `512`), `rootfs` (default `local-lvm:4`), `net` (default `name=eth0,bridge=vmbr0,ip=dhcp`), `unprivileged` (default `true`), and `powerOn` (default `false`).

`container.Clone` clones an LXC container — set `vmName` (source container name or ID, required) and `newId` (new CTID, required). Optionally set `newName`.

`container.Start`, `container.Stop`, `container.Reboot`, `container.Delete` manage container power state and deletion — set `vmName` (required) on each.

`container.Update` updates container configuration — set `vmName` (required). Optionally set `cores`, `memory`, and `config`.

`container.Migrate` migrates a container to another node — set `vmName` and `targetNode` (both required).

`container.List` lists all LXC containers on the node.

`backup.Create` creates a vzdump backup — set `vmName` (VM or container name or ID, required) and `storage` (storage ID, required). Optionally set `mode` (default `snapshot`; also `suspend` or `stop`), `compress` (default `zstd`; also `lzo`, `gzip`, or `0` for none), and `timeout` (default `PT1H`).

`backup.List` lists available backups on a storage — set `storage` (required).

`backup.Restore` restores a backup — set `vmId` (new VMID to restore into, required), `archive` (backup archive path, required), and `storage` (required). Optionally set `resourceType` (`vm` or `container`, default `vm`) and `timeout` (default `PT1H`).

`snapshot.Create` creates a snapshot — set `vmName` (required) and `snapName` (required). Optionally set `snapDescription` and `resourceType` (`vm` or `container`, default `vm`).

`snapshot.List` lists snapshots for a VM or container — set `vmName` (required). Optionally set `resourceType` (default `vm`).

`snapshot.Delete` deletes a snapshot — set `vmName` and `snapName` (both required). Optionally set `resourceType` (default `vm`).

`snapshot.Rollback` rolls back to a snapshot — set `vmName` and `snapName` (both required). Optionally set `resourceType` (default `vm`).

`cluster.ListResources` returns all cluster resources — optionally set `typeFilter` (`vm`, `node`, `storage`, or `pool`) to filter results.

`cluster.GetNodeStatus` returns the status of the connected node; `cluster.ListPools` lists resource pools.

`network.ListNetworks` lists network interfaces on the node; `network.GetFirewallRules` returns firewall rules (optionally scoped to a specific VM or container by setting `vmId` and `resourceType`).

`template.Create` converts a VM or container to a template — set `vmName` (required). `template.List` lists available templates.

`task.WaitForTask` waits for an async Proxmox task to complete — set `upid` (the Proxmox Unique Process ID, required). Optionally set `timeoutSeconds` (default `600`). Outputs `upid`.

`task.GetTaskStatus` returns the current status of a task — set `upid` (required).

## Triggers

`vm.Trigger` fires when any VM's status matches a configured value — set `connection` (a `ProxmoxConnection` block with `host`, `node`, and auth fields, required) and `targetStatus` (e.g. `running` or `stopped`, required). Optionally set `interval` (default `PT1M`). Uses stateful change detection: fires once when a VM enters the target status and again only if it leaves and re-enters it.
