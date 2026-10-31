package com.example.agritech_mobile.ui.order

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GhnParcelWeightTest {
    @Test fun kilogramsBecomeGramsWithoutTruncation() {
        assertEquals(2000, parcelWeightGrams("2"))
        assertEquals(500, parcelWeightGrams("0,5"))
        assertEquals(1234, parcelWeightGrams("1.234"))
        assertEquals(1, parcelWeightGrams("0.001"))
        assertEquals(19999, parcelWeightGrams("19.999"))
    }

    @Test fun invalidOrUnsupportedWeightIsRejected() {
        for (value in listOf("", "0", "-1", "20", "2000", "0.0005", "abc")) {
            assertThrows(IllegalArgumentException::class.java) { parcelWeightGrams(value) }
        }
    }
}
