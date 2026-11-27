package com.example.core.governance

import com.example.model.AppLanguage

/**
 * Single-language localization helper.
 * Ensures the UI renders strictly ONE language at a time (Arabic, English, or French)
 * without mixing languages in customer or governance views.
 */
fun tr(
  lang: AppLanguage,
  ar: String,
  en: String,
  fr: String = en
): String = when (lang) {
  AppLanguage.ARABIC -> ar
  AppLanguage.ENGLISH -> en
  AppLanguage.FRENCH -> fr
}
