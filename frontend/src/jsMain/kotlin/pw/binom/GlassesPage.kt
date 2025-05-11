package pw.binom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.max
import org.jetbrains.compose.web.attributes.min
import org.jetbrains.compose.web.dom.Input

@Composable
fun GlassesPage(id: String) {
    var fileName by remember { mutableStateOf<String?>(null) }
    var videoDuration by remember { mutableStateOf(0L) }

    LaunchedEffect(id, fileName) {

    }

    Input(InputType.Range) {
        min("-200")
        max("200")
        value(0)
        this.onChange {
            GlobalScope.launch {
                Api.updateView(id = id, padding = it.value!!.toInt(), align = 0)
            }
        }
    }
}