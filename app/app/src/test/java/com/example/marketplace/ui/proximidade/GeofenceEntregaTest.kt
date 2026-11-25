package com.example.marketplace.ui.proximidade

import com.example.marketplace.data.entity.PedidoEntity
import org.junit.Assert.*
import org.junit.Test

class GeofenceEntregaTest {
    private val pedido = PedidoEntity(id = 3, emailCliente = "cliente@teste.invalid", enderecoEntrega = "Rua A",
        resumoItens = "Produto", valorTotal = "10", emailEntregador = "entregador@teste.invalid",
        coletado = true, statusEntrega = "EM_ROTA", entregaLatitude = -21.784, entregaLongitude = -48.2107)

    @Test fun monitoraEmRotaComCoordenadasValidasESemAvisoMesmoSemColeta() {
        assertTrue(destinoValido(pedido))
        assertTrue(destinoValido(pedido.copy(coletado = false)))
        assertFalse(destinoValido(pedido.copy(statusEntrega = "ENTREGUE")))
        assertFalse(destinoValido(pedido.copy(avisoProximidade = true)))
        assertFalse(destinoValido(pedido.copy(entregaLatitude = null)))
        assertFalse(destinoValido(pedido.copy(entregaLatitude = Double.NaN)))
        assertFalse(destinoValido(pedido.copy(entregaLongitude = 181.0)))
    }

    @Test fun rejeitaEventoDeRegistroAnteriorOuIdentificadorInvalido() {
        assertEquals(3, idDoEvento("atual:3", "atual"))
        assertNull(idDoEvento("antigo:3", "atual"))
        assertNull(idDoEvento("atual:abc", "atual"))
        assertNull(idDoEvento("atual:0", "atual"))
        assertNull(idDoEvento("atual:3:4", "atual"))
        assertNull(idDoEvento(":3", ""))
    }
}
