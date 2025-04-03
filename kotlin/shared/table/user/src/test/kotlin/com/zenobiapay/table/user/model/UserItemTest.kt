package com.zenobiapay.table.user.model

import kotlin.test.Test
import kotlin.test.assertEquals

class UserItemTest {
    @Test
    fun `test getSub regex parses regex`() {
        val sub = "testSub|withWeirdCharacters"
        val pk = UserItem.generatePk(sub)
        val item = UserItem(pk = pk)
        assertEquals(sub, item.getSub())
    }
}