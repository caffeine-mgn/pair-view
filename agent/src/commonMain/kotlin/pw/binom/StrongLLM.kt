package pw.binom

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject
import pw.binom.llm.LLM
import pw.binom.strong.BeanLifeCycle

class StrongLLM(delegate: Lazy<LLM>) : LLM {
    private val delegate by delegate

    init {
        BeanLifeCycle.postConstruct {
            delegate.value
        }
    }

    override suspend fun send(
        model: String,
        temperature: Float,
        tools: List<JsonObject>?,
        messages: Flow<LLM.Message>,
    ): LLM.Result = delegate.send(
        model = model,
        temperature = temperature,
        tools = tools,
        messages = messages,
    )
}