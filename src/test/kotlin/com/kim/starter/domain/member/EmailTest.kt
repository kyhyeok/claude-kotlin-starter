package com.kim.starter.domain.member

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Email VO")
class EmailTest {
    @Test
    fun `정상 형식이면 통과한다`() {
        val email = Email("user@example.com")

        assertThat(email.value).isEqualTo("user@example.com")
    }

    @Test
    fun `빈 문자열은 거부한다`() {
        assertThatThrownBy { Email("") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("비어있을 수 없습니다")
    }

    @Test
    fun `255자를 초과하면 거부한다`() {
        val tooLong = "a".repeat(250) + "@a.com"

        assertThatThrownBy { Email(tooLong) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("255자")
    }

    @Test
    fun `골뱅이가 없으면 거부한다`() {
        assertThatThrownBy { Email("noatsign.com") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("올바른 이메일 형식")
    }
}
