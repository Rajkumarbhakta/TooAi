package com.rkbapps.tooai.utils

import android.util.Log
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/*
 * Since LiteRT-LM 0.17, message content reaches a model's chat template as a list of parts
 * ([{type: text, text: ...}]) rather than a plain string. ChatML templates written for strings
 * (Qwen2.5's, for one) join it with `+` and fail on every message with "Failed to apply template:
 * ... unsupported types string and sequence". For those models the app passes its own ChatML
 * template, which accepts either form. Templates that already handle lists, Gemma's included,
 * are left alone.
 */

private const val TAG = "ChatTemplates"

// The template sits in the file's header, about 16 KB in for the Qwen2.5 build seen so far.
private const val HEADER_SCAN_BYTES = 4 * 1024 * 1024

// Plain ChatML, accepting content as a string or as a list of parts. Kept to the Jinja subset
// LiteRT-LM supports: the `is string` test and item['key'] access are what Gemma's template uses.
internal val CHATML_TEMPLATE = """
{%- if messages[0]['role'] != 'system' -%}
{{- '<|im_start|>system\nYou are a helpful assistant.<|im_end|>\n' -}}
{%- endif -%}
{%- for message in messages -%}
{{- '<|im_start|>' + message['role'] + '\n' -}}
{%- if message['content'] is string -%}
{{- message['content'] -}}
{%- else -%}
{%- for item in message['content'] -%}
{%- if item['type'] == 'text' -%}
{{- item['text'] -}}
{%- endif -%}
{%- endfor -%}
{%- endif -%}
{{- '<|im_end|>\n' -}}
{%- endfor -%}
{%- if add_generation_prompt -%}
{{- '<|im_start|>assistant\n' -}}
{%- endif -%}
""".trim()

/**
 * True when [header] holds a ChatML template that joins content as a string without checking
 * its type, which is the form LiteRT-LM 0.17+ can't render.
 */
internal fun needsChatMlOverride(header: String): Boolean {
    if (!header.contains("<|im_start|>")) return false
    val joinsContent = header.contains("+ message.content") ||
            header.contains("+ message['content']") ||
            header.contains("+ messages[0]['content']") ||
            header.contains("+ messages[0].content")
    val handlesLists = header.contains("content is string") ||
            header.contains("content'] is string")
    return joinsContent && !handlesLists
}

private val overrides = ConcurrentHashMap<String, String>()
private const val NO_OVERRIDE = ""

/**
 * The chat template to pass in ConversationConfig for the model at [modelPath], or null to use
 * the model's own. Reads the file's header once per file and remembers the answer.
 */
fun chatTemplateOverride(modelPath: String): String? {
    val file = File(modelPath)
    val key = "$modelPath:${file.length()}:${file.lastModified()}"
    val cached = overrides.getOrPut(key) {
        val header = try {
            file.inputStream().use { input ->
                val buffer = ByteArray(minOf(HEADER_SCAN_BYTES.toLong(), file.length()).toInt())
                var read = 0
                while (read < buffer.size) {
                    val count = input.read(buffer, read, buffer.size - read)
                    if (count < 0) break
                    read += count
                }
                // Latin-1 maps every byte to one char, so binary data can't break the search.
                String(buffer, 0, read, Charsets.ISO_8859_1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't read the model header", e)
            ""
        }
        if (needsChatMlOverride(header)) {
            Log.i(TAG, "Using the app's ChatML template for ${file.name}")
            CHATML_TEMPLATE
        } else {
            NO_OVERRIDE
        }
    }
    return cached.takeIf { it != NO_OVERRIDE }
}
