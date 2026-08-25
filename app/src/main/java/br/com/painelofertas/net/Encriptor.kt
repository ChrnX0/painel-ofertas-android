package br.com.painelofertas.net

/**
 * Gera os 10 bytes de código do comando `APAGAR=` — porte de `Encriptor`
 * (Ofertas.pas:914-1063). É uma ofuscação fraca (anti-replay) baseada no
 * horário + na senha de transmissão, não criptografia real.
 *
 * IMPORTANTE: sem senha de transmissão (caso padrão), o código é **10 zeros**
 * (Ofertas.pas:923-935) — verificado no fonte. O caminho com senha é fiel ao
 * original, mas os bytes de "tempo" são apenas ruído (o painel valida a senha
 * usando a chave enviada em codigo[9]); precisa de validação em campo se você
 * usar senha de transmissão.
 */
object Encriptor {

    fun code(password: String?, nowDigits: String = ""): IntArray {
        val codigo = IntArray(10)
        if (password.isNullOrEmpty()) return codigo // sem senha -> 10 zeros

        val pass = password.toLongOrNull() ?: return codigo
        val digits = nowDigits.filter { it.isDigit() }.padEnd(11, '0').take(11)
        val contador = digits.toLongOrNull() ?: 0L

        // decomposição little-endian (repetidas divisões por 2 no original)
        val bytesContador = IntArray(5) { ((contador shr (it * 8)) and 0xFF).toInt() }
        val bytesSenha = IntArray(4) { ((pass shr (it * 8)) and 0xFF).toInt() }
        val chave = (digits.takeLast(3).toIntOrNull() ?: 0) and 0xFF

        codigo[0] = bytesContador[1]; codigo[1] = bytesSenha[3]; codigo[2] = bytesContador[3]
        codigo[3] = bytesContador[4]; codigo[4] = bytesSenha[1]; codigo[5] = bytesSenha[0]
        codigo[6] = bytesContador[0]; codigo[7] = bytesSenha[2]; codigo[8] = bytesContador[2]
        codigo[9] = 0

        var limitador = 0
        for (i in 0..7) if (chave and (1 shl i) != 0) limitador++

        repeat(limitador) {
            for (i in 8 downTo 0) {
                for (mask in intArrayOf(32, 16, 8, 4)) {
                    codigo[i + 1] =
                        if (codigo[i] and mask != 0) codigo[i + 1] or mask
                        else codigo[i + 1] and (mask.inv() and 0xFF)
                }
            }
            for (mask in intArrayOf(32, 16, 8, 4)) {
                codigo[0] =
                    if (codigo[9] and mask != 0) codigo[0] or mask
                    else codigo[0] and (mask.inv() and 0xFF)
            }
        }

        for (i in 0..8) codigo[i] = codigo[i] xor chave
        codigo[9] = chave
        codigo[9] = if (codigo[9] and 64 != 0) codigo[9] and 191 else codigo[9] or 64
        return codigo
    }
}
