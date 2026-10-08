#!/usr/bin/env bash

# RBOT-OPS-004, RBOT-OPS-025, REQ-DEPLOYMENT-014

rbot_stand() {
  case "$1" in
    qa)
      RBOT_STAND_CONTEXT="qa"
      RBOT_STAND_PORT="4206"
      ;;
    *)
      printf 'стенда %s нет: qa\n' "$1" >&2
      return 2
      ;;
  esac
  RBOT_STAND_PROJECT="rbot-$1"
  RBOT_STAND_ADMIN="http://qa.admin.yanapaderina.test:$RBOT_STAND_PORT"
  RBOT_STAND_BOT="http://qa.bot.yanapaderina.test:$RBOT_STAND_PORT"
}
