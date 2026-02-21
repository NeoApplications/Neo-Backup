package com.machiav3lli.backup.utils

import org.w3c.dom.Element
import org.w3c.dom.NodeList


fun NodeList.toElementList(): List<Element> =
    (0 until length).mapNotNull { item(it) as? Element }

fun Element.firstChildElement(tagName: String): Element? =
    childNodes.toElementList().firstOrNull { it.tagName == tagName }

fun Element.namedString(fieldName: String): String? =
    namedElement("string", fieldName)?.textContent?.takeIf { it.isNotBlank() }

fun Element.namedBoolean(fieldName: String): Boolean? =
    namedElement("boolean", fieldName)?.getAttribute("value")
        ?.let { it.equals("true", ignoreCase = true) }

fun Element.namedInt(fieldName: String): Int? =
    namedElement("int", fieldName)?.getAttribute("value")?.toIntOrNull()

fun Element.namedByteArray(fieldName: String): String? =
    namedElement("byte-array", fieldName)?.textContent?.trim()

private fun Element.namedElement(tag: String, fieldName: String): Element? =
    getElementsByTagName(tag)
        .toElementList()
        .firstOrNull { it.getAttribute("name") == fieldName }
        ?.takeUnless { it.tagName == "null" }
