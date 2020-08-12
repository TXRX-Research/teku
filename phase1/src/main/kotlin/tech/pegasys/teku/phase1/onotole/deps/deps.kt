package tech.pegasys.teku.phase1.onotole.deps

import org.apache.milagro.amcl.BLS381.BIG
import org.apache.milagro.amcl.BLS381.FP2
import org.apache.tuweni.crypto.Hash
import tech.pegasys.teku.phase1.integration.bls.G2Points
import tech.pegasys.teku.phase1.integration.toUInt64
import tech.pegasys.teku.phase1.onotole.phase1.BLSPubkey
import tech.pegasys.teku.phase1.onotole.phase1.BLSSignature
import tech.pegasys.teku.phase1.onotole.pylib.pyint
import tech.pegasys.teku.phase1.onotole.ssz.Bytes
import tech.pegasys.teku.phase1.onotole.ssz.Bytes32
import tech.pegasys.teku.phase1.onotole.ssz.Bytes48
import tech.pegasys.teku.phase1.onotole.ssz.Bytes96
import tech.pegasys.teku.phase1.onotole.ssz.SSZCollection
import tech.pegasys.teku.phase1.onotole.ssz.boolean
import tech.pegasys.teku.phase1.onotole.ssz.uint64
import tech.pegasys.teku.phase1.onotole.ssz.uint8
import tech.pegasys.teku.ssz.SSZTypes.Bytes4
import tech.pegasys.teku.ssz.backing.ViewRead
import tech.pegasys.teku.ssz.backing.view.BasicViews
import tech.pegasys.teku.ssz.backing.view.ViewUtils
import java.math.BigInteger
import tech.pegasys.teku.bls.BLS as TekuBLS
import tech.pegasys.teku.bls.BLSPublicKey as TekuBLSPublicKey
import tech.pegasys.teku.bls.BLSSecretKey as TekuBLSSecretKey
import tech.pegasys.teku.bls.BLSSignature as TekuBLSSignature

fun hash_tree_root(v: Any): Bytes32 {
  return when (v) {
    is ViewRead -> v.hashTreeRoot()
    is Byte -> BasicViews.ByteView(v).hashTreeRoot()
    is Bytes4 -> BasicViews.Bytes4View(v).hashTreeRoot()
    is uint8 -> BasicViews.ByteView(v.toByte()).hashTreeRoot()
    is uint64 -> BasicViews.UInt64View(v.toUInt64()).hashTreeRoot()
    is boolean -> BasicViews.BitView(v).hashTreeRoot()
    is Bytes -> ViewUtils.createVectorFromBytes(v).hashTreeRoot()
    is BLSSignature -> ViewUtils.createVectorFromBytes(v.wrappedBytes).hashTreeRoot()
    is BLSPubkey -> ViewUtils.createVectorFromBytes(v).hashTreeRoot()
    is Bytes32 -> BasicViews.Bytes32View(v).hashTreeRoot()
    is SSZCollection<*> -> v.hashTreeRoot()
    else -> throw UnsupportedOperationException("Unsupported type ${v::class.qualifiedName}")
  }
}

fun hash(a: Bytes): Bytes32 = Hash.sha2_256(a)
fun hash(a: Bytes96): Bytes32 = hash(a.wrappedBytes)

data class FQ2(val coeffs: Pair<pyint, pyint>)

fun BIG.toPyInt(): pyint {
  val bytes = ByteArray(BIG.MODBYTES)
  this.toBytes(bytes)
  return pyint(BigInteger(1, bytes))
}

fun FP2.toFQ2(): FQ2 = FQ2(Pair(this.a.toPyInt(), this.b.toPyInt()))

interface BLS {
  fun Sign(privkey: pyint, message: Bytes): BLSSignature
  fun Verify(pubkey: Bytes48, message: Bytes, signature: BLSSignature): boolean
  fun Aggregate(signatures: Collection<BLSSignature>): BLSSignature
  fun FastAggregateVerify(pubkeys: Collection<Bytes48>, root: Bytes, signature: BLSSignature): boolean
  fun AggregateVerify(pubkeys: List<Bytes48>, messages: List<Bytes>, signature: BLSSignature): boolean
  fun signature_to_G2(signature: BLSSignature): Triple<FQ2, FQ2, FQ2>
}

object BLS12381 : BLS {
  override fun Sign(privkey: pyint, message: Bytes): BLSSignature {
    return BLSSignature(
      TekuBLS.sign(
        TekuBLSSecretKey.fromBytes(Bytes32.leftPad(Bytes.wrap(privkey.value.toByteArray()))),
        message
      ).toBytesCompressed()
    )
  }

  override fun Verify(pubkey: Bytes48, message: Bytes, signature: BLSSignature): boolean {
    return TekuBLS.verify(
      TekuBLSPublicKey.fromBytesCompressed(pubkey),
      message,
      TekuBLSSignature.fromBytesCompressed(signature.wrappedBytes)
    )
  }

  override fun Aggregate(signatures: Collection<BLSSignature>): BLSSignature {
    return BLSSignature(
      TekuBLS.aggregate(signatures.map { s -> TekuBLSSignature.fromBytesCompressed(s.wrappedBytes) })
        .toBytesCompressed()
    )
  }

  override fun FastAggregateVerify(pubkeys: Collection<Bytes48>, root: Bytes, signature: BLSSignature): boolean {
    return TekuBLS.fastAggregateVerify(
      pubkeys.map { k -> TekuBLSPublicKey.fromBytesCompressed(k) }.toList(),
      root,
      TekuBLSSignature.fromBytesCompressed(signature.wrappedBytes)
    )
  }

  override fun AggregateVerify(pubkeys: List<Bytes48>, messages: List<Bytes>, signature: BLSSignature): boolean {
    return TekuBLS.aggregateVerify(
      pubkeys.map { p -> TekuBLSPublicKey.fromBytesCompressed(p) }.toList(),
      messages,
      TekuBLSSignature.fromBytesCompressed(signature.wrappedBytes)
    )
  }

  override fun signature_to_G2(signature: BLSSignature): Triple<FQ2, FQ2, FQ2> {
    val p = G2Points.fromBytesCompressed(signature.wrappedBytes)
    return Triple(p.getx().toFQ2(), p.gety().toFQ2(), p.getz().toFQ2())
  }
}

object NoOpBLS : BLS {
  override fun Sign(privkey: pyint, message: Bytes): BLSSignature {
    return Bytes96()
  }

  override fun Verify(pubkey: Bytes48, message: Bytes, signature: BLSSignature): boolean {
    return true
  }

  override fun Aggregate(signatures: Collection<BLSSignature>): BLSSignature {
    return Bytes96()
  }

  override fun FastAggregateVerify(pubkeys: Collection<Bytes48>, root: Bytes, signature: BLSSignature): boolean {
    return true
  }

  override fun AggregateVerify(pubkeys: List<Bytes48>, messages: List<Bytes>, signature: BLSSignature): boolean {
    return true
  }

  override fun signature_to_G2(signature: BLSSignature): Triple<FQ2, FQ2, FQ2> {
    val zero = FQ2(pyint(BigInteger.ZERO) to pyint(BigInteger.ZERO))
    return Triple(zero, zero, zero)
  }
}

object PseudoBLS : BLS {

  override fun Sign(privkey: pyint, message: Bytes): BLSSignature {
    val secretKey = TekuBLSSecretKey.fromBytes(Bytes32.leftPad(Bytes.wrap(privkey.value.toByteArray())))
    val publicKey = Bytes48.wrap(TekuBLSPublicKey(secretKey).toBytesCompressed())
    return pseudoSign(publicKey, message)
  }

  override fun Verify(pubkey: Bytes48, message: Bytes, signature: BLSSignature): boolean {
    val expected = pseudoExp(mapMessage(message), pubkey)
    return expected == signature
  }

  override fun Aggregate(signatures: Collection<BLSSignature>): BLSSignature {
    return mergeSignatures(signatures)
  }

  override fun FastAggregateVerify(pubkeys: Collection<Bytes48>, root: Bytes, signature: BLSSignature): boolean {
    val expected = mergeSignatures(pubkeys.map { pseudoSign(it, root) })
    return expected == signature
  }

  override fun AggregateVerify(pubkeys: List<Bytes48>, messages: List<Bytes>, signature: BLSSignature): boolean {
    val expected = mergeSignatures(pubkeys.zip(messages).map { pseudoSign(it.first, it.second) })
    return expected == signature
  }

  override fun signature_to_G2(signature: BLSSignature): Triple<FQ2, FQ2, FQ2> {
    val p = G2Points.fromBytesCompressed(signature.wrappedBytes)
    return Triple(p.getx().toFQ2(), p.gety().toFQ2(), p.getz().toFQ2())
  }

  private fun mapMessage(message: Bytes): Bytes48 {
    return Bytes48.leftPad(message)
  }

  private fun pseudoExp(a: Bytes48, b: Bytes48): Bytes96 {
    return if (b == Bytes48.ZERO) Bytes96() else Bytes96.leftPad(Bytes.concatenate(a, b))
  }

  fun pseudoSign(key: Bytes48, message: Bytes): Bytes96 {
    return pseudoExp(mapMessage(message), key)
  }

  private fun pseudoSum(a: Bytes48, b: Bytes48): Bytes48 {
    return Bytes48.leftPad(
      toBytesValue(
        BigInteger(1, a.toArrayUnsafe())
          .add(BigInteger(1, b.toArrayUnsafe()))
          .mod(MAX_BYTES48_PLUS_ONE),
        48
      )
    )
  }

  private fun toBytesValue(value: BigInteger, size: Int): Bytes {
    val bytes = value.toByteArray()
    require(!(bytes.size > size + 1 || bytes.size == size + 1 && bytes[0] != 0.toByte())) {
      "Too big value, cannot convert to Bytes $size"
    }
    return if (bytes.size == size + 1) {
      Bytes.wrap(bytes, 1, size)
    } else {
      Bytes.wrap(bytes)
    }
  }

  fun mergeSignatures(signatures: Collection<BLSSignature>): BLSSignature {
    val messageAcc = signatures
      .map { Bytes48.leftPad(it.wrappedBytes.slice(0, 48)) }
      .fold(Bytes48.ZERO, ::pseudoSum)
    val keyAcc: Bytes48 = signatures
      .map { Bytes48.leftPad(it.wrappedBytes.slice(48, 48)) }
      .fold(Bytes48.ZERO, ::pseudoSum)
    return pseudoExp(messageAcc, keyAcc)
  }

  private val MAX_BYTES48_PLUS_ONE = BigInteger.ONE.shiftLeft(48 * 8)
}
