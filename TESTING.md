# TESTING.md — how to check everything works (no second phone needed)

Do these in order the first time. Everything here works with just your own phone and SIM.

## 0. Install & set default

1. Install `apk/Messages.apk` (see README). Open it.
2. Allow all the permission prompts.
3. Tap the blue banner and confirm making it the **default SMS app**.
4. ✅ **Pass:** the banner disappears, and your existing conversations from Google Messages
   appear in the list with names and photos. (History lives in Android's system storage,
   so it shows up automatically — nothing is imported or copied.)

## 1. Receiving works

You don't need a second phone — you need any service that texts you:

- Ask your **carrier** for something by SMS (most carriers text back your balance — dial their
  short code or use their app), or
- Trigger any **verification SMS** (log into a website that sends login codes), or
- Use your bank/telco app that sends SMS alerts.

✅ **Pass:** the message arrives **as a notification from Messages** (not Google Messages),
appears in the conversation list, and the unread badge shows. Open it — the badge clears.

## 2. Sending works

1. Tap the blue **+** button → type **your own phone number** → "Send to …".
2. Type `test 1` and hit send.
3. ✅ **Pass:** the bubble appears in blue on the right with a little check when sent —
   and because you texted *yourself*, it also **arrives back** as an incoming message.
   That one loop tests sending *and* receiving in one go.
4. In the bubble's timestamp row you should see a clock (sending) → check (sent).
   If your carrier supports delivery reports you'll see a double-check circle (delivered).

## 3. Direct reply from the notification

1. Text yourself again, but **don't open the app** when it arrives.
2. Pull down the notification → tap **Reply** → type something → send.
3. ✅ **Pass:** the reply sends without opening the app (and loops back to you again).

## 4. Scheduled send fires

1. Open the conversation with yourself.
2. Type `scheduled test`, then **long-press the send button** (or tap the little clock).
3. Choose **Pick date & time** → pick **3 minutes from now** → Schedule.
4. You'll see an outlined "Scheduled" bubble with the time, plus Edit / Cancel.
5. **Lock the phone and wait.**
6. ✅ **Pass:** at the chosen minute the message sends by itself (you'll get it back as
   an incoming text). The outlined bubble becomes a normal blue bubble.
7. Also check the **Scheduled screen** (clock icon in the bottom bar): before it fires
   the message is listed there; after, the list is empty.

### Scheduled send — reboot test

Schedule a message ~10 minutes out, **restart the phone**, keep it locked.
✅ **Pass:** it still sends at the right time (alarms are re-armed at boot).

## 5. MMS (picture message)

1. In the conversation with yourself, tap the paperclip → pick a photo → send.
2. MMS needs mobile data enabled (that's how MMS works on every phone).
3. ✅ **Pass:** the photo sends and arrives back. Large photos are automatically
   compressed to your carrier's MMS size limit.
   *Note:* some carriers no longer deliver self-addressed MMS — if it doesn't loop back
   but shows "sent", test with a friend's number when convenient.

## 6. Multi-SIM

With both SIMs active: in the composer you'll see a **SIM 1 / SIM 2** chip — tap it to
switch which SIM sends. Incoming messages show which SIM received them next to the time.

## 7. Everything else, quickly

- **Search:** magnifier icon on the home screen — type any word from an old message.
- **Pin / archive / mute / delete:** long-press a conversation row.
- **Swipe left** on a row = archive. Find it under the "Archived" chip.
- **Block:** Settings → Blocked numbers → add a number. Texts from it are silently dropped.
- **Backup:** Settings → Export backup → saves a JSON file (SMS text + app settings; MMS
  pictures aren't included). Import restores it. No cloud involved.

## If something breaks

Tell Claude exactly: which step, what you tapped, what you saw (screenshot of any error).
The app can't crash your messages — everything lives in Android's own message store.
