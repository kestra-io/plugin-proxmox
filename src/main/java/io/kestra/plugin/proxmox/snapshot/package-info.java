@PluginSubGroup(
    title = "Proxmox VE - Snapshots",
    description = "Tasks that create, list, delete, and roll back snapshots for QEMU VMs and LXC containers.",
    categories = { PluginSubGroup.PluginCategory.CLOUD, PluginSubGroup.PluginCategory.INFRASTRUCTURE }
)
package io.kestra.plugin.proxmox.snapshot;

import io.kestra.core.models.annotations.PluginSubGroup;
