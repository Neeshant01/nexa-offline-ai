package com.nexa.offlineai.ai.di

import com.nexa.offlineai.ai.engine.AssistantGatewayImpl
import com.nexa.offlineai.ai.local.GemmaTokenizer
import com.nexa.offlineai.ai.local.LocalModelRuntimeAdapter
import com.nexa.offlineai.ai.local.SimpleGemmaTokenizer
import com.nexa.offlineai.ai.local.StubGemmaRuntimeAdapter
import com.nexa.offlineai.domain.repository.AssistantGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {
    @Binds
    abstract fun bindAssistantGateway(impl: AssistantGatewayImpl): AssistantGateway

    @Binds
    abstract fun bindTokenizer(impl: SimpleGemmaTokenizer): GemmaTokenizer

    @Binds
    abstract fun bindRuntimeAdapter(impl: StubGemmaRuntimeAdapter): LocalModelRuntimeAdapter
}
