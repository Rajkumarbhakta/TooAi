package com.rkbapps.tooai.utils

import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.rkbapps.tooai.db.entity.LlmModel

// Where a model runs. Stored on the model row by name.
enum class ModelBackend(val label: String) {
    CPU("CPU"),
    GPU("GPU");

    companion object {
        // Anything unrecognised runs on CPU, which every model and phone supports.
        fun of(name: String?): ModelBackend = entries.firstOrNull { it.name == name } ?: CPU
    }
}

// An initialised engine and the backend it actually ended up on.
class LoadedEngine(val engine: Engine, val backend: ModelBackend)

/**
 * Loads [model] on its chosen backend. Whether GPU works depends on both the model file and the
 * phone, and only trying it tells, so a GPU load that fails is retried on CPU. The result says
 * which one it got.
 *
 * Blocking, and it cannot be interrupted. Call it off the main thread.
 */
fun openEngine(model: LlmModel): LoadedEngine {
    if (ModelBackend.of(model.backend) == ModelBackend.GPU) {
        try {
            return LoadedEngine(initEngine(model, Backend.GPU()), ModelBackend.GPU)
        } catch (e: Exception) {
            Log.w("ModelBackend", "GPU load failed for ${model.displayName}, using CPU", e)
        }
    }
    return LoadedEngine(initEngine(model, Backend.CPU()), ModelBackend.CPU)
}

private fun initEngine(model: LlmModel, backend: Backend): Engine {
    val engine = Engine(
        EngineConfig(
            modelPath = model.path,
            backend = backend,
            visionBackend = null,
            audioBackend = null,
            maxNumTokens = model.maxTokens
        )
    )
    try {
        engine.initialize()
        return engine
    } catch (e: Throwable) {
        // A half-initialised engine still holds native memory.
        runCatching { engine.close() }
        throw e
    }
}
