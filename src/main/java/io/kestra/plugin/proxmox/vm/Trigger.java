package io.kestra.plugin.proxmox.vm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.conditions.ConditionContext;
import io.kestra.core.models.executions.Execution;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.triggers.AbstractTrigger;
import io.kestra.core.models.triggers.PollingTriggerInterface;
import io.kestra.core.models.triggers.TriggerContext;
import io.kestra.core.models.triggers.TriggerOutput;
import io.kestra.core.models.triggers.TriggerService;
import io.kestra.plugin.proxmox.ClientFactory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Trigger on Proxmox VE QEMU VM status changes",
    description = """
        Polls /nodes/{node}/qemu on the configured interval and fires when any VM's status matches
        the configured targetStatus (e.g. running, stopped). Returns the list of matching VMs.
        """
)
@Plugin(
    examples = {
        @Example(
            title = "Fire when any VM enters stopped state",
            full = true,
            code = """
                id: vm_stopped_trigger
                namespace: company.team

                triggers:
                  - id: watch
                    type: io.kestra.plugin.proxmox.vm.Trigger
                    interval: PT1M
                    host: "{{ secret('PROXMOX_HOST') }}"
                    username: "{{ secret('PROXMOX_USERNAME') }}"
                    password: "{{ secret('PROXMOX_PASSWORD') }}"
                    node: pve
                    targetStatus: stopped

                tasks:
                  - id: log
                    type: io.kestra.plugin.core.log.Log
                    message: "VMs stopped: {{ trigger.vms }}"
                """
        )
    }
)
public class Trigger extends AbstractTrigger implements PollingTriggerInterface, TriggerOutput<Trigger.Output> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Builder.Default
    private final Duration interval = Duration.ofMinutes(2);

    @Schema(title = "Proxmox host")
    @NotNull
    @PluginProperty(group = "main")
    protected Property<String> host;

    @Schema(title = "API port", description = "Defaults to 8006.")
    @Builder.Default
    @PluginProperty(group = "connection")
    protected Property<Integer> port = Property.ofValue(8006);

    @Schema(title = "Proxmox node name")
    @NotNull
    @PluginProperty(group = "main")
    protected Property<String> node;

    @Schema(title = "Username")
    @PluginProperty(group = "main")
    protected Property<String> username;

    @Schema(title = "Password")
    @PluginProperty(group = "main")
    protected Property<String> password;

    @Schema(title = "API token ID")
    @PluginProperty(group = "main")
    protected Property<String> tokenId;

    @Schema(title = "API token secret")
    @PluginProperty(group = "main")
    protected Property<String> tokenSecret;

    @Schema(title = "Verify SSL", description = "Defaults to false.")
    @Builder.Default
    @PluginProperty(group = "advanced")
    protected Property<Boolean> verifySsl = Property.ofValue(false);

    @Schema(
        title = "Target VM status",
        description = "Status to match for triggering, e.g. running or stopped."
    )
    @NotNull
    @PluginProperty(group = "main")
    protected Property<String> targetStatus;

    @Override
    public Duration getInterval() {
        return interval;
    }

    @Override
    public Optional<Execution> evaluate(ConditionContext conditionContext, TriggerContext context) throws Exception {
        var runContext = conditionContext.getRunContext();

        var rHost = runContext.render(host).as(String.class).orElseThrow();
        var rPort = runContext.render(port).as(Integer.class).orElse(8006);
        var rNode = runContext.render(node).as(String.class).orElseThrow();
        var rUsername = runContext.render(username).as(String.class).orElse(null);
        var rPassword = runContext.render(password).as(String.class).orElse(null);
        var rTokenId = runContext.render(tokenId).as(String.class).orElse(null);
        var rTokenSecret = runContext.render(tokenSecret).as(String.class).orElse(null);
        var rVerifySsl = runContext.render(verifySsl).as(Boolean.class).orElse(false);
        var rTargetStatus = runContext.render(targetStatus).as(String.class).orElseThrow();

        List<VmSnapshot> matched = new ArrayList<>();

        try (var client = ClientFactory.create(rHost, rPort, rNode, rUsername, rPassword, rTokenId, rTokenSecret, rVerifySsl, runContext)) {
            var data = client.get("/nodes/" + rNode + "/qemu");
            for (var item : data) {
                var status = item.path("status").asText("");
                if (rTargetStatus.equalsIgnoreCase(status)) {
                    matched.add(VmSnapshot.builder()
                        .vmid(item.path("vmid").asInt())
                        .name(item.path("name").asText(null))
                        .status(status)
                        .build());
                }
            }
        }

        if (matched.isEmpty()) {
            return Optional.empty();
        }

        var output = Output.builder().vms(matched).build();
        return Optional.of(TriggerService.generateExecution(this, conditionContext, context, output));
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VmSnapshot {

        @Schema(title = "VM ID")
        private int vmid;

        @Schema(title = "VM name")
        private String name;

        @Schema(title = "VM status")
        private String status;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Output implements io.kestra.core.models.tasks.Output {

        @Schema(title = "VMs matching the configured status")
        private List<VmSnapshot> vms;
    }
}
