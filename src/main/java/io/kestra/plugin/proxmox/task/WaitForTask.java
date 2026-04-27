package io.kestra.plugin.proxmox.task;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.proxmox.AbstractTask;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
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
    title = "Wait for a Proxmox VE async task to complete",
    description = """
        Polls /nodes/{node}/tasks/{upid}/status until exitstatus=OK or the timeout is exceeded.
        Default timeout is 10 minutes, poll interval is 2 seconds.
        """
)
@Plugin(
    examples = {
        @Example(
            title = "Wait for a task",
            full = true,
            code = """
                id: wait_for_task
                namespace: company.team

                tasks:
                  - id: wait
                    type: io.kestra.plugin.proxmox.task.WaitForTask
                    host: "{{ secret('PROXMOX_HOST') }}"
                    username: "{{ secret('PROXMOX_USERNAME') }}"
                    password: "{{ secret('PROXMOX_PASSWORD') }}"
                    node: pve
                    upid: "UPID:pve:00001234:00000000:65000000:qmstart:100:root@pam:"
                    timeoutSeconds: 600
                """
        )
    }
)
public class WaitForTask extends AbstractTask<AbstractTask.Output> {

    @Schema(title = "Task UPID", description = "Proxmox Unique Process ID to poll.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> upid;

    @Schema(title = "Timeout in seconds", description = "Maximum time to wait for task completion. Defaults to 600 (10 minutes).")
    @Builder.Default
    @PluginProperty(group = "advanced")
    private Property<Integer> timeoutSeconds = Property.ofValue(600);

    @Override
    public AbstractTask.Output run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rUpid = runContext.render(upid).as(String.class).orElseThrow();
        var rTimeout = runContext.render(timeoutSeconds).as(Integer.class).orElse(600);

        logger.info("Waiting for task upid={} (timeout={}s)", rUpid, rTimeout);

        try (var client = createClient(runContext)) {
            client.waitForTask(rUpid, rTimeout);
        }

        logger.info("Task {} completed", rUpid);
        return AbstractTask.Output.of(null, null, rUpid);
    }
}
