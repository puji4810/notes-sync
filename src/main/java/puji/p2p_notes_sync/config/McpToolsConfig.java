package puji.p2p_notes_sync.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import puji.p2p_notes_sync.mcp.NotesManagementMcpTools;
import puji.p2p_notes_sync.mcp.P2PSyncMcpTools;

@Configuration
public class McpToolsConfig {

	@Bean
	public ToolCallbackProvider notesManagementMcpToolsProvider(NotesManagementMcpTools notesManagementMcpTools) {
		return MethodToolCallbackProvider.builder()
				.toolObjects(notesManagementMcpTools)
				.build();
	}

	@Bean
	public ToolCallbackProvider p2pSyncMcpToolsProvider(P2PSyncMcpTools p2pSyncMcpTools) {
		return MethodToolCallbackProvider.builder()
				.toolObjects(p2pSyncMcpTools)
				.build();
	}
}