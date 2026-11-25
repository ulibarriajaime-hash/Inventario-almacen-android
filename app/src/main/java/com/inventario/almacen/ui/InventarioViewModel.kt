package com.inventario.almacen.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inventario.almacen.data.AppDatabase
import com.inventario.almacen.data.Producto
import com.inventario.almacen.data.TipoMovimiento
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InventarioViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).dao()
    private val compartir = SharingStarted.WhileSubscribed(5_000)

    val busqueda = MutableStateFlow("")
    val soloBajos = MutableStateFlow(false)

    private val todos = dao.productos()
        .stateIn(viewModelScope, compartir, emptyList())

    val productos = combine(todos, busqueda, soloBajos) { lista, q, bajos ->
        lista.filter { p ->
            (!bajos || p.esStockBajo()) &&
                (q.isBlank() ||
                    p.nombre.contains(q, ignoreCase = true) ||
                    p.codigo.contains(q, ignoreCase = true) ||
                    p.ubicacion.contains(q, ignoreCase = true))
        }
    }.stateIn(viewModelScope, compartir, emptyList())

    val cantidadBajos = todos
        .map { lista -> lista.count { it.esStockBajo() } }
        .stateIn(viewModelScope, compartir, 0)

    val historial = dao.historial()
        .stateIn(viewModelScope, compartir, emptyList())

    private val _mensajes = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val mensajes = _mensajes.asSharedFlow()

    fun guardarProducto(producto: Producto) {
        viewModelScope.launch {
            if (producto.id == 0L) {
                dao.crearProducto(producto)
                _mensajes.tryEmit("Producto agregado")
            } else {
                dao.actualizar(producto)
                _mensajes.tryEmit("Producto actualizado")
            }
        }
    }

    fun eliminarProducto(producto: Producto) {
        viewModelScope.launch {
            dao.eliminar(producto)
            _mensajes.tryEmit("Producto eliminado")
        }
    }

    fun registrarMovimiento(producto: Producto, tipo: String, cantidad: Double, nota: String) {
        viewModelScope.launch {
            val ok = dao.registrar(producto.id, tipo, cantidad, nota)
            _mensajes.tryEmit(
                when {
                    !ok -> "No hay suficiente stock para esa salida"
                    tipo == TipoMovimiento.ENTRADA -> "Entrada registrada"
                    else -> "Salida registrada"
                }
            )
        }
    }
}
