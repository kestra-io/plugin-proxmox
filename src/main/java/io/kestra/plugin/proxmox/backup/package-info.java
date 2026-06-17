@PluginSubGroup(
    title = "Proxmox VE Backup",
    description = "Tasks that create, list, and restore VM backups via vzdump on a Proxmox VE node.",
    categories = { PluginSubGroup.PluginCategory.CLOUD, PluginSubGroup.PluginCategory.INFRASTRUCTURE }
)
package io.kestra.plugin.proxmox.backup;

import io.kestra.core.models.annotations.PluginSubGroup;
