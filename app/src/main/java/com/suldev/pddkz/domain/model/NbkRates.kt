package com.suldev.pddkz.domain.model

/** Курсы НБ РК в тенге. Дефолты — оффлайн-фолбэк §2.2; `date == null` значит «с сервера ещё не приходили». */
data class NbkRates(
    val cnyKzt: Double = 68.11,
    val usdKzt: Double = 453.71,
    val date: String? = null,
    val source: String? = null,
)

/** Статус загрузки курсов и его метка в заголовке карточки (§2.2). */
sealed interface RatesStatus {
    val label: String

    data object Loading : RatesStatus {
        override val label = "загрузка…"
    }

    data class Fresh(val date: String) : RatesStatus {
        override val label = "на $date"
    }

    data object Offline : RatesStatus {
        override val label = "нет связи с НБ РК"
    }

    data class Cached(val date: String) : RatesStatus {
        override val label = "кэш от $date · нет связи с НБ РК"
    }
}

data class RatesState(
    val rates: NbkRates = NbkRates(),
    val status: RatesStatus = RatesStatus.Loading,
)
