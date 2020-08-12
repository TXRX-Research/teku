package tech.pegasys.teku.phase1.onotole.ssz

import tech.pegasys.teku.phase1.integration.UInt8View
import tech.pegasys.teku.phase1.integration.ssz.SSZBitlistImpl
import tech.pegasys.teku.phase1.integration.ssz.SSZBitvectorImpl
import tech.pegasys.teku.phase1.integration.ssz.SSZByteListImpl
import tech.pegasys.teku.phase1.integration.ssz.SSZListImpl
import tech.pegasys.teku.phase1.integration.ssz.SSZVectorImpl
import tech.pegasys.teku.phase1.integration.toUInt64
import tech.pegasys.teku.phase1.onotole.phase1.ENDIANNESS
import tech.pegasys.teku.phase1.onotole.phase1.Root
import tech.pegasys.teku.phase1.onotole.pylib.pybytes
import tech.pegasys.teku.phase1.onotole.pylib.pyint
import tech.pegasys.teku.phase1.onotole.pylib.to_bytes
import tech.pegasys.teku.ssz.SSZTypes.Bitvector
import tech.pegasys.teku.ssz.backing.ContainerViewRead
import tech.pegasys.teku.ssz.backing.ViewRead
import tech.pegasys.teku.ssz.backing.tree.TreeNode.BranchNode
import tech.pegasys.teku.ssz.backing.tree.TreeNode.LeafNode
import tech.pegasys.teku.ssz.backing.type.ViewType
import tech.pegasys.teku.ssz.backing.view.BasicViews
import org.apache.tuweni.bytes.Bytes as TuweniBytes
import org.apache.tuweni.bytes.Bytes32 as TuweniBytes32
import org.apache.tuweni.bytes.Bytes48 as TuweniBytes48
import tech.pegasys.teku.ssz.SSZTypes.Bytes4 as TekuBytes4

typealias SSZObject = Any

typealias bit = Boolean
typealias boolean = Boolean
typealias uint8 = UByte
typealias uint32 = UInt
typealias uint64 = ULong

typealias Bytes = TuweniBytes
typealias Bytes1 = Byte
typealias Bytes4 = TekuBytes4
typealias Bytes32 = TuweniBytes32
typealias Bytes48 = TuweniBytes48

data class Bytes96(val wrappedBytes: Bytes) {
  override fun toString(): String {
    return wrappedBytes.toString()
  }

  companion object {
    fun leftPad(bytes: Bytes): Bytes96 {
      require(bytes.size() <= 96)
      if (bytes.size() == 96) {
        return Bytes96(bytes)
      }
      val zeros = Bytes.wrap(ByteArray(96 - bytes.size()))
      return Bytes96(Bytes.concatenate(zeros, bytes))
    }
  }
}

typealias SSZDict<K, V> = MutableMap<K, V>

typealias Sequence<T> = List<T>

fun uint8(v: pyint): uint8 = v.value.toByte().toUByte()
fun uint8(v: uint64): uint8 = v.toUByte()
fun uint32(v: pyint): uint32 = v.value.toInt().toUInt()
fun uint32(v: uint64): uint32 = v.toUInt()
fun uint64(v: pyint): uint64 = v.value.toLong().toULong()
fun uint64(v: ULong): uint64 = v
fun Bytes4(): Bytes4 = Bytes4.fromHexString("0x00000000")
fun Bytes32(): Bytes32 = Bytes32.ZERO
fun Bytes32(x: List<Byte>): Bytes32 = Bytes32.wrap(x.toByteArray())
fun Bytes48(): Bytes48 = Bytes48.ZERO
fun Bytes96(): Bytes96 = Bytes96(TuweniBytes.concatenate(Bytes48.ZERO, Bytes48.ZERO))

fun <K, V> SSZDict() = mutableMapOf<K, V>()

interface SSZCollection<T : Any> : Sequence<T> {
  operator fun get(index: ULong): T
  override operator fun get(index: Int): T = get(index.toULong())
  fun hashTreeRoot(): Root
}

interface SSZMutableCollection<T : Any> : SSZCollection<T> {
  operator fun set(index: ULong, item: T): T
  operator fun set(index: Int, item: T): T = set(index.toULong(), item)
}

interface SSZList<T : Any> : SSZCollection<T> {
  val maxSize: ULong
  fun updated(mutator: (SSZMutableList<T>) -> Unit): SSZList<T>
}

interface SSZMutableList<T : Any> : SSZList<T>, SSZMutableCollection<T> {
  fun append(item: T)
  fun clear()
  fun replaceAll(elements: Sequence<T>)
}

interface SSZBitlist : SSZList<Boolean> {
  infix fun or(other: SSZBitlist): SSZBitlist
  fun set(index: uint64): SSZBitlist
  fun bitsSet(): List<uint64>
}

interface SSZByteList : SSZList<Byte> {
  fun toBytes(): Bytes
}

interface SSZVector<T : Any> : SSZCollection<T> {
  fun updated(mutator: (SSZMutableVector<T>) -> Unit): SSZVector<T>
}

interface SSZByteVector : SSZVector<Byte>
interface SSZMutableVector<T : Any> : SSZVector<T>, SSZMutableCollection<T>
interface SSZBitvector : SSZVector<Boolean>
interface SSZMutableBitvector : SSZMutableVector<Boolean>

fun SSZByteList.toPyBytes(): pybytes = Bytes.wrap(this.toByteArray())

fun uint_to_bytes(b: uint8): pybytes = int_to_bytes(b.toULong(), pyint(1uL))
fun uint_to_bytes(b: uint32): pybytes = int_to_bytes(b.toULong(), pyint(4uL))
fun uint_to_bytes(b: uint64): pybytes = int_to_bytes(b, pyint(8uL))
fun int_to_bytes(n: uint64, length: pyint): pybytes {
  return n.to_bytes(length, ENDIANNESS)
}

interface TreeNode {
  fun get_left(): TreeNode
  fun get_right(): TreeNode
  fun merkle_root(): Bytes32
}

class LeafNodeDelegate(private val delegate: LeafNode) : TreeNode {
  override fun get_left(): TreeNode = throw UnsupportedOperationException()
  override fun get_right(): TreeNode = throw UnsupportedOperationException()
  override fun merkle_root(): Bytes32 = delegate.root
}

class BranchNodeDelegate(private val delegate: BranchNode) : TreeNode {
  override fun get_left(): TreeNode = buildNodeDelegate(delegate.left())
  override fun get_right(): TreeNode = buildNodeDelegate(delegate.right())
  override fun merkle_root(): Bytes32 = delegate.hashTreeRoot()
}

private fun buildNodeDelegate(node: tech.pegasys.teku.ssz.backing.tree.TreeNode): TreeNode {
  return if (node is LeafNode) LeafNodeDelegate(node) else BranchNodeDelegate(node as BranchNode)
}

fun SSZByteList.get_backing(): TreeNode = buildNodeDelegate((this as SSZByteListImpl).backingNode)

fun <T : Any> SSZVector(
  elementType: ViewType,
  size: ULong,
  unwrapper: (ViewRead) -> T = getUnwrapper(elementType.default)
): SSZVector<T> = SSZVectorImpl(elementType, size.toLong(), unwrapper)

fun <T : Any> SSZVector(
  elementType: ViewType,
  elements: List<T>,
  unwrapper: (ViewRead) -> T = getUnwrapper(elementType.default),
  wrapper: (T) -> ViewRead = getWrapper(elementType.default)
): SSZVector<T> = SSZVectorImpl(elementType, elements, unwrapper, wrapper)

fun <T : Any> SSZList(
  elementType: ViewType,
  maxSize: ULong,
  elements: List<T> = listOf(),
  unwrapper: (ViewRead) -> T = getUnwrapper(elementType.default),
  wrapper: (T) -> ViewRead = getWrapper(elementType.default)
): SSZList<T> =
  SSZListImpl(
    elementType,
    maxSize,
    elements,
    unwrapper,
    wrapper
  )

fun SSZByteList(maxSize: ULong, elements: List<Byte> = listOf()): SSZByteList =
  SSZByteListImpl(maxSize, elements)

fun SSZBitlist(maxSize: ULong, elements: List<Boolean> = listOf()): SSZBitlist =
  SSZBitlistImpl(maxSize, elements)

fun SSZBitvector(bitvector: Bitvector): SSZBitvector {
  return SSZBitvectorImpl((0 until bitvector.size).map { bitvector.getBit(it) }.toList())
}

@Suppress("UNCHECKED_CAST")
fun <U : Any, V : ViewRead> getWrapper(view: ViewRead): (U) -> V {
  return when (view) {
    is ContainerViewRead -> { e -> e as V }
    is BasicViews.BitView -> { e -> BasicViews.BitView(e as boolean) as V }
    is BasicViews.ByteView -> { e -> BasicViews.ByteView(e as Byte) as V }
    is UInt8View -> { e -> UInt8View(e as uint8) as V }
    is BasicViews.UInt64View -> { e -> BasicViews.UInt64View((e as uint64).toUInt64()) as V }
    is BasicViews.Bytes4View -> { e -> BasicViews.Bytes4View(e as Bytes4) as V }
    is BasicViews.Bytes32View -> { e -> BasicViews.Bytes32View(e as Bytes32) as V }
    else -> throw IllegalArgumentException("Unsupported type ${view::class.qualifiedName}")
  }
}

@Suppress("UNCHECKED_CAST")
fun <U : Any, V : ViewRead> getUnwrapper(view: ViewRead): (V) -> U {
  return when (view) {
    is ContainerViewRead -> { v -> v as U }
    is BasicViews.BitView -> { v -> (v as BasicViews.BitView).get() as U }
    is BasicViews.ByteView -> { v -> (v as BasicViews.ByteView).get() as U }
    is UInt8View -> { v -> (v as UInt8View).get() as U }
    is BasicViews.UInt64View -> { v -> (v as BasicViews.UInt64View).get() as U }
    is BasicViews.Bytes4View -> { v -> (v as BasicViews.Bytes4View).get() as U }
    is BasicViews.Bytes32View -> { v -> (v as BasicViews.Bytes32View).get() as U }
    else -> throw IllegalArgumentException("Unsupported type ${view::class.qualifiedName}")
  }
}
