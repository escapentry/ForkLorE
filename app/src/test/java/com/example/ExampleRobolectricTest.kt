package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.CallDirection
import com.example.model.CallLogEntry
import com.example.model.ContactEntry
import com.example.model.ConversationThread
import com.example.model.ForkLoreNumberHelper
import com.example.ui.ForkLoreUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `app name matches ForkLorE`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("ForkLorE", context.getString(R.string.app_name))
  }

  @Test
  fun `number helper normalizes matches and converts names to T9 digits`() {
    assertTrue(ForkLoreNumberHelper.numbersMatch("+1 (555) 234-5678", "5552345678"))
    assertFalse(ForkLoreNumberHelper.numbersMatch("5552345678", "5552349999"))
    assertEquals("25423", ForkLoreNumberHelper.toT9Digits("Alice"))

    val alice = ContactEntry(
      id = 1L,
      lookupKey = "k1",
      displayName = "Alice Vance",
      primaryNumber = "555-234-5678",
      allNumbers = listOf("555-234-5678"),
      photoUri = null,
      isStarred = true,
    )
    assertTrue(ForkLoreNumberHelper.matchesDialpadQuery(alice, "254"))
    assertTrue(ForkLoreNumberHelper.matchesDialpadQuery(alice, "2345"))
    assertFalse(ForkLoreNumberHelper.matchesDialpadQuery(alice, "999"))
  }

  @Test
  fun `call duration formatter outputs clean labels`() {
    assertEquals("0s", ForkLoreNumberHelper.formatCallDuration(0L))
    assertEquals("45s", ForkLoreNumberHelper.formatCallDuration(45L))
    assertEquals("3m 5s", ForkLoreNumberHelper.formatCallDuration(185L))
    assertEquals("1h 2m", ForkLoreNumberHelper.formatCallDuration(3729L))
  }

  @Test
  fun `ui state filters contacts call logs and conversations accurately`() {
    val contacts = listOf(
      ContactEntry(1L, "k1", "Alice Vance", "555-0101", listOf("555-0101"), null, true),
      ContactEntry(2L, "k2", "Marcus Cole", "555-0202", listOf("555-0202"), null, false),
    )
    val logs = listOf(
      CallLogEntry(10L, "555-0101", "Alice Vance", CallDirection.OUTGOING, 1000L, 64L),
      CallLogEntry(11L, "555-0202", "Marcus Cole", CallDirection.MISSED, 2000L, 0L),
    )
    val convos = listOf(
      ConversationThread(100L, "555-0101", "Alice Vance", "See you at 5pm", 1000L, 1, null),
      ConversationThread(101L, "555-0202", "Marcus Cole", "Build finished cleanly", 2000L, 0, null),
    )

    val state = ForkLoreUiState(
      searchQuery = "alice",
      dialpadDigits = "254",
      contacts = contacts,
      callLogs = logs,
      conversations = convos,
    )

    assertEquals(listOf("Alice Vance"), state.filteredContacts.map { it.displayName })
    assertEquals(listOf("Alice Vance"), state.filteredCallLogs.mapNotNull { it.displayName })
    assertEquals(listOf("Alice Vance"), state.filteredConversations.mapNotNull { it.displayName })
    assertEquals(listOf("Alice Vance"), state.dialpadMatches.map { it.displayName })
  }
}
