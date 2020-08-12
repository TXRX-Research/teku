package tech.pegasys.teku.phase1.util

import tech.pegasys.teku.phase1.onotole.phase1.BLSSignature
import tech.pegasys.teku.phase1.onotole.phase1.Root
import java.time.LocalDateTime

fun printRoot(root: Root): String = root.toString().substring(0..10)
fun printSignature(signature: BLSSignature): String = signature.toString().substring(0..18)
fun log(msg: String, color: Color = Color.NONE) {
  val output = "${LocalDateTime.now()} - $msg"
  if (color != Color.NONE) {
    println("${color.code}$output${RESET_CODE}")
  } else {
    println(output)
  }
}

private var isDebugEnabled = false
fun logSetDebugMode(debug: Boolean) {
  isDebugEnabled = debug
}

fun logDebug(lazyMsg: () -> String) {
  if (isDebugEnabled) {
    log("[${Thread.currentThread().name}]: ${lazyMsg()}", Color.WHITE)
  }
}

enum class Color(val code: String) {
  BLACK("\u001b[30m"),
  RED("\u001b[31m"),
  GREEN("\u001b[32m"),
  YELLOW("\u001b[33m"),
  BLUE("\u001b[34m"),
  PURPLE("\u001b[35m"),
  CYAN("\u001b[36m"),
  WHITE("\u001b[37m"),
  NONE("")
}

private const val RESET_CODE = "\u001B[0m"
