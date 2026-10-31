package com.example.agritech_mobile.ui.order

import com.example.agritech_mobile.data.remote.dto.GhnArea
import com.example.agritech_mobile.data.remote.dto.GhnShop
import org.junit.Assert.*
import org.junit.Test

class GhnRecipientAddressTest {
    private val province = GhnArea("1000016", "Tỉnh Ninh Bình")
    private val ward = GhnArea("123", "Xã Hải Hưng")

    @Test fun fillsFromBuyersAddressWithOrWithoutPrefixesAndAccents() {
        for (address in listOf("689H+P9Q, Hải Hưng, Ninh Bình", "Xóm 12\n, Xã Hải Hưng, Tỉnh Ninh Bình",
            "So 12, xa Hai Hung, tinh Ninh Binh, Viet Nam")) {
            assertEquals(province, matchRecipientProvince(address, listOf(province)))
            assertEquals(ward, matchRecipientWard(address, province, listOf(ward)))
        }
    }

    @Test fun doesNotGuessOldOrAmbiguousOrPartialNames() {
        assertNull(matchRecipientProvince("Xóm 9, Hải Long, Nam Định", listOf(province)))
        assertNull(matchRecipientWard("Số 12, Tân Hải Hưng, Ninh Bình", province, listOf(ward)))
        assertNull(matchRecipientWard("Hải Hưng, Ninh Bình", province, listOf(ward, GhnArea("456", "Xã Hải Hưng"))))
        assertNull(matchRecipientWard("Hải Hưng, Hà Nội", province, listOf(ward)))
        assertNull(matchRecipientProvince("", listOf(province)))
    }

    @Test fun senderPrefersAgriTechPickupAndCanMatchAnExplicitWardBeforeDistrict() {
        val shop = GhnShop(true, false, "staging", 123, "Shop", "Old GHN address", suggestedPickupAddress = "Xóm 9, Hải Hưng, Ninh Bình")
        assertEquals("Xóm 9, Hải Hưng, Ninh Bình", suggestedSenderAddress(shop))
        assertEquals("Old GHN address", suggestedSenderAddress(shop.copy(suggestedPickupAddress = " ")))
        assertEquals(ward, matchRecipientWard("Xóm 9, Hải Hưng, Hải Hậu, Ninh Bình", province, listOf(ward)))
    }
}
