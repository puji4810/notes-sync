package puji.p2p_notes_sync.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 前端路由控制器，确保SPA应用的路由能够正常工作
 */
@RestController
public class FrontendController {

	/**
	 * 将主应用路径映射到index.html
	 * 
	 * @return 返回index.html内容
	 */
	@GetMapping(value = { "/", "/dashboard", "/dashboard/**" }, produces = MediaType.TEXT_HTML_VALUE)
	public Mono<ResponseEntity<Resource>> index() {
		Resource resource = new ClassPathResource("static/index.html");
		return Mono.just(ResponseEntity.ok()
				.contentType(MediaType.TEXT_HTML)
				.body(resource));
	}

	/**
	 * 将peers页面路径映射到peers.html
	 * 避免与/api/v1/peers API路径冲突
	 * 
	 * @return 返回peers.html内容
	 */
	@GetMapping(value = { "/peers.html", "/peers/view", "/peers/page" }, produces = MediaType.TEXT_HTML_VALUE)
	public Mono<ResponseEntity<Resource>> peers() {
		Resource resource = new ClassPathResource("static/peers.html");
		return Mono.just(ResponseEntity.ok()
				.contentType(MediaType.TEXT_HTML)
				.body(resource));
	}

	/**
	 * 将settings页面路径映射到settings.html
	 * 避免与/api/v1/settings API路径冲突
	 * 
	 * @return 返回settings.html内容
	 */
	@GetMapping(value = { "/settings.html", "/settings/view", "/settings/page" }, produces = MediaType.TEXT_HTML_VALUE)
	public Mono<ResponseEntity<Resource>> settings() {
		Resource resource = new ClassPathResource("static/settings.html");
		return Mono.just(ResponseEntity.ok()
				.contentType(MediaType.TEXT_HTML)
				.body(resource));
	}
}
