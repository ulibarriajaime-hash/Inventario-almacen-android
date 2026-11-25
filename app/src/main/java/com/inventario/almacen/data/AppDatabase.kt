package com.inventario.almacen.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class InventarioDao {

    @Query("SELECT * FROM productos ORDER BY nombre COLLATE NOCASE")
    abstract fun productos(): Flow<List<Producto>>

    @Query("SELECT * FROM productos WHERE id = :id")
    abstract suspend fun obtener(id: Long): Producto?

    @Insert
    abstract suspend fun insertar(producto: Producto): Long

    @Update
    abstract suspend fun actualizar(producto: Producto)

    @Delete
    abstract suspend fun eliminar(producto: Producto)

    @Insert
    abstract suspend fun insertarMovimiento(movimiento: Movimiento)

    @Query("UPDATE productos SET stock = stock + :delta WHERE id = :id")
    abstract suspend fun ajustarStock(id: Long, delta: Double)

    @Query(
        """
        SELECT m.*, p.nombre AS productoNombre, p.unidad AS unidad
        FROM movimientos m
        JOIN productos p ON p.id = m.productoId
        ORDER BY m.fecha DESC
        LIMIT 300
        """
    )
    abstract fun historial(): Flow<List<MovimientoConProducto>>

    /** Crea el producto y, si trae existencias, registra la entrada inicial. */
    @Transaction
    open suspend fun crearProducto(producto: Producto) {
        val id = insertar(producto)
        if (producto.stock > 0) {
            insertarMovimiento(
                Movimiento(
                    productoId = id,
                    tipo = TipoMovimiento.ENTRADA,
                    cantidad = producto.stock,
                    nota = "Stock inicial"
                )
            )
        }
    }

    /**
     * Registra una entrada o salida y actualiza el stock de forma atómica.
     * Devuelve false si una salida dejaría el stock en negativo.
     */
    @Transaction
    open suspend fun registrar(
        productoId: Long,
        tipo: String,
        cantidad: Double,
        nota: String
    ): Boolean {
        val producto = obtener(productoId) ?: return false
        val delta = if (tipo == TipoMovimiento.ENTRADA) cantidad else -cantidad
        if (producto.stock + delta < 0) return false
        ajustarStock(productoId, delta)
        insertarMovimiento(
            Movimiento(productoId = productoId, tipo = tipo, cantidad = cantidad, nota = nota)
        )
        return true
    }
}

@Database(entities = [Producto::class, Movimiento::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): InventarioDao

    companion object {
        @Volatile private var instancia: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "inventario.db"
                ).build().also { instancia = it }
            }
    }
}
