package tech.pegasys.teku.phase1.eth1engine

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.rlp.RLP
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import tech.pegasys.teku.phase1.integration.datastructures.LOGS_BLOOM_SIZE
import tech.pegasys.teku.phase1.onotole.ssz.Bytes20
import java.security.Security
import kotlin.math.abs
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Eth1BlockUtilTest {

  @BeforeAll
  fun setup() {
    Security.addProvider(BouncyCastleProvider())
  }

  @Test
  fun shouldDecodeBlockRLP() {
      val block = randomBlock()

      val blockRLP = encodeEth1BlockWithRLP(block)
      val decodedBlock = decodeEth1BlockRLP(blockRLP)

      Assertions.assertEquals(block, decodedBlock)
  }

  @Test
  fun shouldDecodeBlockHeaderRLP() {
    val header = randomHeader()

    val headerRLP = encodeEth1BlockHeaderWithRLP(header)
    val decodedHeader = decodeEth1BlockHeaderRLP(headerRLP)

    Assertions.assertEquals(header, decodedHeader)
  }

  @Test
  fun shouldDecodeBlockHeaderWithEmptyTxRoot() {
    var header = randomHeader()
    header = header.copy(txRoot = EMPTY_TRIE_HASH, receiptsRoot = EMPTY_TRIE_HASH)

    val headerRLP = encodeEth1BlockHeaderWithRLP(header)
    val decodedHeader = decodeEth1BlockHeaderRLP(headerRLP)

    Assertions.assertEquals(header, decodedHeader)
  }

  @Test
  fun shouldMatchCanonicalRLP() {
    val canonicalRLP = Bytes.fromHexString("0xf901fcf901f7a066cc60741a36a2f5824627e455b67dad661c2d10f535852905ee3c0737502edea01dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347941000000000000000000000000000000000000000a0673d6cb7f843d8c1890c3f359c9a05b4e48a4a018730e90ed41b8d855aaeb601a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421b90100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000008302000003832fefd880845fbe50c980a00000000000000000000000000000000000000000000000000000000000000000880000000000000000c0c0")
    val decodedBlock = decodeEth1BlockRLP(canonicalRLP)
    val actualRLP = encodeEth1BlockWithRLP(decodedBlock)

    println(decodedBlock)

    Assertions.assertEquals(canonicalRLP, actualRLP)
  }
}

fun randomBlock() = Eth1Block(
  randomHeader(),
  (1..10).map { txIndex ->
    RLP.encodeList {
      it.writeValue(Bytes.random(abs(Random.Default.nextInt()) % 100))
      it.writeValue(Bytes.random(abs(Random.Default.nextInt()) % 100))
    }
  }
)

fun randomHeader() = Eth1BlockHeader(
  parentHash = Bytes32.random(),
  coinbase = Bytes20(Bytes.random(20)),
  stateRoot = Bytes32.random(),
  logsBloom = Bytes.random(LOGS_BLOOM_SIZE.toInt()),
  number = (abs(Random.Default.nextInt()) % 1000).toULong(),
  timestamp = (System.currentTimeMillis() / 1000).toULong(),
  gasLimit = (abs(Random.Default.nextInt()) % 1000).toULong(),
  gasUsed = (abs(Random.Default.nextInt()) % 1000).toULong(),
  receiptsRoot = Bytes32.random(),
  txRoot = Bytes32.random(),
  difficulty = (abs(Random.Default.nextInt()) % 1000).toULong(),
  mixHash = Bytes32.random(),
  extraData = Bytes32.random(),
  nonce = Bytes.wrap(ByteArray(8)),
  unclesHash = Bytes32.random()
)