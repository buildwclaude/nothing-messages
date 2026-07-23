# DESIGN.md — the Figma design, decoded

Source of truth: [Messaging App UI](https://www.figma.com/community/file/881015895655545375/messaging-app-ui)
by **fafaaye** (free community file, 27k+ duplicates). The raw design file was downloaded from
Figma's public community viewer and decoded (fig-kiwi format) to extract **exact** values —
every token below comes from the file itself, not from screenshots. A copy is in `design/canvas.fig`.

## Screens in the file → Compose screens here

| Figma frame (375×812) | This app |
|---|---|
| Home / "Recent Chats" (pinned grid, filter chips, chat rows, bottom bar + FAB) | `ConversationsScreen` |
| Group Chat (bubbles, typing area, top bar) | `ThreadScreen` |
| Settings | `SettingsScreen` |
| Group Description / Create category sheets | patterns reused for the schedule bottom sheet |
| Recent Calls / Call History | **not built** — this is a messaging app; calling stays with the Phone app |

## Colours (fill-count in file)

| Token | Hex | Used for |
|---|---|---|
| Brand blue | `#2F80ED` | outgoing bubbles, FAB, selected chip, badges, active nav icon |
| Text primary | `#1B1A57` | titles, names, incoming bubble text |
| Text secondary | `#4F5E7B` | previews, subtitles, inactive icons |
| Navy accent | `#21205A` | status-bar glyphs, dark accents |
| Incoming bubble | `#F7F7F7` | incoming bubbles, pinned tiles, cards |
| Timestamps (rows) | `#333333` | list-row times |
| Muted | `#A1A1BC` | in-bubble times, "admin" labels |
| Disabled/pending | `#C4C4C4` | send button when empty, image placeholders |
| Online green | `#4CE417` | presence dots (see honesty notes) |
| Away yellow | `#F2C94C` | presence dots |
| Error red | `#EB5757` | failures, destructive actions |
| Success green | `#27AE60` | success states |
| Sender accents | `#F2994A` `#2D9CDB` `#9B51E0` | per-sender name colours in group chats |
| Divider | `#EDEDED` | hairlines |
| Shadow | `#466087` @ 10%, y=4 blur=32 | card shadows (y=-8 for bottom bars) |

## Typography

Design font: **Plus Jakarta Display** (Regular/Medium/Bold).
**Substitution:** we bundle **Plus Jakarta Sans** (SIL Open Font License, same designer/family —
the Display cut isn't openly licensed; Sans is its successor and near-identical at UI sizes).
The iOS status bar in the mockups uses SF Pro — irrelevant on Android (system draws it).

Scale used (all from the file): 24 Medium (page titles) · 18 Medium (screen/section titles) ·
14 Medium (names, list titles) · 12 Regular, line-height 18 (message text, previews) ·
12 Medium (labels, chips) · 9–10 Regular (timestamps) · 9–10 Bold (unread badge count).

## Shape & spacing

Corner radii: **8** (bubbles, chips, cards, inputs) · **24/27** (pinned tiles, large cards) ·
**4** (image thumbnails in bubbles) · **32** top-only (bottom sheets) · **circle** (avatars, FAB, badges).
Key sizes: chat row 64dp; avatar 48 (rows) / 40 (bubbles) / 36 (tiles); presence dot 12;
unread badge 24; FAB 56; bottom bar ~86; pinned tile 167×102; bubble max width ~280.

## Icons

The design uses a thin 2px-stroke outline icon set (custom). **Substitution:** bundled
**Feather icons** (MIT) converted to Android vector drawables — same 24×24 grid, 2px stroke,
round caps — visually equivalent. No Compose Material icon placeholders are used.

## Honesty notes — design elements that can't be truthful in an SMS app

- **Online/typing indicators** (green dots, "+2 others are typing"): SMS has no presence or
  typing signal. Presence dots are **not shown**; the green/yellow palette is kept for other states.
- **"7 Online, from 12 peoples"** group subtitle → replaced with member count / number.
- The design's bottom-nav "Calls" tab → dropped (see screen table). Nav bar carries
  Chats / Scheduled / Settings with the design's floating FAB.
- The design is light-mode only; at the owner's request the app now also ships a **dark palette**
  (follows the system setting). Typography is **Inter** (SIL OFL, bundled) — the open typeface
  closest to Apple's San Francisco — for a clean iMessage-like feel. The original Figma palette
  remains the light theme.
- Extra touches added on request: a swipeable pager for the All/Unread/Groups/Archived tabs,
  a snapping **time-window wheel** above the bottom bar, and **Clawd** — a small code-drawn
  animated crab — in the header's spare space.
