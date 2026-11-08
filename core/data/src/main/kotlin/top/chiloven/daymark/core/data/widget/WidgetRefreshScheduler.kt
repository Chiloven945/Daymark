package top.chiloven.daymark.core.data.widget

interface WidgetRefreshScheduler {

    fun refreshAll()

    fun refresh(eventId: Long)

}
