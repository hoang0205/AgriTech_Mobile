package com.example.agritech_mobile.ui.bargain

import org.junit.Assert.*
import org.junit.Test

class BargainErrorsTest {
    @Test fun hidesLegacySqlErrorEvenWithBadRequestStatus() {
        val result = bargainErrorMessage(400, "could not execute statement [Column 'order_id' cannot be null] [insert into user_notifications]")
        assertFalse(result.contains("order_id"))
        assertFalse(result.contains("insert"))
    }
    @Test fun hidesUnexpectedServerError() {
        assertEquals("Chưa thực hiện được yêu cầu. Vui lòng thử lại sau.", bargainErrorMessage(500, "Internal details"))
    }
    @Test fun preservesBusinessValidationMessage() {
        val message = "Bạn đã có đề nghị đang hiệu lực cho sản phẩm này."
        assertEquals(message, bargainErrorMessage(409, message))
    }
}
