package pw.binom

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.withTimeoutOrNull
import pw.binom.BackgroundService.Companion.CHANNEL
import kotlin.time.Duration.Companion.seconds

class MainActivity : ComponentActivity() {
    private val contextVariableHolder = ContextVariableHolder(this)
    private val exchange by contextVariableHolder.define { ctx ->
        val service = ExchangeService(
            context = ctx,
            broadcastChannel = CHANNEL,
            server = false,
        )
        service.reg()
        service
    }.destroyWith {
        it.unreg()
    }

    init {
        contextVariableHolder.define {
            exchange.implements(Methods.sendServiceState) {
                state = it
                Methods.OK
            }
        }
    }

    var state by mutableStateOf<Methods.ServiceState?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BackgroundService.control(this@MainActivity,true)
        contextVariableHolder.create()
        setContent {
//            LaunchedEffect(null) {
//                state = withTimeoutOrNull(1.seconds) {
//                    exchange.call(
//                        Methods.getServiceStatus,
//                        Methods.GetServiceState,
//                    )
//                }
//            }


            MaterialTheme {
                Column(
//                    horizontalArrangement = Arrangement.Center,
//                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
//                        .background(Color.White),
                ) {
                    Box {
                        val status1 = if (state?.running ?: false) "Запущено" else "Не запущено"
                        Text(text = status1, fontSize = 28.sp, color = Color.White)
                    }
                    Box {
                        Spacer(modifier = Modifier.height(4.dp))
                        val status2 = if (state?.connected ?: false) "Подключен" else "Не подключен"
                        Text(text = status2, fontSize = 28.sp, color = Color.White)
                    }
                    Box {
                        Button(onClick = {
                            BackgroundService.control(this@MainActivity,true)
                        }) {
                            Text("START")
                        }
                    }
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onStop() {
        contextVariableHolder.destroy()
        super.onStop()
    }
}