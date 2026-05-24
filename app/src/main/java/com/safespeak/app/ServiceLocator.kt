package com.safespeak.app

import android.content.Context
import com.safespeak.app.ai.ToxicityClassifier
import com.safespeak.app.data.local.AppDatabase
import com.safespeak.app.data.repository.MessageRepository
import com.safespeak.app.domain.engine.MessageAnalysisEngine
import com.safespeak.app.domain.middleware.MessageAnalysisMiddleware

/**
 * Lightweight service locator. Hilt would normally fill this role
 * (the report mentions it) but to keep the project build-clean without
 * extra annotation processors we wire singletons by hand.
 */
object ServiceLocator {

    @Volatile private var classifier: ToxicityClassifier? = null
    @Volatile private var repository: MessageRepository? = null
    @Volatile private var engine: MessageAnalysisEngine? = null
    @Volatile private var middleware: MessageAnalysisMiddleware? = null

    fun classifier(context: Context): ToxicityClassifier =
        classifier ?: synchronized(this) {
            classifier ?: ToxicityClassifier(context.applicationContext).also { classifier = it }
        }

    fun repository(context: Context): MessageRepository =
        repository ?: synchronized(this) {
            repository ?: MessageRepository(AppDatabase.get(context).messageDao())
                .also { repository = it }
        }

    fun engine(context: Context): MessageAnalysisEngine =
        engine ?: synchronized(this) {
            engine ?: MessageAnalysisEngine(classifier(context)).also { engine = it }
        }

    fun middleware(context: Context): MessageAnalysisMiddleware =
        middleware ?: synchronized(this) {
            middleware ?: MessageAnalysisMiddleware(engine(context), repository(context))
                .also { middleware = it }
        }
}
