package ovh.plrapps.mapcompose.utils

object DpConverter {
    var density: Float = 96f / 160f
    fun dpToPx(dp: Float) = dp * density
}