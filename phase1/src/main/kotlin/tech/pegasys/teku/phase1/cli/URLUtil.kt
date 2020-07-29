package tech.pegasys.teku.phase1.cli

import java.net.MalformedURLException
import java.net.URL

fun checkURL(url: String) {
  try {
    URL(url)
  } catch (e: MalformedURLException) {
    throw IllegalArgumentException("Invalid URL '$url': ${e.message}")
  }
}
