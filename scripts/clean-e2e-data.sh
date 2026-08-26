#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────
# E2E 가 남긴 테스트 데이터 정리 (MariaDB)
#
# 실행:  bash scripts/clean-e2e-data.sh
#
# E2E 는 매 실행마다 고유 이메일(@haru.test)로 회원가입한다.
# 계정이 겹치면 테스트가 서로를 방해하기 때문인데, 그만큼 DB에 쌓인다.
# (한 번 정리했을 때 계정 477개 · 글 45개가 있었다)
#
# ⚠️ 회원을 참조하는 표가 늘어나면 여기도 늘려야 한다 —
#    빠뜨리면 FK 위반으로 계정 삭제가 통째로 막힌다 (favorite·post_like·subscription)
#
# 남기는 것: 실제 계정과, 글·댓글이 달린 계정
# 지우는 것: @haru.test 이면서 흔적이 없는 계정 + 제목에 타임스탬프가 있는 글
#
# ⚠️ DB는 하나뿐이다 — 로컬 백엔드와 컨테이너가 함께 쓰는 MariaDB.
# ─────────────────────────────────────────────────────────────
set -euo pipefail
cd "$(dirname "$0")/.."

if ! docker ps --format '{{.Names}}' | grep -q '^haruverse-mariadb$'; then
  echo "MariaDB 컨테이너가 떠 있지 않습니다:  docker compose up -d mariadb"
  exit 1
fi

q() { docker exec haruverse-mariadb mariadb -uharuverse -pharuverse haruverse -e "$1" 2>&1 | grep -v "Warning" || true; }

echo "=== 정리 전 ==="
q "select (select count(*) from member) as members, (select count(*) from post) as posts;"

# ★삭제 순서★ 참조하는 쪽부터 — 순서를 어기면 FK 위반으로 막힌다
echo
echo "--- 테스트 글(제목에 타임스탬프)과 딸린 것들 ---"
q "delete from post_like where post_id in (select id from post where title like '%178%');
   delete from comment   where post_id in (select id from post where title like '%178%');
   delete from post      where title like '%178%';"

# ★MySQL/MariaDB 는 '삭제 대상 테이블'을 서브쿼리에서 직접 읽지 못한다★
#   delete from member where id not in (select ... from member) → 에러.
#   대상 id 를 임시 테이블에 담아두고 그것을 참조한다.
echo "--- 흔적 없는 테스트 계정 ---"
q "drop temporary table if exists _drop;
   create temporary table _drop as
     select id from member
     where email like '%@haru.test'
       and id not in (select member_id from post)
       and id not in (select member_id from comment);
   delete from favorite     where member_id in (select id from _drop);
   delete from post_like    where member_id in (select id from _drop);
   delete from subscription where member_id in (select id from _drop);
   delete from member       where id in (select id from _drop);
   drop temporary table _drop;"

echo
echo "=== 정리 후 ==="
q "select (select count(*) from member) as members, (select count(*) from post) as posts;"
echo
echo "남은 계정:"
q "select id, nickname, email from member order by id;"
