package br.com.painelofertas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import br.com.painelofertas.data.Panel
import br.com.painelofertas.data.PanelStatus

/**
 * Escolha de destino da publicação — o que antes era a aba "Enviar" inteira.
 *
 * Abre a partir da própria barra Publicar: escolhe um painel, vários, um grupo
 * inteiro ou o USB, e ajusta a senha de transmissão ali mesmo. O lojista nunca
 * precisa sair da tela onde monta a oferta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinoSheet(
    paineis: List<Panel>,
    grupos: List<String>,
    ipsDoGrupo: (String) -> List<String>,
    selecionados: Set<String>,
    usbConectado: Boolean,
    usarUsb: Boolean,
    usarSenha: Boolean,
    senha: String,
    onSelecionados: (Set<String>) -> Unit,
    onUsarUsb: (Boolean) -> Unit,
    onUsarSenha: (Boolean) -> Unit,
    onSenha: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = cs.surfaceContainerLow,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Publicar em", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)

            if (paineis.isEmpty() && !usbConectado) {
                Text(
                    "Nenhum painel encontrado. Ligue o painel na mesma rede Wi-Fi do celular — ele aparece sozinho.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                )
            }

            // Grupos: um toque leva a todos do setor.
            if (grupos.isNotEmpty()) {
                SectionLabel("Grupos")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    grupos.forEach { g ->
                        val ips = ipsDoGrupo(g)
                        val todos = ips.isNotEmpty() && selecionados.containsAll(ips)
                        FilterChip(
                            selected = todos,
                            onClick = { onSelecionados(if (todos) selecionados - ips.toSet() else selecionados + ips) },
                            label = { Text("$g (${ips.size})") },
                            leadingIcon = { Icon(Icons.Filled.Groups, null, Modifier.size(16.dp)) },
                        )
                    }
                }
            }

            if (paineis.isNotEmpty()) {
                SectionLabel("Painéis")
                paineis.forEach { p ->
                    val marcado = p.ip in selecionados
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (marcado) cs.secondaryContainer else cs.surfaceContainerHigh)
                            .clickable { onSelecionados(if (marcado) selecionados - p.ip else selecionados + p.ip) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            if (marcado) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                            null,
                            tint = if (marcado) Accent.Green else cs.outline,
                        )
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.titleSmall)
                            MonoText(
                                p.ip + (if (p.group.isNotBlank()) " · ${p.group}" else "") +
                                    (if (p.status == PanelStatus.ONLINE) " · online" else ""),
                                size = 10,
                            )
                        }
                        Box(
                            Modifier.size(9.dp).clip(CircleShape).background(
                                if (p.status == PanelStatus.ONLINE) Accent.Green else Accent.Gray,
                            ),
                        )
                    }
                }
            }

            // IPs digitados na mão (ainda não descobertos na rede) aparecem aqui
            // para o usuário ver o que está marcado e poder desmarcar.
            val avulsos = selecionados - paineis.map { it.ip }.toSet()
            if (avulsos.isNotEmpty()) {
                SectionLabel("IP digitado")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    avulsos.forEach { ip ->
                        FilterChip(
                            selected = true,
                            onClick = { onSelecionados(selecionados - ip) },
                            label = { Text(ip) },
                            leadingIcon = { Icon(Icons.Filled.CheckCircle, null, Modifier.size(16.dp)) },
                        )
                    }
                }
            }

            if (usbConectado) {
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (usarUsb) cs.secondaryContainer else cs.surfaceContainerHigh)
                        .clickable { onUsarUsb(!usarUsb) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Filled.Usb, null, tint = if (usarUsb) Accent.Blue else cs.outline)
                    Text("Painel conectado por cabo USB", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    if (usarUsb) Icon(Icons.Filled.CheckCircle, null, tint = Accent.Green)
                }
            }

            // Escape hatch: painel em outra sub-rede, ou que ainda não respondeu à
            // varredura. Sem isto, um painel "invisível" ficaria inalcançável.
            var manual by remember { mutableStateOf(false) }
            var ipManual by remember { mutableStateOf("") }
            if (!manual) {
                Text(
                    "Digitar o IP na mão",
                    Modifier.clickable { manual = true }.padding(vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = cs.primary,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        ipManual, { ipManual = it },
                        label = { Text("IP do painel") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    AccentButton(
                        onClick = {
                            val ip = ipManual.trim()
                            if (ip.isNotBlank()) { onSelecionados(selecionados + ip); ipManual = ""; manual = false }
                        },
                        enabled = ipManual.isNotBlank(),
                    ) { Text("Usar") }
                }
            }

            HorizontalDivider(color = cs.outlineVariant)

            // Senha de transmissão: ajuste no momento em que importa.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Enviar com senha", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Só se o painel tiver senha gravada.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }
                Switch(usarSenha, onUsarSenha)
            }
            androidx.compose.animation.AnimatedVisibility(usarSenha) {
                OutlinedTextField(
                    senha, onSenha,
                    label = { Text("Senha (numérica)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.size(2.dp))
            AccentButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Tv, null, Modifier.size(18.dp)); Spacer(Modifier.size(8.dp))
                Text(
                    when {
                        usarUsb -> "Usar o painel por USB"
                        selecionados.size > 1 -> "Publicar em ${selecionados.size} painéis"
                        selecionados.size == 1 -> "Pronto"
                        else -> "Fechar"
                    },
                )
            }
        }
    }
}
