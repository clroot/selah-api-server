-- Selah Database 초기화 스크립트
-- DataGrip에서 postgres(superuser) 계정으로 실행하세요.

-- 1. 데이터베이스 생성
CREATE DATABASE selah
    WITH ENCODING = 'UTF8'
    LC_COLLATE = 'ko_KR.UTF-8'
    LC_CTYPE = 'ko_KR.UTF-8'
    TEMPLATE = template0;

-- 2. 사용자 생성 (비밀번호는 변경하세요!)
CREATE USER selah_user WITH PASSWORD 'CHANGE_ME_STRONG_PASSWORD';

-- 3. selah 데이터베이스에 연결 후 아래 실행
-- DataGrip에서 selah 데이터베이스로 연결을 변경한 후 실행하세요.
\connect selah;

-- 4. 권한 부여 (public 스키마 사용)
GRANT CONNECT ON DATABASE selah TO selah_user;
GRANT USAGE ON SCHEMA public TO selah_user;
GRANT CREATE ON SCHEMA public TO selah_user;

-- public 스키마 내 모든 테이블에 대한 권한
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO selah_user;

-- public 스키마 내 모든 시퀀스에 대한 권한
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO selah_user;

-- 5. 기본 검색 경로 설정
ALTER USER selah_user SET search_path TO public;

-- 확인
SELECT current_database(), current_schema();
