package com.rkbapps.tooai.utils

object PredefinePrompts {

    const val REWRITE_FORMAL = "Rewrite the following text using a formal tone: "
    const val REWRITE_CASUAL = "Rewrite the following text using a casual tone: "
    const val REWRITE_FRIENDLY = "Rewrite the following text using a friendly tone: "


    const val SUMMARY_BULLET_POINT = "Please summarize the following in key bullet points (3-5): "
    const val SUMMARY_SHORT_PARAGRAPH = "Please summarize the following in short paragraph (1-2 sentences): "
    const val SUMMARY_CONCISE = "Please summarize the following in concise summary (~50 words): "

    const val PROOFREAD_GRAMMAR =
        "Correct only the spelling, grammar, and punctuation in the following text. " +
            "Keep the original wording, tone, and meaning, and do not rephrase anything: "
    const val PROOFREAD_CLARITY =
        "Correct the spelling, grammar, and punctuation in the following text, and rephrase any " +
            "unclear or awkward parts so it reads smoothly. Keep the original meaning: "
    const val PROOFREAD_CONCISE =
        "Correct the spelling, grammar, and punctuation in the following text, then tighten it by " +
            "removing redundancy and filler. Keep the original meaning: "

    const val CODE_SNIPPET_CPP = "Write a C++ code snippet to "
    const val CODE_SNIPPET_JAVA = "Write a Java code snippet to "
    const val CODE_SNIPPET_JAVA_SCRIPT= "Write a JavaScript code snippet to "
    const val CODE_SNIPPET_KOTLIN = "Write a Kotlin code snippet to "
    const val CODE_SNIPPET_PYTHON = "Write a Python code snippet to "
    const val CODE_SNIPPET_SWIFT = "Write a Swift code snippet to "
    const val CODE_SNIPPET_TYPESCRIPT = "Write a TypeScript code snippet to "



    val listOfPrompts = listOf(
        //rewrite
        Prompts(
            type = TypeOfPrompt.Rewrite,
            subType = RewritePromptType.Formal.displayString,
            prompt = REWRITE_FORMAL
        ),
        Prompts(
            type = TypeOfPrompt.Rewrite,
            subType = RewritePromptType.Casual.displayString,
            prompt = REWRITE_CASUAL
        ),
        Prompts(
            type = TypeOfPrompt.Rewrite,
            subType = RewritePromptType.Friendly.displayString,
            prompt = REWRITE_FRIENDLY
        ),
        // summary
        Prompts(
            type = TypeOfPrompt.Summary,
            subType = SummaryPromptType.BulletPoint.displayString,
            prompt = SUMMARY_BULLET_POINT
        ),Prompts(
            type = TypeOfPrompt.Summary,
            subType = SummaryPromptType.ShortParagraph.displayString,
            prompt = SUMMARY_SHORT_PARAGRAPH
        ),Prompts(
            type = TypeOfPrompt.Summary,
            subType = SummaryPromptType.Concise.displayString,
            prompt = SUMMARY_CONCISE
        ),
        // proofread
        Prompts(
            type = TypeOfPrompt.Proofread,
            subType = ProofreadPromptType.Grammar.displayString,
            prompt = PROOFREAD_GRAMMAR
        ),
        Prompts(
            type = TypeOfPrompt.Proofread,
            subType = ProofreadPromptType.Clarity.displayString,
            prompt = PROOFREAD_CLARITY
        ),
        Prompts(
            type = TypeOfPrompt.Proofread,
            subType = ProofreadPromptType.Concise.displayString,
            prompt = PROOFREAD_CONCISE
        ),
        // code snippet
        Prompts(
            type = TypeOfPrompt.CodeSnippet,
            subType = CodeSnippetPromptType.Cpp.displayString,
            prompt = CODE_SNIPPET_CPP
        ),Prompts(
            type = TypeOfPrompt.CodeSnippet,
            subType = CodeSnippetPromptType.Java.displayString,
            prompt = CODE_SNIPPET_JAVA
        ),Prompts(
            type = TypeOfPrompt.CodeSnippet,
            subType = CodeSnippetPromptType.Kotlin.displayString,
            prompt = CODE_SNIPPET_KOTLIN
        ),Prompts(
            type = TypeOfPrompt.CodeSnippet,
            subType = CodeSnippetPromptType.Python.displayString,
            prompt = CODE_SNIPPET_PYTHON
        ),Prompts(
            type = TypeOfPrompt.CodeSnippet,
            subType = CodeSnippetPromptType.Swift.displayString,
            prompt = CODE_SNIPPET_SWIFT
        ),Prompts(
            type = TypeOfPrompt.CodeSnippet,
            subType = CodeSnippetPromptType.TypeScript.displayString,
            prompt = CODE_SNIPPET_TYPESCRIPT
        ),Prompts(
            type = TypeOfPrompt.CodeSnippet,
            subType = CodeSnippetPromptType.JavaScript.displayString,
            prompt = CODE_SNIPPET_JAVA_SCRIPT
        ),

    )

}

enum class TypeOfPrompt(val displayString: String) {
    Rewrite("Rewrite"),
    Summary("Summary"),
    Proofread("Proofread"),
    CodeSnippet("Code Snippet")
}


data class Prompts(
    val type: TypeOfPrompt,
    val subType: String,
    val prompt: String
)



enum class RewritePromptType(val displayString: String) {
    Formal("Formal"),
    Casual("Casual"),
    Friendly("Friendly")
}

enum class SummaryPromptType(val displayString: String) {
    BulletPoint("Bullet points"),
    ShortParagraph("Short paragraph"),
    Concise("Concise")
}

/** Escalating levels of intervention: fix errors only, then smooth it, then tighten it. */
enum class ProofreadPromptType(val displayString: String) {
    Grammar("Grammar"),
    Clarity("Clarity"),
    Concise("Concise")
}

enum class CodeSnippetPromptType(val displayString: String) {
    Cpp("C++"),
    Java("Java"),
    JavaScript("JavaScript"),
    Kotlin("Kotlin"),
    Python("Python"),
    Swift("Swift"),
    TypeScript("TypeScript")
}

