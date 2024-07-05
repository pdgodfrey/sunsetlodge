package us.pgodfrey.sunsetlodge.helpers

import com.github.jknack.handlebars.Options
import com.github.jknack.handlebars.Template
import io.vertx.core.impl.logging.LoggerFactory

class HandleBarsHelperSource {
  val logger = LoggerFactory.getLogger(javaClass)

    fun checkListLengthIsGreaterThan(param0: List<Any>, param1: Int, options: Options): CharSequence? {
        return if (param0.size > param1) {
            options.fn(this)
        } else options.inverse(this)

    }
}
