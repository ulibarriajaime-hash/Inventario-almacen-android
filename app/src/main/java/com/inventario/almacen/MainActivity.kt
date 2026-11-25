package com.inventario.almacen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inventario.almacen.data.MovimientoConProducto
import com.inventario.almacen.data.Producto
import com.inventario.almacen.data.TipoMovimiento
import com.inventario.almacen.ui.InventarioViewModel
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            ) {
                AppInventario()
            }
        }
    }
}

/** Formatea 5.0 como "5" y 2.5 como "2.5". */
private fun Double.fmt(): String =
    if (this % 1.0 == 0.0) this.toLong().toString() else "%.2f".format(this).trimEnd('0').trimEnd('.')

private fun String.aNumero(): Double? = replace(',', '.').trim().toDoubleOrNull()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppInventario(vm: InventarioViewModel = viewModel()) {
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }

    var editando by remember { mutableStateOf<Producto?>(null) }
    var creando by remember { mutableStateOf(false) }
    var movimiento by remember { mutableStateOf<Pair<Producto, String>?>(null) }
    var porEliminar by remember { mutableStateOf<Producto?>(null) }

    LaunchedEffect(Unit) {
        vm.mensajes.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (pestana == 0) "Inventario" else "Historial") })
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = pestana == 0,
                    onClick = { pestana = 0 },
                    icon = { Icon(Icons.Filled.Inventory2, contentDescription = null) },
                    label = { Text("Productos") }
                )
                NavigationBarItem(
                    selected = pestana == 1,
                    onClick = { pestana = 1 },
                    icon = { Icon(Icons.Filled.History, contentDescription = null) },
                    label = { Text("Historial") }
                )
            }
        },
        floatingActionButton = {
            if (pestana == 0) {
                FloatingActionButton(onClick = { creando = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Agregar producto")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (pestana == 0) {
                PantallaProductos(
                    vm = vm,
                    onEditar = { editando = it },
                    onEliminar = { porEliminar = it },
                    onMovimiento = { p, tipo -> movimiento = p to tipo }
                )
            } else {
                PantallaHistorial(vm)
            }
        }
    }

    if (creando) {
        DialogoProducto(
            inicial = null,
            onCancelar = { creando = false },
            onGuardar = { vm.guardarProducto(it); creando = false }
        )
    }
    editando?.let { p ->
        DialogoProducto(
            inicial = p,
            onCancelar = { editando = null },
            onGuardar = { vm.guardarProducto(it); editando = null }
        )
    }
    movimiento?.let { (p, tipo) ->
        DialogoMovimiento(
            producto = p,
            tipo = tipo,
            onCancelar = { movimiento = null },
            onGuardar = { cantidad, nota ->
                vm.registrarMovimiento(p, tipo, cantidad, nota)
                movimiento = null
            }
        )
    }
    porEliminar?.let { p ->
        AlertDialog(
            onDismissRequest = { porEliminar = null },
            title = { Text("Eliminar producto") },
            text = { Text("¿Eliminar \"${p.nombre}\" y todo su historial? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { vm.eliminarProducto(p); porEliminar = null }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { porEliminar = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun PantallaProductos(
    vm: InventarioViewModel,
    onEditar: (Producto) -> Unit,
    onEliminar: (Producto) -> Unit,
    onMovimiento: (Producto, String) -> Unit
) {
    val productos by vm.productos.collectAsState()
    val busqueda by vm.busqueda.collectAsState()
    val soloBajos by vm.soloBajos.collectAsState()
    val bajos by vm.cantidadBajos.collectAsState()

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = busqueda,
            onValueChange = { vm.busqueda.value = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Buscar por nombre, código o ubicación") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true
        )
        FilterChip(
            selected = soloBajos,
            onClick = { vm.soloBajos.value = !soloBajos },
            label = { Text("Stock bajo ($bajos)") },
            leadingIcon = { Icon(Icons.Filled.Warning, contentDescription = null) },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (productos.isEmpty()) {
            Text(
                text = if (busqueda.isBlank() && !soloBajos)
                    "Aún no hay productos. Toca + para agregar el primero."
                else "Sin resultados.",
                modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(productos, key = { it.id }) { p ->
                    TarjetaProducto(p, onEditar, onEliminar, onMovimiento)
                }
            }
        }
    }
}

@Composable
private fun TarjetaProducto(
    p: Producto,
    onEditar: (Producto) -> Unit,
    onEliminar: (Producto) -> Unit,
    onMovimiento: (Producto, String) -> Unit
) {
    val bajo = p.esStockBajo()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (bajo) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(p.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    val detalle = listOf(p.codigo, p.ubicacion).filter { it.isNotBlank() }.joinToString(" · ")
                    if (detalle.isNotEmpty()) {
                        Text(detalle, style = MaterialTheme.typography.bodySmall)
                    }
                }
                IconButton(onClick = { onEditar(p) }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar")
                }
                IconButton(onClick = { onEliminar(p) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
                }
            }
            Text(
                "Stock: ${p.stock.fmt()} ${p.unidad}",
                style = MaterialTheme.typography.headlineSmall
            )
            if (p.stockMinimo > 0) {
                Text(
                    (if (bajo) "⚠ Stock bajo — " else "") + "mínimo ${p.stockMinimo.fmt()} ${p.unidad}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onMovimiento(p, TipoMovimiento.ENTRADA) }) { Text("Entrada") }
                OutlinedButton(onClick = { onMovimiento(p, TipoMovimiento.SALIDA) }) { Text("Salida") }
            }
        }
    }
}

@Composable
private fun PantallaHistorial(vm: InventarioViewModel) {
    val historial by vm.historial.collectAsState()
    if (historial.isEmpty()) {
        Text("Todavía no hay movimientos.", modifier = Modifier.padding(24.dp))
        return
    }
    val formato = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(historial, key = { it.movimiento.id }) { h -> FilaHistorial(h, formato) }
    }
}

@Composable
private fun FilaHistorial(h: MovimientoConProducto, formato: DateFormat) {
    val m = h.movimiento
    val entrada = m.tipo == TipoMovimiento.ENTRADA
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(h.productoNombre, fontWeight = FontWeight.Bold)
                Text(formato.format(Date(m.fecha)), style = MaterialTheme.typography.bodySmall)
                if (m.nota.isNotBlank()) Text(m.nota, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                (if (entrada) "+" else "−") + m.cantidad.fmt() + " " + h.unidad,
                style = MaterialTheme.typography.titleMedium,
                color = if (entrada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun DialogoProducto(
    inicial: Producto?,
    onCancelar: () -> Unit,
    onGuardar: (Producto) -> Unit
) {
    var nombre by remember { mutableStateOf(inicial?.nombre ?: "") }
    var codigo by remember { mutableStateOf(inicial?.codigo ?: "") }
    var unidad by remember { mutableStateOf(inicial?.unidad ?: "pza") }
    var ubicacion by remember { mutableStateOf(inicial?.ubicacion ?: "") }
    var minimo by remember { mutableStateOf(inicial?.stockMinimo?.takeIf { it > 0 }?.fmt() ?: "") }
    var stockInicial by remember { mutableStateOf("") }

    val minimoValido = minimo.isBlank() || (minimo.aNumero()?.let { it >= 0 } == true)
    val stockValido = stockInicial.isBlank() || (stockInicial.aNumero()?.let { it >= 0 } == true)
    val puedeGuardar = nombre.isNotBlank() && unidad.isNotBlank() && minimoValido && stockValido

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (inicial == null) "Nuevo producto" else "Editar producto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre *") }, singleLine = true)
                OutlinedTextField(codigo, { codigo = it }, label = { Text("Código / SKU") }, singleLine = true)
                OutlinedTextField(unidad, { unidad = it }, label = { Text("Unidad (pza, m, kg…) *") }, singleLine = true)
                OutlinedTextField(ubicacion, { ubicacion = it }, label = { Text("Ubicación (estante, bodega)") }, singleLine = true)
                OutlinedTextField(
                    minimo, { minimo = it },
                    label = { Text("Stock mínimo (alerta)") },
                    isError = !minimoValido,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                if (inicial == null) {
                    OutlinedTextField(
                        stockInicial, { stockInicial = it },
                        label = { Text("Stock inicial") },
                        isError = !stockValido,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = puedeGuardar,
                onClick = {
                    onGuardar(
                        (inicial ?: Producto(nombre = "")).copy(
                            nombre = nombre.trim(),
                            codigo = codigo.trim(),
                            unidad = unidad.trim(),
                            ubicacion = ubicacion.trim(),
                            stockMinimo = minimo.aNumero() ?: 0.0,
                            stock = if (inicial == null) stockInicial.aNumero() ?: 0.0 else inicial.stock
                        )
                    )
                }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

@Composable
private fun DialogoMovimiento(
    producto: Producto,
    tipo: String,
    onCancelar: () -> Unit,
    onGuardar: (Double, String) -> Unit
) {
    var cantidad by remember { mutableStateOf("") }
    var nota by remember { mutableStateOf("") }
    val valor = cantidad.aNumero()
    val entrada = tipo == TipoMovimiento.ENTRADA

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text((if (entrada) "Entrada: " else "Salida: ") + producto.nombre) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Stock actual: ${producto.stock.fmt()} ${producto.unidad}")
                OutlinedTextField(
                    cantidad, { cantidad = it },
                    label = { Text("Cantidad (${producto.unidad})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    nota, { nota = it },
                    label = { Text(if (entrada) "Nota (proveedor, factura…)" else "Nota (obra, persona…)") }
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valor != null && valor > 0,
                onClick = { onGuardar(valor!!, nota.trim()) }
            ) { Text("Registrar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}
