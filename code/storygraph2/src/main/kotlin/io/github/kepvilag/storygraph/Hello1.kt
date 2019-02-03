package io.github.kepvilag.storygraph

import kotlin.browser.*
import kotlinx.html.*
import kotlinx.html.dom.*

fun printHello() {
    window.onload = {
        document.body!!.append.div {
            span {
                +"Hello"
            }
        }
    }
}