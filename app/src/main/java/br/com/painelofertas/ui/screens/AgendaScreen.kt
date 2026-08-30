package br.com.painelofertas.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.painelofertas.data.ScheduledTask
import br.com.painelofertas.ui.components.EmptyState
import br.com.painelofertas.ui.components.SectionLabel
import br.com.painelofertas.ui.rememberContainer
import java.util.Calendar
import java.util.UUID

@Composable
fun AgendaScreen() {
    val container = rememberContainer()
    var tarefas by remember { mutableStateOf(container.schedule.list()) }
    val albuns by container.albums.names.collectAsState()

    val now = remember { Calendar.getInstance() }
    var dia by remember { mutableStateOf(now.get(Calendar.DAY_OF_MONTH).toString()) }
    var mes by remember { mutableStateOf((now.get(Calendar.MONTH) + 1).toString()) }
    var ano by remember { mutableStateOf(now.get(Calendar.YEAR).toString()) }
    var hora by remember { mutableStateOf("08") }
    var minuto by remember { mutableStateOf("00") }
    var album by remember { mutableStateOf(albuns.firstOrNull() ?: "") }
    var ip by remember { mutableStateOf("") }
    var diaria by remember { mutableStateOf(false) }
    var brilho by remember { mutableStateOf(100f) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            SectionLabel("Programação")
            Text("Agendamento", style = MaterialTheme.typography.headlineSmall)
        }
        Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Column(Modifier.padding(12.dp)) {
                FilterChip(selected = diaria, onClick = { diaria = !diaria }, label = { Text("Diariamente") })
                if (!diaria) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                        Num("Dia", dia) { dia = it }
                        Num("Mês", mes) { mes = it }
                        Num("Ano", ano) { ano = it }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                    Num("Hora", hora) { hora = it }
                    Num("Min", minuto) { minuto = it }
                }
                Text("Brilho: ${brilho.toInt()}%", style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 6.dp))
                Slider(value = brilho, onValueChange = { brilho = it }, valueRange = 0f..100f)
                Text("Álbum:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 6.dp))
                if (albuns.isEmpty()) {
                    Text("Nenhum álbum salvo — crie um na aba Editar primeiro.", style = MaterialTheme.typography.bodySmall)
                } else {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        albuns.forEach { a -> FilterChip(selected = album == a, onClick = { album = a }, label = { Text(a) }) }
                    }
                }
                OutlinedTextField(ip, { ip = it }, label = { Text("IP do painel") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                Button(
                    onClick = {
                        container.schedule.add(
                            ScheduledTask(
                                id = UUID.randomUUID().toString(),
                                hour = hora.toIntOrNull() ?: 0, minute = minuto.toIntOrNull() ?: 0,
                                day = dia.toIntOrNull() ?: 1, month = mes.toIntOrNull() ?: 1, year = ano.toIntOrNull() ?: 2026,
                                album = album, panelIp = ip,
                                daily = diaria, brightness = brilho.toInt(),
                            )
                        )
                        tarefas = container.schedule.list()
                        container.refreshAlarms() // passa a valer com o app fechado
                    },
                    enabled = album.isNotBlank() && ip.isNotBlank(),
                    modifier = Modifier.padding(top = 8.dp),
                ) { Text("Agendar envio") }
            }
        }

        Text("Agendamentos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        if (tarefas.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.CalendarMonth,
                title = "Nenhum agendamento",
                subtitle = "Configure data, hora e álbum acima e toque em Agendar envio.",
            )
        }
        tarefas.forEach { t ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            (if (t.daily) "Diariamente" else "%02d/%02d/%d".format(t.day, t.month, t.year)) +
                                " às %02d:%02d".format(t.hour, t.minute) +
                                if (t.brightness in 0..100) " · brilho ${t.brightness}%" else ""
                        )
                        Text("→ ${t.album} para ${t.panelIp}", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { container.schedule.remove(t.id); tarefas = container.schedule.list(); container.refreshAlarms() }) {
                        Icon(Icons.Filled.Delete, "Remover")
                    }
                }
            }
        }
    }
}

@Composable
private fun Num(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.width(90.dp),
    )
}
