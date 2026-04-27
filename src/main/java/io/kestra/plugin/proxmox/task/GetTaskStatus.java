package io.kestra.plugin.proxmox.task;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.proxmox.AbstractTask;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Get the status of a Proxmox VE async task by UPID",
    description = "Retrieves the current status of a task from /nodes/{node}/tasks/{upid}/status."
)
@Plugin(
    examples = {
        @Example(
            title = "Get task status",
            full = true,
            code = """
                id: get_task_status
                namespace: company.team

                tasks:
                  - id: status
                    type: io.kestra.plugin.proxmox.task.GetTaskStatus
                    host: "{{ secret('PROXMOX_HOST') }}"
                    username: "{{ secret('PROXMOX_USERNAME') }}"
                    password: "{{ secret('PROXMOX_PASSWORD') }}"
                    node: pve
                    upid: "UPID:pve:00001234:00000000:65000000:qmstart:100:root@pam:"
                """
        )
    }
)
public class GetTaskStatus extends AbstractTask<GetTaskStatus.Output> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Schema(title = "Task UPID", description = "Proxmox Unique Process ID of the task to query.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> upid;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var rNode = runContext.render(node).as(String.class).orElseThrow();
        var rUpid = runContext.render(upid).as(String.class).orElseThrow();

        try (var client = createClient(runContext)) {
            var encodedUpid = URLEncoder.encode(rUpid, StandardCharsets.UTF_8);
            var data = client.get("/nodes/" + URLEncoder.encode(rNode, StandardCharsets.UTF_8) + "/tasks/" + encodedUpid + "/status");
            var status = MAPPER.treeToValue(data, TaskStatus.class);
            return new Output(status);
        }
    }

    public record Output(
        @Schema(title = "Task status details") TaskStatus taskStatus
    ) implements io.kestra.core.models.tasks.Output {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TaskStatus(
        @Schema(title = "Task UPID") String upid,
        @Schema(title = "Task status", description = "running or stopped.") String status,
        @Schema(title = "Exit status", description = "OK if the task succeeded; error message otherwise.") String exitstatus,
        @Schema(title = "Task type") String type,
        @Schema(title = "Start time (epoch seconds)") long starttime
    ) {}
}
