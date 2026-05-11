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
DB에서 데이터를 조회할 때 바로 락을 걸어버리는 방식.
`SELECT ... FOR UPDATE` 쿼리로 다른 트랜잭션이 같은 데이터에 접근하지 못하게 막음.

#### 구현
- `CouponRepository`에 `@Lock(LockModeType.PESSIMISTIC_WRITE)` 적용
- `CouponIssueService`에서 락이 걸린 메서드로 쿠폰 조회

#### 테스트 결과
| 요청 수 | 기대값 | 실제값 |
|--------|--------|--------|
| 1,000명 | 10장 | 10장 ✅ |

#### 장단점
| 장점 | 단점 |
|------|------|
| 데이터 정합성 보장 | 락 대기로 인한 성능 저하 |
| 구현이 단순 | 데드락 발생 가능성 |
| | 트래픽 많을수록 병목 심화 |