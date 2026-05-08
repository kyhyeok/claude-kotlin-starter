package com.kim.starter.adapter.webapi

import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

// classpath(bootJar) → filesystem(bootRun) fallback. sourceSet output 등록 시 circular dep 발생 (ADR-0013).
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
