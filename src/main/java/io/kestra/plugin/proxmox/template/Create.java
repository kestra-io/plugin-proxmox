package io.kestra.plugin.proxmox.template;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.proxmox.AbstractTask;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Convert a stopped QEMU VM to a template on Proxmox VE",
    description = """
        Issues POST /nodes/{node}/qemu/{vmid}/template to convert a stopped VM into a reusable template.
        The VM must be powered off before calling this task.
        """
)
@Plugin(
    examples = {
        @Example(
            title = "Convert a VM to a template",
            full = true,
            code = """
                id: create_template
                namespace: company.team

                tasks:
                  - id: templatize
                    type: io.kestra.plugin.proxmox.template.Create
                    host: "{{ secret('PROXMOX_HOST') }}"
                    username: "{{ secret('PROXMOX_USERNAME') }}"
                    password: "{{ secret('PROXMOX_PASSWORD') }}"
                    node: pve
                    vmName: base-ubuntu
                """
        )
    }
)
public class Create extends AbstractTask<VoidOutput> {

    @Schema(title = "VM name or ID to convert", description = "Either a VM name (resolved at runtime) or an integer VMID.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> vmName;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rNode = runContext.render(node).as(String.class).orElseThrow();
        var rVmName = runContext.render(vmName).as(String.class).orElseThrow();

        try (var client = createClient(runContext)) {
            var vmid = client.resolveVmId(rVmName);
            logger.info("Converting VM {} (vmid={}) to template", rVmName, vmid);
            client.post("/nodes/" + URLEncoder.encode(rNode, StandardCharsets.UTF_8) + "/qemu/" + vmid + "/template", null);
            logger.info("VM {} converted to template", rVmName);
        }

        return new VoidOutput();
    }
}
