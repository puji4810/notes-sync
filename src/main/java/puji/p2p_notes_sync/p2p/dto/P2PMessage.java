package puji.p2p_notes_sync.p2p.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

// 使用Jackson的类型信息注解来帮助反序列化为正确的子类型
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type" // 这个属性将决定消息的具体类型
)
@JsonSubTypes({
		@JsonSubTypes.Type(value = RepoConfigP2PNotification.class, name = "CONFIG_REPO"),
		@JsonSubTypes.Type(value = RepoSyncP2PRequest.class, name = "REQUEST_SYNC")
// 未来可以添加更多消息类型
})
public abstract class P2PMessage {
	// 可以包含所有消息共有的字段，比如发送者ID，时间戳等
	// private String senderId;
	// private long timestamp;

	public abstract String getType(); // 子类需要实现此方法以匹配 @JsonSubTypes.Type 中的 name
}