package puji.p2p_notes_sync.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 用于创建特定类型的ResponseEntity的工具类
 */
public class ResponseEntityUtil {

	/**
	 * 创建一个400 Bad Request响应，具有指定的泛型类型
	 * 
	 * @param <T> 响应体的类型
	 * @return 带有null主体的400响应
	 */
	public static <T> ResponseEntity<T> badRequest() {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
	}

	/**
	 * 创建一个404 Not Found响应，具有指定的泛型类型
	 * 
	 * @param <T> 响应体的类型
	 * @return 带有null主体的404响应
	 */
	public static <T> ResponseEntity<T> notFound() {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
	}
}
