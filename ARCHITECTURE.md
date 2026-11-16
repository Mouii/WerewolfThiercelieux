# Architecture

How the code of the application is organised. Written to be read without knowing Kotlin.

## The four layers

The code is cut in four layers. **A layer only knows the layers below it**, never the ones above.

```
ui       the screens : what is drawn, what happens when the moderator taps
   |
game     the rules of the game : the night, the phases, the deaths, the powers
   |
data     the files of the phone : the roles file, the saved game, the settings
   |
model    one role of the game, and what describes it
```

`data` sits between `game` and `model` because it reads and writes a game, so it
knows the rules' vocabulary. The arrow never goes the other way : **the rules never
read a file and never know a screen exists**.

| Folder | What it holds |
|---|---|
| `model/` | A role (`CharacterGame`), its power, its activation condition, the usual order of the roles |
| `main/werewolfkotlin/game/` | The game being played : the table, the night, the phases, the deaths, the powers |
| `main/werewolfkotlin/data/` | Everything written on the phone : roles file, saved game, player names, settings |
| `main/werewolfkotlin/ui/` | The screens (`activity/`), the windows (`dialog/`), the drawn views (`view/`), the look (`design/`) |

## Two rules that keep it that way

**1. No text of the application inside the rules.**
A rule never builds a sentence. It says *what* happened with a `GameMessage`
(`PLAYER_DEAD`, `TARGET`...) and the names it concerns. One single file turns that into a
sentence : `ui/Narrator.kt`. Adding a language only means adding
`res/values-xx/strings.xml`.

**2. No value of design inside the code.**
Colours and sizes live in `res/values/colors.xml`. The code asks for them by name :
`colorOf(R.color.cyan)`. No `#RRGGBB` is ever written in a `.kt` file.

Because of rule 1, `model/` and `game/` are plain Kotlin : no Android at all. That is
what makes them testable on a computer, without a phone or an emulator.

## A role is data, not a class

There is **one** class for every role of the game : `model/CharacterGame.kt`. A role is
told apart by its name, `className` ("Seer", "Werewolf"...), which is read from the roles
file.

This matters : the identity of a role is a text, never the name of a Kotlin class, so
renaming or minifying the code can never break a saved game or the roles file.

**No rule of the game names a role.** What used to be a special case written in Kotlin is
now a field of the roles file :

| Field | What it says |
|---|---|
| `playsAt: "WEREWOLF_TURN"` | the role is called with the pack (little girl, wolfhound, wolf father) |
| `definesPackTurn` | the moment of this role is also the moment of the pack (the simple werewolf) |
| `hasSeparateTurn` | the role eats with the pack and then once alone (big bad wolf, white werewolf) |
| `ownsInfectionBlock` | the block of this role holds the players it infected (wolf father) |
| `condition` + `becomes` | what has to happen, and what the role becomes then |

`model/RoleId.kt` holds the only name still needed : the place of the pack in the turn
order is remembered under it, because the pack is not a role and has no place of its own.

### A role that changes during the game

A role says *when* it changes (`condition`) and *what it becomes* (`becomes`). The engine
applies it, so a new behaviour needs no Kotlin :

```json
"condition": "IS_ALONE",
"becomes": { "fires": "ONCE", "isNocturnal": false, "powerState": "PERMANENT" }
```

| Condition | What has to happen | Watched in `PowerRules` by |
|---|---|---|
| `ONE_WEREWOLF`, `ONE_VILLAGER` | a player of that side dies | `afterDeath` |
| `ALL_WEREWOLVES`, `ALL_VILLAGERS` | every other player of that side is dead | `afterDeath` |
| `IS_ALONE` | the role is the last of its kind | `afterDeath` |
| `MY_TARGET_DIES` | a player the role chose dies | `afterDeath` |
| `TARGET_CHOSEN` | the role has just chosen a player | `afterTargeting` |
| `TURN_REACHED` | the game reaches the turn `conditionTurn` | `onNewTurn` |
| `ON_MORNING` | the night ends | `onMorning` |

`becomes` can set `isNocturnal`, `isWerewolf`, `isSolo`, `doTargeting`, `doKilling`,
`powerState` and `playsAt`. With `copiesTarget`, the role takes the whole behaviour of the role
held by the player it chose, and keeps its own card (see `CharacterGame.copyBehaviourOf`).
A role with a condition and no `becomes` simply wakes up again.

`fires` says whether the change happens only the first time or every time the condition is
met. `CharacterGame.changeApplied` remembers that it happened, and is saved with the game.

Two more fields describe the power of a role without any condition :

| Field | What it says |
|---|---|
| `keepsTargets` | the players it chose stay chosen night after night (the charmed of the piper) |
| `targetsDead` | it chooses among the dead players (the necromancer) |

When a role copies another one, the copied player is kept in a block of his own (kind `MODEL`),
separate from the targets of the role : who was copied stays visible for the whole game.

**The pack without wolves.** While no player holds a werewolf role or a role that kills, the pack
of werewolves is still called at night (`NightFlow.isPackWithoutWolves`) : the moderator may not
have entered the roles, and can still mark the victim. It gives way to the real pack as soon as
such a role is given, and the moderator can turn it off in the turn order window.

One effect stays in Kotlin, in `game/PowerRules.kt`, because it is not about the role itself :
the elder, who takes the powers of the **other** villagers away.

## The game being played

Everything about the game in progress is in one place, `game/GameState.kt`. It only holds
the state. **The rules that change it are in separate files**, one subject each :

| File | Subject |
|---|---|
| `GameSession` | Start a game, start it again, take every role away |
| `PhaseCycle` | The cycle : sleeping, night, dawn, day, sunset |
| `NightFlow` | The night : who is called, in which order, the moves of the moderator |
| `RoleRoster` | The roles in the game : given to a player, deaths, infection |
| `PowerRules` | The powers that wake up because of what happened |
| `TableActions` | What a tap on a player does : a victim, a target |
| `EffectBlocks` | The blocks of the "Roles" tab : who is targeted, who is about to die |
| `Lights` | Which players are lit, and in which colour |

`GameState` is shared by the whole application, so **every test starts by emptying it**
(see `GameFixtures.newGame` in the tests).

## The roles files

One file is one **source** of roles, named by its key. A role can exist in several sources : those
are its variants, and the moderator picks the one he wants when he gives a role to a player.

| File | Key | Written by |
|---|---|---|
| `assets/Roles-default.json` | `NORMAL` | the roles of the game. Read only |
| `assets/Roles-author.json` | `AUTHOR` | the variants of the author. Read only, and optional |
| `Roles-custom.json` of the phone | `CUSTOM` | what the moderator writes. The only file the application writes |

Because the roles of the game are never written to, **an update can never lose what the moderator
wrote** : his roles live in their own file.

A role is described once, and its texts are given for every language, so there is no file per
language :

```json
{ "nameRole": "Seer", "extension": "MAIN", "powerState": "PERMANENT", "isNocturnal": true,
  "doTargeting": true,
  "texts": { "en": { "name": "Seer", "description": "...", "action": "..." },
             "fr": { "name": "Voyante", "description": "...", "action": "..." } } }
```

The key of a source only says **where a role comes from**. What tells the variants of a role
apart is their **name** : a source can hold several variants of the same role (the author has two
Cupids), and the moderator picks a variant by its name.

One detail makes this safe : the name used as the key (`CharacterGame.key`) is the name in the
reference language of the file, English for the files of the application. The name shown on screen
follows the language of the phone, the key never does, so a saved game finds its roles again after
a change of language. Two variants of a role can therefore not share a name : a test checks the
files, and the creation screen refuses it.

Every value that names a choice (power, condition, moment of the night) is read as plain text and
converted with a default, so **one unknown word never makes a whole file unreadable**. Renaming a
value in a later version therefore cannot break the files already on the phones.

The box of the game a role belongs to (`extension`) is written only in the first source that
declares the role : a variant never changes the box of its role.

## Saving a game

A game that is left is written in the phone settings, as text in JSON format.

- `data/GameSaveFormat.kt` : the format of the file, field by field. Read this file to
  know what a saved game holds.
- `data/GameSnapshot.kt` : turns the game being played into that format, and back.
- `data/GameSaveStore.kt` : writes it on the phone and reads it back.

Only what the game changed is written (the moment of the night of a role, its power, who
is dead...). The description of a role is read again from the roles file, so a game can
only be resumed while its roles still exist.

## Tests

`app/src/test/java/` runs on a computer, with no phone and no emulator :
`./gradlew test`.

| File | What it checks |
|---|---|
| `GameFixtures.kt` | Short ways to build a game in a test |
| `model/CharacterGameTest.kt` | The copies of a role, and what calling a role does |
| `game/NightFlowTest.kt` | The night : who is called, moving in it, the pack of werewolves |
| `game/RoleRosterTest.kt` | Giving a role, the deaths, coming back to the game |
| `game/PhaseCycleTest.kt` | The cycle of a turn |
| `game/TableActionsTest.kt` | Victims, targets, roles given at random |
| `game/PowerRulesTest.kt` | The conditions and the changes of the roles |
| `game/PackTurnTest.kt` | The moment of the pack and the roles that play with it |
| `game/SpecialRolesTest.kt` | Doppleganger, mother of wolves, white werewolf, piper, necromancer, serial killer |
| `data/RoleFilesTest.kt` | The roles files themselves : readable, complete in every language, coherent |

A test reads as three parts : the game is prepared, one thing is done, the result is
checked.

## Where to add something

| What you want to add | Where |
|---|---|
| A new role | In the roles file of its source, or from the application itself |
| A new behaviour of a role | A `condition` and a `becomes` in the roles file, no Kotlin |
| A new language | One more entry in `texts` of each role, plus `res/values-xx/strings.xml` |
| A new rule of the game | A file of `game/`, plus its test |
| A new message to the moderator | A `GameMessage`, its text in `strings.xml`, one line in `ui/Narrator.kt` |
| A new colour | `res/values/colors.xml` only |
| A new screen | `ui/activity/`, declared in `AndroidManifest.xml` |
