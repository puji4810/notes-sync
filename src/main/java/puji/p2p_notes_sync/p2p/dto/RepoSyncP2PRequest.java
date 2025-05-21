package puji.p2p_notes_sync.p2p.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("REQUEST_SYNC") // 必须与P2PMessage中定义的name匹配
public class RepoSyncP2PRequest extends P2PMessage {
	private String repoUrlOrAlias; // 要同步的仓库URL或别名

	// 构造函数, getters, setters
	public RepoSyncP2PRequest() {
	}

	public RepoSyncP2PRequest(String repoUrlOrAlias) {
		this.repoUrlOrAlias = repoUrlOrAlias;
	}

	@Override
	public String getType() {
		return "REQUEST_SYNC";
	}

	public String getRepoUrlOrAlias() {
		return repoUrlOrAlias;
	}

	public void setRepoUrlOrAlias(String repoUrlOrAlias) {
		this.repoUrlOrAlias = repoUrlOrAlias;
	}

	@Override
	public String toString() {
		return "RepoSyncP2PRequest{" +
				"repoUrlOrAlias='" + repoUrlOrAlias + '\'' +
				'}';
	}
}
