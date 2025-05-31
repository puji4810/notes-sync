package puji.p2p_notes_sync.config.app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;

@Configuration
@EnableWebFlux
public class WebFluxConfig implements WebFluxConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		// 添加静态资源处理器 - 避免使用 /** 以免拦截 API 请求
		registry.addResourceHandler("/css/**", "/js/**", "/images/**")
				.addResourceLocations("classpath:/static/css/", "classpath:/static/js/", "classpath:/static/images/")
				.resourceChain(false);

		// 处理 HTML 页面
		registry.addResourceHandler("/", "/index.html", "/peers.html", "/settings.html", "/test.html", "/demo.html")
				.addResourceLocations("classpath:/static/")
				.resourceChain(false);

		// 为Swagger UI添加资源处理器
		registry.addResourceHandler("/swagger-ui/**")
				.addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/")
				.resourceChain(false);

		registry.addResourceHandler("/webjars/**")
				.addResourceLocations("classpath:/META-INF/resources/webjars/")
				.resourceChain(false);
	}
}
