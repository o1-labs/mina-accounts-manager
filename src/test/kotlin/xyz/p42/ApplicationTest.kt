package xyz.p42

import kotlinx.serialization.json.Json
import org.junit.Test
import xyz.p42.model.Account
import xyz.p42.model.GenesisLedger
import xyz.p42.utils.getAccountVkGraphQlQuery
import xyz.p42.utils.getImportAccountGraphQlQuery
import xyz.p42.utils.getLockAccountGraphQlQuery
import xyz.p42.utils.getUnlockAccountGraphQlQuery
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GraphQlQueryTest {
  @Test
  fun testUnlockAccountQuery() {
    accountCommonPassword = "test-password"
    val query = getUnlockAccountGraphQlQuery("B62qtest123")
    assertTrue(query.contains("unlockAccount"))
    assertTrue(query.contains("B62qtest123"))
    assertTrue(query.contains("test-password"))
  }

  @Test
  fun testLockAccountQuery() {
    val query = getLockAccountGraphQlQuery("B62qtest456")
    assertTrue(query.contains("lockAccount"))
    assertTrue(query.contains("B62qtest456"))
  }

  @Test
  fun testImportAccountQuery() {
    accountCommonPassword = "naughty blue worm"
    val query = getImportAccountGraphQlQuery("/root/.mina-network/key-pairs/B62qtest789")
    assertTrue(query.contains("importAccount"))
    assertTrue(query.contains("/root/.mina-network/key-pairs/B62qtest789"))
    assertTrue(query.contains("naughty blue worm"))
    assertTrue(query.contains("publicKey"))
    assertTrue(query.contains("alreadyImported"))
    assertTrue(query.contains("success"))
  }

  @Test
  fun testAccountVkQuery() {
    val query = getAccountVkGraphQlQuery("B62qabc")
    assertTrue(query.contains("account"))
    assertTrue(query.contains("B62qabc"))
    assertTrue(query.contains("verificationKey"))
  }
}

class ModelTest {
  private val json = Json { ignoreUnknownKeys = true; isLenient = true }

  @Test
  fun testAccountDeserialization() {
    val accountJson = """{"pk": "B62qtest", "sk": "EKtest"}"""
    val account = json.decodeFromString<Account>(accountJson)
    assertEquals("B62qtest", account.pk)
    assertEquals("EKtest", account.sk)
    assertEquals(false, account.used)
  }

  @Test
  fun testAccountWithoutSk() {
    val accountJson = """{"pk": "B62qtest"}"""
    val account = json.decodeFromString<Account>(accountJson)
    assertEquals("B62qtest", account.pk)
    assertNull(account.sk)
  }

  @Test
  fun testGenesisLedgerDeserialization() {
    val ledgerJson = """{
      "ledger": {
        "name": "test-ledger",
        "accounts": [
          {"pk": "B62q1", "sk": "EK1"},
          {"pk": "B62q2", "sk": "EK2"},
          {"pk": "B62q3"}
        ]
      }
    }"""
    val genesis = json.decodeFromString<GenesisLedger>(ledgerJson)
    assertEquals("test-ledger", genesis.ledger.name)
    assertEquals(3, genesis.ledger.accounts.size)
    assertNotNull(genesis.ledger.accounts[0].sk)
    assertNull(genesis.ledger.accounts[2].sk)
  }

  @Test
  fun testAccountUsedFlag() {
    val account = Account(pk = "B62qtest", sk = "EKtest")
    assertEquals(false, account.used)
    account.used = true
    assertEquals(true, account.used)
  }
}
