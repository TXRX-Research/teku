package tech.pegasys.teku.phase1.integration.ssz

import tech.pegasys.teku.phase1.onotole.phase1.Root
import tech.pegasys.teku.phase1.onotole.ssz.Bytes
import tech.pegasys.teku.phase1.onotole.ssz.SSZBitlist
import tech.pegasys.teku.phase1.onotole.ssz.SSZBitvector
import tech.pegasys.teku.phase1.onotole.ssz.SSZByteList
import tech.pegasys.teku.phase1.onotole.ssz.SSZByteVector
import tech.pegasys.teku.phase1.onotole.ssz.SSZCollection
import tech.pegasys.teku.phase1.onotole.ssz.SSZList
import tech.pegasys.teku.phase1.onotole.ssz.SSZMutableBitvector
import tech.pegasys.teku.phase1.onotole.ssz.SSZMutableList
import tech.pegasys.teku.phase1.onotole.ssz.SSZMutableVector
import tech.pegasys.teku.phase1.onotole.ssz.SSZVector
import tech.pegasys.teku.phase1.onotole.ssz.Sequence
import tech.pegasys.teku.phase1.onotole.ssz.getWrapper
import tech.pegasys.teku.phase1.onotole.ssz.uint64
import tech.pegasys.teku.ssz.backing.CompositeViewRead
import tech.pegasys.teku.ssz.backing.ListViewRead
import tech.pegasys.teku.ssz.backing.ListViewWrite
import tech.pegasys.teku.ssz.backing.VectorViewRead
import tech.pegasys.teku.ssz.backing.VectorViewWrite
import tech.pegasys.teku.ssz.backing.VectorViewWriteRef
import tech.pegasys.teku.ssz.backing.ViewRead
import tech.pegasys.teku.ssz.backing.tree.TreeNode
import tech.pegasys.teku.ssz.backing.type.BasicViewTypes
import tech.pegasys.teku.ssz.backing.type.ListViewType
import tech.pegasys.teku.ssz.backing.type.VectorViewType
import tech.pegasys.teku.ssz.backing.type.ViewType
import tech.pegasys.teku.ssz.backing.view.BasicViews.BitView
import tech.pegasys.teku.ssz.backing.view.BasicViews.ByteView
import tech.pegasys.teku.ssz.backing.view.ViewUtils

abstract class SSZAbstractCollection<U : Any, V : ViewRead> : SSZCollection<U> {
  protected abstract val unwrapper: (V) -> U
  internal abstract val view: CompositeViewRead<V>

  override val size: Int
    get() = view.size()

  override fun get(index: ULong): U = unwrapper(view.get(index.toInt()) as V)
  override fun contains(element: U): Boolean = indexOf(element) > 0
  override fun indexOf(element: U): Int {
    for ((index, item) in this.withIndex()) {
      if (item == element)
        return index
    }
    return -1
  }

  override fun isEmpty(): Boolean = size == 0
  override fun iterator(): Iterator<U> = object : Iterator<U> {
    private var it: Int = -1
    override fun hasNext(): Boolean = (it + 1) < size
    override fun next(): U {
      if (!hasNext()) throw NoSuchElementException()
      return get(++it)
    }
  }

  override fun subList(fromIndex: Int, toIndex: Int): List<U> {
    if (fromIndex < 0 || toIndex > size) {
      throw IndexOutOfBoundsException("fromIndex: $fromIndex, toIndex: $toIndex, size: $size")
    }
    if (fromIndex > toIndex) {
      throw IllegalArgumentException("fromIndex: $fromIndex > toIndex: $toIndex")
    }
    return (fromIndex until toIndex).map { get(it) }
  }

  override fun toString(): String {
    return "[${this.joinToString { it.toString() }}]"
  }


  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other is SSZListImpl<*, *>) {
      return view == other.view
    }

    if (other is Sequence<*>) {
      if (!this.isEmpty() && !other.isEmpty()) {
        for (i in 0 until this.size) {
          if (this[i] != other[i]) {
            return false
          }
        }
        return true
      } else if (this.isEmpty() && other.isEmpty()) {
        return true
      }

      return false
    }

    return false
  }

  override fun hashTreeRoot(): Root {
    return view.hashTreeRoot()
  }

  override fun lastIndexOf(element: U): Int = throw UnsupportedOperationException()
  override fun containsAll(elements: Collection<U>): Boolean = throw UnsupportedOperationException()
  override fun listIterator(): ListIterator<U> = throw UnsupportedOperationException()
  override fun listIterator(index: Int): ListIterator<U> = throw UnsupportedOperationException()
}

class SSZVectorImpl<U : Any, V : ViewRead>(
  override val view: VectorViewRead<V>,
  override val unwrapper: (V) -> U
) : SSZAbstractCollection<U, V>(), SSZVector<U> {
  constructor(
    elementType: ViewType,
    elements: List<U>,
    unwrapper: (V) -> U,
    wrapper: (U) -> V
  ) : this(
    initializeVectorView<U, V>(elementType, elements, wrapper).commitChanges(),
    unwrapper
  )

  constructor(elementType: ViewType, size: Long, unwrapper: (V) -> U) : this(
    VectorViewType<V>(elementType, size).default,
    unwrapper
  )

  override fun updated(mutator: (SSZMutableVector<U>) -> Unit): SSZVector<U> {
    val mutableCopy = SSZMutableVectorImpl<U, V>(
      view.createWritableCopy(),
      unwrapper,
      getWrapper(view.type.elementType.default)
    )
    mutator(mutableCopy)
    return SSZVectorImpl<U, V>(mutableCopy.view.commitChanges(), unwrapper)
  }
}

class SSZMutableVectorImpl<U : Any, V : ViewRead>(
  override val view: VectorViewWrite<V>,
  override val unwrapper: (V) -> U,
  private val wrapper: (U) -> V
) : SSZAbstractCollection<U, V>(), SSZMutableVector<U> {

  override fun set(index: ULong, item: U): U {
    val oldValue = get(index)
    view.set(index.toInt(), wrapper(item))
    return oldValue
  }

  constructor(
    elementType: ViewType,
    elements: List<U>,
    unwrapper: (V) -> U,
    wrapper: (U) -> V
  ) : this(initializeVectorView<U, V>(elementType, elements, wrapper), unwrapper, wrapper)

  constructor(elementType: ViewType, size: ULong, unwrapper: (V) -> U, wrapper: (U) -> V) : this(
    VectorViewType<V>(
      elementType,
      size.toLong()
    ).default.createWritableCopy() as VectorViewWriteRef<V, *>, unwrapper, wrapper
  )

  override fun updated(mutator: (SSZMutableVector<U>) -> Unit): SSZVector<U> =
    throw UnsupportedOperationException()
}

class SSZListImpl<U : Any, V : ViewRead>(
  override val view: ListViewRead<V>,
  override val unwrapper: (V) -> U
) : SSZAbstractCollection<U, V>(), SSZList<U> {

  override val maxSize: ULong
    get() = view.type.maxLength.toULong()

  constructor(
    elementType: ViewType,
    maxSize: ULong,
    elements: List<U>,
    unwrapper: (V) -> U,
    wrapper: (U) -> V
  ) : this(
    initializeListView<U, V>(elementType, maxSize, elements, wrapper).commitChanges(),
    unwrapper
  )

  override fun updated(mutator: (SSZMutableList<U>) -> Unit): SSZList<U> {
    val mutableCopy = SSZMutableListImpl<U, V>(
      view.createWritableCopy(),
      unwrapper,
      getWrapper(view.type.elementType.default)
    )
    mutator(mutableCopy)
    return SSZListImpl<U, V>(mutableCopy.view.commitChanges(), unwrapper)
  }
}

class SSZMutableListImpl<U : Any, V : ViewRead>(
  override val view: ListViewWrite<V>,
  override val unwrapper: (V) -> U,
  private val wrapper: (U) -> V
) : SSZAbstractCollection<U, V>(), SSZMutableList<U> {

  override val maxSize: ULong
    get() = view.type.maxLength.toULong()

  override fun set(index: ULong, item: U): U {
    val oldValue = get(index)
    view.set(index.toInt(), wrapper(item))
    return oldValue
  }

  override fun append(item: U) {
    if (size.toULong() >= maxSize) {
      throw IndexOutOfBoundsException()
    }
    view.append(wrapper(item))
  }

  override fun clear() = view.clear()

  constructor(
    elementType: ViewType,
    maxSize: ULong,
    elements: List<U>,
    unwrapper: (V) -> U,
    wrapper: (U) -> V
  ) : this(
    initializeListView<U, V>(elementType, maxSize, elements, wrapper),
    unwrapper,
    wrapper
  )

  override fun updated(mutator: (SSZMutableList<U>) -> Unit): SSZList<U> =
    throw UnsupportedOperationException()

  override fun replaceAll(elements: Sequence<U>) {
    this.clear()
    elements.forEach { this.append(it) }
  }
}

class SSZBitvectorImpl(override val view: VectorViewRead<BitView>) :
  SSZAbstractCollection<Boolean, BitView>(), SSZBitvector {
  override val unwrapper: (BitView) -> Boolean = BitView::get

  constructor(size: ULong) : this(
    VectorViewType<BitView>(
      BasicViewTypes.BIT_TYPE,
      size.toLong()
    ).default.createWritableCopy()
  )

  constructor(elements: List<Boolean>) : this(
    initializeVectorView<Boolean, BitView>(
      BasicViewTypes.BIT_TYPE,
      elements,
      ::BitView
    ).commitChanges()
  )

  override fun updated(mutator: (SSZMutableVector<Boolean>) -> Unit): SSZVector<Boolean> =
    throw UnsupportedOperationException()
}

class SSZMutableBitvectorImpl(override val view: VectorViewWrite<BitView>) :
  SSZAbstractCollection<Boolean, BitView>(), SSZMutableBitvector {
  override val unwrapper: (BitView) -> Boolean = BitView::get
  override fun updated(mutator: (SSZMutableVector<Boolean>) -> Unit): SSZVector<Boolean> =
    throw UnsupportedOperationException()

  override fun set(index: ULong, item: Boolean): Boolean {
    val oldValue = get(index)
    view.set(index.toInt(), BitView(item))
    return oldValue
  }

  constructor(size: ULong) : this(
    VectorViewType<BitView>(
      BasicViewTypes.BIT_TYPE,
      size.toLong()
    ).default.createWritableCopy()
  )

  constructor(elements: List<Boolean>) : this(
    initializeVectorView<Boolean, BitView>(BasicViewTypes.BIT_TYPE, elements, ::BitView)
  )
}

class SSZBitlistImpl(override val view: ListViewRead<BitView>) :
  SSZAbstractCollection<Boolean, BitView>(), SSZBitlist {
  override val unwrapper: (BitView) -> Boolean = BitView::get
  override fun or(other: SSZBitlist): SSZBitlist {
    assert((other as SSZBitlistImpl).view.type.maxLength == this.view.type.maxLength)

    val resultBits = (0 until view.size().coerceAtLeast(other.view.size())).map {
      val a = if (it < view.size()) view.get(it).get() else false
      val b = if (it < other.view.size()) other.view.get(it).get() else false
      a or b
    }
    return SSZBitlistImpl(view.type.maxLength.toULong(), resultBits)
  }

  override fun set(index: uint64): SSZBitlist {
    if (index > maxSize) {
      throw IndexOutOfBoundsException()
    }

    val mutableCopy = view.createWritableCopy()
    for (i in view.size()..index.toInt()) {
      mutableCopy.append(BitView(false))
    }
    mutableCopy.set(index.toInt(), BitView(true))
    return SSZBitlistImpl(mutableCopy.commitChanges())
  }

  override fun bitsSet(): List<uint64> {
    return (0uL until size.toULong()).filter { get(it) }.toList()
  }

  override val maxSize: ULong
    get() = view.type.maxLength.toULong()

  override fun updated(mutator: (SSZMutableList<Boolean>) -> Unit): SSZList<Boolean> =
    throw UnsupportedOperationException()

  constructor(maxSize: ULong, elements: List<Boolean> = listOf()) : this(
    initializeListView<Boolean, BitView>(
      BasicViewTypes.BIT_TYPE,
      maxSize,
      elements,
      ::BitView
    ).commitChanges()
  )
}

class SSZByteListImpl(override val view: ListViewRead<ByteView>) :
  SSZAbstractCollection<Byte, ByteView>(), SSZByteList {
  override val unwrapper: (ByteView) -> Byte = ByteView::get

  override val maxSize: ULong
    get() = view.type.maxLength.toULong()

  override fun updated(mutator: (SSZMutableList<Byte>) -> Unit): SSZList<Byte> =
    throw UnsupportedOperationException()

  val backingNode: TreeNode
    get() = view.backingNode

  constructor(maxSize: ULong, elements: List<Byte>) : this(
    initializeListView<Byte, ByteView>(
      BasicViewTypes.BYTE_TYPE,
      maxSize,
      elements,
      ::ByteView
    ).commitChanges()
  )

  constructor(maxSize: ULong, bytes: Bytes)
      : this(maxSize, (0 until bytes.size()).map { bytes[it] })

  override fun toBytes(): Bytes {
    val bytes = ByteArray(size)
    this.forEachIndexed { i, value -> bytes[i] = value }
    return Bytes.wrap(bytes)
  }
}

class SSZByteVectorImpl(override val view: VectorViewRead<ByteView>) :
  SSZAbstractCollection<Byte, ByteView>(), SSZByteVector {
  override val unwrapper: (ByteView) -> Byte = ByteView::get

  override fun toBytes(): Bytes {
    return ViewUtils.getAllBytes(view)
  }

  override fun updated(mutator: (SSZMutableVector<Byte>) -> Unit): SSZVector<Byte> =
    throw UnsupportedOperationException()

  override val size: Int
    get() = view.size()
  val backingNode: TreeNode
    get() = view.backingNode

  constructor(elements: List<Byte>) : this(
    initializeVectorView(BasicViewTypes.BYTES32_TYPE, elements, ::ByteView).commitChanges()
  )
}

private fun <U : Any, V : ViewRead> initializeVectorView(
  elementType: ViewType,
  elements: List<U>,
  wrapper: (U) -> V
): VectorViewWrite<V> {
  val type = VectorViewType<V>(elementType, elements.size.toLong())
  val view = type.default.createWritableCopy() as VectorViewWrite<V>
  elements.forEachIndexed { index, item -> view.set(index, wrapper(item)) }
  return view
}

private fun <U : Any, V : ViewRead> initializeListView(
  elementType: ViewType,
  maxSize: ULong,
  elements: List<U>,
  wrapper: (U) -> V
): ListViewWrite<V> {
  val type = ListViewType<V>(elementType, maxSize.toLong())
  val view = type.default.createWritableCopy() as ListViewWrite<V>
  elements.forEachIndexed { index, item -> view.set(index, wrapper(item)) }
  return view
}

internal fun <U : Any, V : ViewRead> getListView(
  elementType: ViewType,
  maxSize: ULong,
  elements: List<U>,
  wrapper: (U) -> V
): ListViewRead<V> {
  require(
    elements.size.toULong() < maxSize
  ) { "Number of elements given ${elements.size} is greater than the max size of the list $maxSize" }
  return initializeListView(elementType, maxSize, elements, wrapper).commitChanges()
}

internal fun <U : Any, V : ViewRead> getVectorView(
  elementType: ViewType,
  vectorLength: ULong,
  elements: List<U>,
  wrapper: (U) -> V
): VectorViewRead<V> {
  require(
    vectorLength == elements.size.toULong()
  ) { "Vector length $vectorLength is not equal to the number of elements given ${elements.size}" }
  return initializeVectorView(elementType, elements, wrapper).commitChanges()
}


