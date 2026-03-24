# CanvasMC Discord Bot

The official Discord bot for [CanvasMC](https://github.com/CraftCanvasMC), built with [Discord4J](https://discord4j.com/).

## Commands

| Command | Description |
|---------|-------------|
| `/about` | About CanvasMC |
| `/website` | Links to the CanvasMC website |
| `/project <project>` | Info about a project (Canvas, Horizon) with links |
| `/docs [project] [keyword]` | Without args: docs link. With keyword: searches CanvasMC docs and returns matching links |
| `/git` | Links to the CraftCanvasMC GitHub organization |
| `/faq <type> [for_user]` | FAQs and common resources (Folia Spread, Scheduler, Spark Report, Logs) |
| `/optimizationguide` | Links to the server optimization guide |

## Building and running

Prereq - Java 21

```bash
./gradlew clean build
./gradlew run
```