package com.inventario.almacen.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "productos")
data class Producto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val codigo: String = "",
    val unidad: String = "pza",
    val stock: Double = 0.0,
    val stockMinimo: Double = 0.0,
    val ubicacion: String = ""
) {
    /** Hay alerta cuando se definió un mínimo y el stock llegó o bajó de él. */
    fun esStockBajo(): Boolean = stockMinimo > 0 && stock <= stockMinimo
}

@Entity(
    tableName = "movimientos",
    foreignKeys = [
        ForeignKey(
            entity = Producto::class,
            parentColumns = ["id"],
            childColumns = ["productoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("productoId")]
)
data class Movimiento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productoId: Long,
    val tipo: String, // "ENTRADA" o "SALIDA"
    val cantidad: Double,
    val nota: String = "",
    val fecha: Long = System.currentTimeMillis()
)

data class MovimientoConProducto(
    @Embedded val movimiento: Movimiento,
    val productoNombre: String,
    val unidad: String
)

object TipoMovimiento {
    const val ENTRADA = "ENTRADA"
    const val SALIDA = "SALIDA"
}
