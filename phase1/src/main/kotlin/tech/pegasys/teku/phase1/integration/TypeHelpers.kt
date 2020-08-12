package tech.pegasys.teku.phase1.integration

import com.google.common.primitives.UnsignedLong
import org.apache.tuweni.bytes.Bytes48
import tech.pegasys.teku.phase1.integration.ssz.SSZAbstractCollection
import tech.pegasys.teku.phase1.onotole.phase1.BLSPubkey
import tech.pegasys.teku.phase1.onotole.phase1.BLSSignature
import tech.pegasys.teku.phase1.onotole.ssz.Bytes
import tech.pegasys.teku.phase1.onotole.ssz.Bytes32
import tech.pegasys.teku.phase1.onotole.ssz.Bytes4
import tech.pegasys.teku.phase1.onotole.ssz.Bytes96
import tech.pegasys.teku.phase1.onotole.ssz.boolean
import tech.pegasys.teku.phase1.onotole.ssz.uint64
import tech.pegasys.teku.phase1.onotole.ssz.uint8
import tech.pegasys.teku.ssz.backing.BasicView
import tech.pegasys.teku.ssz.backing.VectorViewRead
import tech.pegasys.teku.ssz.backing.ViewRead
import tech.pegasys.teku.ssz.backing.ViewWrite
import tech.pegasys.teku.ssz.backing.tree.TreeNode
import tech.pegasys.teku.ssz.backing.type.BasicViewTypes
import tech.pegasys.teku.ssz.backing.type.VectorViewType
import tech.pegasys.teku.ssz.backing.type.ViewType
import tech.pegasys.teku.ssz.backing.view.BasicViews
import tech.pegasys.teku.ssz.backing.view.BasicViews.ByteView
import tech.pegasys.teku.ssz.backing.view.ViewUtils
import tech.pegasys.teku.infrastructure.unsigned.UInt64 as TekuUInt64

val Bytes48Type = VectorViewType<ByteView>(BasicViewTypes.BYTE_TYPE, 48)
val Bytes96Type = VectorViewType<ByteView>(BasicViewTypes.BYTE_TYPE, 96)

fun TekuUInt64.toUInt64() = this.longValue().toULong()
fun uint64.toUnsignedLong() = UnsignedLong.valueOf(this.toString())
fun uint64.toUInt64() = TekuUInt64.fromLongBits(this.toLong())

fun <T : Any> wrapValues(vararg values: T): Array<ViewRead> {
  return values.map { wrapValue(it) }.toTypedArray()
}

fun wrapValue(value: Any): ViewRead {
  return when (value) {
    is SSZAbstractCollection<*, *> -> value.view
    is ViewRead -> value
    else -> wrapBasicValue(value)
  }
}

class UInt8View(private val delegate: ByteView) : BasicView<uint8> {
  constructor(value: uint8) : this(ByteView(value.toByte()))

  override fun createWritableCopy(): ViewWrite = delegate.createWritableCopy()
  override fun getType(): ViewType = UInt8Type
  override fun get(): uint8 = delegate.get().toUByte()
  override fun getBackingNode(): TreeNode = delegate.backingNode
}

val UInt8Type = object : ViewType {
  override fun createFromBackingNode(node: TreeNode?, internalIndex: Int): UInt8View {
    return UInt8View(BasicViewTypes.BYTE_TYPE.createFromBackingNode(node, internalIndex))
  }

  override fun getDefaultTree(): TreeNode = BasicViewTypes.BYTE_TYPE.defaultTree
  override fun getBitsSize(): Int = BasicViewTypes.BYTE_TYPE.bitsSize
  override fun updateBackingNode(
    srcNode: TreeNode?,
    internalIndex: Int,
    newValue: ViewRead?
  ): TreeNode {
    val bytes = srcNode!!.hashTreeRoot().toArray()
    bytes[internalIndex] = (newValue as UInt8View).get().toByte()
    return TreeNode.createLeafNode(Bytes32.wrap(bytes))
  }

  override fun createFromBackingNode(node: TreeNode?): ViewRead =
    UInt8View(BasicViewTypes.BYTE_TYPE.createFromBackingNode(node))
}

inline fun <reified T> wrapBasicValue(value: Any): T {
  return when (value) {
    is uint8 -> UInt8View(value)
    is uint64 -> BasicViews.UInt64View(value.toUInt64())
    is boolean -> BasicViews.BitView(value)
    is BLSSignature -> ViewUtils.createVectorFromBytes(value.wrappedBytes)
    is BLSPubkey -> ViewUtils.createVectorFromBytes(value)
    is Bytes32 -> BasicViews.Bytes32View(value)
    is Bytes4 -> BasicViews.Bytes4View(value)
    is Bytes -> ViewUtils.createVectorFromBytes(value)
    is Byte -> ByteView(value.toByte())
    else -> throw IllegalArgumentException("Unsupported type ${value::class.qualifiedName}")
  } as T
}

@Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
inline fun <reified T> getBasicValue(view: ViewRead): T {
  return when (T::class) {
    uint8::class -> (view as UInt8View).get()
    uint64::class -> (view as BasicViews.UInt64View).get().toUInt64()
    boolean::class -> (view as BasicViews.BitView).get()
    BLSSignature::class -> Bytes96(ViewUtils.getAllBytes(view as VectorViewRead<BasicViews.ByteView>))
    BLSPubkey::class -> Bytes48.wrap(ViewUtils.getAllBytes(view as VectorViewRead<BasicViews.ByteView>))
    Bytes32::class -> (view as BasicViews.Bytes32View).get()
    Bytes4::class -> (view as BasicViews.Bytes4View).get()
    Bytes::class -> ViewUtils.getAllBytes(view as VectorViewRead<BasicViews.ByteView>)
    Byte::class -> (view as BasicViews.ByteView).get()
    else -> throw IllegalArgumentException("Unsupported type ${T::class.qualifiedName}")
  } as T
}
