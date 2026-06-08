package com.hobsojam.simpleestimation.data.websocket

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MoveItemMessageBuilderTest :
    FunSpec({
        test("builds move_item JSON with a non-null position") {
            MoveItemMessageBuilder.build(itemId = "item-1", position = "M") shouldBe
                """{"type":"move_item","itemId":"item-1","position":"M"}"""
        }

        test("builds move_item JSON with null position") {
            MoveItemMessageBuilder.build(itemId = "item-1", position = null) shouldBe
                """{"type":"move_item","itemId":"item-1","position":null}"""
        }
    })
