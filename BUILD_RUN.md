# 치직온(CHZIKON) — 빌드 / 실행 / 배포 가이드

> 구현 산출물 안내. 요구사항/스펙은 핸드오프(`../chzikon-handoff/`) 참조.

## 구성
```
chzikon/
├── backend/   Spring Boot 3.3.5 / Java 21 / MyBatis+JPA / Flyway / H2(local)·MySQL8(prod)
└── frontend/  Vue 3 + TypeScript + Vite
```

## ⚠️ Java 버전
- 백엔드는 **Java 21** 바이트코드. Gradle toolchain 이 빌드 시 JDK21 을 자동 다운로드한다.
- **실행(jar)도 JDK21 필요.** 시스템 java 가 17 이면 `UnsupportedClassVersionError`.
  - 운영 서버에 JDK21 설치 후 그 java 로 실행.

## 로컬 개발
```bash
# 1) 백엔드 (H2 인메모리, 프로파일 local)
cd backend
./gradlew bootRun                 # http://localhost:8080  (Windows: .\gradlew.bat)

# 2) 프론트
cd frontend
npm install
npm run dev                       # http://localhost:5173  (/api·/oauth2·/login → 8080 프록시)
```
- 로컬 OAuth 는 실제 치지직 키가 있어야 동작. 키 없이도 홈/공개 API·어드민 UI 골격 확인 가능.
- 로컬 ADMIN 부여: `CHZZK_ADMIN_CHANNEL_ID` 를 본인 채널ID로 설정 후 로그인.

## 테스트 (EDD/TDD)
```bash
cd backend
./gradlew test
```
- `KeyCipherTest` 키 암호화/마스킹, `RoleCalculatorTest` 권한 임계값 경계,
  `FcfsConcurrencyTest` **슬롯1 동시신청 1명만 성공(ADR-007)**, `contextLoads` Flyway+JPA validate.

## 프로덕션 빌드
```bash
cd backend && ./gradlew bootJar          # build/libs/chzikon-0.0.1-SNAPSHOT.jar
cd frontend && npm run build             # dist/
```

## 배포 (요약 — 02 아키텍처 문서 기준)
1. VM(2vCore/4GB+) + MySQL8(utf8mb4 DB `chzikon` 생성).
2. `.env`(→ `.env.example` 복사 후 채움)로 시크릿 주입. **HTTPS 필수**(certbot) — OAuth 리다이렉트 깨짐 방지.
3. jar 를 JDK21 로 systemd 서비스 등록(재부팅 자동 기동).
4. Nginx: `/api/`·`/oauth2/`·`/login/` → `localhost:8080` 프록시, 그 외 → 프론트 `dist/`.
5. 치지직 개발자센터에 운영 Redirect URI 등록(불일치 시 콜백 실패).
6. 프로덕션 `ddl-auto: validate` 고정. 스키마 변경은 Flyway 새 V 파일만.

## 운영자(대표) 셀프 범위
- 캠페인 CRUD·배포방식/키모드 선택, 키 일괄등록(마스킹), 신청 승인/거절,
  콜라보 게임·영상·로고 CRUD, 팔로워 임계값 변경, 권한 수동 오버라이드, 감사로그 — 전부 `/admin`.
- 더미 데이터 없음. 데이터 없으면 빈 상태로 표시.

## 미구현(스펙상 TBD — 임의 구현 금지)
- 굿즈 PG(ADR-008), 대회(TOUR), 마이페이지 집계(MY) — 발주자 확정 후.
