package com.rkbapps.tooai.utils

object PredefinePrompts {

    const val REWRITE_FORMAL = "Rewrite the following text using a formal tone: "
    const val REWRITE_CASUAL = "Rewrite the following text using a casual tone: "
    const val REWRITE_FRIENDLY = "Rewrite the following text using a friendly tone: "


    const val SUMMARY_BULLET_POINT = "Please summarize the following in key bullet points (3-5): "
    const val SUMMARY_SHORT_PARAGRAPH = "Please summarize the following in short paragraph (1-2 sentences): "
    const val SUMMARY_CONCISE = "Please summarize the following in concise summary (~50 words): "

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
            subType = RewritePromptType.Formal.name,
            prompt = REWRITE_FORMAL
        ),
        Prompts(
            type = TypeOfPrompt.Rewrite,
            subType = RewritePromptType.Casual.name,
            prompt = REWRITE_CASUAL
        ),
        Prompts(
            type = TypeOfPrompt.Rewrite,
            subType = RewritePromptType.Friendly.name,
            prompt = REWRITE_FRIENDLY
        ),
        // summary
        Prompts(
            type = TypeOfPrompt.Summary,
            subType = SummaryPromptType.BulletPoint.name,
            prompt = SUMMARY_BULLET_POINT
        ),Prompts(
            type = TypeOfPrompt.Summary,
            subType = SummaryPromptType.ShortParagraph.name,
            prompt = SUMMARY_SHORT_PARAGRAPH
        ),Prompts(
            type = TypeOfPrompt.Summary,
            subType = SummaryPromptType.Concise.name,
            prompt = SUMMARY_CONCISE
        ),
        // code snippet
        Prompts(
            type = TypeOfPrompt.CodeSnippet,
            subType = CodeSnippetPromptType.Cpp.name,
            prompt = CODE_SNIPPET_CPP
        ),Prompts(
            type = TypeOfPrompt.CodeSnippet,
            subType = CodeSnippetPromptType.Java.name,
            prompt = CODE_SNIPPET_JAVA
        ),Prompts(
            type = TypeOfPrompt.CodeSnippet,
            subType = CodeSnippetPromptType.Kotlin.name,
            prompt = CODE_SNIPPET_KOTLIN
        ),Prompts(
            type = TypeOfPrompt.CodeSnippet,
            subType = CodeSnippetPromptType.Python.name,
            prompt = CODE_SNIPPET_PYTHON
        ),Prompts(
            type = TypeOfPrompt.CodeSnippet,
            subType = CodeSnippetPromptType.Swift.name,
            prompt = CODE_SNIPPET_SWIFT
        ),Prompts(
            type = TypeOfPrompt.CodeSnippet,
            subType = CodeSnippetPromptType.TypeScript.name,
            prompt = CODE_SNIPPET_TYPESCRIPT
        ),Prompts(
            type = TypeOfPrompt.CodeSnippet,
            subType = CodeSnippetPromptType.JavaScript.name,
            prompt = CODE_SNIPPET_JAVA_SCRIPT
        ),

    )

}

enum class TypeOfPrompt(val displayString: String) {
    Rewrite("Rewrite"),
    Summary("Summary"),
    CodeSnippet("Code Snippet")
}


data class Prompts(
    val type: TypeOfPrompt,
    val subType: String,
    val prompt: String
)



enum class RewritePromptType() {
    Formal,
    Casual,
    Friendly
}

enum class SummaryPromptType {
    BulletPoint,
    ShortParagraph,
    Concise
}

enum class CodeSnippetPromptType {
    Cpp,
    Java,
    JavaScript,
    Kotlin,
    Python,
    Swift,
    TypeScript
}

