# 선착순 쿠폰 발급 시스템

## 프로젝트 목적
동시성 문제를 직접 재현하고 단계별로 해결하면서 Race Condition과 다양한 락 전략을 학습하기 위한 프로젝트.<br>
단순 구현에 그치지 않고, 문제 발생 → 원인 분석 → 해결 과정을 직접 체험하는 것이 목표.

## 기술 스택
- Java 17
- Spring Boot 3.5.14
- MySQL 8.0
- Redis
- Redisson 3.27.2
- Flyway
- JUnit5

---

## 1단계: 요구사항 파악 + 도메인 모델 / API 설계

### 비즈니스 요구사항
- 관리자는 쿠폰을 생성할 수 있다 (총 발급 수량 지정)
- 사용자는 쿠폰 발급을 요청할 수 있다
- 선착순으로 수량 내에서만 발급된다
- 한 사용자는 동일 쿠폰을 1장만 받을 수 있다
- 수량이 소진되면 발급 요청은 실패 처리된다

### 도메인 모델
| 엔티티 | 주요 필드 |
|--------|-----------|
| `Coupon` | id, name, totalQuantity, issuedQuantity |
| `CouponIssue` | id, coupon, user, regDt |
| `User` | id, username |

### API 설계
| 메서드 | URL | 설명 |
|--------|-----|------|
| POST | /api/coupon-issues | 쿠폰 발급 요청 |

---

## 2단계: 프로젝트 세팅 + 패키지 구조 설계

### 패키지 구조
```
com.example.couponSystem
├── coupon
│   ├── controller
│   ├── entity
│   ├── repository
│   └── service
├── user
│   ├── entity
│   ├── repository
│   └── service
└── global
    └── response
```

### 주요 Dependencies
- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-data-redis
- mysql-connector-j
- flyway-core
- redisson-spring-boot-starter
- lombok

---

## 3단계: 엔티티, 레포지토리, 서비스, 컨트롤러 구현

### 쿠폰 발급 흐름
1. 쿠폰 조회
2. 사용자 조회
3. 중복 발급 확인
4. 재고 확인 및 감소 (`coupon.issue()`)
5. 발급 내역 저장 (`CouponIssue`)

### Flyway 마이그레이션
- `V1__create_users.sql`
- `V2__create_coupons.sql`
- `V3__create_coupon_issues.sql`

---

## 4단계: 동시성 문제 재현

### Race Condition이란?
여러 스레드가 동시에 공유 자원(쿠폰 재고)에 접근할 때 발생하는 문제.
각 스레드가 동시에 `issuedQuantity`를 읽으면 모두 같은 값을 보게 되고,
각자 재고가 있다고 판단해 발급을 진행한다.
결과적으로 실제 재고보다 적게 또는 예상과 다른 수량이 발급된다.

### 테스트 환경
- 쿠폰 총 수량: 10장
- 동시 요청 수: 1,000명
- 스레드 풀: 32개

### 테스트 결과
| 기대값 | 실제값 |
|--------|--------|
| 10장 | 1장 |

### 문제 흐름
1. 1,000개 스레드가 동시에 `issuedQuantity` 조회 → 모두 `0` 읽음
2. 모두 재고 있다고 판단 → 동시에 발급 진행
3. 마지막으로 UPDATE한 스레드의 값만 DB에 반영
4. 결과적으로 1장만 발급됨

---

## 5단계: 동시성 문제 해결

### 1. 비관적 락 (Pessimistic Lock)

#### 개념
DB에서 데이터를 조회할 때 바로 락을 걸어버리는 방식.<br>
`SELECT ... FOR UPDATE` 쿼리로 다른 트랜잭션이 같은 데이터에 접근하지 못하게 막음.

#### 구현
- `CouponRepository`에 `@Lock(LockModeType.PESSIMISTIC_WRITE)` 적용
- `CouponIssueService`에서 락이 걸린 메서드로 쿠폰 조회

#### 테스트 결과
| 요청 수 | 기대값 | 실제값 | 실행 시간 |
|--------|--------|--------|----------|
| 1,000명 | 10장 | 10장 ✅ | 6,043ms (5회 평균) |

#### 장단점
| 장점 | 단점 |
|------|------|
| 데이터 정합성 보장 | 락 대기로 인한 성능 저하 |
| 구현이 단순 | 데드락 발생 가능성 |
| | 트래픽 많을수록 병목 심화 |

---

### 2. 낙관적 락 (Optimistic Lock)

#### 개념
DB 락을 걸지 않고, `version` 컬럼으로 충돌을 감지하는 방식.<br>
커밋 시점에 읽었던 version과 현재 DB version을 비교해, 다르면 `ObjectOptimisticLockingFailureException`을 던짐.<br>
충돌이 드문 환경에서 비관적 락보다 성능이 유리하다.

#### 구현
- `Coupon` 엔티티에 `@Version private Long version` 필드 추가
- Flyway 마이그레이션 `V4__add_version_to_coupons.sql`로 DB 컬럼 추가
- `CouponRepository`에 `@Lock(LockModeType.OPTIMISTIC)` 적용
- `CouponIssueService`에서 낙관적 락 메서드로 쿠폰 조회
- `CouponIssueFacade` 추가 — `ObjectOptimisticLockingFailureException` 발생 시 최대 10회 재시도

#### 테스트 결과
| 요청 수 | 기대값 | 실제값 | 실행 시간 |
|--------|--------|--------|----------|
| 1,000명 | 10장 | 10장 ✅ | 2,361ms (5회 평균) |

> `CouponIssueFacade`의 재시도 로직으로 충돌 발생 시에도 정합성이 보장됨.

#### 장단점
| 장점 | 단점 |
|------|------|
| DB 락 없이 성능 유리 | 충돌 시 예외 처리 및 재시도 로직 필요 |
| 읽기가 많고 충돌이 드문 환경에 적합 | 충돌 빈도 높으면 재시도 오버헤드 발생 |
| 데드락 없음 | 구현 복잡도 증가 |

---

### 3. 분산 락 (Distributed Lock with Redisson)

#### 개념
DB 락이 아닌 Redis를 중간 조율자로 사용하는 방식.<br>
DB에 접근하기 전에 Redis에서 먼저 락을 획득한 서버만 로직을 실행하게 해, 여러 앱 서버가 동시에 요청해도 순차 처리가 보장된다.
```
서버 A ──┐
서버 B ──┼──▶ Redis (락 획득 경쟁) ──▶ 락 획득한 서버만 DB 접근
서버 C ──┘
```

#### 구현
- `CouponIssueService`는 일반 `findById`로 복원 (DB 락 제거)
- `CouponIssueRequestService` 신규 추가 — Redisson `RLock` 획득/해제 담당
- 락 키: `coupon:lock:{couponId}`
- `tryLock(10, 3, TimeUnit.SECONDS)`: 최대 10초 대기, 락 보유 시간 3초

#### 테스트 결과
| 요청 수 | 기대값 | 실제값 | 실행 시간 |
|--------|--------|--------|----------|
| 1,000명 | 10장 | 10장 ✅ | 11,637ms (5회 평균) |

#### 장단점
| 장점 | 단점 |
|------|------|
| 다중 서버 환경에서 정합성 보장 | 매 요청마다 Redis 네트워크 통신 비용 발생 |
| DB 부하 감소 | 단일 서버 환경에서는 오히려 느림 |
| 데드락 없음 | Redis 장애 시 락 획득 불가 |

---

### 락 전략 최종 비교

#### 성능 비교 (단일 서버, 1,000명 동시 요청, 쿠폰 10장)

| 방식 | 실행 시간 | 정합성 |
|------|----------|--------|
| 비관적 락 | 6,043ms (5회 평균) | 항상 보장 |
| 낙관적 락 | 2,361ms (5회 평균) | 항상 보장 |
| 분산 락 | 11,637ms (5회 평균) | 항상 보장 |

#### 결과 분석

낙관적 락이 가장 빠르고 분산 락이 가장 느리게 나왔지만, 이는 **단일 서버 환경**이라는 조건 때문이다. 수치는 동일 조건 5회 반복 측정 평균값이다.

- **낙관적 락**: DB 락도 없고 Redis 통신도 없어 오버헤드가 가장 적음. `CouponIssueFacade`의 재시도 로직으로 충돌 시에도 정합성이 보장됨.
- **비관적 락**: DB 내부에서 락을 처리해 추가 네트워크 비용 없음. 단일 서버에서는 분산 락보다 유리.
- **분산 락**: 매 요청마다 Redis 왕복 통신이 추가되어 단일 서버에서는 가장 느림. 그러나 **앱 서버가 여러 대인 분산 환경**에서는 DB 커넥션 경쟁과 데드락 위험 없이 중앙에서 락을 조율할 수 있어 가장 확장성이 높은 방식이다.

#### 환경별 적합한 락 전략

| 환경 | 추천 방식 |
|------|----------|
| 단일 서버, 충돌 빈도 낮음 | 낙관적 락 + 재시도 |
| 단일 서버, 충돌 빈도 높음 | 비관적 락 |
| 다중 서버 (분산 환경) | 분산 락 (Redisson) |