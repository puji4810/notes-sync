package puji.p2p_notes_sync.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("P2P笔记同步平台 API")
						.description("用于P2P笔记同步与发布平台的RESTful API接口文档")
						.version("v1.0.0"));
	}
}
