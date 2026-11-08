package top.chiloven.daymark.core.model

/**
 * Stable built-in categories for the first release. Custom categories can be
 * added later without changing the event schema because events only store an id.
 */
object DefaultCategories {

    val Birthday = Category(
        id = 1L,
        name = "Birthday",
        colorArgb = 0xFFE85D75,
        iconKey = "cake",
        sortOrder = 0,
    )
    val Anniversary = Category(
        id = 2L,
        name = "Anniversary",
        colorArgb = 0xFF7C6CF2,
        iconKey = "favorite",
        sortOrder = 1,
    )
    val Holiday = Category(
        id = 3L,
        name = "Holiday",
        colorArgb = 0xFF2AA876,
        iconKey = "celebration",
        sortOrder = 2,
    )
    val Milestone = Category(
        id = 4L,
        name = "Milestone",
        colorArgb = 0xFF2E86DE,
        iconKey = "flag",
        sortOrder = 3,
    )
    val Other = Category(
        id = 5L,
        name = "Other",
        colorArgb = 0xFF8A8A8A,
        iconKey = "event",
        sortOrder = 4,
    )

    val all: List<Category> = listOf(Birthday, Anniversary, Holiday, Milestone, Other)

    fun byId(id: Long?): Category? =
        all.firstOrNull { it.id == id }

}
