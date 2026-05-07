package com.kim.starter.adapter.webapi

import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * REST Docs로 생성된 OpenAPI 3 spec을 호스팅한다.
 *
 * 두 가지 경로를 fallback 순으로 시도한다:
 * 1. `static/api-spec/openapi3.yaml` (classpath) — bootJar에 박힌 경우. 운영/배포 환경.
 * 2. `build/api-spec/openapi3.yaml` (filesystem) — `./gradlew bootRun` 같은 개발 환경.
 *
 * spec이 둘 다 없으면 404 — 사용자에게 `./gradlew build`로 spec을 먼저 만들도록 알린다.
 *
 * `/swagger-ui.html`(static)이 이 endpoint를 fetch하여 화면에 렌더링한다.
 */
@RestController
class OpenApiSpecController {
    @GetMapping("/api-spec/openapi3.yaml", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun openApiSpec(): ResponseEntity<Resource> {
        val resource = locateSpec()
        return if (resource == null) {
            ResponseEntity.notFound().build()
        } else {
            ResponseEntity.ok().contentType(MediaType.parseMediaType("application/yaml")).body(resource)
        }
    }

    private fun locateSpec(): Resource? {
        val classpath = ClassPathResource("static/api-spec/openapi3.yaml")
        if (classpath.exists()) return classpath
        val devFile = FileSystemResource("build/api-spec/openapi3.yaml")
        return if (devFile.exists()) devFile else null
    }
}
