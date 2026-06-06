package com.hobsojam.simpleestimation

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ScaffoldTest :
    FunSpec({
        test("package namespace matches application ID") {
            BuildConfig.APPLICATION_ID shouldBe "com.hobsojam.simpleestimation"
        }
    })
