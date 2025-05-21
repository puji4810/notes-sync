package puji.p2p_notes_sync.p2p.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;

// 定义P2P网络中仓库配置变更的消息体
// 注意：这个消息体不应包含敏感信息，如Git Token
@JsonTypeName("CONFIG_REPO") // 必须与P2PMessage中定义的name匹配
public class RepoConfigP2PNotification extends P2PMessage {
	public enum Action {
		ADD, REMOVE, UPDATE
	}

	private Action action; // "ADD" 或 "REMOVE" 或 "UPDATE"
	private String repoAlias; // 仓库别名（对于UPDATE操作，这是新的别名）
	private String oldRepoAlias; // 仓库旧别名（仅用于UPDATE操作，当别名变更时）
	private String repoUrl; // 仓库URL (对于ADD和UPDATE操作)
	// 对于REMOVE操作，repoAlias可能就足够了

	// 构造函数, getters, setters

	public RepoConfigP2PNotification() {
	}

	public RepoConfigP2PNotification(Action action, String repoAlias, String repoUrl) {
		this.action = action;
		this.repoAlias = repoAlias;
		this.oldRepoAlias = repoAlias; // 默认旧别名与新别名相同
		this.repoUrl = repoUrl;
	}

	// 增加一个带oldRepoAlias参数的构造函数，用于别名更改场景
	public RepoConfigP2PNotification(Action action, String oldRepoAlias, String newRepoAlias, String repoUrl) {
		this.action = action;
		this.repoAlias = newRepoAlias;
		this.oldRepoAlias = oldRepoAlias;
		this.repoUrl = repoUrl;
	}

	@Override
	public String getType() {
		return "CONFIG_REPO";
	}

	public Action getAction() {
		return action;
	}

	public void setAction(Action action) {
		this.action = action;
	}

	public String getRepoAlias() {
		return repoAlias;
	}

	public void setRepoAlias(String repoAlias) {
		this.repoAlias = repoAlias;
	}

	public String getOldRepoAlias() {
		return oldRepoAlias;
	}

	public void setOldRepoAlias(String oldRepoAlias) {
		this.oldRepoAlias = oldRepoAlias;
	}

	public String getRepoUrl() {
		return repoUrl;
	}

	public void setRepoUrl(String repoUrl) {
		this.repoUrl = repoUrl;
	}

	@Override
	public String toString() {
		return "RepoConfigP2PNotification{" +
				"action=" + action +
				", repoAlias='" + repoAlias + '\'' +
				", oldRepoAlias='" + oldRepoAlias + '\'' +
				", repoUrl='" + repoUrl + '\'' +
				'}';
	}
}