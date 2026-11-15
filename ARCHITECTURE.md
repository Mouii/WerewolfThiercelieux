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
file. The names the rules mention are listed in `model/RoleId.kt`.

This matters : the identity of a role is a text, never the name of a Kotlin class, so
renaming or minifying the code can never break a saved game or the roles file.

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
| `game/PowerRulesTest.kt` | The powers that wake up after a death |

A test reads as three parts : the game is prepared, one thing is done, the result is
checked.

## Where to add something

| What you want to add | Where |
|---|---|
| A new role | In the roles file (`assets/Roles-default*.json`), or from the application itself |
| A new rule of the game | A file of `game/`, plus its test |
| A new message to the moderator | A `GameMessage`, its text in `strings.xml`, one line in `ui/Narrator.kt` |
| A new colour | `res/values/colors.xml` only |
| A new screen | `ui/activity/`, declared in `AndroidManifest.xml` |
