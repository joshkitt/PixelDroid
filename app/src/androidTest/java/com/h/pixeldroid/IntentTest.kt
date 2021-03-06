package com.h.pixeldroid

import android.content.Context
import android.content.Intent
import android.text.SpannableString
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.DrawerMatchers
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.h.pixeldroid.utils.db.AppDatabase
import com.h.pixeldroid.utils.db.entities.InstanceDatabaseEntity
import com.h.pixeldroid.utils.db.entities.UserDatabaseEntity
import com.h.pixeldroid.posts.StatusViewHolder
import com.h.pixeldroid.utils.api.objects.Account
import com.h.pixeldroid.utils.api.objects.Account.Companion.ACCOUNT_TAG
import com.h.pixeldroid.settings.AboutActivity
import com.h.pixeldroid.testUtility.MockServer
import com.h.pixeldroid.testUtility.clearData
import com.h.pixeldroid.testUtility.initDB
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class IntentTest {

    private lateinit var mockServer: MockServer
    private lateinit var db: AppDatabase
    private lateinit var context: Context

    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(100)

    @get:Rule
    var mLoginActivityActivityTestRule =
        ActivityTestRule(
            AboutActivity::class.java
        )

    @Before
    fun before() {
        mockServer = MockServer()
        mockServer.start()
        val baseUrl = mockServer.getUrl()

        context = ApplicationProvider.getApplicationContext()
        db = initDB(context)
        db.clearAllTables()
        db.instanceDao().insertInstance(
            InstanceDatabaseEntity(
                uri = baseUrl.toString(),
                title = "PixelTest"
            )
        )

        db.userDao().insertUser(
            UserDatabaseEntity(
                    user_id = "123",
                    instance_uri = baseUrl.toString(),
                    username = "Testi",
                    display_name = "Testi Testo",
                    avatar_static = "some_avatar_url",
                    isActive = true,
                    accessToken = "token",
                    refreshToken = refreshToken,
                    clientId = clientId,
                    clientSecret = clientSecret
            )
        )
        db.close()

        Intents.init()
    }


    @Test
    fun clickingMentionOpensProfile() {
        ActivityScenario.launch(MainActivity::class.java)

        val account = Account("1450", "deerbard_photo", "deerbard_photo",
            "https://pixelfed.social/deerbard_photo", "deerbard photography",
            "",
            "https://pixelfed.social/storage/avatars/000/000/001/450/SMSep5NoabDam1W8UDMh_avatar.png?v=4b227777d4dd1fc61c6f884f48641d02b4d121d3fd328cb08b5531fcacdabf8a",
            "https://pixelfed.social/storage/avatars/000/000/001/450/SMSep5NoabDam1W8UDMh_avatar.png?v=4b227777d4dd1fc61c6f884f48641d02b4d121d3fd328cb08b5531fcacdabf8a",
            "", "", false, emptyList(), null,
            "2018-08-01T12:58:21.000000Z", 72, 68, 27,
            null, null, false, null)
        val expectedIntent: Matcher<Intent> = CoreMatchers.allOf(
            IntentMatchers.hasExtra(ACCOUNT_TAG, account)
        )

        Thread.sleep(1000)

        //Click the mention
        Espresso.onView(ViewMatchers.withId(R.id.list))
            .perform(RecyclerViewActions.actionOnItemAtPosition<StatusViewHolder>
                (0, clickClickableSpanInDescription("@Dobios")))

        //Wait a bit
        Thread.sleep(1000)

        //Check that the Profile is shown
        intended(expectedIntent)
    }

    private fun clickClickableSpanInDescription(textToClick: CharSequence): ViewAction {
        return object : ViewAction {

            override fun getConstraints(): Matcher<View> {
                return Matchers.instanceOf(TextView::class.java)
            }

            override fun getDescription(): String {
                return "clicking on a ClickableSpan"
            }

            override fun perform(uiController: UiController, view: View) {
                val textView = view.findViewById<View>(R.id.description) as TextView
                val spannableString = textView.text as SpannableString

                if (spannableString.isEmpty()) {
                    // TextView is empty, nothing to do
                    throw NoMatchingViewException.Builder()
                        .includeViewHierarchy(true)
                        .withRootView(textView)
                        .build()
                }

                // Get the links inside the TextView and check if we find textToClick
                val spans = spannableString.getSpans(0, spannableString.length, ClickableSpan::class.java)
                if (spans.isNotEmpty()) {
                    var spanCandidate: ClickableSpan
                    for (span: ClickableSpan in spans) {
                        spanCandidate = span
                        val start = spannableString.getSpanStart(spanCandidate)
                        val end = spannableString.getSpanEnd(spanCandidate)
                        val sequence = spannableString.subSequence(start, end)
                        if (textToClick.toString() == sequence.toString()) {
                            span.onClick(textView)
                            return
                        }
                    }
                }

                // textToClick not found in TextView
                throw NoMatchingViewException.Builder()
                    .includeViewHierarchy(true)
                    .withRootView(textView)
                    .build()

            }
        }
    }


    @Test
    fun clickEditProfileMakesIntent() {
        ActivityScenario.launch(MainActivity::class.java)

        Espresso.onView(ViewMatchers.withId(R.id.drawer_layout))
            .check(ViewAssertions.matches(DrawerMatchers.isClosed())) // Left Drawer should be closed.
            .perform(DrawerActions.open()) // Open Drawer

        val expectedIntent: Matcher<Intent> = CoreMatchers.allOf(
            IntentMatchers.hasAction(Intent.ACTION_VIEW),
            IntentMatchers.hasDataString(CoreMatchers.containsString("settings/home"))
        )

        // Start the screen of your activity.
        Espresso.onView(ViewMatchers.withText(R.string.menu_account)).perform(ViewActions.click())
        // Check that profile activity was opened.
        Espresso.onView(ViewMatchers.withId(R.id.editButton))
            .perform(ViewActions.click())
        intended(expectedIntent)

    }

    @After
    fun after() {
        Intents.release()
        clearData()
        mockServer.stop()
    }
}